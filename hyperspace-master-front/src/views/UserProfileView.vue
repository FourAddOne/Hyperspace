<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElSlider } from 'element-plus'
import {getUserInfo, saveUserInfo, updateUserInfo} from '../utils/user'
import apiClient, { API_ENDPOINTS } from '../services/api'
import { useUserStore } from '../stores/userStore'

// 用户信息
const userInfo = ref({
  userId: '',
  userName: '',
  email: '',
  Ip: '',
  avatarUrl: ''
})

// 设置选项
const settings = ref({
  darkMode: false,
  backgroundImage: '',
  backgroundOpacity: 100, // 添加背景透明度属性
  layout: 'default',
  personalSignature: '', // 添加个人签名属性
  gender: '', // 添加性别属性
  age: null // 添加年龄属性
})

// 编辑资料对话框相关
const isEditProfileDialogVisible = ref(false)
const editedUserName = ref('')
const editedPersonalSignature = ref('')
const editedGender = ref('')
const editedAge = ref(null)

// 文件输入引用
const fileInput = ref<HTMLInputElement | null>(null)
const avatarInput = ref<HTMLInputElement | null>(null)

// 路由器
const router = useRouter()

// 用户store
const userStore = useUserStore()

// 加载用户信息
const loadUserInfo = async () => {
  try {
    // 修改为直接访问data，因为拦截器现在自动返回response.data
    const response = await apiClient.get(API_ENDPOINTS.USER_INFO)
    if (response) {
      // 检查响应结构，如果是{code, msg, data}格式，则使用data字段
      const userData = response.data || response;
      userInfo.value = { ...userData }
      // 保存用户信息到store和localStorage
      userStore.setUserInfo(userData)
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
    // 如果是认证错误，重定向到登录页
    if (error.response && error.response.status === 401) {
      router.push('/login')
    }
  }
}

// 加载用户设置
const loadUserSettings = async () => {
  try {
    // 修改为直接访问data，因为拦截器现在自动返回response.data
    const response = await apiClient.get(API_ENDPOINTS.USER_SETTINGS)
    
    if (response) {
      // 检查响应结构，如果是{code, msg, data}格式，则使用data字段
      const settingsData = response.data || response;
      
      // 直接使用response而不是response.data
      // 注意：从后端获取的userData包含userId字段，但我们不需要在settings中存储userId
      const { userId, ...filteredSettings } = settingsData;
      settings.value = { 
        ...settings.value, 
        ...filteredSettings 
      }
      
      // 如果背景透明度未设置，设置默认值
      if (settings.value.backgroundOpacity === undefined) {
        settings.value.backgroundOpacity = 100;
      }
      
      // 如果背景图片为空字符串，则设置为null
      if (settings.value.backgroundImage === "") {
        settings.value.backgroundImage = null;
      }
      
      // 应用暗色模式设置
      document.body.classList.toggle('dark-mode', settings.value.darkMode)
      
      // 保存到本地存储，以便其他页面使用
      localStorage.setItem('userDarkMode', settings.value.darkMode ? 'true' : 'false')
      
      // 处理背景图片URL
      let fullBackgroundImageUrl = settings.value.backgroundImage || '';
      if (fullBackgroundImageUrl && fullBackgroundImageUrl.startsWith('/uploads/')) {
        fullBackgroundImageUrl = '/api' + fullBackgroundImageUrl;
      }
      
      localStorage.setItem('userBackgroundImage', fullBackgroundImageUrl)
      localStorage.setItem('userBackgroundOpacity', settings.value.backgroundOpacity.toString())
      
      // 如果有背景图片，添加类名以隐藏全局背景
      if (settings.value.backgroundImage) {
        document.getElementById('app')?.classList.add('has-background');
      } else {
        document.getElementById('app')?.classList.remove('has-background');
      }
      
      // 更新store中的用户设置
      userStore.setUserSettings(settings.value)
    }
  } catch (error) {
    console.error('加载用户设置失败:', error)
    // 如果是认证错误，重定向到登录页
    if (error.response && error.response.status === 401) {
      router.push('/login')
    }
  }
}

// 返回聊天界面
const goBackToChat = () => {
  router.push('/chat')
}

// 显示编辑资料对话框
const showEditProfileDialog = () => {
  editedUserName.value = userInfo.value.userName
  editedPersonalSignature.value = settings.value.personalSignature || ''
  editedGender.value = settings.value.gender || ''
  editedAge.value = settings.value.age || null
  isEditProfileDialogVisible.value = true
}

// 保存编辑的资料
const saveEditedProfile = async () => {
  try {
    // 更新用户名
    if (editedUserName.value !== userInfo.value.userName) {
      // 修改为直接访问data，因为拦截器现在自动返回response.data
      const updateResponse = await apiClient.post('/user/update', {
        userName: editedUserName.value
      })
      
      if (updateResponse && updateResponse.data) {
        // 更新本地用户信息
        userInfo.value.userName = editedUserName.value
        const currentUserInfo = getUserInfo()
        if (currentUserInfo) {
          const updatedUserInfo = { ...currentUserInfo, userName: editedUserName.value }
          saveUserInfo(updatedUserInfo)
          userStore.setUserInfo(updatedUserInfo)
        }
      }
    }
    
    // 更新个人签名、性别和年龄
    if (editedPersonalSignature.value !== settings.value.personalSignature ||
        editedGender.value !== settings.value.gender ||
        editedAge.value !== settings.value.age) {
      settings.value.personalSignature = editedPersonalSignature.value
      settings.value.gender = editedGender.value
      settings.value.age = editedAge.value
      await saveSettings()
    }
    
    // 关闭对话框
    isEditProfileDialogVisible.value = false
    ElMessage.success('资料已更新')
  } catch (error) {
    console.error('更新资料失败:', error)
    ElMessage.error('更新资料失败')
  }
}

// 取消编辑资料
const cancelEditProfile = () => {
  isEditProfileDialogVisible.value = false
}

// 处理头像选择
const handleAvatarSelect = () => {
  avatarInput.value?.click()
}

// 处理文件选择
const handleFileSelect = () => {
  fileInput.value?.click()
}

// 获取完整的头像URL
const getFullAvatarUrl = (avatarUrl: string) => {
  // 确保avatarUrl是字符串类型
  if (typeof avatarUrl !== 'string') {
    return '/src/assets/logo.svg';
  }
  
  // 如果avatarUrl是相对路径，需要拼接后端服务器地址
  if (avatarUrl && avatarUrl.startsWith('/uploads/')) {
    // 通过API代理访问上传的文件
    return '/api' + avatarUrl;
  }
  
  // 如果avatarUrl是OSS路径，直接返回
  if (avatarUrl && (avatarUrl.includes('oss') || avatarUrl.includes('aliyuncs.com'))) {
    return avatarUrl;
  }
  
  return avatarUrl || '/src/assets/logo.svg';
};

// 处理头像文件上传
const handleAvatarUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  
  if (file) {
    try {
      // 创建FormData对象用于文件上传
      const formData = new FormData()
      formData.append('file', file)
      formData.append('fileType', 'avatar') // 添加文件类型参数
      
      // 上传文件到服务器
      // 修改为直接访问data，因为拦截器现在自动返回response.data
      const response = await apiClient.post(API_ENDPOINTS.FILE_UPLOAD, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
      
      if (response) {
        // 删除旧头像文件（如果存在且不是默认头像）
        if (userInfo.value.avatarUrl) {
          try {
            await apiClient.delete('/file/delete', {
              params: { fileUrl: userInfo.value.avatarUrl }
            });
          } catch (error) {
            console.warn('删除旧头像失败:', error);
          }
        }
        
        // 确保只传递字符串URL而不是整个响应对象
        const avatarUrl = typeof response === 'string' ? response : response.data;
        
        // 更新用户头像URL
        userInfo.value.avatarUrl = avatarUrl;
        
        // 更新用户头像到数据库
        try {
          await apiClient.post('/user/avatar', null, {
            params: {
              avatarUrl: avatarUrl
            }
          });
        } catch (error) {
          console.error('更新数据库头像信息失败:', error);
          ElMessage.error('头像更新失败，请重试');
          return;
        }
        
        // 更新本地存储的用户信息
        const currentUserInfo = getUserInfo()
        if (currentUserInfo) {
          const updatedUserInfo = { ...currentUserInfo, avatarUrl: avatarUrl }
          saveUserInfo(updatedUserInfo)
          userStore.setUserInfo(updatedUserInfo)
        }
        
        ElMessage.success('头像已更新')
      }
    } catch (error: any) {
      console.error('头像上传失败:', error);
      console.error('错误详情:', {
        message: error.message,
        code: error.code,
        response: error.response,
        request: error.request
      });
      ElMessage.error('头像上传失败: ' + (error.message || '未知错误'))
    }
  }
}

// 处理背景图片上传
const handleBackgroundUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  
  if (file) {
    try {
      // 创建FormData对象用于文件上传
      const formData = new FormData()
      formData.append('file', file)
      formData.append('fileType', 'background') // 添加文件类型参数
      
      // 上传文件到服务器
      // 修改为直接访问data，因为拦截器现在自动返回response.data
      const response = await apiClient.post(API_ENDPOINTS.FILE_UPLOAD, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
      
      if (response) {
        // 删除旧的背景图片（如果存在）
        if (settings.value.backgroundImage) {
          try {
            await apiClient.delete('/file/delete', {
              params: { fileUrl: settings.value.backgroundImage }
            });
          } catch (error) {
            console.warn('删除旧背景图片失败:', error);
          }
        }
        
        // 确保只传递字符串URL而不是整个响应对象
        const backgroundImageUrl = typeof response === 'string' ? response : response.data;
        
        // 临时保存旧的背景图片URL用于错误回滚
        const oldBackgroundImage = settings.value.backgroundImage;
        
        // 更新背景图片URL
        settings.value.backgroundImage = backgroundImageUrl;
        
        // 保存设置到数据库
        try {
          await saveSettings();
        } catch (error) {
          // 如果保存失败，回滚背景图片URL
          settings.value.backgroundImage = oldBackgroundImage;
          console.error('保存背景图片设置失败:', error);
          ElMessage.error('背景图片保存失败，请重试');
          return;
        }
        
        // 添加下面这行代码来通知聊天界面更新背景
        window.dispatchEvent(new CustomEvent('backgroundImageChanged', {
          detail: { backgroundImage: backgroundImageUrl }
        }));
        
        // 同时发送不透明度更新事件，确保透明度设置同步
        window.dispatchEvent(new CustomEvent('backgroundOpacityChanged', {
          detail: { opacity: settings.value.backgroundOpacity }
        }));
        
        ElMessage.success('背景图片已更新')
      }
    } catch (error: any) {
      console.error('背景图片上传失败:', error);
      ElMessage.error('背景图片上传失败: ' + (error.message || '未知错误'))
    }
  }
}

// 保存设置
const saveSettings = async () => {
  try {
    // 修改为直接访问data，因为拦截器现在自动返回response.data
    const response = await apiClient.post(API_ENDPOINTS.USER_SETTINGS, settings.value)
    
    if (response && response.data) {
      // 保存到本地存储
      // 对于OSS URL，直接保存；对于本地URL，需要添加/api前缀
      let fullBackgroundImageUrl = settings.value.backgroundImage || '';
      if (fullBackgroundImageUrl && fullBackgroundImageUrl.startsWith('/uploads/')) {
        fullBackgroundImageUrl = '/api' + fullBackgroundImageUrl;
      }
      
      localStorage.setItem('userBackgroundImage', fullBackgroundImageUrl)
      localStorage.setItem('userBackgroundOpacity', settings.value.backgroundOpacity.toString())
    }
    
    // 如果有背景图片，添加类名以隐藏全局背景
    if (settings.value.backgroundImage) {
      document.getElementById('app')?.classList.add('has-background');
    } else {
      document.getElementById('app')?.classList.remove('has-background');
    }
    
    ElMessage.success('设置已保存')
  } catch (error) {
    console.error('保存设置失败:', error)
    ElMessage.error('保存设置失败')
  }
}

// 切换暗色模式
const toggleDarkMode = async () => {
  settings.value.darkMode = !settings.value.darkMode
  document.body.classList.toggle('dark-mode', settings.value.darkMode)
  
  // 在亮色模式下清除body的背景图片
  if (!settings.value.darkMode) {
    document.body.style.backgroundImage = 'none';
  }
  
  // 保存到本地存储，以便其他页面使用
  localStorage.setItem('userDarkMode', settings.value.darkMode ? 'true' : 'false')
  
  // 自动保存设置
  const success = await saveSettings()
  
  // 发送自定义事件通知其他组件更新暗色模式
  window.dispatchEvent(new CustomEvent('darkModeChanged', {
    detail: { darkMode: settings.value.darkMode }
  }));
  
  if (success) {
    if (settings.value.darkMode) {
      ElMessage.success('已切换到暗色模式')
    } else {
      ElMessage.success('已切换到亮色模式')
    }
  }
  
  // 同步更新store中的设置
  userStore.updateUserSettings({ darkMode: settings.value.darkMode })
}

// 更改布局
const changeLayout = async () => {
  const success = await saveSettings() // 自动保存设置
  if (success) {
    ElMessage.success('布局已更新')
  }
}

// 清除背景图片
const clearBackground = async () => {
  if (settings.value.backgroundImage) {
    try {
      // 删除背景图片文件
      await apiClient.delete('/file/delete', {
        params: { fileUrl: settings.value.backgroundImage }
      });
    } catch (error) {
      console.warn('删除背景图片失败:', error);
    }
  }
  
  settings.value.backgroundImage = '';
  settings.value.backgroundOpacity = 100; // 重置透明度
  
  try {
    // 保存设置到数据库
    const success = await saveSettings();
    
    if (success) {
      // 添加下面这行代码来通知聊天界面更新背景
      window.dispatchEvent(new CustomEvent('backgroundImageChanged', {
        detail: { backgroundImage: '' }
      }));
      
      ElMessage.success('背景已清除');
    }
  } catch (error) {
    console.error('清除背景失败:', error);
    ElMessage.error('背景清除失败，请重试');
  }
}

// 背景透明度改变
const onBackgroundOpacityChange = async (value: number) => {
  settings.value.backgroundOpacity = value;
  // 保存到本地存储
  localStorage.setItem('userBackgroundOpacity', value.toString());
  
  // 自动保存设置
  try {
    await saveSettings();
    ElMessage.success('背景透明度已保存');
    
    // 发送自定义事件通知其他组件更新
    window.dispatchEvent(new CustomEvent('backgroundOpacityChanged', {
      detail: { opacity: value }
    }));
  } catch (error) {
    console.error('保存背景透明度失败:', error);
    ElMessage.error('背景透明度保存失败');
  }
}

// 更新用户头像
const updateAvatar = async (avatarUrl: string) => {
  try {
    // 修改为直接访问data，因为拦截器现在自动返回response.data
    const response = await apiClient.post('/user/avatar', null, {
      params: {
        avatarUrl: avatarUrl
      }
    })
    return response;
  } catch (error) {
    console.error('更新头像失败:', error);
    throw error;
  }
}

onMounted(() => {
  // 从store中获取用户信息（如果存在）
  if (userStore.getUserInfo) {
    // 检查store中的用户信息结构
    const storedUserInfo = userStore.getUserInfo;
    if (storedUserInfo.data) {
      // 如果是{code, msg, data}结构，使用data字段
      userInfo.value = { ...storedUserInfo.data }
    } else {
      // 直接使用用户信息
      userInfo.value = { ...storedUserInfo }
    }
  }
  
  // 从store中获取用户设置（如果存在）
  if (userStore.getUserSettings) {
    settings.value = { ...settings.value, ...userStore.getUserSettings }
  }
  
  loadUserInfo()
  loadUserSettings()
})
</script>

<template>
  <div class="user-profile-view">
    <div class="header">
      <button @click="goBackToChat" class="back-button">返回聊天</button>
      <h1>个人资料</h1>
    </div>
    
    <div class="content">
      <div class="profile-section">
        <div class="profile-header">
          <h2>基本信息</h2>
          <button @click="showEditProfileDialog" class="edit-profile-button">编辑资料</button>
        </div>
        <div class="profile-info">
          <div class="avatar-section" >
            <img 
              :src="getFullAvatarUrl(userInfo.avatarUrl)" 
              alt="用户头像" 
              class="avatar" 
              @click="handleAvatarSelect"
            />
            <input 
              ref="avatarInput" 
              type="file" 
              accept="image/*" 
              @change="handleAvatarUpload" 
              style="display: none;"
            />
          </div>

          <div class="info-details">
            <div class="info-item">
              <label>用户ID:</label>
              <span>{{ userInfo.userId }}</span>
            </div>
            <div class="info-item">
              <label>用户名:</label>
              <span>{{ userInfo.userName }}</span>
            </div>
            <div class="info-item">
              <label>邮箱:</label>
              <span>{{ userInfo.email }}</span>
            </div>
            <div class="info-item">
              <label>个人签名:</label>
              <span>{{ settings.personalSignature && settings.personalSignature.length > 30 ? settings.personalSignature.substring(0, 30) + '...' : settings.personalSignature || '暂无签名' }}</span>
            </div>
            <div class="info-item">
              <label>性别:</label>
              <span>{{ settings.gender || '未设置' }}</span>
            </div>
            <div class="info-item">
              <label>年龄:</label>
              <span>{{ settings.age || '未设置' }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="settings-section">
        <h2>个性化设置</h2>
        <div class="settings-form">
          <div class="setting-item">
            <label>界面主题:</label>
            <div class="switch-container">
              <span :class="{ active: !settings.darkMode }">亮色模式</span>
              <button @click="toggleDarkMode" class="switch-button">
                <span :class="{ 'switch-on': settings.darkMode }" class="switch-handle"></span>
              </button>
              <span :class="{ active: settings.darkMode }">暗色模式</span>
            </div>
          </div>
          
          <div class="setting-item">
            <label>聊天背景:</label>
            <div class="background-setting">
              <button @click="handleFileSelect" class="upload-button">上传背景图片</button>
              <button v-if="settings.backgroundImage" @click="clearBackground" class="clear-button">清除背景</button>
              <div v-if="settings.backgroundImage" class="preview-background">
                <!-- 处理背景图片URL用于预览 -->
                <img :src="settings.backgroundImage.startsWith('/api/') ? settings.backgroundImage : (settings.backgroundImage.startsWith('/uploads/') ? '/api' + settings.backgroundImage : settings.backgroundImage)" alt="预览" />
              </div>
              <div class="opacity-control" v-if="settings.backgroundImage">
                <label>背景不透明度:</label>
                <el-slider 
                  v-model="settings.backgroundOpacity" 
                  :min="0" 
                  :max="100" 
                  @change="onBackgroundOpacityChange"
                  show-input>
                </el-slider>
              </div>
            </div>
            <input 
              ref="fileInput" 
              type="file" 
              accept="image/*" 
              @change="handleBackgroundUpload" 
              style="display: none;"
            />
          </div>
          
          <div class="setting-item">
            <label>布局模式:</label>
            <select v-model="settings.layout" @change="changeLayout">
              <option value="default">默认布局</option>
              <option value="compact">紧凑布局</option>
              <option value="wide">宽屏布局</option>
            </select>
          </div>
          
          <div class="setting-item">
            <label>个人签名:</label>
            <textarea 
              v-model="settings.personalSignature" 
              placeholder="请输入个人签名（最多200字）"
              maxlength="200"
              @blur="saveSettings"
              class="personal-signature-input">
            </textarea>
            <div class="signature-count">
              {{ settings.personalSignature ? settings.personalSignature.length : 0 }}/200
            </div>
          </div>

        </div>
      </div>
      
      <div class="features-section">
        <h2>实用功能</h2>
        <div class="features-grid">
          <div class="feature-card">
            <div class="feature-icon">📁</div>
            <h3>文件传输</h3>
            <p>安全快速地发送和接收文件</p>
          </div>
          
          <div class="feature-card">
            <div class="feature-icon">📅</div>
            <h3>日程提醒</h3>
            <p>设置重要消息提醒和日程安排</p>
          </div>
          
          <div class="feature-card">
            <div class="feature-icon">🔍</div>
            <h3>消息搜索</h3>
            <p>快速查找历史聊天记录</p>
          </div>
          
          <div class="feature-card">
            <div class="feature-icon">🔒</div>
            <h3>消息加密</h3>
            <p>端到端加密保护您的隐私</p>
          </div>
        </div>
      </div>
    </div>
  </div>
  
  <!-- 编辑资料对话框 -->
  <div v-if="isEditProfileDialogVisible" class="dialog-overlay" @click="cancelEditProfile">
    <div class="dialog-content" @click.stop>
      <div class="dialog-header">
        <h3>编辑个人资料</h3>
        <button class="dialog-close" @click="cancelEditProfile">×</button>
      </div>
      <div class="dialog-body">
        <div class="form-group">
          <label for="edit-username">用户名:</label>
          <input 
            id="edit-username"
            v-model="editedUserName" 
            type="text" 
            class="form-input"
            maxlength="50"
          />
        </div>
        <div class="form-group">
          <label for="edit-gender">性别:</label>
          <select id="edit-gender" v-model="editedGender" class="form-select">
            <option value="男">男</option>
            <option value="女">女</option>
            <option value="武装直升机">武装直升机</option>
            <option value="沃尔玛购物袋">沃尔玛购物袋</option>
            <option value="其他">其他</option>
          </select>
        </div>
        <div class="form-group">
          <label for="edit-age">年龄:</label>
          <input 
            id="edit-age"
            v-model.number="editedAge" 
            type="number" 
            class="form-input"
            min="1"
            max="150"
          />
        </div>
        <div class="form-group">
          <label for="edit-signature">个人签名:</label>
          <textarea 
            id="edit-signature"
            v-model="editedPersonalSignature" 
            class="form-textarea"
            placeholder="请输入个人签名（最多200字）"
            maxlength="200"
          ></textarea>
          <div class="signature-count">
            {{ editedPersonalSignature ? editedPersonalSignature.length : 0 }}/200
          </div>
        </div>
      </div>
      <div class="dialog-footer">
        <button class="cancel-button" @click="cancelEditProfile">取消</button>
        <button class="save-button" @click="saveEditedProfile">保存</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-profile-view {
  width: 100%;
  height: 100vh;
  background-color: #f5f5f5;
  overflow-y: auto;
  color: #333; /* 默认亮色模式下的文字颜色 */
}

.header {
  background-color: white;
  padding: 15px 30px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
}

.back-button {
  padding: 8px 16px;
  background-color: #0084ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.back-button:hover {
  background-color: #0066cc;
}

.header h1 {
  text-align: center;
  margin: 0;
  flex: 1;
  color: #333;
}

.content {
  max-width: 800px;
  margin: 20px auto;
  padding: 0 20px;
}

.profile-section, .settings-section, .features-section {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.profile-section h2, .settings-section h2, .features-section h2 {
  margin-top: 0;
  color: #333; /* 主要文字颜色 */
  padding-bottom: 15px; /* 从10px增加到15px，增加下方间距 */
  margin-bottom: 15px; /* 添加下方外边距 */
}

.settings-section h2 {
  border-bottom: 1px solid #ddd;
}

.profile-info {
  display: flex;
  gap: 30px;
  border-top: 1px solid #ddd;
  padding-top: 30px;
}

.avatar-section {
  text-align: center;
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  background-color: #eee;
  margin-bottom: 10px;
  cursor: pointer;
  position: relative;
  transition: opacity 0.2s;
}

.avatar:hover {
  opacity: 0.8;
}

.avatar::after {
  content: "点击更换头像";
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: rgba(0, 0, 0, 0.7);
  color: white;
  font-size: 12px;
  padding: 4px;
  border-bottom-left-radius: 50%;
  border-bottom-right-radius: 50%;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar:hover::after {
  opacity: 1;
}

.upload-button {
  padding: 6px 12px;
  background-color: #0084ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.upload-button:hover {
  background-color: #0066cc;
}

.clear-button {
  padding: 6px 12px;
  background-color: #ff4d4f;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.clear-button:hover {
  background-color: #cc0000;
}

.info-details {
  flex: 1;
}

.info-item {
  display: flex;
  margin-bottom: 15px;
  align-items: center;
}

.info-item label {
  font-weight: bold;
  width: 80px;
  color: #666; /* 次要文字颜色 */
}

.info-item span {
  color: #333; /* 主要文字颜色 */
}

.settings-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.setting-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.setting-item label {
  font-weight: bold;
  color: #333; /* 主要文字颜色 */
}

.switch-container {
  display: flex;
  align-items: center;
  gap: 10px;
}

.switch-container span {
  color: #999; /* 次要文字颜色 */
  font-size: 14px;
}

.switch-container span.active {
  color: #333; /* 主要文字颜色 */
  font-weight: bold;
}

.switch-button {
  width: 50px;
  height: 24px;
  background-color: #ccc;
  border-radius: 12px;
  border: none;
  position: relative;
  cursor: pointer;
}

.switch-handle {
  position: absolute;
  width: 20px;
  height: 20px;
  background-color: white;
  border-radius: 50%;
  top: 2px;
  left: 2px;
  transition: transform 0.2s;
}

.switch-on {
  transform: translateX(26px);
}

.background-setting {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preview-background {
  margin-top: 10px;
}

.preview-background img {
  max-width: 200px;
  max-height: 150px;
  border-radius: 4px;
  border: 1px solid #ddd;
}

select {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: white;
  color: #333; /* 主要文字颜色 */
}

.actions {
  margin-top: 10px;
}

.save-button {
  padding: 10px 20px;
  background-color: #0084ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

.save-button:hover {
  background-color: #0066cc;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
}

.feature-card {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  transition: transform 0.2s, box-shadow 0.2s;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.feature-icon {
  font-size: 2em;
  margin-bottom: 10px;
}

.feature-card h3 {
  margin: 10px 0;
  color: #333; /* 主要文字颜色 */
}

.feature-card p {
  color: #666; /* 次要文字颜色 */
  font-size: 0.9em;
}

.opacity-control {
  margin-top: 15px;
}

.opacity-control label {
  display: block;
  margin-bottom: 10px;
  font-weight: bold;
  color: #333; /* 主要文字颜色 */
}

.personal-signature-input {
  width: 100%;
  min-height: 80px;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-family: inherit;
  font-size: 14px;
  resize: vertical;
  box-sizing: border-box;
  color: #333; /* 主要文字颜色 */
}

.signature-count {
  text-align: right;
  font-size: 12px;
  color: #999; /* 次要文字颜色 */
  margin-top: 5px;
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.dialog-content {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.dialog-header h3 {
  margin: 0;
  color: #333; /* 主要文字颜色 */
}

.dialog-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999; /* 次要文字颜色 */
}

.dialog-body {
  padding: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333; /* 主要文字颜色 */
}

.form-input, .form-textarea, .form-select {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-textarea {
  min-height: 100px;
  resize: vertical;
}

.form-select {
  appearance: none;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 1rem center;
  background-size: 1em;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 20px;
  border-top: 1px solid #eee;
}

.cancel-button, .save-button {
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.cancel-button {
  background-color: #f5f5f5;
  color: #333; /* 主要文字颜色 */
  border: 1px solid #ddd;
}

.cancel-button:hover {
  background-color: #e0e0e0;
}

.save-button {
  background-color: #0084ff;
  color: white;
  border: none;
}

.save-button:hover {
  background-color: #0066cc;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

}

.edit-profile-button {
  margin-bottom: 30px;
  padding: 6px 12px;
  background-color: #0084ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.edit-profile-button:hover {
  background-color: #0066cc;
}

.dark-mode .dialog-content {
  background-color: #2d2d2d;
  color: #f5f5f5;
}

.dark-mode .dialog-header {
  border-bottom: 1px solid #444;
}

.dark-mode .dialog-header h3,
.dark-mode .form-group label {
  color: #f5f5f5; /* 暗色模式下的主要文字颜色 */
}

.dark-mode .form-input, 
.dark-mode .form-textarea,
.dark-mode .form-select {
  background-color: #3d3d3d;
  border-color: #555;
  color: #f5f5f5; /* 暗色模式下的主要文字颜色 */
}

.dark-mode .form-select {
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23f5f5f5' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e");
}

.dark-mode .dialog-close {
  color: #ccc; /* 暗色模式下的次要文字颜色 */
}

/* 暗色模式样式 */
.dark-mode .user-profile-view {
  background-color: #1a1a1a;
  color: #f5f5f5;
}

.dark-mode .header,
.dark-mode .profile-section,
.dark-mode .settings-section,
.dark-mode .features-section {
  background-color: #2d2d2d;
  color: #f5f5f5;
}

.dark-mode .header h1,
.dark-mode .profile-section h2,
.dark-mode .settings-section h2,
.dark-mode .features-section h2,
.dark-mode .info-item label,
.dark-mode .setting-item label,
.dark-mode .switch-container span.active,
.dark-mode .feature-card h3 {
  color: #f5f5f5; /* 暗色模式下的主要文字颜色 */
}

.dark-mode .settings-section h2 {
  border-bottom: 1px solid #444;
}

.dark-mode .info-item span,
.dark-mode .feature-card p,
.dark-mode select {
  color: #ccc; /* 暗色模式下的次要文字颜色 */
}

.dark-mode .info-item label,
.dark-mode .setting-item label {
  color: #ccc; /* 暗色模式下的次要文字颜色 */
}

.dark-mode .feature-card {
  background-color: #3d3d3d;
  border-color: #555;
}

.dark-mode .profile-info {
  border-top: 1px solid #444;
}

.dark-mode .avatar {
  background-color: #555;
}

.dark-mode .back-button,
.dark-mode .upload-button,
.dark-mode .save-button {
  background-color: #0066cc;
}

.dark-mode select {
  background-color: #3d3d3d;
  border-color: #555;
}

.dark-mode .opacity-control label {
  color: #f5f5f5; /* 暗色模式下的主要文字颜色 */
}

.dark-mode .personal-signature-input {
  background-color: #3d3d3d;
  border-color: #555;
  color: #f5f5f5; /* 暗色模式下的主要文字颜色 */
}

.conversation-name {
  font-weight: 500;
  margin-bottom: 5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #333; /* 添加主要文字颜色 */
}

.conversation-last-message {
  font-size: 12px;
  color: #666; /* 修改为更合适的次要文字颜色 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conversation-time {
  font-size: 12px;
  color: #999; /* 保持时间的浅色 */
}

/* 暗色模式样式 */
.dark-mode .conversation-name {
  color: #f5f5f5;
}

.dark-mode .conversation-last-message {
  color: #ccc;
}

.dark-mode .conversation-time {
  color: #ccc;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: #fff; /* 添加默认背景色 */
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
  background-color: #fff; /* 确保header有白色背景 */
}

.chat-header-info {
  display: flex;
  align-items: center;
}

.chat-header-text {
  margin-left: 15px;
}

.chat-participant-name {
  font-weight: 500;
  font-size: 16px;
  color: #333; /* 添加主要文字颜色 */
}

.chat-status {
  font-size: 12px;
  color: #666; /* 修改为更合适的次要文字颜色 */
}

.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #fff;
  display: flex;
  flex-direction: column;
}

@media (max-width: 768px) {
  .profile-info {
    flex-direction: column;
    align-items: center;
  }
  
  .features-grid {
    grid-template-columns: 1fr;
  }
  
  .header {
    flex-direction: column;
    gap: 10px;
  }
  
  .header h1 {
    text-align: center;
  }
}
</style>