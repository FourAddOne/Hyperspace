<script setup lang="ts">
import { ref, onMounted, nextTick, computed, onUnmounted, watch, reactive, onActivated, watchEffect } from 'vue'
import { ElMessage } from 'element-plus'
import apiClient from '../services/api'
import { useRoute } from 'vue-router'
import { getFullAvatarUrl, getUserInfo } from '../utils/user'
import globalWebSocketManager from '../services/globalWebSocketManager'
import { useUserStore } from '../stores/userStore'

// 获取路由实例
const route = useRoute()

// 定义响应式数据
const conversations = ref<any[]>([])
const activeConversation = ref<any>(null)
const messages = ref<any[]>([])
const newMessage = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const unreadCounts = ref<Map<string, number>>(new Map()) // 添加未读消息计数
const unreadRefreshInterval = ref<number | null>(null) // 定时刷新未读消息的interval ID

// 获取用户store
const userStore = useUserStore()

// 背景设置
const backgroundSettings = ref({
  backgroundImage: '',
  backgroundOpacity: 100
})

// 计算背景样式
const backgroundStyle = computed(() => {
  if (backgroundSettings.value.backgroundImage) {
    return {
      backgroundImage: `url(${backgroundSettings.value.backgroundImage})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat',
      position: 'absolute',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      zIndex: 0
    }
  }
  // 如果没有设置聊天背景，则使用默认背景
  return {
    backgroundImage: 'none',
    backgroundColor: 'transparent',
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    zIndex: 0
  }
})


// 计算覆盖层样式
const overlayStyle = computed(() => {
  if (backgroundSettings.value.backgroundImage) {
    // 计算透明度 (0-1之间)
    const opacity = 1 - (backgroundSettings.value.backgroundOpacity / 100);
    return {
      backgroundColor: document.body.classList.contains('dark-mode') ? '#1a1a1a' : 'white',
      opacity: opacity,
      position: 'absolute',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      zIndex: 1
    }
  }
  // 如果没有背景图片，返回透明覆盖层
  return {
    backgroundColor: 'transparent',
    opacity: 0,
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    zIndex: 1
  }
})

// 格式化日期显示
const formatDate = (date: string) => {
  const messageDate = new Date(date);
  const today = new Date();

  // 检查是否是今天
  if (messageDate.toDateString() === today.toDateString()) {
    return '今天';
  }

  // 检查是否是昨天
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);
  if (messageDate.toDateString() === yesterday.toDateString()) {
    return '昨天';
  }

  // 其他日期格式化为年月日
  return `${messageDate.getFullYear()}年${messageDate.getMonth() + 1}月${messageDate.getDate()}日`;
}

// 监听暗色模式变化并应用到DOM
watch(() => userStore.getDarkMode, (newDarkMode) => {
  document.body.classList.toggle('dark-mode', newDarkMode);
  
  // 在亮色模式下清除body的背景图片
  if (!newDarkMode) {
    document.body.style.backgroundImage = 'none';
  }
})

// 更新背景设置
const updateBackgroundSettings = () => {
  const savedBackgroundImage = localStorage.getItem('userBackgroundImage')
  const savedBackgroundOpacity = localStorage.getItem('userBackgroundOpacity')

  if (savedBackgroundImage) {
    backgroundSettings.value.backgroundImage = savedBackgroundImage
  } else {
    backgroundSettings.value.backgroundImage = ''
  }

  if (savedBackgroundOpacity) {
    backgroundSettings.value.backgroundOpacity = parseInt(savedBackgroundOpacity, 10)
  } else {
    backgroundSettings.value.backgroundOpacity = 100
  }
  
  // 更新store中的背景状态
  userStore.setHasBackground(!!savedBackgroundImage)
}

// 添加处理暗色模式变化事件
const handleDarkModeChanged = (e: CustomEvent) => {
  const detail = e.detail as { darkMode: boolean };
  userStore.setDarkMode(detail.darkMode);
};

// 处理背景透明度变化事件
const handleBackgroundOpacityChanged = (e: CustomEvent) => {
  const detail = e.detail as { opacity: number };
  backgroundSettings.value.backgroundOpacity = detail.opacity;
};

// 添加处理背景图片变化事件
const handleBackgroundImageChanged = (e: CustomEvent) => {
  const detail = e.detail as { backgroundImage: string };
  backgroundSettings.value.backgroundImage = detail.backgroundImage;
  // 更新store中的背景状态
  userStore.setHasBackground(!!detail.backgroundImage);
};

// 处理存储变化事件
const handleStorageChanged = (e: StorageEvent) => {
  if (e.key === 'userBackgroundImage' || e.key === 'userBackgroundOpacity' || e.key === 'userDarkMode') {
    updateBackgroundSettings()
  }
};

// 处理用户状态变更
const handleUserStatusChange = (data: any) => {
  console.log('开始处理用户状态变更:', data);

  // 检查data是否有效
  if (!data || !data.userId) {
    console.log('无效的用户状态数据');
    return;
  }

  // 使用Vue的响应式系统更新状态
  // 直接修改数组元素以确保响应式更新
  for (let i = 0; i < conversations.value.length; i++) {
    if (conversations.value[i].id === data.userId) {
      console.log(`找到用户 ${data.userId}，原状态: ${conversations.value[i].status}`);
      // 直接修改数组元素的属性
      conversations.value[i].status = data.status ? '在线' : '离线';
      console.log(`用户状态已更新为: ${conversations.value[i].status}`);

      // 如果当前活跃会话是该用户，也更新活跃会话的状态
      if (activeConversation.value && activeConversation.value.id === data.userId) {
        console.log(`更新当前会话用户 ${data.userId} 状态`);
        activeConversation.value.status = data.status ? '在线' : '离线';
        console.log('当前会话已更新');
      }

      // 强制触发Vue的响应式更新
      conversations.value = [...conversations.value];
      break;
    }
  }
};

// 处理实时消息
const handleRealTimeMessage = (message: any) => {
  console.log('收到实时消息:', message);

  // 检查消息是否已存在（避免重复显示）
  const exists = messages.value.some(msg => msg.id === message.id);
  if (exists) {
    console.log('消息已存在，跳过:', message.id);
    return;
  }

  // 确定对话伙伴ID（不是当前用户ID的另一个用户）
  const partnerId = message.senderId === getUserInfo()?.userId ? message.receiverId : message.senderId;

  // 处理当前活跃会话的消息
  if (activeConversation.value && activeConversation.value.id === partnerId) {
    // 添加消息到列表
    const newMsg = {
      id: message.id || Date.now().toString(),
      text: message.content,
      time: message.createdAt ? new Date(message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }): new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      isSent: message.senderId === getUserInfo()?.userId,
      showDate: message.showDate || false, // 如果后端提供了showDate属性则使用，否则默认为false
      createdAt: message.createdAt || new Date().toISOString()
    };

    // 检查是否需要显示日期（如果这是当天第一条消息）
    if (messages.value.length === 0) {
      newMsg.showDate = true;
    } else {
      // 获取最后一条消息的日期
      const lastMessage = messages.value[messages.value.length - 1];
      if (lastMessage.createdAt) {
        const lastMessageDate = new Date(lastMessage.createdAt);
        const newMessageDate = new Date(newMsg.createdAt);

        // 如果日期不同，则显示日期
        if (lastMessageDate.toDateString() !== newMessageDate.toDateString()) {
          newMsg.showDate = true;
        }
      }
    }

    messages.value.push(newMsg);

    // 滚动到底部
    scrollToBottom();

    // 如果消息来自其他用户，标记为已读
    if (message.senderId !== getUserInfo()?.userId) {
      markMessagesAsRead(message.senderId);
    }
  }

  // 更新会话列表中的最后消息（无论是否是当前会话）
  const updatedConversations = conversations.value.map(conversation => {
    if (conversation.id === partnerId) {
      return {
        ...conversation,
        lastMessage: message.content,
        time: message.createdAt ? new Date(message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      };
    }
    return conversation;
  });

  conversations.value = updatedConversations;

  // 如果不是当前会话的消息，增加未读计数
  if (!activeConversation.value || activeConversation.value.id !== partnerId) {
    const currentCount = unreadCounts.value.get(partnerId) || 0;
    unreadCounts.value.set(partnerId, currentCount + 1);
  }
};

// 选择会话
const selectConversation = async (conversation: any) => {
  activeConversation.value = conversation
  // 清除未读计数
  unreadCounts.value.delete(conversation.id);
  // 加载消息
  await loadMessages(conversation.id)

  // 标记消息为已读
  if (conversation.id) {
    markMessagesAsRead(conversation.id);
  }

  // 确保在DOM更新后滚动到底部
  setTimeout(() => {
    scrollToBottom()
  }, 10)
}

// 加载消息
const loadMessages = async (conversationId: string) => {
  try {
    const response = await apiClient.get('/api/messages/history', {
      params: { friendId: conversationId }
    })

    // 确保响应数据存在且为数组
    if (response && Array.isArray(response.data)) {
      messages.value = response.data.map((msg: any) => ({
        id: msg.id,
        text: msg.content,
        time: msg.createdAt ? new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }): '',
        isSent: msg.senderId !== conversationId,
        showDate: msg.showDate || false,
        createdAt: msg.createdAt
      }))
    } else {
      // 如果没有消息或响应格式不正确，初始化为空数组
      messages.value = []
    }

    // 滚动到底部
    setTimeout(() => {
      scrollToBottom()
    }, 0)
  } catch (error: any) {
    // 检查是否是认证错误
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      ElMessage.error('登录已过期，请重新登录')
      return
    }

    ElMessage.error('加载消息失败: ' + (error.message || '未知错误'))
    // 清空消息列表而不是显示固定消息
    messages.value = []
  }
}

// 发送消息
const sendMessage = async () => {
  if (!newMessage.value.trim() || !activeConversation.value) {
    return
  }

  try {
    // 构造本地消息对象（用于立即显示）
    const localMessage = {
      id: 'temp_' + Date.now().toString(), // 临时ID
      text: newMessage.value,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      isSent: true,
      showDate: false,
      createdAt: new Date().toISOString()
    };

    // 立即添加到消息列表（优化用户体验）
    messages.value.push(localMessage);

    // 滚动到底部
    scrollToBottom();

    // 通过WebSocket发送消息
    if (globalWebSocketManager.isConnected()) {
      const messageDTO = {
        receiverId: activeConversation.value.id,
        content: newMessage.value,
        createdAt: new Date().toISOString()
      };

      globalWebSocketManager.sendMessage(messageDTO);

      // Clear input field
      newMessage.value = ''
    } else {
      ElMessage.error('WebSocket未连接，无法发送消息')
      // 如果发送失败，从消息列表中移除本地消息
      const index = messages.value.findIndex(msg => msg.id === localMessage.id);
      if (index !== -1) {
        messages.value.splice(index, 1);
      }
    }
  } catch (error: any) {
    // 检查是否是认证错误
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      ElMessage.error('登录已过期，请重新登录')
      return
    }

    ElMessage.error('发送消息失败: ' + (error.message || '未知错误'))
  }
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 监听路由变化
watch(() => route.path, (newPath) => {
  if (newPath === '/chat') {
    // 检查是否有从好友列表传递过来的选中好友
    const selectedFriend = localStorage.getItem('selectedFriendForChat');
    if (selectedFriend) {
      try {
        const friend = JSON.parse(selectedFriend);
        // 创建一个临时会话对象
        const tempConversation = {
          id: friend.userId,
          name: friend.remark || friend.userName,
          avatar: getFullAvatarUrl(friend.avatarUrl) || '/src/assets/logo.svg',
          lastMessage: '',
          time: '',
          status: friend.loginStatus ? '在线' : '离线'
        };

        // 选择这个会话
        selectConversation(tempConversation);

        // 清除localStorage中的临时数据
        localStorage.removeItem('selectedFriendForChat');
      } catch (e) {
        console.error('解析选中好友信息失败:', e);
      }
    }
  }
});

// 加载会话列表
const loadConversations = async () => {
  try {
    const response = await apiClient.get('/api/friends/list')
    conversations.value = response.data.map((friend: any) => ({
      id: friend.userId,
      name: friend.remark || friend.userName,
      avatar: getFullAvatarUrl(friend.avatarUrl) || '/src/assets/logo.svg',
      lastMessage: '',
      time: '',
      status: friend.loginStatus ? '在线' : '离线'
    }))

    // 获取未读消息数量
    if (conversations.value.length > 0) {
      const friendIds = conversations.value.map((conv: any) => conv.id);
      const unreadResponse = await apiClient.get('/api/messages/unread-counts', {
        params: { friendIds }
      });

      if (unreadResponse && unreadResponse.data) {
        // 更新未读消息计数
        Object.entries(unreadResponse.data).forEach(([friendId, count]) => {
          if (count > 0) {
            unreadCounts.value.set(friendId, count as number);
          }
        });
      }
    }
  } catch (error: any) {
    console.error('加载会话列表失败:', error)
    ElMessage.error('加载会话列表失败: ' + error.message)
  }
}

// 标记消息为已读
const markMessagesAsRead = async (friendId: string) => {
  try {
    await apiClient.post('/api/messages/mark-as-read', null, {
      params: { friendId }
    });
  } catch (error) {
    console.error('标记消息为已读失败:', error);
  }
}

// 刷新未读消息数量
const refreshUnreadCounts = async () => {
  if (conversations.value.length > 0) {
    try {
      const friendIds = conversations.value.map((conv: any) => conv.id);
      const unreadResponse = await apiClient.get('/api/messages/unread-counts', {
        params: { friendIds }
      });

      if (unreadResponse && unreadResponse.data) {
        // 只更新未读计数大于0的会话，不清空其他已读会话的计数
        Object.entries(unreadResponse.data).forEach(([friendId, count]) => {
          if (count > 0) {
            unreadCounts.value.set(friendId, count as number);
          } else {
            // 如果未读数为0，确保从Map中删除该会话
            unreadCounts.value.delete(friendId);
          }
        });
      }
    } catch (error) {
      console.error('刷新未读消息数量失败:', error);
    }
  }
}

// 组件挂载
onMounted(() => {
  // 监听全局用户状态变更事件
  window.addEventListener('userStatusChange', handleGlobalUserStatusChange);

  // 监听全局实时消息事件
  window.addEventListener('realTimeMessage', handleGlobalRealTimeMessage);

  // 加载会话列表
  loadConversations()

  // 检查并应用暗色模式
  const savedDarkMode = localStorage.getItem('userDarkMode')
  if (savedDarkMode === 'true') {
    document.body.classList.add('dark-mode')
  } else {
    // 确保在亮色模式下去除全局背景
    document.body.style.backgroundImage = 'none';
    document.body.classList.remove('login-page');
  }

  // 初始化背景设置
  updateBackgroundSettings()

  // 监听自定义背景透明度变化事件
  window.addEventListener('backgroundOpacityChanged', handleBackgroundOpacityChanged as EventListener);
  
  // 监听自定义背景图片变化事件
  window.addEventListener('backgroundImageChanged', handleBackgroundImageChanged as EventListener);
  
  // 监听暗色模式变化事件
  window.addEventListener('darkModeChanged', handleDarkModeChanged as EventListener);

  // 监听localStorage变化
  window.addEventListener('storage', handleStorageChanged);

  // 设置定时刷新未读消息数量
  unreadRefreshInterval.value = window.setInterval(() => {
    refreshUnreadCounts();
  }, 30000); // 每30秒刷新一次
})

// 组件卸载
onUnmounted(() => {
  // 移除全局用户状态变更事件监听器
  window.removeEventListener('userStatusChange', handleGlobalUserStatusChange);

  // 移除全局实时消息事件监听器
  window.removeEventListener('realTimeMessage', handleGlobalRealTimeMessage);

  // 清理事件监听器
  window.removeEventListener('backgroundOpacityChanged', handleBackgroundOpacityChanged as EventListener);
  window.removeEventListener('backgroundImageChanged', handleBackgroundImageChanged as EventListener);
  window.removeEventListener('darkModeChanged', handleDarkModeChanged as EventListener);
  window.removeEventListener('storage', handleStorageChanged);

  // 清理定时器
  if (unreadRefreshInterval.value) {
    clearInterval(unreadRefreshInterval.value);
  }
})

// 组件激活时检查是否有需要选择的好友
onActivated(() => {
  // 检查是否有从好友列表传递过来的选中好友
  const selectedFriend = localStorage.getItem('selectedFriendForChat');
  if (selectedFriend) {
    try {
      const friend = JSON.parse(selectedFriend);
      // 创建一个临时会话对象
      const tempConversation = {
        id: friend.userId,
        name: friend.remark || friend.userName,
        avatar: getFullAvatarUrl(friend.avatarUrl) || '/src/assets/logo.svg',
        lastMessage: '',
        time: '',
        status: friend.loginStatus ? '在线' : '离线'
      };

      // 选择这个会话
      selectConversation(tempConversation);

      // 清除localStorage中的临时数据
      localStorage.removeItem('selectedFriendForChat');
    } catch (e) {
      console.error('解析选中好友信息失败:', e);
    }
  }
});

// 处理全局用户状态变更事件
const handleGlobalUserStatusChange = (event: Event) => {
  console.log('ChatView收到用户状态变更事件');
  const customEvent = event as CustomEvent;
  handleUserStatusChange(customEvent.detail);
};

// 处理全局实时消息事件
const handleGlobalRealTimeMessage = (event: Event) => {
  console.log('ChatView收到实时消息事件');
  const customEvent = event as CustomEvent;
  handleRealTimeMessage(customEvent.detail);
};
</script>






<template>
  <div class="chat-view">
    <div class="chat-container">
      <div class="chat-sidebar">
        <div class="chat-sidebar-header">
          <h2>聊天</h2>
        </div>
        
        <div class="conversations-list">
          <div 
            v-for="conversation in conversations" 
            :key="conversation.id"
            class="conversation-item"
            :class="{ active: activeConversation?.id === conversation.id }"
            @click="selectConversation(conversation)"
          >
            <el-avatar :src="getFullAvatarUrl(conversation.avatar)" />
            <div class="conversation-info">
              <div class="conversation-name">{{ conversation.name }}</div>
              <div class="conversation-last-message">{{ conversation.lastMessage }}</div>
            </div>
            <div class="conversation-time">{{ conversation.time }}</div>
            <div class="conversation-status" :class="{ online: conversation.status === '在线' }"></div>
            <!-- 未读消息红点 -->
            <div v-if="unreadCounts.get(conversation.id) > 0" class="unread-badge">
              {{ unreadCounts.get(conversation.id) > 99 ? '99+' : unreadCounts.get(conversation.id) }}
            </div>
          </div>
        </div>
      </div>
      
      <div class="chat-main">
        <div class="chat-header" v-if="activeConversation">
          <div class="chat-header-info">
            <el-avatar :size="40" :src="getFullAvatarUrl(activeConversation.avatar)" />
            <div class="chat-header-text">
              <div class="chat-participant-name">{{ activeConversation.name }}</div>
              <div class="chat-status">{{ activeConversation.status }}</div>
            </div>
          </div>
          <div class="chat-header-actions">
            <el-button icon="el-icon-more" circle></el-button>
          </div>
        </div>
        
        <!-- 背景容器 -->
        <div class="chat-background-container" v-if="activeConversation">
          <!-- 背景图片层 -->
          <div 
            class="message-background" 
            v-if="backgroundSettings.backgroundImage"
            :style="backgroundStyle"
          ></div>
          <!-- 覆盖层用于控制透明度 -->
          <div 
            class="message-overlay" 
            v-if="backgroundSettings.backgroundImage"
            :style="overlayStyle"
          ></div>
          
            <div class="chat-messages-container" ref="messagesContainer">
            <div class="chat-messages">
              <template v-for="message in messages" :key="message.id">
                <div v-if="message.showDate" class="message-date-divider">
                  {{ formatDate(message.createdAt) }}
                </div>
                <div 
                  class="message"
                  :class="{ 'sent': message.isSent, 'received': !message.isSent }"
                >
                  <el-avatar v-if="!message.isSent" :src="getFullAvatarUrl(activeConversation.avatar)" class="message-avatar" />
                  <div class="message-content">
                     <div class="message-sender-placeholder" v-if="message.isSent"></div>
                    <div class="message-sender" v-if="!message.isSent">{{ activeConversation.name }}</div>
                    <div class="message-text">{{ message.text }}</div>
                    <div class="message-time">{{ message.time }}</div>
                  </div>
                  <el-avatar v-if="message.isSent" :src="getFullAvatarUrl(getUserInfo()?.avatarUrl)" class="message-avatar" />
                </div>
              </template>
            </div>
          </div>
        </div>
        
        <div class="chat-input-area" v-if="activeConversation">
          <div class="chat-input-tools">
            <el-button icon="el-icon-picture" circle size="small"></el-button>
            <el-button icon="el-icon-folder" circle size="small"></el-button>
          </div>
          <el-input
            v-model="newMessage"
            type="textarea"
            :rows="2"
            placeholder="输入消息..."
            @keyup.enter="sendMessage"
            class="message-input"
          ></el-input>
          <div class="chat-input-actions">
            <el-button 
              type="primary" 
              @click="sendMessage"
              :disabled="!newMessage.trim()"
              size="small"
            >
              发送
            </el-button>
          </div>
        </div>
        
        <div class="chat-placeholder" v-else>
          <div class="placeholder-content">
            <i class="el-icon-chat-dot-round placeholder-icon"></i>
            <p>选择一个聊天或开始新对话</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>


<style scoped>
.chat-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--background-color);
  color: var(--text-color);
}

.chat-container {
  display: flex;
  flex: 1;
  min-height: 0;
}

.chat-sidebar {
  width: 300px;
  border-right: 1px solid #ddd;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
}

.dark-mode .chat-sidebar {
  border-right: 1px solid #444;
  background-color: #1a1a1a;
}

.chat-sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #ddd;
  background-color: #ffffff;
}

.dark-mode .chat-sidebar-header {
  border-bottom: 1px solid #444;
  background-color: #2d2d2d;
}

.chat-sidebar-header h2 {
  margin: 0;
  font-size: 18px;
  color: #1a1a1a;
}

.dark-mode .chat-sidebar-header h2 {
  color: #f5f5f5;
}

.conversations-list {
  flex: 1;
  overflow-y: auto;
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  cursor: pointer;
  position: relative;
  border-bottom: 1px solid #eee;
  background-color: #ffffff;
}

.dark-mode .conversation-item {
  border-bottom: 1px solid #333;
  background-color: #1a1a1a;
}

.conversation-item:hover {
  background-color: #f9f9f9;
}

.dark-mode .conversation-item:hover {
  background-color: #2a2a2a;
}

.conversation-item.active {
  background-color: #e0f0ff;
}

.dark-mode .conversation-item.active {
  background-color: #1a3a4a;
}

.conversation-item .el-avatar {
  margin-right: 15px;
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-name {
  font-weight: 500;
  margin-bottom: 4px;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dark-mode .conversation-name {
  color: #f5f5f5;
}

.conversation-last-message {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dark-mode .conversation-last-message {
  color: #ccc;
}

.conversation-time {
  font-size: 12px;
  color: #666;
  margin-right: 10px;
}

.dark-mode .conversation-time {
  color: #ccc;
}

.conversation-status {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: #ccc;
  margin-right: 5px;
}

.conversation-status.online {
  background-color: #4caf50;
}

/* 未读消息红点 */
.unread-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background-color: #f56c6c;
  color: white;
  border-radius: 10px;
  padding: 0 5px;
  font-size: 12px;
  min-width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  position: relative;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 20px;
  border-bottom: 1px solid #ddd;
  background-color: #ffffff;
}

.dark-mode .chat-header {
  border-bottom: 1px solid #444;
  background-color: #2d2d2d;
}

.chat-header-info {
  display: flex;
  align-items: center;
}

.chat-header-text {
  margin-left: 15px;
}

.chat-participant-name {
  font-weight: 500;
  font-size: 16px;
  color: #1a1a1a;
}

.dark-mode .chat-participant-name {
  color: #f5f5f5;
}

.chat-status {
  font-size: 12px;
  color: #666;
}

.dark-mode .chat-status {
  color: #aaa;
}

.chat-background-container {
  flex: 1;
  position: relative;
  overflow: hidden;
  background-color: #ffffff; /* 添加默认背景色 */
}

.dark-mode .chat-background-container {
  background-color: #1a1a1a; /* 暗色模式下的背景色 */
}

.chat-messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 2;
  height: calc(100vh - 200px);
}

.chat-messages {
  display: flex;
  flex-direction: column;
}

.message-date-divider {
  text-align: center;
  padding: 10px 0;
  color: #666;
  font-size: 12px;
}

.dark-mode .message-date-divider {
  color: #ccc;
}

.message {
  display: flex;
  margin-bottom: 15px;
  max-width: 80%;
  position: relative;
}

.message.received {
  align-self: flex-start;
}

.message.sent {
  align-self: flex-end;
  flex-direction: row;
  margin-left: auto;
}

.message-avatar {
  flex-shrink: 0;
  align-self: flex-start;
  margin-top: -5px;
}

.message.received .message-avatar {
  margin-right: 10px;
}

.message.sent .message-avatar {
  order: 2;
  margin-left: 10px;
}

.message-sender-placeholder {
  height: 16px;
  margin-bottom: 4px;
}

.message-sender {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.dark-mode .message-sender {
  color: #ccc;
}

.message-text {
  padding: 10px 15px;
  border-radius: 18px;
  word-wrap: break-word;
  max-width: 100%;
  color: #1a1a1a;
}

.dark-mode .message-text {
  color: #f5f5f5;
}

.message.received .message-text {
  background-color: #f5f5f5;
  border-top-left-radius: 4px;
  color: #1a1a1a;
}

.dark-mode .message.received .message-text {
  background-color: #333;
  color: #f5f5f5;
}

.message.sent .message-text {
  background-color: #409eff;
  color: white;
  border-top-right-radius: 4px;
}

.message-time {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
  align-self: flex-start;
  text-align: left;
}

.message.received .message-time {
  align-self: flex-start;
  text-align: left;
}

.message.sent .message-time {
  align-self: flex-end;
  text-align: right;
}

.dark-mode .message-time {
  color: #ccc;
}

.chat-input-area {
  padding: 15px;
  border-top: 1px solid #ddd;
  background-color: #ffffff;
  position: relative;
  z-index: 2;
}

.dark-mode .chat-input-area {
  border-top: 1px solid #444;
  background-color: #1a1a1a;
}

/* Element Plus 输入框在暗色模式下的样式 */
.dark-mode .message-input :deep(.el-textarea__inner) {
  background-color: #1a1a1a;
  border-color: #444;
  color: #f5f5f5;
}

.dark-mode .message-input :deep(.el-textarea__inner:focus) {
  border-color: #409eff;
}

.dark-mode .message-input :deep(.el-textarea .el-input__count) {
  color: #ccc;
}

.chat-input-tools {
  margin-bottom: 10px;
}

.message-input {
  margin-bottom: 10px;
}

.chat-input-actions {
  display: flex;
  justify-content: flex-end;
}

.chat-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ffffff;
}

.dark-mode .chat-placeholder {
  background-color: #1a1a1a;
}

.placeholder-content {
  text-align: center;
  color: #999;
}

.dark-mode .placeholder-content {
  color: #ccc;
  background-color: #1a1a1a;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
}
</style>