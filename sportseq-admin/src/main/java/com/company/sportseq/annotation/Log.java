package com.company.sportseq.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标注在 Controller 方法上，由 LogAspect 自动记录。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {

    /** 模块标题 */
    String title() default "";

    /** 业务类型：0其它 1新增 2修改 3删除 4审核 5导出 6导入 7领用 8归还 */
    int businessType() default 0;
}
