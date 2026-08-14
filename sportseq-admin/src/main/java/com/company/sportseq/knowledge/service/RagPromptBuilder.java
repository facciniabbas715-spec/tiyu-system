package com.company.sportseq.knowledge.service;

import com.company.sportseq.knowledge.vo.RagHitVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * RAG Prompt 组装：把检索到的知识片段注入严格约束的提示词，防止模型脱离资料编造。
 */
@Component
public class RagPromptBuilder {

    /**
     * RAG 系统提示词：只允许依据知识库资料回答，资料不足时明确告知，禁止编造系统数据。
     */
    public static final String RAG_SYSTEM_PROMPT = """
            你是“体育器材智能客服”，隶属于体育器材管理系统，现在已接入系统知识库（RAG）。
            你必须严格遵守以下规则回答用户问题：
            1. 只能依据下方用户消息中提供的【知识库资料】回答，不得使用资料之外的臆测或模型自身知识编造规则。
            2. 如果【知识库资料】中没有与问题相关的内容，或资料不足以回答问题，你必须原样回复：知识库中暂无相关信息。不得补充任何解释或建议。
            3. 禁止编造本系统的具体数据，例如库存数量、价格、借用人、单据号、单据状态等；涉及具体数据或操作时，请引导用户到系统对应页面查询或办理。
            4. 回答使用简体中文，语气友好、专业、简洁；优先使用资料中的原话表述规则。
            5. 输出纯文本，不要使用 Markdown 等排版语法。
            """;

    /** 无命中时的固定回复：不调用大模型，从根上避免编造。 */
    public static final String NO_KNOWLEDGE_REPLY = "知识库中暂无相关信息。";

    /**
     * 组装用户消息：问题 + 编号的知识片段（含来源标题与块序号）。
     */
    public String buildUserMessage(String question, List<RagHitVO> hits) {
        AtomicInteger index = new AtomicInteger();
        String context = hits.stream()
                .map(hit -> String.format("【资料%d｜来源：%s｜片段#%d】\n%s",
                        index.incrementAndGet(), hit.title(), hit.chunkIndex(), hit.content()))
                .collect(Collectors.joining("\n\n"));
        return "请回答用户问题。\n\n【知识库资料】\n" + context + "\n\n【用户问题】\n" + question;
    }
}
