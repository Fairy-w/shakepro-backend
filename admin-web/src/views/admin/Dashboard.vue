<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adminApi, type DashboardStats } from '@/api/admin'

const stats = ref<DashboardStats | null>(null)
const loading = ref(false)

const statCards = [
  { key: 'totalUsers', label: '用户总数', tone: 'cyan' },
  { key: 'totalAdmins', label: '管理员', tone: 'amber' },
  { key: 'totalCocktails', label: '鸡尾酒条目', tone: 'blue' },
  { key: 'totalMaterials', label: '材料条目', tone: 'cyan' },
  { key: 'totalFavorites', label: '收藏记录', tone: 'amber' },
  { key: 'totalAiCocktailFavorites', label: 'AI 配方收藏', tone: 'blue' },
  { key: 'totalFiles', label: '文件记录', tone: 'blue' },
] as const

async function loadDashboard() {
  loading.value = true
  try {
    stats.value = await adminApi.getDashboard()
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <section class="dashboard-page">
    <div class="page-head">
      <div>
        <h1 class="page-title">后台总览</h1>
        <p class="page-subtitle">用统一的深海控制台视角查看用户、内容、素材和 AI 收藏规模，答辩时也更容易讲清楚整套系统的数据闭环。</p>
      </div>
      <button class="button-primary" :disabled="loading" @click="loadDashboard">
        {{ loading ? '刷新中...' : '刷新数据' }}
      </button>
    </div>

    <div class="stat-grid">
      <article v-for="item in statCards" :key="item.key" class="stat-card card" :class="item.tone">
        <span>{{ item.label }}</span>
        <strong>{{ stats ? stats[item.key] : '--' }}</strong>
      </article>
    </div>

    <div class="insight-grid">
      <article class="insight-panel card">
        <p class="insight-tag">CONTROL NOTES</p>
        <h3>当前控制台已经完成的重点</h3>
        <ul>
          <li>后台路由、登录和导航已经统一切到深海调酒台风格。</li>
          <li>用户、收藏、材料、鸡尾酒和配方采集工作台共用同一套视觉系统。</li>
          <li>AI 内容生产流程现在能在后台用流水线方式完整演示。</li>
        </ul>
      </article>

      <article class="insight-panel accent card">
        <p class="insight-tag">NEXT STEP</p>
        <h3>下一步可以继续补的能力</h3>
        <ul>
          <li>操作日志和审核记录</li>
          <li>Banner / 分类配置</li>
          <li>AI 调用统计和运营分析</li>
        </ul>
      </article>
    </div>
  </section>
</template>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 18px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 22px;
  min-height: 160px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background: linear-gradient(180deg, rgba(11, 29, 46, 0.92), rgba(7, 18, 31, 0.94));
}

.stat-card span {
  color: var(--ink-600);
  font-weight: 700;
}

.stat-card strong {
  font-size: clamp(2rem, 4vw, 3.4rem);
  letter-spacing: -0.06em;
}

.stat-card.cyan {
  border-color: rgba(72, 215, 255, 0.18);
}

.stat-card.amber {
  border-color: rgba(255, 182, 72, 0.18);
}

.stat-card.blue {
  border-color: rgba(76, 111, 255, 0.18);
}

.insight-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 16px;
}

.insight-panel {
  padding: 24px;
}

.insight-panel h3 {
  font-size: 1.5rem;
  letter-spacing: -0.04em;
  margin: 10px 0 16px;
}

.insight-panel ul {
  display: grid;
  gap: 12px;
  color: var(--ink-800);
  list-style: disc;
  padding-left: 18px;
}

.insight-tag {
  color: var(--primary);
  letter-spacing: 0.2em;
  font-size: 0.72rem;
}

.accent {
  background: linear-gradient(180deg, rgba(17, 39, 60, 0.96), rgba(9, 24, 39, 0.96));
}

@media (max-width: 960px) {
  .stat-grid,
  .insight-grid {
    grid-template-columns: 1fr;
  }
}
</style>
