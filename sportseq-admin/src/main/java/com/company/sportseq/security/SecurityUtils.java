package com.company.sportseq.security;

import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具类。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BizException(ErrorCode.LOGIN_EXPIRED);
        }
        return loginUser;
    }

    public static Long getUserId() {
        return getLoginUser().getUserId();
    }
}
