<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  VideoCamera, 
  Picture, 
  Document,
  Upload,
  Plus
} from '@element-plus/icons-vue'
import apiClient, { API_ENDPOINTS } from '../services/api'
import { useUserStore } from '../stores/userStore'
import FluentEditorWrapper from '../components/FluentEditorWrapper.vue'
import type { ResVO } from '../types/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 发布类型，默认为'article'
const publishType = ref('article')

// 表单数据
const formData = ref({
  title: '',
  content: '',
  category: 'life',
  tags: [] as string[]
})

// 标签输入
const tagInput = ref({
  value: '',
  visible: false
})

// 视频相关
const videoFile = ref<File | null>(null)
const videoPreview = ref('')

// 图片相关（单张封面图片）
const coverImageFile = ref<File | null>(null)
const coverImagePreview = ref('')

// 防重复提交标志
const isSubmitting = ref(false)

// 处理路由参数
onMounted(() => {
  const type = route.query.type as string
  if (type) {
    publishType.value = type
  }
})

// 处理视频文件选择
const handleVideoSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files[0]) {
    videoFile.value = target.files[0]
    // 创建预览
    videoPreview.value = URL.createObjectURL(videoFile.value)
  }
}

// 处理封面图片选择
const handleCoverImageSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files[0]) {
    coverImageFile.value = target.files[0]
    // 创建预览
    coverImagePreview.value = URL.createObjectURL(coverImageFile.value)
  }
}

// 添加标签
const addTag = () => {
  if (tagInput.value.value && !formData.value.tags.includes(tagInput.value.value)) {
    formData.value.tags.push(tagInput.value.value)
  }
  tagInput.value.value = ''
  tagInput.value.visible = false
}

// 移除标签
const removeTag = (tag: string) => {
  formData.value.tags = formData.value.tags.filter(t => t !== tag)
}

// 显示标签输入框并自动聚焦
const showTagInput = () => {
  tagInput.value.visible = true
  // 使用 nextTick 确保 DOM 更新后再聚焦
  nextTick(() => {
    const tagInputEl = document.querySelector('.tag-input input') as HTMLInputElement
    if (tagInputEl) {
      tagInputEl.focus()
    }
  })
}

// 提取URL中的相对路径
const extractRelativePath = (fullUrl: string): string => {
  try {
    // 创建URL对象
    const url = new URL(fullUrl);
    // 返回路径部分，去掉开头的斜杠
    return url.pathname.substring(1);
  } catch (e) {
    // 如果不是有效的URL，尝试通过字符串分割获取相对路径
    const parts = fullUrl.split('/');
    // 找到包含域名的部分并提取后面的内容
    for (let i = 0; i < parts.length - 1; i++) {
      if (parts[i].includes('.')) {
        return parts.slice(i + 1).join('/');
      }
    }
    // 如果无法解析，返回原字符串
    return fullUrl;
  }
};

// 提交发布
const submitPublish = async () => {
  // 防止重复提交
  if (isSubmitting.value) {
    ElMessage.warning('正在发布中，请勿重复点击')
    return
  }

  if (!formData.value.content.trim()) {
    ElMessage.warning('请输入内容')
    return
  }

  if (publishType.value === 'video' && !videoFile.value) {
    ElMessage.warning('请选择视频文件')
    return
  }

  if (publishType.value === 'article' && !coverImageFile.value) {
    ElMessage.warning('请选择封面图片')
    return
  }

  try {
    // 设置提交标志
    isSubmitting.value = true
    
    // 构造帖子数据
    const postData: {
      userId?: string,
      username?: string,
      avatarUrl?: string,
      type: string,
      title: string,
      content: string,
      category: string,
      tags: string,
      viewCount: number,
      likeCount: number,
      commentCount: number,
      shareCount: number,
      coverUrl?: string
    } = {
      userId: userStore.getUserInfo?.userId,
      username: userStore.getUserInfo?.userName,
      // 保存头像的相对路径而不是完整URL
      avatarUrl: userStore.getUserInfo?.avatarUrl ? 
        extractRelativePath(userStore.getUserInfo.avatarUrl) : 
        undefined,
      type: publishType.value,
      title: formData.value.title,
      content: formData.value.content,
      category: formData.value.category,
      tags: JSON.stringify(formData.value.tags),
      viewCount: 0,
      likeCount: 0,
      commentCount: 0,
      shareCount: 0
    }

    // 如果是文章类型，添加封面图片URL
    if (publishType.value === 'article' && coverImageFile.value) {
      // 通过后端中转上传图片到OSS
      const formDataObj = new FormData()
      formDataObj.append('file', coverImageFile.value)
      formDataObj.append('fileType', 'cover') // 指定文件类型为cover
      
      try {
        // 调用后端上传接口
        const uploadResponse: ResVO<string> = await apiClient.post(API_ENDPOINTS.FILE_UPLOAD, formDataObj, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        
        console.log('上传响应:', uploadResponse); // 调试信息
        
        if (uploadResponse.code === 200 && uploadResponse.data) {
          // 提取相对路径部分存储到数据库
          postData.coverUrl = extractRelativePath(uploadResponse.data);
        } else {
          throw new Error(uploadResponse.msg || '上传失败')
        }
      } catch (error: any) {
        console.error('上传封面图片失败:', error)
        ElMessage.error('上传封面图片失败: ' + (error instanceof Error ? error.message : '未知错误'))
        return
      }
    }

    // 调用后端API提交发布内容
    const response: ResVO<any> = await apiClient.post(API_ENDPOINTS.POST_CREATE, postData)
    
    if (response.code === 200) {
      ElMessage.success('发布成功！')
      // 确保在下次DOM更新周期后进行跳转
      await nextTick()
      router.push('/discover')
    } else {
      ElMessage.error(response.msg || '发布失败')
    }
  } catch (error: any) {
    console.error('发布内容失败:', error)
    ElMessage.error('发布失败，请稍后重试: ' + (error.message || '未知错误'))
  } finally {
    // 重置提交标志
    isSubmitting.value = false
  }
}

// 返回发现页面
const goBack = () => {
  router.push('/discover')
}
</script>

<template>
  <div class="publish-container">
    <div class="header">
      <h2>发布内容</h2>
      <div class="header-actions">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" @click="submitPublish" :loading="isSubmitting">发布</el-button>
      </div>
    </div>

    <div class="content">
      <!-- 发布类型选择 -->
      <div class="section">
        <div class="section-title">内容类型</div>
        <div class="type-selector">
          <el-radio-group v-model="publishType">
            <el-radio-button value="video" class="type-button video-button">
              <video-camera class="icon" />
              <span class="button-text">视频</span>
            </el-radio-button>
            <el-radio-button value="article" class="type-button article-button">
              <document class="icon" />
              <span class="button-text">文章</span>
            </el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <!-- 基本信息 -->
      <div class="section">
        <div class="section-title">基本信息</div>
        <el-form :model="formData" label-position="top">
          <el-row :gutter="24" class="basic-info-row">
            <el-col :span="5">
              <el-form-item label="标题（可选）">
                <el-input 
                  v-model="formData.title" 
                  placeholder="请输入标题"
                />
              </el-form-item>
            </el-col>
            
            <el-col :span="3">
              <el-form-item label="分类">
                <el-select v-model="formData.category" placeholder="请选择分类">
                  <el-option label="生活" value="life" />
                  <el-option label="游戏" value="game" />
                  <el-option label="科技" value="tech" />
                  <el-option label="娱乐" value="entertainment" />
                  <el-option label="学习" value="study" />
                </el-select>
              </el-form-item>
            </el-col>
            
            <el-col :span="16">
              <el-form-item label="标签">
                <div class="tag-list">
                  <el-tag
                    v-for="tag in formData.tags"
                    :key="tag"
                    closable
                    @close="removeTag(tag)"
                  >
                    {{ tag }}
                  </el-tag>
                  <el-input
                    v-if="tagInput.visible"
                    v-model="tagInput.value"
                    class="tag-input"
                    size="small"
                    @keyup.enter="addTag"
                    @blur="addTag"
                  />
                  <el-button 
                    v-else 
                    class="button-new-tag" 
                    size="small" 
                    @click="showTagInput"
                  >
                    <plus class="icon" />
                    添加标签
                  </el-button>
                </div>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-form-item label="内容" class="content-item">
            <div class="editor-container full-width">
              <FluentEditorWrapper 
                v-model="formData.content"
                placeholder="请输入内容..."
              />
            </div>
          </el-form-item>
        </el-form>
      </div>

      <!-- 媒体上传 -->
      <div class="section">
        <div class="section-title">
          {{ publishType === 'video' ? '视频上传' : (publishType === 'article' ? '封面图片' : '媒体上传') }}
        </div>
        
        <!-- 视频上传 -->
        <div v-if="publishType === 'video'" class="upload-section">
          <div v-if="!videoPreview" class="upload-area">
            <input 
              type="file" 
              accept="video/*" 
              @change="handleVideoSelect" 
              style="display: none" 
              ref="videoInput"
            />
            <el-button 
              type="primary" 
              @click="$refs.videoInput.click()"
            >
              <upload class="icon" />
              选择视频文件
            </el-button>
            <div class="upload-tip">支持常见视频格式，大小不超过1GB</div>
          </div>
          
          <div v-else class="video-preview">
            <video :src="videoPreview" controls class="preview-video"></video>
            <el-button 
              @click="videoFile = null; videoPreview = ''"
              type="danger" 
              plain
            >
              删除视频
            </el-button>
          </div>
        </div>
        
        <!-- 封面图片上传（仅用于文章类型） -->
        <div v-else-if="publishType === 'article'" class="upload-section">
          <div v-if="!coverImagePreview" class="upload-area">
            <input 
              type="file" 
              accept="image/*" 
              @change="handleCoverImageSelect" 
              style="display: none" 
              ref="coverImageInput"
            />
            <el-button 
              type="primary" 
              @click="$refs.coverImageInput.click()"
            >
              <upload class="icon" />
              选择封面图片
            </el-button>
            <div class="upload-tip">支持JPG、PNG格式，选择一张作为封面图片</div>
          </div>
          
          <div v-else class="cover-image-preview-container">
            <div class="cover-image-preview">
              <img :src="coverImagePreview" alt="封面图片" />
              <el-button 
                @click="coverImageFile = null; coverImagePreview = ''"
                type="danger" 
                plain
                class="delete-button"
              >
                删除图片
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.publish-container {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
  background: var(--background-color);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: var(--card-background);
  border-radius: 12px;
  box-shadow: var(--box-shadow);
}

.header h2 {
  margin: 0;
  font-size: 24px;
  color: var(--text-primary);
}

.section {
  background: var(--card-background);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: var(--box-shadow);
}

.section-title {
  font-size: 18px;
  font-weight: 500;
  margin-bottom: 16px;
  color: var(--text-primary);
}

.type-selector {
  margin-bottom: 20px;
}

.type-button {
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
  border: 1px solid var(--border-color) !important;
  background: var(--card-background) !important;
  color: var(--text-primary) !important;
  border-radius: 8px !important;
  margin: 0 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  min-width: 120px; /* 确保按钮宽度一致 */
  text-align: center;
}

.type-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.type-button:active {
  transform: translateY(0);
}

.type-button.is-active {
  color: white !important;
  border-color: transparent !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.type-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s;
}

.type-button:hover::before {
  left: 100%;
}

.video-button.is-active {
  background: linear-gradient(135deg, #ff6b6b, #ff8e53) !important;
  border-color: #ff6b6b !important;
}

.article-button.is-active {
  background: linear-gradient(135deg, #4facfe, #00f2fe) !important;
  border-color: #4facfe !important;
}

.video-button:hover {
  border-color: #ff6b6b !important;
}

.article-button:hover {
  border-color: #4facfe !important;
}

/* 去掉内部按钮的背景 */
:deep(.el-radio-button__inner) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  padding: 12px 20px;
}

.icon {
  margin-right: 4px;
  width: 1em;
  height: 1em;
}

.button-text {
  font-weight: 500;
}

.upload-area {
  text-align: center;
  padding: 40px 20px;
  border: 2px dashed var(--border-color);
  border-radius: 6px;
  margin-bottom: 20px;
}

.upload-tip {
  margin-top: 10px;
  color: var(--text-secondary);
  font-size: 12px;
}

.video-preview {
  text-align: center;
}

.preview-video {
  max-width: 100%;
  max-height: 400px;
  margin-bottom: 10px;
}

.cover-image-preview-container {
  display: flex;
  justify-content: center;
}

.cover-image-preview {
  position: relative;
  display: inline-block;
}

.cover-image-preview img {
  max-width: 100%;
  max-height: 400px;
  border-radius: 6px;
  display: block;
}

.delete-button {
  position: absolute;
  top: 10px;
  right: 10px;
}

.basic-info-row {
  margin-bottom: 20px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.tag-input {
  flex: 1;
  min-width: 100px;
  margin-left: 10px;
  vertical-align: bottom;
}

.button-new-tag {
  margin-left: 10px;
  height: 32px;
  line-height: 30px;
  padding-top: 0;
  padding-bottom: 0;
}

.editor-container {
  height: 580px;
}


.content-item {
  height: 610px;
}

.content-item :deep(.el-form-item__content) {
  height: 580px;
  max-height: 580px;
  overflow: hidden;
  border-bottom: 1px solid #ccc;
  padding-bottom: 10px;
}

.full-width {
  width: 100%;
}

/* 暗色模式适配 */
.dark-mode .publish-container {
  background: #1a1a1a;
}

.dark-mode .header,
.dark-mode .section {
  background: #2d2d2d;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.3);
  color: #f5f5f5;
}

.dark-mode .section :deep(.el-form-item__content){
  padding-bottom: 0;
  border-bottom: 0;
  background: #aaa;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.3);
}

.dark-mode .header h2,
.dark-mode .section-title {
  color: #f5f5f5;
}

.dark-mode .upload-area {
  border-color: #444;
  color: #f5f5f5;
}

.dark-mode .section :deep(.el-form .el-row .el-col-5 .el-form-item .el-form-item__content .el-input__wrapper){
  background-color: #333 ;
}
.dark-mode .section :deep(.el-form .el-row .el-col-5 .el-form-item .el-form-item__content .el-input__inner){
  color: #f5f5f5;
}

.dark-mode .section :deep(.el-form .el-row .el-col-3 .el-form-item .el-form-item__content .el-select .el-select__wrapper){
  background-color: #333 ;
}
.dark-mode .section :deep(.el-form .el-row .el-col-3 .el-form-item .el-form-item__content .el-select .el-select__wrapper .el-select__selected-item){
  color: #f5f5f5;
}

.dark-mode .section :deep(.el-form .el-row .el-col-16 .el-form-item .el-form-item__content ){
  background-color: #2d2d2d ;
  box-shadow: none;
}
.dark-mode .section :deep(.el-form .el-row .el-col-16 .el-form-item .el-form-item__content ){
  color: #f5f5f5;
}

.dark-mode .section :deep(.el-form .el-row .el-col-16 .el-form-item .el-form-item__content .el-input__wrapper ){
  background-color: #2c2c2c ;

}
.dark-mode .section :deep(.el-form .el-row .el-col-16 .el-form-item .el-form-item__content .el-input__inner){
  color: #f5f5f5;
  height: 30px;
}


.dark-mode .tag-list .el-tag {
  background-color: #3d3d3d;
  border-color: #555;
  color: #f5f5f5;
}

.dark-mode .tag-list .el-tag .el-tag__close {
  color: #aaa;
}

.dark-mode .tag-list .el-tag .el-tag__close:hover {
  background-color: #555;
  color: #fff;
}

/* 暗色模式下的类型选择按钮样式优化 */
.dark-mode .type-button {
  background: #3d3d3d !important;
  border-color: #555 !important;
  color: #f0f0f0 !important;
}

.dark-mode .type-button:hover {
  background: #4d4d4d !important;
  border-color: #666 !important;
}

.dark-mode .type-button.is-active {
  color: white !important;
}

.dark-mode .video-button.is-active {
  background: linear-gradient(135deg, #ff6b6b, #ff8e53) !important;
  border-color: #ff6b6b !important;
  color: white !important;
}

.dark-mode .article-button.is-active {
  background: linear-gradient(135deg, #4facfe, #00f2fe) !important;
  border-color: #4facfe !important;
  color: white !important;
}

.dark-mode .icon {
  color: #f0f0f0;
}

.dark-mode .button-text {
  color: #f0f0f0;
}



.dark-mode .basic-info-row :deep(.el-form-item__label) {
  color: #ffffff;
}

.dark-mode .basic-info-row :deep(.el-form-item__content) {
  background-color: #555;
  color: #ffffff;
}


.dark-mode .basic-info-row :deep(.el-form-item__content .el-select) {
  color: #ffffff;
}

.dark-mode .button-new-tag {
  background: #3d3d3d;
  border-color: #555;
  color: #f5f5f5;
}

.dark-mode .button-new-tag:hover {
  background: #4d4d4d;
  border-color: #666;
  color: #fff;
}
</style>