import { get } from './request'

export interface CategoryStockItem {
  categoryName: string
  quantity: number
}

export interface WarehouseStockItem {
  warehouseName: string
  quantity: number
}

export interface BorrowTrendItem {
  month: string
  borrowQuantity: number
  returnQuantity: number
}

export interface EquipmentUsageItem {
  equipmentCode: string
  equipmentName: string
  categoryName: string | null
  borrowCount: number
}

export interface DeptBorrowItem {
  deptName: string
  borrowCount: number
  borrowQuantity: number
  outstandingQuantity: number
}

export interface OverdueStat {
  overdueOrderCount: number
  overdueItemCount: number
  avgOverdueDays: number
  penaltyTotal: number
}

export interface OverdueItem {
  returnOrderNo: string
  borrowOrderNo: string
  equipmentCode: string
  equipmentName: string
  quantity: number
  overdueDays: number
  penaltyAmount: number
  confirmTime: string
}

export interface RangeParams {
  startDate?: string
  endDate?: string
}

export const getCategoryStock = () => get<CategoryStockItem[]>('/statistics/category-stock')

export const getWarehouseStock = () => get<WarehouseStockItem[]>('/statistics/warehouse-stock')

export const getBorrowTrend = (params: RangeParams = {}) =>
  get<BorrowTrendItem[]>('/statistics/borrow-trend', { params })

export const getEquipmentUsage = (params: RangeParams = {}) =>
  get<EquipmentUsageItem[]>('/statistics/equipment-usage', { params })

export const getDeptBorrowStats = (params: RangeParams = {}) =>
  get<DeptBorrowItem[]>('/statistics/dept-borrow', { params })

export const getOverdueStats = (params: RangeParams = {}) =>
  get<OverdueStat>('/statistics/overdue', { params })

export const getOverdueItems = (params: RangeParams = {}) =>
  get<OverdueItem[]>('/statistics/overdue/items', { params })

export const statisticsExportUrl = (type: string, params: RangeParams = {}) => {
  const query = new URLSearchParams({
    type,
    ...(Object.fromEntries(
      Object.entries(params).filter(([, v]) => v !== undefined && v !== ''),
    ) as Record<string, string>),
  })
  return `/statistics/export?${query.toString()}`
}
