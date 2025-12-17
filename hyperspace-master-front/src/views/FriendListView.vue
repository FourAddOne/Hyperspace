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
        />
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
            {{ friend.personalSignature.length > 30 ? friend.personalSignature.substring(0, 30) + '...' : friend.personalSignature }}
          </div>
          <div class="friend-signature empty" v-else>
            暂无签名
          </div>
          <div class="friend-email">{{ friend.email }}</div>
        </div>
        <div class="friend-status" :class="{ online: friend.loginStatus }">
          {{ friend.loginStatus ? '在线' : '离线' }}
        </div>
        <div class="friend-actions">
          <el-button size="small" type="primary" @click.stop="editRemark(friend)" class="action-button">备注</el-button>
          <el-button size="small" type="danger" @click.stop="deleteFriend(friend.userId)" class="action-button">删除</el-button>
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
  
  <!-- 备注编辑对话框 -->
  <div v-if="showRemarkDialog" class="dialog-overlay" @click="showRemarkDialog = false">
    <div class="dialog-content" @click.stop>
      <div class="dialog-header">
        <h3>编辑备注</h3>
        <button class="dialog-close" @click="showRemarkDialog = false">×</button>
      </div>
      <div class="dialog-body">
        <div class="form-group">
          <label>为 "{{ currentFriend?.userName }}" 设置备注:</label>
          <input 
            v-model="remarkInput" 
            type="text" 
            class="form-input"
            maxlength="20"
            placeholder="请输入备注名（最多20个字符）"
          />
        </div>
      </div>
      <div class="dialog-footer">
        <button class="cancel-button" @click="showRemarkDialog = false">取消</button>
        <button class="save-button" @click="saveRemark">保存</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, onActivated, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
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

// 备注编辑相关
const showRemarkDialog = ref(false)
const currentFriend = ref<any>(null)
const remarkInput = ref('')

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
  
  // 检查data是否有效
  if (!data || !data.userId) {
    console.log('无效的用户状态数据');
    return;
  }
  
  let updated = false;
  
  // 更新好友列表中的用户状态
  for (let i = 0; i < allFriends.value.length; i++) {
    if (allFriends.value[i].userId === data.userId) {
      // 直接修改数组元素的属性以确保响应式更新
      allFriends.value[i].loginStatus = data.status;
      console.log(`好友 ${data.userId} 状态已更新为: ${data.status ? '在线' : '离线'}`);
      updated = true;
      break;
    }
  }
  
  if (updated) {
    // 强制触发Vue的响应式更新
    allFriends.value = [...allFriends.value];
    
    // 更新过滤后的好友列表
    filteredFriends.value = [...allFriends.value];
    
    // 更新好友列表
    friends.value = [...allFriends.value];
  }
  
  // 同时更新好友请求列表中的用户状态（如果存在）
  for (let i = 0; i < pendingRequests.value.length; i++) {
    if (pendingRequests.value[i].userId === data.userId) {
      // 直接修改数组元素的属性以确保响应式更新
      pendingRequests.value[i].loginStatus = data.status;
      console.log(`请求中的好友 ${data.userId} 状态已更新为: ${data.status ? '在线' : '离线'}`);
      
      // 强制触发Vue的响应式更新
      pendingRequests.value = [...pendingRequests.value];
    }
  }
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

// 删除好友
const deleteFriend = (friendId: string) => {
  ElMessageBox.confirm('确定要删除这个好友吗？', '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await apiClient.delete('/api/friends/delete', {
        params: { friendId }
      });
      ElMessage.success('好友已删除');
      // 重新加载好友列表
      await loadFriends();
    } catch (error: any) {
      ElMessage.error('删除好友失败: ' + error.message);
    }
  }).catch(() => {
    // 取消删除
  });
};

// 编辑备注
const editRemark = (friend: any) => {
  currentFriend.value = friend;
  remarkInput.value = friend.userName; // 默认使用当前用户名作为备注
  showRemarkDialog.value = true;
};

// 保存备注
const saveRemark = async () => {
  if (!currentFriend.value) return;
  
  try {
    await apiClient.post('/api/friends/remark', null, {
      params: { 
        friendId: currentFriend.value.userId,
        remark: remarkInput.value
      }
    });
    ElMessage.success('备注已更新');
    showRemarkDialog.value = false;
    // 重新加载好友列表以显示更新后的备注
    await loadFriends();
  } catch (error: any) {
    ElMessage.error('更新备注失败: ' + error.message);
  }
};

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

// 添加activated钩子确保组件激活时WebSocket回调正确设置
onActivated(() => {
  // 确保WebSocket回调设置
})

// 处理全局用户状态变更事件
const handleGlobalUserStatusChange = (event: Event) => {
  console.log('FriendListView收到用户状态变更事件');
  const customEvent = event as CustomEvent;
  handleUserStatusChange(customEvent.detail);
};

// 监听搜索关键词变化
watch(searchKeyword, () => {
  searchFriends();
});
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.add-friend-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.search-input {
  width: 300px;
}

.search-input :deep(.el-input__wrapper) {
  border: none;
  background: transparent;
  box-shadow: none;
  padding: 0;
}

.search-input :deep(.el-input__wrapper) {
  border: none;
  background: transparent;
  box-shadow: none;
  padding: 0;
}

.search-input :deep(.el-input__inner) {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 8px 15px;
  transition: all 0.3s ease;
}

.search-input :deep(.el-input__inner:focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
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
  margin-right: 15px; /* 增加与操作按钮的间距 */
}

.friend-status.online {
  background-color: #e8f5e9;
  color: #4caf50;
}

.friend-actions {
  display: flex;
  gap: 8px; /* 按钮之间的间距 */
}

.action-button {
  border-radius: 15px; /* 圆角按钮 */
  font-size: 12px; /* 调整字体大小 */
  padding: 4px 10px; /* 调整内边距 */
  transition: all 0.3s ease; /* 添加过渡效果 */
}

.action-button:hover {
  transform: scale(1.05); /* 悬停时轻微放大 */
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

.dark-mode .add-friend-button {
  background: linear-gradient(135deg, #4a5568 0%, #2d3748 100%);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
}

.dark-mode .add-friend-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

.dark-mode .search-input :deep(.el-input__inner) {
  background-color: #2d3748;
  border-color: #4a5568;
  color: #f7fafc;
}

.dark-mode .search-input :deep(.el-input__inner:focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);
}

.dark-mode .search-input :deep(.el-input__wrapper) {
  background: transparent;
  box-shadow: none;
  border: none;
}

/* 已移除搜索按钮相关样式 */

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

.dark-mode .action-button {
  border: none;
}

.dark-mode .action-button:hover {
  opacity: 0.9;
  transform: scale(1.05);
}
</style>