import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import { getRouters, type RouterNode } from '@/api/system/menu'
import Layout from '@/layout/index.vue'

const viewModules = import.meta.glob('@/views/**/*.vue')

function resolveComponent(component: string) {
  if (component === 'Layout') {
    return Layout
  }
  const key = `/src/views/${component}.vue`
  const loader = viewModules[key]
  if (!loader) {
    console.warn(`[permission] 找不到组件: ${key}`)
    return Layout
  }
  return loader
}

function buildRoutes(nodes: RouterNode[], parentPath = ''): RouteRecordRaw[] {
  return nodes.map((node) => {
    const path = parentPath ? `${parentPath}/${node.path}`.replace(/\/+/g, '/') : node.path
    // 目录路由（component=Layout）挂在 Root 布局下，不再重复嵌套 Layout，
    // 否则会出现“布局套布局”（页面内又渲染一套侧边栏）。
    if (node.component && node.component !== 'Layout') {
      return {
        path,
        name: node.name,
        component: resolveComponent(node.component),
        meta: { title: node.meta.title, icon: node.meta.icon },
        children: node.children ? buildRoutes(node.children, path) : undefined,
      } as RouteRecordRaw
    }
    return {
      path,
      name: node.name,
      meta: { title: node.meta.title, icon: node.meta.icon },
      children: node.children ? buildRoutes(node.children, path) : undefined,
    } as RouteRecordRaw
  })
}

export const usePermissionStore = defineStore('permission', () => {
  const routes = ref<RouteRecordRaw[]>([])
  const loaded = ref(false)

  async function generateRoutes(): Promise<RouteRecordRaw[]> {
    const nodes = await getRouters()
    const dynamicRoutes = buildRoutes(nodes)
    routes.value = dynamicRoutes
    loaded.value = true
    return dynamicRoutes
  }

  function reset(): void {
    routes.value = []
    loaded.value = false
  }

  return { routes, loaded, generateRoutes, reset }
})
