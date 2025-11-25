<template>
  <div class="group-view">
    <div class="group-container">
      <!-- 左侧群聊列表 -->
      <div class="group-sidebar">
        <div class="group-sidebar-header">
          <h2>群聊</h2>
          <el-button type="primary" circle @click="createGroup">
            +
          </el-button>
        </div>

        <div class="search-box">
          <el-input
              v-model="searchKeyword"
              placeholder="搜索群组"
              @keyup.enter="searchGroups"
          />
        </div>

        <div class="groups-list">
          <div
              v-for="group in mockGroups"
              :key="group.id"
              class="group-item"
              :class="{ active: selectedGroup?.id === group.id }"
              @click="selectGroup(group)"
          >
            <el-avatar :icon="ChatLineSquare" />
            <div class="group-info">
              <div class="group-name">{{ group.name }}</div>
              <div class="group-description">{{ group.description }}</div>
            </div>
            <div class="group-members">
              {{ group.memberCount }}人
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧聊天区域 -->
      <div class="group-main" v-if="selectedGroup">
        <div class="chat-header">
          <div class="chat-header-info">
            <el-avatar :icon="ChatLineSquare" :size="40" />
            <div class="chat-header-text">
              <div class="group-name-header">{{ selectedGroup.name }}</div>
              <div class="group-member-count">{{ selectedGroup.memberCount }}名成员</div>
            </div>
          </div>
        </div>

        <div class="chat-content">
          <div class="message-list">
            <div
                v-for="message in selectedGroup.messages"
                :key="message.id"
                class="message-item"
                :class="{ 'is-self': message.userId === userStore.userId }"
            >
              <el-avatar
                  :size="32"
                  :class="{ 'message-avatar': true }"
              />
              <div class="message-content">
                <div class="message-info">
                  <span class="username">{{ message.userName }}</span>
                  <span class="time">{{ message.time }}</span>
                </div>
                <div class="message-text">{{ message.content }}</div>
              </div>
            </div>
          </div>

          <div class="message-input">
            <el-input
                v-model="currentMessage"
                type="textarea"
                :rows="3"
                placeholder="输入消息..."
                @keyup.ctrl.enter="sendMessage"
            />
            <el-button type="primary" @click="sendMessage">发送</el-button>
          </div>
        </div>
      </div>

      <!-- 未选择群组时的占位 -->
      <div class="group-placeholder" v-else>
        <div class="placeholder-content">
          <chat-line-square class="placeholder-icon" />
          <p>选择一个群组开始聊天</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ChatLineSquare } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/userStore'

// 用户store
const userStore = useUserStore()

// 状态管理
const searchKeyword = ref('')
const messages = ref<any[]>([])
const currentMessage = ref('')
const selectedGroup = ref(null)

// 模拟本地数据
const mockGroups = ref([
  {
    id: '1',
    name: '群qqqq',
    description: 'aaaa',
    memberCount: 25,
    messages: [
      { id: 1, userId: '1', userName: '张三', content: '大家好！', time: '10:00' },
      { id: 2, userId: '2', userName: '李四', content: '你好！', time: '10:01' }
    ]
  },
  {
    id: '2',
    name: 'Vue',
    description: 'Vue.js',
    memberCount: 30,
    messages: [
      { id: 1, userId: '3', userName: '王五', content: 'Vue3好用吗？', time: '11:00' }
    ]
  }
])

// 方法
const createGroup = () => {
  // 创建群组逻辑
}

const searchGroups = () => {
  // 搜索群组逻辑
}

const selectGroup = (group) => {
  selectedGroup.value = group
}

const sendMessage = () => {
  if (!currentMessage.value.trim() || !selectedGroup.value) return

  const newMessage = {
    id: Date.now(),
    userId: userStore.userId,
    userName: userStore.userName,
    content: currentMessage.value,
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }

  selectedGroup.value.messages.push(newMessage)
  currentMessage.value = ''
}
</script>

<style>
.group-view {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

.group-container {
  display: flex;
  height: 100%;
  background-color: #ffffff;
}

.dark-mode .group-container {
  background-color: #1a1a1a;
}

.group-sidebar {
  width: 300px;
  border-right: 1px solid #ddd;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
}

.dark-mode .group-sidebar {
  border-right: 1px solid #444;
  background-color: #2d2d2d;
}

.group-sidebar-header {
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee;
}

.dark-mode .group-sidebar-header {
  border-bottom: 1px solid #444;
}

.search-box {
  padding: 10px;
  border-bottom: 1px solid #eee;
}

.dark-mode .search-box {
  border-bottom: 1px solid #444;
}

.groups-list {
  flex: 1;
  overflow-y: auto;
}

.group-item {
  padding: 15px;
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: background-color 0.3s;
}

.group-item:hover {
  background-color: #f5f5f5;
}

.group-item.active {
  background-color: #e6f7ff;
}

.dark-mode .group-item:hover {
  background-color: #2d2d2d;
}

.dark-mode .group-item.active {
  background-color: #1f1f1f;
}

.group-info {
  flex: 1;
  margin-left: 15px;
}

.group-name {
  font-weight: bold;
  margin-bottom: 5px;
}

.group-description {
  font-size: 12px;
  color: #666;
}

.group-members {
  font-size: 12px;
  color: #999;
}

.group-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
}

.dark-mode .group-main {
  background-color: #1a1a1a;
}

.chat-header {
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

.group-name-header {
  font-weight: bold;
  font-size: 16px;
}

.group-member-count {
  font-size: 12px;
  color: #666;
  margin-top: 5px;
}

.chat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: #ffffff;
}

.dark-mode .chat-content {
  background-color: #1a1a1a;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
}

.message-item.is-self {
  flex-direction: row-reverse;
}

.message-item.is-self .message-content {
  align-items: flex-end;
}

.message-item.is-self .message-info {
  flex-direction: row-reverse;
}

.message-avatar {
  margin: 0 10px;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 70%;
}

.message-info {
  display: flex;
  align-items: center;
  margin-bottom: 5px;
}

.message-info .username {
  font-size: 12px;
  color: #666;
  margin-right: 10px;
}

.message-info .time {
  font-size: 12px;
  color: #999;
}

.message-text {
  background-color: #f5f5f5;
  padding: 10px 15px;
  border-radius: 8px;
  word-break: break-word;
}

.dark-mode .message-text {
  background-color: #2d2d2d;
  color: #f5f5f5;
}

.message-input {
  padding: 20px;
  border-top: 1px solid #ddd;
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.dark-mode .message-input {
  border-top: 1px solid #444;
}

.message-input .el-textarea {
  flex: 1;
}

.group-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ffffff;
}

.dark-mode .group-placeholder {
  background-color: #1a1a1a;
}

.placeholder-content {
  text-align: center;
  color: #999;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 10px;
}
</style>