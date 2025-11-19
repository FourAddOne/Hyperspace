<template>
  <div class="user-profile">
    <h2>用户信息</h2>
    <div v-if="userInfo">
      <div class="profile-item">
        <label>用户ID:</label>
        <span>{{ userInfo.userId }}</span>
      </div>
      <div class="profile-item">
        <label>用户名:</label>
        <span>{{ userInfo.userName }}</span>
      </div>
      <div class="profile-item">
        <label>邮箱:</label>
        <span>{{ userInfo.email }}</span>
      </div>
      <div class="profile-item">
        <label>登录IP:</label>
        <span>{{ userInfo.Ip }}</span>
      </div>
      <div class="profile-item">
        <label>头像:</label>
        <img v-if="userInfo.avatarUrl" :src="userInfo.avatarUrl" alt="用户头像" class="avatar" />
      </div>
    </div>
    <div v-else>
      <p>未找到用户信息，请先登录</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
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
.user-profile {
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
  margin: 20px 0;
  background-color: #f9f9f9;
}

.profile-item {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
}

.profile-item label {
  font-weight: bold;
  margin-right: 10px;
  min-width: 80px;
}

.avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
  margin-left: 10px;
}
</style>