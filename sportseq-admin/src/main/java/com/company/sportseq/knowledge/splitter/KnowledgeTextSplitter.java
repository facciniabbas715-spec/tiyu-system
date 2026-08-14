package com.company.sportseq.knowledge.splitter;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.List;

/**
 * 知识库文本切分：封装 Spring AI {@link TokenTextSplitter}。
 *
 * <p>按 token 数（默认 400）切块，并优先在英文/中文句末标点与换行处断句，
 * 避免把完整句子拦腰截断，提升检索片段的可读性与回答质量。</p>
 */
public class KnowledgeTextSplitter {

    /** 中英文混排文档常用断句标点。 */
    private static final List<Character> PUNCTUATION_MARKS =
            List.of('.', '?', '!', '\n', '。', '？', '！', '；');

    private final TokenTextSplitter delegate;

    public KnowledgeTextSplitter(int chunkSize, int minChunkSizeChars) {
        this.delegate = TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .withMinChunkSizeChars(minChunkSizeChars)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .withPunctuationMarks(PUNCTUATION_MARKS)
                .build();
    }

    /**
     * 把一段文本切成若干片段。
     *
     * @param text 原文
     * @return 片段列表（空文本返回空列表）
     */
    public List<String> split(String text) {
        return delegate.split(List.of(new org.springframework.ai.document.Document(text)))
                .stream()
                .map(org.springframework.ai.document.Document::getText)
                .filter(chunk -> chunk != null && !chunk.isBlank())
                .toList();
    }
}
