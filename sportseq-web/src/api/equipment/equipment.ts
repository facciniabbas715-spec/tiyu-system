import { del, get, post, put } from '../request'
import type { PageResult } from '../request'

export interface EquipmentItem {
  id: number
  equipmentCode: string
  equipmentName: string
  categoryId: number
  categoryName: string | null
  brand: string | null
  model: string | null
  spec: string | null
  unit: string
  purchasePrice: number | null
  safeStock: number
  maxBorrowDays: number | null
  imageUrl: string | null
  status: number
  description: string | null
  createTime: string
}

export interface EquipmentDTO {
  id?: number
  equipmentName: string
  categoryId: number
  brand?: string
  model?: string
  spec?: string
  unit: string
  purchasePrice?: number
  safeStock?: number
  maxBorrowDays?: number
  imageUrl?: string
  status?: number
  description?: string
}

export interface ImportResult {
  successCount: number
  failCount: number
  errors: string[]
}

export const getEquipmentPage = (params: {
  current: number
  size: number
  equipmentName?: string
  equipmentCode?: string
  categoryId?: number
  status?: number
}) => get<PageResult<EquipmentItem>>('/equipment/page', { params })

export const getEquipmentDetail = (id: number) => get<EquipmentItem>(`/equipment/${id}`)

export const addEquipment = (data: EquipmentDTO) => post<void>('/equipment', data)

export const updateEquipment = (data: EquipmentDTO) => put<void>('/equipment', data)

export const deleteEquipment = (id: number) => del<void>(`/equipment/${id}`)

export const changeEquipmentStatus = (id: number, status: number) =>
  put<void>(`/equipment/${id}/status?status=${status}`)

export const importEquipment = (file: File) => {
  const form = new FormData()
  form.append('file', file)
  return post<ImportResult>('/equipment/import', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const exportEquipmentUrl = (params: {
  equipmentName?: string
  equipmentCode?: string
  categoryId?: number
  status?: number
}) => `/equipment/export?${new URLSearchParams(
  Object.entries(params).filter(([, v]) => v !== undefined && v !== '') as [string, string][],
).toString()}`
