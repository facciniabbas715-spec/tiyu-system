package com.company.sportseq.knowledge;

import com.company.sportseq.knowledge.splitter.KnowledgeTextSplitter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文本切分单元测试：中文长文本按句子边界切块，短文本保持单块。
 */
class KnowledgeTextSplitterTest {

    private final KnowledgeTextSplitter splitter = new KnowledgeTextSplitter(400, 80);

    @Test
    void split_shouldChunkLongChineseTextIntoMultiplePieces() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            builder.append("篮球使用后应当及时清洁表面灰尘，避免尖锐物体划伤表皮。")
                    .append("篮球存放时应避免阳光暴晒与潮湿环境，防止变形与开胶。\n");
        }
        String text = builder.toString();

        List<String> chunks = splitter.split(text);

        assertTrue(chunks.size() > 1, "长文本应被切成多块，实际块数=" + chunks.size());
        for (String chunk : chunks) {
            assertTrue(chunk.length() > 0, "每个分块都应为非空文本");
        }
        long joined = chunks.stream().mapToLong(String::length).sum();
        assertTrue(joined > text.length() / 2, "切分不应大面积丢失原文内容");
    }

    @Test
    void split_shouldKeepShortTextAsSingleChunk() {
        String shortText = "篮球充气至标准气压后使用，避免过度充气导致爆裂。";

        List<String> chunks = splitter.split(shortText);

        assertEquals(1, chunks.size());
        assertEquals(shortText, chunks.get(0));
    }

    @Test
    void split_shouldReturnEmptyListForBlankText() {
        assertTrue(splitter.split("").isEmpty());
        assertTrue(splitter.split("   ").isEmpty());
    }
}
