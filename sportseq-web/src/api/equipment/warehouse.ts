import { del, get, post, put } from '../request'

export interface WarehouseItem {
  id: number
  warehouseCode: string
  warehouseName: string
  manager: number | null
  phone: string | null
  address: string | null
  status: number
  remark: string | null
}

export interface WarehouseDTO {
  id?: number
  warehouseCode: string
  warehouseName: string
  manager?: number
  phone?: string
  address?: string
  status?: number
  remark?: string
}

export const getWarehouseList = () => get<WarehouseItem[]>('/equipment/warehouse/list')

export const addWarehouse = (data: WarehouseDTO) => post<void>('/equipment/warehouse', data)

export const updateWarehouse = (data: WarehouseDTO) => put<void>('/equipment/warehouse', data)

export const deleteWarehouse = (id: number) => del<void>(`/equipment/warehouse/${id}`)
