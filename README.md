# 体育器材管理系统

面向学校/企事业单位/体育场馆的体育器材全生命周期管理平台，覆盖器材档案、库存、入库、借用归还、报废处置与统计分析。

## 技术栈

- 前端：Vue 3 + Vite + TypeScript + Element Plus + Pinia + Vue Router + Axios + ECharts
- 后端：Spring Boot 3.5 + Spring Security（JWT + Redis）+ MyBatis-Plus + MySQL 8.0 + Flyway + EasyExcel + Spring AI（AI 智能客服：RAG + 业务数据 + 工具调用）

## 目录结构

```text
sportseq-admin/    后端工程（Spring Boot，端口 8080）
sportseq-web/      前端工程（Vite，开发端口 5173，/api 代理到 8080）
scripts/           本地脚本（冒烟测试等）
docs/              设计文档与实施计划
```

## 本地运行

### 1. 环境要求

- JDK 21（`JAVA_HOME` 指向安装目录）
- MySQL 8.0（3306），数据库 `sportseq`，账号 `sportseq` / `Sportseq@123`
- Redis（6379，无密码），例如 `D:\Redis\redis-server.exe D:\Redis\redis.windows.conf`
- Node.js 24+（前端构建）

数据库表结构由 Flyway 在应用启动时自动迁移，无需手工建表。

### 2. 启动后端

```powershell
cd sportseq-admin
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
.\mvnw.cmd spring-boot:run
```

健康检查：<http://localhost:8080/api/health>

### 3. 启动前端

```powershell
cd sportseq-web
npm install
npm run dev
```

浏览器访问 <http://localhost:5173>，默认账号 `admin` / `admin123`。

### 4. 运行测试与构建

```powershell
# 后端全量测试（需先启动 Redis）
cd sportseq-admin
.\mvnw.cmd test

# 前端类型检查与构建
cd ..\sportseq-web
npm run type-check
npm run build
```

### 5. 本地冒烟测试

后端启动后执行：

```powershell
.\scripts\smoke-test.ps1
```

脚本依次验证健康检查、验证码登录、仪表盘汇总、统计报表、业务列表与未认证 401 拦截。

## 文档

- [项目设计文档](docs/design/体育器材管理系统-设计文档.md)
- [实施计划](docs/superpowers/plans/)（按开发阶段拆分）
- [开发交接文档](docs/handoff.md)

## Redis 缓存

业务缓存采用 Cache Aside：查询先读 Redis，未命中回源 MySQL 并写回 Redis；写操作更新数据库后使相关缓存失效。所有缓存键统一在 `sportseq-admin/src/main/java/com/company/sportseq/common/constant/CacheConstants.java` 中管理（命名空间前缀 `sportseq:`）。库存等实时数据使用短 TTL + 写前/事务提交后双失效保证一致性；Redis 不可用时自动降级为直查 MySQL。

### 启动 Redis

方式一：Docker（仅本地开发依赖，Redis 7，不包含应用服务）

```powershell
docker compose up -d redis
```

方式二：本机 Windows Redis

```powershell
D:\Redis\redis-server.exe D:\Redis\redis.windows.conf
```

> 两种方式都会占用 6379 端口，不能同时运行。若本机 Redis 已在运行，可让容器改用 6380：
> `$env:REDIS_PORT=6380; docker compose up -d redis`，并把后端环境变量同步设为 `REDIS_PORT=6380`。
> 容器健康状态可用 `docker inspect --format "{{.State.Health.Status}}" sportseq-redis` 查看（应为 healthy）。

### 验证 Redis 生效

```powershell
# 访问一次器材列表/详情后检查对应缓存键
D:\Redis\redis-cli.exe KEYS "sportseq:equipment:*"
D:\Redis\redis-cli.exe GET "sportseq:equipment:detail:1"
```

连接参数可用环境变量覆盖：`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`、`REDIS_DATABASE`。

## AI 智能客服（RAG + 业务数据 + 工具调用）

侧边栏进入「AI客服」即可对话。AI 会自动区分两类问题：知识类（怎么保养、怎么借、规则流程）走 RAG 知识库检索；业务数据类（现在有多少库存、我借了什么、什么时候归还）通过受控工具查询真实业务数据库，再由模型组织成自然语言。

支持多轮对话：最近 8 条消息按用户隔离缓存在 Redis（默认 30 分钟过期），追问无需重复背景；页面刷新后历史自动恢复，「清空对话」同时清空服务端历史。

内置防护：每个用户每分钟默认限流 20 次提问（可配置）；单次对话的工具调用轮数默认上限 8 轮，防止异常模型无限调用；回答要求纯文本输出，前端也会做轻量排版清理。

### 业务工具与安全边界

| 工具 | 用途 | 所需权限 |
| --- | --- | --- |
| `searchKnowledge` | 检索系统知识库（保养/流程/规则） | 登录即可 |
| `getEquipmentStock` | 查询器材真实库存（可用=在库-锁定） | `stock:list` |
| `getUserBorrowRecords` | 查询当前用户本人的借用记录 | 登录即可 |
| `getBorrowRule` | 借用规则参数（天数/违约金/续借上限） | 登录即可 |
| `getEquipmentDetail` / `searchEquipment` | 器材档案详情 / 按名称搜索 | `equipment:list` |

AI 不能执行 SQL、不能修改数据库：所有工具只能调用业务 Service 的只读方法；每个工具执行前按上表做权限校验；「我的借阅」的用户身份由服务端绑定，模型无法查他人数据；工具无数据或失败时模型必须如实转告，不允许编造库存或借用记录。

开发环境（dev profile）会在回答下方展示「AI 处理详情」（意图、工具调用与入参/结果、知识检索片段），生产环境恒不返回这些内部信息。

### 知识库管理

侧边栏进入「知识库」可管理知识文档：

- 上传文档（txt / md / docx / pdf）：系统自动完成 解析 → 文本切分 → Embedding → 写入向量库；
- 查看文档详情与知识分块、删除文档、更新标题/备注或替换文件、重新构建向量；
- 「导入内置知识库」一键导入 11 篇内置器材知识（使用说明、借用/归还/损坏/维护规则、分类说明、篮球/足球/羽毛球/乒乓球/网球知识）。

### 配置

模型配置位于 `sportseq-admin/src/main/resources/application.yml` 的 `spring.ai.*` 段，走 OpenAI 兼容协议；**API Key 只能通过环境变量提供**，禁止写死或提交到 Git：

```powershell
$env:AI_API_KEY="sk-xxxx"                       # 必填，未配置时对话接口返回友好提示（code=3101）
$env:AI_BASE_URL="https://api.openai.com"       # 可选，DeepSeek: https://api.deepseek.com；DashScope: https://dashscope.aliyuncs.com/compatible-mode/v1
$env:AI_MODEL="gpt-4o-mini"                     # 可选，例如 deepseek-chat / qwen-plus

# RAG 向量化（Embedding）：DeepSeek 不提供 Embedding 接口，需单独配置支持 Embedding 的服务
$env:AI_EMBEDDING_API_KEY="sk-xxxx"                                   # 默认回落到 AI_API_KEY
$env:AI_EMBEDDING_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1"
$env:AI_EMBEDDING_MODEL="text-embedding-v3"                            # OpenAI 可用 text-embedding-3-small
$env:AI_EMBEDDING_DIMENSIONS="1024"                                    # 需与模型实际维度一致（DashScope v3=1024）

# AI 工具调用总开关（可选，默认开启）
$env:AI_TOOLS_ENABLED="true"          # 关闭后客服退回 RAG / 纯对话路径
$env:AI_TOOL_MAX_ITERATIONS="8"       # 单次对话最大工具调用轮数

# 多轮记忆与限流（可选）
$env:AI_CHAT_HISTORY_ENABLED="true"   # 多轮对话记忆（Redis）
$env:AI_CHAT_HISTORY_TTL_SECONDS="1800"
$env:AI_RATE_LIMIT_ENABLED="true"     # 用户级提问限流
$env:AI_RATE_LIMIT_MAX_PER_MINUTE="20"
```

设置环境变量后重启后端，使用 `admin / admin123` 登录；先在「知识库」页导入内置知识库，再到「AI客服」页提问即可。

### 接口

`POST /api/ai/chat`，请求 `{ "message": "篮球怎么借？" }`，响应 `{ "code": 0, "data": { "content": "..." } }`。

### 向量数据库

默认使用 **Redis Stack（Redis Vector）**：复用现有 Redis，单容器部署，RediSearch + RedisJSON 内置，Spring AI 官方 `spring-ai-redis-store` 集成。普通 Redis 5/6/7 没有 RediSearch 模块，本地可用 Docker 启动：

```powershell
docker compose up -d redis   # 已配置 redis/redis-stack-server:7.4.0-v0
```

本机没有 Docker 时，dev profile 自动切换为内存向量库兜底（`spring.ai.rag.vector.type=simple`，JSON 持久化到 `data/knowledge/vector-store.json`），接口与 Redis 实现完全一致，适合开发验证；生产/部署建议使用 Redis（环境变量 `RAG_VECTOR_STORE=redis`）。

### 验证 RAG 与业务工具确实生效

```powershell
# 知识链路：后端启动后（Redis 6379 已运行）：
.\scripts\rag-verify.ps1

# 知识 + 业务链路（需配置 AI_API_KEY 并重启后端）：
.\scripts\ai-tools-verify.ps1
```

`rag-verify.ps1` 验证：导入内置知识库 → 提问「篮球应该怎么保养？」（校验命中知识库并接地回答）→ 提问知识库外问题（校验固定回复，证明没有胡编）。

`ai-tools-verify.ps1` 验证：知识题走 `searchKnowledge`（RAG 命中）→ 库存题走 `getEquipmentStock`（返回真实库存结构）→ 借阅题走 `getUserBorrowRecords`（当前用户真实记录），并断言工具确实被调用且返回真实业务数据。
