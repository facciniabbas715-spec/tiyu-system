import { del, get, post, put } from '../request'
import type { PageResult } from '../request'

export interface UserItem {
  id: number
  deptId: number | null
  deptName: string | null
  username: string
  nickname: string | null
  realName: string
  phone: string | null
  email: string | null
  gender: number
  userType: string | null
  status: number
  loginDate: string | null
  roleIds: number[]
}

export interface UserDTO {
  id?: number
  deptId?: number | null
  username: string
  password?: string
  nickname?: string
  realName: string
  phone?: string
  email?: string
  gender?: number
  userType?: string
  status?: number
  remark?: string
  roleIds?: number[]
}

export interface PageQuery {
  current: number
  size: number
  username?: string
  phone?: string
  status?: number
  deptId?: number
}

export const getUserPage = (params: PageQuery) =>
  get<PageResult<UserItem>>('/system/user/page', { params })

export const getUserDetail = (id: number) => get<UserItem>(`/system/user/${id}`)

export const addUser = (data: UserDTO) => post<void>('/system/user', data)

export const updateUser = (data: UserDTO) => put<void>('/system/user', data)

export const deleteUser = (id: number) => del<void>(`/system/user/${id}`)

export const resetPassword = (userId: number, password: string) =>
  put<void>('/system/user/resetPassword', { userId, password })

export const changeUserStatus = (userId: number, status: number) =>
  put<void>('/system/user/status', { userId, status })
