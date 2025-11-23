import { defineStore } from 'pinia'
import { getUserInfo, saveUserInfo, removeUserInfo, updateUserInfo, isUserLoggedIn } from '../utils/user'
import type { UserInfo, UserSettings } from '../utils/user'

interface UserState {
  userInfo: UserInfo | null
  userSettings: UserSettings | null
  isAuthenticated: boolean
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    userInfo: null,
    userSettings: null,
    isAuthenticated: false
  }),

  getters: {
    // 获取用户信息
    getUserInfo(state): UserInfo | null {
      return state.userInfo
    },

    // 获取用户设置
    getUserSettings(state): UserSettings | null {
      return state.userSettings
    },

    // 检查用户是否已认证
    getIsAuthenticated(state): boolean {
      return state.isAuthenticated
    },

    // 获取用户ID
    getUserId(state): string | undefined {
      return state.userInfo?.userId
    },

    // 获取用户名
    getUserName(state): string | undefined {
      return state.userInfo?.userName
    },

    // 获取用户邮箱
    getUserEmail(state): string | undefined {
      return state.userInfo?.email
    },

    // 获取暗色模式设置
    getDarkMode(state): boolean | undefined {
      return state.userSettings?.darkMode
    }
  },

  actions: {
    // 初始化用户状态
    initUser() {
      this.userInfo = getUserInfo()
      this.isAuthenticated = isUserLoggedIn()
    },

    // 设置用户信息
    setUserInfo(userInfo: any) {
      // 检查是否是{code, msg, data}格式的响应
      const userData = userInfo.data || userInfo;
      this.userInfo = userData
      this.isAuthenticated = true
      saveUserInfo(userData)
    },

    // 设置用户设置
    setUserSettings(settings: any) {
      // 检查是否是{code, msg, data}格式的响应
      const settingsData = settings.data || settings;
      this.userSettings = settingsData
    },

    // 更新用户信息
    updateUserInfo(updates: Partial<UserInfo>) {
      if (this.userInfo) {
        this.userInfo = { ...this.userInfo, ...updates }
        updateUserInfo(updates)
      }
    },

    // 更新用户设置
    updateUserSettings(updates: Partial<UserSettings>) {
      if (this.userSettings) {
        this.userSettings = { ...this.userSettings, ...updates }
      }
    },

    // 清除用户信息
    clearUserInfo() {
      this.userInfo = null
      this.userSettings = null
      this.isAuthenticated = false
      removeUserInfo()
    }
  }
})