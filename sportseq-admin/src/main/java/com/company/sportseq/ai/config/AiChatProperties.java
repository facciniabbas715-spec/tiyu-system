package com.company.sportseq.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 客服对话配置，绑定 {@code spring.ai.chat.*}。
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.chat")
public class AiChatProperties {

    private History history = new History();

    @Data
    public static class History {

        /**
         * 是否启用多轮对话记忆（Redis，按用户隔离）。
         */
        private boolean enabled = true;

        /**
         * 保留的最大消息条数（一问一答算两条）。
         */
        private int maxMessages = 8;

        /**
         * 历史消息过期时间（秒）。
         */
        private int ttlSeconds = 1800;
    }
}
