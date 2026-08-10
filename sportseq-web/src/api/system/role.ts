import { del, get, post, put } from '../request'
import type { PageResult } from '../request'

export interface RoleItem {
  id: number
  roleName: string
  roleKey: string
  roleSort: number
  dataScope: number
  status: number
  remark: string | null
  createTime: string
}

export interface RoleDTO {
  id?: number
  roleName: string
  roleKey: string
  roleSort?: number
  dataScope?: number
  status?: number
  remark?: string
  menuIds?: number[]
}

export const getRolePage = (params: { current: number; size: number; roleName?: string; status?: number }) =>
  get<PageResult<RoleItem>>('/system/role/page', { params })

export const addRole = (data: RoleDTO) => post<void>('/system/role', data)

export const updateRole = (data: RoleDTO) => put<void>('/system/role', data)

export const deleteRole = (id: number) => del<void>(`/system/role/${id}`)

export const getRoleMenuIds = (id: number) => get<number[]>(`/system/role/${id}/menus`)

export const assignRoleMenus = (id: number, menuIds: number[]) =>
  put<void>(`/system/role/${id}/menus`, menuIds)
