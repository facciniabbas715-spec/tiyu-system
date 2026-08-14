package com.company.sportseq.ai.exception;

import com.company.sportseq.common.exception.BizException;

/**
 * 用户提问触发限流时抛出。
 */
public class AiRateLimitedException extends BizException {

    public static final int CODE = 3105;

    public AiRateLimitedException() {
        super(CODE, "提问过于频繁，请稍后再试");
    }
}
