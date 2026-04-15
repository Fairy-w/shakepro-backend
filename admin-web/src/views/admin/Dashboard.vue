<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminApi, type DashboardStats } from '@/api/admin'

const stats = ref<DashboardStats | null>(null)
const loading = ref(false)

const statCards = [
  { key: 'totalUsers', label: '用户总数', tone: 'green' },
  { key: 'totalAdmins', label: '管理账号', tone: 'gold' },
  { key: 'totalCocktails', label: '鸡尾酒条目', tone: 'blue' },
  { key: 'totalMaterials', label: '材料条目', tone: 'green' },
  { key: 'totalFavorites', label: '收藏记录', tone: 'gold' },
  { key: 'totalAiCocktailFavorites', label: 'AI 配方收藏', tone: 'blue' },
  { key: 'totalFiles', label: '文件记录', tone: 'blue' },
] as const

const highlights = computed(() => {
  if (!stats.value) {
    return [
      '加载后可快速看到用户、酒单、材料与收藏的整体情况。',
      '刷新数据后更容易判断今天优先处理哪一块内容。',
      '如果刚更新了酒单或材料，这里会同步显示最新规模。',
    ]
  }

  return [
    `当前共沉淀 ${stats.value.totalUsers} 位用户，其中管理账号 ${stats.value.totalAdmins} 位。`,
    `已整理 ${stats.value.totalCocktails} 款鸡尾酒，并关联 ${stats.value.totalMaterials} 种材料。`,
    `收藏总量达到 ${stats.value.totalFavorites + stats.value.totalAiCocktailFavorites} 条，适合优先回看热门内容。`,
  ]
})

const suggestions = computed(() => [
  '优先检查新上架酒单的图片、简介和步骤是否完整。',
  '根据收藏趋势补齐常用材料标签，方便后续检索。',
  '定期清理不需要保留的测试内容，保持列表更干净。',
])

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
        <h1 class="page-title">经营概览</h1>
        <p class="page-subtitle">快速了解用户、酒单、材料和收藏的当前规模，把今天最重要的维护事项排在前面。</p>
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
        <p class="insight-tag">今日关注</p>
        <h3>一眼看清当前状态</h3>
        <ul>
          <li v-for="item in highlights" :key="item">{{ item }}</li>
        </ul>
      </article>

      <article class="insight-panel accent card">
        <p class="insight-tag">维护建议</p>
        <h3>让内容区更顺手</h3>
        <ul>
          <li v-for="item in suggestions" :key="item">{{ item }}</li>
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
  min-height: 156px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
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
  background: linear-gradient(180deg, rgba(15, 118, 110, 0.12), rgba(255, 255, 255, 0.72));
}

.stat-card.gold {
  background: linear-gradient(180deg, rgba(245, 158, 11, 0.14), rgba(255, 255, 255, 0.72));
}

.stat-card.blue {
  background: linear-gradient(180deg, rgba(56, 189, 248, 0.14), rgba(255, 255, 255, 0.72));
}

.insight-grid {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
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
  margin: 0;
}

.accent {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.18), rgba(255, 255, 255, 0.76));
}

@media (max-width: 960px) {
  .stat-grid,
  .insight-grid {
    grid-template-columns: 1fr;
  }
}
</style>
