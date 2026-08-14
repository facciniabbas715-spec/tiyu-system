package com.company.sportseq.ai.tool.impl;

import com.company.sportseq.ai.tool.AiTool;
import com.company.sportseq.ai.tool.AiToolOutcome;
import com.company.sportseq.ai.tool.AiToolPermission;
import com.company.sportseq.knowledge.service.RagPromptBuilder;
import com.company.sportseq.knowledge.service.RagService;
import com.company.sportseq.knowledge.vo.RagHitVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识检索工具：知识类问题（怎么保养 / 怎么借 / 规则流程）走 RAG 向量检索。
 *
 * <p>本工具只做检索，最终回答由主模型基于命中片段组织；无命中返回固定文案，
 * 从根源上杜绝模型编造规则。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@AiToolPermission
public class KnowledgeTool implements AiTool {

    private final RagService ragService;

    @Tool(description = "检索系统知识库，适用于器材保养方法、借用归还流程与规则等知识类问题；不适合查询实时库存、借用单据等业务数据")
    public AiToolOutcome searchKnowledge(
            @ToolParam(description = "需要检索的知识问题") String query) {
        if (!ragService.isReady()) {
            return AiToolOutcome.failure("知识库暂不可用（未启用或未配置向量化服务），无法检索知识资料。");
        }
        try {
            List<RagHitVO> hits = ragService.retrieve(query);
            if (hits.isEmpty()) {
                return AiToolOutcome.failure(RagPromptBuilder.NO_KNOWLEDGE_REPLY);
            }
            List<Hit> mapped = hits.stream()
                    .map(hit -> new Hit(hit.title(), hit.content(), hit.similarity()))
                    .toList();
            return AiToolOutcome.success(mapped);
        } catch (Exception e) {
            log.warn("知识检索工具调用失败，query={}", query, e);
            return AiToolOutcome.failure("知识检索失败，请稍后重试。");
        }
    }

    /**
     * 单条知识命中片段（供模型组织回答）。
     */
    public record Hit(String title, String content, Double similarity) {
    }
}
