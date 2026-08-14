package com.company.sportseq.ai.tool.impl;

import com.company.sportseq.ai.tool.AiToolOutcome;
import com.company.sportseq.knowledge.exception.KnowledgeException;
import com.company.sportseq.knowledge.service.RagPromptBuilder;
import com.company.sportseq.knowledge.service.RagService;
import com.company.sportseq.knowledge.vo.RagHitVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 知识检索工具单元测试：只做检索，命中/无命中/不可用/失败四种结果都返回固定语义。
 */
class KnowledgeToolTest {

    @Test
    void search_shouldReturnHitsFromRag() {
        RagService ragService = mock(RagService.class);
        when(ragService.isReady()).thenReturn(true);
        RagHitVO hit = new RagHitVO(5L, "篮球相关知识", 1,
                "篮球使用后应清洁表面灰尘，存放于干燥阴凉处，避免暴晒与尖锐物划伤。", 0.83);
        when(ragService.retrieve("篮球怎么保养")).thenReturn(List.of(hit));
        KnowledgeTool tool = new KnowledgeTool(ragService);

        AiToolOutcome outcome = tool.searchKnowledge("篮球怎么保养");

        assertTrue(outcome.ok());
        List<KnowledgeTool.Hit> hits = cast(outcome.data());
        assertEquals(1, hits.size());
        assertEquals("篮球相关知识", hits.get(0).title());
        assertEquals(0.83, hits.get(0).similarity());
    }

    @Test
    void search_shouldReturnCannedMessageWhenNoHit() {
        RagService ragService = mock(RagService.class);
        when(ragService.isReady()).thenReturn(true);
        when(ragService.retrieve("不存在的问题")).thenReturn(List.of());
        KnowledgeTool tool = new KnowledgeTool(ragService);

        AiToolOutcome outcome = tool.searchKnowledge("不存在的问题");

        assertFalse(outcome.ok());
        assertEquals(RagPromptBuilder.NO_KNOWLEDGE_REPLY, outcome.message());
        assertNull(outcome.data());
    }

    @Test
    void search_shouldReturnUnavailableMessageWhenNotReady() {
        RagService ragService = mock(RagService.class);
        when(ragService.isReady()).thenReturn(false);
        KnowledgeTool tool = new KnowledgeTool(ragService);

        AiToolOutcome outcome = tool.searchKnowledge("篮球怎么保养");

        assertFalse(outcome.ok());
        assertTrue(outcome.message().contains("不可用"));
        verify(ragService, never()).retrieve(any());
    }

    @Test
    void search_shouldReturnFriendlyErrorOnFailure() {
        RagService ragService = mock(RagService.class);
        when(ragService.isReady()).thenReturn(true);
        when(ragService.retrieve(any())).thenThrow(
                new KnowledgeException(KnowledgeException.VECTOR_UNAVAILABLE, "向量检索失败"));
        KnowledgeTool tool = new KnowledgeTool(ragService);

        AiToolOutcome outcome = tool.searchKnowledge("篮球怎么保养");

        assertFalse(outcome.ok());
        assertTrue(outcome.message().contains("稍后重试"));
    }

    @SuppressWarnings("unchecked")
    private List<KnowledgeTool.Hit> cast(Object data) {
        return (List<KnowledgeTool.Hit>) data;
    }
}
