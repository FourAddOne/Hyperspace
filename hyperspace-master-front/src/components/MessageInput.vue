<script setup lang="ts">
import { ref } from 'vue'

const message = ref('')

const emit = defineEmits(['send'])

const sendMessage = () => {
  if (message.value.trim()) {
    // 发送消息事件给父组件处理
    emit('send', message.value)
    message.value = ''
  }
}
</script>

<template>
  <div class="message-input">
    <div class="input-tools">
      <span class="tool-icon">😊</span>
      <span class="tool-icon">📎</span>
    </div>
    <div class="input-container">
      <textarea 
        v-model="message" 
        placeholder="输入消息..." 
        class="message-textarea"
        @keydown.enter.exact.prevent="sendMessage"
      ></textarea>
    </div>
    <div class="input-actions">
      <button 
        class="send-button" 
        :disabled="!message.trim()"
        @click="sendMessage"
      >
        发送
      </button>
    </div>
  </div>
</template>

<style scoped>
.message-input {
  height: 120px;
  padding: 10px;
  border-top: 1px solid #ddd;
  background-color: #ffffff;
  display: flex;
}

.input-tools {
  display: flex;
  align-items: flex-end;
  padding: 0 10px;
  gap: 10px;
}

.tool-icon {
  font-size: 20px;
  cursor: pointer;
  color: #333;
}

.input-container {
  flex: 1;
  padding: 0 10px;
}

.message-textarea {
  width: 100%;
  height: 100%;
  resize: none;
  border: 1px solid #ddd;
  border-radius: 5px;
  padding: 10px;
  font-family: inherit;
  font-size: 14px;
  outline: none;
  background-color: #ffffff;
  color: #1a1a1a;
}

.message-textarea:focus {
  border-color: #0084ff;
}

.message-textarea::placeholder {
  color: #999;
}

.input-actions {
  display: flex;
  align-items: flex-end;
  padding: 0 10px;
}

.send-button {
  background-color: #0084ff;
  color: white;
  border: none;
  border-radius: 5px;
  padding: 8px 15px;
  cursor: pointer;
  font-size: 14px;
}

.send-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

/* 暗色模式样式 */
.dark-mode .message-input {
  border-top: 1px solid #444;
  background-color: #2d2d2d;
}

.dark-mode .tool-icon {
  color: #ccc;
}

.dark-mode .message-textarea {
  background-color: #3d3d3d;
  border: 1px solid #555;
  color: #f5f5f5;
}

.dark-mode .message-textarea:focus {
  border-color: #409eff;
}

.dark-mode .send-button {
  background-color: #409eff;
}
</style>