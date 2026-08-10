package com.company.sportseq.common.exception;

import lombok.Getter;

/**
 * 业务错误码：
 * 0 成功；1xxx 参数；2xxx 认证授权；3xxx 业务规则；5xxx 系统异常。
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),

    PARAM_ERROR(1001, "参数校验失败"),

    UNAUTHORIZED(2001, "账号或密码错误"),
    CAPTCHA_ERROR(2002, "验证码错误"),
    LOGIN_EXPIRED(2003, "登录已失效，请重新登录"),
    ACCOUNT_LOCKED(2004, "账号已锁定，请稍后再试"),
    ACCOUNT_DISABLED(2005, "账号已停用，请联系管理员"),

    STOCK_NOT_ENOUGH(3001, "库存不足"),
    ORDER_STATUS_ERROR(3002, "当前单据状态不允许该操作"),
    CATEGORY_HAS_CHILDREN(3003, "存在子分类，无法删除"),
    EQUIPMENT_IN_USE(3004, "器材存在未完结业务，无法删除"),

    SYSTEM_ERROR(5000, "系统异常，请稍后重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
