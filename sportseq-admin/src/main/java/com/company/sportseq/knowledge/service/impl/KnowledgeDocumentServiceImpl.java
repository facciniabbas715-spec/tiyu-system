package com.company.sportseq.knowledge.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.knowledge.config.RagProperties;
import com.company.sportseq.knowledge.entity.KnowledgeChunk;
import com.company.sportseq.knowledge.entity.KnowledgeDocument;
import com.company.sportseq.knowledge.exception.KnowledgeException;
import com.company.sportseq.knowledge.loader.DocumentLoader;
import com.company.sportseq.knowledge.mapper.KnowledgeChunkMapper;
import com.company.sportseq.knowledge.mapper.KnowledgeDocumentMapper;
import com.company.sportseq.knowledge.service.KnowledgeDocumentService;
import com.company.sportseq.knowledge.splitter.KnowledgeTextSplitter;
import com.company.sportseq.knowledge.vector.VectorStoreService;
import com.company.sportseq.knowledge.vo.KnowledgeChunkVO;
import com.company.sportseq.knowledge.vo.KnowledgeDocumentDetailVO;
import com.company.sportseq.knowledge.vo.KnowledgeDocumentVO;
import com.company.sportseq.knowledge.vo.SeedResultVO;
import com.company.sportseq.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 知识库文档管理实现。
 *
 * <p>写入链路：保存原始文件 → 落库(status=处理中) → 解析 → 切分 → 写 chunk →
 * 向量化入库 → 置为已就绪；任一步失败清理分块并把文档置为失败。</p>
 */
@Slf4j
@Service
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    private static final String SEED_LOCATION = "classpath:knowledge/*.md";

    private final KnowledgeDocumentMapper documentMapper;

    private final KnowledgeChunkMapper chunkMapper;

    private final DocumentLoader documentLoader;

    private final KnowledgeTextSplitter textSplitter;

    private final VectorStoreService vectorStoreService;

    private final RagProperties properties;

    public KnowledgeDocumentServiceImpl(KnowledgeDocumentMapper documentMapper,
                                        KnowledgeChunkMapper chunkMapper,
                                        DocumentLoader documentLoader,
                                        KnowledgeTextSplitter textSplitter,
                                        VectorStoreService vectorStoreService,
                                        RagProperties properties) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.documentLoader = documentLoader;
        this.textSplitter = textSplitter;
        this.vectorStoreService = vectorStoreService;
        this.properties = properties;
    }

    @Override
    public PageResult<KnowledgeDocumentVO> page(long current, long size, String keyword,
                                                String fileType, Integer status) {
        Page<KnowledgeDocument> page = documentMapper.selectPage(new Page<>(current, size),
                Wrappers.<KnowledgeDocument>lambdaQuery()
                        .and(StrUtil.isNotBlank(keyword), wrapper -> wrapper
                                .like(KnowledgeDocument::getTitle, keyword)
                                .or()
                                .like(KnowledgeDocument::getFileName, keyword))
                        .eq(StrUtil.isNotBlank(fileType), KnowledgeDocument::getFileType, fileType)
                        .eq(status != null, KnowledgeDocument::getStatus, status)
                        .orderByDesc(KnowledgeDocument::getCreateTime));
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(),
                page.getRecords().stream().map(KnowledgeDocumentServiceImpl::toVo).toList());
    }

    @Override
    public KnowledgeDocumentDetailVO detail(Long id) {
        KnowledgeDocument document = requireDocument(id);
        List<KnowledgeChunk> chunks = chunkMapper.selectList(Wrappers.<KnowledgeChunk>lambdaQuery()
                .eq(KnowledgeChunk::getDocumentId, id)
                .orderByAsc(KnowledgeChunk::getChunkIndex));
        return new KnowledgeDocumentDetailVO(
                document.getId(), document.getTitle(), document.getFileName(), document.getFileType(),
                document.getFileSize(), document.getStatus(), document.getChunkCount(),
                document.getErrorMsg(), document.getRemark(), document.getCreateTime(),
                document.getUpdateTime(),
                chunks.stream()
                        .map(chunk -> new KnowledgeChunkVO(chunk.getId(), chunk.getChunkIndex(),
                                chunk.getContent(), chunk.getTokenCount()))
                        .toList());
    }

    @Override
    public KnowledgeDocumentVO upload(MultipartFile file, String title, String remark) {
        validateUpload(file);
        String originalName = file.getOriginalFilename();
        String fileType = extension(originalName);
        Path target = saveFile(file, newStoredName(fileType));

        KnowledgeDocument document = new KnowledgeDocument();
        document.setTitle(StringUtils.hasText(title) ? title.trim() : stripExtension(originalName));
        document.setFileName(originalName);
        document.setFileType(fileType);
        document.setFileSize(file.getSize());
        document.setFilePath(target.getFileName().toString());
        document.setStatus(KnowledgeDocument.STATUS_PROCESSING);
        document.setChunkCount(0);
        document.setRemark(remark);
        document.setCreateBy(currentUserId());
        documentMapper.insert(document);

        try {
            indexFile(document, new FileSystemResource(target.toFile()));
            return toVo(requireDocument(document.getId()));
        } catch (KnowledgeException e) {
            markFailed(document.getId(), e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            markFailed(document.getId(), "文档处理失败：" + e.getMessage());
            throw translate(e);
        }
    }

    @Override
    public KnowledgeDocumentVO update(Long id, MultipartFile file, String title, String remark) {
        KnowledgeDocument document = requireDocument(id);
        boolean replaceFile = file != null && !file.isEmpty();
        if (replaceFile) {
            validateUpload(file);
            String newType = extension(file.getOriginalFilename());
            Path target = saveFile(file, newStoredName(newType));
            String oldStoredName = document.getFilePath();

            deleteChunksAndVectors(id);
            chunkMapper.delete(Wrappers.<KnowledgeChunk>lambdaQuery()
                    .eq(KnowledgeChunk::getDocumentId, id));

            document.setFileName(file.getOriginalFilename());
            document.setFileType(newType);
            document.setFileSize(file.getSize());
            document.setFilePath(target.getFileName().toString());
            if (StringUtils.hasText(title)) {
                document.setTitle(title.trim());
            }
            KnowledgeDocument processing = new KnowledgeDocument();
            processing.setId(id);
            processing.setFileName(document.getFileName());
            processing.setFileType(newType);
            processing.setFileSize(file.getSize());
            processing.setFilePath(document.getFilePath());
            processing.setStatus(KnowledgeDocument.STATUS_PROCESSING);
            processing.setChunkCount(0);
            processing.setErrorMsg(null);
            processing.setUpdateBy(currentUserId());
            documentMapper.updateById(processing);

            try {
                indexFile(document, new FileSystemResource(target.toFile()));
                deleteStoredFileQuietly(oldStoredName);
            } catch (KnowledgeException e) {
                markFailed(id, e.getMessage());
                throw e;
            } catch (RuntimeException e) {
                markFailed(id, "文档处理失败：" + e.getMessage());
                throw translate(e);
            }
        }

        KnowledgeDocument meta = new KnowledgeDocument();
        meta.setId(id);
        if (StringUtils.hasText(title)) {
            meta.setTitle(title.trim());
        }
        if (remark != null) {
            meta.setRemark(remark);
        }
        meta.setUpdateBy(currentUserId());
        documentMapper.updateById(meta);
        return toVo(requireDocument(id));
    }

    @Override
    public void delete(Long id) {
        KnowledgeDocument document = requireDocument(id);
        deleteChunksAndVectors(id);
        chunkMapper.delete(Wrappers.<KnowledgeChunk>lambdaQuery()
                .eq(KnowledgeChunk::getDocumentId, id));
        documentMapper.deleteById(id);
        deleteStoredFileQuietly(document.getFilePath());
    }

    @Override
    public KnowledgeDocumentVO rebuild(Long id) {
        KnowledgeDocument document = requireDocument(id);
        Path stored = storedFilePath(document.getFilePath());
        if (!Files.exists(stored)) {
            throw new KnowledgeException(KnowledgeException.NOT_FOUND, "原始文件不存在，请重新上传文档后再重建向量");
        }
        deleteChunksAndVectors(id);
        chunkMapper.delete(Wrappers.<KnowledgeChunk>lambdaQuery()
                .eq(KnowledgeChunk::getDocumentId, id));

        KnowledgeDocument processing = new KnowledgeDocument();
        processing.setId(id);
        processing.setStatus(KnowledgeDocument.STATUS_PROCESSING);
        processing.setChunkCount(0);
        processing.setErrorMsg(null);
        processing.setUpdateBy(currentUserId());
        documentMapper.updateById(processing);

        try {
            indexFile(document, new FileSystemResource(stored.toFile()));
            return toVo(requireDocument(id));
        } catch (KnowledgeException e) {
            markFailed(id, e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            markFailed(id, "重建向量失败：" + e.getMessage());
            throw translate(e);
        }
    }

    @Override
    public SeedResultVO seed() {
        Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(SEED_LOCATION);
        } catch (IOException e) {
            throw new KnowledgeException(KnowledgeException.INVALID_FILE, "读取内置知识库失败：" + e.getMessage());
        }
        int imported = 0;
        int skipped = 0;
        for (Resource resource : resources) {
            String fileName = resource.getFilename();
            String title = stripExtension(fileName);
            Long exists = documentMapper.selectCount(Wrappers.<KnowledgeDocument>lambdaQuery()
                    .eq(KnowledgeDocument::getTitle, title));
            if (exists > 0) {
                skipped++;
                continue;
            }
            importSeedResource(resource, fileName, title);
            imported++;
        }
        return new SeedResultVO(imported, skipped);
    }

    private void importSeedResource(Resource resource, String fileName, String title) {
        Path target = null;
        try (InputStream in = resource.getInputStream()) {
            String storedName = newStoredName("md");
            target = saveStream(in, storedName);
            KnowledgeDocument document = new KnowledgeDocument();
            document.setTitle(title);
            document.setFileName(fileName);
            document.setFileType("md");
            document.setFileSize(Files.size(target));
            document.setFilePath(storedName);
            document.setStatus(KnowledgeDocument.STATUS_PROCESSING);
            document.setChunkCount(0);
            document.setRemark("内置知识库");
            document.setCreateBy(currentUserId());
            documentMapper.insert(document);
            indexFile(document, new FileSystemResource(target.toFile()));
        } catch (KnowledgeException e) {
            if (target != null) {
                deleteStoredFileQuietly(target.getFileName().toString());
            }
            throw e;
        } catch (Exception e) {
            if (target != null) {
                deleteStoredFileQuietly(target.getFileName().toString());
            }
            throw new KnowledgeException(KnowledgeException.INVALID_FILE,
                    "内置知识库导入失败：" + e.getMessage());
        }
    }

    /**
     * 解析 → 切分 → 写 chunk → 向量化，并把文档置为已就绪。
     */
    private void indexFile(KnowledgeDocument document, Resource resource) {
        List<String> texts = documentLoader.read(resource, document.getFileType()).stream()
                .flatMap(parsed -> textSplitter.split(parsed.getText()).stream())
                .toList();
        if (texts.isEmpty()) {
            throw new KnowledgeException(KnowledgeException.INVALID_FILE, "文档内容为空，无法生成知识片段");
        }
        writeChunksAndVectors(document, texts);
    }

    private void writeChunksAndVectors(KnowledgeDocument document, List<String> texts) {
        List<Long> insertedChunkIds = new ArrayList<>(texts.size());
        try {
            List<Document> documents = new ArrayList<>(texts.size());
            for (int i = 0; i < texts.size(); i++) {
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setDocumentId(document.getId());
                chunk.setChunkIndex(i);
                chunk.setContent(texts.get(i));
                chunkMapper.insert(chunk);
                insertedChunkIds.add(chunk.getId());
                documents.add(Document.builder()
                        .id(String.valueOf(chunk.getId()))
                        .text(texts.get(i))
                        .metadata(Map.of(
                                VectorStoreService.METADATA_DOCUMENT_ID, document.getId(),
                                VectorStoreService.METADATA_CHUNK_INDEX, i,
                                VectorStoreService.METADATA_TITLE, document.getTitle(),
                                VectorStoreService.METADATA_FILE_TYPE, document.getFileType()))
                        .build());
            }
            vectorStoreService.store(documents);

            KnowledgeDocument ready = new KnowledgeDocument();
            ready.setId(document.getId());
            ready.setStatus(KnowledgeDocument.STATUS_READY);
            ready.setChunkCount(texts.size());
            ready.setErrorMsg(null);
            ready.setUpdateBy(currentUserId());
            documentMapper.updateById(ready);
        } catch (RuntimeException e) {
            if (!insertedChunkIds.isEmpty()) {
                chunkMapper.deleteBatchIds(insertedChunkIds);
            }
            throw e;
        }
    }

    private void deleteChunksAndVectors(Long documentId) {
        List<Long> chunkIds = chunkMapper.selectList(Wrappers.<KnowledgeChunk>lambdaQuery()
                        .eq(KnowledgeChunk::getDocumentId, documentId)
                        .select(KnowledgeChunk::getId))
                .stream()
                .map(KnowledgeChunk::getId)
                .toList();
        if (!chunkIds.isEmpty()) {
            vectorStoreService.delete(chunkIds);
        }
    }

    private void markFailed(Long id, String message) {
        KnowledgeDocument failed = new KnowledgeDocument();
        failed.setId(id);
        failed.setStatus(KnowledgeDocument.STATUS_FAILED);
        failed.setErrorMsg(message == null ? "未知错误" : message.substring(0, Math.min(message.length(), 500)));
        failed.setUpdateBy(currentUserId());
        documentMapper.updateById(failed);
    }

    private KnowledgeDocument requireDocument(Long id) {
        KnowledgeDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new KnowledgeException(KnowledgeException.NOT_FOUND, "知识文档不存在");
        }
        return document;
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new KnowledgeException(KnowledgeException.INVALID_FILE, "请选择要上传的知识文档");
        }
        if (file.getSize() > properties.getMaxFileSize()) {
            throw new KnowledgeException(KnowledgeException.INVALID_FILE,
                    "文件大小超过限制：" + (properties.getMaxFileSize() / 1024 / 1024) + "MB");
        }
        if (!DocumentLoader.isSupported(extension(file.getOriginalFilename()))) {
            throw new KnowledgeException(KnowledgeException.INVALID_FILE,
                    "不支持的文件类型，仅支持 txt/md/docx/pdf");
        }
    }

    private Path saveFile(MultipartFile file, String storedName) {
        try (InputStream in = file.getInputStream()) {
            return saveStream(in, storedName);
        } catch (IOException e) {
            throw new KnowledgeException(KnowledgeException.INVALID_FILE, "保存上传文件失败：" + e.getMessage());
        }
    }

    private Path saveStream(InputStream in, String storedName) throws IOException {
        Path directory = uploadDirectory();
        Files.createDirectories(directory);
        Path target = directory.resolve(storedName);
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private Path uploadDirectory() {
        return Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
    }

    private Path storedFilePath(String storedName) {
        return uploadDirectory().resolve(storedName).normalize();
    }

    private String newStoredName(String fileType) {
        return UUID.randomUUID().toString().replace("-", "") + "." + fileType;
    }

    private void deleteStoredFileQuietly(String storedName) {
        if (!StringUtils.hasText(storedName)) {
            return;
        }
        try {
            Files.deleteIfExists(storedFilePath(storedName));
        } catch (IOException e) {
            log.warn("知识库原始文件删除失败：{}", storedName, e);
        }
    }

    private Long currentUserId() {
        try {
            return SecurityUtils.getUserId();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private KnowledgeException translate(RuntimeException e) {
        return new KnowledgeException(KnowledgeException.INVALID_FILE,
                "文档解析失败，请确认文件格式与内容正确（" + e.getMessage() + "）");
    }

    private static KnowledgeDocumentVO toVo(KnowledgeDocument document) {
        return new KnowledgeDocumentVO(
                document.getId(), document.getTitle(), document.getFileName(), document.getFileType(),
                document.getFileSize(), document.getStatus(), document.getChunkCount(),
                document.getErrorMsg(), document.getRemark(), document.getCreateTime(),
                document.getUpdateTime());
    }

    private static String extension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static String stripExtension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return fileName == null ? "" : fileName;
        }
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }
}
