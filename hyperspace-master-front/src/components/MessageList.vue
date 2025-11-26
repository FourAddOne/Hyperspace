<script setup lang="ts">
import { computed } from 'vue'
import type { CSSProperties } from 'vue'

// 定义消息类型
interface Message {
  id: string
  content: string
  sender: string
  time: string
  isOwn: boolean
  avatar?: string
}

const props = defineProps({
  messages: {
    type: Array as () => Message[],
    required: true
  },
  backgroundSettings: {
    type: Object,
    default: () => ({
      backgroundImage: '',
      backgroundOpacity: 100
    })
  }
})

// 计算背景样式
const backgroundStyle = computed<CSSProperties>(() => {
  if (props.backgroundSettings?.backgroundImage) {
    return {
      backgroundImage: `url(${props.backgroundSettings.backgroundImage})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat',
      position: 'absolute',
      top: '0px',
      left: '0px',
      right: '0px',
      bottom: '0px',
      zIndex: 0
    }
  }
  return {}
})

// 计算覆盖层样式
const overlayStyle = computed<CSSProperties>(() => {
  if (props.backgroundSettings?.backgroundImage) {
    // 计算透明度 (0-1之间)
    const opacity = 1 - (props.backgroundSettings.backgroundOpacity / 100);
    return {
      backgroundColor: 'white',
      opacity: opacity,
      position: 'absolute',
      top: '0px',
      left: '0px',
      right: '0px',
      bottom: '0px',
      zIndex: 1
    }
  }
  return {}
})
</script>

<template>
  <div class="message-list-container">
    <div 
      v-if="props.backgroundSettings?.backgroundImage"
      class="message-list-background"
      :style="backgroundStyle"
    ></div>
    <div 
      v-if="props.backgroundSettings?.backgroundImage"
      class="message-list-overlay"
      :style="overlayStyle"
    ></div>
    <div class="message-list" :style="{ position: 'relative', zIndex: 2 }">
      <div 
        v-for="message in messages" 
        :key="message.id"
        class="message"
        :class="{ 'own-message': message.isOwn }"
      >
        <div v-if="!message.isOwn" class="avatar">
          <img :src="message.avatar || '../assets/logo.svg'" alt="avatar" />
        </div>
        <div class="message-content">
          <div v-if="!message.isOwn" class="sender-name">{{ message.sender }}</div>
          <div class="message-bubble" :class="{ 'own-bubble': message.isOwn }">
            <div class="message-text">{{ message.content }}</div>
          </div>
          <div class="message-time">{{ message.time }}</div>
        </div>
        <div v-if="message.isOwn" class="avatar">
          <img :src="message.avatar || '../assets/logo.svg'" alt="avatar" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.message-list-container {
  flex: 1;
  position: relative;
  background-color: #ffffff;
}

.message-list {
  padding: 20px;
  overflow-y: auto;
  height: 100%;
}

.message-list-background {
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.message-list-overlay {
  background-color: white;
}

.message {
  display: flex;
  margin-bottom: 15px;
}

.own-message {
  flex-direction: row-reverse;
}

.avatar img {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #ddd;
  margin: 0 10px;
}

.message-content {
  max-width: 70%;
}

.sender-name {
  font-size: 12px;
  color: #666;
  margin-bottom: 3px;
}

.message-bubble {
  display: inline-block;
  padding: 8px 12px;
  border-radius: 16px;
  background-color: white;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  word-wrap: break-word;
  word-break: break-all;
  line-height: 1.4;
}

.message-text {
  color: #1a1a1a;
}

.own-bubble {
  background-color: #0084ff;
  color: white;
}

.message-time {
  font-size: 10px;
  color: #999;
  margin-top: 5px;
  text-align: left;
}

.own-message .message-time {
  text-align: right;
}

/* 暗色模式样式 */
.dark-mode .message-list-container {
  background-color: #1a1a1a;
}

.dark-mode .message-bubble {
  background-color: #2d2d2d;
  color: #f5f5f5;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.dark-mode .sender-name {
  color: #ccc;
}

.dark-mode .message-text {
  color: #f5f5f5;
}

.dark-mode .message-list-overlay {
  background-color: #1a1a1a;
}
</style>