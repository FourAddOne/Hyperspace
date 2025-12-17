<script setup lang="ts">
import { RouterView } from 'vue-router'
import { onMounted } from 'vue'
import { useUserStore } from './stores/userStore'
import { getToken } from './utils/auth'

// 应用启动时检查用户登录状态
onMounted(() => {
  const userStore = useUserStore()
  const token = getToken()
  
  // 如果没有token，确保用户状态被清空
  if (!token) {
    userStore.clearUserInfo()
  }
})
</script>

<template>
  <RouterView />
</template>

<style scoped>
</style>
