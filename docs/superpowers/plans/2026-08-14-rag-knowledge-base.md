# RAG 知识库 Implementation Plan

> **For agentic workers:** 本计划由主会话内联执行（本仓库交接文档要求不使用子代理协作）。任务粒度按 TDD：先写失败测试 → 跑测试确认失败 → 最小实现 → 跑测试确认通过。最终按用户要求合并为一次提交 `feat: implement rag knowledge base`。

**Goal:** 把现有 AI 智能客服升级为基于 Redis Stack 向量库的 RAG 问答，并提供知识库文档管理能力。

**Architecture:** 新增自包含 `com.company.sportseq.knowledge` 模块（loader→splitter→embedding→vector），MySQL 存文档/分块元数据，Redis Stack 存向量；`/api/ai/chat` 编排 RAG，无命中时不调 LLM 返回固定文案，dev 环境返回调试信息。全部代码面向 Spring AI `VectorStore`/`EmbeddingModel` 接口，便于未来切换向量库。

**Tech Stack:** Spring Boot 3.5.16 / Spring AI 1.1.8（`spring-ai-redis-store`、`spring-ai-tika-document-reader`）/ Jedis 6 / MyBatis-Plus / MySQL 8 / Flyway V5 / Vue 3 + Element Plus。

---

### Task 1: 基础设施与配置

**Files:**
- Modify: `sportseq-admin/pom.xml`（+`spring-ai-redis-store`、+`spring-ai-tika-document-reader`）
- Modify: `sportseq-admin/src/main/resources/application.yml`（`spring.ai.rag.*` 默认值、debug 默认 false）
- Modify: `sportseq-admin/src/main/resources/application-dev.yml`（debug=true）
- Modify: `sportseq-admin/src/main/java/com/company/sportseq/config/MybatisPlusConfig.java`（@MapperScan 增加 `com.company.sportseq.knowledge.mapper`）
- Create: `sportseq-admin/src/main/java/com/company/sportseq/knowledge/config/RagProperties.java`
- Create: `sportseq-admin/src/main/resources/db/migration/V5__add_knowledge_base.sql`
- Modify: `.gitignore`（+`data/`）、`docker-compose.yml`（redis-stack-server）

- [ ] **Step 1:** 写 `RagProperties`（prefix=`spring.ai.rag`：enabled、uploadDir、topK、similarityThreshold、debugEnabled、embedding.*、vector.*）与 V5 SQL（见设计文档第 5 节 + 菜单 id=801）。
- [ ] **Step 2:** `cd .worktrees/phase-01-scaffold/sportseq-admin; $env:JAVA_HOME=...; .\mvnw.cmd -q compile` 确认依赖可解析、编译通过。
- [ ] **Step 3:** 启动上下文测试见 Task 9；本任务以编译为验证门槛。

### Task 2: 实体 / Mapper / DTO / VO / 异常

**Files:**
- Create: `.../knowledge/entity/KnowledgeDocument.java`（extends BaseEntity）
- Create: `.../knowledge/entity/KnowledgeChunk.java`
- Create: `.../knowledge/mapper/KnowledgeDocumentMapper.java`、`KnowledgeChunkMapper.java`
- Create: `.../knowledge/dto/KnowledgeDocumentDTO.java`、`KnowledgeChunkDTO.java`
- Create: `.../knowledge/vo/KnowledgeDocumentVO.java`、`KnowledgeDocumentDetailVO.java`、`KnowledgeChunkVO.java`、`RagHitVO.java`、`RagDebugVO.java`
- Create: `.../knowledge/exception/KnowledgeException.java`（3201 未配置、3202 上传文件非法、3203 文档不存在、3204 Embedding 失败、3205 向量库不可用）

- [ ] 字段与 SQL 严格一致；`KnowledgeDocument.status` 0/1/2；chunkId 规则 `sportseq:knowledge:chunk:{id}` 以常量形式放在 `VectorStoreService`。

### Task 3: splitter（TDD）

**Files:**
- Test: `sportseq-admin/src/test/java/com/company/sportseq/knowledge/KnowledgeTextSplitterTest.java`
- Create: `.../knowledge/splitter/KnowledgeTextSplitter.java`

- [ ] **Step 1:** 失败测试：`split_shouldChunkLongChineseTextAtSentenceBoundaries`（构造 2000+ 字符中文文本，断言块数>1 且每块非空）、`split_shouldKeepShortTextAsSingleChunk`（短文本单块且内容不变）。
- [ ] **Step 2:** `.\mvnw.cmd -q -Dtest=KnowledgeTextSplitterTest test` → FAIL（类不存在）。
- [ ] **Step 3:** 实现：封装 `TokenTextSplitter.builder().withChunkSize(400).withMinChunkSizeChars(80).withPunctuationMarks(List.of('.','?','!','\n','。','？','！','；')).build()`，`split(String)` 返回内容列表。
- [ ] **Step 4:** 同上命令 → PASS。

### Task 4: loader（TDD）

**Files:**
- Test: `.../knowledge/DocumentLoaderTest.java`（测试资源 `test-knowledge.txt`、`test-knowledge.md`；docx 用 ZipOutputStream 运行时构造）
- Create: `.../knowledge/loader/DocumentLoader.java`

- [ ] **Step 1:** 失败测试：`loadTxt_shouldReturnSingleDocument`、`loadDocx_shouldExtractParagraphText`、`loadUnsupported_shouldThrowKnowledgeException(3202)`。
- [ ] **Step 2:** `-Dtest=DocumentLoaderTest test` → FAIL。
- [ ] **Step 3:** 实现 `DocumentLoader.read(Resource, String fileType)`：txt/md→`TextReader`（UTF-8），docx→POI `XWPFWordExtractor`（与 EasyExcel 同版本 POI 4.1.2，避免 Tika 引入 POI/xmlbeans 冲突），pdf→`PagePdfDocumentReader`，其它→3202。
- [ ] **Step 4:** → PASS。

### Task 5: embedding（TDD）

**Files:**
- Test: `.../knowledge/EmbeddingServiceTest.java`
- Create: `.../knowledge/embedding/EmbeddingService.java`

- [ ] **Step 1:** 失败测试（Mockito `EmbeddingModel`）：`embed_shouldReturnVector`、`embed_shouldWrapFailureAsKnowledgeException(3204)`（模型抛异常）。
- [ ] **Step 2:** → FAIL。
- [ ] **Step 3:** 实现 `float[] embed(String)` / `List<float[]> embedAll(List<Document>)`，异常翻译为 3204，且 `baseUrl` 含 `deepseek` 时提示换 Embedding 服务商。
- [ ] **Step 4:** → PASS。

### Task 6: vector（TDD）

**Files:**
- Test: `.../knowledge/VectorStoreServiceTest.java`
- Create: `.../knowledge/vector/VectorStoreService.java`

- [ ] **Step 1:** 失败测试（Mockito `VectorStore`）：`store_shouldAddDocumentsWithStableIdsAndMetadata`（验证 id=`sportseq:knowledge:chunk:{n}`、metadata 全量）、`deleteByIds_shouldDelegate`、`search_shouldApplyTopKAndThreshold`（验证 SearchRequest 参数）、`ensureSchema_shouldSkipNonInitializingStore`。
- [ ] **Step 2:** → FAIL。
- [ ] **Step 3:** 实现 `store(List<ChunkDoc>)`、`delete(List<Long> chunkIds)`、`search(String query, int topK, double threshold)`（返回 `List<RagHit>`：从 Document.score 取相似度）、`ensureSchema()`（store 实现 `InitializingBean` 时惰性调用 afterPropertiesSet 并翻译异常为 3205）。
- [ ] **Step 4:** → PASS。

### Task 7: KnowledgeDocumentService（TDD，集成测试连 MySQL）

**Files:**
- Test: `.../knowledge/KnowledgeDocumentServiceTest.java`（@SpringBootTest，`@MockitoBean EmbeddingModel/VectorStore`，JdbcTemplate 清理 knowledge 表）
- Create: `.../knowledge/service/KnowledgeDocumentService.java` + `.../service/impl/KnowledgeDocumentServiceImpl.java`
- Create: `.../knowledge/config/KnowledgeConfig.java`（EmbeddingModel / JedisPooled / RedisVectorStore / VectorStoreService / splitter / loader 装配；`initializeSchema(false)`）

- [ ] **Step 1:** 失败测试：
  - `upload_shouldPersistDocumentChunksAndVectors`（MockMultipartFile txt；断言 doc status=1、chunk 落库、vectorStore.add 被调用且 id 稳定）
  - `page_shouldFilterByKeywordAndStatus`
  - `delete_shouldRemoveChunksAndVectors`
  - `rebuild_shouldDeleteOldVectorsThenReEmbed`
  - `seed_shouldImportBuiltinDocsAndSkipExistingTitle`
  - `uploadUnsupported_shouldThrow3202`
- [ ] **Step 2:** → FAIL（类不存在）。
- [ ] **Step 3:** 实现：上传（校验扩展名/大小上限 10MB → 保存文件 → 落库 status=0 → loader→splitter→写 chunk→embedding→vector → status=1；失败 status=2 记录 error）、分页、详情、更新（标题/备注 或 替换文件）、删除（vector 先删、chunk 物理删、doc 逻辑删）、重建、seed（classpath `knowledge/*.md`）。
- [ ] **Step 4:** → PASS。注意测试用例必须用 `finally` 清理，避免残留数据影响断言。

### Task 8: RagService（TDD）

**Files:**
- Test: `.../knowledge/RagServiceTest.java`（纯 Mockito：EmbeddingModel/VectorStore/ChatClient/VectorStoreService 桩）
- Create: `.../knowledge/service/RagService.java` + `impl/RagServiceImpl.java`、`.../knowledge/service/RagPromptBuilder.java`

- [ ] **Step 1:** 失败测试：
  - `answer_shouldReturnCannedMessageWhenNoHit`（检索空 → 返回「知识库中暂无相关信息。」、knowledgeUsed=false、不调用 ChatClient）
  - `answer_shouldGroundAnswerOnRetrievedChunks`（有命中 → 捕获 Prompt 断言包含片段与严格约束、返回 answer）
  - `answer_shouldIncludeDebugOnlyWhenEnabled`
- [ ] **Step 2:** → FAIL。
- [ ] **Step 3:** 实现：`isReady()`（RAG enabled 且 EmbeddingModel 非降级、VectorStore 可用）、`answer(String question)`；系统提示词含「只能依据知识库资料回答……知识库中没有相关资料时必须回复：知识库中暂无相关信息。不得编造……」，temperature 0.2。
- [ ] **Step 4:** → PASS。

### Task 9: Controller 与 AI 客服接入（TDD）

**Files:**
- Create: `.../knowledge/controller/KnowledgeController.java`
- Modify: `.../ai/service/impl/AiChatServiceImpl.java`、`.../ai/vo/AiChatVO.java`
- Test: `.../knowledge/KnowledgeControllerTest.java`（登录 admin、权限 403、分页契约、删除后 404 语义=3203）、更新 `.../ai/AiChatTest.java`（RAG 未就绪时保持原有断言；AiChatVO 增加 debug 字段）

- [ ] **Step 1:** 失败测试：knowledge 接口 401/403/参数/分页契约；`ai:chat` 无 Key 时 debug=null 且 content 正常。
- [ ] **Step 2:** → FAIL。
- [ ] **Step 3:** 实现 Controller（`@PreAuthorize("hasAuthority('knowledge:manage')")`、`@Log`、@Valid）；`AiChatServiceImpl`：`ragService.isReady()` → `AiChatVO(content, debug)`，否则走原 chatClient 路径（保持 3101 语义）。
- [ ] **Step 4:** → PASS；同时确认 AiChatNotConfiguredTest 仍通过。

### Task 10: 内置知识文档与 seed

**Files:**
- Create: `sportseq-admin/src/main/resources/knowledge/*.md`（≥11 篇：使用说明/借用规则/归还规则/损坏处理/维护方法/分类说明/篮球/足球/羽毛球/乒乓球/网球；篮球篇必须含「保养」内容以支撑验收问题）

- [ ] 内容与系统真实状态机一致（借用 0待审核→…→6已取消；归还 0待确认…；违约金默认 5 元/天等，摘自设计文档第 6 节）。

### Task 11: 前端知识库管理与客服展示

**Files:**
- Create: `sportseq-web/src/api/knowledge/index.ts`
- Create: `sportseq-web/src/views/knowledge/index.vue`（列表/上传对话框/详情抽屉/重建/删除/seed 按钮）
- Modify: `sportseq-web/src/api/ai.ts`（AiChatVO+debug）、`sportseq-web/src/views/ai/index.vue`（debug 折叠面板、知识库接入提示）

- [ ] 菜单路由由 V5 的 `sys_menu`（path=/knowledge，component=knowledge/index）驱动，与 `import.meta.glob('@/views/**/*.vue')` 映射一致。

### Task 12: 文档、脚本与全量验证

**Files:**
- Modify: `scripts/smoke-test.ps1`（+知识库分页/未授权 403）
- Create: `scripts/rag-verify.ps1`（UTF-8 BOM；seed→提问「篮球应该怎么保养？」→校验 debug hit 与回答含知识内容）
- Modify: `README.md`、`docs/handoff.md`

- [ ] **Step 1:** 后端全量 `.\mvnw.cmd test`（先确保 Redis 6379 运行）—— 期望全绿。
- [ ] **Step 2:** 前端 `npm run type-check`、`npm run build` —— 期望 exit 0。
- [ ] **Step 3:** 启动后端点：`.\scripts\smoke-test.ps1` 全过。
- [ ] **Step 4:** 真实验证（可选，取决于环境）：Redis Stack + `AI_EMBEDDING_*` 就绪后跑 `.\scripts\rag-verify.ps1`。
- [ ] **Step 5:** 在 develop worktree `git add -A` + `git commit -m "feat: implement rag knowledge base"`。

## Self-Review

- 覆盖检查：用户 7 步要求分别对应 Task 3-11；「告诉我 9 项」在最终回复与 README/handoff 中给出。
- 一致性：`document_id` 元数据类型 NUMERIC（Long），`chunk_index` NUMERIC，`title` TEXT，`file_type` TAG；RedisVectorStore 的 `metadataFields` 必须与写入 metadata 同名同型。
- 无占位：所有任务包含明确文件、用例名与验证命令。
