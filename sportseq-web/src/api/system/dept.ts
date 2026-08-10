import { del, get, post, put } from '../request'

export interface DeptItem {
  id: number
  parentId: number
  deptName: string
  orderNum: number
  leader: string | null
  phone: string | null
  email: string | null
  status: number
  children?: DeptItem[]
}

export interface DeptDTO {
  id?: number
  parentId: number
  deptName: string
  orderNum?: number
  leader?: string
  phone?: string
  email?: string
  status?: number
}

export const getDeptTree = (deptName?: string) =>
  get<DeptItem[]>('/system/dept/tree', { params: { deptName } })

export const addDept = (data: DeptDTO) => post<void>('/system/dept', data)

export const updateDept = (data: DeptDTO) => put<void>('/system/dept', data)

export const deleteDept = (id: number) => del<void>(`/system/dept/${id}`)
