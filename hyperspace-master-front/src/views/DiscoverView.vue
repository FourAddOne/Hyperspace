<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate } from 'vue-router'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Star,
  StarFilled,
  ChatDotSquare,
  Share,
  VideoPlay
} from '@element-plus/icons-vue'
import DiscoverHeader from '@/components/DiscoverHeader.vue'
import DiscoverTabs from '@/components/DiscoverTabs.vue'
import PostCard from '@/components/PostCard.vue'
import apiClient, { API_ENDPOINTS } from '../services/api'

const router = useRouter()

// 当前选中的标签
const activeTab = ref('recommend')

// 标签列表
const tabs = ref([
  { id: 'recommend', name: '推荐' },
  { id: 'video', name: '视频' },
  { id: 'article', name: '文章' },
  { id: 'guide', name: '攻略' }
])

// 帖子数据
const posts = ref([])

// 加载帖子数据
const loadPosts = async () => {
  try {
    let type = undefined;
    
    // 根据当前选中的标签设置类型参数
    switch (activeTab.value) {
      case 'video':
        type = 'video';
        break;
      case 'article':
        type = 'article';
        break;
      case 'guide':
        type = 'guide';
        break;
      default:
        type = undefined; // 推荐和其他标签不设置类型
    }
    
    const response = await apiClient.get(API_ENDPOINTS.POST_LIST, {
      params: {
        page: 1,
        size: 20,
        type: type
      }
    });
    
    if (response.code === 200) {
      posts.value = response.data;
    } else {
      ElMessage.error('加载帖子失败');
    }
  } catch (error) {
    console.error('加载帖子出错:', error);
    ElMessage.error('加载帖子出错');
  }
};

// 监听标签变化
watch(activeTab, (newTab) => {
  loadPosts();
});

// 点赞功能
const handleLike = (post: any) => {
  console.log('帖子点赞状态更新:', post)
  // 这里可以添加额外的处理逻辑，如显示提示等
  ElMessage.success(post.isLiked ? '点赞成功' : '已取消点赞')
}

// 评论功能
const handleComment = (post: any) => {
  console.log('评论帖子:', post)
  // 跳转到帖子详情页进行评论
  router.push(`/post/${post.postId}`)
}

// 分享功能
const handleShare = (post: any) => {
  console.log('分享帖子:', post)
  ElMessage.info('分享功能开发中')
}

// 查看详情
const handleViewDetail = (post: any) => {
  router.push(`/post/${post.postId}`)
}

// 处理搜索事件
const handleSearch = (keyword: string) => {
  ElMessage.info(`搜索关键词: ${keyword}`)
  // 实际项目中这里会调用API进行搜索
}

// 处理发布事件
const handlePublish = () => {
  // 发布功能已在DiscoverHeader中处理
}

// 处理从帖子详情页返回时的刷新
const handlePostUpdate = (event: CustomEvent) => {
  // 重新加载帖子数据以反映最新的浏览数、点赞数等
  loadPosts();
}

// 监听路由变化，当从其他页面返回时刷新数据
const handleRouteChange = () => {
  // 检查当前路由是否是discover页面
  if (router.currentRoute.value.name === 'discover') {
    loadPosts();
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadPosts();
  // 监听自定义事件，当从帖子详情页返回时刷新数据
  window.addEventListener('postUpdated', handlePostUpdate as EventListener);
  // 监听路由变化
  router.afterEach(handleRouteChange);
});

// 组件卸载时移除事件监听器
onUnmounted(() => {
  window.removeEventListener('postUpdated', handlePostUpdate as EventListener);
});

// 路由更新时也刷新数据
onBeforeRouteUpdate((to, from) => {
  if (to.name === 'discover') {
    loadPosts();
  }
});
</script>

<template>
  <div class="discover-container">
    <!-- 头部组件 -->
    <discover-header 
      @search="handleSearch"
      @publish="handlePublish"
    />
    
    <!-- 标签组件 -->
    <discover-tabs
      v-model="activeTab"
      :tabs="tabs"
    />
    
    <!-- 内容列表 -->
    <div class="content-list">
      <post-card
        v-for="post in posts"
        :key="post.postId"
        :post="post"
        @like="handleLike"
        @comment="handleComment"
        @share="handleShare"
        @view-detail="handleViewDetail"
      />
    </div>
    
    <!-- 科技感装饰元素 -->
    <div class="tech-elements">
      <div class="floating-orb orb-1"></div>
      <div class="floating-orb orb-2"></div>
      <div class="floating-orb orb-3"></div>
      <div class="grid-lines"></div>
    </div>
  </div>
</template>

<style scoped>
.discover-container {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
  background: var(--background-color);
  position: relative;
  isolation: isolate;
}

.content-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 24px;
  position: relative;
  z-index: 2;
}

/* 科技感装饰元素 */
.tech-elements {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
  overflow: hidden;
}

.floating-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.3;
  z-index: 1;
}

.orb-1 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #409eff, #64b5f6);
  top: 10%;
  left: 5%;
  animation: float 15s infinite ease-in-out;
}

.orb-2 {
  width: 200px;
  height: 200px;
  background: linear-gradient(135deg, #ff6b6b, #ff8e53);
  bottom: 15%;
  right: 10%;
  animation: float 12s infinite ease-in-out reverse;
}

.orb-3 {
  width: 150px;
  height: 150px;
  background: linear-gradient(135deg, #4facfe, #00f2fe);
  top: 40%;
  right: 20%;
  animation: float 10s infinite ease-in-out;
}

.grid-lines {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: 
    linear-gradient(rgba(64, 158, 255, 0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(64, 158, 255, 0.1) 1px, transparent 1px);
  background-size: 30px 30px;
  mask-image: radial-gradient(ellipse at center, rgba(0,0,0,0.1) 0%, rgba(0,0,0,0) 70%);
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0);
  }
  25% {
    transform: translate(20px, 20px);
  }
  50% {
    transform: translate(0, 40px);
  }
  75% {
    transform: translate(-20px, 20px);
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

/* 暗色模式 */
.dark-mode .discover-container {
  background: #1a1a1a;
}

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
}

/* 暗色模式下的输入框优化 */
.dark-mode .el-input__wrapper {
  background-color: #2d2d2d !important;
  box-shadow: 0 0 0 1px #444444 inset !important;
}

.dark-mode .el-input__inner {
  background-color: #2d2d2d !important;
  color: #f5f5f5 !important;
}

.dark-mode .el-input__inner::placeholder {
  color: #888888 !important;
}
</style>