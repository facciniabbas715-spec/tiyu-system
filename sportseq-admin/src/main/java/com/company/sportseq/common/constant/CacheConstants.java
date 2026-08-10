package com.company.sportseq.common.constant;

/**
 * Redis 缓存键与过期时间常量。
 */
public final class CacheConstants {

    /** 登录会话键前缀：sportseq:login:token:{jti} -> LoginUser JSON */
    public static final String LOGIN_TOKEN_KEY = "sportseq:login:token:";

    /** 图形验证码键前缀：sportseq:captcha:{uuid} -> 验证码文本 */
    public static final String CAPTCHA_CODE_KEY = "sportseq:captcha:";

    /** 验证码有效期（分钟） */
    public static final long CAPTCHA_EXPIRATION_MINUTES = 5;

    private CacheConstants() {
    }
}
