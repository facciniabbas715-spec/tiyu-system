package com.company.sportseq.ai.exception;

import com.company.sportseq.common.exception.BizException;

/**
 * AI 服务未配置（缺少环境变量 AI_API_KEY）时抛出，避免返回 500。
 */
public class AiNotConfiguredException extends BizException {

    public static final int CODE = 3101;

    public AiNotConfiguredException() {
        super(CODE, "AI 客服尚未配置：请设置环境变量 AI_API_KEY 后重启服务");
    }
}
