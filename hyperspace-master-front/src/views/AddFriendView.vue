<template>
  <div class="add-friend-view">
    <div class="header">
      <el-page-header @back="goBack" title="返回">
        <template #content>
          <span class="header-title">添加好友</span>
        </template>
      </el-page-header>
    </div>
    
    <div class="search-section">
      <h2>搜索用户</h2>
      <div class="search-form">
        <el-input
          v-model="searchKeyword"
          placeholder="输入用户ID或用户名搜索用户"
          class="search-input"
          @keyup.enter="searchUsers"
        >
          <template #append>
            <el-button @click="searchUsers" :loading="searching">
              <i class="el-icon-search"></i> 搜索
            </el-button>
          </template>
        </el-input>
      </div>
    </div>
    
    <div class="search-results" v-if="searchResults.length > 0">
      <h3>搜索结果</h3>
      <div 
        v-for="user in searchResults" 
        :key="user.userId" 
        class="user-item"
      >
        <el-avatar :src="getFullAvatarUrl(user.avatarUrl)" :size="40" />
        <div class="user-info">
          <div class="username">{{ user.userName }}</div>
          <div class="user-id">ID: {{ user.userId }}</div>
        </div>
        <div class="user-actions">
          <el-button 
            v-if="!isFriend(user.userId) && !hasPendingRequest(user.userId)"
            type="primary" 
            size="small" 
            @click="sendFriendRequest(user.userId)"
            :loading="sendingRequests[user.userId]"
          >
            添加好友
          </el-button>
          <el-tag v-else-if="isFriend(user.userId)" type="success">已是好友</el-tag>
          <el-tag v-else-if="hasPendingRequest(user.userId)" type="warning">等待确认</el-tag>
        </div>
      </div>
    </div>
    
    <div class="empty-state" v-else-if="searched && searchResults.length === 0">
      <p>未找到匹配的用户</p>
    </div>
    
    <div class="instructions" v-else>
      <p>请输入用户ID或用户名来搜索用户</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import apiClient, { API_ENDPOINTS } from '../services/api'
import { getFullAvatarUrl } from '../utils/user'

const router = useRouter()

// 搜索相关
const searchKeyword = ref('')
const searchResults = ref<any[]>([])
const searching = ref(false)
const searched = ref(false)

// 好友相关
const friends = ref<any[]>([])
const pendingRequests = ref<any[]>([])
const sendingRequests = ref<Record<string, boolean>>({})

// 返回上一页
const goBack = () => {
  router.back()
}

// 搜索用户
const searchUsers = async () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  
  searching.value = true
  searched.value = true
  
  try {
    const response = await apiClient.get(API_ENDPOINTS.FRIEND_SEARCH, {
      params: { keyword: searchKeyword.value }
    })
    
    searchResults.value = response.data || []
    console.log('搜索结果:', searchResults.value)
  } catch (error: any) {
    console.error('搜索用户失败:', error)
    ElMessage.error('搜索用户失败: ' + (error.message || '未知错误'))
    searchResults.value = []
  } finally {
    searching.value = false
  }
}

// 发送好友请求
const sendFriendRequest = async (userId: string) => {
  sendingRequests.value[userId] = true
  
  try {
    await apiClient.post(API_ENDPOINTS.FRIEND_REQUEST, null, {
      params: { friendId: userId }
    })
    
    ElMessage.success('好友请求已发送')
    // 更新本地状态
    pendingRequests.value.push({ userId })
  } catch (error: any) {
    ElMessage.error('发送好友请求失败: ' + (error.message || '未知错误'))
  } finally {
    sendingRequests.value[userId] = false
  }
}

// 检查是否已经是好友
const isFriend = (userId: string) => {
  return friends.value && Array.isArray(friends.value) 
    ? friends.value.some(friend => friend.userId === userId)
    : false;
}

// 检查是否已经发送过好友请求
const hasPendingRequest = (userId: string) => {
  return pendingRequests.value && Array.isArray(pendingRequests.value)
    ? pendingRequests.value.some(request => request.userId === userId)
    : false;
}

// 加载好友列表
const loadFriends = async () => {
  try {
    const response = await apiClient.get(API_ENDPOINTS.FRIENDS_LIST)
    friends.value = response.data || []
  } catch (error: any) {
    console.error('加载好友列表失败:', error)
    friends.value = []
  }
}

// 加载好友请求
const loadFriendRequests = async () => {
  try {
    const response = await apiClient.get(API_ENDPOINTS.FRIEND_REQUESTS)
    pendingRequests.value = response.data || []
  } catch (error: any) {
    console.error('加载好友请求失败:', error)
    pendingRequests.value = []
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadFriends()
  loadFriendRequests()
})
</script>

<style scoped>
.add-friend-view {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
  background-color: #fff; /* 添加白色背景 */
}

.header {
  margin-bottom: 30px;
}

.header-title {
  font-size: 18px;
  font-weight: 500;
  color: #333; /* 设置标题文字颜色 */
}

.search-section h2 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #333; /* 设置标题文字颜色 */
}

.search-form {
  max-width: 500px;
}

.search-input {
  width: 100%;
}

.search-input :deep(.el-input__wrapper) {
  border: none;
  background: transparent;
  box-shadow: none;
  padding: 0;
}

.search-input :deep(.el-input__inner) {
  border: 2px solid #e2e8f0;
  border-radius: 8px 0 0 8px;
  padding: 10px 15px;
  transition: all 0.3s ease;
  height: 40px;
}

.search-input :deep(.el-input__inner:focus) {
  border-color: #0084ff;
  box-shadow: 0 0 0 3px rgba(0, 132, 255, 0.1);
}

.search-input :deep(.el-input-group__append) {
  background: linear-gradient(135deg, #0084ff 0%, #0066cc 100%);
  border: 2px solid #0084ff;
  border-left: none;
  border-radius: 0 8px 8px 0;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  cursor: pointer;
}

.search-input :deep(.el-input-group__append:hover) {
  background: linear-gradient(135deg, #0066cc 0%, #004a99 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 132, 255, 0.3);
}

.search-input :deep(.el-button) {
  color: white;
  font-weight: 500;
  padding: 0;
  min-height: auto;
  background: transparent;
  border: none;
}

.search-results {
  margin-top: 30px;
}

.search-results h3 {
  margin-top: 0;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
  color: #333; /* 设置标题文字颜色 */
}

.user-item {
  display: flex;
  align-items: center;
  padding: 15px 10px;
  border-bottom: 1px solid #f0f0f0;
}

.user-info {
  flex: 1;
  margin-left: 15px;
  min-width: 0;
}

.username {
  font-weight: 500;
  margin-bottom: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #333; /* 设置用户名文字颜色 */
}

.user-id {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-actions {
  display: flex;
  align-items: center;
}

.empty-state,
.instructions {
  text-align: center;
  padding: 40px 0;
  color: #999;
}

/* 暗色模式样式 */
.dark-mode .add-friend-view {
  background-color: #1a1a1a;
  color: #f5f5f5;
}

.dark-mode .header-title,
.dark-mode .search-section h2,
.dark-mode .search-results h3 {
  color: #f5f5f5;
}

.dark-mode .search-input :deep(.el-input__wrapper) {
  background: transparent;
  box-shadow: none;
  border: none;
}

.dark-mode .search-input :deep(.el-input__inner) {
  background-color: #2d3748;
  border-color: #4a5568;
  color: #f7fafc;
  height: 40px;
}

.dark-mode .search-input :deep(.el-input__inner:focus) {
  border-color: #0084ff;
  box-shadow: 0 0 0 3px rgba(0, 132, 255, 0.2);
}

.dark-mode .search-input :deep(.el-input-group__append) {
  background: linear-gradient(135deg, #0084ff 0%, #0066cc 100%);
  border-color: #0084ff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dark-mode .search-input :deep(.el-input-group__append:hover) {
  background: linear-gradient(135deg, #0066cc 0%, #004a99 100%);
  box-shadow: 0 4px 8px rgba(0, 132, 255, 0.4);
}

.dark-mode .search-input :deep(.el-button) {
  color: white;
}

.dark-mode .search-results h3 {
  border-bottom: 1px solid #444;
}

.dark-mode .user-item {
  border-bottom: 1px solid #444;
}

.dark-mode .username {
  color: #f5f5f5; /* 设置暗色模式下用户名文字颜色 */
}

.dark-mode .user-id {
  color: #ccc;
}

/* 全局样式覆盖Element Plus默认样式 */
html.dark .el-page-header {
  --el-page-header-text-color: #ffffff;
  --el-page-header-back-arrow-color: #ffffff;
}

.el-page-header {
  --el-page-header-text-color: #000000;
  --el-page-header-back-arrow-color: #000000;
  --el-page-header-title-font-size: 14px;
}

.el-page-header__left,
.el-page-header__content {
  color: var(--el-page-header-text-color) !important;
}

.el-page-header__left .el-button {
  color: var(--el-page-header-text-color) !important;
  border-color: var(--el-page-header-text-color) !important;
  background-color: transparent !important;
}

.el-page-header__arrow {
  color: var(--el-page-header-text-color) !important;
  border-color: var(--el-page-header-text-color) !important;
}

.el-page-header__back {
  color: var(--el-page-header-text-color) !important;
}

.el-page-header__icon {
  color: var(--el-page-header-text-color) !important;
}

.el-page-header__title {
  color: var(--el-page-header-text-color) !important;
}

.el-page-header__content {
  color: var(--el-page-header-text-color) !important;
}

/* 直接覆盖Element Plus组件样式以确保在亮色模式下有足够的对比度 */
.el-page-header {
  color: #000000 !important;
}

.el-page-header .el-page-header__left,
.el-page-header .el-page-header__content {
  color: #000000 !important;
}

.el-page-header .el-page-header__back,
.el-page-header .el-page-header__icon,
.el-page-header .el-page-header__title {
  color: #000000 !important;
}

.el-page-header .el-page-header__arrow {
  color: #000000 !important;
  border-color: #000000 !important;
}

/* 暗色模式样式 - 确保在暗色模式下使用白色以提供足够对比度 */
.dark-mode .el-page-header {
  color: #ffffff !important;
}

.dark-mode .el-page-header .el-page-header__left,
.dark-mode .el-page-header .el-page-header__content {
  color: #ffffff !important;
}

.dark-mode .el-page-header .el-page-header__back,
.dark-mode .el-page-header .el-page-header__icon,
.dark-mode .el-page-header .el-page-header__title {
  color: #ffffff !important;
}

.dark-mode .el-page-header .el-page-header__arrow {
  color: #ffffff !important;
  border-color: #ffffff !important;
}</style>