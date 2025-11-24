// Token管理工具类

import {removeUserInfo} from "@/utils/user.ts";

/**
 * 保存token到localStorage
 * @param token JWT token
 */
export const saveToken = (token: string): void => {
  localStorage.setItem('accessToken', token)
}

/**
 * 从localStorage获取token
 * @returns token字符串或null
 */
export const getToken = (): string | null => {
  return localStorage.getItem('accessToken')
}

/**
 * 从localStorage移除token
 */
export const removeToken = (): void => {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
}

/**
 * 检查用户是否已登录
 * @returns boolean
 */
export const isLoggedIn = (): boolean => {
  return !!getToken()
}

/**
 * 登出用户，清除所有认证信息
 */
export const logout = (): void => {
  removeToken()
  removeUserInfo()
}