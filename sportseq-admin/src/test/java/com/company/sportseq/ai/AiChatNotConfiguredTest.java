package com.company.sportseq.ai;

import cn.hutool.json.JSONUtil;
import com.company.sportseq.common.constant.CacheConstants;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 未配置 AI_API_KEY 时的降级行为：应用正常启动，对话接口返回友好提示而非 500。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AiChatNotConfiguredTest {

    private static final String CAPTCHA_UUID = "ai-chat-noconfig-uuid";
    private static final String CAPTCHA_CODE = "1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void chat_shouldReturnFriendlyErrorWhenApiKeyMissing() throws Exception {
        String token = loginAdmin();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of("message", "篮球怎么借？"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3101))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("AI_API_KEY")));
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
