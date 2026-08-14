package com.company.sportseq.ai.tool;

import com.company.sportseq.security.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 工具目录单元测试：验证白名单注册、权限声明强校验与执行前统一鉴权。
 */
class AiToolCatalogTest {

    private final AiToolPermissionService permissionService = new AiToolPermissionService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRegisterAnnotatedToolMethodsAsPermissionAwareCallbacks() {
        AiToolCatalog catalog = catalog(true, new StockTool());

        assertEquals(1, catalog.callbacks().size());
        AiToolCatalog.ToolInfo info = catalog.toolInfos().get(0);
        assertEquals("getStock", info.name());
        assertEquals("stock:list", info.permission());
        assertEquals("getStock", catalog.callbacks().get(0).getToolDefinition().name());

        login(Set.of("stock:list"));
        // String 返回值经 Spring AI 默认 JsonToolConverter 编码为 JSON 字符串（带引号），
        // 这正是模型在工具调用结果里收到的内容。
        assertEquals("\"真实库存数据\"", catalog.callbacks().get(0).call("{}"));

        login(Set.of());
        String denied = catalog.callbacks().get(0).call("{}");
        assertTrue(denied.contains("没有"), "无权限时不应执行工具");
        assertTrue(denied.contains("stock:list"), "文案应指明所需权限");
    }

    @Test
    void shouldRejectToolWithoutPermissionAnnotation() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> catalog(true, new UndeclaredTool()));
        assertTrue(exception.getMessage().contains("@AiToolPermission"));
    }

    @Test
    void shouldRejectToolWithoutToolMethod() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> catalog(true, new EmptyTool()));
        assertTrue(exception.getMessage().contains("@Tool"));
    }

    @Test
    void shouldRegisterNothingWhenDisabled() {
        AiToolCatalog catalog = catalog(false, new StockTool());

        assertTrue(catalog.callbacks().isEmpty());
        assertTrue(catalog.toolInfos().isEmpty());
    }

    private AiToolCatalog catalog(boolean enabled, AiTool... tools) {
        AiToolsProperties properties = new AiToolsProperties();
        properties.setEnabled(enabled);
        return new AiToolCatalog(List.of(tools), properties, permissionService);
    }

    private void login(Set<String> permissions) {
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setUsername("tester");
        user.setPermissions(permissions);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AiToolPermission("stock:list")
    public static class StockTool implements AiTool {

        @Tool(description = "查询器材真实库存")
        public String getStock() {
            return "真实库存数据";
        }
    }

    public static class UndeclaredTool implements AiTool {

        @Tool(description = "未声明权限的工具")
        public String undeclared() {
            return "bad";
        }
    }

    @AiToolPermission("stock:list")
    public static class EmptyTool implements AiTool {

        public String notATool() {
            return "bad";
        }
    }
}
