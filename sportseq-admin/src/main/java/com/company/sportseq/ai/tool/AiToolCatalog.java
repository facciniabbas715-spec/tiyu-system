package com.company.sportseq.ai.tool;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 工具目录（对话层唯一对外出口）：
 *
 * <ol>
 *   <li>收集全部 {@link AiTool} 实现；</li>
 *   <li>启动期校验：每个工具必须声明 {@link AiToolPermission}，且至少定义一个
 *       {@code org.springframework.ai.tool.annotation.Tool} 方法；</li>
 *   <li>按 {@link AiToolsProperties#isEnabled()} 总开关过滤；</li>
 *   <li>把每个工具方法包装成 {@link PermissionAwareToolCallback}，保证执行前统一鉴权。</li>
 * </ol>
 *
 * <p>对话编排层只能使用本目录导出的回调，因此模型只可能调用白名单内的只读业务工具，
 * 不可能直接执行 SQL 或修改数据库。</p>
 */
@Component
public class AiToolCatalog {

    private final List<ToolCallback> callbacks;

    private final List<ToolInfo> toolInfos;

    public AiToolCatalog(List<AiTool> tools, AiToolsProperties properties,
                         AiToolPermissionService permissionService) {
        if (!properties.isEnabled()) {
            this.callbacks = List.of();
            this.toolInfos = List.of();
            return;
        }
        List<ToolCallback> registered = new ArrayList<>();
        List<ToolInfo> infos = new ArrayList<>();
        for (AiTool tool : tools) {
            AiToolPermission permission = tool.getClass().getAnnotation(AiToolPermission.class);
            if (permission == null) {
                throw new IllegalStateException("AI 工具 " + tool.getClass().getName()
                        + " 必须使用 @AiToolPermission 声明所需权限");
            }
            MethodToolCallbackProvider provider = MethodToolCallbackProvider.builder()
                    .toolObjects(tool)
                    .build();
            ToolCallback[] toolCallbacks = provider.getToolCallbacks();
            if (toolCallbacks.length == 0) {
                throw new IllegalStateException("AI 工具 " + tool.getClass().getName()
                        + " 未定义任何 @Tool 方法");
            }
            for (ToolCallback callback : toolCallbacks) {
                registered.add(new PermissionAwareToolCallback(callback, permission.value(), permissionService));
                infos.add(new ToolInfo(callback.getToolDefinition().name(),
                        callback.getToolDefinition().description(), permission.value()));
            }
        }
        this.callbacks = List.copyOf(registered);
        this.toolInfos = List.copyOf(infos);
    }

    /**
     * 已注册、已包装权限校验的工具回调（供对话编排层使用）。
     */
    public List<ToolCallback> callbacks() {
        return callbacks;
    }

    /**
     * 工具元信息（名称 / 说明 / 所需权限），用于调试与审计。
     */
    public List<ToolInfo> toolInfos() {
        return toolInfos;
    }

    /**
     * 工具元信息。
     *
     * @param name        工具名称（对应 {@code @Tool} 方法名）
     * @param description 工具说明（喂给模型帮助其选择工具）
     * @param permission  所需权限标识；空串表示登录即可调用
     */
    public record ToolInfo(String name, String description, String permission) {
    }
}
