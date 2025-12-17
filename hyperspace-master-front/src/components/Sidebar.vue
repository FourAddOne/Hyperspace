<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {ChatRound, User, Setting, SwitchButton, ChatLineSquare,SwitchFilled} from '@element-plus/icons-vue'
import { getUserInfo, getFullAvatarUrl } from '../utils/user'
import { removeToken } from '../utils/auth'
import apiClient, { API_ENDPOINTS } from '../services/api'
import { useUserStore } from '../stores/userStore'

const router = useRouter()
const userStore = useUserStore()

// 获取用户信息
const loadUserInfo = () => {
  const userInfo = getUserInfo()
  if (userInfo) {
    userStore.setUserInfo(userInfo)
  }
}

// 跳转到个人资料页面
const goToProfile = () => {
  router.push('/profile')
}

// 登出
const logout = () => {
  console.log('Logout function called') // 调试日志
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    console.log('Logout confirmed') // 调试日志
    try {
      // 调用后端登出接口
      await apiClient.post(API_ENDPOINTS.USER_LOGOUT)
      
      // 清除本地状态
      userStore.clearUserInfo()
      removeToken()
      
      ElMessage.success('已退出登录')
      router.push('/login')
    } catch (error) {
      console.error('登出失败:', error)
      // 即使后端登出失败，也要清除本地状态以确保用户能退出
      userStore.clearUserInfo()
      removeToken()
      router.push('/login')
    }
  }).catch(() => {
    console.log('Logout cancelled') // 调试日志
    // 取消登出
  })
}

// 监听暗色模式变化并应用
watch(() => userStore.getDarkMode, (newDarkMode) => {
  if (newDarkMode) {
    document.body.classList.add('dark-mode')
    localStorage.setItem('userDarkMode', 'true')
  } else {
    document.body.classList.remove('dark-mode')
    localStorage.setItem('userDarkMode', 'false')
  }
})

onMounted(() => {
  loadUserInfo()
  
  // 如果用户已登录但store中没有用户设置，则从后端获取
  if (userStore.getIsAuthenticated && !userStore.getUserSettings) {
    // 添加延迟确保token被正确设置
    setTimeout(() => {
      // 检查localStorage中是否有token
      const token = localStorage.getItem('accessToken');
      if (token) {
        apiClient.get(API_ENDPOINTS.USER_SETTINGS)
          .then(userData => {
            // 数据现在直接在userData中，因为拦截器自动返回response.data
            if (userData) {
              userStore.setUserSettings(userData)
            }
          })
          .catch(error => {
            console.error('获取用户设置失败:', error)
            // 如果是认证错误，清除本地状态并重定向到登录页
            if (error.response && error.response.status === 401) {
              userStore.clearUserInfo()
              localStorage.removeItem('accessToken')
              router.push('/login')
            }
          })
      } else {
        console.warn('未找到访问令牌，跳过用户设置获取')
      }
    }, 200)
  }
})

const userAvatar = computed(() => {
  const avatarUrl = userStore.getUserInfo?.avatarUrl;
  return getFullAvatarUrl(avatarUrl) || '/src/assets/logo.svg';
})
const username = computed(() => userStore.getUserName || '未知用户')
const userStatus = computed(() => userStore.getUserInfo?.status || '在线')
const statusClass = computed(() => userStatus.value === '在线' ? 'status-online' : 'status-offline')

</script>

<template>
  <div class="sidebar">
    <div class="user-profile-header" @click="goToProfile">
      <img 
        :src="userAvatar" 
        alt="用户头像" 
        class="user-avatar"
      />
      <div class="user-info">
        <div class="username">{{ username }}</div>
        <div class="user-status" :class="statusClass">{{ userStatus }}</div>
      </div>
    </div>
    
    <nav class="nav-menu">
      <ul>
        <li 
          :class="{ active: $route.name === 'chat' || $route.name === 'chat-main' }" 
          @click="$router.push('/chat')"
        >
          <chat-round class="nav-icon" />
          <span>聊天</span>
        </li>
        <li 
          :class="{ active: $route.name === 'friends' }" 
          @click="$router.push('/friends')"
        >
          <user class="nav-icon" />
          <span>好友</span>
        </li>
        <li
            :class="{ active: $route.name === 'groups' }"
            @click="$router.push('/groups')"
        >
          <ChatLineSquare class="nav-icon" />
          <span>群聊</span>
        </li>

        <li
            :class="{ active: $route.name === 'ai-chat' }"
            @click="$router.push('/ai-chat')"
        >
          <ChatRound class="nav-icon" />
          <span>AI聊天</span>
        </li>

        <li
            :class="{ active: $route.name === 'games' }"
            @click="$router.push('/games')"
        >
          <SwitchFilled  class="nav-icon"/>
          <span>游戏</span>
        </li>

        <li 
          :class="{ active: $route.name === 'profile' }" 
          @click="$router.push('/profile')"
        >
          <setting class="nav-icon" />
          <span>设置</span>
        </li>
      </ul>
    </nav>
    
    <div class="sidebar-footer">
      <button class="logout-btn" @click="logout">
        <switch-button class="nav-icon" />
        <span>退出登录</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.sidebar {
  width: 80px; /* 缩小Sidebar宽度 */
  height: 100%;
  background-color: #fafafa;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.user-profile-header {
  padding: 15px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-bottom: 1px solid #e0e0e0;
  cursor: pointer;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #ddd;
  margin-bottom: 8px;
  object-fit: cover;
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.username {
  font-size: 12px;
  font-weight: 500;
  color: #333;
  margin-bottom: 2px;
}

.user-status {
  font-size: 10px;
}

.status-online {
  color: #67c23a; /* 绿色 */
}

.status-offline {
  color: #f56c6c; /* 红色 */
}

.nav-menu {
  flex: 1;
  padding: 10px 0;
  width: 100%;
}

.nav-menu ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.nav-menu li {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 0;
  cursor: pointer;
  color: #666;
  transition: background-color 0.2s;
}

.nav-menu li:hover {
  background-color: #f1f1f1;
}

.nav-menu li.active {
  background-color: #e0e0e0;
  color: #0084ff;
}

.nav-menu li.active .nav-icon {
  color: #0084ff;
}

.nav-icon {
  margin-bottom: 4px;
  width: 30px;  /* 设置图标宽度 */
  height: 30px; /* 设置图标高度 */
}

.nav-menu li span {
  font-size: 16px; /* 从12px缩小到10px */
}

.sidebar-footer {
  padding: 15px 0;
  border-top: 1px solid #e0e0e0;
  width: 100%;
}

.logout-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: none;
  border: none;
  color: #666;
  cursor: pointer;
  font-size: 12px;
  width: 100%;
}

.logout-btn:hover {
  background-color: #f1f1f1;
}

.logout-btn .nav-icon {
  margin-bottom: 4px;
  width: 30px;  /* 设置图标宽度 */
  height: 30px; /* 设置图标高度 */
}
</style>

<!-- 使用非scoped样式来处理暗色模式 -->
<style>
/* 暗色模式样式 */
.dark-mode .sidebar {
  background-color: #1a1a1a;
  border-right: 1px solid #444;
  color: #f5f5f5;
}

.dark-mode .sidebar .user-profile-header {
  border-bottom: 1px solid #444;
}

.dark-mode .sidebar .username {
  color: #f5f5f5;
}

.dark-mode .sidebar .nav-menu li {
  color: #ccc;
}

.dark-mode .sidebar .nav-menu li:hover {
  background-color: #3d3d3d;
}

.dark-mode .sidebar .nav-menu li.active {
  background-color: #3d3d3d;
  color: #409eff;
}

.dark-mode .sidebar .nav-menu li.active .nav-icon {
  color: #409eff;
}

.dark-mode .sidebar .sidebar-footer {
  border-top: 1px solid #444;
}

.dark-mode .logout-btn {
  color: #ccc;
}

.dark-mode .logout-btn:hover {
  background-color: #3d3d3d;
}
</style>
