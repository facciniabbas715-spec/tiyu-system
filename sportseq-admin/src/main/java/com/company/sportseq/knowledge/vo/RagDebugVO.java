package com.company.sportseq.knowledge.vo;

import java.util.List;

/**
 * RAG 调试信息：仅当 spring.ai.rag.debug-enabled=true（开发环境）时返回，生产环境恒为 null。
 */
public record RagDebugVO(
        String query,
        boolean knowledgeUsed,
        int topK,
        double similarityThreshold,
        List<RagHitVO> hits,
        String answer) {
}
