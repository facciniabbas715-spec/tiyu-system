import { del, get, post, put, type PageResult } from '../request'

export interface KnowledgeDocumentVO {
  id: number
  title: string
  fileName: string
  fileType: string
  fileSize: number
  status: number
  chunkCount: number
  errorMsg: string | null
  remark: string | null
  createTime: string
  updateTime: string | null
}

export interface KnowledgeChunkVO {
  id: number
  chunkIndex: number
  content: string
  tokenCount: number | null
}

export interface KnowledgeDocumentDetailVO extends KnowledgeDocumentVO {
  chunks: KnowledgeChunkVO[]
}

export interface SeedResultVO {
  imported: number
  skipped: number
}

export interface KnowledgePageQuery {
  current?: number
  size?: number
  keyword?: string
  fileType?: string
  status?: number
}

export const pageKnowledgeDocuments = (params: KnowledgePageQuery) =>
  get<PageResult<KnowledgeDocumentVO>>('/knowledge/documents/page', { params })

export const getKnowledgeDocument = (id: number) =>
  get<KnowledgeDocumentDetailVO>(`/knowledge/documents/${id}`)

export const uploadKnowledgeDocument = (formData: FormData) =>
  post<KnowledgeDocumentVO>('/knowledge/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000,
  })

export const updateKnowledgeDocument = (id: number, formData: FormData) =>
  put<KnowledgeDocumentVO>(`/knowledge/documents/${id}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000,
  })

export const deleteKnowledgeDocument = (id: number) =>
  del<void>(`/knowledge/documents/${id}`)

export const rebuildKnowledgeDocument = (id: number) =>
  post<KnowledgeDocumentVO>(`/knowledge/documents/${id}/rebuild`, undefined, {
    timeout: 120000,
  })

export const seedKnowledge = () =>
  post<SeedResultVO>('/knowledge/documents/seed', undefined, { timeout: 300000 })
