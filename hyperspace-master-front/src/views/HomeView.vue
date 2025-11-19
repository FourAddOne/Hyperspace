<script setup lang="ts">
import { useRouter } from 'vue-router'
import { onMounted } from 'vue'

const router = useRouter()

onMounted(() => {
  // 检查是否已登录（是否有token）
  const token = localStorage.getItem('token')
  if (token) {
    // 如果已登录，直接跳转到聊天页面
    router.push('/chat')
  } else {
    // 如果未登录，跳转到登录页面
    router.push('/login')
  }
})
</script>

<template>
  <div class="home">
    <p>正在跳转...</p>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  font-size: 18px;
  color: #666;
}
</style>