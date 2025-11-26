import axios from 'axios'
import type { ResVO } from '../types/api'
import { API_ENDPOINTS } from '../constants/api'

export { API_ENDPOINTS }

// 创建一个扩展的 AxiosInstance 类型，明确表示拦截器会返回 ResVO<T> 中的 T
interface HyperspaceApiClient extends Omit<import('axios').AxiosInstance, 'get' | 'post' | 'put' | 'delete' | 'patch'> {
  get<T = any>(url: string, config?: import('axios').AxiosRequestConfig): Promise<ResVO<T>>
  post<T = any>(url: string, data?: any, config?: import('axios').AxiosRequestConfig): Promise<ResVO<T>>
  put<T = any>(url: string, data?: any, config?: import('axios').AxiosRequestConfig): Promise<ResVO<T>>
  delete<T = any>(url: string, config?: import('axios').AxiosRequestConfig): Promise<ResVO<T>>
  patch<T = any>(url: string, data?: any, config?: import('axios').AxiosRequestConfig): Promise<ResVO<T>>
}

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10000
}) as HyperspaceApiClient

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
    const isLogoutRequest = error.config && error.config.url === API_ENDPOINTS.USER_LOGOUT;
    
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

export default apiClient