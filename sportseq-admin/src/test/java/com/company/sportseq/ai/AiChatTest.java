package com.company.sportseq.ai;

import cn.hutool.json.JSONUtil;
import com.company.sportseq.common.constant.CacheConstants;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 智能客服集成测试：使用桩 ChatModel 验证接口契约、认证、参数校验与消息拼装。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AiChatTest {

    private static final String CAPTCHA_UUID = "ai-chat-uuid";
    private static final String CAPTCHA_CODE = "1234";
    private static final String AI_REPLY = "篮球可以通过“借用归还-借用单”申请借用，审核通过后到器材室领用。";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private ChatModel chatModel;

    @Test
    void chat_shouldReturnAiContent() throws Exception {
        stubModelReply();
        String token = loginAdmin();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of("message", "篮球怎么借？"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").value(AI_REPLY));
    }

    @Test
    void chat_shouldSendSystemPromptAndUserMessage() throws Exception {
        stubModelReply();
        String token = loginAdmin();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of("message", "篮球怎么借？"))))
                .andExpect(jsonPath("$.code").value(0));

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        List<Message> messages = captor.getValue().getInstructions();
        assertEquals(2, messages.size());
        assertEquals(MessageType.SYSTEM, messages.get(0).getMessageType());
        assertTrue(messages.get(0).getText().contains("体育器材智能客服"),
                "系统提示词应告知模型客服身份");
        assertEquals("篮球怎么借？", messages.get(1).getText());
    }

    @Test
    void chat_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of("message", "篮球怎么借？"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void chat_shouldRejectBlankMessage() throws Exception {
        String token = loginAdmin();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of("message", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    private void stubModelReply() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(AI_REPLY)))));
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
