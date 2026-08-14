package com.company.sportseq.knowledge.vo;

/**
 * 单条检索命中片段（调试信息用）。
 */
public record RagHitVO(
        Long documentId,
        String title,
        Integer chunkIndex,
        String content,
        Double similarity) {
}
