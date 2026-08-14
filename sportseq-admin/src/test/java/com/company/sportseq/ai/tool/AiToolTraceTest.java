package com.company.sportseq.ai.tool;

import com.company.sportseq.ai.vo.AiToolCallVO;
import com.company.sportseq.knowledge.vo.RagHitVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工具调用链路追踪单元测试：仅调试开启时收集，end 后清理状态。
 */
class AiToolTraceTest {

    @Test
    void shouldNotCollectWhenDisabled() {
        AiToolTrace.begin(false);
        try {
            AiToolTrace.recordCall("getEquipmentStock", "{}", "result");
            AiToolTrace.recordKnowledgeHits(List.of(new RagHitVO(1L, "标题", 0, "内容", 0.9)));

            assertTrue(AiToolTrace.currentCalls().isEmpty());
            assertTrue(AiToolTrace.currentKnowledgeHits().isEmpty());
            assertFalse(AiToolTrace.knowledgeUsed());
        } finally {
            AiToolTrace.end();
        }
    }

    @Test
    void shouldCollectCallsAndKnowledgeHitsWhenEnabled() {
        AiToolTrace.begin(true);
        try {
            AiToolTrace.recordCall("searchKnowledge", "{\"query\":\"篮球怎么保养\"}", "{\"ok\":true}");
            AiToolTrace.recordKnowledgeHits(List.of(new RagHitVO(5L, "篮球知识", 1, "内容", 0.83)));

            List<AiToolCallVO> calls = AiToolTrace.currentCalls();
            assertEquals(1, calls.size());
            assertEquals("searchKnowledge", calls.get(0).name());
            assertEquals("{\"query\":\"篮球怎么保养\"}", calls.get(0).arguments());
            assertTrue(AiToolTrace.knowledgeUsed());
            assertEquals("篮球知识", AiToolTrace.currentKnowledgeHits().get(0).title());
        } finally {
            AiToolTrace.end();
        }
    }

    @Test
    void endShouldClearState() {
        AiToolTrace.begin(true);
        AiToolTrace.recordCall("searchEquipment", "{}", "result");

        AiToolTrace.end();

        assertTrue(AiToolTrace.currentCalls().isEmpty());
        assertTrue(AiToolTrace.currentKnowledgeHits().isEmpty());
        assertFalse(AiToolTrace.knowledgeUsed());
    }
}
