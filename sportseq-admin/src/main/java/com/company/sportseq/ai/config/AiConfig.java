package com.company.sportseq.ai.config;

import com.company.sportseq.ai.exception.AiNotConfiguredException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * AI 模块装配：仅当环境变量 AI_API_KEY 存在时构建真实模型，
 * 否则提供降级模型，保证应用与全量测试在未配置 Key 时也能正常启动。
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    @Bean
    @Conditional(ApiKeyConfiguredCondition.class)
    public ChatModel openAiChatModel(AiProperties properties) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(new SimpleApiKey(properties.getApiKey()))
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(properties.getChat().getOptions().getModel())
                .temperature(properties.getChat().getOptions().getTemperature())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel disabledChatModel() {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                throw new AiNotConfiguredException();
            }
        };
    }

    @Bean
    public ChatClient aiChatClient(ChatModel chatModel, AiProperties properties) {
        return ChatClient.builder(chatModel)
                .defaultSystem(properties.getChat().getSystemPrompt())
                .build();
    }

    /**
     * API Key 必须为非空字符串才算已配置（空字符串视为未配置）。
     */
    public static class ApiKeyConfiguredCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String apiKey = context.getEnvironment().getProperty("spring.ai.openai.api-key", "");
            return StringUtils.hasText(apiKey);
        }
    }
}
