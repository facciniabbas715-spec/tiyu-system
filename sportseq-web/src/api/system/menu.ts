import { del, get, post, put } from '../request'

export interface MenuItem {
  id: number
  parentId: number
  menuName: string
  orderNum: number
  menuType: 'M' | 'C' | 'F'
  path: string | null
  component: string | null
  perms: string | null
  icon: string | null
  visible: number
  status: number
  children?: MenuItem[]
}

export interface RouterNode {
  path: string
  component: string
  name: string
  meta: { title: string; icon: string | null }
  children: RouterNode[] | null
}

export interface MenuDTO {
  id?: number
  parentId: number
  menuName: string
  orderNum?: number
  menuType: 'M' | 'C' | 'F'
  path?: string
  component?: string
  query?: string
  perms?: string
  icon?: string
  visible?: number
  status?: number
}

export const getMenuTree = (menuName?: string) =>
  get<MenuItem[]>('/system/menu/tree', { params: { menuName } })

export const getRouters = () => get<RouterNode[]>('/system/menu/routers')

export const addMenu = (data: MenuDTO) => post<void>('/system/menu', data)

export const updateMenu = (data: MenuDTO) => put<void>('/system/menu', data)

export const deleteMenu = (id: number) => del<void>(`/system/menu/${id}`)
