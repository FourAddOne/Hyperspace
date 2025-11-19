<script setup lang="ts">
import { RouterView } from 'vue-router'
import Sidebar from './Sidebar.vue'
import { onMounted, onUnmounted } from 'vue'
import globalWebSocketManager from '../services/globalWebSocketManager'

// 处理用户状态变更
const handleUserStatusChange = (data: any) => {
  console.log('用户状态变更 (来自MainLayout):', data);
  // 这里可以处理全局的用户状态变更，比如更新用户存储等
  // 通过事件总线或全局状态管理将状态变更传播到其他组件
  window.dispatchEvent(new CustomEvent('userStatusChange', { detail: data }));
};

onMounted(() => {
  console.log('MainLayout挂载，设置WebSocket回调并增加连接引用');
  // 设置WebSocket回调并增加连接引用
  globalWebSocketManager.setUserStatusCallback(handleUserStatusChange);
  globalWebSocketManager.incrementConnection();
  console.log('WebSocket连接引用已增加');
})

onUnmounted(() => {
  console.log('MainLayout卸载，减少WebSocket连接引用计数');
  // 减少连接引用计数
  globalWebSocketManager.decrementConnection();
  console.log('WebSocket连接引用已减少');
})
</script>

<template>
  <div class="main-layout">
    <Sidebar class="sidebar" />
    <div class="content">
      <RouterView />
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