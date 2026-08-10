import { del, get, post, put } from '../request'
import type { PageResult } from '../request'

export interface StockInItem {
  id: number
  equipmentId: number
  equipmentCode: string
  equipmentName: string
  unit: string
  quantity: number
  unitPrice: number | null
  amount: number | null
  remark: string | null
}

export interface StockInOrder {
  id: number
  orderNo: string
  warehouseId: number
  warehouseName: string
  supplier: string | null
  inType: number
  totalQuantity: number
  totalAmount: number
  status: number
  auditBy: number | null
  auditTime: string | null
  auditRemark: string | null
  receiveBy: number | null
  receiveTime: string | null
  remark: string | null
  createTime: string
  items: StockInItem[] | null
}

export interface StockInDTO {
  warehouseId: number
  supplier?: string
  inType: number
  remark?: string
  items: { equipmentId: number; quantity: number; unitPrice?: number; remark?: string }[]
}

export const getStockInPage = (params: { current: number; size: number; orderNo?: string; status?: number }) =>
  get<PageResult<StockInOrder>>('/stock/in/page', { params })

export const getStockInDetail = (id: number) => get<StockInOrder>(`/stock/in/${id}`)

export const createStockIn = (data: StockInDTO) => post<void>('/stock/in', data)

export const submitStockIn = (id: number) => put<void>(`/stock/in/${id}/submit`)

export const auditStockIn = (orderId: number, pass: boolean, remark?: string) =>
  put<void>('/stock/in/audit', { orderId, pass, remark })

export const receiveStockIn = (id: number) => put<void>(`/stock/in/${id}/receive`)

export const cancelStockIn = (id: number) => put<void>(`/stock/in/${id}/cancel`)

export const deleteStockIn = (id: number) => del<void>(`/stock/in/${id}`)
