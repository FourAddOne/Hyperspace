<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/userStore'
import apiClient, { API_ENDPOINTS } from '../services/api'
import { getFullAvatarUrl } from '../utils/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Star,
  StarFilled,
  ChatDotSquare,
  Share,
  Picture,
  ArrowUp,
  ArrowDown
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 帖子数据
const post = ref<any>({})
const comments = ref<any[]>([])
const newComment = ref('')
const commentImageFiles = ref<File[]>([])
const commentImagePreviews = ref<string[]>([])
const commentImageUrls = ref<string[]>([]) // 存储已上传到OSS的图片URL
const likeLoading = ref(false) // 添加点赞加载状态
const loading = ref(false) // 添加页面加载状态

// 回复相关状态
const replyingTo = ref<string | null>(null)
const replyContent = ref('')

// 内容区域亮色模式状态
const contentLightMode = ref(false)

// 评论输入框引用
const commentInputRef = ref<any>(null)

// 计算属性：是否显示模式切换按钮（仅在暗色模式下显示）
const showModeToggleBtn = computed(() => {
  return userStore.getDarkMode
})

// 获取完整头像URL
const getFullAvatarUrl = (avatarUrl: string) => {
  // 如果已经是完整URL，直接返回
  if (avatarUrl && (avatarUrl.startsWith('http') || avatarUrl.startsWith('https'))) {
    return avatarUrl;
  }
  
  // 如果是相对路径，拼接基础URL
  if (avatarUrl && !avatarUrl.startsWith('/')) {
    const baseUrl = import.meta.env.VITE_OSS_DOMAIN_PREFIX || 'https://space2025.oss-cn-shanghai.aliyuncs.com/';
    return baseUrl + avatarUrl;
  }
  
  // 默认头像
  return '/src/assets/logo.svg';
}

// 切换内容区域亮色模式
const toggleContentLightMode = () => {
  contentLightMode.value = !contentLightMode.value
}

// 聚焦到评论输入框
const focusCommentInput = async () => {
  // 滚动到评论区域
  const commentsSection = document.querySelector('.comments-section');
  if (commentsSection) {
    commentsSection.scrollIntoView({ behavior: 'smooth' });
  }
  
  // 等待滚动完成后聚焦到输入框
  await nextTick();
  
  // 聚焦到评论输入框
  if (commentInputRef.value) {
    commentInputRef.value.focus();
  }
}

// 返回上一页
const goBack = () => {
  // 通知发现页面更新数据
  window.dispatchEvent(new CustomEvent('postUpdated'));
  router.back()
}

// 监听浏览器的后退事件
const handlePopState = () => {
  // 通知发现页面更新数据
  window.dispatchEvent(new CustomEvent('postUpdated'));
}

onMounted(() => {
  // 添加对浏览器后退事件的监听
  window.addEventListener('popstate', handlePopState);
  // 加载数据
  loadData();
});

onUnmounted(() => {
  // 移除对浏览器后退事件的监听
  window.removeEventListener('popstate', handlePopState);
});

// 加载所有数据
const loadData = async () => {
  await Promise.all([
    loadPostDetail(),
    loadComments()
  ]);
};

// 加载帖子详情
const loadPostDetail = async () => {
  try {
    const postId = route.params.postId as string
    // 检查postId是否存在且有效
    if (!postId || postId === 'undefined' || postId === 'null' || postId === undefined) {
      return
    }
    
    loading.value = true
    console.log('正在加载帖子详情，postId:', postId)
    
    // 构造请求URL
    const url = API_ENDPOINTS.POST_DETAIL.replace('{postId}', postId)
    console.log('请求URL:', url)
    
    const response = await apiClient.get(url)
    console.log('帖子详情响应:', response)
    
    if (response.code === 200) {
      post.value = response.data
      console.log('帖子数据:', post.value)
    } else {
      console.error('加载帖子详情失败:', response.msg)
      // 只有在不是导航离开时才显示错误消息
      if (route.params.postId && route.params.postId === postId) {
        ElMessage.error('加载帖子详情失败: ' + response.msg)
      }
    }
  } catch (error: any) {
    // 检查是否是导航离开导致的错误
    const currentPostId = route.params.postId as string
    if (!currentPostId || currentPostId === 'undefined' || currentPostId === 'null' || currentPostId === undefined) {
      return
    }
    
    console.error('加载帖子详情出错:', error)
    console.error('错误详情:', {
      message: error.message,
      stack: error.stack,
      response: error.response
    })
    // 只有在不是导航离开时才显示错误消息
    if (currentPostId && currentPostId !== 'undefined' && currentPostId !== 'null' && currentPostId !== undefined) {
      ElMessage.error('加载帖子详情出错: ' + (error.message || '未知错误'))
    }
  } finally {
    loading.value = false
  }
}

// 加载评论列表
const loadComments = async () => {
  try {
    const postId = route.params.postId as string
    // 检查postId是否存在且有效
    if (!postId || postId === 'undefined' || postId === 'null' || postId === undefined) {
      return
    }
    
    console.log('正在加载评论，postId:', postId)
    
    const response = await apiClient.get(API_ENDPOINTS.POST_COMMENTS.replace('{postId}', postId))
    console.log('评论列表响应:', response)
    
    if (response.code === 200) {
      // 处理评论数据，将回复的评论组织到对应的父评论下
      const allComments = response.data
      const parentComments = allComments.filter((comment: any) => !comment.parentId)
      const replyComments = allComments.filter((comment: any) => comment.parentId)
      
      // 将回复评论添加到对应的父评论中
      replyComments.forEach((reply: any) => {
        const parent = parentComments.find((parent: any) => parent.commentId === reply.parentId)
        if (parent) {
          if (!parent.replies) {
            parent.replies = []
          }
          parent.replies.push(reply)
        }
      })
      
      comments.value = parentComments
      console.log('评论数据:', comments.value)
    } else {
      console.error('加载评论失败:', response.msg)
      // 只有在不是导航离开时才显示错误消息
      if (route.params.postId && route.params.postId === postId) {
        ElMessage.error('加载评论失败: ' + response.msg)
      }
    }
  } catch (error: any) {
    // 检查是否是导航离开导致的错误
    const currentPostId = route.params.postId as string
    if (!currentPostId || currentPostId === 'undefined' || currentPostId === 'null' || currentPostId === undefined) {
      return
    }
    
    console.error('加载评论出错:', error)
    console.error('错误详情:', {
      message: error.message,
      stack: error.stack,
      response: error.response
    })
    // 只有在不是导航离开时才显示错误消息
    if (currentPostId && currentPostId !== 'undefined' && currentPostId !== 'null' && currentPostId !== undefined) {
      ElMessage.error('加载评论出错: ' + (error.message || '未知错误'))
    }
  }
}

// 点赞帖子
const toggleLike = async () => {
  // 检查用户是否登录
  const currentUser = userStore.getUserInfo
  if (!currentUser) {
    ElMessage.warning('请先登录')
    return
  }
  
  // 防止重复点击
  if (likeLoading.value) {
    return
  }
  
  likeLoading.value = true
  
  try {
    const response = await apiClient.post(
      `${API_ENDPOINTS.POST_LIKE}/${post.value.postId}`,
      {
        userId: currentUser.userId,
        username: currentUser.userName
      }
    )
    
    if (response.code === 200) {
      // 实时更新UI上的点赞数和状态
      if (post.value.isLiked) {
        // 取消点赞
        post.value.likeCount -= 1
        post.value.isLiked = false
      } else {
        // 点赞
        post.value.likeCount += 1
        post.value.isLiked = true
      }
      
      // 根据操作前的状态显示不同的消息
      if (post.value.isLiked) {
        ElMessage.success('点赞成功')
      } else {
        ElMessage.success('已取消点赞')
      }
      
      // 不再重新加载帖子详情，因为我们已经实时更新了UI状态
      // 这样可以避免页面出现刷新的感觉
    } else {
      ElMessage.error('点赞操作失败: ' + response.msg)
    }
  } catch (error: any) {
    console.error('点赞操作出错:', error)
    ElMessage.error('点赞操作出错: ' + (error.message || '未知错误'))
  } finally {
    likeLoading.value = false
  }
}

// 点赞评论
const likeComment = async (comment: any) => {
  // 检查用户是否登录
  const currentUser = userStore.getUserInfo
  if (!currentUser) {
    ElMessage.warning('请先登录')
    return
  }
  
  // 防止重复点击
  if (comment.likeLoading) {
    return
  }
  
  comment.likeLoading = true
  
  try {
    const response = await apiClient.post(
      `/posts/comment/like/${comment.commentId}`,
      {
        userId: currentUser.userId,
        username: currentUser.userName
      }
    )
    
    if (response.code === 200) {
      // 实时更新UI上的点赞数和状态
      if (comment.isLiked) {
        // 取消点赞
        comment.likeCount -= 1
        comment.isLiked = false
      } else {
        // 点赞
        comment.likeCount += 1
        comment.isLiked = true
      }
      
      // 根据操作前的状态显示不同的消息
      if (comment.isLiked) {
        ElMessage.success('点赞成功')
      } else {
        ElMessage.success('已取消点赞')
      }
    } else if (response.code === 500 && response.msg.includes("操作过于频繁")) {
      // 特殊处理频率限制的情况
      ElMessage.warning('操作过于频繁，请稍后重试')
    } else {
      ElMessage.error('点赞操作失败: ' + response.msg)
    }
  } catch (error: any) {
    console.error('点赞评论出错:', error)
    ElMessage.error('点赞评论出错: ' + (error.message || '未知错误'))
  } finally {
    comment.likeLoading = false
  }
}

// 回复评论
const replyToComment = async (comment: any, replyContent: string) => {
  console.log('回复评论:', comment, replyContent)
  
  // 检查用户是否登录
  const currentUser = userStore.getUserInfo
  if (!currentUser) {
    ElMessage.warning('请先登录')
    return
  }
  
  if (!replyContent.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  
  try {
    const response = await apiClient.post(
      `/posts/comment/reply/${comment.commentId}`,
      {
        userId: currentUser.userId,
        username: currentUser.userName,
        content: replyContent,
        avatarUrl: currentUser.avatarUrl
      }
    )
    
    if (response.code === 200) {
      ElMessage.success('回复成功')
      
      // 重新加载评论以更新缓存
      loadComments()
    } else {
      ElMessage.error('回复失败: ' + response.msg)
    }
  } catch (error: any) {
    console.error('回复评论出错:', error)
    ElMessage.error('回复评论出错: ' + (error.message || '未知错误'))
  }
}

// 触发图片选择
const triggerImageSelect = () => {
  imageInput.value?.click()
}

// 处理图片选择
const imageInput = ref()
const handleImageSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files) {
    for (let i = 0; i < target.files.length; i++) {
      const file = target.files[i]
      if (commentImageFiles.value.length < 9) {
        commentImageFiles.value.push(file)
        commentImagePreviews.value.push(URL.createObjectURL(file))
        
        // 上传图片到服务器
        try {
          const formData = new FormData()
          formData.append('file', file)
          
          const response = await apiClient.post(
            `/posts/comment/upload-image`,
            formData,
            {
              headers: {
                'Content-Type': 'multipart/form-data'
              }
            }
          )
          
          if (response.code === 200) {
            // 将上传成功的图片URL保存起来
            commentImageUrls.value.push(response.data)
          } else {
            ElMessage.error('图片上传失败: ' + response.msg)
            // 移除预览图
            const index = commentImageFiles.value.indexOf(file)
            if (index > -1) {
              commentImageFiles.value.splice(index, 1)
              commentImagePreviews.value.splice(index, 1)
            }
          }
        } catch (error: any) {
          ElMessage.error('图片上传出错: ' + (error.message || '未知错误'))
          // 移除预览图
          const index = commentImageFiles.value.indexOf(file)
          if (index > -1) {
            commentImageFiles.value.splice(index, 1)
            commentImagePreviews.value.splice(index, 1)
          }
        }
      }
    }
  }
}

// 移除评论图片
const removeCommentImage = (index: number) => {
  commentImageFiles.value.splice(index, 1)
  commentImagePreviews.value.splice(index, 1)
  if (commentImageUrls.value.length > index) {
    commentImageUrls.value.splice(index, 1)
  }
}

// 发布评论
const publishComment = async () => {
  if (!newComment.value.trim() && commentImageUrls.value.length === 0) {
    ElMessage.warning('请输入评论内容或选择图片')
    return
  }
  
  try {
    const postId = route.params.postId as string
    
    // 构造评论数据，包含图片URL
    const commentData: any = {
      userId: userStore.getUserInfo?.userId,
      username: userStore.getUserInfo?.userName,
      avatarUrl: userStore.getUserInfo?.avatarUrl,
      content: newComment.value
    }
    
    // 如果有上传的图片，则添加到imageUrls字段
    if (commentImageUrls.value.length > 0) {
      commentData.imageUrls = JSON.stringify(commentImageUrls.value)
    }
    
    const response = await apiClient.post(
      API_ENDPOINTS.POST_COMMENT_CREATE.replace('{postId}', postId),
      commentData
    )
    
    if (response.code === 200) {
      ElMessage.success('评论成功')
      newComment.value = ''
      commentImageFiles.value = []
      commentImagePreviews.value = []
      commentImageUrls.value = []
      // 重新加载评论
      loadComments()
    } else {
      ElMessage.error('评论失败: ' + response.msg)
    }
  } catch (error) {
    console.error('评论出错:', error)
    ElMessage.error('评论出错')
  }
}

// 显示回复表单
const showReplyForm = (comment: any) => {
  replyingTo.value = comment.commentId
  replyContent.value = ''
}

// 取消回复
const cancelReply = () => {
  replyingTo.value = null
  replyContent.value = ''
}

// 提交回复
const submitReply = async (comment: any) => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  
  await replyToComment(comment, replyContent.value)
  cancelReply()
}

// 切换回复展开/收起
const toggleReplies = (comment: any) => {
  comment.showReplies = !comment.showReplies
}

// 计算回复数量
const getReplyCount = (comment: any) => {
  // 这里应该从后端获取实际的回复数量
  // 目前我们返回一个默认值
  return comment.replyCount || 0;
};

// 监听路由变化，当postId改变时重新加载数据
watch(() => route.params.postId, (newPostId, oldPostId) => {
  if (newPostId !== oldPostId) {
    // 重置数据
    post.value = {}
    comments.value = []
    // 重新加载数据
    loadPostDetail()
    loadComments()
  }
})

// 组件挂载时加载数据
onMounted(() => {
  console.log('PostDetailView mounted, route params:', route.params)
  loadPostDetail()
  loadComments()
})

</script>

<template>
  <div class="post-detail-container">
    <!-- 加载动画 -->
    <LoadingView :loading="loading"/>

    <!-- 头部 -->
    <div class="header">
      <el-button circle class="back-btn" @click="goBack">
        <arrow-left class="icon"/>
      </el-button>
      <h2>帖子详情</h2>
      <div class="header-actions">
        <el-button 
          v-if="userStore.getDarkMode"
          circle 
          class="mode-toggle-btn" 
          @click="toggleContentLightMode"
          :title="contentLightMode ? '切换回暗色模式' : '切换为亮色模式'"
        >
          <sunny v-if="contentLightMode" class="icon"/>
          <moon v-else class="icon"/>
        </el-button>
      </div>
    </div>

    <div class="content-wrapper">
      <!-- 左侧：帖子详情 -->
      <div class="post-detail" :class="{ 'light-content': contentLightMode }">
        <div class="post-content">
          <!-- 作者信息 -->
          <div class="post-header">
            <img
                :src="getFullAvatarUrl(post.avatarUrl)"
                alt="用户头像"
                class="author-avatar"
            />
            <div class="author-info">
              <div class="author-name">{{ post.username }}</div>
              <div class="post-time">{{ new Date(post.createTime).toLocaleString() }}</div>
            </div>
          </div>

          <!-- 帖子内容 -->
          <div class="post-body">
            <h3 v-if="post.title" class="post-title centered-title">{{ post.title }}</h3>
            <!-- 文章内容使用v-html渲染，因为可能是富文本 -->
            <div class="post-text" v-html="post.content"></div>

            <!-- 帖子图片 -->
            <div
                v-if="post.mediaList && post.mediaList.length > 0"
                class="image-grid"
                :class="`image-grid-${Math.min(post.mediaList.length, 9)}`"
            >
              <div
                  v-for="(media, index) in post.mediaList.slice(0, 9)"
                  :key="index"
                  class="image-item"
              >
                <img :src="media.url" :alt="`图片${index + 1}`"/>
                <div
                    v-if="index === 8 && post.mediaList.length > 9"
                    class="more-images-overlay"
                >
                  +{{ post.mediaList.length - 9 }}
                </div>
              </div>
            </div>

            <!-- 视频 -->
            <div v-if="post.type === 'video' && post.videoUrl" class="video-container">
              <video :src="post.videoUrl" controls class="post-video"></video>
            </div>
          </div>

          <!-- 帖子统计 -->
          <div class="post-stats">
            <div class="stat-item">
              <span class="stat-label">浏览</span>
              <span class="stat-value">{{ post.viewCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">点赞</span>
              <span class="stat-value">{{ post.likeCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">评论</span>
              <span class="stat-value">{{ post.commentCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">转发</span>
              <span class="stat-value">{{ post.shareCount }}</span>
            </div>
          </div>

          <!-- 帖子操作 -->
          <div class="post-actions">
            <el-button type="text" class="action-btn" @click="toggleLike">
              <star-filled v-if="post.isLiked" class="action-icon liked"/>
              <star v-else class="action-icon"/>
              <span class="action-text">点赞</span>
            </el-button>
            <el-button type="text" class="action-btn" @click="focusCommentInput">
              <chat-dot-square class="action-icon"/>
              <span class="action-text">评论</span>
            </el-button>
            <el-button type="text" class="action-btn">
              <share class="action-icon"/>
              <span class="action-text">转发</span>
            </el-button>
          </div>
        </div>
      </div>

      <!-- 右侧：评论区域 -->
      <div class="comments-section">
        <div class="comments-header">
          <h3>评论 ({{ comments.length }})</h3>
        </div>

        <!-- 评论列表 -->
        <div class="comments-list">
          <div
              v-for="comment in comments"
              :key="comment.commentId"
              class="comment-item"
          >
            <img
                :src="getFullAvatarUrl(comment.avatarUrl)"
                alt="用户头像"
                class="comment-avatar"
            />
            <div class="comment-content">
              <div class="comment-header">
                <div class="comment-author">{{ comment.username }}</div>
                <div class="comment-time">{{ new Date(comment.createTime).toLocaleString() }}</div>
              </div>
              <!-- 优先显示图片，再显示文字内容 -->
              <div
                  v-if="comment.imageUrls && comment.imageUrls.length > 0"
                  class="comment-images"
              >
                <img
                    v-for="(imageUrl, index) in JSON.parse(comment.imageUrls)"
                    :key="index"
                    :src="imageUrl"
                    :alt="`评论图片${index + 1}`"
                    class="comment-image"
                />
              </div>
              <p v-if="comment.content" class="comment-text">{{ comment.content }}</p>

              <div class="comment-actions">
                <el-button type="text" size="small" @click="likeComment(comment)">
                  <star-filled v-if="comment.isLiked" class="action-icon liked small"/>
                  <star v-else class="action-icon small"/>
                  <span class="action-text">{{ comment.likeCount || 0 }}</span>
                </el-button>
                <el-button type="text" size="small" @click="showReplyForm(comment)">
                  <span class="action-text">回复</span>
                </el-button>
              </div>
              
              <!-- 回复表单 -->
              <div v-if="replyingTo === comment.commentId" class="reply-form">
                <el-input
                    v-model="replyContent"
                    type="textarea"
                    placeholder="请输入回复内容..."
                    :rows="2"
                    resize="none"
                />
                <div class="reply-actions">
                  <el-button size="small" @click="cancelReply">取消</el-button>
                  <el-button type="primary" size="small" @click="submitReply(comment)">回复</el-button>
                </div>
              </div>
              
              <!-- 回复列表 -->
              <div v-if="comment.replies && comment.replies.length > 0" class="replies-section">
                <div class="replies-toggle" @click="toggleReplies(comment)">
                  {{ comment.showReplies ? '收起回复' : `展开 ${comment.replies.length} 条回复` }}
                  <el-icon v-if="comment.showReplies"><ArrowUp /></el-icon>
                  <el-icon v-else><ArrowDown /></el-icon>
                </div>
                
                <div v-show="comment.showReplies" class="replies-list">
                  <div
                      v-for="reply in comment.replies"
                      :key="reply.commentId"
                      class="reply-item"
                  >
                    <img
                        :src="getFullAvatarUrl(reply.avatarUrl)"
                        alt="用户头像"
                        class="reply-avatar"
                    />
                    <div class="reply-content">
                      <div class="reply-header">
                        <div class="reply-author">{{ reply.username }}</div>
                        <div class="reply-time">{{ new Date(reply.createTime).toLocaleString() }}</div>
                      </div>
                      <p class="reply-text">
                        <span v-if="reply.replyToUsername" class="reply-to">@{{ reply.replyToUsername }}</span>
                        {{ reply.content }}
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 发布评论 -->
        <div class="comment-form">
          <div class="form-header">
            <img
                :src="userStore.getUserInfo?.avatarUrl || '/src/assets/logo.svg'"
                alt="我的头像"
                class="my-avatar"
            />
            <div class="form-content">
              <el-input
                  ref="commentInputRef"
                  v-model="newComment"
                  type="textarea"
                  placeholder="说点什么..."
                  :rows="3"
                  resize="none"
                />
              <div class="image-preview" v-if="commentImagePreviews.length > 0">
                <div
                    v-for="(preview, index) in commentImagePreviews"
                    :key="index"
                    class="preview-item"
                >
                  <img :src="preview" alt="预览图"/>
                  <el-button
                      circle
                      size="small"
                      class="remove-btn"
                      @click="removeCommentImage(index)"
                  >
                    X
                  </el-button>
                </div>
              </div>
              <div class="form-actions">
                <input
                    type="file"
                    accept="image/*"
                    multiple
                    @change="handleImageSelect"
                    style="display: none"
                    ref="imageInput"
                />
                <el-button
                    type="text"
                    @click="triggerImageSelect"
                >
                  <picture class="icon"/>
                  图片
                </el-button>
                <el-button
                    type="primary"
                    size="small"
                    @click="publishComment"
                >
                  发布
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.post-detail-container {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
  background: var(--background-color);
  position: relative;
}

.post-detail-container::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 100%;
  background: radial-gradient(circle at 10% 20%, rgba(102, 126, 234, 0.05) 0%, transparent 20%),
  radial-gradient(circle at 90% 80%, rgba(118, 75, 162, 0.05) 0%, transparent 20%);
  pointer-events: none;
  z-index: -1;
}

.header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: var(--card-background);
  border-radius: 16px;
  box-shadow: var(--box-shadow);
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.header-actions {
  margin-left: auto;
  display: flex;
  gap: 10px;
}

.mode-toggle-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
}

.mode-toggle-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.mode-toggle-btn .icon {
  width: 1.3em;
  height: 1.3em;
  color: white;
}

.header::before {
  content: "";
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent, rgba(255, 255, 255, 0.1), transparent);
  transform: rotate(45deg);
  transition: all 0.5s ease;
}

.header:hover::before {
  transform: rotate(45deg) translate(20%, 20%);
}

.back-btn {
  margin-right: 16px;
  width: 40px;
  height: 35px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
}

.back-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.back-btn .icon {
  width: 1.3em;
  height: 1.3em;
  color: white;
}

.header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  position: relative;
}

.content-wrapper {
  display: flex;
  gap: 24px;
  position: relative;
}

/* 左侧帖子详情 */
.post-detail {
  flex: 2;
  background: var(--card-background);
  border-radius: 16px;
  box-shadow: var(--box-shadow);
  padding: 24px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.post-detail.light-content {
  background: #ffffff !important;
  color: #333333 !important;
}

.dark-mode .post-detail.light-content {
  background: #ffffff !important;
  color: #333333 !important;
}

.post-detail.light-content .post-text,
.post-detail.light-content .author-info .author-name,
.post-detail.light-content .post-time,
.post-detail.light-content .stat-label,
.post-detail.light-content .action-text,
.post-detail.light-content .comment-text,
.post-detail.light-content .reply-text {
  color: #333333 !important;
}

.dark-mode .post-detail.light-content .post-text,
.dark-mode .post-detail.light-content .author-info .author-name,
.dark-mode .post-detail.light-content .post-time,
.dark-mode .post-detail.light-content .stat-label,
.dark-mode .post-detail.light-content .action-text,
.dark-mode .post-detail.light-content .comment-text,
.dark-mode .post-detail.light-content .reply-text {
  color: #333333 !important;
}

.post-detail.light-content .post-stats,
.post-detail.light-content .post-header,
.post-detail.light-content .comment-item,
.post-detail.light-content .post-actions {
  border-color: #e0e0e0 !important;
}

.dark-mode .post-detail.light-content .post-stats,
.dark-mode .post-detail.light-content .post-header,
.dark-mode .post-detail.light-content .comment-item,
.dark-mode .post-detail.light-content .post-actions {
  border-color: #e0e0e0 !important;
}

.post-detail.light-content .stat-value {
  background: linear-gradient(135deg, #667eea, #764ba2) !important;
  -webkit-background-clip: text !important;
  -webkit-text-fill-color: transparent !important;
  background-clip: text !important;
}

.dark-mode .post-detail.light-content .stat-value {
  background: linear-gradient(135deg, #667eea, #764ba2) !important;
  -webkit-background-clip: text !important;
  -webkit-text-fill-color: transparent !important;
  background-clip: text !important;
}

.post-detail.light-content .action-btn,
.post-detail.light-content .comment-actions .el-button {
  color: #666666 !important;
}

.dark-mode .post-detail.light-content .action-btn,
.dark-mode .post-detail.light-content .comment-actions .el-button {
  color: #666666 !important;
}

.post-detail.light-content .action-btn:hover,
.post-detail.light-content .comment-actions .el-button:hover {
  color: #667eea !important;
}

.dark-mode .post-detail.light-content .action-btn:hover,
.dark-mode .post-detail.light-content .comment-actions .el-button:hover {
  color: #667eea !important;
}

.post-detail.light-content .image-item {
  background: #f5f5f5 !important;
}

.dark-mode .post-detail.light-content .image-item {
  background: #f5f5f5 !important;
}

.post-detail.light-content :deep(.el-textarea__inner) {
  background: #ffffff !important;
  border-color: #e0e0e0 !important;
  color: #333333 !important;
}

.dark-mode .post-detail.light-content :deep(.el-textarea__inner) {
  background: #ffffff !important;
  border-color: #e0e0e0 !important;
  color: #333333 !important;
}

.post-detail.light-content :deep(.el-textarea__inner:focus) {
  border-color: #667eea !important;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2) !important;
}

.dark-mode .post-detail.light-content :deep(.el-textarea__inner:focus) {
  border-color: #667eea !important;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2) !important;
}

.post-detail.light-content .reply-form {
  background: #f5f5f5 !important;
}

.dark-mode .post-detail.light-content .reply-form {
  background: #f5f5f5 !important;
}

.post-detail.light-content .replies-section {
  border-top: 1px dashed #e0e0e0 !important;
}

.dark-mode .post-detail.light-content .replies-section {
  border-top: 1px dashed #e0e0e0 !important;
}

.post-detail::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  transition: all 0.3s ease;
}

.post-detail:hover::before {
  height: 6px;
}

.post-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
  position: relative;
}

.post-header::after {
  content: "";
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 60px;
  height: 2px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  border-radius: 1px;
  transition: width 0.3s ease;
}

.post-header:hover::after {
  width: 100px;
}

.author-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  margin-right: 16px;
  object-fit: cover;
  border: 3px solid transparent;
  background: linear-gradient(white, white) padding-box,
  linear-gradient(135deg, #667eea, #764ba2) border-box;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.author-avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.3);
}

.author-info .author-name {
  font-weight: 600;
  font-size: 18px;
  margin-bottom: 6px;
  color: var(--text-primary);
  position: relative;
  display: inline-block;
}

.author-info .author-name::after {
  content: "";
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  transition: width 0.3s ease;
}

.author-info .author-name:hover::after {
  width: 100%;
}

.author-info .post-time {
  font-size: 14px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
}

.author-info .post-time::before {
  content: "•";
  margin: 0 8px;
  color: var(--text-secondary);
}

.post-text {
  color: var(--text-primary);
  font-size: 16px;
  line-height: 1.8;
  margin-bottom: 24px;
  position: relative;
}

.post-text::selection {
  background: rgba(102, 126, 234, 0.3);
}

.image-grid {
  display: grid;
  grid-gap: 8px;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  position: relative;
}

.image-grid-1 {
  grid-template-columns: 1fr;
  grid-template-rows: 300px;
}

.image-grid-2,
.image-grid-4 {
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 180px 180px;
}

.image-grid-3,
.image-grid-5,
.image-grid-6 {
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 120px 120px 120px;
}

.image-grid-7,
.image-grid-8,
.image-grid-9 {
  grid-template-columns: 1fr 1fr 1fr;
  grid-template-rows: 120px 120px 120px;
}

.image-item {
  position: relative;
  background: var(--media-background);
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.image-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.image-item:hover img {
  transform: scale(1.05);
}

.more-images-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(45deg, rgba(102, 126, 234, 0.8), rgba(118, 75, 162, 0.8));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
  border-radius: 8px;
  backdrop-filter: blur(4px);
}

.post-stats {
  display: flex;
  justify-content: space-around;
  padding: 20px 0;
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
  margin: 24px 0;
  position: relative;
  overflow: hidden;
}

.post-stats::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, #667eea, #764ba2, transparent);
}

.stat-item {
  text-align: center;
  position: relative;
  padding: 10px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.stat-item:hover {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
  transform: translateY(-2px);
}

.stat-label {
  display: block;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 6px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-value {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.post-actions {
  display: flex;
  justify-content: space-around;
  padding: 20px 0;
}

.action-btn {
  flex: 1;
  text-align: center;
  color: var(--text-secondary);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  border-radius: 12px;
  margin: 0 8px;
  position: relative;
  overflow: hidden;
  border: 1px solid transparent;
}

.action-btn::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: -1;
}

.action-btn:hover {
  color: #667eea;
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.2);
  border-color: rgba(102, 126, 234, 0.3);
}

.action-btn:hover::before {
  opacity: 1;
}

.action-btn:hover .action-icon {
  transform: scale(1.2);
}

.action-btn:hover .action-text {
  font-weight: 500;
}

.action-text {
  margin-left: 8px;
  font-size: 15px;
  font-weight: 400;
  transition: all 0.3s ease;
}

.action-icon {
  width: 1.2em;
  height: 1.2em;
  transition: all 0.3s ease;
}

.liked {
  color: #f56c6c;
  text-shadow: 0 0 8px rgba(245, 108, 108, 0.3);
}

.small {
  font-size: 0.9em;
}

/* 居中显示的文章标题 */
.centered-title {
  text-align: center;
  font-size: 32px;
  font-weight: 800;
  margin: 24px 0;
  color: var(--text-primary);
  position: relative;
  padding: 20px 0;
}

.centered-title::before,
.centered-title::after {
  content: "";
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100px;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  border-radius: 2px;
}

.centered-title::after {
  content: "";
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100px;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  border-radius: 2px;
}

/* 右侧评论区域 */
.comments-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--card-background);
  border-radius: 16px;
  box-shadow: var(--box-shadow);
  padding: 24px;
  max-height: calc(100vh - 120px);
  position: sticky;
  top: 20px;
  align-self: flex-start;
  overflow: hidden;
}

.comments-section::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
}

.comments-header h3 {
  margin: 0 0 24px 0;
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 600;
  position: relative;
  display: inline-block;
}

.comments-header h3::after {
  content: "";
  position: absolute;
  bottom: -8px;
  left: 0;
  width: 40px;
  height: 3px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  border-radius: 1.5px;
}

.comments-list {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 24px;
  scrollbar-width: thin;
  scrollbar-color: #667eea transparent;
}

.comments-list::-webkit-scrollbar {
  width: 6px;
}

.comments-list::-webkit-scrollbar-track {
  background: transparent;
}

.comments-list::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 3px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid var(--border-color);
}

.comment-item:last-child {
  border-bottom: none;
}

/* 回复的评论添加特殊样式 */
.comment-item.reply-comment {
  padding-left: 30px;
  border-left: 3px solid var(--primary-color-transparent);
  background-color: var(--comment-bg);
  border-radius: 8px;
  margin-bottom: 8px;
}

.comment-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.comment-author {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
}

.comment-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.comment-text {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.5;
  margin-bottom: 12px;
  white-space: pre-wrap;
}

.comment-images {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.comment-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
}

.comment-actions {
  display: flex;
  gap: 16px;
}

.action-text {
  margin-left: 4px;
  font-size: 13px;
  color: var(--text-secondary);
}

.action-icon {
  width: 16px;
  height: 16px;
}

.liked {
  color: #f56c6c;
}

/* 评论表单 */
.comment-form {
  border-top: 1px solid var(--border-color);
  padding-top: 24px;
}

.form-header {
  display: flex;
}

.my-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  margin-right: 16px;
  object-fit: cover;
  border: 2px solid transparent;
  background: linear-gradient(white, white) padding-box,
  linear-gradient(135deg, #667eea, #764ba2) border-box;
}

.form-content {
  flex: 1;
}

:deep(.el-textarea__inner) {
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: var(--card-background);
  color: var(--text-primary);
  transition: all 0.3s ease;
}

:deep(.el-textarea__inner:focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.image-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 16px 0;
}

.preview-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
}

.preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: all 0.3s ease;
}

.preview-item:hover img {
  transform: scale(1.1);
}

.remove-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  font-size: 12px;
  padding: 0;
  background: #f56c6c;
  border: 2px solid white;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

.remove-btn:hover {
  background: #ff4d4f;
  transform: scale(1.1);
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  border-radius: 8px;
  padding: 10px 24px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

/* 回复相关样式 */
.reply-form {
  margin-top: 12px;
  padding: 12px;
  background: var(--comment-bg);
  border-radius: 8px;
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.replies-section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--border-color);
}

.replies-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px 0;
}

.replies-toggle:hover {
  color: var(--primary-color);
}

.replies-list {
  margin-top: 12px;
}

.reply-item {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  padding: 8px;
  background: var(--comment-bg);
  border-radius: 8px;
  border-left: 3px solid var(--primary-color-transparent);
}

.reply-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.reply-content {
  flex: 1;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.reply-author {
  font-weight: 500;
  font-size: 13px;
  color: var(--text-primary);
}

.reply-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.reply-text {
  font-size: 13px;
  color: var(--text-primary);
  line-height: 1.4;
}

.reply-to {
  color: var(--primary-color);
  margin-right: 4px;
  font-weight: 500;
}

/* 暗色模式适配 */
.dark-mode :root {
  --background-color: #121212;
  --card-background: #1e1e1e;
  --text-primary: #f5f5f5;
  --text-secondary: #aaaaaa;
  --border-color: #333333;
  --box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  --hover-shadow: 0 8px 30px rgba(0, 0, 0, 0.4);
  --media-background: #2d2d2d;
  --tag-background: #2d2d2d;
  --tag-border: #444444;
  --tag-color: #cccccc;
  --comment-bg: #2d2d2d;
}

.dark-mode .post-detail-container {
  background: #1a1a1a;
}

.dark-mode .post-detail-container::before {
  background: radial-gradient(circle at 10% 20%, rgba(102, 126, 234, 0.1) 0%, transparent 20%),
  radial-gradient(circle at 90% 80%, rgba(118, 75, 162, 0.1) 0%, transparent 20%);
}

.dark-mode .header,
.dark-mode .post-detail,
.dark-mode .comments-section {
  background: linear-gradient(145deg, #2d2d2d, #1f1f1f);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.dark-mode .author-info .author-name,
.dark-mode .comment-author,
.dark-mode .comments-header h3 {
  color: #f0f0f0;
}

.dark-mode .author-info .post-time,
.dark-mode .comment-time,
.dark-mode .stat-label {
  color: #aaa;
}

.dark-mode .post-text,
.dark-mode .comment-text {
  color: #e0e0e0;
}

.dark-mode .post-stats,
.dark-mode .comment-item {
  border-top: 1px solid #444;
  border-bottom: 1px solid #444;
}

.dark-mode .comments-section {
  border-top: 1px solid #444;
}

.dark-mode .image-item {
  background: #333;
}

.dark-mode .action-btn,
.dark-mode .comment-actions .el-button {
  color: #bbb;
}

.dark-mode .action-btn:hover,
.dark-mode .comment-actions .el-button:hover {
  color: #667eea;
}

.dark-mode .stat-value {
  background: linear-gradient(135deg, #8a9bff, #9d77d0);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.dark-mode :deep(.el-textarea__inner) {
  background: #2d2d2d;
  border-color: #444;
  color: #f0f0f0;
}

.dark-mode :deep(.el-textarea__inner:focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);
}

.dark-mode .post-detail.light-content .centered-title {
  color: #333333 !important;
}

.dark-mode .post-detail.light-content .centered-title::before,
.dark-mode .post-detail.light-content .centered-title::after {
  background: linear-gradient(90deg, #667eea, #764ba2) !important;
}

/* 科技感光效效果 */
@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(102, 126, 234, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(102, 126, 234, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(102, 126, 234, 0);
  }
}

.tech-glow {
  animation: pulse 2s infinite;
}
</style>