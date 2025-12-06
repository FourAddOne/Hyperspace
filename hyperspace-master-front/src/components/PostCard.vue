<script setup lang="ts">
import { ref } from 'vue'
import { More } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/userStore'
import { ElMessage } from 'element-plus'

// 获取用户存储
const userStore = useUserStore()

// 定义 props
const props = defineProps<{
  post: any
}>()

// 定义 emits
const emit = defineEmits<{
  (e: 'view-detail', post: any): void
}>()

// 处理内容显示，去除HTML标签并限制长度
const formatContent = (content: string) => {
  if (!content) return '';
  
  // 去除HTML标签
  const textContent = content.replace(/<[^>]*>/g, '');
  
  // 限制显示长度
  if (textContent.length > 30) {
    return textContent.substring(0, 30) + '...';
  }
  
  return textContent;
}

// 查看详情
const viewDetail = (post: any) => {
  emit('view-detail', post)
}
</script>
<template>
  <div class="post-card" @click="viewDetail(post)">
    <div class="post-header">
      <img 
        :src="post.avatarUrl || '/src/assets/logo.svg'" 
        alt="用户头像" 
        class="author-avatar"
      />
      <div class="author-info">
        <div class="author-name">{{ post.username }}</div>
        <div class="post-time">{{ new Date(post.createTime).toLocaleString() }}</div>
      </div>
      <div class="more-actions">
        <el-button circle class="more-btn">
          <more class="icon" />
        </el-button>
      </div>
    </div>

    <div class="post-content">
      <!-- 标题 -->
      <h3 v-if="post.title" class="post-title">{{ post.title }}</h3>
      
      <!-- 内容预览 -->
      <p class="post-text">{{ formatContent(post.content) }}</p>
      
      <!-- 封面图片 (针对图文类型，文章类型不显示) -->
      <div v-if="post.type === 'article' && post.coverUrl" class="cover-image-container">
        <img :src="post.coverUrl" alt="封面图片" class="cover-image" />
      </div>
      
      <!-- 图片网格 -->
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
          <img :src="media.url" :alt="`图片${index + 1}`" />
          <div 
            v-if="index === 8 && post.mediaList.length > 9" 
            class="more-images-overlay"
          >
            +{{ post.mediaList.length - 9 }}
          </div>
        </div>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="post-stats">
      <span class="stat-item">浏览 {{ post.viewCount }}</span>
      <span class="stat-item">点赞 {{ post.likeCount }}</span>
      <span class="stat-item">评论 {{ post.commentCount }}</span>
    </div>
  </div>
</template>

<style scoped>
.post-card {
  background: var(--card-background);
  border-radius: 16px;
  box-shadow: var(--box-shadow);
  padding: 20px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  border: 1px solid var(--border-color);
  position: relative;
  overflow: hidden;
}

.post-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, transparent 0%, rgba(64, 158, 255, 0.02) 100%);
  z-index: 0;
  pointer-events: none;
}

.post-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--hover-shadow);
  border-color: var(--primary-color-transparent);
}

.post-card:hover::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, #409eff, #64b5f6);
  animation: borderGlow 2s infinite;
}

.post-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  position: relative;
  z-index: 1;
}

.author-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  margin-right: 12px;
  object-fit: cover;
  border: 2px solid var(--border-color);
  transition: all 0.3s ease;
}

.post-card:hover .author-avatar {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.3);
}

.author-info {
  flex: 1;
}

.author-info .author-name {
  font-weight: 600;
  margin-bottom: 4px;
  color: var(--text-primary);
}

.author-info .post-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.more-actions {
  display: flex;
  align-items: center;
}

.more-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: transparent;
  border: none;
  color: var(--text-secondary);
  transition: all 0.3s ease;
}

.more-btn:hover {
  background: var(--background-color);
  color: var(--text-primary);
  transform: rotate(90deg);
}

.icon {
  width: 1em;
  height: 1em;
}

.post-content {
  margin-bottom: 20px;
  position: relative;
  z-index: 1;
}

.post-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 12px 0;
  color: var(--text-primary);
  line-height: 1.4;
}

.post-text {
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 16px 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.cover-image-container {
  margin: 16px 0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  position: relative;
}

.cover-image {
  width: 100%;
  max-height: 300px;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.cover-image-container:hover .cover-image {
  transform: scale(1.03);
}

.image-grid {
  display: grid;
  grid-gap: 6px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.image-grid-1 {
  grid-template-columns: 1fr;
  grid-template-rows: 200px;
}

.image-grid-2,
.image-grid-4 {
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 120px 120px;
}

.image-grid-3,
.image-grid-5,
.image-grid-6 {
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 80px 80px 80px;
}

.image-grid-7,
.image-grid-8,
.image-grid-9 {
  grid-template-columns: 1fr 1fr 1fr;
  grid-template-rows: 80px 80px 80px;
}

.image-item {
  position: relative;
  background: var(--media-background);
  overflow: hidden;
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
  background: rgba(0, 0, 0, 0.6);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
}

.post-stats {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 12px;
  font-size: 12px;
  color: var(--text-secondary);
  position: relative;
  z-index: 1;
}

.stat-item:not(:last-child)::after {
  content: "·";
  margin-left: 8px;
}

.post-actions {
  display: flex;
  justify-content: space-around;
  position: relative;
  z-index: 1;
}

.action-btn {
  flex: 1;
  text-align: center;
  color: var(--text-secondary);
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 0;
  border-radius: 8px;
  margin: 0 4px;
}

.action-btn:hover {
  color: var(--primary-color);
  background: rgba(64, 158, 255, 0.05);
  transform: translateY(-2px);
}

.action-text {
  margin-left: 6px;
  font-size: 14px;
  font-weight: 500;
}

.action-icon {
  width: 1.1em;
  height: 1.1em;
}

.liked {
  color: #f56c6c;
}

@keyframes borderGlow {
  0% {
    opacity: 0.5;
    transform: scaleX(0);
  }
  50% {
    opacity: 1;
    transform: scaleX(1);
  }
  100% {
    opacity: 0.5;
    transform: scaleX(0);
  }
}

/* 全局CSS变量 */
:root {
  --background-color: #f0f2f5;
  --card-background: #ffffff;
  --text-primary: #1a1a1a;
  --text-secondary: #666666;
  --border-color: #e4e7ed;
  --box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  --hover-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  --primary-color: #409eff;
  --primary-color-transparent: rgba(64, 158, 255, 0.3);
  --media-background: #f8f9fa;
  --tag-background: #f0f2f5;
  --tag-border: #dcdfe6;
  --tag-color: #606266;
}

/* 暗色模式适配 */
.dark-mode .post-card {
  background: #2d2d2d;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  border-color: #444;
}

.dark-mode .post-card:hover {
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.4);
  border-color: #409eff;
}

.dark-mode .author-info .author-name,
.dark-mode .post-title {
  color: #f5f5f5;
}

.dark-mode .author-info .post-time,
.dark-mode .post-stats {
  color: #909399;
}

.dark-mode .post-text {
  color: #f5f5f5;
}

.dark-mode .more-btn {
  color: #aaa;
}

.dark-mode .more-btn:hover {
  background: #3d3d3d;
  color: #f5f5f5;
}

.dark-mode .image-item {
  background: #333;
}

.dark-mode .post-stats {
  border-top: 1px solid #444;
  border-bottom: 1px solid #444;
}

.dark-mode .action-btn:hover {
  background: rgba(64, 158, 255, 0.1);
}
</style>