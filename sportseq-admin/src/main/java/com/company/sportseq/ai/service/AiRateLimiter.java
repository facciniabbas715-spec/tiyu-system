package com.company.sportseq.ai.service;

import com.company.sportseq.ai.config.AiRateLimitProperties;
import com.company.sportseq.ai.exception.AiRateLimitedException;
import com.company.sportseq.security.SecurityUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * AI 客服提问限流：按用户 + 分钟桶做固定窗口计数（Redis INCR），
 * 防止模型调用被刷量导致费用失控。
 */
@Service
public class AiRateLimiter {

    public static final String KEY_PREFIX = "sportseq:ai:ratelimit:";

    private final StringRedisTemplate redisTemplate;

    private final AiRateLimitProperties properties;

    public AiRateLimiter(StringRedisTemplate redisTemplate, AiRateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void check() {
        if (!properties.isEnabled()) {
            return;
        }
        Long userId = SecurityUtils.getUserId();
        long bucket = Instant.now().getEpochSecond() / 60;
        String key = KEY_PREFIX + userId + ":" + bucket;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(2));
        }
        if (count != null && count > properties.getMaxRequestsPerMinute()) {
            throw new AiRateLimitedException();
        }
    }
}
