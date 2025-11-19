<script setup lang="ts">
import { useRouter } from 'vue-router'
import { onMounted } from 'vue'
import Sidebar from '../Sidebar.vue'
import ChatWindow from './ChatWindow.vue'
import { getToken, logout } from '../../utils/auth.ts'
import apiClient from '../../services/api.ts'
import { API_ENDPOINTS } from '../../constants/api.ts'

const router = useRouter()

onMounted(() => {
  // 检查用户是否已登录
  if (!getToken()) {
    // 如果未登录，跳转到登录页面
    router.push('/login')
  }
})

// 登出方法
const handleLogout = async () => {
  try {
    // 调用后端登出接口
    await apiClient.post(API_ENDPOINTS.USER_LOGOUT, {})
  } catch (error) {
    console.error('登出失败:', error)
  } finally {
    // 清除本地存储的用户信息和token
    logout()
    
    // 跳转到登录页面
    router.push('/login')
  }
}
</script>

<template>
  <div class="chat-layout">
    <Sidebar />
    <ChatWindow />
  </div>
</template>

<style scoped>
.chat-layout {
  display: flex;
  width: 100%;
  height: 100%;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  background-color: white;
}

/* 暗色模式样式 */
.dark-mode .chat-layout {
  background-color: #1a1a1a;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.3);
}
</style>