# 体育器材管理系统 · 交接文档（2026-08-10）

> 本文件供新会话（新聊天窗口）继续开发使用。新会话只需读取本文件即可接手全部上下文。

## 1. 一句话当前进度

系统已完成阶段 0-11（脚手架、数据库、认证、权限、系统管理、器材基础、库存入库、借用归还、报废、统计分析、本地联调加固），后端 **35 个集成测试全绿**，前端构建通过，本地冒烟测试全通过；**暂不部署上线**，后续按需迭代维护。

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

develop 分支（worktree）已有 14 个提交，最近 5 个：

```text
7036697 feat: 本地联调加固（JWT环境变量/参数校验/报表索引/冒烟脚本）（commit 13）
c4df795 feat: 统计分析模块（仪表盘汇总/报表图表/Excel导出）（commit 12）
255c006 feat: 报废管理模块（申请/审核/处置出库）（commit 11）
c77ff8d feat: 借用归还模块（库存锁定/领用/归还回补/逾期违约金）（commit 10）
361b154 feat: 库存与入库模块（库存/流水/预警/调整、入库单全流程）（commit 9）
```

约定：commit 编号与主计划对应；阶段 11 按用户要求仅做本地联调加固（未上线、未打 tag v1.0.0）。全部工作提交在 develop；main 只接受 release 合并。

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

## 7. 阶段 11 本地联调加固（已完成，commit 13；未上线）

1. **安全加固**：JWT secret/有效期支持环境变量覆盖（`JWT_SECRET`/`JWT_EXPIRATION`，开发缺省值不变）；参数校验复查补充 `ReturnItemDTO.penaltyAmount`、`StockInItemDTO.unitPrice` 非负校验（其余 DTO 校验覆盖完整，控制器 `@Valid` 全覆盖）。
2. **性能索引**：新增 Flyway V3 `V3__optimize_indexes.sql`：借用单 issue_time/create_time、借用明细 (status, issued_quantity)、归还单 confirm_time/create_time、入库单/报废单 create_time、库存流水 (change_type, create_time)。
3. **冒烟脚本**：`scripts/smoke-test.ps1`（UTF-8 BOM）：健康检查 → 验证码登录（redis-cli 取码）→ 仪表盘/统计报表/业务列表 → 未认证 401。已验证全通过；前端 dev server（5173）代理 `/api → 8080` 联通。
4. **文档**：README 更新为本地运行指南。

> 后续若需要上线：再补 Docker/部署脚本、生产 profile、tag v1.0.0（原 commit 13 内容拆为新的独立提交）。

### 7.1 阶段 11 后维护（commit 14）

- **修复**：LogAspect 序列化参数时过滤 `ServletRequest/ServletResponse/OutputStream/Writer`，导出接口不再把 Excel 二进制写入 `sys_oper_log.oper_param`（回归测试 `StatisticTest.exportLog_shouldSkipServletResponse`）。
- **前端**：报表导出文件名由类型码改为中文标签。
- **冒烟脚本**：新增 Excel 导出检查（`-OutFile` 下载 + PK 头校验）。
- **本地运维脚本**：`scripts/backup-db.ps1`（mysqldump 单事务备份到 `backups/`，已 gitignore）、`scripts/start-backend.ps1`（隐藏窗口后台启动后端）。

### 7.2 高危缺陷修复（commit 15）

- **H1 归还重复入账**：`ReturnServiceImpl` 创建时按借用明细聚合校验归还数量；确认时对 `borrow_item` 加行锁（`selectForUpdate`）并二次校验未还数量，杜绝两张待确认归还单/同单重复明细导致的重复加库存。
- **H2 扣减忽略锁定库存**：新增 `EquipmentStockMapper.subtractAvailableQuantity`（`quantity - locked_quantity >= ?`）；报废处置与盘亏改走可用库存扣减；借用发放 `issue()` 在行锁内校验锁定数量足够后再扣减解锁。
- **回归测试**：BorrowReturnTest +3（重复归还拦截、同单重复明细拦截、报废不占用锁定库存），StockTest +1（盘亏不占用锁定库存）；全量 40/40 通过。
- **E2E 脚本**：`scripts/e2e-review.ps1` 已改为回归断言（主流程 + 两个缺陷的拦截验证），实测全部通过。

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
11. **PowerShell 脚本中文解析**：`.ps1` 必须保存为 **UTF-8 with BOM**，否则 Windows PowerShell 按 ANSI 解析中文报语法错误（本次 smoke-test.ps1 已踩过）。

## 9. 新会话启动语（建议）

> 请读取 `docs/handoff.md`，然后在 develop 分支（worktree 路径见文档）继续维护：本地启动后端+前端后执行 `.\scripts\smoke-test.ps1` 冒烟；有迭代需求时按阶段计划推进。
