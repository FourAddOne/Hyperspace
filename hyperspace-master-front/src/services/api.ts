import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    // 为AI聊天接口禁用token
    const token = localStorage.getItem('accessToken')
    if (token && !config.url?.includes('/chatbot/')) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => {
    // 自动返回response.data，简化数据访问
    return response.data
  },
  (error) => {
    // 检查是否是登出请求
    const isLogoutRequest = error.config && error.config.url === '/user/logout';
    // 检查是否是AI聊天接口
    const isChatBotRequest = error.config && error.config.url?.includes('/chatbot/');
    
    if ((error.response?.status === 401 || error.response?.status === 403) && !isLogoutRequest && !isChatBotRequest) {
      // token过期或无效，清除本地存储并跳转到登录页（但登出请求和AI聊天接口除外）
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userInfo')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// API端点
export const API_ENDPOINTS = {
  USER_LOGIN: '/user/login',
  USER_REGISTER: '/user/register',
  USER_INFO: '/user/info',
  USER_SETTINGS: '/user/settings',
  USER_LOGOUT: '/user/logout',
  FILE_UPLOAD: '/file/upload',
  USER_AVATAR: '/user/avatar',
  FRIENDS_LIST: '/friends/list',
  FRIEND_REQUEST: '/friends/request',
  FRIEND_ACCEPT: '/friends/accept',
  FRIEND_REJECT: '/friends/reject'
}

export default apiClient