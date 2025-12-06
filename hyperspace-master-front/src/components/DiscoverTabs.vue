<script setup lang="ts">
import { ref, watch } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

// 定义 props
const props = defineProps<{
  modelValue: string
  tabs: Array<{ id: string; name: string }>
}>()

// 定义 emits
const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

// 当前激活的标签
const activeTab = ref(props.modelValue)

// 监听props变化
watch(() => props.modelValue, (newValue) => {
  activeTab.value = newValue
})

// 监听activeTab变化
watch(activeTab, (newValue) => {
  emit('update:modelValue', newValue)
})

// 切换标签
const switchTab = (tabId: string) => {
  activeTab.value = tabId
}

// 标签滚动相关
const tabContainerRef = ref<HTMLElement | null>(null)

const scrollTabs = (direction: 'left' | 'right') => {
  if (tabContainerRef.value) {
    const scrollAmount = 200
    tabContainerRef.value.scrollBy({
      left: direction === 'left' ? -scrollAmount : scrollAmount,
      behavior: 'smooth'
    })
  }
}
</script>

<template>
  <div class="discover-tabs">
    <div class="tabs-container">
      <el-button 
        class="scroll-btn left" 
        circle 
        @click="scrollTabs('left')"
      >
        <arrow-left class="icon" />
      </el-button>
      
      <div ref="tabContainerRef" class="tabs-wrapper">
        <div 
          v-for="tab in tabs" 
          :key="tab.id"
          class="tab-item"
          :class="{ active: activeTab === tab.id }"
          @click="switchTab(tab.id)"
        >
          <span class="tab-text">{{ tab.name }}</span>
          <div class="tab-indicator"></div>
          <div class="tab-glow"></div>
          <div class="tab-shine"></div>
        </div>
      </div>
      
      <el-button 
        class="scroll-btn right" 
        circle 
        @click="scrollTabs('right')"
      >
        <arrow-right class="icon" />
      </el-button>
    </div>
    
    <!-- 科技感装饰线条 -->
    <div class="tech-decoration">
      <div class="tech-line"></div>
      <div class="tech-node" v-for="i in 5" :key="i"></div>
    </div>
  </div>
</template>

<style scoped>
.discover-tabs {
  background: var(--card-background);
  border-radius: 16px;
  box-shadow: var(--box-shadow);
  padding: 0 20px;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.tabs-container {
  display: flex;
  align-items: center;
  position: relative;
  height: 60px;
}

.scroll-btn {
  width: 36px;
  height: 36px;
  background: rgba(64, 158, 255, 0.1);
  border: none;
  color: var(--text-secondary);
  z-index: 2;
  transition: all 0.3s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.scroll-btn:hover {
  background: rgba(64, 158, 255, 0.2);
  color: var(--primary-color);
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.scroll-btn.left {
  margin-right: 10px;
}

.scroll-btn.right {
  margin-left: 10px;
}

.tabs-wrapper {
  display: flex;
  flex: 1;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.tabs-wrapper::-webkit-scrollbar {
  display: none;
}

.tab-item {
  position: relative;
  padding: 0 24px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 16px;
  white-space: nowrap;
  transition: all 0.3s ease;
  overflow: hidden;
  border-radius: 8px;
  margin: 0 4px;
}

.tab-item:first-child {
  margin-left: 0;
}

.tab-item:last-child {
  margin-right: 0;
}

.tab-item:hover {
  color: var(--text-primary);
  background: rgba(64, 158, 255, 0.05);
}

.tab-item.active {
  color: var(--primary-color);
  font-weight: 500;
  background: rgba(64, 158, 255, 0.1);
}

.tab-text {
  position: relative;
  z-index: 2;
}

.tab-indicator {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: var(--primary-color);
  transform: scaleX(0);
  transform-origin: center;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 3px 3px 0 0;
  z-index: 2;
}

.tab-item.active .tab-indicator {
  transform: scaleX(1);
}

.tab-glow {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: radial-gradient(
    circle at center,
    rgba(64, 158, 255, 0.2) 0%,
    transparent 70%
  );
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 1;
}

.tab-item:hover .tab-glow {
  opacity: 1;
}

.tab-item.active .tab-glow {
  opacity: 1;
  background: radial-gradient(
    circle at center,
    rgba(64, 158, 255, 0.3) 0%,
    transparent 70%
  );
}

.tab-shine {
  position: absolute;
  top: -50%;
  left: -100%;
  width: 50%;
  height: 200%;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.2),
    transparent
  );
  transform: skewX(-25deg);
  transition: left 0.5s;
}

.tab-item:hover .tab-shine {
  left: 150%;
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

.tech-node:nth-child(1) { left: 10%; }
.tech-node:nth-child(2) { left: 30%; animation-delay: 0.5s; }
.tech-node:nth-child(3) { left: 50%; animation-delay: 1s; }
.tech-node:nth-child(4) { left: 70%; animation-delay: 1.5s; }
.tech-node:nth-child(5) { left: 90%; animation-delay: 2s; }

@keyframes flow {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

@keyframes pulse {
  0%, 100% { opacity: 0.6; transform: translateY(-50%) scale(1); }
  50% { opacity: 1; transform: translateY(-50%) scale(1.2); }
}

.tech-node {
  animation: pulse 2s infinite;
}

/* 暗色模式适配 */
.dark-mode .discover-tabs {
  background: #2d2d2d;
  border-color: #444;
}

.dark-mode .scroll-btn {
  background: rgba(64, 158, 255, 0.15);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}

.dark-mode .scroll-btn:hover {
  background: rgba(64, 158, 255, 0.3);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
}

.dark-mode .tab-item:hover {
  background: rgba(64, 158, 255, 0.1);
}

.dark-mode .tab-item.active {
  background: rgba(64, 158, 255, 0.15);
}

.dark-mode .tech-decoration {
  background: linear-gradient(90deg, transparent, #444, transparent);
}
</style>