package com.company.sportseq.ai;

import com.company.sportseq.ai.config.AiConfig;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.api.OpenAiApi;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * OpenAI 兼容网关路径拼接测试：带 /v1 的 baseUrl 不能与 Spring AI 默认路径叠加成双 /v1。
 */
class AiConfigTest {

    @Test
    void buildApi_shouldKeepDefaultPathsForPlainBaseUrl() {
        OpenAiApi api = AiConfig.buildOpenAiApi("https://api.deepseek.com", "sk-test");

        assertEquals("https://api.deepseek.com", field(api, "baseUrl"));
        assertEquals("/v1/chat/completions", field(api, "completionsPath"));
        assertEquals("/v1/embeddings", field(api, "embeddingsPath"));
    }

    @Test
    void buildApi_shouldDropVersionPrefixForCompatibleModeGateway() {
        OpenAiApi api = AiConfig.buildOpenAiApi(
                "https://dashscope.aliyuncs.com/compatible-mode/v1", "sk-test");

        assertEquals("/chat/completions", field(api, "completionsPath"));
        assertEquals("/embeddings", field(api, "embeddingsPath"));
    }

    @Test
    void buildApi_shouldNormalizeTrailingSlash() {
        OpenAiApi api = AiConfig.buildOpenAiApi(
                "https://dashscope.aliyuncs.com/compatible-mode/v1/", "sk-test");

        assertEquals("https://dashscope.aliyuncs.com/compatible-mode/v1", field(api, "baseUrl"));
        assertEquals("/embeddings", field(api, "embeddingsPath"));
    }

    private Object field(OpenAiApi api, String name) {
        try {
            Field field = OpenAiApi.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(api);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
