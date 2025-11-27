// Token管理工具类

import {removeUserInfo} from "@/utils/user.ts";

/**
 * 保存token到localStorage
 * @param accessToken JWT访问令牌
 * @param refreshToken JWT刷新令牌
 */
export const saveTokens = (accessToken: string, refreshToken: string): void => {
  localStorage.setItem('accessToken', accessToken)
  localStorage.setItem('refreshToken', refreshToken)
}

/**
 * 保存访问令牌到localStorage
 * @param token JWT访问令牌
 */
export const saveAccessToken = (token: string): void => {
  localStorage.setItem('accessToken', token)
}

/**
 * 从localStorage获取访问令牌
 * @returns 访问令牌字符串或null
 */
export const getAccessToken = (): string | null => {
  return localStorage.getItem('accessToken')
}

/**
 * 从localStorage获取刷新令牌
 * @returns 刷新令牌字符串或null
 */
export const getRefreshToken = (): string | null => {
  return localStorage.getItem('refreshToken')
}

/**
 * 从localStorage移除token
 */
export const removeTokens = (): void => {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
}

/**
 * 检查用户是否已登录
 * @returns boolean
 */
export const isLoggedIn = (): boolean => {
  return !!getAccessToken()
}

/**
 * 登出用户，清除所有认证信息
 */
export const logout = (): void => {
  removeTokens()
  removeUserInfo()
}