import axios from 'axios'
import type { ResVO } from '../types/api'
import type { OssPolicy } from '../types/oss'
import { API_ENDPOINTS } from '../constants/api'
import { getAccessToken, getRefreshToken, saveAccessToken, removeTokens } from '../utils/auth'
import { removeUserInfo } from '../utils/user'

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

// 是否正在刷新token的标志
let isRefreshing = false
// 请求队列
let failedQueue: Array<{
  resolve: (value?: any) => void
  reject: (reason?: any) => void
}> = []

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token)
    }
  })
  
  failedQueue = []
}

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    const token = getAccessToken()
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
  async (error) => {
    const originalRequest = error.config
    
    // 检查是否是登出请求
    const isLogoutRequest = originalRequest && originalRequest.url === API_ENDPOINTS.USER_LOGOUT;
    
    if (error.response?.status === 401 && !originalRequest._retry && !isLogoutRequest) {
      if (isRefreshing) {
        // 如果正在刷新token，则将请求加入队列
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then(token => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          // 修复：使用apiClient的request方法而不是直接调用apiClient
          return apiClient.request(originalRequest)
        }).catch(err => {
          return Promise.reject(err)
        })
      }
      
      originalRequest._retry = true
      isRefreshing = true
      
      const refreshToken = getRefreshToken()
      
      if (refreshToken) {
        try {
          // 尝试刷新token
          const response = await apiClient.post(API_ENDPOINTS.USER_REFRESH, {}, {
            headers: {
              Authorization: `Bearer ${refreshToken}`
            }
          })

          // 注意：直接使用axios.post返回的是AxiosResponse对象
          // 需要通过response.data访问实际的响应数据
          if (response && response.data && response.code === 200) {
            const { accessToken } = response.data
            saveAccessToken(accessToken)
            
            // 更新原始请求的token
            originalRequest.headers.Authorization = `Bearer ${accessToken}`
            
            // 处理队列中的请求
            processQueue(null, accessToken)
            
            // 重新发起原始请求
            // 修复：使用apiClient的request方法而不是直接调用apiClient
            return apiClient.request(originalRequest)
          } else {
            throw new Error('刷新令牌失败')
          }
        } catch (refreshError) {
          // 刷新失败，清除令牌并跳转到登录页
          removeTokens()
          removeUserInfo()
          processQueue(refreshError, null)
          
          // 通知应用需要重新登录
          window.dispatchEvent(new CustomEvent('auth-expired'))
          
          return Promise.reject(refreshError)
        } finally {
          isRefreshing = false
        }
      } else {
        // 没有refreshToken，直接清除认证信息
        removeTokens()
        removeUserInfo()
        isRefreshing = false
        
        // 通知应用需要重新登录
        window.dispatchEvent(new CustomEvent('auth-expired'))
      }
    }
    
    return Promise.reject(error)
  }
)

/**
 * 直传文件到OSS
 * @param file 要上传的文件
 * @param fileType 文件类型 (avatar, background, message, file)
 * @returns 上传后的文件URL
 */
export const uploadFileToOSSDirectly = async (file: File, fileType: string): Promise<string> => {
  try {
    // 1. 获取OSS上传策略
    const policyResponse = await apiClient.get<OssPolicy>(API_ENDPOINTS.OSS_POLICY, {
      params: { fileType }
    });
    
    if (policyResponse.code !== 200) {
      throw new Error(policyResponse.msg || '获取OSS上传策略失败');
    }
    
    const { accessKeyId, policy, signature, dir, host } = policyResponse.data;
    
    // 2. 构造表单数据
    const key = `${dir}${Date.now()}_${file.name}`;
    const formData = new FormData();
    formData.append('key', key);
    formData.append('policy', policy);
    formData.append('OSSAccessKeyId', accessKeyId);
    formData.append('signature', signature);
    // 添加阿里云OSS需要的额外参数
    formData.append('success_action_status', '200');
    formData.append('file', file);
    
    // 3. 直接上传到OSS
    // 注意：这里不使用axios拦截器，因为我们需要直接与OSS通信
    const response = await axios.post(host, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      // 禁用withCredentials以避免CORS问题
      withCredentials: false
    });
    
    // 4. 检查响应状态
    if (response.status !== 200) {
      throw new Error(`OSS上传失败，状态码: ${response.status}`);
    }
    
    // 5. 构造并返回文件URL
    const fileUrl = `${host}/${key}`;
    return fileUrl;
  } catch (error: any) {
    console.error('直传OSS失败:', error);
    if (error.isAxiosError && error.code === 'ERR_NETWORK') {
      throw new Error('OSS上传网络错误，请检查OSS CORS配置是否正确');
    }
    throw new Error('上传到OSS失败: ' + (error instanceof Error ? error.message : '未知错误'));
  }
};

export default apiClient