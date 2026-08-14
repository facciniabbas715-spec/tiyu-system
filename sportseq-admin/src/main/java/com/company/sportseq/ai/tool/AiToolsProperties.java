package com.company.sportseq.ai.tool;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 工具调用配置，绑定 {@code spring.ai.tools.*}。
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.tools")
public class AiToolsProperties {

    /**
     * 工具调用总开关。关闭时 AI 客服不注册任何业务工具，
     * 自动退回原有 RAG / 纯对话路径。
     */
    private boolean enabled = true;
}
