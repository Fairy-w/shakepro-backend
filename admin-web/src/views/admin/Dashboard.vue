<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adminApi, type DashboardStats } from '@/api/admin'

const stats = ref<DashboardStats | null>(null)
const loading = ref(false)

const statCards = [
  { key: 'totalUsers', label: '用户总数', tone: 'green' },
  { key: 'totalAdmins', label: '管理员', tone: 'gold' },
  { key: 'totalCocktails', label: '鸡尾酒条目', tone: 'blue' },
  { key: 'totalMaterials', label: '材料条目', tone: 'green' },
  { key: 'totalFavorites', label: '收藏记录', tone: 'gold' },
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
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">后台总览</h1>
        <p class="page-subtitle">这一页先给你最核心的运营和内容规模感知，方便答辩时直接展示系统整体数据面貌。</p>
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
        <h3>后台改造已经完成的重点</h3>
        <ul>
          <li>现有 Web 路由已切为后台管理端，不再承载 C 端页面。</li>
          <li>新增管理员鉴权和 <code>/api/admin/**</code> 接口层。</li>
          <li>用户、收藏、材料、鸡尾酒四类核心管理对象已纳入后台。</li>
        </ul>
      </article>

      <article class="insight-panel accent card">
        <p class="insight-tag">NEXT STEP</p>
        <h3>下一步可以继续补什么</h3>
        <ul>
          <li>Banner / 分类配置</li>
          <li>用户启停用和密码重置</li>
          <li>AI 推荐调用记录与运营分析</li>
        </ul>
      </article>
    </div>
  </section>
</template>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 22px;
  min-height: 150px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.stat-card span {
  color: var(--ink-600);
  font-weight: 700;
}

.stat-card strong {
  font-size: clamp(2rem, 4vw, 3.4rem);
  letter-spacing: -0.06em;
}

.stat-card.green {
  background: linear-gradient(180deg, rgba(15, 118, 110, 0.08), rgba(255, 255, 255, 0.9));
}

.stat-card.gold {
  background: linear-gradient(180deg, rgba(245, 158, 11, 0.1), rgba(255, 255, 255, 0.9));
}

.stat-card.blue {
  background: linear-gradient(180deg, rgba(21, 33, 43, 0.08), rgba(255, 255, 255, 0.9));
}

.insight-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 16px;
  margin-top: 18px;
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
  color: var(--ink-600);
  letter-spacing: 0.2em;
  font-size: 0.72rem;
}

.accent {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.16), rgba(255, 255, 255, 0.94));
}

@media (max-width: 960px) {
  .stat-grid,
  .insight-grid {
    grid-template-columns: 1fr;
  }
}
</style>
