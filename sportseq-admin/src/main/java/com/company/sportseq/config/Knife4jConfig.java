package com.company.sportseq.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 3 接口文档配置。
 * 访问地址：http://localhost:8080/doc.html
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "体育器材管理系统 API",
        version = "1.0.0",
        description = "器材档案、库存、入库、借用归还、报废与统计相关接口"
))
public class Knife4jConfig {
}
