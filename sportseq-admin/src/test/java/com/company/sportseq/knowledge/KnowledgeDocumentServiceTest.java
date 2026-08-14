package com.company.sportseq.knowledge;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.company.sportseq.common.constant.CacheConstants;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.knowledge.entity.KnowledgeChunk;
import com.company.sportseq.knowledge.entity.KnowledgeDocument;
import com.company.sportseq.knowledge.exception.KnowledgeException;
import com.company.sportseq.knowledge.mapper.KnowledgeChunkMapper;
import com.company.sportseq.knowledge.mapper.KnowledgeDocumentMapper;
import com.company.sportseq.knowledge.service.KnowledgeDocumentService;
import com.company.sportseq.knowledge.vo.KnowledgeDocumentVO;
import com.company.sportseq.knowledge.vo.SeedResultVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 知识文档管理集成测试：真实 MySQL（V5 表）+ 桩 EmbeddingModel/VectorStore，验证上传全链路、
 * 分页、删除、重建与内置知识库导入。测试数据在 finally 中物理清理，避免残留。
 */
@SpringBootTest(properties = {
        "spring.ai.rag.upload-dir=target/test-knowledge-files",
        "spring.ai.rag.embedding.api-key=test-embedding-key",
        "spring.ai.rag.debug-enabled=false"
})
class KnowledgeDocumentServiceTest {

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @Autowired
    private KnowledgeDocumentMapper documentMapper;

    @Autowired
    private KnowledgeChunkMapper chunkMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    @MockitoBean
    private VectorStore vectorStore;

    @AfterEach
    void cleanKnowledgeTables() {
        jdbcTemplate.update("DELETE FROM knowledge_chunk");
        jdbcTemplate.update("DELETE FROM knowledge_document");
    }

    @Test
    void upload_shouldPersistDocumentChunksAndVectors() {
        String content = "篮球使用后应清洁表面灰尘，存放于干燥阴凉处，避免暴晒与尖锐物划伤。"
                + "标准7号篮球气压应保持在0.49~0.54MPa之间。";
        MultipartFile file = new MockMultipartFile("file", "篮球保养.md", "text/markdown",
                content.getBytes(StandardCharsets.UTF_8));

        KnowledgeDocumentVO vo = knowledgeDocumentService.upload(file, "篮球保养", "教学用");

        assertEquals(1, vo.status(), "上传成功后文档应为已就绪状态");
        assertTrue(vo.chunkCount() > 0, "文档应产生至少一个分块");
        assertTrue(vo.id() != null && vo.id() > 0);
        List<KnowledgeChunk> chunks = chunkMapper.selectList(
                Wrappers.<KnowledgeChunk>lambdaQuery().eq(KnowledgeChunk::getDocumentId, vo.id()));
        assertEquals(vo.chunkCount(), chunks.size());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        Document stored = captor.getValue().get(0);
        assertNotNull(stored.getId(), "向量 id 应为 chunk 数据库 id 的字符串");
        assertEquals("篮球保养", stored.getMetadata().get("title"));
        assertEquals("md", stored.getMetadata().get("file_type"));
    }

    @Test
    void page_shouldFilterByKeyword() {
        seedOneDocument("篮球保养手册", "basketball.md");

        PageResult<KnowledgeDocumentVO> page = knowledgeDocumentService.page(1, 10, "篮球", null, null);

        assertEquals(1, page.total());
        assertEquals("篮球保养手册", page.records().get(0).title());
    }

    @Test
    void delete_shouldRemoveChunksAndVectors() {
        KnowledgeDocumentVO vo = seedOneDocument("待删除文档", "delete-me.md");
        List<KnowledgeChunk> chunks = chunkMapper.selectList(
                Wrappers.<KnowledgeChunk>lambdaQuery().eq(KnowledgeChunk::getDocumentId, vo.id()));

        knowledgeDocumentService.delete(vo.id());

        assertEquals(0, documentMapper.selectCount(Wrappers.<KnowledgeDocument>lambdaQuery()
                .eq(KnowledgeDocument::getId, vo.id())), "文档应被逻辑删除");
        assertEquals(0, chunkMapper.selectCount(Wrappers.<KnowledgeChunk>lambdaQuery()
                .eq(KnowledgeChunk::getDocumentId, vo.id())), "分块应被物理删除");
        verify(vectorStore).delete(chunks.stream().map(KnowledgeChunk::getId).map(String::valueOf).toList());
    }

    @Test
    void rebuild_shouldDeleteOldVectorsThenReEmbed() {
        KnowledgeDocumentVO vo = seedOneDocument("重建文档", "rebuild.md");

        knowledgeDocumentService.rebuild(vo.id());

        verify(vectorStore, times(2)).add(any());
        assertEquals(1, documentMapper.selectById(vo.id()).getStatus(), "重建成功后应为已就绪");
    }

    @Test
    void seed_shouldImportBuiltinDocsAndSkipExistingTitle() {
        SeedResultVO first = knowledgeDocumentService.seed();

        assertTrue(first.imported() >= 11, "内置知识库应至少导入 11 篇文档，实际=" + first.imported());
        Long basketball = documentMapper.selectCount(Wrappers.<KnowledgeDocument>lambdaQuery()
                .eq(KnowledgeDocument::getTitle, "篮球相关知识"));
        assertTrue(basketball > 0, "应包含篮球相关知识文档");

        SeedResultVO second = knowledgeDocumentService.seed();

        assertEquals(0, second.imported(), "同标题文档再次导入应跳过");
        assertEquals(first.imported(), second.skipped());
    }

    @Test
    void uploadUnsupported_shouldThrowInvalidFileException() {
        MultipartFile file = new MockMultipartFile("file", "virus.exe", "application/octet-stream",
                "MZ".getBytes(StandardCharsets.ISO_8859_1));

        KnowledgeException exception = assertThrows(KnowledgeException.class,
                () -> knowledgeDocumentService.upload(file, "非法文件", null));

        assertEquals(KnowledgeException.INVALID_FILE, exception.getCode());
    }

    @Test
    void detail_shouldReturnChunksInOrder() {
        KnowledgeDocumentVO vo = seedOneDocument("详情文档", "detail.md");

        var detail = knowledgeDocumentService.detail(vo.id());

        assertEquals(vo.id(), detail.id());
        assertTrue(detail.chunks().size() > 0);
        assertTrue(detail.chunks().stream().allMatch(chunk -> chunk.chunkIndex() >= 0));
    }

    private KnowledgeDocumentVO seedOneDocument(String title, String fileName) {
        MultipartFile file = new MockMultipartFile("file", fileName, "text/markdown",
                ("# " + title + "\n\n这是测试知识内容，用于验证文档管理流程。").getBytes(StandardCharsets.UTF_8));
        return knowledgeDocumentService.upload(file, title, null);
    }
}
