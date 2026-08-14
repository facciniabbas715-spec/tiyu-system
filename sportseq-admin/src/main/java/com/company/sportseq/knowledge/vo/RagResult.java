package com.company.sportseq.knowledge.vo;

/**
 * RAG 回答结果：debug 仅在开发环境（debug-enabled=true）返回，生产恒为 null。
 */
public record RagResult(String content, RagDebugVO debug) {
}
