import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken')
    if (token) {
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
    
    if ((error.response?.status === 401 || error.response?.status === 403) && !isLogoutRequest) {
      // token过期或无效，清除本地存储
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userInfo')
      // 不再自动跳转到登录页，而是将错误传递给调用方处理
      console.warn('认证失败，请重新登录');
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