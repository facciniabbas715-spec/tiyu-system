import { del, get } from '../request'
import type { PageResult } from '../request'

export interface OperLogItem {
  id: number
  title: string
  businessType: number
  method: string
  requestMethod: string
  operName: string
  operUrl: string
  operIp: string
  operParam: string
  jsonResult: string
  status: number
  errorMsg: string | null
  costTime: number
  operTime: string
}

export const getOperLogPage = (params: { current: number; size: number; title?: string; operName?: string; status?: number }) =>
  get<PageResult<OperLogItem>>('/monitor/operlog/page', { params })

export const deleteOperLog = (id: number) => del<void>(`/monitor/operlog/${id}`)

export const cleanOperLog = () => del<void>('/monitor/operlog/clean')
