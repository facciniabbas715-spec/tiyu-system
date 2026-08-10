import { del, get } from '../request'
import type { PageResult } from '../request'

export interface LoginLogItem {
  id: number
  userName: string
  ipaddr: string
  loginLocation: string | null
  browser: string | null
  os: string | null
  status: number
  msg: string
  loginTime: string
}

export const getLoginLogPage = (params: { current: number; size: number; userName?: string; status?: number }) =>
  get<PageResult<LoginLogItem>>('/monitor/loginlog/page', { params })

export const cleanLoginLog = () => del<void>('/monitor/loginlog/clean')
