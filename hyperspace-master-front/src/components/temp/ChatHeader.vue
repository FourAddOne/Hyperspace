<script setup lang="ts">
// 模拟当前聊天对象信息
const chatTarget = {
  name: '张三',
  status: 'online',
  members: 2
}
</script>

<template>
  <div class="chat-header">
    <div class="header-info">
      <div class="avatar">
        <img src="../assets/logo.svg" alt="avatar" />
      </div>
      <div class="chat-details">
        <div class="chat-name">{{ chatTarget.name }}</div>
        <div class="chat-status">
          <span class="status-indicator" :class="chatTarget.status"></span>
          <span v-if="chatTarget.members > 2" class="members">
            {{ chatTarget.members }}人
          </span>
          <span v-else class="online-status">
            {{ chatTarget.status === 'online' ? '在线' : '离线' }}
          </span>
        </div>
      </div>
    </div>
    <div class="header-actions">
      <span class="action-icon">🔍</span>
      <span class="action-icon">⋯</span>
    </div>
  </div>
</template>

<style scoped>
.chat-header {
  height: 60px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ddd;
  background-color: #ffffff;
}

.header-info {
  display: flex;
  align-items: center;
}

.avatar img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #ddd;
  margin-right: 10px;
}

.chat-name {
  font-size: 16px;
  font-weight: 500;
  color: #1a1a1a;
}

.chat-status {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #666;
}

.status-indicator {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 5px;
}

.status-indicator.online {
  background-color: #4caf50;
}

.status-indicator.offline {
  background-color: #9e9e9e;
}

.status-indicator.busy {
  background-color: #f44336;
}

.header-actions {
  display: flex;
  gap: 20px;
}

.action-icon {
  font-size: 18px;
  cursor: pointer;
  color: #333;
}

/* 暗色模式样式 */
.dark-mode .chat-header {
  border-bottom: 1px solid #444;
  background-color: #2d2d2d;
}

.dark-mode .chat-name {
  color: #f5f5f5;
}

.dark-mode .chat-status {
  color: #ccc;
}

.dark-mode .action-icon {
  color: #ccc;
}
</style>