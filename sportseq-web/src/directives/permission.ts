import type { Directive } from 'vue'
import { useUserStore } from '@/store/modules/user'

/**
 * 按钮权限指令：v-permission="['system:user:add']"
 * 无权限时移除元素（后端接口仍有 @PreAuthorize 兜底）。
 */
export const permission: Directive<HTMLElement, string[]> = {
  mounted(el, binding) {
    const { value } = binding
    if (!value || !Array.isArray(value) || value.length === 0) {
      return
    }
    const userStore = useUserStore()
    const hasPermission = userStore.permissions.some(
      (p) => value.includes(p) || p === '*:*:*',
    )
    if (!hasPermission) {
      el.parentNode?.removeChild(el)
    }
  },
}
