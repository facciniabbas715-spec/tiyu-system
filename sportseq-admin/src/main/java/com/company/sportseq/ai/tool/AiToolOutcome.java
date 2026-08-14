package com.company.sportseq.ai.tool;

/**
 * AI 工具统一返回结构。
 *
 * <p>{@code ok=false} 时 {@code message} 是面向用户的固定文案（无数据 / 无权限 / 失败），
 * 模型必须原样转述、不得补充编造；{@code ok=true} 时 {@code data} 是工具返回的真实业务数据。</p>
 */
public record AiToolOutcome(boolean ok, String message, Object data) {

    public static AiToolOutcome success(Object data) {
        return new AiToolOutcome(true, null, data);
    }

    public static AiToolOutcome failure(String message) {
        return new AiToolOutcome(false, message, null);
    }
}
