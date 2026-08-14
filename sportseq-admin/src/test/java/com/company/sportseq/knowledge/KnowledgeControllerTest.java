package com.company.sportseq.knowledge;

import cn.hutool.json.JSONUtil;
import com.company.sportseq.common.constant.CacheConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 知识库接口契约测试：认证、权限、上传、分页、删除、内置导入（真实 MySQL + 桩模型/向量库）。
 */
@SpringBootTest(properties = {
        "spring.ai.rag.upload-dir=target/test-knowledge-files",
        "spring.ai.rag.vector.type=simple",
        "spring.ai.rag.embedding.api-key=test-embedding-key"
})
@AutoConfigureMockMvc
class KnowledgeControllerTest {

    private static final String CAPTCHA_UUID = "knowledge-captcha-uuid";
    private static final String CAPTCHA_CODE = "1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

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
    void page_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/knowledge/documents/page"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upload_shouldCreateDocument() throws Exception {
        String token = loginAdmin();
        MockMultipartFile file = new MockMultipartFile("file", "篮球知识.md", "text/markdown",
                "篮球使用后应清洁表面灰尘，存放于干燥阴凉处。".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/knowledge/documents/upload")
                        .file(file)
                        .param("title", "篮球知识")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.title").value("篮球知识"));
    }

    @Test
    void upload_shouldRejectUnsupportedFile() throws Exception {
        String token = loginAdmin();
        MockMultipartFile file = new MockMultipartFile("file", "x.exe", "application/octet-stream",
                new byte[] { 1, 2, 3 });

        mockMvc.perform(multipart("/api/knowledge/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3202));
    }

    @Test
    void page_shouldReturnUploadedDocument() throws Exception {
        String token = loginAdmin();
        MockMultipartFile file = new MockMultipartFile("file", "足球知识.md", "text/markdown",
                "足球使用后擦拭表面泥土与污渍，自然晾干后再收纳。".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/knowledge/documents/upload")
                        .file(file)
                        .param("title", "足球知识")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/knowledge/documents/page")
                        .param("current", "1")
                        .param("size", "10")
                        .param("keyword", "足球")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("足球知识"));
    }

    @Test
    void delete_shouldRemoveDocument() throws Exception {
        String token = loginAdmin();
        MockMultipartFile file = new MockMultipartFile("file", "待删除.md", "text/markdown",
                "待删除的知识内容。".getBytes(StandardCharsets.UTF_8));
        MvcResult result = mockMvc.perform(multipart("/api/knowledge/documents/upload")
                        .file(file)
                        .param("title", "待删除")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long id = JSONUtil.parseObj(result.getResponse().getContentAsString())
                .getByPath("data.id", Long.class);

        mockMvc.perform(delete("/api/knowledge/documents/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void seed_shouldImportBuiltinKnowledge() throws Exception {
        String token = loginAdmin();

        mockMvc.perform(post("/api/knowledge/documents/seed")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.imported").value(org.hamcrest.Matchers.greaterThanOrEqualTo(11)));
    }

    private String loginAdmin() throws Exception {
        redisTemplate.opsForValue().set(
                CacheConstants.CAPTCHA_CODE_KEY + CAPTCHA_UUID, CAPTCHA_CODE, Duration.ofMinutes(5));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "username", "admin",
                                "password", "admin123",
                                "code", CAPTCHA_CODE,
                                "uuid", CAPTCHA_UUID))))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return (String) JSONUtil.parseObj(result.getResponse().getContentAsString())
                .getByPath("data.token");
    }
}
