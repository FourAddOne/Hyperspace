import { defineStore } from 'pinia'
import { getUserInfo, saveUserInfo, removeUserInfo, updateUserInfo, isUserLoggedIn } from '../utils/user'
import type { UserInfo, UserSettings } from '../utils/user'

interface UserState {
  userInfo: UserInfo | null
  userSettings: UserSettings | null
  isAuthenticated: boolean
  hasBackground: boolean // 添加背景状态管理
  darkMode: boolean // 添加暗色模式状态管理
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    userInfo: null,
    userSettings: null,
    isAuthenticated: false,
    hasBackground: false, // 初始化背景状态
    darkMode: false // 初始化暗色模式状态
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
    getDarkMode(state): boolean {
      return state.darkMode
    },

    // 获取背景状态
    getHasBackground(state): boolean {
      return state.hasBackground
    }
  },

  actions: {
    // 初始化用户状态
    initUser() {
      this.userInfo = getUserInfo()
      this.isAuthenticated = isUserLoggedIn()
      // 初始化背景状态
      const backgroundImage = localStorage.getItem('userBackgroundImage')
      this.hasBackground = !!backgroundImage
      
      // 初始化暗色模式状态
      const savedDarkMode = localStorage.getItem('userDarkMode')
      this.darkMode = savedDarkMode === 'true'
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
      
      // 更新背景状态
      if (settingsData.backgroundImage !== undefined) {
        this.hasBackground = !!settingsData.backgroundImage
      }
      
      // 更新暗色模式状态
      if (settingsData.darkMode !== undefined) {
        this.darkMode = settingsData.darkMode
      }
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
        // 更新背景状态
        if (updates.backgroundImage !== undefined) {
          this.hasBackground = !!updates.backgroundImage
        }
        
        // 更新暗色模式状态
        if (updates.darkMode !== undefined) {
          this.darkMode = updates.darkMode
        }
        
        // 持久化用户设置到localStorage
        localStorage.setItem('userSettings', JSON.stringify(this.userSettings))
      }
    },

    // 清除用户信息
    clearUserInfo() {
      this.userInfo = null
      this.userSettings = null
      this.isAuthenticated = false
      this.hasBackground = false
      this.darkMode = false
      removeUserInfo()
    },

    // 设置背景状态
    setHasBackground(hasBackground: boolean) {
      this.hasBackground = hasBackground
    },
    
    // 设置暗色模式状态
    setDarkMode(darkMode: boolean) {
      this.darkMode = darkMode
      // 同步到localStorage
      localStorage.setItem('userDarkMode', darkMode ? 'true' : 'false')
    },
    
    // 切换暗色模式
    toggleDarkMode() {
      this.darkMode = !this.darkMode
      // 同步到localStorage
      localStorage.setItem('userDarkMode', this.darkMode ? 'true' : 'false')
    }
  }
})