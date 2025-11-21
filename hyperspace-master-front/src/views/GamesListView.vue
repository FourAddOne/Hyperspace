<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { SwitchFilled, CaretRight } from '@element-plus/icons-vue'
import apiClient, { API_ENDPOINTS } from '../services/api'
import { useUserStore } from '../stores/userStore'

const router = useRouter()
const userStore = useUserStore()

// 游戏数据
const games = ref([
  {
    id: 'snake',
    name: '贪吃蛇',
    description: '经典贪吃蛇游戏，控制小蛇吃食物变长',
    icon: '🐍',
    players: '单人',
    status: 'available',
    url:"http://slither.com/io"
  },
  {
    id: 'tetris',
    name: '俄罗斯方块',
    description: '经典的俄罗斯方块游戏',
    icon: '🧱',
    players: '单人',
    status: 'available',
    url:"http://guozhivip.com/fun/els/"
  },
  {
    id: 'chess',
    name: '中国象棋',
    description: '中国传统棋类游戏',
    icon: '格將',
    players: '双人',
    status: 'coming-soon',
    url:"http://guozhivip.com/fun/els/"
  }
])

const loading = ref(false)

// 开始游戏
const startGame = (gameurl: string) => {

  //跳转到游戏页面
  window.open(gameurl, '_blank')
}

onMounted(() => {
  // 可以在这里从API获取游戏列表
})
</script>

<template>
  <div class="games-list-container">
    <div class="header">
      <h2>游戏中心</h2>
    </div>

    <div class="content">
      <el-empty v-if="!loading && games.length === 0" description="暂无游戏" />

      <div v-else class="games-grid">
        <el-card
          v-for="game in games"
          :key="game.id"
          class="game-card"
          :class="{ 'coming-soon': game.status === 'coming-soon' }"
        >
          <div class="game-info">
            <div class="game-icon">
              {{ game.icon }}
            </div>
            <div class="game-details">
              <h3>{{ game.name }}</h3>
              <p>{{ game.description }}</p>
              <div class="game-meta">
                <span>玩家: {{ game.players }}</span>
                <span v-if="game.status === 'coming-soon'" class="status-tag">即将上线</span>
              </div>
            </div>
          </div>

          <div class="game-actions">
            <el-button
              type="primary"
              @click="startGame(game.url)"
              :disabled="game.status === 'coming-soon'"
            >
              <caret-right class="icon" />
              {{ game.status === 'coming-soon' ? '敬请期待' : '开始游戏' }}
            </el-button>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.games-list-container {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
}

.content {
  flex: 1;
}

.games-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.game-card {
  transition: transform 0.2s, box-shadow 0.2s;
}

.game-card:hover:not(.coming-soon) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.game-card.coming-soon {
  opacity: 0.7;
}

.game-info {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
}

.game-icon {
  font-size: 32px;
  margin-right: 16px;
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.game-details {
  flex: 1;
}

.game-details h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
}

.game-details p {
  margin: 0 0 12px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.4;
}

.game-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-tag {
  background-color: #f5f5f5;
  color: #999;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.game-actions {
  text-align: right;
}

.icon {
  margin-right: 4px;
}

/* 暗色模式适配 */
.dark-mode .games-list-container {
  color: #f5f5f5;
}

.dark-mode .game-card {
  background-color: #2d2d2d;
  border-color: #444;
}

.dark-mode .game-details p {
  color: #aaa;
}

.dark-mode .status-tag {
  background-color: #3d3d3d;
  color: #ccc;
}
</style>
