package com.company.sportseq.ai.exception;

import com.company.sportseq.common.exception.BizException;

/**
 * 上游模型调用失败（网络、限流、鉴权等）时抛出，对用户返回友好提示。
 */
public class AiServiceException extends BizException {

    public static final int CODE = 3102;

    public AiServiceException() {
        super(CODE, "AI 服务暂时不可用，请稍后重试");
    }

    public AiServiceException(int code, String message) {
        super(code, message);
    }
}
