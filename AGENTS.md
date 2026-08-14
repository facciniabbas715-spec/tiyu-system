# AGENTS.md

本文件是体育器材管理系统仓库对 AI 代理（Codex 等）的强制工作约定。任何新会话在本仓库开始任务前，必须先读本文件并遵守；本文件与 `docs/handoff.md` 冲突时，以 `docs/handoff.md` 的最新记录为准。

## 动手前必读

1. `docs/handoff.md` —— 项目交接文档（当前进度、环境基线、Git 约定、验证命令）。
2. `README.md` —— 项目简介与本地运行指南。
3. 执行 `git status`、`git log --oneline --graph -20`、`git tag` 确认真实仓库状态，禁止凭空假设项目阶段或功能。

## 项目一句话

面向学校/企事业单位/体育场馆的体育器材全生命周期管理平台：器材档案、库存、入库、借用归还、报废处置、统计分析。后端 Spring Boot 3.5 + Spring Security（JWT + Redis）+ MyBatis-Plus + MySQL 8 + Flyway；前端 Vue 3 + Vite + TypeScript + Element Plus。

## 工作区与分支约定

- 根目录（main 分支）：release / 存档，现为 v2.0.0。
- `.worktrees\phase-01-scaffold`（develop 分支）：日常开发。
- 日常提交到 develop；main 只接受 release 合并；大升级完成后合并回 main、打新标签（下一个为 v3.0.0）并推送 GitHub。
- 远端：`origin = https://github.com/facciniabbas715-spec/tiyu-system.git`（公开）。
- 本机网络：github.com 443 直连可能不通，本仓库已配置 `http.https://github.com.proxy = http://127.0.0.1:7897`；push/pull 失败先确认 Clash Verge 代理在运行。

## 环境基线（本机）

- JDK：Temurin 21.0.12 LTS，`JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot`；每个 `mvnw.cmd` 命令前设置 `$env:JAVA_HOME`。
- MySQL 8.0 @ localhost:3306：库 `sportseq`，账号 `sportseq / Sportseq@123`。
- Redis @ 6379 无密码：**必须运行**（登录验证码、会话缓存、单据号生成依赖）。
- Node 24 + npm；后端端口 8080，前端 dev 端口 5173（`/api` 代理到 8080）。
- 默认登录账号：`admin / admin123`。
- 表结构由 Flyway 启动时自动迁移：**禁止修改 V1/V2**，结构变更新增 V3+ 迁移文件。

## 完成/验证门槛

改动必须经过以下验证后才能声称"完成/修复/通过"：

```powershell
# 后端全量测试（先确认 Redis 运行）
cd sportseq-admin
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
.\mvnw.cmd test

# 前端类型检查与构建
cd ..\sportseq-web
npm run type-check
npm run build

# 本地冒烟测试（后端运行后）
.\scripts\smoke-test.ps1
```

声称"完成"前必须附上上述命令的实际输出证据。

## 已知注意事项

- Redis 未运行时，登录/缓存/测试会失败。
- `.ps1` 脚本必须保存为 UTF-8 with BOM，否则 Windows PowerShell 中文解析报错。
- 新增前端页面必须与 `sys_menu.component` 路径一致，动态路由按该路径映射组件。
- 无 `del_flag` 的表（`sys_menu`/`sys_dict_type`/`sys_dict_data`/`sys_config`）实体不继承 `BaseEntity`，否则查询报 `Unknown column 'del_flag'`。
- 生产安全配置（`JWT_SECRET` 等）必须走环境变量，不要写死进仓库。
