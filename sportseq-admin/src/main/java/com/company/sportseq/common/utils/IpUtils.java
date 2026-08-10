package com.company.sportseq.common.utils;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 获取：优先代理头，回退 remoteAddr。
 */
public final class IpUtils {

    private static final String UNKNOWN = "unknown";

    private IpUtils() {
    }

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
