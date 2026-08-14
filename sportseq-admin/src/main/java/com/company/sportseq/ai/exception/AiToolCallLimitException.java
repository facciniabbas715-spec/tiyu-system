package com.company.sportseq.ai.exception;

import com.company.sportseq.common.exception.BizException;

/**
 * 单次对话的工具调用轮数超过上限时抛出，强制终止循环。
 */
public class AiToolCallLimitException extends BizException {

    public static final int CODE = 3106;

    public AiToolCallLimitException() {
        super(CODE, "问题涉及的工具调用次数过多，请简化问题后重试");
    }
}
