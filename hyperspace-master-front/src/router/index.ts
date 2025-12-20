import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import MainLayout from '../components/MainLayout.vue'
import ChatView from '../views/ChatView.vue'
import UserProfileView from '../views/UserProfileView.vue'
import FriendListView from '../views/FriendListView.vue'
import AddFriendView from '../views/AddFriendView.vue'
import GroupListView from "@/views/GroupListView.vue";
import GamesListView from "@/views/GamesListView.vue";
import DiscoverView from "@/views/DiscoverView.vue";
import PublishView from "@/views/PublishView.vue";
import PostDetailView from "@/views/PostDetailView.vue";
import AIChatView from "@/views/AIChatView.vue"; // 导入AI聊天视图

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView
    },
    {
      path: '/',
      component: MainLayout,
      children: [
        {
          path: '',
          name: 'chat',
          component: ChatView
        },
        {
          path: '/chat',
          name: 'chat-main',
          component: ChatView
        },
        {
          path: '/profile',
          name: 'profile',
          component: UserProfileView
        },
        {
          path: '/friends',
          name: 'friends',
          component: FriendListView
        },
        {
          path: '/groups',
          name: 'groups',
          component: GroupListView
        },
        {
          path: '/games',
          name: 'games',
          component: GamesListView
        },
        {
          path: '/discover',
          name: 'discover',
          component: DiscoverView
        },
        {
          path: '/publish',
          name: 'publish',
          component: PublishView
        },
        {
          path: '/post/:postId',
          name: 'post-detail',
          component: PostDetailView,
          props: true
        },
        {
          path: '/add-friend',
          name: 'add-friend',
          component: AddFriendView
        },
        {
          path: '/ai-chat',
          name: 'ai-chat',
          component: AIChatView
        }
      ]
    }
  ]
})

// 添加路由守卫确保登录和注册页面有背景
router.beforeEach((to, from, next) => {
  // 如果目标是登录或注册页面，确保添加背景类
  if (to.name === 'login' || to.name === 'register') {
    document.body.classList.add('login-page')
  } else {
    document.body.classList.remove('login-page')
  }
  next()
})

export default router