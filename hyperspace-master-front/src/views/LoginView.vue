<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import apiClient, { API_ENDPOINTS } from '../services/api'
import { saveTokens, removeTokens } from '../utils/auth'
import { saveUserInfo } from '../utils/user'
import { useUserStore } from '../stores/userStore'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const useUserId = ref(false) // 控制使用用户ID还是邮箱登录

const loginForm = ref({
  userId: '',
  email: '',
  password: '',
  ip: ''
})

// 获取用户store
const userStore = useUserStore()

onMounted(() => {
  // 页面加载时清空可能存在的旧token
  removeTokens()
  // 添加登录页面的CSS类
  document.body.classList.add('login-page')
})

onUnmounted(() => {
  // 组件销毁时移除CSS类
  document.body.classList.remove('login-page')
})

// 切换登录方式
const toggleLoginMethod = () => {
  useUserId.value = !useUserId.value
  // 清空表单
  loginForm.value.userId = ''
  loginForm.value.email = ''
}

const handleLogin = async () => {
  if (useUserId.value && !loginForm.value.userId) {
    ElMessage.error('请输入用户ID')
    return
  }
  
  if (!useUserId.value && !loginForm.value.email) {
    ElMessage.error('请输入邮箱地址')
    return
  }
  
  if (!loginForm.value.password) {
    ElMessage.error('请输入密码')
    return
  }
  
  loading.value = true
  
  try {
    // 获取客户端IP
    try {
      const ipResponse = await fetch('https://httpbin.org/ip').then(res => res.json())
      loginForm.value.ip = ipResponse.origin
    } catch (ipError) {
      console.error('获取IP失败:', ipError)
      loginForm.value.ip = '127.0.0.1' // 默认IP
    }
    
    // 准备登录数据
    const loginData: any = {
      password: loginForm.value.password,
      ip: loginForm.value.ip
    }
    
    // 根据登录方式添加对应的字段
    if (useUserId.value) {
      loginData.userId = loginForm.value.userId
    } else {
      loginData.email = loginForm.value.email
    }
    
    console.log('登录请求数据:', loginData)
    
    // 执行登录
    const response = await apiClient.post<{ accessToken: string; refreshToken: string }>(API_ENDPOINTS.USER_LOGIN, loginData)
    
    console.log('登录响应:', response)
    
    // 修复响应判断逻辑
    if (response && response.code === 200 && response.data) {
      // 保存token
      const { accessToken, refreshToken } = response.data
      saveTokens(accessToken, refreshToken)
      
      try {
        // 获取用户信息
        const userResponse = await apiClient.get(API_ENDPOINTS.USER_INFO)
        console.log('用户信息响应:', userResponse)
        if (userResponse && userResponse.code === 200) {
          // 保存用户信息到localStorage
          saveUserInfo(userResponse.data)
          // 更新store中的用户信息
          userStore.setUserInfo(userResponse.data)
        }
        
        // 获取用户设置
        const settingsResponse = await apiClient.get(API_ENDPOINTS.USER_SETTINGS)
        console.log('用户设置响应:', settingsResponse)
        if (settingsResponse && settingsResponse.code === 200) {
          // 处理背景图片URL
          let fullBackgroundImageUrl = settingsResponse.data.backgroundImage || '';
          if (fullBackgroundImageUrl && fullBackgroundImageUrl.startsWith('/uploads/')) {
            fullBackgroundImageUrl = '/api' + fullBackgroundImageUrl;
          }
          
          localStorage.setItem('userBackgroundImage', fullBackgroundImageUrl)
          localStorage.setItem('userBackgroundOpacity', settingsResponse.data.backgroundOpacity?.toString() || '100')
          
          // 更新store中的用户设置和背景状态
          userStore.setUserSettings(settingsResponse.data)
          userStore.setHasBackground(!!fullBackgroundImageUrl)
          userStore.setDarkMode(settingsResponse.data.darkMode || false)
        } else {
          // 如果没有获取到设置，默认使用亮色模式
          userStore.setDarkMode(false)
          userStore.setHasBackground(false)
        }
      } catch (settingsError) {
        console.error('获取用户设置失败:', settingsError)
        // 如果获取设置失败，默认使用亮色模式
        userStore.setDarkMode(false)
        userStore.setHasBackground(false)
      }
      
      // 应用暗色模式设置
      document.body.classList.toggle('dark-mode', userStore.getDarkMode)
      // 移除登录页面背景
      document.body.classList.remove('login-page')
      
      ElMessage.success('登录成功')
      
      // 添加延迟确保token被正确设置
      await new Promise(resolve => setTimeout(resolve, 500));
      
      // 跳转到聊天页面
      await router.push('/chat')
    } else {
      ElMessage.error(response?.msg || '登录响应异常')
    }
  } catch (error: any) {
    console.error('登录失败:', error)
    if (error.response && error.response.data && error.response.data.msg) {
      ElMessage.error(error.response.data.msg)
    } else {
      ElMessage.error(error.message || '登录失败')
    }
  } finally {
    loading.value = false
  }
}

// 跳转到注册页面
const goToRegister = () => {
  router.push('/register')
}

// 监听路由变化，确保正确添加背景类
router.afterEach((to) => {
  // 如果进入登录页面，确保添加背景类
  if (to.name === 'login') {
    document.body.classList.add('login-page')
  }
})
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1>欢迎回来</h1>
        <p>登录到您的账户</p>
      </div>
      
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group" v-if="useUserId">
          <label for="userId">用户ID</label>
          <input 
            id="userId"
            v-model="loginForm.userId" 
            type="text" 
            placeholder="请输入用户ID" 
            class="form-input"
            :disabled="loading"
          />
        </div>
        
        <div class="form-group" v-else>
          <label for="email">邮箱</label>
          <input 
            id="email"
            v-model="loginForm.email" 
            type="email" 
            placeholder="请输入邮箱地址" 
            class="form-input"
            :disabled="loading"
          />
        </div>
        
        <div class="form-group">
          <label for="password">密码</label>
          <input 
            id="password"
            v-model="loginForm.password" 
            type="password" 
            placeholder="请输入密码" 
            class="form-input"
            :disabled="loading"
          />
        </div>

        <div class="toggle-login-method">
          <span @click="toggleLoginMethod">
            {{ useUserId ? '使用邮箱登录' : '使用用户ID登录' }}
          </span>
        </div>

        <button 
          type="submit" 
          class="login-button" 
          :disabled="loading"
        >
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
      
      <div class="login-footer">
        <p>还没有账户？<a href="#" @click.prevent="goToRegister">立即注册</a></p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  min-height: 100vh;
  padding: 20px;
  margin: 0;
  position: relative;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  animation: fadeIn 0.5s ease-out;
  backdrop-filter: blur(10px);
  margin: 0;
  z-index: 10;
}

.login-header {
  background: linear-gradient(120deg, #0084ff, #4568dc);
  color: white;
  padding: 30px 20px;
  text-align: center;
}

.login-header h1 {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
}

.login-header p {
  font-size: 16px;
  opacity: 0.9;
}

.login-form {
  padding: 30px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.form-input {
  width: 100%;
  padding: 14px;
  border: 2px solid #e1e5e9;
  border-radius: 8px;
  font-size: 16px;
  transition: all 0.3s ease;
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.9);
}

.form-input:focus {
  outline: none;
  border-color: #0084ff;
  box-shadow: 0 0 0 3px rgba(0, 132, 255, 0.1);
}

.form-input:disabled {
  background-color: #f5f7fa;
  cursor: not-allowed;
}

.login-button {
  width: 100%;
  padding: 14px;
  background: linear-gradient(120deg, #0084ff, #4568dc);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 132, 255, 0.3);
}

.login-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 132, 255, 0.4);
}

.login-button:disabled {
  background: #bdc3c7;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.login-footer {
  text-align: center;
  padding: 20px 30px 30px;
  border-top: 1px solid #eee;
}

.login-footer p {
  color: #666;
  font-size: 14px;
}

.login-footer a {
  color: #0084ff;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s ease;
}

.login-footer a:hover {
  color: #4568dc;
  text-decoration: underline;
}

.toggle-login-method {
  text-align: center;
  margin-bottom: 20px;
}

.toggle-login-method span {
  color: #0084ff;
  cursor: pointer;
  font-size: 14px;
  text-decoration: underline;
}

.toggle-login-method span:hover {
  color: #4568dc;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 480px) {
  .login-card {
    border-radius: 12px;
  }
  
  .login-header {
    padding: 25px 15px;
  }
  
  .login-header h1 {
    font-size: 24px;
  }
  
  .login-form {
    padding: 20px;
  }
}

/* 暗色模式样式 */
.dark-mode .login-card {
  background: rgba(45, 45, 45, 0.95);
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.5);
}

.dark-mode .login-header {
  background: linear-gradient(120deg, #0066cc, #3a56c0);
}

.dark-mode .form-group label {
  color: #f5f5f5;
}

.dark-mode .form-input {
  background: rgba(50, 50, 50, 0.9);
  border-color: #555;
  color: #f5f5f5;
}

.dark-mode .form-input:focus {
  border-color: #0084ff;
  box-shadow: 0 0 0 3px rgba(0, 132, 255, 0.2);
}

.dark-mode .form-input::placeholder {
  color: #aaa;
}

.dark-mode .login-button {
  background: linear-gradient(120deg, #0066cc, #3a56c0);
  box-shadow: 0 4px 12px rgba(0, 102, 204, 0.3);
}

.dark-mode .login-button:hover:not(:disabled) {
  box-shadow: 0 6px 16px rgba(0, 102, 204, 0.4);
}

.dark-mode .login-footer {
  border-top: 1px solid #444;
}

.dark-mode .login-footer p {
  color: #ccc;
}

.dark-mode .login-footer a {
  color: #409eff;
}

.dark-mode .login-footer a:hover {
  color: #66b1ff;
}

.dark-mode .toggle-login-method span {
  color: #409eff;
}

.dark-mode .toggle-login-method span:hover {
  color: #66b1ff;
}
</style>