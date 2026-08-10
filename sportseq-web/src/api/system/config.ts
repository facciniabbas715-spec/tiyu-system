import { del, get, post, put } from '../request'
import type { PageResult } from '../request'

export interface ConfigItem {
  id: number
  configName: string
  configKey: string
  configValue: string
  configType: string
  remark: string | null
  createTime: string
}

export interface ConfigDTO {
  id?: number
  configName: string
  configKey: string
  configValue: string
  configType?: string
  remark?: string
}

export const getConfigPage = (params: { current: number; size: number; configName?: string; configKey?: string }) =>
  get<PageResult<ConfigItem>>('/system/config/page', { params })

export const getConfigByKey = (key: string) => get<string>(`/system/config/key/${key}`)

export const addConfig = (data: ConfigDTO) => post<void>('/system/config', data)

export const updateConfig = (data: ConfigDTO) => put<void>('/system/config', data)

export const deleteConfig = (id: number) => del<void>(`/system/config/${id}`)
