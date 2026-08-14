package com.company.sportseq.knowledge;

import com.company.sportseq.knowledge.exception.KnowledgeException;
import com.company.sportseq.knowledge.loader.DocumentLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文档加载单元测试：txt/md 走 TextReader，docx 走 Tika，非法类型报 3202。
 */
class DocumentLoaderTest {

    private final DocumentLoader loader = new DocumentLoader();

    @Test
    void loadTxt_shouldReturnSingleDocumentWithContent() {
        Resource resource = new ClassPathResource("test-knowledge.txt");

        List<Document> documents = loader.read(resource, "txt");

        assertEquals(1, documents.size());
        assertTrue(documents.get(0).getText().contains("体育器材使用说明"));
    }

    @Test
    void loadMarkdown_shouldReturnContent() {
        Resource resource = new ClassPathResource("test-knowledge.md");

        List<Document> documents = loader.read(resource, "md");

        assertEquals(1, documents.size());
        assertTrue(documents.get(0).getText().contains("篮球基础知识"));
    }

    @Test
    void loadDocx_shouldExtractParagraphText(@TempDir Path tempDir) throws IOException {
        Path docx = tempDir.resolve("knowledge.docx");
        writeMinimalDocx(docx, "篮球保养要点：使用后清洁表面并存放于干燥阴凉处。");

        List<Document> documents = loader.read(new FileSystemResource(docx), "docx");

        assertEquals(1, documents.size());
        assertTrue(documents.get(0).getText().contains("篮球保养要点"),
                "Tika 应提取出 docx 正文");
        assertFalse(documents.get(0).getText().isBlank());
    }

    @Test
    void loadUnsupportedType_shouldThrowInvalidFileException() {
        Resource resource = new ClassPathResource("test-knowledge.txt");

        KnowledgeException exception = assertThrows(KnowledgeException.class,
                () -> loader.read(resource, "exe"));

        assertEquals(KnowledgeException.INVALID_FILE, exception.getCode());
    }

    private void writeMinimalDocx(Path target, String text) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                    </Types>
                    """.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("_rels/.rels"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                    </Relationships>
                    """.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                      <w:body><w:p><w:r><w:t>%s</w:t></w:r></w:p></w:body>
                    </w:document>
                    """.formatted(text)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }
}
