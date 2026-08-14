import type { Router } from 'vue-router'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/store/modules/user'
import { usePermissionStore } from '@/store/modules/permission'

const WHITE_LIST = ['/login']

export function setupRouterGuard(router: Router): void {
  router.beforeEach(async (to) => {
    const token = getToken()
    if (token) {
      if (to.path === '/login') {
        return { path: '/statistics/dashboard' }
      }
      const userStore = useUserStore()
      const permissionStore = usePermissionStore()
      if (!userStore.userInfo) {
        try {
          await userStore.fetchInfo()
        } catch {
          userStore.reset()
          permissionStore.reset()
          return { path: '/login' }
        }
      }
      if (!permissionStore.loaded) {
        try {
          const dynamicRoutes = await permissionStore.generateRoutes()
          dynamicRoutes.forEach((route) => router.addRoute('Root', route))
          // 只按 path 重新导航，避免把 404 路由的 name 一起带回导致仍解析到 404
          return { path: to.path, query: to.query, hash: to.hash, replace: true }
        } catch {
          userStore.reset()
          permissionStore.reset()
          return { path: '/login' }
        }
      }
      return true
    }
    if (WHITE_LIST.includes(to.path)) {
      return true
    }
    return { path: '/login', query: { redirect: to.fullPath } }
  })
}
