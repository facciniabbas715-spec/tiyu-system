package com.company.sportseq.ai.tool;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * 权限包装回调：在任何 {@code @Tool} 方法执行前，统一校验当前登录用户是否持有
 * {@link AiToolPermission} 声明的权限。
 *
 * <p>未通过时不执行工具、不抛异常，把无权限文案作为工具结果回传给模型，
 * 让模型据此礼貌回复，同时不向模型暴露任何业务数据。</p>
 */
public class PermissionAwareToolCallback implements ToolCallback {

    private final ToolCallback delegate;

    private final String permission;

    private final AiToolPermissionService permissionService;

    public PermissionAwareToolCallback(ToolCallback delegate, String permission,
                                       AiToolPermissionService permissionService) {
        this.delegate = delegate;
        this.permission = permission;
        this.permissionService = permissionService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        String denyMessage = permissionService.denyMessage(permission);
        return denyMessage != null ? denyMessage : delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        String denyMessage = permissionService.denyMessage(permission);
        return denyMessage != null ? denyMessage : delegate.call(toolInput, toolContext);
    }
}
