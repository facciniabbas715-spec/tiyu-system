package com.company.sportseq.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 知识库配置，绑定 {@code spring.ai.rag.*}。
 *
 * <p>向量检索面向 Spring AI {@code VectorStore} 接口实现，Redis Stack 只是默认落地；
 * 若将来切换 Qdrant 等实现，仅需替换依赖与 Bean 装配，业务代码不变。</p>
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.rag")
public class RagProperties {

    /** 总开关：关闭时聊天回退到纯 LLM 对话，知识库管理接口返回未启用提示。 */
    private boolean enabled = true;

    /** 是否在响应中携带 RAG 调试信息（检索片段/相似度/最终回答），生产环境必须为 false。 */
    private boolean debugEnabled = false;

    /** 上传文档的原始文件存储目录。 */
    private String uploadDir = "./data/knowledge";

    /** 单次检索返回的知识片段数。 */
    private int topK = 4;

    /** 相似度阈值（Redis COSINE 已归一化到 0~1，越接近 1 越相似）。 */
    private double similarityThreshold = 0.7;

    /** 文本切分目标 token 数。 */
    private int chunkSize = 400;

    /** 切分时句子边界的最小字符数。 */
    private int minChunkSizeChars = 80;

    /** 单文件大小上限（字节），默认 10MB。 */
    private long maxFileSize = 10 * 1024 * 1024L;

    private Embedding embedding = new Embedding();

    private Vector vector = new Vector();

    @Data
    public static class Embedding {

        /** Embedding 接口地址，默认复用 AI_BASE_URL；DeepSeek 不提供 Embedding，需另配。 */
        private String baseUrl;

        /** Embedding API Key，默认复用 AI_API_KEY。 */
        private String apiKey;

        /** OpenAI 兼容 Embedding 模型名；DashScope 使用 text-embedding-v3。 */
        private String model = "text-embedding-3-small";

        /** 向量维度；为空时由模型实际返回维度决定（Redis 索引创建时自动探测）。 */
        private Integer dimensions;
    }

    @Data
    public static class Vector {

        /** 向量库实现：redis（默认推荐，需 Redis Stack）或 simple（本地开发兜底，内存+JSON持久化）。 */
        private String type = "redis";

        /** 向量库 Redis 主机，默认与业务 Redis 相同（Redis Stack 单实例承载两类职责）。 */
        private String host;

        /** 向量库 Redis 端口。 */
        private int port = 6379;

        /** 向量库 Redis 密码（可选）。 */
        private String password;

        /** RediSearch 索引名。 */
        private String indexName = "sportseq-knowledge-index";

        /** 向量 key 前缀；与 MySQL chunkId 一一对应。 */
        private String prefix = "sportseq:knowledge:chunk:";

        /** 连接/命令超时（毫秒）。 */
        private int timeoutMs = 5000;

        /** simple 模式的 JSON 持久化路径。 */
        private String persistPath = "./data/knowledge/vector-store.json";
    }
}
