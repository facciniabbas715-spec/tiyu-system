import { get, post, put } from '../request'
import type { PageResult } from '../request'

export interface ScrapItem {
  id: number
  equipmentId: number
  equipmentCode: string
  equipmentName: string
  unit: string
  quantity: number
  scrapReason: string
  lossAmount: number
}

export interface ScrapOrder {
  id: number
  orderNo: string
  warehouseId: number
  warehouseName: string
  scrapType: number
  totalQuantity: number
  totalLossAmount: number
  status: number
  auditRemark: string | null
  disposeMethod: number | null
  remark: string | null
  createTime: string
  items: ScrapItem[]
}

export const getScrapPage = (params: { current: number; size: number; orderNo?: string; status?: number }) =>
  get<PageResult<ScrapOrder>>('/scrap/page', { params })

export const getScrapDetail = (id: number) => get<ScrapOrder>(`/scrap/${id}`)

export const createScrap = (data: {
  warehouseId: number
  scrapType: number
  remark?: string
  items: { equipmentId: number; quantity: number; scrapReason: string }[]
}) => post<void>('/scrap', data)

export const auditScrap = (orderId: number, pass: boolean, remark?: string) =>
  put<void>('/scrap/audit', { orderId, pass, remark })

export const disposeScrap = (orderId: number, disposeMethod: number, remark?: string) =>
  put<void>('/scrap/dispose', { orderId, disposeMethod, remark })

export const cancelScrap = (id: number) => put<void>(`/scrap/${id}/cancel`)
