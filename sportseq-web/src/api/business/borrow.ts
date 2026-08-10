import { get, post, put } from '../request'
import type { PageResult } from '../request'

export interface BorrowItem {
  id: number
  equipmentId: number
  equipmentCode: string
  equipmentName: string
  unit: string
  warehouseId: number
  warehouseName: string
  quantity: number
  issuedQuantity: number
  returnedQuantity: number
  expectedReturnDate: string
  overdueFlag: number
  overdueDays: number
  status: number
}

export interface BorrowOrder {
  id: number
  orderNo: string
  userId: number
  username: string
  realName: string
  borrowType: number
  purpose: string
  expectedReturnDate: string
  totalQuantity: number
  status: number
  extendCount: number
  auditRemark: string | null
  remark: string | null
  createTime: string
  items: BorrowItem[]
}

export const getBorrowPage = (params: {
  current: number
  size: number
  orderNo?: string
  username?: string
  status?: number
  borrowUserId?: number
}) => get<PageResult<BorrowOrder>>('/borrow/page', { params })

export const getBorrowDetail = (id: number) => get<BorrowOrder>(`/borrow/${id}`)

export const createBorrow = (data: {
  borrowType: number
  purpose: string
  expectedReturnDate: string
  remark?: string
  items: { equipmentId: number; warehouseId: number; quantity: number }[]
}) => post<void>('/borrow', data)

export const auditBorrow = (orderId: number, pass: boolean, remark?: string) =>
  put<void>('/borrow/audit', { orderId, pass, remark })

export const issueBorrow = (id: number) => put<void>(`/borrow/${id}/issue`)

export const extendBorrow = (orderId: number, days: number) =>
  put<void>('/borrow/extend', { orderId, days })

export const cancelBorrow = (id: number) => put<void>(`/borrow/${id}/cancel`)
