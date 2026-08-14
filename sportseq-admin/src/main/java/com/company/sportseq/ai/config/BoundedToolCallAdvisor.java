package com.company.sportseq.ai.config;

import com.company.sportseq.ai.exception.AiToolCallLimitException;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.util.Assert;

/**
 * 有界工具循环 Advisor：Spring AI 1.1.8 的 {@link ToolCallAdvisor} 循环无轮数上限，
 * 本类在每个请求线程内计数，超过配置轮数时强制终止，防止异常模型无限调用工具。
 */
public class BoundedToolCallAdvisor extends ToolCallAdvisor {

    private static final ThreadLocal<Integer> ITERATIONS = new ThreadLocal<>();

    private final int maxIterations;

    public BoundedToolCallAdvisor(ToolCallingManager toolCallingManager, int advisorOrder, int maxIterations) {
        super(toolCallingManager, advisorOrder);
        Assert.isTrue(maxIterations > 0, "maxIterations must be positive");
        this.maxIterations = maxIterations;
    }

    @Override
    protected ChatClientRequest doInitializeLoop(ChatClientRequest chatClientRequest,
                                                 CallAdvisorChain callAdvisorChain) {
        ITERATIONS.set(0);
        return super.doInitializeLoop(chatClientRequest, callAdvisorChain);
    }

    @Override
    protected ChatClientRequest doBeforeCall(ChatClientRequest chatClientRequest,
                                             CallAdvisorChain callAdvisorChain) {
        int current = ITERATIONS.get() == null ? 0 : ITERATIONS.get();
        if (current >= maxIterations) {
            ITERATIONS.remove();
            throw new AiToolCallLimitException();
        }
        ITERATIONS.set(current + 1);
        return super.doBeforeCall(chatClientRequest, callAdvisorChain);
    }

    @Override
    protected ChatClientResponse doFinalizeLoop(ChatClientResponse chatClientResponse,
                                                CallAdvisorChain callAdvisorChain) {
        ITERATIONS.remove();
        return super.doFinalizeLoop(chatClientResponse, callAdvisorChain);
    }
}
