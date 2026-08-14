package com.company.sportseq.ai.service;

import com.company.sportseq.ai.dto.AiChatDTO;
import com.company.sportseq.ai.vo.AiChatVO;

/**
 * AI 智能客服服务。下一阶段 RAG 只在本包内扩展，不侵入器材业务。
 */
public interface AiChatService {

    AiChatVO chat(AiChatDTO dto);
}
