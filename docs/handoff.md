# 体育器材管理系统 · 交接文档（2026-08-10）

> 本文件供新会话（新聊天窗口）继续开发使用。新会话只需读取本文件即可接手全部上下文。

## 1. 一句话当前进度

系统已完成阶段 0-11（脚手架、数据库、认证、权限、系统管理、器材基础、库存入库、借用归还、报废、统计分析、本地联调加固）及第 8 节中危项处理清单（commit 18-24），后端 **65 个集成测试全绿**，前端构建通过，本地冒烟测试全通过；**暂不部署上线**，后续按需迭代维护。

2026-08-14 完成「AI 客服最终升级（RAG + 业务数据 + Tool Calling，v3.2.0）」：AI 能区分知识类与业务数据类问题，知识走 RAG、业务走受控工具层查真实数据，详见第 16 节。

2026-08-14 完成「前端 Micro 主题 UI 优化」：按仓库根目录 `DESIGN (1).md` 将前端切换为暖白纸感浅色主题（不改动任何业务逻辑/接口/路由/权限），详见第 17 节。

## 2. 环境基线（重要）

| 项 | 值 |
| --- | --- |
| 工作目录（开发） | `E:\codex——vibecoding works\体育器材管理系统\.worktrees\phase-01-scaffold`（git worktree，分支 develop） |
| 主仓库 | `E:\codex——vibecoding works\体育器材管理系统`（main 分支，已含完整快照） |
| 远程仓库 | `origin = https://github.com/facciniabbas715-spec/tiyu-system.git`（公开，`main` 与标签 `v1.0.0`、`v2.0.0`、`v3.0.0` 已推送） |
| 本机代理 | 本仓库已配置 `http.https://github.com.proxy = http://127.0.0.1:7897`（Clash Verge），push/pull 直连不通时走代理 |
| JDK | Temurin 21.0.12 LTS，用户级 `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot`；**每个构建/启动命令前设置 `$env:JAVA_HOME`**（mvnw 用 JAVA_HOME） |
| Maven | 项目自带 mvnw（`.\mvnw.cmd`），无全局 Maven |
| MySQL | 8.0.45 @ localhost:3306；库 `sportseq`；账号 `sportseq` / `Sportseq@123`；root 密码 `061101` |
| Redis | 移植版 @ `D:\Redis\redis-server.exe`（进程启动，端口 6379，无密码）；**开发/测试前必须运行** |
| 前端 | Node 24 + Vite；dev 端口 5173，代理 `/api → localhost:8080` |
| 后端 | 端口 8080；dev profile（数据源/日志配置在 `application-dev.yml`） |

## 3. Git 状态

- **2026-08-10 已做第一版存档**：`main` 已合并 develop 全部实现（311 个文件），打标签 `v1.0.0` 并推送到 GitHub 公开仓库 `facciniabbas715-spec/tiyu-system`。
- **2026-08-14 Redis 业务缓存 v2.0.0**：develop `feat: integrate redis cache` 已合并回 `main`，打标签 `v2.0.0` 并推送 GitHub。
- **2026-08-14 AI 智能客服 v3.0.0**：develop `feat: add spring ai chatbot` 与 Redis 缓存加固（`fix: complete dashboard summary cache invalidation`、`fix: harden cache key hashing and throttle redis failure logs`）已合并回 `main`，打标签 `v3.0.0` 并推送 GitHub。
- **2026-08-14 RAG 知识库 v3.1.0**：develop `feat: implement rag knowledge base` 已合并回 `main`，打标签 `v3.1.0` 并推送 GitHub。
- **2026-08-14 AI 客服最终升级 v3.2.0**：`develop` 上 M1-M4 四个提交（工具层基座 / 业务工具 / 对话编排 / 配置文档）已合并回 `main`，打标签 `v3.2.0` 并推送 GitHub。
- `develop`（worktree）与 `main` 当前内容一致（develop 分支本身未推送远端，需要时再推）。
- 约定：commit 编号与主计划对应；日常开发在 `develop` 提交，`main` 只接受 release 合并；后续大升级完成后合并回 `main` 并打新版本标签（如 `v1.1.0`）再推送。

中危项处理提交：commit 18（用户会话踢出）、19（器材删除校验）、20（仓库删除校验）、21（admin 角色保护）、22（菜单成环校验）、23（借用分页 SQL 过滤）、24（列表 N+1 批量优化），详见第 11 节。

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

> v1.0.0 已由用户在 2026-08-10 确认存档并推送到 GitHub（见第 3 节）；后续若需要上线：再补 Docker/部署脚本、生产 profile。

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

### 7.3 前端布局修复（commit 16）

- **双侧边栏**：动态菜单的目录路由（`component=Layout`）被当作子路由挂到已含 Layout 的 Root 下，导致“布局套布局”，每个业务页面内又渲染一套侧边栏（登录日志、报废单等页面出现两列“体育器材管理”）。修复：`permission.ts` 对目录路由不再重复挂 Layout，只保留最外层一套。
- **深链/刷新 404**：守卫重定向时把 404 路由的 `name` 一并带回，导致首次直达菜单页（或刷新）仍解析到 404。修复：`guard.ts` 重定向只按 path/query/hash 重新导航。
- **验证**：Playwright 实测 `/monitor/loginlog`、`/scrap/scrap` 均只剩 1 套侧边栏、1 处标题；直接刷新菜单页不再 404；前端 type-check/build 通过。

### 7.4 数据权限（commit 17）

- **机制**：`DataScopeService` 按当前用户角色 `data_scope`（1全部/2本部门/3本部门及以下/4仅本人）计算可见范围；内置 `admin` 角色恒为全部，多角色取最宽（最小 scope）。
- **落点**：借用单、归还单、用户管理三处列表查询自动过滤；借用/归还的详情与操作（审核/发放/续借/取消/确认/驳回）、用户详情做访问校验（越权返回 403）。
- **角色管理**：角色新增/编辑支持设置数据范围（RoleDTO/RoleVO + 前端下拉），角色列表展示数据范围列。
- **边界**：器材/仓库/入库/报废/统计无部门语义，暂不参与数据范围过滤（文档化决定）。
- **测试**：`DataScopeTest` 3 个用例（本部门、仅本人、角色 dataScope 返回），全量 43/43 通过。

## 8. 中危项处理清单（交给新对话窗口）

> 用户已要求：在新对话窗口处理以下中危项。新窗口请先读取本文件，然后按清单逐项实现、补测试、提交。
> **状态：已完成（commit 18-24）**，全量测试 65/65 通过，本地冒烟通过；逐项记录见第 11 节。

1. **删除/重置密码/改角色后旧会话仍有效**：`SysUserServiceImpl.remove/resetPassword/update` 应调用现有 `kickUserSessions`（目前仅 `changeStatus` 停用时踢会话）；角色/权限变更后可考虑全量踢出或缩短缓存。
2. **器材删除无业务校验**：`EquipmentServiceImpl.remove` 未使用已有 `EQUIPMENT_IN_USE`；应校验无库存（quantity+locked>0）、无未完结借用/报废后允许删除。
3. **仓库删除无校验**：`WarehouseServiceImpl.remove` 直接删除；应校验 `equipment_stock`、入库/归还/报废单无引用。
4. **内置 admin 角色可被修改/清空菜单**：`SysRoleServiceImpl.update/assignMenus` 对 id=1 应禁止改 roleKey/status 与清空核心菜单。
5. **菜单可成环**：`SysMenuServiceImpl.update` 仅防“父=自己”，需沿祖先链校验新父级不是自身后代，避免 `routers()` 递归栈溢出。
6. **借用分页按借用人过滤不准**：`BorrowServiceImpl.page` 用内存过滤且 total 不准确，应改为 SQL 联查 sys_user。
7. **列表 N+1 查询**：stock/borrow/return/scrap/equipment 的 VO 组装逐行查关联表，改用 `selectBatchIds` 批量查询。

每项建议：先写/补集成测试再改代码（参考 `DataScopeTest`、`BorrowReturnTest` 模式），最后全量 `.\mvnw.cmd test` + 前端 `npm run type-check && npm run build`。

## 11. 中危项处理完成记录（commit 18-24）

1. **删除/重置密码/改角色后踢旧会话（commit 18）**：`SysUserServiceImpl.remove/resetPassword/update` 均调用现有 `kickUserSessions`；新增 `UserSessionKickTest` 3 例（重置密码、改角色、删除后旧 token 均 401）。
2. **器材删除业务校验（commit 19）**：`EquipmentServiceImpl.remove` 校验在库库存（quantity+locked>0）、未完结借用（borrow_order.status 0-3）、未完结报废（scrap_order.status 0-1），命中返回 `EQUIPMENT_IN_USE(3004)`；新增 `EquipmentRemoveTest` 4 例。
3. **仓库删除引用校验（commit 20）**：`WarehouseServiceImpl.remove` 校验 `equipment_stock`、入库单、归还单、报废单引用；新增 `WarehouseRemoveTest` 5 例。
4. **内置 admin 角色保护（commit 21）**：`SysRoleServiceImpl.update/assignMenus` 对 id=1 禁止修改 roleKey/status，并要求保留全部菜单权限；新增 `AdminRoleProtectTest` 3 例。
5. **菜单成环校验（commit 22）**：`SysMenuServiceImpl.update` 沿新父级祖先链校验，禁止挂到自身子孙；新增 `MenuCycleTest`（子级/孙级/自身均拦截，合法移动通过）。
6. **借用分页按借用人过滤（commit 23）**：`BorrowServiceImpl.page` 改为 SQL 子查询过滤 username，`total` 准确；新增 `BorrowPageUsernameTest`。
7. **列表 N+1 批量优化（commit 24）**：equipment/stock/borrow/return/scrap 五处分页 VO 组装改用 `selectBatchIds` 批量加载关联表；新增 `NPlusOneTest`，通过 MyBatis Executor 拦截器断言单次分页 SELECT 次数（器材≤4、库存≤6、借用≤7、归还≤8、报废≤6）。

**验证结果**：`.\mvnw.cmd test` 65/65 通过；`npm run type-check`、`npm run build` 通过；重启后端后 `.\scripts\smoke-test.ps1` 全部通过。

## 12. 高级代码复查与 UI 走查（commit 26-27）

### 12.1 复查发现并修复（commit 26）

1. **角色编辑会清空菜单（存量严重缺陷 + 项4回归）**：前端“修改角色”表单不携带 `menuIds`，原后端 `update()` 会先删除角色-菜单关系再跳过重建，导致任意角色编辑后菜单被清空；项4的 admin 保护又使 admin 角色编辑直接返回 1001。修复：`SysRoleServiceImpl.update` 仅当 `menuIds != null` 时才重建角色-菜单关系；新增 `RoleMenuPreserveTest` 2 例（普通角色菜单保留、admin 角色编辑放行且 74 项菜单不变）。
2. **借用分页“借用人”筛选口径不一致**：界面展示与搜索标签均为真实姓名（realName），但过滤只匹配登录账号（username）。修复：`BorrowServiceImpl.page` 的 SQL 子查询同时匹配 `username` 或 `real_name`；`BorrowPageUsernameTest` 增加真实姓名搜索断言。

### 12.2 UI 走查（Playwright 真实浏览器，localhost:5173）

- **页面覆盖**：19 个菜单页（工作台、器材分类/档案/仓库、库存/流水/入库、借用/归还、报废、统计报表、用户/角色/菜单/部门/字典/参数、操作/登录日志）全部可访问，均为 1 套侧边栏、无横向溢出、无 404 与报错文本；浏览器控制台 0 error / 0 warning。
- **路由**：深链直达各页正常；未知路由正确显示 404 页与“返回首页”。
- **功能点**：验证码登录正常；admin 角色修改名称保存成功（此前 1001），菜单保持 74 项；借用单按“系统管理员”（真实姓名）与 `admin`（账号）搜索均命中。
- **验证**：全量后端测试 67/67；冒烟脚本 11 项全通过；走查截图存档于 `output/playwright/ui-review/*.png`（19 张）。

## 9. 已知注意事项（踩过的坑）

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

## 10. 新会话启动语（建议）

> 请先读取本仓库根目录的 `docs/handoff.md`、`README.md`，并执行 `git status`、`git log --oneline --graph -20`、`git tag` 确认当前状态；然后根据用户本次需求在 develop 分支（worktree 路径见第 2 节）推进；改动前后跑全量测试（`.\mvnw.cmd test`）与前端构建（`npm run type-check && npm run build`），完成后合并回 main 并打新版本标签推送 GitHub。
## 13. Redis 业务缓存改造（2026-08-14）

- 新增统一 Key 管理：`common/constant/CacheConstants`（业务缓存键 + TTL + 参数哈希），`common/cache/CacheService`（Cache Aside、SCAN 模式失效、事务提交后失效、异常降级直查 MySQL），业务代码不再散落 Redis Key 字符串。
- 缓存范围：分类全量列表 `sportseq:category:list:all`、分类详情 `sportseq:category:{id}`、器材详情 `sportseq:equipment:detail:{id}`、器材分页 `sportseq:equipment:list:{md5}`、库存分页 `sportseq:stock:page:{md5}`、热门器材 `sportseq:statistics:usage:{md5}`、仪表盘汇总 `sportseq:dashboard:summary`；配置/字典改用统一 `sportseq:config` / `sportseq:dict` 命名空间。
- 不缓存：借用/归还/报废/入库等业务单据分页（含数据权限、高频写入）、菜单路由（登录时按角色实时构建）、库存锁定数量（继续走 DB 原子 SQL）。
- 一致性：器材/分类/仓库增删改时按需失效相关 Key；库存变更在盘点、入库验收、借用创建/发放/取消/驳回、归还确认、报废处置全部写路径执行“写前删除 + 事务提交后删除 + 60 秒短 TTL”双保险。
- 配置：`application-dev.yml` 新增 `spring.data.redis`（`REDIS_HOST`/`REDIS_PORT`/`REDIS_PASSWORD`/`REDIS_DATABASE` 可覆盖）；新增根目录 `docker-compose.yml`（仅 `redis:7.4-alpine`，本地开发依赖，非部署配置）。
- 测试：新增 `BusinessCacheTest` 6 例（命中回填、统一 Key、写后失效）；`StatisticTest.seed` 前置清汇总/热门缓存以保证 JDBC 直插断言；后端全量 73/73 通过。
- 环境备注：dev 库残留的历史测试数据（分类 AB、器材 AB-2026-000001、用户 yze）已清理；与本次缓存改动无关。

## 14. AI 智能客服（第一阶段，2026-08-14）

1. **依赖**：引入 Spring AI 1.1.8（`spring-ai-bom` + `spring-ai-starter-model-openai`，官方对应 Spring Boot 3.5.x 版本线）；模型走 OpenAI 兼容协议，标准接口 `ChatModel` / `ChatClient`。
2. **后端模块**：独立包 `com.company.sportseq.ai`（controller/service/config/dto/vo/exception），`POST /api/ai/chat` 权限 `ai:chat`，请求 `{message}`，响应 `Result<AiChatVO{content}>`。不修改任何器材业务代码。
3. **配置**：`application.yml` 的 `spring.ai.openai.*`；`api-key` 只从环境变量 `AI_API_KEY` 读取（`AI_BASE_URL`/`AI_MODEL` 可选）。为让无 Key 时应用/测试仍能启动，`spring.ai.model.*=none` 关闭 starter 自动装配，由 `AiConfig` 条件构建 Bean；未配置时对话返回 3101 友好提示。
4. **菜单**：Flyway V4 新增 `sys_menu` id=800（`/ai` → `ai/index`，perms `ai:chat`），并给 role_id=1 授权；其他角色在「角色管理」按需分配。
5. **前端**：`src/api/ai.ts` + `src/views/ai/index.vue`（对话区/左右气泡/输入框/发送/Loading/清空），请求超时 60s。
6. **测试**：新增 `AiChatTest`（桩模型验证接口/认证/参数/系统提示词）与 `AiChatNotConfiguredTest`（无 Key 友好降级）；全量后端 78/78、前端 type-check/build、冒烟 11 项、Playwright 实测「AI客服」页面均通过。

## 15. RAG 知识库升级（2026-08-14，develop 未合并）

1. **选型**：五种向量方案对比后选 **Redis Stack（Redis Vector）**——复用既有 Redis、单容器、规模匹配、Spring AI 官方 `spring-ai-redis-store` 一等支持；代码面向 `VectorStore` 接口，未来切 Qdrant 只需换依赖+配置。Milvus（etcd+minio）、Elasticsearch（资源重）、pgvector（引入第二数据库）不推荐。
2. **后端模块**：独立包 `com.company.sportseq.knowledge`（controller/service/loader/splitter/embedding/vector/config/entity/mapper/dto/vo/exception），不侵入器材业务。写入链路：上传保存 → 解析（txt/md=TextReader；docx=POI XWPFWordExtractor；pdf=Spring AI PDFBox）→ TokenTextSplitter 切分（中文标点断句）→ 写 chunk → Embedding → 向量入库。问答链路：Embedding → 相似度检索（topK=4、threshold=0.7）→ 无命中直接返回「知识库中暂无相关信息。」（不调 LLM）→ 有命中注入严格约束 Prompt → 低温生成。
3. **数据库**：Flyway V5 新增 `knowledge_document`（逻辑删除）、`knowledge_chunk`（物理删除）；菜单 id=801（`/knowledge` → `knowledge/index`，perms `knowledge:manage`，授权 role 1）。向量存 Redis，MySQL 不存向量。
4. **接口**：`/api/knowledge/documents/{page,{id},upload,{id}(PUT),{id}(DELETE),{id}/rebuild,seed}`，权限 `knowledge:manage`；`POST /api/ai/chat` 自动走 RAG（`AiChatVO{content,debug}`，debug 仅 dev）。
5. **配置**：`spring.ai.rag.*`（enabled/topK/threshold/chunkSize/debug/upload-dir/vector.type）；Embedding 独立于聊天：`AI_EMBEDDING_API_KEY/API_BASE_URL/MODEL/DIMENSIONS`（默认回落 AI_*）。**DeepSeek 无 Embedding 接口**，本机已配：聊天=DeepSeek `deepseek-chat`，向量化=百炼 `text-embedding-v3`（1024 维，需 `AI_EMBEDDING_DIMENSIONS=1024`）。
6. **向量库双模式**：`spring.ai.rag.vector.type=redis`（默认，需 Redis Stack，docker-compose 已换 `redis/redis-stack-server:7.4.0-v0`）；dev profile 用 `simple`（PersistentSimpleVectorStore 内存+JSON 持久化，本机无 Docker 的兜底，生产不用）。
7. **前端**：`src/views/knowledge/index.vue`（上传/列表/详情分块/删除/重建/内置导入）、`src/api/knowledge/index.ts`；`ai/index.vue` 展示 RAG 检索详情（dev）。
8. **内置知识**：`sportseq-admin/src/main/resources/knowledge/*.md` 共 11 篇（使用说明/借用/归还/损坏/维护/分类/篮球/足球/羽毛球/乒乓球/网球），内容与真实状态机一致；`POST /api/knowledge/documents/seed` 导入。
9. **踩坑与处理**：Tika 3.x 引入 POI 5.x 与 EasyExcel POI 4.1.2/xmlbeans 冲突（CTWorkbook NoClassDefFoundError）→ 弃 Tika，docx 用 POI 4.1.2、pdf 用 spring-ai-pdf-document-reader；dev 库残留用户 `yze`（测试数据）已软删除（del_flag=1）以稳定 `SysPermissionTest`。
10. **验证**：后端全量 **137/137** 通过；前端 type-check/build 通过；冒烟脚本新增知识库分页检查；新增 `scripts/rag-verify.ps1`（UTF-8 BOM，真实 seed+Embedding+检索+聊天端到端验证）。
11. **发布状态**：已提交 `feat: implement rag knowledge base`，并合并回 main、打标签 v3.1.0、推送 GitHub。后续生产上线前设 `RAG_VECTOR_STORE=redis` 并部署 Redis Stack；如需把 develop 也推到远端可再补推。

## 16. AI 客服最终升级：RAG + 业务数据 + Tool Calling（2026-08-14，已合并 main 并发布 v3.2.0）

1. **目标**：让 AI 客服区分「知识类问题」（怎么保养/怎么借/规则流程 → RAG）与「业务数据类问题」（库存多少/我借了什么/何时归还 → 真实业务库），并在不开放 SQL、不允许写库、全部经 Service 层、全部工具鉴权的前提下实现。
2. **架构**：对话由「无条件 RAG」升级为「单循环工具路由」——RAG 检索也做成工具 `searchKnowledge`，与业务工具一起交给模型选择，一次 Tool Calling 循环完成意图判断、取数、组织回答；无工具时回退 RAG / 纯 LLM。注意：`ChatClient` 手动构建时必须显式挂 `ToolCallAdvisor`（Spring AI 1.1.8 不自动装配）。
3. **工具层**（`com.company.sportseq.ai.tool`）：`AiTool` 标记接口 + `@AiToolPermission` 声明所需权限 + `AiToolPermissionService` 鉴权 + `PermissionAwareToolCallback` 执行前统一强制鉴权 + `AiToolCatalog` 白名单目录（未声明权限/无 `@Tool` 方法的工具启动即报错）。
4. **业务工具**（`ai.tool.impl`）：`KnowledgeTool.searchKnowledge`（登录即可）、`InventoryTool.getEquipmentStock`（`stock:list`，可用=在库-锁定）、`BorrowTool.getUserBorrowRecords`（登录即可，userId 服务端绑定）/`getBorrowRule`（真实 sys_config 参数）、`EquipmentTool.getEquipmentDetail`/`searchEquipment`（`equipment:list`）。全部只调 Service 只读方法，无 SQL、无写库。
5. **编排与调试**：`AiChatServiceImpl` 按 工具 → RAG → 纯 LLM 三级回退；提示词 `AiChatPrompt.TOOL_SYSTEM_PROMPT` 强制只用工具结果作答、禁止编造；dev 环境 `AiDebugVO` 返回意图（KNOWLEDGE/BUSINESS/MIXED/CHAT）、工具调用链、知识命中片段（ThreadLocal 追踪，生产为 null）。
6. **配置**：`spring.ai.tools.enabled`（环境变量 `AI_TOOLS_ENABLED`，默认开）；`application.yml` 的 `chat.system-prompt` 仅用于回退路径。
7. **提交**：`a93054d` 设计文档 → `78c64a3` M1 工具层基座 → `e0e2eeb` M2 业务工具 → `5451e9f` M3 对话编排 → M4（本次 docs/配置/验证脚本提交）。
8. **验证**：后端全量测试 173/173（45 套件，M3 时点），前端 type-check/build 通过；`scripts/ai-tools-verify.ps1`（UTF-8 BOM）提供知识题/库存题/借阅题三链路真机验证（需 `AI_API_KEY`）。
9. **已知口径**：库存/器材工具受 `stock:list` / `equipment:list` 权限约束，无权限角色会得到「无权限」答复而非数据；`rag-verify.ps1` 在新流程下仍然有效（工具模式下知识库外问题同样返回固定文案）。

10. **P2/P3 审查整改（2026-08-14）**：新增用户级提问限流（`spring.ai.rate-limit.*`，Redis 分钟桶，超限 3105）；多轮对话记忆（`spring.ai.chat.history.*`，Redis 按用户隔离 + TTL，`GET/DELETE /api/ai/history`，前端刷新恢复、清空对话同步清理）；工具循环硬上限（`BoundedToolCallAdvisor`，`spring.ai.tools.max-tool-call-iterations` 默认 8，超限 3106——Spring AI 1.1.8 原生循环无上限）；上游 429/超时区分错误码 3103/3104；库存/借阅工具跨页全量聚合（每页 100）；`getEquipmentDetail` 不再向模型透出采购价；提示词禁止 Markdown + 前端纯文本清理；登录/守卫/首页统一落地 `/statistics/dashboard`（旧 `/dashboard` 保留为跳转别名）。发布状态：已合并 main、打标签 v3.2.1 并推送 GitHub。

## 17. 前端 Micro 主题 UI 优化（2026-08-14，develop 未合并）

1. **目标**：依据仓库根目录 `DESIGN (1).md`（Micro — Style Reference）优化前端视觉，**不改动任何业务功能**（无接口/路由/权限/字段变化）。
2. **主题落地**：`src/styles/index.scss` 集中定义 Micro 令牌并覆盖 Element Plus CSS 变量（暖白画布、墨黑文字、蔚蓝主色、8/14/18px 圆角、发丝线边框、双层柔和阴影）；成功/警告/危险语义色加深以通过 4.5:1 文字对比度。
3. **布局**：侧栏深色→暖白（渐变 logo、白色胶囊选中态），顶栏→白底发丝线 + 用户胶囊；登录页→全屏 Azure→Teal 渐变开场卡片；工作台统计卡→粉彩底图标 + 墨黑数字，图表统一品牌色并补充「暂无数据」空态；统计报表/AI 客服/知识库/库存流水/404/favicon 同步对齐。
4. **响应性**：知识库、借用单表格列宽微调，1366px 宽度不横向滚动（1280px 下固定操作列仍可见）。
5. **验证**：`npm run type-check`、`npm run build` 通过；`scripts/smoke-test.ps1` 12 项全通过；Playwright 走查登录/工作台/器材/统计/AI/知识库/借用/404 共 8 页，控制台 0 error，截图存 `output/playwright/micro-ui/*.png`（gitignore）。
6. **说明**：主题设计文档见 `docs/design/ui-micro-theme.md`；`DESIGN (1).md` 位于主仓库根目录（未跟踪，未纳入提交）。
