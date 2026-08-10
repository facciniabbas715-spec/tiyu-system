package com.company.sportseq.common.result;

import java.io.Serializable;

/**
 * 统一响应结构：code=0 表示成功；非 0 为业务/系统错误码。
 */
public record Result<T>(int code, String message, T data, String traceId) implements Serializable {

    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data, null);
    }

    public static <T> Result<T> success() {
        return new Result<>(0, "success", null, null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null, null);
    }
}
