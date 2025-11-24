<script setup lang="ts">
import { useUserStore } from './stores/userStore'
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

// 初始化用户状态
onMounted(() => {
  userStore.initUser()
})

// 监听路由变化，确保登录页背景正确显示
router.beforeEach((to, from, next) => {
  // 如果目标是登录或注册页面，确保添加背景类
  if (to.name === 'login' || to.name === 'register') {
    document.body.classList.add('login-page')
  } else {
    document.body.classList.remove('login-page')
  }
  next()
})
</script>

<template>
  <div id="app" :class="{ 'has-background': useUserStore().getHasBackground }">
    <RouterView />
  </div>
</template>

<style scoped>
</style>