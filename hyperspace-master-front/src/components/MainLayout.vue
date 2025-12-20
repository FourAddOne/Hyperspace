<script setup lang="ts">
import { RouterView } from 'vue-router'
import Sidebar from './Sidebar.vue'
import { onMounted, onUnmounted, onActivated, onDeactivated } from 'vue'
import globalWebSocketManager from '../services/globalWebSocketManager'

// 简单的日志级别控制
const isDevelopment = process.env.NODE_ENV === 'development';

const log = {
  debug: (...args: any[]) => {
    console.debug('[MainLayout][DEBUG]', ...args);
  },
  info: (...args: any[]) => {
    console.info('[MainLayout][INFO]', ...args);
  },
  warn: (...args: any[]) => {
    console.warn('[MainLayout][WARN]', ...args);
  },
  error: (...args: any[]) => {
    console.error('[MainLayout][ERROR]', ...args);
  }
};

// 处理用户状态变更
const handleUserStatusChange = (data: any) => {
  log.debug('用户状态变更 (来自MainLayout):', data);
  // 通过全局事件将状态变更传播到其他组件
  window.dispatchEvent(new CustomEvent('userStatusChange', { detail: data }));
};

// 处理实时消息
const handleRealTimeMessage = (data: any) => {
  log.debug('收到实时消息 (来自MainLayout):', data);
  console.log('[MainLayout] 收到实时消息 (来自MainLayout):', data);
  // 通过全局事件将消息传播到其他组件
  window.dispatchEvent(new CustomEvent('realTimeMessage', { detail: data }));
};

onMounted(() => {
  // 设置WebSocket回调并增加连接引用
  globalWebSocketManager.setUserStatusCallback(handleUserStatusChange);
  globalWebSocketManager.setMessageCallback(handleRealTimeMessage);
  globalWebSocketManager.incrementConnection();
  console.log('WebSocket连接初始化完成，当前连接数:', globalWebSocketManager.getConnectionCount());
})

onUnmounted(() => {
  console.log('MainLayout卸载，清理WebSocket连接');
  // 移除事件监听器
  window.removeEventListener('userStatusChange', handleUserStatusChange);
  window.removeEventListener('realTimeMessage', handleRealTimeMessage);
  
  // 减少连接引用计数
  globalWebSocketManager.decrementConnection();
})

// 添加activated钩子确保组件激活时WebSocket回调正确设置
onActivated(() => {
  console.log('MainLayout激活，确保WebSocket连接，当前连接数:', globalWebSocketManager.getConnectionCount());
  globalWebSocketManager.setUserStatusCallback(handleUserStatusChange);
  globalWebSocketManager.setMessageCallback(handleRealTimeMessage);
  globalWebSocketManager.incrementConnection();
  
  // 确保用户状态是最新的
  if (globalWebSocketManager.isConnected()) {
    globalWebSocketManager.sendUserStatus(true);
  }
})

// 添加deactivated钩子确保组件失活时减少连接引用
onDeactivated(() => {
  globalWebSocketManager.decrementConnection();
})
</script>

<template>
  <div class="main-layout">
    <Sidebar class="sidebar" />
    <div class="content">
      <RouterView v-slot="{ Component }">
        <keep-alive>
          <component :is="Component" />
        </keep-alive>
      </RouterView>
    </div>
  </div>
</template>

<style scoped>
.main-layout {
  display: flex;
  height: 100vh;
  width: 100%;
}

.sidebar {
  flex-shrink: 0;
}

.content {
  flex: 1;
  overflow: auto;
}

/* 暗色模式样式 */
.dark-mode .main-layout {
  background-color: #1a1a1a;
}

.dark-mode .content {
  background-color: #1a1a1a;
}
</style>