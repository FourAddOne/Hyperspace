<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import apiClient, { API_ENDPOINTS } from '../services/api'
import { saveToken } from '../utils/auth'
import { saveUserInfo } from '../utils/user'
import { useUserStore } from '../stores/userStore'
import { getUserIP } from '../utils/ip'

const router = useRouter()
const userStore = useUserStore()

// 表单数据
const form = ref({
  userId: '',
  email: '',
  password: ''
})

// 登录方式：email 或 userId
const loginMethod = ref('email')

// 登录加载状态
const loading = ref(false)

// IP地址（动态获取）
const ip = ref('')

// 获取用户IP
const fetchUserIP = async () => {
  try {
    const userIP = await getUserIP()
    ip.value = userIP // 现在getUserIP在失败时返回'127.0.0.1'
  } catch (error) {
    console.error('获取IP失败:', error)
    ip.value = '127.0.0.1' // 出错时使用默认值
  }
}

// 处理登录
const handleLogin = async () => {
  if (((loginMethod.value === 'email' && !form.value.email) || 
       (loginMethod.value === 'userId' && !form.value.userId)) || 
      !form.value.password) {
    ElMessage.error('请填写完整信息')
    return
  }

  loading.value = true
  
  try {
    const requestData: any = {
      password: form.value.password,
      ip: ip.value
    }
    
    if (loginMethod.value === 'email') {
      requestData.email = form.value.email
    } else {
      requestData.userId = form.value.userId
    }
    
    const loginResponse = await apiClient.post(API_ENDPOINTS.USER_LOGIN, requestData)
    console.log('登录响应:', loginResponse); // 添加日志查看实际响应

    // Check if response indicates an error
    if (loginResponse.code !== 200) {
      throw new Error(loginResponse.msg || '登录失败');
    }

    if (loginResponse) {
      // 检查响应数据中是否包含accessToken
      // 数据现在直接在loginResponse中，因为拦截器自动返回response.data
      const userData = loginResponse.data || loginResponse;
      if (!userData || !userData.accessToken) {
        console.error('登录响应中缺少访问令牌:', loginResponse);
        throw new Error('登录响应中缺少访问令牌');
      }
      
      // 保存token到localStorage
      saveToken(userData.accessToken)
      
      // 保存用户信息到store和localStorage
      userStore.setUserInfo(userData)
      
      // 添加一个小延迟确保token被axios拦截器正确设置
      await new Promise(resolve => setTimeout(resolve, 100))
      
      // 获取并应用用户的暗色模式设置
      try {
        const settingsResponse = await apiClient.get(API_ENDPOINTS.USER_SETTINGS)
        console.log('用户设置响应:', settingsResponse); // 添加调试日志
        
        if (settingsResponse && settingsResponse.data) {
          // 保存用户设置到store
          userStore.setUserSettings(settingsResponse.data)
          
          // 立即应用暗色模式到当前页面
          if (settingsResponse.data.darkMode) {
            document.body.classList.add('dark-mode')
            // 保存到本地存储，以便其他页面使用
            localStorage.setItem('userDarkMode', 'true')
          } else {
            document.body.classList.remove('dark-mode')
            localStorage.setItem('userDarkMode', 'false')
          }
          
          // 保存背景设置
          // 处理背景图片URL
          let fullBackgroundImageUrl = settingsResponse.data.backgroundImage || '';
          if (fullBackgroundImageUrl && fullBackgroundImageUrl.startsWith('/uploads/')) {
            fullBackgroundImageUrl = '/api' + fullBackgroundImageUrl;
          }
          
          localStorage.setItem('userBackgroundImage', fullBackgroundImageUrl)
          localStorage.setItem('userBackgroundOpacity', settingsResponse.data.backgroundOpacity?.toString() || '100')
        } else {
          // 如果没有获取到设置，默认使用亮色模式
          document.body.classList.remove('dark-mode')
          localStorage.setItem('userDarkMode', 'false')
        }
      } catch (settingsError) {
        console.error('获取用户设置失败:', settingsError)
        // 如果获取设置失败，默认使用亮色模式
        document.body.classList.remove('dark-mode')
        localStorage.setItem('userDarkMode', 'false')
      }
      
      ElMessage.success('登录成功')
      
      // 跳转到聊天页面
      await router.push('/chat')
    }
  } catch (error: any) {
    console.error('登录失败:', error)
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}

// 前往注册页面
const goToRegister = () => {
  router.push('/register')
}

// 切换登录方式
const toggleLoginMethod = () => {
  loginMethod.value = loginMethod.value === 'email' ? 'userId' : 'email'
}

// 组件挂载时获取用户IP
onMounted(() => {
  fetchUserIP()
  // 添加login-page类以显示登录背景
  document.body.classList.add('login-page')
})

// 在组件卸载时移除login-page类
onUnmounted(() => {
  document.body.classList.remove('login-page')
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
        <div class="form-group">
          <label v-if="loginMethod === 'email'" for="email">邮箱</label>
          <label v-else for="userId">用户ID</label>
          <input 
            v-if="loginMethod === 'email'"
            id="email"
            v-model="form.email" 
            type="email" 
            placeholder="请输入邮箱地址" 
            class="form-input"
            :disabled="loading"
          />
          <input 
            v-else
            id="userId"
            v-model="form.userId" 
            type="text" 
            placeholder="请输入用户ID" 
            class="form-input"
            :disabled="loading"
          />

        </div>
        
        <div class="form-group">
          <label for="password">密码</label>
          <input 
            id="password"
            v-model="form.password" 
            type="password" 
            placeholder="请输入密码" 
            class="form-input"
            :disabled="loading"
          />
        </div>

        <div class="toggle-login-method">
          <span @click="toggleLoginMethod">使用{{ loginMethod === 'email' ? '用户ID' : '邮箱' }}登录</span>
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

.toggle-login-method {
  text-align: center;
  margin-top: 8px;
  margin-bottom: 16px;
}

.toggle-login-method span {
  font-size: 14px;
  color: #0084ff;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}

.toggle-login-method span:hover {
  text-decoration: underline;
  background-color: rgba(0, 132, 255, 0.1);
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

.dark-mode .toggle-login-method span {
  color: #409eff;
}

.dark-mode .toggle-login-method span:hover {
  background-color: rgba(64, 158, 255, 0.1);
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
</style>