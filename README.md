# 体育器材管理系统

面向学校/企事业单位/体育场馆的体育器材全生命周期管理平台，覆盖器材档案、库存、入库、借用归还、报废处置与统计分析。

## 技术栈

- 前端：Vue 3 + Vite + TypeScript + Element Plus + Pinia + Vue Router + Axios + ECharts
- 后端：Spring Boot 3.5 + Spring Security（JWT + Redis）+ MyBatis-Plus + MySQL 8.0 + Flyway + EasyExcel + Spring AI（AI 智能客服）

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

## AI 智能客服（第一阶段）

侧边栏进入「AI客服」即可对话。当前为无状态单轮对话，暂未接入知识库（RAG 为下一阶段）。

### 配置

模型配置位于 `sportseq-admin/src/main/resources/application.yml` 的 `spring.ai.*` 段，走 OpenAI 兼容协议；**API Key 只能通过环境变量提供**，禁止写死或提交到 Git：

```powershell
$env:AI_API_KEY="sk-xxxx"                       # 必填，未配置时对话接口返回友好提示（code=3101）
$env:AI_BASE_URL="https://api.openai.com"       # 可选，DeepSeek: https://api.deepseek.com；DashScope: https://dashscope.aliyuncs.com/compatible-mode/v1
$env:AI_MODEL="gpt-4o-mini"                     # 可选，例如 deepseek-chat / qwen-plus
```

设置环境变量后重启后端，使用 `admin / admin123` 登录，在「AI客服」页输入问题即可。

### 接口

`POST /api/ai/chat`，请求 `{ "message": "篮球怎么借？" }`，响应 `{ "code": 0, "data": { "content": "..." } }`。
