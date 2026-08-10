import { get, post, put } from './request'

export interface CaptchaVO {
  uuid: string
  img: string
}

export interface UserInfoVO {
  userId: number
  username: string
  realName: string
  avatar: string | null
  userType: string | null
  permissions: string[]
  roles: string[]
}

export interface LoginVO {
  token: string
  user: UserInfoVO
}

export interface LoginDTO {
  username: string
  password: string
  code: string
  uuid: string
}

export const getCaptcha = () => get<CaptchaVO>('/auth/captcha')

export const login = (data: LoginDTO) => post<LoginVO>('/auth/login', data)

export const logout = () => post<void>('/auth/logout')

export const getInfo = () => get<UserInfoVO>('/auth/info')

export const updatePassword = (data: { oldPassword: string; newPassword: string }) =>
  put<void>('/auth/updatePassword', data)
