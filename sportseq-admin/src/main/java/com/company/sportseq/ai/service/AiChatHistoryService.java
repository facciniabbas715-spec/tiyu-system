package com.company.sportseq.ai.service;

import com.company.sportseq.ai.config.AiChatProperties;
import com.company.sportseq.ai.vo.AiHistoryMessage;
import com.company.sportseq.security.SecurityUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 客服多轮对话历史（Redis，按用户隔离，带 TTL）。
 */
@Slf4j
@Service
public class AiChatHistoryService {

    public static final String KEY_PREFIX = "sportseq:ai:history:";

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final AiChatProperties properties;

    public AiChatHistoryService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                                AiChatProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public List<AiHistoryMessage> load() {
        if (!properties.getHistory().isEnabled()) {
            return List.of();
        }
        String json = redisTemplate.opsForValue().get(key());
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<AiHistoryMessage>>() {
            });
        } catch (Exception e) {
            log.warn("读取 AI 对话历史失败，已忽略", e);
            return List.of();
        }
    }

    public void append(String userMessage, String assistantMessage) {
        if (!properties.getHistory().isEnabled()) {
            return;
        }
        try {
            List<AiHistoryMessage> messages = new ArrayList<>(load());
            messages.add(new AiHistoryMessage("user", userMessage));
            messages.add(new AiHistoryMessage("assistant", assistantMessage));
            int max = properties.getHistory().getMaxMessages();
            if (messages.size() > max) {
                messages = new ArrayList<>(messages.subList(messages.size() - max, messages.size()));
            }
            String json = objectMapper.writeValueAsString(messages);
            redisTemplate.opsForValue().set(key(), json,
                    Duration.ofSeconds(properties.getHistory().getTtlSeconds()));
        } catch (Exception e) {
            log.warn("保存 AI 对话历史失败，已忽略", e);
        }
    }

    public void clear() {
        redisTemplate.delete(key());
    }

    private String key() {
        return KEY_PREFIX + SecurityUtils.getUserId();
    }
}
