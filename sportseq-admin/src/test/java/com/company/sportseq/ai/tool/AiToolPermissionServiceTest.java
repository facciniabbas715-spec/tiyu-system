package com.company.sportseq.ai.tool;

import com.company.sportseq.security.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 工具权限校验服务单元测试：登录态与权限集合是唯一判定依据。
 */
class AiToolPermissionServiceTest {

    private final AiToolPermissionService permissionService = new AiToolPermissionService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAllowAuthenticatedUserWhenToolRequiresNoPermission() {
        login("普通用户", Set.of());

        assertTrue(permissionService.hasPermission(""));
        assertNull(permissionService.denyMessage(""));
    }

    @Test
    void shouldAllowUserHoldingRequiredPermission() {
        login("库管", Set.of("stock:list"));

        assertTrue(permissionService.hasPermission("stock:list"));
        assertNull(permissionService.denyMessage("stock:list"));
    }

    @Test
    void shouldDenyUserMissingRequiredPermission() {
        login("普通用户", Set.of());

        assertFalse(permissionService.hasPermission("stock:list"));
        String message = permissionService.denyMessage("stock:list");
        assertTrue(message.contains("stock:list"), "文案应指明所需权限标识");
        assertTrue(message.contains("没有"), "文案应明确无权限");
    }

    @Test
    void shouldDenyWhenNotAuthenticated() {
        assertFalse(permissionService.isAuthenticated());
        assertFalse(permissionService.hasPermission(""));
        assertEquals(AiToolPermissionService.SESSION_EXPIRED_REPLY, permissionService.denyMessage(""));
    }

    private void login(String username, Set<String> permissions) {
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setUsername(username);
        user.setPermissions(permissions);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
