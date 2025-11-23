<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/userStore'
import apiClient from '../services/api'
import { Client } from '@stomp/stompjs'

interface Message {
  id: string
  content: string
  senderId: string
  senderName: string
  timestamp: string
  type: 'text' | 'image' | 'system'
}

interface Group {
  id: string
  name: string
  memberCount: number
}

const stompClient = ref<Client | null>(null)
const route = useRoute()
const userStore = useUserStore()

// 状态管理
const groupId = route.params.groupId as string
const messages = ref<Message[]>([])
const newMessage = ref('')
const loading = ref(false)
const ws = ref<WebSocket | null>(null)

// 群组信息
const groupInfo = reactive<Group>({
  id: '',
  name: '',
  memberCount: 0
})

// 初始化STOMP连接
const initStompClient = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const wsUrl = `${protocol}://${window.location.host}/api/ws/group/${groupId}`

  stompClient.value = new Client({
    brokerURL: wsUrl,
    connectHeaders: {},
    debug: function (str) {
      console.log(str)
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  })

  stompClient.value.onConnect = (frame) => {
    console.log('Connected: ' + frame)

    // 订阅群组消息
    stompClient.value?.subscribe(`/topic/group/${groupId}`, (message) => {
      const receivedMessage: Message = JSON.parse(message.body)
      messages.value.push(receivedMessage)
      scrollToBottom()
    })
  }

  stompClient.value.onStompError = (frame) => {
    console.error('Broker reported error: ' + frame.headers['message'])
    console.error('Additional details: ' + frame.body)
  }

  stompClient.value.activate()
}






// 2. 发送群消息
// 发送群消息 - 修复后的版本
function sendGroupMsg(groupId: string, content: string) {
  if (stompClient.value && stompClient.value.connected) {
    const msg = {
      groupId: groupId,
      type: "text",
      textContent: content,
      senderId: userStore.userId,
      senderName: userStore.username
    }
    stompClient.value.publish({
      destination: "/app/group/send",
      body: JSON.stringify(msg)
    })
  } else {
    ElMessage.error('连接未建立')
  }
}

// 加载历史消息
const loadHistoryMessages = async () => {
  loading.value = true
  try {
    const response = await apiClient.get(`/api/groups/${groupId}/messages`)
    if (response.data.code === 200) {
      messages.value = response.data.data
      nextTick(() => {
        scrollToBottom()
      })
    }
  } catch (error) {
    console.error('加载消息失败:', error)
    ElMessage.error('加载消息失败')
  } finally {
    loading.value = false
  }
}

// 获取群组信息
const fetchGroupInfo = async () => {
  try {
    const response = await apiClient.get(`/api/groups/${groupId}`)
    if (response.data.code === 200) {
      Object.assign(groupInfo, response.data.data)
    }
  } catch (error) {
    console.error('获取群组信息失败:', error)
    ElMessage.error('获取群组信息失败')
  }
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    const container = document.querySelector('.chat-messages')
    if (container) {
      container.scrollTop = container.scrollHeight
    }
  })
}

// 格式化时间
const formatTime = (timestamp: string) => {
  return new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// 组件挂载
onMounted(() => {
  fetchGroupInfo()
  loadHistoryMessages()
  initStompClient()
})

// 组件卸载
onUnmounted(() => {
  if (ws.value) {
    ws.value.close()
  }
})

// 组件卸载时断开STOMP连接
onUnmounted(() => {
  if (stompClient.value) {
    stompClient.value.deactivate()
  }
})
</script>

<template>
  <div class="group-chat-view">
    <!-- 聊天头部 -->
    <div class="chat-header">
      <div class="header-info">
        <el-avatar shape="square" :size="40" />
        <div class="group-info">
          <div class="group-name">{{ groupInfo.name }}</div>
          <div class="group-meta">{{ groupInfo.memberCount }}名成员</div>
        </div>
      </div>
      <div class="header-actions">
        <el-button link>
          <el-icon><More /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 消息区域 -->
    <div class="chat-messages" v-loading="loading">
      <div
        v-for="message in messages"
        :key="message.id"
        class="message-item"
        :class="{ 'is-mine': message.senderId === userStore.userId }"
      >
        <div class="message-avatar">
          <el-avatar :size="32" />
        </div>
        <div class="message-content">
          <div class="message-sender">{{ message.senderName }}</div>
          <div class="message-bubble">
            <div class="message-text">{{ message.content }}</div>
          </div>
          <div class="message-time">{{ formatTime(message.timestamp) }}</div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-area">
      <div class="input-tools">
        <el-button link>
          <el-icon><Picture /></el-icon>
        </el-button>
        <el-button link>
          <el-icon><Folder /></el-icon>
        </el-button>
      </div>
      <el-input
        v-model="newMessage"
        type="textarea"
        :rows="3"
        placeholder="输入消息..."
        @keydown.enter.exact.prevent="sendMessage"
        @keydown.ctrl.enter.exact.prevent="newMessage += '\n'"
      />
      <div class="input-actions">
        <el-button type="primary" @click="sendMessage" :disabled="!newMessage.trim()">
          发送(Ctrl+Enter)
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.group-chat-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--el-bg-color-page);
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  border-bottom: 1px solid var(--el-border-color-light);
  background-color: var(--el-bg-color);
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.group-info {
  display: flex;
  flex-direction: column;
}

.group-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.group-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  gap: 12px;
  max-width: 80%;
}

.message-item.is-mine {
  align-self: flex-end;
}

.message-item.is-mine .message-content {
  align-items: flex-end;
}

.message-item.is-mine .message-bubble {
  background-color: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-5);
}

.message-sender {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}

.message-bubble {
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 8px 12px;
  word-break: break-word;
}

.message-text {
  font-size: 14px;
  color: var(--el-text-color-primary);
  line-height: 1.5;
}

.message-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.chat-input-area {
  padding: 16px;
  border-top: 1px solid var(--el-border-color-light);
  background-color: var(--el-bg-color);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-tools {
  display: flex;
  gap: 8px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
