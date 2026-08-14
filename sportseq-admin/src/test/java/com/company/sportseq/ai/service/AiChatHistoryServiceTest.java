package com.company.sportseq.ai.service;

import com.company.sportseq.ai.config.AiChatProperties;
import com.company.sportseq.ai.vo.AiHistoryMessage;
import com.company.sportseq.security.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 多轮对话历史单元测试：加载/追加/裁剪/TTL/清空。
 */
class AiChatHistoryServiceTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> ops = mock(ValueOperations.class);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnEmptyWhenNoHistory() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn(null);
        AiChatHistoryService service = new AiChatHistoryService(redis, new ObjectMapper(), properties());
        login();

        assertTrue(service.load().isEmpty());
    }

    @Test
    void shouldAppendPairAndTrimToMaxMessages() throws Exception {
        AtomicReference<String> stored = new AtomicReference<>();
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenAnswer(invocation -> stored.get());
        doAnswer(invocation -> {
            stored.set(invocation.getArgument(1));
            return null;
        }).when(ops).set(anyString(), anyString(), any(Duration.class));
        AiChatProperties properties = properties();
        AiChatHistoryService service = new AiChatHistoryService(redis, new ObjectMapper(), properties);
        login();

        for (int i = 0; i < 5; i++) {
            service.append("问题" + i, "回答" + i);
        }

        String expected = new ObjectMapper().writeValueAsString(List.of(
                new AiHistoryMessage("user", "问题1"),
                new AiHistoryMessage("assistant", "回答1"),
                new AiHistoryMessage("user", "问题2"),
                new AiHistoryMessage("assistant", "回答2"),
                new AiHistoryMessage("user", "问题3"),
                new AiHistoryMessage("assistant", "回答3"),
                new AiHistoryMessage("user", "问题4"),
                new AiHistoryMessage("assistant", "回答4")));
        verify(ops).set(anyString(), eq(expected), eq(Duration.ofSeconds(1800)));
    }

    @Test
    void shouldClearHistory() {
        when(redis.opsForValue()).thenReturn(ops);
        AiChatHistoryService service = new AiChatHistoryService(redis, new ObjectMapper(), properties());
        login();

        service.clear();

        verify(redis).delete(anyString());
    }

    private AiChatProperties properties() {
        AiChatProperties properties = new AiChatProperties();
        properties.getHistory().setEnabled(true);
        properties.getHistory().setMaxMessages(8);
        properties.getHistory().setTtlSeconds(1800);
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
