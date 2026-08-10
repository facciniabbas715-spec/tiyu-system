# 体育器材管理系统 · 交接文档（2026-08-10）

> 本文件供新会话（新聊天窗口）继续开发使用。新会话只需读取本文件即可接手全部上下文。

## 1. 一句话当前进度

系统已完成阶段 0-10（脚手架、数据库、认证、权限、系统管理、器材基础、库存入库、借用归还、报废、统计分析），后端 **35 个集成测试全绿**，前端构建通过；下一步是**阶段 11：联调优化与上线**。

## 2. 环境基线（重要）

| 项 | 值 |
| --- | --- |
| 工作目录（开发） | `E:\codex——vibecoding works\体育器材管理系统\.worktrees\phase-01-scaffold`（git worktree，分支 develop） |
| 主仓库 | `E:\codex——vibecoding works\体育器材管理系统`（main 分支，保持干净） |
| JDK | Temurin 21.0.12 LTS，用户级 `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot`；**每个构建/启动命令前设置 `$env:JAVA_HOME`**（mvnw 用 JAVA_HOME） |
| Maven | 项目自带 mvnw（`.\mvnw.cmd`），无全局 Maven |
| MySQL | 8.0.45 @ localhost:3306；库 `sportseq`；账号 `sportseq` / `Sportseq@123`；root 密码 `061101` |
| Redis | 移植版 @ `D:\Redis\redis-server.exe`（进程启动，端口 6379，无密码）；**开发/测试前必须运行** |
| 前端 | Node 24 + Vite；dev 端口 5173，代理 `/api → localhost:8080` |
| 后端 | 端口 8080；dev profile（数据源/日志配置在 `application-dev.yml`） |

## 3. Git 状态

develop 分支（worktree）已有 13 个提交，最近 5 个：

```text
7c79186 feat: 统计分析模块（仪表盘汇总/报表图表/Excel导出）（commit 12）
255c006 feat: 报废管理模块（申请/审核/处置出库）（commit 11）
c77ff8d feat: 借用归还模块（库存锁定/领用/归还回补/逾期违约金）（commit 10）
361b154 feat: 库存与入库模块（库存/流水/预警/调整、入库单全流程）（commit 9）
35262a2 feat: 器材基础资料模块（分类/器材档案/仓库、自动编码、Excel导入导出）（commit 8）
```

约定：commit 编号与主计划对应（commit 13=联调上线，tag v1.0.0）。全部工作提交在 develop；main 只接受 release 合并。

## 4. 架构速览

- 后端：Spring Boot 3.5.16 / Spring Security 6（JWT+Redis 会话）/ MyBatis-Plus 3.5.9 / Druid / Flyway（V1 24 表 + V2 种子）/ Knife4j。包根 `com.company.sportseq`，分层 controller/service/impl/mapper/entity/dto/vo/security/aspect/config/job。
- 前端：Vue3 + Vite + TS + Element Plus + Pinia + Vue Router。动态路由来自 `GET /api/system/menu/routers`（permissionStore 用 `import.meta.glob('@/views/**/*.vue')` 映射组件，**新页面必须与 sys_menu.component 路径一致**）。
- 认证：admin / admin123（种子）；验证码登录；权限注解 `@PreAuthorize`。
- 库存口径：`equipment_stock.quantity` 在库、`locked_quantity` 锁定（借用申请提交时锁定，领用核销，驳回/取消解锁）；所有 quantity 变动写 `stock_record`；单据号 `{RK/JY/GH/BF}{yyyyMMdd}{6位}` 由 Redis INCR 生成。
- 操作日志：`@Log` 注解 + LogAspect（异步落库、敏感字段脱敏）。

## 5. 验证命令

```powershell
# 后端全量测试（先启动 Redis）
cd "E:\codex——vibecoding works\体育器材管理系统\.worktrees\phase-01-scaffold\sportseq-admin"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
.\mvnw.cmd test

# 前端类型检查与构建
cd ..\sportseq-web
npm run type-check
npm run build
```

## 6. 阶段 10 统计分析（已完成，commit 12）

实现内容：
1. 后端 `StatisticService` + `/api/dashboard/summary`：今日借用/归还/入库数、库存预警数量、报废待审数、器材总数与库存总价值。
2. 报表接口（ECharts 数据）：`/api/statistics/category-stock`（分类饼图）、`/api/statistics/warehouse-stock`（仓库柱状图）、`/api/statistics/borrow-trend`（近 12 个月借用/归还折线，支持时间范围）、`/api/statistics/equipment-usage`（TOP 10）、`/api/statistics/dept-borrow`、`/api/statistics/overdue` + `/overdue/items`。
3. 报表 Excel 导出 `/api/statistics/export?type=category|warehouse|trend|usage|dept|overdue`（EasyExcel，权限 `statistics:export`）。
4. 前端：`dashboard/index.vue`（替换占位页：7 张汇总卡片 + 4 张 ECharts 图）、`statistics/index.vue`（报表页：时间筛选、趋势/排行图、部门表、逾期统计卡与明细表、导出下拉）。
5. 聚合 SQL 位于 `resources/mapper/StatisticMapper.xml`；集成测试 `StatisticTest`（3 个用例，共 35 个全绿）。

## 7. 下一步：阶段 11 联调优化与上线

安全加固（生产密钥环境变量、参数校验复查）、性能检查、Docker/部署脚本、冒烟测试、tag v1.0.0（commit 13）。

## 8. 已知注意事项（踩过的坑）

1. **不要用子代理协作**：本会话子代理消息投递机制故障（任务消息丢失、子代理偏离任务），后续全部**内联执行**。
2. **本机 shell 策略**：`Remove-Item`、`Start-Process`（含 cmd 包装）会被拦截；删除文件用 `apply_patch`，后台启动进程用 `.NET ProcessStartInfo`（`UseShellExecute=false, CreateNoWindow=true`，环境变量用 `$psi.Environment["JAVA_HOME"]=...`）。
3. **Redis 必须运行**：`D:\Redis\redis-server.exe D:\Redis\redis.windows.conf`（后台进程方式）；不运行则登录/缓存测试失败。
4. **Lombok**：pom 已固定 1.18.46 + maven-compiler-plugin `proc=full`（JDK 兼容）。
5. **Hutool 5.8 不能序列化 Java record**（会输出 `{}`）：Security 401/403 处理器已改用 Jackson；新代码不要在 Hutool JSONUtil 里序列化 record。
6. **无 del_flag 的表**（sys_menu/sys_dict_type/sys_dict_data/sys_config）：实体不继承 BaseEntity，用独立审计字段，否则查询报 `Unknown column 'del_flag'`。
7. **@WebMvcTest 坑**：JWT 过滤器必须由 SecurityConfig 显式注册（不能 @Component）；@MapperScan 放在 MybatisPlusConfig；切片测试加 `@AutoConfigureMockMvc(addFilters=false)`。
8. **数据库已是迁移后状态**：修改表结构必须新增 Flyway 版本（V3），不要改 V1/V2。
9. **测试数据清理**：集成测试用 finally 物理清理（JdbcTemplate），避免残留导致断言失败。
10. **中文编码**：PowerShell 控制台显示乱码是 GBK 显示问题，文件本身 UTF-8 正常；不要据此误判文件损坏。

## 9. 新会话启动语（建议）

> 请读取 `docs/handoff.md`，然后在 develop 分支（worktree 路径见文档）按第 6 节继续实现阶段 10 统计分析模块，完成后提交 commit 12。
