package com.company.sportseq.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 客服提问限流配置，绑定 {@code spring.ai.rate-limit.*}。
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.rate-limit")
public class AiRateLimitProperties {

    /**
     * 是否启用用户级提问限流。
     */
    private boolean enabled = true;

    /**
     * 每个用户每分钟允许的最大提问次数。
     */
    private int maxRequestsPerMinute = 20;
}
