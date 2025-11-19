<template>
  <div class="user-info-demo">
    <h3>当前用户信息</h3>
    <div v-if="userInfo">
      <p>用户ID: {{ userInfo.userId }}</p>
      <p>用户名: {{ userInfo.userName }}</p>
      <p>邮箱: {{ userInfo.email }}</p>
      <p>IP地址: {{ userInfo.Ip }}</p>
      <img v-if="userInfo.avatarUrl" :src="userInfo.avatarUrl" alt="用户头像" class="avatar" />
    </div>
    <div v-else>
      <p>未找到用户信息</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getUserInfo } from '../../utils/user.ts'

// 用户信息
const userInfo = ref(null)

// 获取用户信息
const loadUserInfo = () => {
  userInfo.value = getUserInfo()
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.user-info-demo {
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
  margin: 20px 0;
}

.avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
}
</style>