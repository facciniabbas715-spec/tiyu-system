import { get, put } from '../request'
import type { PageResult } from '../request'

export interface StockItem {
  equipmentId: number
  equipmentCode: string
  equipmentName: string
  categoryName: string | null
  unit: string | null
  warehouseId: number
  warehouseName: string
  quantity: number
  lockedQuantity: number
  safeStock: number
  warning: boolean
}

export interface StockRecordItem {
  id: number
  equipmentCode: string
  equipmentName: string
  warehouseName: string
  changeType: number
  changeQuantity: number
  beforeQuantity: number
  afterQuantity: number
  refOrderNo: string | null
  remark: string | null
  createTime: string
}

export interface StockAdjustDTO {
  equipmentId: number
  warehouseId: number
  changeQuantity: number
  remark?: string
}

export const getStockPage = (params: {
  current: number
  size: number
  equipmentName?: string
  equipmentCode?: string
  warehouseId?: number
  categoryId?: number
  warningOnly?: boolean
}) => get<PageResult<StockItem>>('/stock/page', { params })

export const getStockRecordPage = (params: {
  current: number
  size: number
  equipmentId?: number
  warehouseId?: number
  changeType?: number
}) => get<PageResult<StockRecordItem>>('/stock/record/page', { params })

export const getStockWarnings = () => get<StockItem[]>('/stock/warning')

export const adjustStock = (data: StockAdjustDTO) => put<void>('/stock/adjust', data)
