package com.company.sportseq.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 客服配置，绑定 {@code spring.ai.openai.*}。
 * API Key 只从环境变量 AI_API_KEY 读取（application.yml 中为 ${AI_API_KEY:}），禁止写死在代码或配置中。
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.openai")
public class AiProperties {

    /**
     * OpenAI 兼容服务的 API Key（必须通过环境变量提供）。
     */
    private String apiKey;

    /**
     * OpenAI 兼容服务的接口地址，可切换 DeepSeek / DashScope 等服务商。
     */
    private String baseUrl = "https://api.openai.com";

    private Chat chat = new Chat();

    @Data
    public static class Chat {

        /**
         * 客服人设与边界约束（第一阶段不接知识库，避免模型编造系统内部数据）。
         */
        private String systemPrompt = "你是“体育器材智能客服”，隶属于体育器材管理系统。"
                + "请用简体中文，语气友好、专业、简洁地回答问题；"
                + "当前未接入系统内部知识库，请基于通用知识回答，不要编造具体库存、价格等数据。";

        private Options options = new Options();
    }

    @Data
    public static class Options {

        private String model = "gpt-4o-mini";

        private Double temperature = 0.7;
    }
}
