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
          path: '/add-friend',
          name: 'add-friend',
          component: AddFriendView
        }
      ]
    }
  ]
})

export default router