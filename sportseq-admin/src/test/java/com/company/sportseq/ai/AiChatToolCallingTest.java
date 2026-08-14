package com.company.sportseq.ai;

import cn.hutool.json.JSONUtil;
import com.company.sportseq.common.constant.CacheConstants;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.service.StockService;
import com.company.sportseq.vo.StockVO;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 客服工具调用集成测试：验证「业务问题→业务工具」「知识问题→知识工具」的完整循环，
 * 以及无工具调用时的 CHAT 意图与调试信息。
 */
@SpringBootTest(properties = {
        "spring.ai.rag.enabled=true",
        "spring.ai.rag.embedding.api-key=test-embedding-key",
        "spring.ai.rag.debug-enabled=true",
        "spring.ai.rate-limit.enabled=false"
})
@AutoConfigureMockMvc
class AiChatToolCallingTest {

    private static final String CAPTCHA_UUID = "ai-tool-uuid";
    private static final String CAPTCHA_CODE = "1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private ChatModel chatModel;

    @MockitoBean
    private StockService stockService;

    @MockitoBean
    private VectorStore vectorStore;

    @org.junit.jupiter.api.BeforeEach
    void clearChatHistory() {
        redisTemplate.delete("sportseq:ai:history:1");
    }

    @org.junit.jupiter.api.AfterEach
    void cleanupChatHistory() {
        redisTemplate.delete("sportseq:ai:history:1");
    }

    @Test
    void businessQuestion_shouldCallStockToolAndReturnRealData() throws Exception {
        List<StockVO> rows = List.of(new StockVO(7L, "BALL-2026-000001", "篮球", "球类",
                "个", 1L, "一号仓库", 15, 3, 5, false));
        when(stockService.page(1, 100, "篮球", null, null, null, false))
                .thenReturn(new PageResult<>(1, 1, 100, rows));
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(toolCallResponse("call-1", "getEquipmentStock", "{\"equipmentName\":\"篮球\"}"))
                .thenReturn(textResponse("篮球目前可用 12 个。"));
        String token = loginAdmin();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of("message", "现在篮球还有多少个？"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").value("篮球目前可用 12 个。"))
                .andExpect(jsonPath("$.data.debug.intent").value("BUSINESS"))
                .andExpect(jsonPath("$.data.debug.toolCalls[0].name").value("getEquipmentStock"))
                .andExpect(jsonPath("$.data.debug.toolCalls[0].result")
                        .value(Matchers.containsString("\"ok\":true")))
                .andExpect(jsonPath("$.data.debug.knowledgeUsed").value(false));

        verify(chatModel, times(2)).call(any(Prompt.class));
        verify(stockService).page(1, 100, "篮球", null, null, null, false);
    }

    @Test
    void knowledgeQuestion_shouldCallKnowledgeToolAndReportHits() throws Exception {
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
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(toolCallResponse("call-2", "searchKnowledge", "{\"query\":\"篮球怎么保养\"}"))
                .thenReturn(textResponse("篮球使用后应清洁表面并存放于干燥阴凉处。"));
        String token = loginAdmin();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of("message", "篮球怎么保养？"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").value("篮球使用后应清洁表面并存放于干燥阴凉处。"))
                .andExpect(jsonPath("$.data.debug.intent").value("KNOWLEDGE"))
                .andExpect(jsonPath("$.data.debug.toolCalls[0].name").value("searchKnowledge"))
                .andExpect(jsonPath("$.data.debug.knowledgeUsed").value(true))
                .andExpect(jsonPath("$.data.debug.hits[0].title").value("篮球相关知识"))
                .andExpect(jsonPath("$.data.debug.hits[0].similarity").value(0.83));

        verify(chatModel, times(2)).call(any(Prompt.class));
    }

    @Test
    void plainChat_shouldReportChatIntentWithoutTools() throws Exception {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(textResponse("你好，请问有什么可以帮你？"));
        String token = loginAdmin();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of("message", "你好"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").value("你好，请问有什么可以帮你？"))
                .andExpect(jsonPath("$.data.debug.intent").value("CHAT"))
                .andExpect(jsonPath("$.data.debug.toolCalls").isEmpty())
                .andExpect(jsonPath("$.data.debug.knowledgeUsed").value(false));

        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void chat_shouldKeepMultiTurnContextAndExposeHistoryApi() throws Exception {
        String token = loginAdmin();
        mockMvc.perform(delete("/api/ai/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(textResponse("默认最长借用天数是 15 天。"))
                .thenReturn(textResponse("续借次数上限是 1 次。"));

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of("message", "借用规则是什么？"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("默认最长借用天数是 15 天。"));

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of("message", "那续借上限呢？"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("续借次数上限是 1 次。"));

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, times(2)).call(captor.capture());
        List<Message> second = captor.getAllValues().get(1).getInstructions();
        assertTrue(second.stream().anyMatch(m -> "借用规则是什么？".equals(m.getText())),
                "第二轮提示词应包含上一轮用户问题");
        assertTrue(second.stream().anyMatch(m -> "默认最长借用天数是 15 天。".equals(m.getText())),
                "第二轮提示词应包含上一轮助手回答");

        mockMvc.perform(get("/api/ai/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].role").value("user"))
                .andExpect(jsonPath("$.data[0].content").value("借用规则是什么？"))
                .andExpect(jsonPath("$.data[1].role").value("assistant"))
                .andExpect(jsonPath("$.data[1].content").value("默认最长借用天数是 15 天。"));

        mockMvc.perform(delete("/api/ai/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void chat_shouldMapUpstreamBusyError() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RestClientResponseException(
                "busy", HttpStatusCode.valueOf(429), "Too Many Requests", null, new byte[0], null));
        String token = loginAdmin();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of("message", "篮球怎么保养？"))))
                .andExpect(jsonPath("$.code").value(3103));
    }

    @Test
    void chat_shouldMapUpstreamTimeoutError() throws Exception {
        when(chatModel.call(any(Prompt.class)))
                .thenThrow(new ResourceAccessException("connect timed out"));
        String token = loginAdmin();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of("message", "篮球怎么保养？"))))
                .andExpect(jsonPath("$.code").value(3104));
    }

    private ChatResponse toolCallResponse(String id, String name, String arguments) {
        AssistantMessage message = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, arguments)))
                .build();
        return new ChatResponse(List.of(new Generation(message)));
    }

    private ChatResponse textResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
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
