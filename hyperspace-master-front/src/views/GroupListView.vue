<template>
  <div class="group-view">
    <div class="group-container">
      <div class="group-sidebar">
        <div class="group-sidebar-header">
          <h2>群聊</h2>
          <el-button type="primary" circle @click="createGroup">
            +
            <plus />
          </el-button>
        </div>

        <div class="search-box">
          <el-input
              v-model="searchKeyword"
              placeholder="搜索群组"
              @keyup.enter="searchGroups"
          />
        </div>

        <div class="groups-list">
          <div
              v-for="group in filteredGroups"
              :key="group.groupId"
              class="group-item"
              :class="{ active: selectedGroup?.groupId === group.groupId }"
              @click="selectGroup(group)"
          >
            <el-avatar :icon="ChatLineSquare" />
            <div class="group-info">
              <div class="group-name">{{ group.groupName }}</div>
              <div class="group-description">id : {{ group.groupId }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="group-main">
        <GroupChatView></GroupChatView>



      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatLineSquare, Plus } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/userStore'
import apiClient from '../services/api'
import GroupChatView from "@/views/GroupChatView.vue";
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

interface Group {
  groupId: string
  groupName: string
  userId: string | null
  role: string | null
  members: string[]
}

// 初始化 STOMP 客户端
const stompClient = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
  debug: (str) => {
    console.log(str)
  },
  onConnect: () => {
    console.log('WebSocket 连接成功')
    subscribeUserGroups()
  },
  onDisconnect: () => {
    console.log('WebSocket 连接断开')
  }
})

const router = useRouter()
const userStore = useUserStore()

// 群组数据
const groups = ref<Group[]>([])
const selectedGroup = ref<Group | null>(null)
const searchKeyword = ref('')
const loading = ref(false)

// 获取群组列表
const fetchGroups = async () => {
  console.log('获取群组列表...')
  loading.value = true
  try {
    const response = await apiClient.post('/api/groups/list', {
      userId: userStore.getUserEmail
    })
    if (200 === response.code) {
      groups.value = response.data
    }
  } catch (error) {
    console.error('获取群组列表失败: ', error)
    ElMessage.error('获取群组列表失败')
  } finally {
    loading.value = false
  }
}

// 过滤群组
const filteredGroups = computed(() => {
  if (!searchKeyword.value) return groups.value
  return groups.value.filter(group =>
    group.groupName.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

// 选择群组
// 替换现有的 selectGroup 方法
const selectGroup = (group: Group) => {
  // 直接进入聊天页面
  router.push(`/group/${group.groupId}/chat`)
}

// 创建群组
const createGroup = () => {
  ElMessage.info('创建群组功能开发中...')
}

// 搜索群组
const searchGroups = () => {
  // 搜索功能已通过计算属性实现
}

// 退出群组
const quitGroup = (groupId: string) => {
    ElMessage.confirm('确定要退出该群组吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    groups.value = groups.value.filter(g => g.groupId !== groupId)
    selectedGroup.value = null
    ElMessage.success('已退出群组')
  }).catch(() => {
    // 取消退出
  })
}

// 进入群组聊天
const enterGroupChat = (groupId: string) => {
  router.push(`/group/${groupId}/chat`)
}

// 格式化日期
const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString()
}

// 组件挂载时获取群组列表
onMounted(() => {
  fetchGroups()
  stompClient.activate()
})


// 1. 连接成功后，订阅当前用户所在的所有群（需从后端查询用户的群列表）
// 订阅用户群组
const subscribeUserGroups = () => {
  try {
    groups.value.forEach(group => {
      const groupId = group.groupId
      stompClient.subscribe(`/topic/group/${groupId}`, (message) => {
        const msg = JSON.parse(message.body)
        console.log(`收到群${groupId}的消息:`, msg)

        // 处理接收到的消息
      })
    })
  }catch ( error)
      {
        console.error('订阅用户群组失败:', error)
      }

}

</script>

<style scoped>
/* 保持原有的样式不变 */
.group-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--background-color);
  color: var(--text-color);
}

.group-container {
  display: flex;
  flex: 1;
  min-height: 0;
}

.group-sidebar {
  width: 300px;
  border-right: 1px solid #ddd;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
}

.dark-mode .group-sidebar {
  border-right: 1px solid #444;
  background-color: #1a1a1a;
}

.group-sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #ddd;
  background-color: #ffffff;
}

.dark-mode .group-sidebar-header {
  border-bottom: 1px solid #444;
  background-color: #2d2d2d;
}

.group-sidebar-header h2 {
  margin: 0;
  font-size: 18px;
  color: #1a1a1a;
}

.dark-mode .group-sidebar-header h2 {
  color: #f5f5f5;
}

.search-box {
  padding: 15px;
}

.groups-list {
  flex: 1;
  overflow-y: auto;
}

.group-item {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  cursor: pointer;
  position: relative;
  border-bottom: 1px solid #eee;
  background-color: #ffffff;
}

.dark-mode .group-item {
  border-bottom: 1px solid #333;
  background-color: #1a1a1a;
}

.group-item:hover {
  background-color: #f9f9f9;
}

.dark-mode .group-item:hover {
  background-color: #2a2a2a;
}

.group-item.active {
  background-color: #e0f0ff;
}

.dark-mode .group-item.active {
  background-color: #1a3a4a;
}

.group-item .el-avatar {
  margin-right: 15px;
  background-color: #409eff;
  color: white;
}

.group-info {
  flex: 1;
  min-width: 0;
}

.group-name {
  font-weight: 500;
  margin-bottom: 4px;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dark-mode .group-name {
  color: #f5f5f5;
}

.group-description {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dark-mode .group-description {
  color: #ccc;
}

.group-members {
  font-size: 12px;
  color: #666;
  margin-right: 10px;
}

.dark-mode .group-members {
  color: #ccc;
}

.group-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  position: relative;
}

.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 20px;
  border-bottom: 1px solid #ddd;
  background-color: #ffffff;
}

.dark-mode .group-header {
  border-bottom: 1px solid #444;
  background-color: #2d2d2d;
}

.group-header-info {
  display: flex;
  align-items: center;
}

.group-header-text {
  margin-left: 15px;
}

.group-name-header {
  font-weight: 500;
  font-size: 16px;
  color: #1a1a1a;
}

.dark-mode .group-name-header {
  color: #f5f5f5;
}

.group-member-count {
  font-size: 12px;
  color: #666;
}

.dark-mode .group-member-count {
  color: #aaa;
}

.group-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
}

.dark-mode .group-content {
  background-color: #1a1a1a;
}

.group-details {
  width: 100%;
  max-width: 600px;
  margin: 0 auto;
}

.group-detail-item {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.dark-mode .group-detail-item {
  border-bottom: 1px solid #333;
}

.group-detail-item .label {
  font-weight: 500;
  width: 100px;
  color: #1a1a1a;
}

.dark-mode .group-detail-item .label {
  color: #f5f5f5;
}

.group-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ffffff;
}

.dark-mode .group-placeholder {
  background-color: #1a1a1a;
}

.placeholder-content {
  text-align: center;
  color: #999;
}

.dark-mode .placeholder-content {
  color: #ccc;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #409eff;
}
</style>
