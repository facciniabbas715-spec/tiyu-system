import { get, post, put } from '../request'
import type { PageResult } from '../request'

export interface ReturnItem {
  id: number
  borrowItemId: number
  equipmentCode: string
  equipmentName: string
  quantity: number
  conditionStatus: number
  damageDesc: string | null
  isOverdue: number
  overdueDays: number
  penaltyAmount: number
}

export interface ReturnOrder {
  id: number
  orderNo: string
  borrowOrderId: number
  borrowOrderNo: string
  username: string
  warehouseId: number
  warehouseName: string
  totalQuantity: number
  returnType: number
  status: number
  confirmRemark: string | null
  remark: string | null
  createTime: string
  items: ReturnItem[]
}

export const getReturnPage = (params: { current: number; size: number; orderNo?: string; status?: number }) =>
  get<PageResult<ReturnOrder>>('/return/page', { params })

export const getReturnDetail = (id: number) => get<ReturnOrder>(`/return/${id}`)

export const createReturn = (data: {
  borrowOrderId: number
  remark?: string
  items: { borrowItemId: number; quantity: number; conditionStatus: number; damageDesc?: string }[]
}) => post<void>('/return', data)

export const confirmReturn = (id: number) => put<void>(`/return/${id}/confirm`)

export const rejectReturn = (id: number) => put<void>(`/return/${id}/reject`)
