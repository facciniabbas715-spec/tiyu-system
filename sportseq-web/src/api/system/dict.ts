import { del, get, post, put } from '../request'
import type { PageResult } from '../request'

export interface DictTypeItem {
  id: number
  dictName: string
  dictType: string
  status: number
  remark: string | null
  createTime: string
}

export interface DictDataItem {
  id: number
  dictType: string
  dictSort: number
  dictLabel: string
  dictValue: string
  listClass: string | null
  isDefault: string
  status: number
}

export const getDictTypePage = (params: { current: number; size: number; dictName?: string; dictType?: string }) =>
  get<PageResult<DictTypeItem>>('/system/dict/type/page', { params })

export const addDictType = (data: { dictName: string; dictType: string; status?: number; remark?: string }) =>
  post<void>('/system/dict/type', data)

export const updateDictType = (data: { id: number; dictName: string; dictType: string; status?: number; remark?: string }) =>
  put<void>('/system/dict/type', data)

export const deleteDictType = (id: number) => del<void>(`/system/dict/type/${id}`)

export const getDictDataPage = (params: { current: number; size: number; dictType?: string }) =>
  get<PageResult<DictDataItem>>('/system/dict/data/page', { params })

export const getDictDataByType = (dictType: string) =>
  get<DictDataItem[]>(`/system/dict/data/type/${dictType}`)

export const addDictData = (data: Partial<DictDataItem>) => post<void>('/system/dict/data', data)

export const updateDictData = (data: Partial<DictDataItem> & { id: number }) =>
  put<void>('/system/dict/data', data)

export const deleteDictData = (id: number) => del<void>(`/system/dict/data/${id}`)
