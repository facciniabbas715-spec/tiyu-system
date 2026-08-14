package com.company.sportseq.ai.vo;

/**
 * 单次工具调用调试信息：工具名、入参与回传给模型的结果。
 */
public record AiToolCallVO(String name, String arguments, String result) {
}
