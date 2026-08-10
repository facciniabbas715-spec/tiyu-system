import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getToken, removeToken, setToken } from '@/utils/auth'
import {
  getInfo,
  login as loginApi,
  logout as logoutApi,
  type LoginDTO,
  type UserInfoVO,
} from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(getToken())
  const userInfo = ref<UserInfoVO | null>(null)
  const permissions = ref<string[]>([])
  const roles = ref<string[]>([])

  async function login(dto: LoginDTO): Promise<void> {
    const data = await loginApi(dto)
    token.value = data.token
    setToken(data.token)
  }

  async function fetchInfo(): Promise<UserInfoVO> {
    const info = await getInfo()
    userInfo.value = info
    permissions.value = info.permissions
    roles.value = info.roles
    return info
  }

  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } finally {
      reset()
    }
  }

  function reset(): void {
    token.value = null
    userInfo.value = null
    permissions.value = []
    roles.value = []
    removeToken()
  }

  return { token, userInfo, permissions, roles, login, fetchInfo, logout, reset }
})
