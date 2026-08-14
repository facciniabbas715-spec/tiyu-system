package com.company.sportseq.ai.tool;

/**
 * AI 业务工具标记接口。
 *
 * <p>实现约束（受 {@link AiToolCatalog} 启动期强校验，并由代码评审保证）：</p>
 * <ol>
 *   <li>类上必须声明 {@link AiToolPermission}，空串表示登录即可调用；</li>
 *   <li>只能调用业务 Service 层的只读方法，禁止依赖 Mapper、禁止任何写库操作；</li>
 *   <li>涉及“当前用户”的数据必须从 {@code SecurityUtils.getUserId()} 在服务端取得，
 *       不得把 userId 设计为模型可传的参数。</li>
 * </ol>
 */
public interface AiTool {
}
