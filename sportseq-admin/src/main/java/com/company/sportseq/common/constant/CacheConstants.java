package com.company.sportseq.common.constant;

/**
 * Redis 缓存键与过期时间常量。
 */
public final class CacheConstants {

    /** 登录会话键前缀：sportseq:login:token:{jti} -> LoginUser JSON */
    public static final String LOGIN_TOKEN_KEY = "sportseq:login:token:";

    /** 用户会话索引（Set 存 jti）：sportseq:login:user:{userId}，用于停用/踢出 */
    public static final String LOGIN_USER_KEY = "sportseq:login:user:";

    /** 图形验证码键前缀：sportseq:captcha:{uuid} -> 验证码文本 */
    public static final String CAPTCHA_CODE_KEY = "sportseq:captcha:";

    /** 验证码有效期（分钟） */
    public static final long CAPTCHA_EXPIRATION_MINUTES = 5;

    /** 器材编码流水键前缀：sportseq:equipment:code:{分类码}:{年} */
    public static final String EQUIPMENT_CODE_KEY = "sportseq:equipment:code:";

    private CacheConstants() {
    }
}
