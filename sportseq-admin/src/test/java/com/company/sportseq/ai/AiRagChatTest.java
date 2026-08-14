package com.company.sportseq.ai;

import cn.hutool.json.JSONUtil;
import com.company.sportseq.common.constant.CacheConstants;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 客服 RAG 路径集成测试：命中时接地回答并返回调试信息；无命中时不调用模型返回固定文案。
 */
@SpringBootTest(properties = {
        "spring.ai.rag.enabled=true",
        "spring.ai.rag.debug-enabled=true",
        "spring.ai.rag.embedding.api-key=test-embedding-key"
})
@AutoConfigureMockMvc
class AiRagChatTest {

    private static final String CAPTCHA_UUID = "ai-rag-uuid";
    private static final String CAPTCHA_CODE = "1234";
    private static final String ANSWER = "篮球应清洁表面并存放于干燥阴凉处，避免暴晒与尖锐物划伤。";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private ChatModel chatModel;

    @MockitoBean
    private VectorStore vectorStore;

    @Test
    void chat_shouldGroundAnswerOnRetrievedChunksAndReturnDebug() throws Exception {
        Document hit = Document.builder()
                .id("11")
                .text("篮球使用后应清洁表面灰尘，存放于干燥阴凉处，避免暴晒与尖锐物划伤。")
                .metadata(Map.of(
                        "document_id", "5",
                        "chunk_index", "0",
                        "title", "篮球相关知识",
                        "file_type", "md"))
                .score(0.83)
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(hit));
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(
                List.of(new Generation(new AssistantMessage(ANSWER)))));
        String token = loginAdmin();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of("message", "篮球应该怎么保养？"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").value(ANSWER))
                .andExpect(jsonPath("$.data.debug.query").value("篮球应该怎么保养？"))
                .andExpect(jsonPath("$.data.debug.knowledgeUsed").value(true))
                .andExpect(jsonPath("$.data.debug.hits[0].title").value("篮球相关知识"))
                .andExpect(jsonPath("$.data.debug.hits[0].similarity").value(0.83))
                .andExpect(jsonPath("$.data.debug.answer").value(ANSWER));
    }

    @Test
    void chat_shouldReturnCannedMessageWhenNoHitWithoutCallingModel() throws Exception {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        String token = loginAdmin();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of("message", "篮球应该怎么保养？"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").value("知识库中暂无相关信息。"))
                .andExpect(jsonPath("$.data.debug.knowledgeUsed").value(false));

        verify(chatModel, never()).call(any(Prompt.class));
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
