<template>
  <div class="friend-list-view">
    <div class="friend-list-header">
      <h2>好友列表</h2>
      <div class="header-actions">
        <el-button type="primary" @click="goToAddFriend" class="add-friend-button">
          添加好友
        </el-button>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索好友"
          class="search-input"
          @keyup.enter="searchFriends"
        >
          <template #append>
            <el-button @click="searchFriends">
              <i class="el-icon-search"></i>
            </el-button>
          </template>
        </el-input>
      </div>
    </div>
    
    <div class="friend-requests" v-if="pendingRequests.length > 0">
      <h3>好友请求</h3>
      <div 
        v-for="request in pendingRequests" 
        :key="request.userId" 
        class="friend-request-item"
      >
        <el-avatar :src="getFullAvatarUrl(request.avatarUrl)" :size="40" />
        <div class="request-info">
          <div class="username">{{ request.userName }}</div>
          <div class="request-time">请求时间</div>
        </div>
        <div class="request-actions">
          <el-button 
            type="primary" 
            size="small" 
            @click="acceptFriendRequest(request.userId)"
          >
            接受
          </el-button>
          <el-button 
            type="danger" 
            size="small" 
            @click="rejectFriendRequest(request.userId)"
          >
            拒绝
          </el-button>
        </div>
      </div>
    </div>
    
    <div class="friends-list">
      <h3>好友</h3>
      <div 
        v-for="friend in filteredFriends" 
        :key="friend.userId" 
        class="friend-item"
        @click="startChat(friend)"
        @dblclick="startChatAndNavigate(friend)"
      >
        <el-avatar :src="getFullAvatarUrl(friend.avatarUrl)" :size="40" />
        <div class="friend-info">
          <div class="username">{{ friend.userName }}</div>
          <div class="friend-signature" v-if="friend.personalSignature">
            {{ friend.personalSignature }}
          </div>
          <div class="friend-signature empty" v-else>
            暂无签名
          </div>
          <div class="friend-email">{{ friend.email }}</div>
        </div>
        <div class="friend-status" :class="{ online: friend.loginStatus }">
          {{ friend.loginStatus ? '在线' : '离线' }}
        </div>
      </div>
      
      <div v-if="filteredFriends.length === 0 && !searchKeyword" class="empty-state">
        <p>暂无好友</p>
      </div>
      
      <div v-if="filteredFriends.length === 0 && searchKeyword" class="empty-state">
        <p>未找到匹配的好友</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import apiClient from '../services/api'
import { getFullAvatarUrl } from '../utils/user'

const router = useRouter()

// 定义响应式数据
const searchKeyword = ref('')
const friends = ref<any[]>([])
const pendingRequests = ref<any[]>([])
const allFriends = ref<any[]>([])

// 计算属性：过滤后的好友列表
const filteredFriends = ref<any[]>([])

// 跳转到添加好友页面
const goToAddFriend = () => {
  router.push('/add-friend')
}

// 搜索好友
const searchFriends = () => {
  if (!searchKeyword.value) {
    filteredFriends.value = allFriends.value
    return
  }
  
  filteredFriends.value = allFriends.value.filter(friend => 
    friend.userName.includes(searchKeyword.value)
  )
}

// 加载好友列表
const loadFriends = async () => {
  try {
    const response = await apiClient.get('/api/friends/list')
    allFriends.value = response.data
    filteredFriends.value = response.data
    friends.value = response.data
  } catch (error: any) {
    ElMessage.error('加载好友列表失败: ' + error.message)
  }
}

// 处理用户状态变更
const handleUserStatusChange = (data: any) => {
  console.log('好友状态变更:', data);
  
  // 更新好友列表中的用户状态
  const updatedFriends = allFriends.value.map(friend => {
    if (friend.userId === data.userId) {
      return {
        ...friend,
        loginStatus: data.status
      };
    }
    return friend;
  });
  
  allFriends.value = updatedFriends;
  filteredFriends.value = updatedFriends;
  friends.value = updatedFriends;
  
  // 同时更新好友请求列表中的用户状态（如果存在）
  const updatedPendingRequests = pendingRequests.value.map(request => {
    if (request.userId === data.userId) {
      return {
        ...request,
        loginStatus: data.status
      };
    }
    return request;
  });
  
  pendingRequests.value = updatedPendingRequests;
};

// 加载好友请求
const loadFriendRequests = async () => {
  try {
    const response = await apiClient.get('/api/friends/requests')
    pendingRequests.value = response.data
  } catch (error: any) {
    ElMessage.error('加载好友请求失败: ' + error.message)
  }
}

// 发送好友请求
const sendFriendRequest = async (friendId: string) => {
  try {
    await apiClient.post('/api/friends/request', null, {
      params: { friendId }
    })
    ElMessage.success('好友请求已发送')
  } catch (error: any) {
    ElMessage.error('发送好友请求失败: ' + error.message)
  }
}

// 接受好友请求
const acceptFriendRequest = async (requesterId: string) => {
  try {
    await apiClient.post('/api/friends/accept', null, {
      params: { requesterId }
    })
    ElMessage.success('好友请求已接受')
    // 重新加载好友列表和请求列表
    await loadFriends()
    await loadFriendRequests()
  } catch (error: any) {
    ElMessage.error('接受好友请求失败: ' + error.message)
  }
}

// 拒绝好友请求
const rejectFriendRequest = async (requesterId: string) => {
  try {
    await apiClient.post('/api/friends/reject', null, {
      params: { requesterId }
    })
    ElMessage.success('好友请求已拒绝')
    await loadFriendRequests()
  } catch (error: any) {
    ElMessage.error('拒绝好友请求失败: ' + error.message)
  }
}

// 开始聊天
const startChat = (friend: any) => {
  // 发送事件给父组件或路由跳转到聊天页面
  console.log('开始与', friend.userName, '聊天')
}

// 双击好友开始聊天并导航到聊天页面
const startChatAndNavigate = (friend: any) => {
  // 使用事件总线或全局状态管理来传递选中的好友信息
  // 这里我们使用localStorage来临时存储选中的好友信息
  localStorage.setItem('selectedFriendForChat', JSON.stringify(friend));
  
  // 跳转到聊天页面
  router.push('/chat');
}

// 组件挂载时加载数据
onMounted(() => {
  loadFriends()
  loadFriendRequests()
  
  // 监听全局用户状态变更事件
  window.addEventListener('userStatusChange', handleGlobalUserStatusChange);
})

// 组件卸载
onUnmounted(() => {
  // 移除全局用户状态变更事件监听器
  window.removeEventListener('userStatusChange', handleGlobalUserStatusChange);
})

// 处理全局用户状态变更事件
const handleGlobalUserStatusChange = (event: Event) => {
  const customEvent = event as CustomEvent;
  handleUserStatusChange(customEvent.detail);
};

</script>

<style scoped>
.friend-list-view {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
  background-color: #fff; /* 添加白色背景 */
}

.friend-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.friend-list-header h2 {
  margin: 0;
  color: #333; /* 设置标题文字颜色 */
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.add-friend-button {
  white-space: nowrap;
}

.search-input {
  width: 300px;
}

.friend-requests {
  margin-bottom: 30px;
}

.friend-requests h3,
.friends-list h3 {
  margin-top: 0;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
  color: #333; /* 设置标题文字颜色 */
}

.friend-request-item,
.friend-item {
  display: flex;
  align-items: center;
  padding: 12px 10px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
}

.friend-request-item:hover,
.friend-item:hover {
  background-color: #f5f5f5;
}

.request-info,
.friend-info {
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

.friend-signature,
.friend-email {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.friend-signature {
  color: #999;
  font-style: italic;
}

.friend-signature.empty {
  color: #bbb;
  font-style: normal;
}

.request-time,
.friend-email {
  color: #666;
}

.request-actions {
  display: flex;
  gap: 8px;
}

.friend-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  background-color: #f0f0f0;
  color: #333; /* 设置状态文字颜色 */
}

.friend-status.online {
  background-color: #e8f5e9;
  color: #4caf50;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #999;
}

/* 暗色模式样式 */
.dark-mode .friend-list-view {
  background-color: #1a1a1a;
  color: #f5f5f5;
}

.dark-mode .friend-list-header h2,
.dark-mode .friend-requests h3,
.dark-mode .friends-list h3 {
  color: #f5f5f5;
}

.dark-mode .friend-requests h3,
.dark-mode .friends-list h3 {
  border-bottom: 1px solid #444;
}

.dark-mode .friend-request-item,
.dark-mode .friend-item {
  border-bottom: 1px solid #444;
}

.dark-mode .friend-request-item:hover,
.dark-mode .friend-item:hover {
  background-color: #3d3d3d;
}

.dark-mode .username {
  color: #f5f5f5; /* 设置暗色模式下用户名文字颜色 */
}

.dark-mode .friend-signature,
.dark-mode .request-time,
.dark-mode .friend-email {
  color: #ccc;
}

.dark-mode .friend-signature.empty {
  color: #999;
}

.dark-mode .friend-status {
  background-color: #3d3d3d;
  color: #ccc;
}

.dark-mode .friend-status.online {
  background-color: #335a33;
  color: #a5d6a7;
}
</style>
