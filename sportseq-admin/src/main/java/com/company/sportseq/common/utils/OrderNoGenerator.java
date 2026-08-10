package com.company.sportseq.common.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 单据号生成：{前缀}{yyyyMMdd}{6位当日流水}，Redis INCR 保证同日不重复。
 */
public final class OrderNoGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private OrderNoGenerator() {
    }

    public static String generate(String prefix, StringRedisTemplate redisTemplate) {
        String date = LocalDate.now().format(DATE_FORMAT);
        Long seq = redisTemplate.opsForValue().increment("sportseq:order:" + prefix + ":" + date);
        return prefix + date + String.format("%06d", seq);
    }
}
