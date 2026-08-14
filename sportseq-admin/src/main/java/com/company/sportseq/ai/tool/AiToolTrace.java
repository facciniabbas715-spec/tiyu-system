package com.company.sportseq.ai.tool;

import com.company.sportseq.ai.vo.AiToolCallVO;
import com.company.sportseq.knowledge.vo.RagHitVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 单次对话的工具调用链路追踪（ThreadLocal）：
 * 由对话编排 begin/end，权限回调记录每次调用，知识工具记录命中片段；
 * 仅调试开启时收集，避免生产环境保留内部数据。
 */
public final class AiToolTrace {

    private static final ThreadLocal<Holder> HOLDER = new ThreadLocal<>();

    private AiToolTrace() {
    }

    public static void begin(boolean enabled) {
        HOLDER.set(new Holder(enabled));
    }

    public static void recordCall(String name, String arguments, String result) {
        Holder holder = HOLDER.get();
        if (holder != null && holder.enabled) {
            holder.calls.add(new AiToolCallVO(name, arguments, result));
        }
    }

    public static void recordKnowledgeHits(List<RagHitVO> hits) {
        Holder holder = HOLDER.get();
        if (holder != null && holder.enabled) {
            holder.knowledgeHits.addAll(hits);
        }
    }

    public static List<AiToolCallVO> currentCalls() {
        Holder holder = HOLDER.get();
        return holder == null ? List.of() : List.copyOf(holder.calls);
    }

    public static List<RagHitVO> currentKnowledgeHits() {
        Holder holder = HOLDER.get();
        return holder == null ? List.of() : List.copyOf(holder.knowledgeHits);
    }

    public static boolean knowledgeUsed() {
        Holder holder = HOLDER.get();
        return holder != null && !holder.knowledgeHits.isEmpty();
    }

    public static void end() {
        HOLDER.remove();
    }

    private static final class Holder {

        private final boolean enabled;

        private final List<AiToolCallVO> calls = new ArrayList<>();

        private final List<RagHitVO> knowledgeHits = new ArrayList<>();

        Holder(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
