import { get } from './request'

export interface DashboardSummary {
  todayBorrowCount: number
  todayReturnCount: number
  todayStockInCount: number
  warningStockCount: number
  pendingScrapCount: number
  equipmentTotal: number
  stockTotalValue: number
}

export const getDashboardSummary = () => get<DashboardSummary>('/dashboard/summary')
