<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps({
  conversations: {
    type: Array,
    required: true
  }
})

// 模拟选中会话
const selectedConversation = ref(1)
</script>

<template>
  <div class="conversation-list">
    <div 
      v-for="conversation in conversations" 
      :key="conversation.id"
      class="conversation-item"
      :class="{ active: conversation.id === selectedConversation }"
      @click="selectedConversation = conversation.id"
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
</style>