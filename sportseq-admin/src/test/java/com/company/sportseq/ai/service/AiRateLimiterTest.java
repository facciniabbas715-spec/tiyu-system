package com.company.sportseq.ai.service;

import com.company.sportseq.ai.config.AiRateLimitProperties;
import com.company.sportseq.ai.exception.AiRateLimitedException;
import com.company.sportseq.security.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * AI 提问限流器单元测试：固定窗口计数，超过阈值拒绝，开关关闭时放行。
 */
class AiRateLimiterTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> ops = mock(ValueOperations.class);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAllowUpToLimitAndRejectAfter() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(1L, 2L, 3L);
        AiRateLimiter limiter = new AiRateLimiter(redis, properties(true, 2));
        login();

        assertDoesNotThrow(limiter::check);
        assertDoesNotThrow(limiter::check);
        assertThrows(AiRateLimitedException.class, limiter::check);

        verify(redis, times(1)).expire(anyString(), any(Duration.class));
    }

    @Test
    void shouldSkipWhenDisabled() {
        AiRateLimiter limiter = new AiRateLimiter(redis, properties(false, 1));
        login();

        assertDoesNotThrow(limiter::check);

        verifyNoInteractions(redis);
    }

    private AiRateLimitProperties properties(boolean enabled, int maxRequestsPerMinute) {
        AiRateLimitProperties properties = new AiRateLimitProperties();
        properties.setEnabled(enabled);
        properties.setMaxRequestsPerMinute(maxRequestsPerMinute);
        return properties;
    }

    private void login() {
        LoginUser user = new LoginUser();
        user.setUserId(42L);
        user.setUsername("tester");
        user.setPermissions(Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
