package com.company.sportseq.knowledge.loader;

import com.company.sportseq.knowledge.exception.KnowledgeException;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 文档解析（DocumentReader 封装）：根据扩展名选择解析器。
 *
 * <ul>
 *   <li>txt / md：{@link TextReader}，按 UTF-8 读取整篇文本</li>
 *   <li>docx：Apache POI {@link XWPFWordExtractor}（与 EasyExcel 共用 POI 4.1.2，无版本冲突）</li>
 *   <li>pdf：{@link PagePdfDocumentReader}（Spring AI + PDFBox）</li>
 * </ul>
 */
public class DocumentLoader {

    public static final Set<String> SUPPORTED_TYPES = Set.of("txt", "md", "docx", "pdf");

    private static final Set<String> TEXT_TYPES = Set.of("txt", "md");

    public static boolean isSupported(String fileType) {
        return fileType != null && SUPPORTED_TYPES.contains(normalize(fileType));
    }

    /**
     * 读取并解析文档为 Spring AI Document 列表（通常一篇文档一个 Document，后续统一切分）。
     */
    public List<Document> read(Resource resource, String fileType) {
        String type = normalize(fileType);
        if (TEXT_TYPES.contains(type)) {
            return new TextReader(resource).get();
        }
        if ("docx".equals(type)) {
            return readDocx(resource);
        }
        if ("pdf".equals(type)) {
            return new PagePdfDocumentReader(resource).get();
        }
        throw new KnowledgeException(KnowledgeException.INVALID_FILE,
                "不支持的文件类型：" + fileType + "（支持 txt/md/docx/pdf）");
    }

    private List<Document> readDocx(Resource resource) {
        try (InputStream in = resource.getInputStream();
             XWPFDocument xwpf = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(xwpf)) {
            return List.of(new Document(extractor.getText()));
        } catch (IOException | RuntimeException e) {
            throw new KnowledgeException(KnowledgeException.INVALID_FILE,
                    "docx 文档解析失败：" + e.getMessage());
        }
    }

    private static String normalize(String fileType) {
        return fileType == null ? "" : fileType.trim().toLowerCase(Locale.ROOT);
    }
}
