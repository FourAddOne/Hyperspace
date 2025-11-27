<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import apiClient, { API_ENDPOINTS } from '../services/api'
import { saveTokens } from '../utils/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
// 表单数据
const registerForm = ref({
  userName: '',
  email: '',
  password: '',
  confirmPassword: ''
})

// 状态控制
const loading = ref(false)
const errorMessage = ref('')

// 弹窗控制
const showPopup = ref(false)
const popupMessage = ref('')
const popupType = ref('') // 'success' 或 'error'

// 显示弹窗
const showPopupMessage = (message: string, type: string) => {
  popupMessage.value = message
  popupType.value = type
  showPopup.value = true
  
  // 3秒后自动关闭弹窗
  setTimeout(() => {
    showPopup.value = false
  }, 3000)
}

onMounted(() => {
  // 添加登录页面的CSS类以显示背景
  document.body.classList.add('login-page')
})

onUnmounted(() => {
  // 组件销毁时移除CSS类
  document.body.classList.remove('login-page')
})

// 注册方法
const handleRegister = async () => {
  // 表单验证
  if (!registerForm.value.userName) {
    showPopupMessage('请输入用户名', 'error')
    return
  }
  
  if (!registerForm.value.email) {
    showPopupMessage('请输入邮箱', 'error')
    return
  }
  
  if (!registerForm.value.password) {
    showPopupMessage('请输入密码', 'error')
    return
  }
  
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    showPopupMessage('两次输入的密码不一致', 'error')
    return
  }

  if (registerForm.value.password.length < 6) {
    showPopupMessage('密码长度不能少于6位', 'error')
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const response = await apiClient.post(API_ENDPOINTS.USER_REGISTER, registerForm.value)
      
    if (response && response.code !== 200) {
      throw new Error(response.msg || '注册失败')
    }
      
    ElMessage.success('注册成功')
      
      // 注册成功后自动登录
      await handleAutoLogin(registerForm.value)
    } catch (error: any) {
      console.error('注册失败:', error)
      if (error.response && error.response.data && error.response.data.msg) {
        ElMessage.error(error.response.data.msg)
      } else {
        ElMessage.error(error.message || '注册失败')
      }
    } finally {
      loading.value = false
    }
  }
  
  // 自动登录函数
  const handleAutoLogin = async (userData: any) => {
    try {
      const loginData = {
        email: userData.email,
        password: userData.password
      }
      
      const response = await apiClient.post(API_ENDPOINTS.USER_LOGIN, loginData)
      
      if (response && response.code === 200 && response.data) {
        // 保存token
        const { accessToken, refreshToken } = response.data
        saveTokens(accessToken, refreshToken)
        
        // 应用暗色模式设置
        document.body.classList.remove('login-page')
        
        ElMessage.success('注册并登录成功')
        
        // 添加延迟确保token被正确设置
        await new Promise(resolve => setTimeout(resolve, 500));
        
        // 跳转到聊天页面
        await router.push('/chat')
      } else {
        ElMessage.error(response?.msg || '自动登录失败')
      }
    } catch (error: any) {
      console.error('自动登录失败:', error)
      if (error.response && error.response.data && error.response.data.msg) {
        ElMessage.error(error.response.data.msg)
      } else {
        ElMessage.error(error.message || '自动登录失败')
      }
    }
  }

// 监听路由变化，确保正确添加背景类
router.afterEach((to) => {
  // 如果进入注册页面，确保添加背景类
  if (to.name === 'register') {
    document.body.classList.add('login-page')
  }
})
</script>

<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <h1>创建账户</h1>
        <p>加入我们的聊天社区</p>
      </div>
      
      <form @submit.prevent="handleRegister" class="register-form">
        <div class="form-group">
          <label for="username">用户名</label>
          <input 
            id="username"
            v-model="registerForm.userName" 
            type="text" 
            placeholder="请输入用户名" 
            class="form-input"
            :disabled="loading"
          />
        </div>
        
        <div class="form-group">
          <label for="email">邮箱</label>
          <input 
            id="email"
            v-model="registerForm.email" 
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
            v-model="registerForm.password" 
            type="password" 
            placeholder="请输入密码（至少6位）" 
            class="form-input"
            :disabled="loading"
          />
        </div>
        
        <div class="form-group">
          <label for="confirmPassword">确认密码</label>
          <input 
            id="confirmPassword"
            v-model="registerForm.confirmPassword" 
            type="password" 
            placeholder="请再次输入密码" 
            class="form-input"
            :disabled="loading"
          />
        </div>
        
        <button 
          type="submit" 
          class="register-button" 
          :disabled="loading"
        >
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>
      
      <div class="register-footer">
        <p>已有账户？<router-link to="/login">立即登录</router-link></p>
      </div>
    </div>
  </div>
  
  <!-- 弹窗组件 -->
  <div v-if="showPopup" class="popup-overlay" @click="showPopup = false">
    <div class="popup-content" :class="popupType" @click.stop>
      <div class="popup-icon">
        <span v-if="popupType === 'success'">✓</span>
        <span v-else>✗</span>
      </div>
      <div class="popup-message">{{ popupMessage }}</div>
    </div>
  </div>
</template>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  width: 100%;
  padding: 20px;
  margin: 0;
  position: relative;
}

.register-card {
  width: 100%;
  max-width: 450px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  animation: fadeIn 0.5s ease-out;
  backdrop-filter: blur(10px);
  margin: 0;
  z-index: 10;
}

.register-header {
  background: linear-gradient(120deg, #0084ff, #4568dc);
  color: white;
  padding: 30px 20px;
  text-align: center;
}

.register-header h1 {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
}

.register-header p {
  font-size: 16px;
  opacity: 0.9;
}

.register-form {
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

.error-message {
  color: #e74c3c;
  font-size: 14px;
  margin-bottom: 15px;
  padding: 10px;
  background-color: #fdf2f2;
  border-radius: 6px;
  border-left: 4px solid #e74c3c;
}

.register-button {
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
  margin-top: 10px;
}

.register-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 132, 255, 0.4);
}

.register-button:disabled {
  background: #bdc3c7;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.register-footer {
  text-align: center;
  padding: 20px 30px 30px;
  border-top: 1px solid #eee;
}

.register-footer p {
  color: #666;
  font-size: 14px;
}

.register-footer a {
  color: #0084ff;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s ease;
}

.register-footer a:hover {
  color: #4568dc;
  text-decoration: underline;
}

/* 弹窗样式 */
.popup-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: flex-start; /* 上移弹窗 */
  padding-top: 20vh; /* 上移弹窗 */
  z-index: 1000;
}

.popup-content {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  background-color: white;
  max-width: 300px;
  animation: popupFadeIn 0.3s ease-out;
}

.popup-content.success {
  border-left: 6px solid #4caf50;
}

.popup-content.error {
  border-left: 6px solid #f44336;
}

.popup-icon {
  font-size: 24px;
  font-weight: bold;
  margin-right: 15px;
}

.popup-icon span {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  color: white;
}

.popup-content.success .popup-icon span {
  background-color: #4caf50;
}

.popup-content.error .popup-icon span {
  background-color: #f44336;
}

.popup-message {
  font-size: 16px;
  color: #333;
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

@keyframes popupFadeIn {
  from {
    opacity: 0;
    transform: scale(0.8);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@media (max-width: 480px) {
  .register-card {
    border-radius: 12px;
  }
  
  .register-header {
    padding: 25px 15px;
  }
  
  .register-header h1 {
    font-size: 24px;
  }
  
  .register-form {
    padding: 20px;
  }
  
  .popup-content {
    margin: 0 20px;
  }
  
  .popup-overlay {
    padding-top: 15vh; /* 在小屏幕上稍微调整 */
  }
}

/* 暗色模式样式 */
.dark-mode .register-card {
  background: rgba(45, 45, 45, 0.95);
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.5);
}

.dark-mode .register-header {
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

.dark-mode .toggle-register-method span {
  color: #409eff;
}

.dark-mode .toggle-register-method span:hover {
  background-color: rgba(64, 158, 255, 0.1);
}

.dark-mode .register-button {
  background: linear-gradient(120deg, #0066cc, #3a56c0);
  box-shadow: 0 4px 12px rgba(0, 102, 204, 0.3);
}

.dark-mode .register-button:hover:not(:disabled) {
  box-shadow: 0 6px 16px rgba(0, 102, 204, 0.4);
}

.dark-mode .register-footer {
  border-top: 1px solid #444;
}

.dark-mode .register-footer p {
  color: #ccc;
}

.dark-mode .register-footer a {
  color: #409eff;
}

.dark-mode .register-footer a:hover {
  color: #66b1ff;
}

.dark-mode .popup-content {
  background-color: #2d2d2d;
}

.dark-mode .popup-message {
  color: #f5f5f5;
}
</style>