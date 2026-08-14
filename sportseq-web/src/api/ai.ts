import { del, get, post } from './request'

export interface AiChatDTO {
  message: string
}

export interface AiChatVO {
  content: string
  debug: AiDebugVO | null
}

export interface RagHitVO {
  documentId: number | null
  title: string | null
  chunkIndex: number | null
  content: string
  similarity: number | null
}

export interface AiToolCallVO {
  name: string
  arguments: string
  result: string
}

export interface AiDebugVO {
  query: string
  intent: string
  toolCalls: AiToolCallVO[]
  knowledgeUsed: boolean
  hits: RagHitVO[]
  answer: string
}

export interface AiHistoryMessage {
  role: 'user' | 'assistant'
  content: string
}

/**
 * AI 智能客服对话。模型生成可能耗时较长，单独放宽超时时间。
 */
export const sendChatMessage = (data: AiChatDTO) =>
  post<AiChatVO>('/ai/chat', data, { timeout: 60000 })

/** 拉取当前用户的多轮对话历史。 */
export const getAiHistory = () => get<AiHistoryMessage[]>('/ai/history')

/** 清空当前用户的多轮对话历史。 */
export const clearAiHistory = () => del<void>('/ai/history')
