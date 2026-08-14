package com.company.sportseq.ai.vo;

import com.company.sportseq.knowledge.vo.RagDebugVO;

/**
 * AI 对话响应：data 为 { "content": "...", "debug": {...} }。
 * debug 仅在开发环境（spring.ai.rag.debug-enabled=true）返回，生产恒为 null。
 */
public record AiChatVO(String content, RagDebugVO debug) {
}
