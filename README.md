# 体育器材管理系统

面向学校/企事业单位/体育场馆的体育器材全生命周期管理平台，覆盖器材档案、库存、入库、借用归还、报废处置与统计分析。

## 技术栈

- 前端：Vue 3 + Vite + TypeScript + Element Plus + Pinia + Vue Router + Axios + ECharts
- 后端：Spring Boot 3.5 + Spring Security（JWT + Redis）+ MyBatis-Plus + MySQL 8.0 + Flyway + EasyExcel

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
