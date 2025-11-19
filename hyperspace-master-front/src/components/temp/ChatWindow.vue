<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import ChatHeader from './ChatHeader.vue'
import MessageList from '../MessageList.vue'
import MessageInput from '../MessageInput.vue'

interface Message {
  id: number
  text: string
  sender: 'me' | 'other'
  time: string
  avatar: string
}

const messages = ref<Message[]>([
  { id: 1, text: '你好！最近怎么样？', sender: 'other', time: '10:30', avatar: '' },
  { id: 2, text: '还不错，正在开发新功能呢', sender: 'me', time: '10:32', avatar: '' },
  { id: 3, text: '听起来不错，是什么功能？', sender: 'other', time: '10:33', avatar: '' },
])

const currentChat = ref({
  name: '张三',
  status: '在线',
  avatar: ''
})

const sendMessage = (text: string) => {
  if (text.trim()) {
    const newMessage: Message = {
      id: messages.value.length + 1,
      text,
      sender: 'me',
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      avatar: ''
    }
    messages.value.push(newMessage)
  }
}

// 从localStorage加载背景设置并传递给MessageList
const backgroundSettings = ref({
  backgroundImage: '',
  backgroundOpacity: 100
})

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
}

// 处理背景透明度变化事件
const handleBackgroundOpacityChanged = (e: CustomEvent) => {
  const detail = e.detail as { opacity: number };
  backgroundSettings.value.backgroundOpacity = detail.opacity;
};

// 处理存储变化事件
const handleStorageChanged = (e: StorageEvent) => {
  if (e.key === 'userBackgroundImage' || e.key === 'userBackgroundOpacity') {
    updateBackgroundSettings()
  }
};

onMounted(() => {
  updateBackgroundSettings()
  
  // 监听自定义背景透明度变化事件
  window.addEventListener('backgroundOpacityChanged', handleBackgroundOpacityChanged as EventListener);
  
  // 监听localStorage变化
  window.addEventListener('storage', handleStorageChanged);
})

onUnmounted(() => {
  // 清理事件监听器
  window.removeEventListener('backgroundOpacityChanged', handleBackgroundOpacityChanged as EventListener);
  window.removeEventListener('storage', handleStorageChanged);
})
</script>

<template>
  <div class="chat-window">
    <ChatHeader :chat="currentChat" />
    <MessageList :messages="messages" :background-settings="backgroundSettings" />
    <MessageInput @send="sendMessage" />
  </div>
</template>

<style scoped>
.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #ffffff;
  position: relative;
  color: #333; /* 默认文字颜色 */
}

/* 暗色模式样式 */
.dark-mode .chat-window {
  background-color: #1a1a1a;
  color: #f5f5f5;
}

.dark-mode .chat-window::before {
  background-image: linear-gradient(rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.05));
}
</style>