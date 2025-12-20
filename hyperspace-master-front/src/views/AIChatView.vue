<script setup lang="ts">
import { ref, onMounted, nextTick, computed, onUnmounted, watch, reactive, onActivated } from 'vue'
import { ElMessage } from 'element-plus'
import apiClient from '../services/api'
import { getFullAvatarUrl, getUserInfo } from '../utils/user'

// 定义响应式数据
const activeConversation = ref<any>(null)
const messages = ref<any[]>([])
const newMessage = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const selectedModel = ref('deepseek-r1:8b') // 默认选择deepseek-r1:8b模型
const availableModels = [
  { value: 'deepseek-r1:8b', label: 'DeepSeek R1:8B' },
  { value: 'llama3.2', label: 'Llama 3.2' },
  { value: 'gemma2:9b', label: 'Gemma 2:9B' },
  { value: 'mistral:7b', label: 'Mistral:7B' }
] // 可用模型列表

// 背景设置
const backgroundSettings = ref({
  backgroundImage: '',
  backgroundOpacity: 100
})

// 计算背景样式
const backgroundStyle = computed(() => {
  return {
    backgroundImage: 'none',
    backgroundColor: 'white',
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

// 更新背景设置
const updateBackgroundSettings = () => {
  const savedBackgroundImage = typeof localStorage !== 'undefined' ? localStorage.getItem('userBackgroundImage') : null
  const savedBackgroundOpacity = typeof localStorage !== 'undefined' ? localStorage.getItem('userBackgroundOpacity') : null

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
}

// 添加处理暗色模式变化事件
const handleDarkModeChanged = (e: CustomEvent) => {
  // 忽略暗色模式变化，始终保持亮色模式（白色背景）
  document.body.classList.remove('dark-mode');
  document.body.style.backgroundImage = 'none';
};

// 处理背景透明度变化事件
const handleBackgroundOpacityChanged = (e: CustomEvent) => {
  const detail = e.detail as { opacity: number };
  backgroundSettings.value.backgroundOpacity = detail.opacity;
};

// 处理背景图片变化事件
const handleBackgroundImageChanged = (e: CustomEvent) => {
  const detail = e.detail as { backgroundImage: string };
  backgroundSettings.value.backgroundImage = detail.backgroundImage;
};

// 处理存储变化事件
const handleStorageChanged = (e: StorageEvent) => {
  if (e.key === 'userBackgroundImage' || e.key === 'userBackgroundOpacity' || e.key === 'userDarkMode') {
    updateBackgroundSettings()
  }
};

// 选择AI聊天机器人会话
const selectAIChatBotConversation = () => {
  // 创建AI聊天机器人会话对象
  const chatBotConversation = {
    id: 'chatbot',
    name: 'AI助手',
    avatar: '/src/assets/logo.svg',
    lastMessage: '',
    time: '',
    status: '在线',
    isBot: true
  };
  activeConversation.value = chatBotConversation;
};

// 加载消息
const loadMessages = async () => {
  // AI聊天机器人会话，暂时不加载历史消息
  messages.value = [];
};

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

    // 保存消息内容，用于聊天机器人响应
    const messageContent = newMessage.value;
    // 清空输入框
    newMessage.value = '';

    // 如果是聊天机器人会话
    if (activeConversation.value.isBot) {
      try {
        // 调用后端聊天机器人API（增加超时时间到30秒）
        const response = await apiClient.post('/chatbot/send', {
            userId: getUserInfo()?.userId,
            message: messageContent,
            model: selectedModel.value
        }, {
            timeout: 30000, // 30秒超时
            headers: {
                'Content-Type': 'application/json', // 指定发送JSON格式数据
                'Accept': 'application/json' // 指定接受JSON格式响应
            }
        });

        // 构造机器人回复消息
        const botMessage = {
          id: 'bot_' + Date.now().toString(),
          text: response.data, // 获取AI回复内容（response是Result对象，需要访问data字段）
          time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          isSent: false,
          showDate: false,
          createdAt: new Date().toISOString()
        };

        // 添加到消息列表
        messages.value.push(botMessage);
        // 滚动到底部
        scrollToBottom();
      } catch (error: any) {
        ElMessage.error('机器人回复失败: ' + (error.message || '未知错误'))
      }
    }
  } catch (error: any) {
    ElMessage.error('发送消息失败: ' + (error.message || '未知错误'))
  }
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      // 使用smooth滚动获得更好的用户体验
      messagesContainer.value.scrollTo({
        top: messagesContainer.value.scrollHeight,
        behavior: 'smooth'
      });
    }
  })
}

// 组件挂载
onMounted(() => {
  // 选择AI聊天机器人会话
  selectAIChatBotConversation();
  // 加载消息
  loadMessages();

  // 确保始终使用亮色模式（白色背景）
  document.body.classList.remove('dark-mode')
  document.body.style.backgroundImage = 'none';
  document.body.classList.remove('login-page');

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
})

// 组件激活
onActivated(() => {
  // 确保AI聊天机器人会话被选中
  selectAIChatBotConversation();
});

// 组件卸载
onUnmounted(() => {
  // 清理事件监听器
  window.removeEventListener('backgroundOpacityChanged', handleBackgroundOpacityChanged as EventListener);
  window.removeEventListener('backgroundImageChanged', handleBackgroundImageChanged as EventListener);
  window.removeEventListener('darkModeChanged', handleDarkModeChanged as EventListener);
  window.removeEventListener('storage', handleStorageChanged);
})
</script>

<template>
  <div class="chat-container">
    <!-- 背景层 -->
    <div class="background-layer" :style="backgroundStyle"></div>
    <!-- 背景覆盖层 -->
    <div class="background-overlay" :style="overlayStyle"></div>

    <!-- 聊天界面主体 -->
    <div class="chat-main">
      <!-- 聊天头部 -->
      <div class="chat-header">
        <div class="chat-header-info">
          <img 
            :src="activeConversation?.avatar || '/src/assets/logo.svg'" 
            alt="头像" 
            class="chat-avatar"
          />
          <div class="chat-title">
            <div class="chat-name">{{ activeConversation?.name || 'AI助手' }}</div>
            <div class="chat-status">在线</div>
          </div>
        </div>
        <!-- 模型选择下拉菜单 -->
        <div class="model-selector">
          <select v-model="selectedModel" class="model-select">
            <option v-for="model in availableModels" :key="model.value" :value="model.value">
              {{ model.label }}
            </option>
          </select>
        </div>
      </div>

      <!-- 聊天消息区域 -->
      <div class="messages-container" ref="messagesContainer">
        <!-- 消息列表 -->
        <div class="messages-list">
          <!-- 日期分隔线 -->
          <div class="date-divider" v-if="messages.length > 0">
            {{ formatDate(messages[0].createdAt) }}
          </div>
          
          <!-- 消息项 -->
          <div 
            v-for="(message, index) in messages" 
            :key="message.id"
            :class="['message-item', message.isSent ? 'sent' : 'received']"
          >
            <!-- 接收消息 (AI消息) -->
            <div v-if="!message.isSent" class="message-wrapper received">
              <!-- AI头像 -->
              <img 
                :src="activeConversation?.avatar || '/src/assets/logo.svg'" 
                alt="AI助手头像" 
                class="message-avatar"
              />
              
              <!-- 消息内容 -->
              <div class="message-content received">
                <div class="message-text received">
                  {{ message.text }}
                </div>
                <div class="message-time">{{ message.time }}</div>
              </div>
            </div>
            
            <!-- 发送消息 (用户消息) -->
            <div v-else class="message-wrapper sent">
              <!-- 用户头像 -->
              <img 
                :src="getFullAvatarUrl(getUserInfo()?.avatarUrl) || '/src/assets/logo.svg'" 
                alt="用户头像" 
                class="message-avatar"
              />
              
              <!-- 消息内容 -->
              <div class="message-content sent">
                <div class="message-text sent">
                  {{ message.text }}
                </div>
                <div class="message-time">{{ message.time }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 消息输入区域 -->
      <div class="message-input-container">
        <div class="message-input-wrapper">
          <textarea 
            v-model="newMessage" 
            class="message-input"
            placeholder="输入消息..."
            rows="1"
            @keydown.enter.exact="sendMessage"
            @keydown.enter.shift=""
          ></textarea>
        </div>
        <button class="send-button" @click="sendMessage">
          发送
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-container {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.background-layer {
  /* 背景样式在计算属性中定义 */
}

.background-overlay {
  /* 背景覆盖层样式在计算属性中定义 */
}

.chat-main {
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
  z-index: 2;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 10px 20px;
  border-bottom: 1px solid #e0e0e0;
  background-color: rgba(255, 255, 255, 0.9);
}

.chat-header-info {
  display: flex;
  align-items: center;
}

.chat-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 10px;
}

.chat-title {
  display: flex;
  flex-direction: column;
}

.chat-name {
  font-weight: 500;
  font-size: 16px;
}

.chat-status {
  font-size: 12px;
  color: #67c23a;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

/* 确保消息从顶部开始排列 */
.date-divider {
  text-align: center;
  color: #999;
  font-size: 12px;
  margin: 10px 0;
  flex-shrink: 0;
}

.message-item {
  display: flex;
  margin-bottom: 15px;
  flex-shrink: 0;
}

.message-wrapper {
  display: flex;
  gap: 10px;
  max-width: 50%;
}

.message-wrapper.received {
  margin-right: auto;
}

.message-wrapper.sent {
  margin-left: auto;
}

.message-wrapper.sent {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  flex-shrink: 0;
  object-fit: cover;
  object-position: center center;
  align-self: flex-start; /* 让头像对齐到顶部 */
}

.message-content {
  display: flex;
  flex-direction: column;
}

.message-content.received {
  align-items: flex-start;
}

.message-content.sent {
  align-items: flex-end;
}

.message-text {
  padding: 10px 15px;
  border-radius: 18px;
  word-wrap: break-word;
  max-width: 100%;
  white-space: pre-wrap;
  position: relative;
}

.message-text.sent {
  background-color: #0084ff;
  color: white;
  border-bottom-right-radius: 4px;
}

/* 发送消息气泡尖角 */
.message-text.sent::before {
  content: '';
  position: absolute;
  right: -8px;
  top: 12px;
  width: 0;
  height: 0;
  border: 8px solid transparent;
  border-left-color: #0084ff;
  border-right: 0;
}

.message-text.received {
  background-color: #f5f5f5;
  color: #333;
  border-bottom-left-radius: 4px;
}

/* 接收消息气泡尖角 */
.message-text.received::before {
  content: '';
  position: absolute;
  left: -8px;
  top: 12px;
  width: 0;
  height: 0;
  border: 8px solid transparent;
  border-right-color: #f5f5f5;
  border-left: 0;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.message-wrapper.received .message-time {
  align-self: flex-start;
}

.message-wrapper.sent .message-time {
  align-self: flex-end;
}

.message-input-container {
  display: flex;
  padding: 10px 20px;
  border-top: 1px solid #e0e0e0;
  background-color: rgba(255, 255, 255, 0.9);
  align-items: flex-end;
  gap: 10px;
}

.message-input-wrapper {
  flex: 1;
  position: relative;
}

.message-input {
  width: 100%;
  min-height: 40px;
  max-height: 120px;
  padding: 10px 15px;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  resize: none;
  outline: none;
  font-size: 14px;
  font-family: inherit;
}

.send-button {
  padding: 10px 20px;
  background-color: #0084ff;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.2s;
  min-height: 40px;
}

.send-button:hover {
  background-color: #0066cc;
}

/* 模型选择器样式 */
.model-selector {
  margin-left: auto;
}

.model-select {
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 18px;
  background-color: white;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s;
}

.model-select:hover {
  border-color: #0084ff;
}

.model-select:focus {
  border-color: #0084ff;
  box-shadow: 0 0 0 2px rgba(0, 132, 255, 0.2);
}

/* 滚动条样式 */
.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.messages-container::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>

<!-- 暗色模式样式 -->
<style>
.dark-mode .chat-header {
  background-color: rgba(26, 26, 26, 0.9);
  border-bottom-color: #444;
}

.dark-mode .chat-name {
  color: #f5f5f5;
}

.dark-mode .message-text.received {
  background-color: #3d3d3d;
  color: #f5f5f5;
}

/* 暗色模式下接收消息气泡尖角 */
.dark-mode .message-text.received::before {
  border-right-color: #3d3d3d;
}

.dark-mode .message-input-container {
  background-color: rgba(26, 26, 26, 0.9);
  border-top-color: #444;
}

.dark-mode .message-input {
  background-color: #3d3d3d;
  border-color: #444;
  color: #f5f5f5;
}

.dark-mode .messages-container::-webkit-scrollbar-track {
  background: #1a1a1a;
}

.dark-mode .messages-container::-webkit-scrollbar-thumb {
  background: #555;
}

.dark-mode .messages-container::-webkit-scrollbar-thumb:hover {
  background: #777;
}
</style>
