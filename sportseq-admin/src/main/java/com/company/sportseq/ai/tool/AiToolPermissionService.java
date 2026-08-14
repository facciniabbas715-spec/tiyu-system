package com.company.sportseq.ai.tool;

import com.company.sportseq.security.LoginUser;
import com.company.sportseq.security.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * AI 工具权限校验服务：以当前登录用户 {@link LoginUser#getPermissions()} 为唯一判定依据。
 *
 * <p>工具运行在 HTTP 请求线程内，SecurityContext 可用；未登录或权限不足时返回固定文案，
 * 由 {@link PermissionAwareToolCallback} 把文案作为工具结果回传给模型，而不是抛出异常。</p>
 */
@Service
public class AiToolPermissionService {

    /** 未登录 / 会话失效时回传给模型的固定文案。 */
    public static final String SESSION_EXPIRED_REPLY = "登录状态已失效，请重新登录后再提问。";

    /**
     * 当前请求是否存在已认证用户。
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof LoginUser;
    }

    /**
     * 是否持有指定权限；空权限视为“登录即可调用”。
     */
    public boolean hasPermission(String permission) {
        if (!isAuthenticated()) {
            return false;
        }
        if (!StringUtils.hasText(permission)) {
            return true;
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return loginUser.getPermissions() != null && loginUser.getPermissions().contains(permission);
    }

    /**
     * 鉴权结果：通过返回 {@code null}；未登录或无权限时返回回传给模型的固定文案。
     */
    public String denyMessage(String permission) {
        if (!isAuthenticated()) {
            return SESSION_EXPIRED_REPLY;
        }
        if (hasPermission(permission)) {
            return null;
        }
        if (StringUtils.hasText(permission)) {
            return "当前账号没有查询该业务数据的权限（需要权限标识：" + permission
                    + "），请向管理员申请权限，或到系统对应页面查询。";
        }
        return "当前账号无权执行该查询，请联系管理员。";
    }
}
