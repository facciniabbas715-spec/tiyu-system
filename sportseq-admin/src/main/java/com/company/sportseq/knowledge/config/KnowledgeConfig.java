package com.company.sportseq.knowledge.config;

import com.company.sportseq.knowledge.exception.KnowledgeException;
import com.company.sportseq.knowledge.loader.DocumentLoader;
import com.company.sportseq.knowledge.service.RagPromptBuilder;
import com.company.sportseq.knowledge.splitter.KnowledgeTextSplitter;
import com.company.sportseq.knowledge.vector.PersistentSimpleVectorStore;
import com.company.sportseq.knowledge.vector.VectorStoreService;
import com.company.sportseq.ai.config.AiConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

import java.nio.file.Paths;

/**
 * RAG 知识库装配：EmbeddingModel、Redis Stack 向量库（惰性建索引）、切分器与加载器。
 *
 * <p>与 {@code ai/config/AiConfig} 约定一致：未配置 Key 时提供降级 Bean，保证应用
 * 与全量测试可正常启动；所有真实网络/Redis 连接均延迟到首次使用时才发生。</p>
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class KnowledgeConfig {

    /** 仅当 Embedding API Key 非空时构建真实 EmbeddingModel。 */
    @Bean
    @Conditional(EmbeddingApiKeyConfiguredCondition.class)
    public EmbeddingModel openAiEmbeddingModel(RagProperties properties) {
        OpenAiApi openAiApi = AiConfig.buildOpenAiApi(
                properties.getEmbedding().getBaseUrl(), properties.getEmbedding().getApiKey());
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(properties.getEmbedding().getModel())
                .dimensions(properties.getEmbedding().getDimensions())
                .build();
        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
    }

    /** 无 Key 时的降级模型：任何调用抛 3201，应用仍可启动。 */
    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel disabledEmbeddingModel() {
        return new EmbeddingModel() {

            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                throw notConfigured();
            }

            @Override
            public float[] embed(Document document) {
                throw notConfigured();
            }

            @Override
            public int dimensions() {
                return 0;
            }

            private KnowledgeException notConfigured() {
                return new KnowledgeException(KnowledgeException.NOT_CONFIGURED,
                        "Embedding 尚未配置：请设置环境变量 AI_API_KEY 或 AI_EMBEDDING_API_KEY 后重启服务");
            }
        };
    }

    /** 向量库连接池（惰性连接，Bean 创建时不会访问 Redis）。 */
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "spring.ai.rag.vector.type", havingValue = "redis", matchIfMissing = true)
    public JedisPooled knowledgeJedis(RagProperties properties) {
        RagProperties.Vector vector = properties.getVector();
        String host = StringUtils.hasText(vector.getHost()) ? vector.getHost() : "localhost";
        DefaultJedisClientConfig.Builder config = DefaultJedisClientConfig.builder()
                .timeoutMillis(vector.getTimeoutMs());
        if (StringUtils.hasText(vector.getPassword())) {
            config.password(vector.getPassword());
        }
        return new JedisPooled(new HostAndPort(host, vector.getPort()), config.build());
    }

    /**
     * Redis VectorStore：索引初始化关闭（避免启动即连接），由 VectorStoreService
     * 在首次写入前幂等触发；metadata 字段声明后才能参与检索结果的返回与过滤。
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.rag.vector.type", havingValue = "redis", matchIfMissing = true)
    public VectorStore knowledgeVectorStore(JedisPooled jedis, EmbeddingModel embeddingModel,
                                            RagProperties properties) {
        RagProperties.Vector vector = properties.getVector();
        return RedisVectorStore.builder(jedis, embeddingModel)
                .indexName(vector.getIndexName())
                .prefix(vector.getPrefix())
                .metadataFields(
                        RedisVectorStore.MetadataField.numeric(VectorStoreService.METADATA_DOCUMENT_ID),
                        RedisVectorStore.MetadataField.numeric(VectorStoreService.METADATA_CHUNK_INDEX),
                        RedisVectorStore.MetadataField.text(VectorStoreService.METADATA_TITLE),
                        RedisVectorStore.MetadataField.tag(VectorStoreService.METADATA_FILE_TYPE))
                .vectorAlgorithm(RedisVectorStore.Algorithm.HNSW)
                .initializeSchema(false)
                .build();
    }

    /**
     * 开发兜底：本机没有 Docker/Redis Stack 时启用（application-dev.yml 配置 simple），
     * 内存向量库 + JSON 持久化，接口与 Redis 实现完全一致，切换零成本。
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.rag.vector.type", havingValue = "simple")
    public VectorStore simpleKnowledgeVectorStore(EmbeddingModel embeddingModel, RagProperties properties) {
        return new PersistentSimpleVectorStore(embeddingModel,
                Paths.get(properties.getVector().getPersistPath()));
    }

    @Bean
    public DocumentLoader knowledgeDocumentLoader() {
        return new DocumentLoader();
    }

    @Bean
    public KnowledgeTextSplitter knowledgeTextSplitter(RagProperties properties) {
        return new KnowledgeTextSplitter(properties.getChunkSize(), properties.getMinChunkSizeChars());
    }

    /**
     * RAG 专用 ChatClient：严格接地系统提示词 + 低温生成，与 ai 模块的普通对话客户端分离。
     */
    @Bean
    public ChatClient ragChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(RagPromptBuilder.RAG_SYSTEM_PROMPT)
                .defaultOptions(OpenAiChatOptions.builder().temperature(0.2).build())
                .build();
    }

    /** 仅当 {@code spring.ai.rag.embedding.api-key} 非空时成立。 */
    public static class EmbeddingApiKeyConfiguredCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String apiKey = context.getEnvironment()
                    .getProperty("spring.ai.rag.embedding.api-key", "");
            return StringUtils.hasText(apiKey);
        }
    }
}
