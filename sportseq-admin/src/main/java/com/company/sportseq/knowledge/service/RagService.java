package com.company.sportseq.knowledge.service;

import com.company.sportseq.knowledge.vo.RagResult;
import com.company.sportseq.knowledge.vo.RagHitVO;

import java.util.List;

/**
 * RAG 问答服务：检索知识库 → 组装 Prompt → 调用 LLM。
 */
public interface RagService {

    /**
     * RAG 是否可用（总开关开启且已配置 Embedding Key）。
     * 不可用时 AI 客服回退到原有纯 LLM 对话路径。
     */
    boolean isReady();

    /**
     * 仅检索知识库，不调用大模型。命中片段交给上层（知识工具 / 对话编排）处理。
     */
    List<RagHitVO> retrieve(String question);

    /**
     * 基于知识库回答。检索无命中时返回固定文案，不调用大模型。
     */
    RagResult answer(String question);
}
