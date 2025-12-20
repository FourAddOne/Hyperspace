<template>
  <div class="group-view">
    <div class="group-container">
      <!-- 左侧群聊列表 -->
      <div class="group-sidebar">
        <div class="group-sidebar-header">
          <h2>群聊</h2>
          <el-button type="primary" circle @click="showCreateGroupDialog">
            +
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
              v-for="group in displayedGroups"
              :key="group.groupId"
              class="group-item"
              :class="{ active: selectedGroup?.groupId === group.groupId }"
              @click="selectGroup(group)"
          >
            <el-avatar :src="group.avatarUrl" v-if="group.avatarUrl" />
            <el-avatar :icon="ChatLineSquare" v-else />
            <div class="group-info">
              <div class="group-name">{{ group.groupName }}</div>
              <div class="group-description">{{ group.description }}</div>
            </div>
            <div class="group-members">
              {{ group.memberCount }}人
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧聊天区域 -->
      <div class="group-main" v-if="selectedGroup">
        <div class="chat-header">
          <div class="chat-header-info">
            <el-avatar :src="selectedGroup.avatarUrl" :size="40" v-if="selectedGroup.avatarUrl" />
            <el-avatar :icon="ChatLineSquare" :size="40" v-else />
            <div class="chat-header-text">
              <div class="group-name-header">{{ selectedGroup.groupName }}</div>
              <div class="group-member-count">{{ selectedGroup.memberCount }}名成员</div>
            </div>
          </div>
          <div class="chat-header-actions">
            <el-dropdown @command="handleGroupAction">
              <el-button type="primary" class="group-action-button">
                操作<i class="el-icon-arrow-down el-icon--right"></i>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="!isGroupOwner" command="quit">退出群聊</el-dropdown-item>
                  <el-dropdown-item v-if="isGroupOwner" command="kick">踢出成员</el-dropdown-item>
                  <el-dropdown-item v-if="isGroupOwner" command="disband">解散群组</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <div class="chat-content">
          <div class="message-list" ref="messageListRef">
            <div
                v-for="message in groupMessages"
                :key="message.messageId"
                class="message-item"
                :class="{ 'is-self': isSelfMessage(message) }"
            >
              <el-avatar
                  :size="32"
                  :src="message.userAvatarUrl || defaultAvatar"
                  class="message-avatar"
              />
              <div class="message-content">
                <!-- 引用消息预览 -->
                <div v-if="message.quoteMessageId" class="quote-preview">
                  <div class="quote-preview-content">
                    <div class="quote-sender">{{ message.quoteMessageSenderName }}</div>
                    <div class="quote-text" v-if="message.quoteMessageType === 'image'">[图片]</div>
                    <div class="quote-text" v-else-if="message.quoteMessageType === 'file'">[文件] {{ message.quoteMessageFileName }}</div>
                    <div class="quote-text" v-else>{{ message.quoteMessageContent }}</div>
                  </div>
                </div>
                
                <div class="message-info">
                  <span class="username">{{ message.userName || message.fromUsername }}</span>
                  <span class="time">{{ formatTime(message.serverTimestamp) }}</span>
                </div>
                <!-- 文本消息 -->
                <div v-if="message.type === 'text'" class="message-text">{{ message.textContent }}</div>
                <!-- 图片消息 -->
                <div v-else-if="message.type === 'image'" class="message-image">
                  <img :src="message.imageUrls" alt="图片消息" @click="previewImage(message.imageUrls)" />
                </div>
                <!-- 文件消息 -->
                <div v-else-if="message.type === 'file'" class="message-file">
                  <div class="file-icon">📄</div>
                  <div class="file-info">
                    <div class="file-name" @click="downloadFile(message.fileUrls, message.fileName)">{{ message.fileName }}</div>
                    <div class="file-size">{{ formatFileSize(message.fileSize) }}</div>
                  </div>
                </div>
                <!-- 引用消息 -->
                <div v-else-if="message.type === 'quote'" class="message-text">
                  <div class="quoted-content">{{ message.textContent }}</div>
                </div>
                <!-- 默认消息 -->
                <div v-else class="message-text">{{ message.textContent }}</div>
              </div>
            </div>
          </div>

          <div class="message-input-area">
            <!-- 引用消息显示区域 -->
            <div v-if="quotedMessage" class="quote-preview">
              <div class="quote-preview-content">
                <div class="quote-sender">{{ quotedMessage.fromUsername }}</div>
                <div class="quote-text" v-if="quotedMessage.type === 'image'">[图片]</div>
                <div class="quote-text" v-else-if="quotedMessage.type === 'file'">[文件] {{ quotedMessage.fileName }}</div>
                <div class="quote-text" v-else>{{ quotedMessage.textContent }}</div>
              </div>
              <button class="cancel-quote" @click="cancelQuote">×</button>
            </div>
            
            <!-- 工具栏 -->
            <div class="chat-input-tools-wrapper">
              <button 
                @click="toggleAttachmentToolbar"
                class="toggle-attachment-button"
                :class="{ expanded: showAttachmentToolbar }"
                title="附件">
                +
              </button>
              
              <div v-show="showAttachmentToolbar" class="chat-input-tools">
                <button 
                  @click="selectImage"
                  class="icon-button"
                  title="发送图片">
                  📷
                </button>
                <button 
                  @click="selectFile"
                  class="icon-button"
                  title="发送文件">
                  📎
                </button>
              </div>
            </div>
            
            <!-- 输入框 -->
            <div class="input-container-wrapper">
              <div class="input-and-toolbar-container">
                <el-input
                    v-model="currentMessage"
                    type="textarea"
                    :rows="3"
                    placeholder="输入消息..."
                    @keyup.ctrl.enter="sendMessage"
                    class="message-input"
                />
                <div class="chat-input-actions">
                  <el-button type="primary" @click="sendMessage">发送</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 未选择群组时的占位 -->
      <div class="group-placeholder" v-else>
        <div class="placeholder-content">
          <chat-line-square class="placeholder-icon" />
          <p>选择一个群组开始聊天</p>
        </div>
      </div>
    </div>
    
    <!-- 创建群组对话框 -->
    <el-dialog v-model="createGroupDialogVisible" title="创建群组" width="500px">
      <el-form :model="newGroupForm" label-width="80px">
        <el-form-item label="群组名称">
          <el-input v-model="newGroupForm.groupName" autocomplete="off" />
        </el-form-item>
        <el-form-item label="群组头像">
          <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
              :on-change="handleAvatarChange"
          >
            <img v-if="newGroupForm.avatarUrl" :src="newGroupForm.avatarUrl" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="群组介绍">
          <el-input
              v-model="newGroupForm.description"
              type="textarea"
              :rows="3"
              placeholder="请输入群组介绍"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="createGroupDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="createGroup">创建</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 搜索结果对话框 -->
    <el-dialog v-model="searchResultDialogVisible" title="搜索结果" width="500px">
      <div class="search-results">
        <div 
          v-for="group in searchResults" 
          :key="group.groupId" 
          class="search-result-item"
          @click="showGroupDetail(group)"
        >
          <el-avatar :src="group.avatarUrl" v-if="group.avatarUrl" />
          <el-avatar :icon="ChatLineSquare" v-else />
          <div class="group-info">
            <div class="group-name">{{ group.groupName }}</div>
            <div class="group-id">ID: {{ group.groupId }}</div>
          </div>
          <el-button type="primary" size="small" @click.stop="joinGroup(group)">加入</el-button>
        </div>
        <div v-if="searchResults.length === 0" class="no-results">
          未找到相关群组
        </div>
      </div>
    </el-dialog>
    
    <!-- 群组详情对话框 -->
    <el-dialog v-model="groupDetailDialogVisible" :title="selectedGroupForDetail?.groupName" width="500px">
      <div class="group-detail">
        <div class="group-detail-header">
          <el-avatar :src="selectedGroupForDetail?.avatarUrl" :size="80" v-if="selectedGroupForDetail?.avatarUrl" />
          <el-avatar :icon="ChatLineSquare" :size="80" v-else />
        </div>
        <div class="group-detail-info">
          <div class="group-detail-row">
            <span class="group-detail-label">群组名称:</span>
            <span class="group-detail-value">{{ selectedGroupForDetail?.groupName }}</span>
          </div>
          <div class="group-detail-row">
            <span class="group-detail-label">群组ID:</span>
            <span class="group-detail-value">{{ selectedGroupForDetail?.groupId }}</span>
          </div>
          <div class="group-detail-row">
            <span class="group-detail-label">成员人数:</span>
            <span class="group-detail-value">{{ selectedGroupForDetail?.memberCount }}人</span>
          </div>
          <div class="group-detail-row" v-if="selectedGroupForDetail?.description">
            <span class="group-detail-label">群组介绍:</span>
            <span class="group-detail-value">{{ selectedGroupForDetail?.description }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="groupDetailDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="joinGroupFromDetail">加入群组</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 踢出成员对话框 -->
    <el-dialog 
      v-model="kickMemberDialogVisible" 
      title="踢出成员" 
      width="500px"
      class="kick-member-dialog"
    >
      <div class="member-list-container">
        <div class="member-search-box" v-if="groupMembers.length > 5">
          <el-input
            v-model="memberSearchKeyword"
            placeholder="搜索成员"
            clearable
            @input="filterMembers"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        
        <div class="member-list">
          <div 
            v-for="(member, index) in filteredGroupMembers" 
            :key="member.userId || index"
            class="member-item"
            :class="{ 'current-user': (member.userId) === getCurrentUserId() }"
          >
            <el-avatar 
              :src="member.avatarUrl || defaultAvatar" 
              :size="36"
              class="member-avatar"
            />
            <div class="member-info">
              <div class="member-name">{{ member.userName }}</div>
              <div class="member-meta">
                <div class="member-role" :class="(member.role || 'member').toLowerCase()">
                  {{ getRoleDisplayName(member.role) }}
                </div>
                <div class="member-id" v-if="member.userId && showMemberIds">
                  ID: {{ member.userId }}
                </div>
              </div>
            </div>
            <el-button 
              v-if="(member.userId) !== getCurrentUserId() && isGroupOwner" 
              type="danger" 
              size="small" 
              @click="kickMember(member)"
              :loading="kickingMemberId === member.userId"
              class="kick-button"
            >
              踢出
            </el-button>
            <el-tag 
              v-if="(member.userId) === getCurrentUserId()" 
              type="info" 
              size="small"
              class="self-tag"
            >
              自己
            </el-tag>
          </div>
          
          <div v-if="filteredGroupMembers.length === 0" class="no-members">
            <el-icon><Warning /></el-icon>
            <span>未找到相关成员</span>
          </div>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-checkbox v-model="showMemberIds" class="show-id-checkbox">显示用户ID</el-checkbox>
          <el-button @click="kickMemberDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed, onUnmounted } from 'vue'
import { ChatLineSquare, Plus, Search, Warning } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/userStore'
import { ElMessage, ElMessageBox } from 'element-plus'
import apiClient, { API_ENDPOINTS } from '../services/api'
import { getUserInfo } from '../utils/user'
import globalWebSocketManager from '@/services/websocket'

// 用户store
const userStore = useUserStore()

// 状态管理
const searchKeyword = ref('')
const currentMessage = ref('')
const selectedGroup = ref<any>(null)
const groups = ref<any[]>([])
const groupMessages = ref<any[]>([])
const messageListRef = ref<HTMLElement | null>(null)
const createGroupDialogVisible = ref(false)
const searchResultDialogVisible = ref(false)
const searchResults = ref<any[]>([])
const groupDetailDialogVisible = ref(false)
const selectedGroupForDetail = ref<any>(null)

// 新增的状态
const showAttachmentToolbar = ref(false)
const selectedImage = ref<File | null>(null)
const quotedMessage = ref<any>(null)
const groupAvatar = ref<File | null>(null)
const isGroupOwner = ref(false)
const groupMembers = ref<any[]>([])
const filteredGroupMembers = ref<any[]>([])
const kickMemberDialogVisible = ref(false)
const kickingMemberId = ref<string | null>(null)
const memberSearchKeyword = ref('')
const defaultAvatar = ref('/src/assets/logo.svg')
const showMemberIds = ref(false) // 控制是否显示用户ID

// 表单数据
const newGroupForm = ref({
  groupName: '',
  avatarUrl: '',
  description: ''
})

// 计算属性：显示的群组列表（搜索结果或全部群组）
const displayedGroups = computed(() => {
  if (searchKeyword.value.trim() === '') {
    return groups.value
  }
  // 简单的本地过滤
  return groups.value.filter(group => 
    group.groupName.includes(searchKeyword.value) || 
    group.groupId.includes(searchKeyword.value)
  )
})

// 格式化时间
const formatTime = (timestamp: number) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 获取用户ID的辅助函数
const getCurrentUserId = () => {
  const userInfo = getUserInfo()
  return userInfo ? userInfo.userId : null
}

// 判断是否为自己发送的消息
const isSelfMessage = (message: any) => {
  const currentUserId = getCurrentUserId()
  console.log('判断消息归属:', {
    currentUserId: currentUserId,
    messageFromUserId: message.fromUserId,
    isSelf: currentUserId && message.fromUserId && currentUserId === message.fromUserId
  })
  return currentUserId && message.fromUserId && currentUserId === message.fromUserId
}

// 加载群组列表
const loadGroups = async () => {
  try {
    const response = await apiClient.get(API_ENDPOINTS.GROUPLIST, {
      params: { userId: getCurrentUserId() }
    })
    groups.value = response.data || []
  } catch (error: any) {
    ElMessage.error('加载群组列表失败: ' + (error.message || '未知错误'))
  }
}

// 加载群组消息
const loadGroupMessages = async () => {
  if (!selectedGroup.value) return
  
  try {
    const response = await apiClient.get(`${API_ENDPOINTS.GROUPMESSAGES}/${selectedGroup.value.groupId}`)
    groupMessages.value = response.data || []
    
    // 滚动到最新消息
    scrollToBottom()
  } catch (error) {
    console.error('获取群组消息失败:', error)
    ElMessage.error('获取群组消息失败')
  }
}

const selectGroup = async (group: any) => {
  try {
    // 取消订阅之前群组的消息
    if (selectedGroup.value) {
      globalWebSocketManager.unsubscribeFromGroupMessages(selectedGroup.value.groupId)
    }
    
    selectedGroup.value = group
    
    // 订阅新群组的消息
    globalWebSocketManager.subscribeToGroupMessages(group.groupId, (message) => {
      console.log('收到群组消息:', message)
      console.log('当前用户ID:', getCurrentUserId())
      console.log('消息发送者ID:', message.fromUserId)
      console.log('是否为自己发送:', message.fromUserId === getCurrentUserId())
      // 添加到消息列表
      groupMessages.value.push(message)
      
      // 滚动到最新消息
      nextTick(() => {
        scrollToBottom()
      })
    })
    
    // 加载群组消息历史
    await loadGroupMessages()
    
    // 滚动到最新消息
    nextTick(() => {
      scrollToBottom()
    })
    
    // 检查是否是群主
    const userInfo = getUserInfo()
    isGroupOwner.value = group.creatorId === userInfo?.userId
  } catch (error) {
    console.error('选择群组时出错:', error)
    ElMessage.error('加载群组信息失败')
  }
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

// 加载群成员列表
const loadGroupMembers = async () => {
  if (!selectedGroup.value) return
  
  try {
    const response = await apiClient.get(`${API_ENDPOINTS.GROUPINFO}/${selectedGroup.value.groupId}/members`)
    console.log('群成员列表原始数据:', response) // 调试日志
    console.log('群成员列表数据:', response.data) // 调试日志
    
    groupMembers.value = response.data || []
    filteredGroupMembers.value = [...groupMembers.value]
    
    // 添加额外的调试信息
    if (groupMembers.value.length > 0) {
      console.log('第一个成员信息:', groupMembers.value[0])
    }
  } catch (error) {
    console.error('获取群成员列表失败:', error)
    ElMessage.error('获取群成员列表失败')
  }
}

// 过滤成员
const filterMembers = () => {
  if (!memberSearchKeyword.value.trim()) {
    filteredGroupMembers.value = [...groupMembers.value]
    return
  }
  
  const keyword = memberSearchKeyword.value.toLowerCase()
  filteredGroupMembers.value = groupMembers.value.filter(member => 
    member.userName.toLowerCase().includes(keyword)
  )
}

// 获取角色显示名称
const getRoleDisplayName = (role: string) => {
  if (!role) return '成员'
  
  switch (role.toLowerCase()) {
    case 'owner':
      return '群主'
    case 'admin':
      return '管理员'
    case 'member':
      return '成员'
    default:
      return role
  }
}

const showCreateGroupDialog = () => {
  newGroupForm.value.groupName = ''
  newGroupForm.value.avatarUrl = ''
  newGroupForm.value.description = ''
  groupAvatar.value = null
  createGroupDialogVisible.value = true
}

const beforeAvatarUpload = (file: File) => {
  const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 10

  if (!isJPG) {
    ElMessage.error('头像图片只能是 JPG 或 PNG 格式!')
  }
  if (!isLt2M) {
    ElMessage.error('头像图片大小不能超过 10MB!')
  }
  return isJPG && isLt2M
}

const handleAvatarChange = (file: any) => {
  groupAvatar.value = file.raw
  // 预览图片
  newGroupForm.value.avatarUrl = URL.createObjectURL(file.raw)
}

const createGroup = async () => {
  if (!newGroupForm.value.groupName.trim()) {
    ElMessage.warning('请输入群组名称')
    return
  }
  
  try {
    let avatarUrl = ''
    
    // 如果选择了头像，先上传头像
    if (groupAvatar.value) {
      // 显示上传提示
      const loadingMessage = ElMessage({
        message: '正在上传群头像...',
        type: 'info',
        duration: 0
      })
      
      try {
        // 上传头像到OSS
        const formData = new FormData()
        formData.append('file', groupAvatar.value)
        formData.append('fileType', 'group')

        const uploadResponse = await apiClient.post(API_ENDPOINTS.FILE_UPLOAD, formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })

        loadingMessage.close()

        if (uploadResponse.code !== 200) {
          throw new Error(uploadResponse?.msg || '上传失败')
        }

        avatarUrl = uploadResponse.data
      } catch (uploadError: any) {
        loadingMessage.close()
        ElMessage.error('上传群头像失败: ' + (uploadError.message || '未知错误'))
        return
      }
    }
    
    const groupData = {
      groupName: newGroupForm.value.groupName,
      userId: getCurrentUserId(),
      avatarUrl: avatarUrl,
      description: newGroupForm.value.description
    }
    
    await apiClient.post(API_ENDPOINTS.CREATEGROUP, groupData)
    ElMessage.success('创建群组成功')
    createGroupDialogVisible.value = false
    await loadGroups()
  } catch (error: any) {
    ElMessage.error('创建群组失败: ' + (error.message || '未知错误'))
  }
}

const searchGroups = async () => {
  if (!searchKeyword.value.trim()) {
    return
  }
  
  try {
    const response = await apiClient.get(API_ENDPOINTS.GROUPSEARCH, {
      params: { keyword: searchKeyword.value, userId: getCurrentUserId() }
    })
    searchResults.value = response.data || []
    searchResultDialogVisible.value = true
  } catch (error: any) {
    ElMessage.error('搜索群组失败: ' + (error.message || '未知错误'))
  }
}

const joinGroup = async (group: any) => {
  try {
    const groupData = {
      groupId: group.groupId,
      userId: getCurrentUserId()
    }
    
    await apiClient.post(API_ENDPOINTS.JOINGROUP, groupData)
    ElMessage.success('加入群组成功')
    searchResultDialogVisible.value = false
    groupDetailDialogVisible.value = false
    await loadGroups()
  } catch (error: any) {
    ElMessage.error('加入群组失败: ' + (error.message || '未知错误'))
  }
}

const showGroupDetail = (group: any) => {
  selectedGroupForDetail.value = group
  groupDetailDialogVisible.value = true
}

const joinGroupFromDetail = () => {
  if (selectedGroupForDetail.value) {
    joinGroup(selectedGroupForDetail.value)
  }
}

const handleGroupAction = (command: string) => {
  switch (command) {
    case 'kick':
      showKickMemberDialog()
      break
    case 'disband':
      disbandGroup()
      break
    case 'quit':
      quitGroup()
      break
  }
}

// 显示踢出成员对话框
const showKickMemberDialog = async () => {
  kickMemberDialogVisible.value = true
  // 加载群成员列表
  await loadGroupMembers()
  // 重置搜索关键词
  memberSearchKeyword.value = ''
  // 重新过滤成员列表
  filteredGroupMembers.value = [...groupMembers.value]
}

// 踢出成员
const kickMember = async (member: any) => {
  ElMessageBox.confirm(`确定要将 "${member.userName}" 踢出群组吗？`, '确认踢出', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      kickingMemberId.value = member.userId
      
      const requestData = {
        groupId: selectedGroup.value.groupId,
        userId: member.userId
      }
      
      await apiClient.post(API_ENDPOINTS.KICK_MEMBER, requestData)
      ElMessage.success(`已将 ${member.userName} 踢出群组`)
      
      // 重新加载成员列表
      await loadGroupMembers()
      
      // 如果踢的是自己，退出群组界面
      if (member.userId === getCurrentUserId()) {
        selectedGroup.value = null
        await loadGroups()
      }
    } catch (error: any) {
      ElMessage.error('踢出成员失败: ' + (error.message || '未知错误'))
    } finally {
      kickingMemberId.value = null
    }
  }).catch(() => {
    // 取消操作
  })
}

// 解散群组
const disbandGroup = () => {
  ElMessageBox.confirm('确定要解散该群组吗？解散后所有成员将被移出，数据将无法恢复！', '确认解散', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    try {
      const requestData = {
        groupId: selectedGroup.value.groupId,
        userId: getCurrentUserId()
      }
      
      await apiClient.post(API_ENDPOINTS.DISBAND_GROUP, requestData)
      ElMessage.success('群组已解散')
      selectedGroup.value = null
      await loadGroups()
    } catch (error: any) {
      ElMessage.error('解散群组失败: ' + (error.message || '未知错误'))
    }
  }).catch(() => {
    // 取消操作
  })
}

const quitGroup = async () => {
  ElMessageBox.confirm('确定要退出该群组吗？', '确认退出', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const groupData = {
        groupId: selectedGroup.value.groupId,
        userId: getCurrentUserId()
      }
      
      await apiClient.post(API_ENDPOINTS.QUITGROUP, groupData)
      ElMessage.success('退出群组成功')
      selectedGroup.value = null
      await loadGroups()
    } catch (error: any) {
      ElMessage.error('退出群组失败: ' + (error.message || '未知错误'))
    }
  }).catch(() => {
    // 取消操作
  })
}

// 发送消息相关函数
const toggleAttachmentToolbar = () => {
  showAttachmentToolbar.value = !showAttachmentToolbar.value
}

const selectImage = () => {
  const fileInput = document.createElement('input')
  fileInput.type = 'file'
  fileInput.accept = 'image/*'
  fileInput.onchange = (e) => {
    const target = e.target as HTMLInputElement
    if (target.files && target.files[0]) {
      selectedImage.value = target.files[0]
      sendImageMessage()
    }
  }
  fileInput.click()
}

const selectFile = () => {
  const fileInput = document.createElement('input')
  fileInput.type = 'file'
  fileInput.onchange = (e) => {
    const target = e.target as HTMLInputElement
    if (target.files && target.files[0]) {
      sendFileMessage(target.files[0])
    }
  }
  fileInput.click()
}

const sendImageMessage = async () => {
  if (!selectedImage.value || !selectedGroup.value) return

  try {
    // 显示上传提示
    const loadingMessage = ElMessage({
      message: '正在上传图片...',
      type: 'info',
      duration: 0
    })

    // 上传图片到OSS (指定文件类型为message)
    const formData = new FormData()
    formData.append('file', selectedImage.value)
    formData.append('fileType', 'message') // 指定文件类型为message，将存储到messages目录

    const uploadResponse = await apiClient.post(API_ENDPOINTS.FILE_UPLOAD, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    loadingMessage.close()

    if (uploadResponse.code !== 200) {
      throw new Error(uploadResponse?.msg || '上传失败')
    }

    const imageUrl = uploadResponse.data

    // 通过WebSocket发送图片消息
    if (globalWebSocketManager.isConnected()) {
      const messageToSend = {
        type: 'image',
        toTargetId: selectedGroup.value.groupId,
        toTargetType: 'group',
        imageUrls: imageUrl, // 发送相对路径
        textContent: '[图片]',
        clientTimestamp: Date.now(),
        quoteMessageId: quotedMessage.value ? quotedMessage.value.messageId : undefined
      }

      globalWebSocketManager.sendMessage(messageToSend)

      // 清除输入框和引用消息
      currentMessage.value = ''
      quotedMessage.value = null
    } else {
      ElMessage.error('WebSocket未连接，无法发送消息')
    }
  } catch (error: any) {
    ElMessage.closeAll()
    ElMessage.error('发送图片失败: ' + (error.message || '未知错误'))
  }
}

const sendFileMessage = async (file: File) => {
  if (!file || !selectedGroup.value) return

  try {
    // 显示上传提示
    const loadingMessage = ElMessage({
      message: '正在上传文件...',
      type: 'info',
      duration: 0
    })

    // 上传文件到OSS
    const formData = new FormData()
    formData.append('file', file)

    const uploadResponse = await apiClient.post(API_ENDPOINTS.FILE_UPLOAD, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    loadingMessage.close()

    if (uploadResponse.code !== 200) {
      throw new Error(uploadResponse?.msg || '上传失败')
    }

    const fileUrl = uploadResponse.data.url

    // 通过WebSocket发送文件消息
    if (globalWebSocketManager.isConnected()) {
      const messageToSend = {
        type: 'file',
        toTargetId: selectedGroup.value.groupId,
        toTargetType: 'group',
        fileUrl: fileUrl,
        fileName: file.name,
        fileSize: file.size,
        textContent: `[文件] ${file.name}`,
        clientTimestamp: Date.now(),
        quoteMessageId: quotedMessage.value ? quotedMessage.value.messageId : undefined
      }

      globalWebSocketManager.sendMessage(messageToSend)

      // 清除输入框和引用消息
      currentMessage.value = ''
      quotedMessage.value = null
    } else {
      ElMessage.error('WebSocket未连接，无法发送消息')
    }
  } catch (error: any) {
    ElMessage.closeAll()
    ElMessage.error('发送文件失败: ' + (error.message || '未知错误'))
  }
}

const sendMessage = async () => {
  if ((!currentMessage.value.trim() && !quotedMessage.value) || !selectedGroup.value) return

  try {
    // 通过WebSocket发送文本消息
    if (globalWebSocketManager.isConnected()) {
      const messageToSend = {
        type: 'text',
        toTargetId: selectedGroup.value.groupId,
        toTargetType: 'group',
        textContent: currentMessage.value,
        clientTimestamp: Date.now(),
        quoteMessageId: quotedMessage.value ? quotedMessage.value.messageId : undefined
      }

      globalWebSocketManager.sendMessage(messageToSend)

      // 清空输入框和引用消息
      currentMessage.value = ''
      quotedMessage.value = null
    } else {
      ElMessage.error('WebSocket未连接，无法发送消息')
    }
  } catch (error: any) {
    ElMessage.error('发送消息失败: ' + (error.message || '未知错误'))
  }
}

const cancelQuote = () => {
  quotedMessage.value = null
}

const previewImage = (url: string) => {
  if (url) {
    window.open(url, '_blank')
  }
}

const downloadFile = (fileUrl: string, fileName: string) => {
  const link = document.createElement('a')
  link.href = fileUrl
  link.download = fileName
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 组件挂载时加载群组列表
onMounted(() => {
  loadGroups()
  
  // 监听全局实时消息事件
  window.addEventListener('realTimeMessage', handleRealTimeMessage as EventListener)
})

// 组件销毁时清理资源
onUnmounted(() => {
  // 移除事件监听器
  window.removeEventListener('realTimeMessage', handleRealTimeMessage as EventListener)
  
  // 取消订阅群组消息
  if (selectedGroup.value) {
    globalWebSocketManager.unsubscribeFromGroupMessages(selectedGroup.value.groupId)
  }
})

// 处理实时消息
const handleRealTimeMessage = (event: CustomEvent) => {
  const message = event.detail
  console.log('收到实时消息:', message)
  
  // 如果是当前群组的消息，则添加到消息列表中
  if (selectedGroup.value && message.toTargetId === selectedGroup.value.groupId) {
    // 确保消息包含发送者用户名
    if (!message.fromUsername) {
      message.fromUsername = '未知用户'
    }
    
    // 确保消息有时间戳
    if (!message.serverTimestamp) {
      message.serverTimestamp = Date.now()
    }
    
    // 确保消息有默认头像
    if (!message.userAvatarUrl || message.userAvatarUrl === 'null' || message.userAvatarUrl === 'undefined') {
      message.userAvatarUrl = defaultAvatar.value
    }
    
    groupMessages.value.push(message)
    
    // 滚动到最新消息
    nextTick(() => {
      if (messageListRef.value) {
        messageListRef.value.scrollTop = messageListRef.value.scrollHeight
      }
    })
  }
}

</script>

<style>
.group-view {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

.group-container {
  display: flex;
  height: 100%;
  background-color: #ffffff;
}

.dark-mode .group-container {
  background-color: #1a1a1a;
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
  background-color: #2d2d2d;
}

.group-sidebar-header {
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee;
  background-color: #ffffff;
}

.group-sidebar-header h2 {
  color: #1a1a1a;
  margin: 0;
}

.dark-mode .group-sidebar-header {
  border-bottom: 1px solid #444;
  background-color: #2d2d2d;
}

.dark-mode .group-sidebar-header h2 {
  color: #ffffff;
}

.search-box {
  padding: 10px;
  border-bottom: 1px solid #eee;
}

.dark-mode .search-box {
  border-bottom: 1px solid #444;
}

.groups-list {
  flex: 1;
  overflow-y: auto;
}

.group-item {
  padding: 15px;
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: background-color 0.3s;
}

.group-item:hover {
  background-color: #f5f5f5;
}

.group-item.active {
  background-color: #e6f7ff;
}

.dark-mode .group-item:hover {
  background-color: #2d2d2d;
}

.dark-mode .group-item.active {
  background-color: #1f1f1f;
}

.group-info {
  flex: 1;
  margin-left: 15px;
}

.group-name {
  font-weight: bold;
  margin-bottom: 5px;
  color: #1a1a1a;
}

.dark-mode .group-name {
  color: #f5f5f5;
}

.group-description {
  font-size: 12px;
  color: #666;
}

.dark-mode .group-description {
  color: #ccc;
}

.group-members {
  font-size: 12px;
  color: #999;
}

.group-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
}

.dark-mode .group-main {
  background-color: #1a1a1a;
}

.chat-header {
  height: 80px;
  padding: 15px 20px;
  border-bottom: 1px solid #ddd;
  background-color: #ffffff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dark-mode .chat-header {
  border-bottom: 1px solid #444;
  background-color: #333333;
  color: #ffffff;
}

.dark-mode .chat-header {
  border-bottom: 1px solid #444;
  background-color: #2d2d2d;
}

.chat-header-info {
  display: flex;
  align-items: center;
}

.chat-header-text {
  margin-left: 15px;
}

.group-name-header {
  font-weight: bold;
  font-size: 16px;
  color: #1a1a1a;
}

.dark-mode .group-name-header {
  color: #ffffff;
}

.group-member-count {
  font-size: 12px;
  color: #666;
  margin-top: 5px;
}

.dark-mode .group-member-count {
  color: #e0e0e0;
}

.chat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: #ffffff;
}

.dark-mode .chat-content {
  background-color: #1a1a1a;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
}

.message-item.is-self {
  flex-direction: row-reverse !important;
}

.message-item.is-self .message-content {
  align-items: flex-end !important;
}

.message-item.is-self .message-info {
  flex-direction: row-reverse !important;
}

.message-avatar {
  margin: 0 10px;
  flex-shrink: 0;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 45%;
}

.message-info {
  display: flex;
  align-items: center;
  margin-bottom: 5px;
}

.message-info .username {
  font-size: 12px;
  color: #666;
  margin-right: 10px;
  font-weight: 500;
}

.message-info .time {
  margin-bottom: -4px;
  font-size: 12px;
  color: #999;
}

.is-self .message-info .username {
  margin-left: 10px;
}

.message-text {
  background-color: #f5f5f5;
  padding: 10px 15px;
  border-radius: 8px;
  word-break: break-word;
  color: #1a1a1a;
  display: inline-block;
  max-width: 100%;
  white-space: pre-wrap;
  width: fit-content;
}

.dark-mode .message-text {
  background-color: #2d2d2d;
  color: #f5f5f5;
}

.message-item.is-self .message-text {
  background-color: #1e90ff !important;
  color: white !important;
  width: fit-content;
}

.dark-mode .message-item.is-self .message-text {
  background-color: #0077e6 !important;
  color: white !important;
  width: fit-content;
}

.message-image img {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  cursor: pointer;
  display: block;
  width: fit-content;
}

.message-file {
  display: flex;
  align-items: center;
  padding: 10px;
  background-color: #f5f5f5;
  border-radius: 8px;
  max-width: 300px;
  white-space: pre-wrap;
  width: fit-content;
}

.dark-mode .message-file {
  background-color: #333;
  display: flex;
}

.file-icon {
  font-size: 24px;
  margin-right: 10px;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  color: #1a1a1a;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dark-mode .file-name {
  color: #ffffff;
}

.file-size {
  font-size: 12px;
  color: #666;
  margin-top: 2px;
}

.dark-mode .file-size {
  color: #e0e0e0;
}

.message-input-area {
  padding: 20px;
  border-top: 1px solid #ddd;
  background-color: #ffffff;
}

.dark-mode .message-input-area {
  border-top: 1px solid #444;
  background-color: #1a1a1a;
}

.quote-preview {
  background-color: #f0f0f0;
  border-left: 3px solid #0084ff;
  padding: 8px 12px;
  margin-bottom: 8px;
  margin-top: 0;
  border-radius: 4px;
  font-size: 13px;
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dark-mode .quote-preview {
  background-color: #333;
  border-left-color: #0077e6;
}

.quote-preview-content {
  flex: 1;
  min-width: 0;
  max-width: 300px;
}

.quote-preview .quote-sender {
  font-weight: bold;
  color: #0084ff;
  margin-bottom: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quote-preview .quote-text {
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dark-mode .quote-preview .quote-sender {
  color: #409eff;
}

.dark-mode .quote-preview .quote-text {
  color: #ccc;
}

.cancel-quote {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: #999;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  margin-left: 8px;
  flex-shrink: 0;
}

.cancel-quote:hover {
  background-color: #e0e0e0;
  color: #666;
}

.dark-mode .cancel-quote {
  color: #aaa;
}

.dark-mode .cancel-quote:hover {
  background-color: #444;
  color: #fff;
}

.chat-input-tools-wrapper {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.toggle-attachment-button {
  margin-right: 8px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #dcdfe6;
  background-color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: all 0.3s;
  color: #606266;
  line-height: 1;
}

.dark-mode .toggle-attachment-button {
  background-color: #2d2d2d;
  border-color: #444;
  color: #ccc;
}

.toggle-attachment-button.expanded {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.dark-mode .toggle-attachment-button.expanded {
  background: linear-gradient(135deg, #764ba2, #667eea);
  color: white;
  border-color: #764ba2;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.chat-input-tools {
  display: flex;
  gap: 8px;
}

.icon-button {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #dcdfe6;
  background-color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: all 0.3s;
  color: #606266;
  line-height: 1;
}

.icon-button:hover {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.dark-mode .icon-button {
  background-color: #2d2d2d;
  border-color: #444;
  color: #ccc;
}

.dark-mode .icon-button:hover {
  background: linear-gradient(135deg, #764ba2, #667eea);
  color: white;
  border-color: #764ba2;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.message-input {
  margin-bottom: 10px;
}

.message-input :deep(.el-textarea__inner) {
  background-color: #ffffff;
  border-color: #ddd;
  color: #1a1a1a;
}

.dark-mode .message-input :deep(.el-textarea__inner) {
  background-color: #1a1a1a;
  border-color: #444;
  color: #f5f5f5;
}

.dark-mode .message-input :deep(.el-textarea__inner:focus) {
  border-color: #409eff;
}

.chat-input-actions {
  display: flex;
  justify-content: flex-end;
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

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.search-results {
  max-height: 400px;
  overflow-y: auto;
}

.search-result-item {
  display: flex;
  align-items: center;
  padding: 10px;
  cursor: pointer;
  border-bottom: 1px solid #eee;
}

.search-result-item:hover {
  background-color: #f5f5f5;
}

.search-result-item .group-info {
  flex: 1;
  margin: 0 15px;
}

.group-id {
  font-size: 12px;
  color: #999;
}

.no-results {
  text-align: center;
  padding: 20px;
  color: #999;
}

.group-detail {
  text-align: center;
}

.group-detail-header {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.group-detail-info {
  text-align: left;
}

.group-detail-row {
  display: flex;
  margin-bottom: 15px;
  align-items: flex-start;
}

.group-detail-label {
  font-weight: bold;
  width: 80px;
  color: #666;
}

.group-detail-value {
  flex: 1;
  word-break: break-word;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.avatar-uploader .avatar {
  max-width: 178px;
  max-height: 178px;
  width: auto;
  height: auto;
  display: block;
  object-fit: contain;
}
.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.el-icon.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  text-align: center;
}

/* 新增样式 */

.group-action-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
}

.group-action-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.group-action-button:active {
  transform: translateY(0);
}

/* 成员列表样式 */
.member-list {
  max-height: 400px;
  overflow-y: auto;
}

.member-item {
  display: flex;
  align-items: center;
  padding: 12px 10px;
  border-bottom: 1px solid #eee;
  transition: all 0.3s;
  border-radius: 8px;
  margin-bottom: 5px;
  /* 确保内容不会溢出 */
  overflow: hidden;
  /* 添加 position:relative 以便更好地控制悬停效果 */
  position: relative;
  /* 添加 z-index 以确保层级正确 */
  z-index: 1;
}

.member-item:hover {
  background-color: #f5f5f5;
  /* 使用更优雅的方式实现悬停效果，避免触发滚动条 */
  box-shadow: 3px 0 8px rgba(0, 0, 0, 0.15);
  /* 添加轻微的缩放效果 */
  transform: scale(1.02);
  /* 确保缩放以左侧为中心 */
  transform-origin: left center;
}

.dark-mode .member-item {
  border-bottom: 1px solid #444;
}

.dark-mode .member-item:hover {
  background-color: #333;
  box-shadow: 3px 0 8px rgba(0, 0, 0, 0.3);
}

.member-item.current-user {
  background-color: #e6f7ff;
}

.dark-mode .member-item.current-user {
  background-color: #1f1f1f;
}

.member-avatar {
  flex-shrink: 0;
}

.member-info {
  flex: 1;
  margin-left: 12px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.member-name {
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dark-mode .member-name {
  color: #f5f5f5;
}

.member-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  align-items: center;
}

.member-role {
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
  display: inline-block;
  width: fit-content;
}

.member-id {
  font-size: 11px;
  color: #999;
}

.member-role.owner {
  background-color: #fff7e6;
  color: #fa8c16;
}

.dark-mode .member-role.owner {
  background-color: #3a2a18;
  color: #faad14;
}

.member-role.admin {
  background-color: #f0f5ff;
  color: #2f54eb;
}

.dark-mode .member-role.admin {
  background-color: #1c2745;
  color: #597ef7;
}

.member-role.member {
  background-color: #f6ffed;
  color: #389e0d;
}

.dark-mode .member-role.member {
  background-color: #223118;
  color: #73d13d;
}

/* 踢出成员对话框样式优化 */
.kick-member-dialog .el-dialog__body {
  padding: 10px 20px 20px;
}

.member-list-container {
  max-height: 400px;
  display: flex;
  flex-direction: column;
  /* 隐藏容器内的滚动条 */
  overflow: hidden;
  /* 添加相对定位 */
  position: relative;
}

.member-search-box {
  padding: 0 0 15px 0;
}

.member-search-box .el-input__wrapper {
  border-radius: 20px;
}

.member-list {
  flex: 1;
  overflow-y: auto;
  max-height: 350px;
  /* 添加这一行确保不会有横向滚动 */
  overflow-x: hidden;
  /* 添加内边距以补偿悬停效果 */
  padding-right: 5px;
  /* 添加盒模型调整 */
  box-sizing: border-box;
}

.member-item {
  display: flex;
  align-items: center;
  padding: 12px 10px;
  border-bottom: 1px solid #eee;
  transition: all 0.3s;
  border-radius: 8px;
  margin-bottom: 5px;
  /* 确保内容不会溢出 */
  overflow: hidden;
}

.member-item:hover {
  background-color: #f5f5f5;
  /* 移除可能导致滚动条出现的 transform */
  /* transform: translateX(5px); */
  /* 使用 box-shadow 替代 transform，避免元素尺寸变化 */
  box-shadow: 5px 0 5px -5px rgba(0, 0, 0, 0.2);
}

.dark-mode .member-item {
  border-bottom: 1px solid #444;
}

.dark-mode .member-item:hover {
  background-color: #333;
}

.member-item.current-user {
  background-color: #e6f7ff;
}

.dark-mode .member-item.current-user {
  background-color: #1f1f1f;
}

.member-avatar {
  flex-shrink: 0;
}

.member-info {
  flex: 1;
  margin-left: 12px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.member-name {
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dark-mode .member-name {
  color: #f5f5f5;
}

.member-role {
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
  display: inline-block;
  width: fit-content;
}

.member-role.owner {
  background-color: #fff7e6;
  color: #fa8c16;
}

.dark-mode .member-role.owner {
  background-color: #3a2a18;
  color: #faad14;
}

.member-role.admin {
  background-color: #f0f5ff;
  color: #2f54eb;
}

.dark-mode .member-role.admin {
  background-color: #1c2745;
  color: #597ef7;
}

.member-role.member {
  background-color: #f6ffed;
  color: #389e0d;
}

.dark-mode .member-role.member {
  background-color: #223118;
  color: #73d13d;
}

.kick-button {
  flex-shrink: 0;
}

.self-tag {
  flex-shrink: 0;
}

.no-members {
  text-align: center;
  padding: 30px 0;
  color: #999;
}

.no-members .el-icon {
  font-size: 24px;
  margin-bottom: 10px;
  display: block;
}

.dialog-footer {
  text-align: right;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .member-item {
    padding: 10px 8px;
  }
  
  .member-name {
    font-size: 14px;
  }
  
  .kick-button {
    padding: 6px 10px;
    font-size: 12px;
  }
}

.show-id-checkbox {
  float: left;
  margin-right: 15px;
  margin-top: 3px;
}
</style>