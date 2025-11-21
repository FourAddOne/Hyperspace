<template>
  <div class="group-view">
    <div class="group-container">
      <div class="group-sidebar">
        <div class="group-sidebar-header">
          <h2>群聊</h2>
          <el-button type="primary" circle @click="createGroup">
            <plus />
          </el-button>
        </div>

        <div class="search-box">
          <el-input placeholder="搜索群组" />
        </div>

        <div class="groups-list">
          <div
            v-for="group in groups"
            :key="group.id"
            class="group-item"
            :class="{ active: selectedGroup?.id === group.id }"
            @click="selectGroup(group)"
          >
            <el-avatar :icon="ChatLineSquare" />
            <div class="group-info">
              <div class="group-name">{{ group.name }}</div>
              <div class="group-description">{{ group.description || '暂无描述' }}</div>
            </div>
            <div class="group-members">
              {{ group.memberCount }}人
            </div>
          </div>
        </div>
      </div>

      <div class="group-main">
        <div class="group-header" v-if="selectedGroup">
          <div class="group-header-info">
            <el-avatar :icon="ChatLineSquare" :size="40" />
            <div class="group-header-text">
              <div class="group-name-header">{{ selectedGroup.name }}</div>
              <div class="group-member-count">{{ selectedGroup.memberCount }}名成员</div>
            </div>
          </div>
          <div class="group-header-actions">
            <el-button @click="enterGroupChat(selectedGroup.id)">进入聊天</el-button>
          </div>
        </div>

        <div class="group-content" v-if="selectedGroup">
          <div class="group-details">
            <h3>群组信息</h3>
            <div class="group-detail-item">
              <span class="label">群组名称:</span>
              <span>{{ selectedGroup.name }}</span>
            </div>
            <div class="group-detail-item">
              <span class="label">群组描述:</span>
              <span>{{ selectedGroup.description || '暂无描述' }}</span>
            </div>
            <div class="group-detail-item">
              <span class="label">成员数量:</span>
              <span>{{ selectedGroup.memberCount }}人</span>
            </div>
          </div>
        </div>

        <div class="group-placeholder" v-else>
          <div class="placeholder-content">
            <chat-line-square class="placeholder-icon" />
            <p>选择一个群组或创建新群组</p>
            <el-button type="primary" @click="createGroup">创建群组</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatLineSquare, Plus } from '@element-plus/icons-vue'
import apiClient, { API_ENDPOINTS } from '../services/api'
import { useUserStore } from '../stores/userStore'

interface Group {
  id: string
  name: string
  description: string
  memberCount: number
}

const router = useRouter()
const userStore = useUserStore()

// 群组数据
const groups = ref<Group[]>([])
const loading = ref(false)
const selectedGroup = ref<Group | null>(null)

// 获取群组列表
const fetchGroups = async () => {
  loading.value = true
  try {
    // 暂时使用模拟数据替代API调用
    // const response = await apiClient.get(API_ENDPOINTS.GROUPS)
    // groups.value = response.data

    // 模拟数据
    groups.value = [
      {
        id: '1',
        name: '群群群',
        description: 'aaa',
        memberCount: 25
      },
      {
        id: '2',
        name: '啥群',
        description: '？',
        memberCount: 18
      },
      {
        id: '3',
        name: 'M78星云聊天室',
        description: '11持',
        memberCount: 12
      }
    ]
  } catch (error) {
    ElMessage.error('获取群组列表失败')
    console.error('获取群组列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 选择群组
const selectGroup = (group: Group) => {
  selectedGroup.value = group
}

// 创建新群组
const createGroup = () => {
  router.push('/group/create')
}

// 进入群组聊天
const enterGroupChat = (groupId: string) => {
  router.push(`/group/${groupId}/chat`)
}

onMounted(() => {
  fetchGroups()
})
</script>

<style scoped>
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
