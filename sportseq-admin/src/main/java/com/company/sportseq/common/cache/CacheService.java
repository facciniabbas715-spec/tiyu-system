package com.company.sportseq.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Cache Aside 缓存服务：
 * 查询 Redis -> 命中直接返回 -> 未命中查 MySQL -> 写入 Redis -> 返回。
 * 读写 Redis 均做异常降级：缓存不可用时不影响主业务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> Optional<T> get(String key, TypeReference<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(objectMapper.readValue(json, type));
        } catch (Exception e) {
            log.warn("读取 Redis 缓存失败, key={}", key, e);
            return Optional.empty();
        }
    }

    /** 缓存未命中时执行 loader 回源数据库，并把结果写回缓存 */
    public <T> T getOrLoad(String key, TypeReference<T> type, Duration ttl, Supplier<T> loader) {
        Optional<T> cached = get(key, type);
        if (cached.isPresent()) {
            return cached.get();
        }
        T value = loader.get();
        if (value != null) {
            put(key, value, ttl);
        }
        return value;
    }

    public <T> void put(String key, T value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            log.warn("写入 Redis 缓存失败, key={}", key, e);
        }
    }

    public void evict(String... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        try {
            redisTemplate.delete(Arrays.asList(keys));
        } catch (Exception e) {
            log.warn("删除 Redis 缓存失败, keys={}", Arrays.toString(keys), e);
        }
    }

    /** 使用 SCAN 按模式删除，避免生产环境 KEYS 阻塞 */
    public void evictByPattern(String pattern) {
        try {
            List<String> keys = new ArrayList<>();
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                cursor.forEachRemaining(keys::add);
            }
            log.debug("evictByPattern pattern={} matchedKeys={}", pattern, keys);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("按模式删除 Redis 缓存失败, pattern={}", pattern, e);
        }
    }

    /** 事务提交成功后执行缓存失效；无事务时立即执行。回滚则不删除，避免误删仍然有效的缓存 */
    public void evictAfterCommit(Runnable eviction) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eviction.run();
                }
            });
        } else {
            eviction.run();
        }
    }
}
