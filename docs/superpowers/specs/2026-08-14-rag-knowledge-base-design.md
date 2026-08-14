# RAG 知识库系统设计文档

> 日期：2026-08-14
> 范围：将「AI 智能客服」从无状态单轮对话升级为基于体育器材知识库的 RAG 问答
> 前置约束：不修改 Flyway V1/V2；AI 模块的 OpenAI 兼容协议与「无 Key 可启动」约定保持不变

## 1. 目标

1. 管理员可上传/查看/更新/删除知识文档，并重新构建向量。
2. 客服回答基于知识库检索结果，而不是模型自身知识；知识库没有相关资料时明确回复「知识库中暂无相关信息。」，禁止模型编造规则。
3. 开发环境返回 RAG 调试信息（query、检索片段、相似度、最终回答），生产环境不暴露。

## 2. 向量数据库选型（第一步）

候选：PostgreSQL+pgvector、Milvus、Qdrant、Elasticsearch、Redis Vector（Redis Stack）。

| 方案 | 项目规模匹配 | 学习成本 | Docker 部署 | Spring AI 1.1.8 集成 | 结论 |
| --- | --- | --- | --- | --- | --- |
| PostgreSQL + pgvector | 够用，但需与 MySQL 并存的第二数据库 | 中（SQL+扩展） | 中（新增数据库服务） | 官方 `spring-ai-pgvector-store` | 不推荐 |
| Milvus | 面向大规模向量库，本项目严重过剩 | 高 | 高（standalone 仍要 etcd+minio 多容器） | 官方 starter | 不推荐 |
| Qdrant | 优秀，专用向量引擎 | 中低 | 低（单容器） | 官方 `spring-ai-qdrant-store` | 备选 |
| Elasticsearch | 通用搜索引擎，资源占用大 | 中高 | 中（JVM、内存） | 官方 starter | 不推荐 |
| **Redis Vector（Redis Stack）** | **几百~几千 chunk，完全够用** | **低（已熟悉 Redis）** | **低（单容器）** | **官方 `spring-ai-redis-store`，一等支持** | **推荐** |

**最终推荐：Redis Stack（Redis Vector）**，理由：

1. **复用现有基础设施**：本项目登录验证码、JWT 会话、业务缓存、单据号生成全部依赖 Redis；向量存储并入同一 Redis 实例，不新增任何技术栈与运维组件。
2. **规模匹配**：知识库为几十篇文档、几百到几千个 chunk，Redis HNSW/FLAT 检索毫秒级，远低于其能力上限。
3. **Spring AI 集成最简单**：`RedisVectorStore` 通过标准 `VectorStore` 接口接入（HNSW + COSINE + metadata 过滤），与 Qdrant/其他实现零耦合，未来切换只换依赖与配置。
4. **Docker 部署最轻**：把现有 `redis:7.4-alpine` 换成 `redis/redis-stack-server` 单容器即可，无需第二数据库、etcd、minio。

**前提与风险**：

- 普通 Redis 5/6/7 没有 RediSearch/RedisJSON，必须使用 Redis Stack（或内置查询引擎的 Redis 8+）。
- 本机当前是 Windows Redis 5.0.14 且未安装 Docker，本地联调需先切换为 Redis Stack；代码抽象了 `VectorStore`，因此验证与迁移不受影响。
- 若未来需要海量规模或复杂过滤，迁移到 Qdrant 的成本 = 替换依赖 + 修改配置，业务代码不动。

## 3. RAG 数据流（第二步）

**写入链路：**

```text
文档上传(MultipartFile)
  → 保存原始文件到 data/knowledge（MySQL 记录元数据 status=处理中）
  → 文档解析 loader（txt/md: TextReader；docx: POI XWPFWordExtractor；pdf: Spring AI PDFBox 读取器）
  → 文本切分 splitter（TokenTextSplitter，chunk≈400 token，中文标点断句）
  → 分块落 MySQL knowledge_chunk（得到稳定 chunkId）
  → Embedding（OpenAI 兼容 EmbeddingModel，向量维度与模型一致）
  → 向量存储 vector（RedisVectorStore，key=chunkId，metadata=documentId/chunkIndex/title/fileType）
  → 文档状态置为「已就绪」；任一步失败则置为「失败」并记录原因
```

**问答链路：**

```text
用户提问（POST /api/ai/chat）
  → Embedding(question)
  → 相似度检索（topK=4，threshold=0.7，COSINE）
  → 无命中：不调用 LLM，直接返回「知识库中暂无相关信息。」
  → 有命中：按相似度排序拼装知识片段 → 注入 Prompt
  → LLM（ChatClient，低温 + 严格约束）
  → 返回答案；dev 环境附带 debug（query/hits/similarity/answer）
```

## 4. knowledge 模块结构（第三步）

后端新包 `com.company.sportseq.knowledge`（与 `ai` 包同级的独立功能模块，不侵入器材业务）：

```text
com/company/sportseq/knowledge/
├── controller/KnowledgeController.java      # 管理端 API（上传/分页/详情/更新/删除/重建/内置导入）
├── service/KnowledgeDocumentService.java    # 文档管理编排（含 impl）
├── service/RagService.java                  # 检索 + Prompt 组装 + LLM 回答（含 impl）
├── loader/DocumentLoader.java               # Resource → List<Document>（txt/md/pdf/docx）
├── splitter/KnowledgeTextSplitter.java      # TokenTextSplitter 封装（中文标点）
├── embedding/EmbeddingService.java          # EmbeddingModel 封装（embed/维度）
├── vector/VectorStoreService.java           # VectorStore 封装（建索引/写入/删除/相似检索）
├── config/KnowledgeConfig.java              # EmbeddingModel/RedisVectorStore/JedisPooled Bean
├── config/RagProperties.java                # spring.ai.rag.* 配置绑定
├── entity/KnowledgeDocument.java            # extends BaseEntity（含 del_flag）
├── entity/KnowledgeChunk.java               # 物理删除，无 del_flag
├── mapper/KnowledgeDocumentMapper.java      # 需加入 @MapperScan
├── mapper/KnowledgeChunkMapper.java
├── dto/ ...（上传/更新/重建入参）
├── vo/ ...（文档分页 VO、详情 VO、RagDebugVO）
└── exception/KnowledgeException.java        # 3201~3205 业务错误码
```

`AiChatServiceImpl` 只增加一步编排：RAG 可用时走 `RagService.answer()`，不可用时回退到现有纯 LLM 路径；`AiChatVO` 增加可选 `debug` 字段。

## 5. 数据库设计（Flyway V5，新增）

```sql
CREATE TABLE knowledge_document (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(200) NOT NULL COMMENT '文档标题',
  file_name   VARCHAR(255) NOT NULL COMMENT '原始文件名',
  file_type   VARCHAR(20)  NOT NULL COMMENT 'txt/md/docx/pdf',
  file_size   BIGINT       NOT NULL DEFAULT 0 COMMENT '文件字节数',
  file_path   VARCHAR(500) NULL COMMENT '本地存储相对路径',
  status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0处理中 1已就绪 2失败',
  chunk_count INT          NOT NULL DEFAULT 0 COMMENT '分块数',
  error_msg   VARCHAR(500) NULL COMMENT '失败原因',
  remark      VARCHAR(500) NULL COMMENT '备注',
  create_by   BIGINT NULL, create_time DATETIME NULL,
  update_by   BIGINT NULL, update_time DATETIME NULL,
  del_flag    TINYINT NOT NULL DEFAULT 0,
  KEY idx_doc_status (del_flag, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档';

CREATE TABLE knowledge_chunk (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  document_id BIGINT NOT NULL COMMENT '文档ID',
  chunk_index INT    NOT NULL COMMENT '块序号(从0开始)',
  content     TEXT   NOT NULL COMMENT '分块文本',
  token_count INT    NULL COMMENT 'token 数',
  create_time DATETIME NULL,
  KEY idx_chunk_doc (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档分块';
```

- `knowledge_document` 继承 `BaseEntity`（逻辑删除 del_flag、createTime/updateTime 自动填充）。
- `knowledge_chunk` 随文档物理删除，无 del_flag；chunkId 与 Redis 向量 key 一一对应：`sportseq:knowledge:chunk:{id}`。
- Redis 索引：index `sportseq-knowledge-index`，prefix `sportseq:knowledge:chunk:`，HNSW + COSINE，向量维度取 EmbeddingModel 实际维度；metadata 字段：`document_id`(NUMERIC)、`chunk_index`(NUMERIC)、`title`(TEXT)、`file_type`(TAG)。
- 菜单：`sys_menu` id=801（知识库，path=/knowledge，component=knowledge/index，perms=knowledge:manage），授权 role_id=1。

## 6. 管理端 API（第四步）

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/knowledge/documents/upload` | knowledge:manage | 上传文档（multipart `file` + `title` 可选），解析→切分→Embedding→写向量 |
| GET | `/api/knowledge/documents/page` | knowledge:manage | 分页（keyword/title/fileType/status） |
| GET | `/api/knowledge/documents/{id}` | knowledge:manage | 详情（含分块预览） |
| PUT | `/api/knowledge/documents/{id}` | knowledge:manage | 更新标题/备注，或替换文件并重建该文档向量 |
| DELETE | `/api/knowledge/documents/{id}` | knowledge:manage | 删除文档 + 分块 + 向量 |
| POST | `/api/knowledge/documents/{id}/rebuild` | knowledge:manage | 重建单文档向量 |
| POST | `/api/knowledge/documents/seed` | knowledge:manage | 导入内置知识库（classpath `knowledge/*.md`，同标题跳过） |

内置知识文档位于 `sportseq-admin/src/main/resources/knowledge/`，覆盖：使用说明、借用规则、归还规则、损坏处理、维护方法、分类说明、篮球/足球/羽毛球/乒乓球/网球知识（与系统真实状态机一致，见 `docs/design/体育器材管理系统-设计文档.md`）。

> 文档解析刻意不使用 Apache Tika：Tika 3.x 会引入 POI 5.x，与 EasyExcel 的 POI 4.1.2/xmlbeans 3.1.0 冲突（`CTWorkbook` NoClassDefFoundError）；因此 docx 直接用同版本 POI 4.1.2 提取，pdf 用 Spring AI PDFBox 读取器，二者均无版本冲突。

## 7. 防幻觉与调试信息（第六、七步）

**防幻觉策略（多层）：**

1. 检索无命中（所有候选相似度 < threshold）时**不调用 LLM**，直接返回固定文案「知识库中暂无相关信息。」——从根上杜绝编造。
2. 有命中时使用严格约束的系统提示词：只能依据「知识库资料」回答；资料未覆盖时明确说不知道；禁止编造库存、价格、借用人、单据状态等系统数据；涉及操作时引导到对应页面。
3. 生成阶段降低 temperature（0.2），提高答案稳定性。

**调试信息：**

- `RagDebugVO { query, knowledgeUsed, topK, similarityThreshold, hits[{documentId,title,chunkIndex,content,similarity}], answer }`。
- 由 `spring.ai.rag.debug-enabled` 控制：`application.yml` 默认 `false`（生产安全），`application-dev.yml` 置 `true`；关闭时响应中 `debug` 恒为 `null`。

## 8. Docker 部署方式

- 修改根 `docker-compose.yml`：`redis:7.4-alpine` → `redis/redis-stack-server:7.4.0-v0`，保留卷持久化；业务缓存与向量存储共用该实例（一个 Redis 两项职责，无需第二服务）。
- 若已有 Windows Redis 占用 6379，可 `$env:REDIS_PORT=6380; docker compose up -d redis`，并同步设置后端 `REDIS_PORT=6380`。
- 生产部署同构：应用容器 + MySQL + Redis Stack 三组件；`AI_*`、`JWT_SECRET` 走环境变量。

## 9. 配置项

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `AI_EMBEDDING_BASE_URL` | `${AI_BASE_URL}` | Embedding 接口地址；DeepSeek 不提供 Embedding，需换 DashScope/OpenAI |
| `AI_EMBEDDING_API_KEY` | `${AI_API_KEY}` | 独立的 Embedding Key |
| `AI_EMBEDDING_MODEL` | `text-embedding-3-small` | DashScope 用 `text-embedding-v3` |
| `AI_EMBEDDING_DIMENSIONS` | 空（自动探测） | DashScope v3 建议显式设 `1024`，避免建索引维度不匹配 |
| `RAG_TOP_K` | `4` | 检索片段数 |
| `RAG_SIMILARITY_THRESHOLD` | `0.7` | 相似度阈值（Redis COSINE 归一化到 0~1） |
| `RAG_DEBUG` | prod `false` / dev `true` | 调试信息开关 |
| `spring.ai.rag.upload-dir` | `./data/knowledge` | 原始文件存储目录 |

## 10. 测试与验证策略

1. TDD：loader（各格式）、splitter（中文断句/最小块）、EmbeddingService、VectorStoreService（增删/阈值过滤）、RagService（无命中固定回复、命中 Prompt 组装、debug 开关）、KnowledgeDocumentService（上传全链路、删除、重建）。
2. 全量 `.\mvnw.cmd test`（真实 EmbeddingModel 与 VectorStore 用 `@MockitoBean` 桩，Redis Stack 不在测试路径上）。
3. 前端 `npm run type-check && npm run build`。
4. 冒烟：扩展 `scripts/smoke-test.ps1` 覆盖知识库分页/上传校验；新增 `scripts/rag-verify.ps1` 供配置齐全后做真实验证（seed → 提问「篮球应该怎么保养？」→ 校验 debug 命中）。
5. 环境约束说明：本机 AI key 为 DeepSeek（无 Embedding 接口）且无 Docker，真实验证需先配置 `AI_EMBEDDING_*`（如 DashScope）并运行 Redis Stack；自动化测试不受影响。
