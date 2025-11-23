import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import { useUserStore } from './stores/userStore'
import { getToken } from './utils/auth'

// 引入Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 引入WebSocket
import webSocketService from './services/websocket'

import App from './App.vue'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus) // 注册Element Plus

// 全局前置守卫，检查路由访问权限
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  // 初始化用户状态
  userStore.initUser()
  
  // 检查是否需要认证
  const requiresAuth = !['login', 'register'].includes(to.name as string)
  
  // 检查用户是否已登录
  const isLoggedIn = !!getToken()
  
  if (requiresAuth && !isLoggedIn) {
    // 需要认证但未登录，跳转到登录页
    next({ name: 'login' })
  } else if (!requiresAuth && isLoggedIn && to.name === 'login') {
    // 已登录但访问登录页，跳转到主页
    next({ name: 'chat' })
  } else {
    // 其他情况允许访问
    next()
  }
})

app.mount('#app')