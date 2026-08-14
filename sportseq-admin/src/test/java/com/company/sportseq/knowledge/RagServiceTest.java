package com.company.sportseq.knowledge;

import com.company.sportseq.knowledge.config.RagProperties;
import com.company.sportseq.knowledge.service.RagPromptBuilder;
import com.company.sportseq.knowledge.service.RagService;
import com.company.sportseq.knowledge.service.impl.RagServiceImpl;
import com.company.sportseq.knowledge.vector.VectorStoreService;
import com.company.sportseq.knowledge.vo.RagHitVO;
import com.company.sportseq.knowledge.vo.RagResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RAG 服务单元测试：无命中不调模型、有命中接地回答、debug 开关。
 */
class RagServiceTest {

    private static final String QUESTION = "篮球应该怎么保养？";

    private static final RagHitVO HIT = new RagHitVO(5L, "篮球知识", 2,
            "篮球使用后应清洁表面灰尘，存放于干燥阴凉处，避免暴晒与尖锐物划伤。", 0.83);

    @Test
    void answer_shouldReturnCannedMessageWhenNoHit() {
        VectorStoreService vectorStore = mock(VectorStoreService.class);
        when(vectorStore.search(QUESTION)).thenReturn(List.of());
        ChatModel model = mock(ChatModel.class);
        RagService service = new RagServiceImpl(vectorStore, ragClient(model),
                new RagPromptBuilder(), properties(false));

        RagResult result = service.answer(QUESTION);

        assertEquals(RagPromptBuilder.NO_KNOWLEDGE_REPLY, result.content());
        assertNull(result.debug());
        verify(model, org.mockito.Mockito.never()).call(any(Prompt.class));
    }

    @Test
    void answer_shouldGroundAnswerOnRetrievedChunks() {
        VectorStoreService vectorStore = mock(VectorStoreService.class);
        when(vectorStore.search(QUESTION)).thenReturn(List.of(HIT));
        ChatModel model = mock(ChatModel.class);
        when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(
                List.of(new Generation(new AssistantMessage("篮球应清洁表面并存放于干燥阴凉处。")))));
        RagService service = new RagServiceImpl(vectorStore, ragClient(model),
                new RagPromptBuilder(), properties(false));

        RagResult result = service.answer(QUESTION);

        assertEquals("篮球应清洁表面并存放于干燥阴凉处。", result.content());
        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(model).call(captor.capture());
        List<Message> messages = captor.getValue().getInstructions();
        assertTrue(messages.get(0).getText().contains("只能依据"),
                "系统提示词应包含防幻觉约束");
        assertTrue(messages.get(1).getText().contains("篮球使用后应清洁表面灰尘"),
                "用户消息应注入检索到的知识片段");
        assertTrue(messages.get(1).getText().contains(QUESTION));
    }

    @Test
    void answer_shouldIncludeDebugOnlyWhenEnabled() {
        VectorStoreService vectorStore = mock(VectorStoreService.class);
        when(vectorStore.search(QUESTION)).thenReturn(List.of(HIT));
        ChatModel model = mock(ChatModel.class);
        when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(
                List.of(new Generation(new AssistantMessage("答案")))));
        RagService service = new RagServiceImpl(vectorStore, ragClient(model),
                new RagPromptBuilder(), properties(true));

        RagResult result = service.answer(QUESTION);

        assertEquals(QUESTION, result.debug().query());
        assertTrue(result.debug().knowledgeUsed());
        assertEquals(1, result.debug().hits().size());
        assertEquals(0.83, result.debug().hits().get(0).similarity());
        assertEquals("答案", result.debug().answer());
    }

    @Test
    void retrieve_shouldDelegateToVectorStoreSearch() {
        VectorStoreService vectorStore = mock(VectorStoreService.class);
        when(vectorStore.search(QUESTION)).thenReturn(List.of(HIT));
        RagService service = new RagServiceImpl(vectorStore, mock(ChatClient.class),
                new RagPromptBuilder(), properties(false));

        List<RagHitVO> hits = service.retrieve(QUESTION);

        assertEquals(1, hits.size());
        assertEquals(HIT, hits.get(0));
        verify(vectorStore).search(QUESTION);
    }

    @Test
    void isReady_shouldRequireEnabledAndApiKey() {
        RagService service = new RagServiceImpl(mock(VectorStoreService.class),
                mock(ChatClient.class), new RagPromptBuilder(), properties(false));
        assertFalse(service.isReady(), "未配置 Embedding Key 时应不可用");

        RagProperties ready = properties(false);
        ready.getEmbedding().setApiKey("sk-test");
        RagService readyService = new RagServiceImpl(mock(VectorStoreService.class),
                mock(ChatClient.class), new RagPromptBuilder(), ready);
        assertTrue(readyService.isReady());

        ready.setEnabled(false);
        assertFalse(readyService.isReady(), "总开关关闭时应不可用");
    }

    private RagProperties properties(boolean debugEnabled) {
        RagProperties props = new RagProperties();
        props.setDebugEnabled(debugEnabled);
        props.setTopK(4);
        props.setSimilarityThreshold(0.7);
        props.getEmbedding().setApiKey("");
        return props;
    }

    private ChatClient ragClient(ChatModel model) {
        return ChatClient.builder(model)
                .defaultSystem(RagPromptBuilder.RAG_SYSTEM_PROMPT)
                .build();
    }
}
