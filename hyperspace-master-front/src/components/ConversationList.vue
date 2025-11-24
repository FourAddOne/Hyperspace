<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps({
  conversations: {
    type: Array,
    required: true
  }
})
</script>

<template>
  <div class="conversation-list">
    <div 
      v-for="conversation in conversations" 
      :key="conversation.id"
      class="conversation-item"
    >
      <div class="avatar">
        <img src="../assets/logo.svg" alt="avatar" />
        <div v-if="conversation.unread > 0" class="unread-badge">
          {{ conversation.unread > 99 ? '99+' : conversation.unread }}
        </div>
      </div>
      <div class="conversation-info">
        <div class="conversation-header">
          <div class="name">{{ conversation.name }}</div>
          <div class="time">{{ conversation.time }}</div>
        </div>
        <div class="last-message">{{ conversation.lastMessage }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.conversation-list {
  padding: 5px;
  background-color: #ffffff;
}

.conversation-item {
  display: flex;
  padding: 10px;
  cursor: pointer;
  border-radius: 5px;
  margin: 5px;
}

.conversation-item:hover {
  background-color: #f5f5f5;
}

.conversation-item.active {
  background-color: #e6f7ff;
}

.avatar {
  position: relative;
  margin-right: 10px;
}

.avatar img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #ddd;
}

.unread-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background-color: #f00;
  color: white;
  font-size: 10px;
  border-radius: 10px;
  padding: 2px 5px;
  min-width: 18px;
  text-align: center;
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}

.name {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
}

.time {
  font-size: 12px;
  color: #666;
}

.last-message {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 暗色模式样式 */
.dark-mode .conversation-list {
  background-color: #1a1a1a;
}

.dark-mode .conversation-item {
  background-color: #2d2d2d;
}

.dark-mode .conversation-item:hover {
  background-color: #333; /* 修改悬停背景色，使其在暗色模式下更合适 */
}

.dark-mode .conversation-item.active {
  background-color: #2d5577;
}

.dark-mode .name {
  color: #f5f5f5; /* 修改用户名颜色，使其在暗色模式下更清晰 */
}

.dark-mode .time,
.dark-mode .last-message {
  color: #ccc; /* 修改时间和其他文本颜色，使其在暗色模式下更清晰 */
}

.dark-mode .avatar img {
  background-color: #555;
}
</style>