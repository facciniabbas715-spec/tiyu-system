package com.company.sportseq.ai.vo;

import com.company.sportseq.knowledge.vo.RagHitVO;

import java.util.List;

/**
 * AI 对话调试信息（仅开发环境返回，生产恒为 null）：
 * 问题、意图、工具调用链、知识命中片段与最终回答。
 */
public record AiDebugVO(
        String query,
        String intent,
        List<AiToolCallVO> toolCalls,
        boolean knowledgeUsed,
        List<RagHitVO> hits,
        String answer
) {
}
