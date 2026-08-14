import { post } from './request'

export interface AiChatDTO {
  message: string
}

export interface AiChatVO {
  content: string
}

/**
 * AI 智能客服对话。模型生成可能耗时较长，单独放宽超时时间。
 */
export const sendChatMessage = (data: AiChatDTO) =>
  post<AiChatVO>('/ai/chat', data, { timeout: 60000 })
