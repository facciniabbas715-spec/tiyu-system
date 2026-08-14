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
         * 客服人设与边界约束。仅用于工具与知识库均不可用时的回退路径；
         * 工具就绪时使用 {@code AiChatPrompt.TOOL_SYSTEM_PROMPT}。
         */
        private String systemPrompt = "你是“体育器材智能客服”，隶属于体育器材管理系统。"
                + "当前业务工具与知识库均不可用，请仅基于通用知识回答；"
                + "使用简体中文，语气友好、专业、简洁，不要编造本系统的库存数量、价格、借用人、单据号等具体数据，"
                + "涉及系统内具体数据或操作时请引导用户到系统对应页面查询或办理；"
                + "输出纯文本，不要使用 Markdown 等排版语法。";

        private Options options = new Options();
    }

    @Data
    public static class Options {

        private String model = "gpt-4o-mini";

        private Double temperature = 0.7;
    }
}
