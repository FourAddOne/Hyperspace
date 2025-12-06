<script setup lang="ts">
import { ref } from 'vue'
import { VideoPlay, Search, Bell, Setting, ArrowDown, VideoCamera, Picture } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 定义 emits 用于向父组件传递事件
const emit = defineEmits<{
  (e: 'search', value: string): void
}>()

// 搜索关键词
const searchKeyword = ref('')

// 控制发布下拉菜单显示
const publishDropdownVisible = ref(false)

// 触发搜索事件
const handleSearch = () => {
  emit('search', searchKeyword.value)
}

// 直接跳转到发布页面，默认为图文类型
const handlePublish = () => {
  router.push('/publish?type=image')
}

// 处理发布选项（保留原有功能，但不再使用）
const handlePublishOption = (type: string) => {
  publishDropdownVisible.value = false
  if (type === 'image') {
    // 跳转到图文发布页面
    router.push('/publish?type=image')
  } else if (type === 'video') {
    // 跳转到视频发布页面
    router.push('/publish?type=video')
  }
}
</script>

<template>
  <div class="discover-header">
    <!-- 顶部导航栏 -->
    <div class="header-top">
      <div class="logo">
        <div class="logo-container">
          <span class="logo-icon">🔍</span>
          <span class="logo-text">发现</span>
        </div>
      </div>
      
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索你感兴趣的内容..."
          class="search-input"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <search class="search-icon" />
          </template>
        </el-input>
      </div>
      
      <div class="header-actions">
        <!-- 发布按钮改为直接跳转 -->
        <el-button type="primary" class="publish-btn" @click="handlePublish">
          <video-play class="icon" />
          发布内容
        </el-button>
        
        <el-badge :value="3" class="notification-badge">
          <el-button circle class="icon-btn">
            <bell class="icon" />
          </el-button>
        </el-badge>
        
        <el-button circle class="icon-btn">
          <setting class="icon" />
        </el-button>
      </div>
    </div>
    
    <!-- 科技感装饰线条 -->
    <div class="tech-decoration">
      <div class="tech-line"></div>
      <div class="tech-node" v-for="i in 7" :key="i"></div>
    </div>
  </div>
</template>

<style scoped>
.discover-header {
  background: var(--card-background);
  border-radius: 16px;
  box-shadow: var(--box-shadow);
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.header-top {
  display: flex;
  align-items: center;
  padding: 16px 20px;
}

.logo {
  margin-right: 30px;
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-icon {
  font-size: 24px;
}

.logo-text {
  font-size: 24px;
  font-weight: 600;
  background: linear-gradient(90deg, #409eff, #64b5f6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  position: relative;
}

.logo-text::after {
  content: '';
  position: absolute;
  bottom: -5px;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, #409eff, #64b5f6);
  border-radius: 3px;
  transform: scaleX(0);
  transform-origin: center;
  animation: logoGlow 3s infinite;
}

.search-bar {
  flex: 1;
  max-width: 500px;
  margin-right: 30px;
}

.search-input {
  border-radius: 24px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.search-input:focus-within {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.3);
}

.search-icon {
  width: 1em;
  height: 1em;
  color: var(--text-secondary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.publish-btn {
  border-radius: 24px;
  padding: 10px 20px;
  font-weight: 500;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.publish-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s;
}

.publish-btn:hover::before {
  left: 100%;
}

.publish-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.3);
}

.icon-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(64, 158, 255, 0.1);
  border: none;
  color: var(--text-secondary);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.icon-btn:hover {
  background: rgba(64, 158, 255, 0.2);
  color: var(--primary-color);
  transform: translateY(-2px);
}

.icon-btn::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  background: rgba(64, 158, 255, 0.1);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  transition: width 0.3s ease, height 0.3s ease;
}

.icon-btn:active::after {
  width: 100px;
  height: 100px;
}

.icon {
  width: 1.2em;
  height: 1.2em;
}

.notification-badge {
  margin-right: 8px;
}

.tech-decoration {
  position: relative;
  height: 3px;
  background: linear-gradient(90deg, transparent, var(--border-color), transparent);
  margin: 0 20px;
}

.tech-line {
  position: absolute;
  top: 50%;
  left: 0;
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, 
    transparent, 
    rgba(64, 158, 255, 0.8), 
    rgba(64, 158, 255, 0.4),
    rgba(64, 158, 255, 0.8),
    transparent);
  animation: flow 3s infinite linear;
}

.tech-node {
  position: absolute;
  top: 50%;
  width: 8px;
  height: 8px;
  background: var(--primary-color);
  border-radius: 50%;
  transform: translateY(-50%);
  box-shadow: 0 0 8px var(--primary-color);
}

.tech-node:nth-child(1) { left: 5%; }
.tech-node:nth-child(2) { left: 20%; animation-delay: 0.5s; }
.tech-node:nth-child(3) { left: 40%; animation-delay: 1s; }
.tech-node:nth-child(4) { left: 60%; animation-delay: 1.5s; }
.tech-node:nth-child(5) { left: 80%; animation-delay: 2s; }
.tech-node:nth-child(6) { left: 90%; animation-delay: 2.5s; }
.tech-node:nth-child(7) { left: 95%; animation-delay: 3s; }

@keyframes flow {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

@keyframes pulse {
  0%, 100% { opacity: 0.6; transform: translateY(-50%) scale(1); }
  50% { opacity: 1; transform: translateY(-50%) scale(1.2); }
}

@keyframes logoGlow {
  0%, 100% { transform: scaleX(0); opacity: 0; }
  50% { transform: scaleX(1); opacity: 1; }
}

.tech-node {
  animation: pulse 2s infinite;
}

/* 暗色模式适配 */
.dark-mode .discover-header {
  background: #2d2d2d;
  border-color: #444;
}

.dark-mode .tech-decoration {
  background: linear-gradient(90deg, transparent, #444, transparent);
}

.dark-mode .icon-btn {
  background: rgba(64, 158, 255, 0.15);
  color: #aaa;
}

.dark-mode .icon-btn:hover {
  background: rgba(64, 158, 255, 0.3);
  color: #409eff;
}

/* 输入框暗色模式优化 */
.dark-mode :deep(.el-input__wrapper) {
  background-color: #2a2a2a !important;
  box-shadow: 0 0 0 1px #444 inset !important;
  border-radius: 24px;
}

.dark-mode :deep(.el-input__inner) {
  background-color: #2a2a2a !important;
  color: #f5f5f5 !important;
  border: none !important;
}

.dark-mode :deep(.el-input__inner::placeholder) {
  color: #888888 !important;
}

/* 亮色模式输入框优化 */
:deep(.el-input__wrapper) {
  background-color: #ffffff !important;
  box-shadow: 0 0 0 1px #e4e7ed inset !important;
  border-radius: 24px;
}

:deep(.el-input__inner) {
  background-color: #ffffff !important;
  color: #333333 !important;
  border: none !important;
}
</style>