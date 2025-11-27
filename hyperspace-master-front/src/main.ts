import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import { useUserStore } from './stores/userStore'
import { getAccessToken } from './utils/auth'

// 引入Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 引入WebSocket
import webSocketService from './services/websocket'

import App from './App.vue'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus) // 注册Element Plus

// 注册所有Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 全局前置守卫，检查路由访问权限
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  // 初始化用户状态
  userStore.initUser()
  
  // 检查是否需要认证
  const requiresAuth = !['login', 'register'].includes(to.name as string)
  
  // 检查用户是否已登录
  const isLoggedIn = !!getAccessToken()
  
  // 如果目标是登录或注册页面，确保添加背景类
  if (to.name === 'login' || to.name === 'register') {
    document.body.classList.add('login-page')
  } else {
    document.body.classList.remove('login-page')
  }
  
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