package com.company.sportseq.knowledge.exception;

import com.company.sportseq.common.exception.BizException;

/**
 * 知识库业务异常：3201~3205，由全局异常处理器统一转为 Result。
 */
public class KnowledgeException extends BizException {

    /** RAG/Embedding 未配置。 */
    public static final int NOT_CONFIGURED = 3201;

    /** 上传文件非法（空文件/不支持的类型/超限）。 */
    public static final int INVALID_FILE = 3202;

    /** 文档不存在。 */
    public static final int NOT_FOUND = 3203;

    /** Embedding 调用失败。 */
    public static final int EMBEDDING_FAILED = 3204;

    /** 向量库不可用（Redis Stack/RediSearch 未就绪）。 */
    public static final int VECTOR_UNAVAILABLE = 3205;

    /** 大模型调用失败。 */
    public static final int LLM_FAILED = 3206;

    public KnowledgeException(int code, String message) {
        super(code, message);
    }
}
