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
          placeholder="输入用户名或邮箱搜索用户"
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
          <div class="user-email">{{ user.email }}</div>
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
      <p>请输入用户名或邮箱来搜索用户</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import apiClient from '../services/api'
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
    const response = await apiClient.get('/user/search', {
      params: { keyword: searchKeyword.value }
    })
    
    searchResults.value = response.data
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
    await apiClient.post('/api/friends/request', null, {
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
  return friends.value.some(friend => friend.userId === userId)
}

// 检查是否已经发送过好友请求
const hasPendingRequest = (userId: string) => {
  return pendingRequests.value.some(request => request.userId === userId)
}

// 加载好友列表
const loadFriends = async () => {
  try {
    const response = await apiClient.get('/api/friends/list')
    friends.value = response.data
  } catch (error: any) {
    console.error('加载好友列表失败:', error)
  }
}

// 加载好友请求
const loadFriendRequests = async () => {
  try {
    const response = await apiClient.get('/api/friends/requests')
    pendingRequests.value = response.data
  } catch (error: any) {
    console.error('加载好友请求失败:', error)
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

.user-email {
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

.dark-mode .search-results h3 {
  border-bottom: 1px solid #444;
}

.dark-mode .user-item {
  border-bottom: 1px solid #444;
}

.dark-mode .username {
  color: #f5f5f5; /* 设置暗色模式下用户名文字颜色 */
}

.dark-mode .user-email {
  color: #ccc;
}
</style>