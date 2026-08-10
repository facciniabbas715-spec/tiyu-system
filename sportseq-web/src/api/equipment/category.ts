import { del, get, post, put } from '../request'

export interface CategoryItem {
  id: number
  parentId: number
  categoryName: string
  categoryCode: string
  icon: string | null
  sortOrder: number
  status: number
  children?: CategoryItem[]
}

export interface CategoryDTO {
  id?: number
  parentId?: number
  categoryName: string
  categoryCode: string
  icon?: string
  sortOrder?: number
  status?: number
  remark?: string
}

export const getCategoryTree = (categoryName?: string) =>
  get<CategoryItem[]>('/equipment/category/tree', { params: { categoryName } })

export const addCategory = (data: CategoryDTO) => post<void>('/equipment/category', data)

export const updateCategory = (data: CategoryDTO) => put<void>('/equipment/category', data)

export const deleteCategory = (id: number) => del<void>(`/equipment/category/${id}`)
