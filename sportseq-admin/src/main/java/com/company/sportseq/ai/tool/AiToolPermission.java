package com.company.sportseq.ai.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明 AI 工具所需的最小权限标识。
 *
 * <p>值对应 {@code sys_menu.perms}（例如 {@code stock:list}、{@code equipment:list}）；
 * 空串表示登录即可调用（例如“我的借用记录”，与 {@code GET /api/borrow/my} 授权一致）。</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AiToolPermission {

    /**
     * 所需权限标识；空串表示登录即可调用。
     */
    String value() default "";
}
