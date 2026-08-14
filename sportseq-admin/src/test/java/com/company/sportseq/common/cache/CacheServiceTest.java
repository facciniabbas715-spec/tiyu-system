package com.company.sportseq.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Redis 故障降级时的日志限流单元测试。
 *
 * <p>Redis 不可用期间业务会自动降级直查 MySQL，但不应因此刷屏 WARN 日志。</p>
 */
@ExtendWith(OutputCaptureExtension.class)
class CacheServiceTest {

    @Test
    void repeatedRedisFailures_shouldEmitSingleWarningPerWindow(CapturedOutput output) {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        when(operations.get(anyString())).thenThrow(new RuntimeException("redis down"));
        CacheService cacheService = new CacheService(redisTemplate, new ObjectMapper());

        for (int i = 0; i < 50; i++) {
            cacheService.get("sportseq:test:key", new TypeReference<>() {
            });
        }

        long warnLines = output.getOut().lines()
                .filter(line -> line.contains("WARN") && line.contains("CacheService"))
                .count();
        assertEquals(1, warnLines, "同一限流窗口内的重复故障只应输出一条 WARN 日志");
    }
}
