# 前端 Micro 主题说明（UI 视觉优化）

> 依据仓库根目录 `DESIGN (1).md`（Micro — Style Reference）对前端做纯视觉改造，不涉及任何业务逻辑、接口、路由或权限变化。实施日期：2026-08-14。

## 1. 设计原则

- 主题：浅色（light）。
- 气质：蓝→青渐变只用于“产品开场”（登录页），产品内部保持暖白画布 + 墨黑文字的纸感工作区。
- 卡片：纯白表面 + 1px 发丝线边框 + 两层柔和阴影，避免厚重投影。
- 圆角：按钮/输入 8px、产品卡片 14px、大卡片（登录）18px、标签胶囊 999px。
- 颜色只做功能性标点：一个蔚蓝（#518bdb）用于动作/焦点/选中态，配合薄荷/蜜桃/薰衣草/青雾等粉彩做底色。

## 2. 色板映射（Micro → Element Plus）

| 语义 | Micro 令牌 | 落点 |
| --- | --- | --- |
| 主色（动作/链接/选中） | Azure `#518bdb`，按钮加深 `#3f74c5`，文字加深 `#2e5aa0` | `--el-color-primary*`、焦点环、图表 |
| 成功 | Signal Green `#3a6b2a` | `--el-color-success*` |
| 警告 | Amber 加深 `#a66a24`（原 `#e5a057` 加深以满足 4.5:1 对比度） | `--el-color-warning*` |
| 危险/错误 | Coral 加深 `#c64a45`（原 `#ed6d68` 加深） | `--el-color-danger*` / `--el-color-error*` |
| 信息 | Stone Gray `#797267` | `--el-color-info*` |
| 文本 | Ink `#221f1c` / Soft `#4a453f` / Stone `#797267` / Pebble `#8c8a88` | `--el-text-color-*` |
| 背景 | Paper `#f5f5f5`（画布）、`#ffffff`（卡片）、`#faf9f7`（侧栏） | `--el-bg-color*`、`--el-fill-color*` |
| 边框 | 暖灰发丝线 `#e6e3dd` 等 | `--el-border-color*` |
| 图表强调色 | Teal `#36bab8`、Amber `#e5a057`、Orchid `#bf89cd`、Coral `#ed6d68` | ECharts series |
| 粉彩底色 | Mint `#cbfbf1`、Peach `#f8ebd8`、Lavender `#ede9fe`、Teal Mist `#cff2ef` | 统计卡图标底、逾期统计卡 |

> 原样使用浅色强调色做正文/标签文字时对比度不足（如 `#36bab8`、`#ed6d68`），因此「文字承载」场景使用加深的同色系值，装饰/图形场景保留原色。

## 3. 主要改动

- `src/styles/index.scss`：集中定义 Micro 令牌、Element Plus 变量覆盖（色板/圆角/阴影/边框/字体）与组件打磨（按钮、输入焦点环、表格、胶囊标签、弹窗、浮层、面包屑、滚动条、`prefers-reduced-motion`）。
- 布局：侧栏由深色改为暖白（品牌渐变 logo、浅色菜单、白色胶囊选中态），顶栏改为白底发丝线 + 用户胶囊头像，主内容改为纸白画布。
- 登录页：全屏 Azure→Teal 渐变开场 + 白色 18px 圆角卡片 + 品牌 logo/eyebrow/副标题。
- 工作台：统计卡改为“粉彩底图标 + 墨黑数字”，第二排卡片居中；4 张 ECharts 图表统一品牌色，空数据显示「暂无数据」。
- 统计报表：图表换品牌色、逾期卡改为粉彩左对齐卡片。
- AI 客服/知识库/库存流水/404/favicon/index.html：颜色与圆角对齐 Micro；404 数字使用品牌渐变。
- 响应性：知识库与借用单表格列宽微调，1366px 常见宽度下不再横向滚动（1280px 下固定操作列保持可见）。

## 4. 验证记录

- `npm run type-check`、`npm run build` 通过。
- `scripts/smoke-test.ps1` 12 项全部通过。
- Playwright 真实浏览器走查：登录 → 工作台 → 器材档案 → 统计报表 → AI 客服 → 知识库 → 借用单 → 404，浏览器控制台 0 error；截图存档 `output/playwright/micro-ui/*.png`（目录已 gitignore）。
