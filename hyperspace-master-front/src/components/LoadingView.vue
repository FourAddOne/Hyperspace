<template>
  <div v-if="loading" class="loading-overlay" :class="{ 'dark-mode': isDarkMode }">
    <div class="loading-content">
      <div class="loading-spinner">
        <div class="spinner-circle"></div>
        <div class="spinner-orbit spinner-orbit-1"></div>
        <div class="spinner-orbit spinner-orbit-2"></div>
        <div class="spinner-orbit spinner-orbit-3"></div>
      </div>
      <div class="loading-text">加载中...</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'

const props = defineProps<{
  loading: boolean
}>()

// 检查是否为暗色模式
const isDarkMode = computed(() => {
  return document.documentElement.classList.contains('dark-mode') || 
         document.body.classList.contains('dark-mode')
})
</script>

<style scoped>
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

/* 暗色模式下的背景 */
.loading-overlay.dark-mode {
  background: black;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #333;
}

.loading-overlay.dark-mode .loading-content {
  color: #f5f5f5;
}

.loading-spinner {
  position: relative;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.spinner-circle {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 50%;
  animation: pulse 1.5s ease-in-out infinite;
}

.loading-overlay.dark-mode .spinner-circle {
  background: linear-gradient(135deg, #2c3e50, #4a235a);
}

.spinner-orbit {
  position: absolute;
  border: 2px solid rgba(102, 126, 234, 0.5);
  border-radius: 50%;
  animation: rotate 2s linear infinite;
}

.loading-overlay.dark-mode .spinner-orbit {
  border-color: rgba(44, 62, 80, 0.5);
}

.spinner-orbit-1 {
  width: 60px;
  height: 60px;
  border-top-color: transparent;
  animation-duration: 2s;
}

.spinner-orbit-2 {
  width: 70px;
  height: 70px;
  border-right-color: transparent;
  animation-duration: 2.5s;
  animation-direction: reverse;
}

.spinner-orbit-3 {
  width: 80px;
  height: 80px;
  border-bottom-color: transparent;
  animation-duration: 3s;
}

@keyframes rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.7;
  }
}

.loading-text {
  letter-spacing: 2px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  animation: text-pulse 2s ease-in-out infinite;
}

.loading-overlay.dark-mode .loading-text {
  text-shadow: 0 2px 4px rgba(255, 255, 255, 0.1);
}

@keyframes text-pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.8;
  }
}
</style>