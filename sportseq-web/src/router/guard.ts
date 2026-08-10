import type { Router } from 'vue-router'
import { getToken } from '@/utils/auth'

const WHITE_LIST = ['/login']

export function setupRouterGuard(router: Router): void {
  router.beforeEach((to) => {
    const token = getToken()
    if (token) {
      if (to.path === '/login') {
        return { path: '/dashboard' }
      }
      return true
    }
    if (WHITE_LIST.includes(to.path)) {
      return true
    }
    return { path: '/login', query: { redirect: to.fullPath } }
  })
}
