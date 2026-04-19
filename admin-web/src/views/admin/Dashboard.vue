<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminApi, type DashboardStats } from '@/api/admin'
import AdminMetricCard from '@/components/admin/AdminMetricCard.vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'

const stats = ref<DashboardStats | null>(null)
const loading = ref(false)

const spotlightCards = computed(() => [
  {
    eyebrow: '内容规模',
    label: '鸡尾酒条目',
    value: stats.value?.totalCocktails ?? '--',
    hint: '适合今天优先回看图片与风味描述完整度。',
    tone: 'strong' as const,
  },
  {
    eyebrow: '基础库',
    label: '材料条目',
    value: stats.value?.totalMaterials ?? '--',
    hint: '基础材料库越完整，后续 AI 导入越顺。',
    tone: 'warm' as const,
  },
  {
    eyebrow: '用户池',
    label: '用户总数',
    value: stats.value?.totalUsers ?? '--',
    hint: `${stats.value?.totalAdmins ?? '--'} 位管理账号在线维护中。`,
    tone: 'default' as const,
  },
  {
    eyebrow: '灵感沉淀',
    label: '收藏总量',
    value: stats.value ? stats.value.totalFavorites + stats.value.totalAiCocktailFavorites : '--',
    hint: '可据此追踪热门调酒方向和高频偏好。',
    tone: 'success' as const,
  },
])

const operations = computed(() => {
  if (!stats.value) {
    return [
      '刷新后可回看当天的酒单、材料与收藏规模。',
      '优先处理图片缺失和说明不完整的内容页。',
      '抓取与批量导入的异常记录也应该纳入今天的清单。',
    ]
  }

  return [
    `当前库内共有 ${stats.value.totalCocktails} 款鸡尾酒，建议优先核对主图与服务说明。`,
    `材料库已沉淀 ${stats.value.totalMaterials} 项，适合继续提升图片覆盖和分类一致性。`,
    `收藏相关数据累计 ${stats.value.totalFavorites + stats.value.totalAiCocktailFavorites} 条，可据此判断近期热门风味。`,
  ]
})

const watchlist = computed(() => [
  'Cocktails：补齐主图、亮点文案与风味标签，优先保证展示面完整。',
  'Materials：同步词典后及时检查图片是否成功镜像到 OSS。',
  'AI Workspace：批量抓取任务结束后，第一时间回看失败项与缺失字段。',
])

const consoleStats = computed(() => [
  { label: '管理账号', value: stats.value?.totalAdmins ?? '--' },
  { label: '文件记录', value: stats.value?.totalFiles ?? '--' },
  { label: 'AI 收藏', value: stats.value?.totalAiCocktailFavorites ?? '--' },
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
  <section class="dashboard-page">
    <AdminPageHeader
      eyebrow="Velvet Glass"
      title="今日运营叙事面板"
      subtitle="用更像旗舰酒廊的方式查看内容资产、用户偏好与自动化处理节奏。"
    >
      <template #meta>
        <span class="badge">Reserve Ops</span>
        <span class="badge subtle">结构优先，玻璃点缀</span>
      </template>
      <template #actions>
        <button class="button-primary" :disabled="loading" @click="loadDashboard">
          {{ loading ? '刷新中...' : '刷新数据' }}
        </button>
      </template>
    </AdminPageHeader>

    <section class="dashboard-hero card">
      <div class="dashboard-hero__main">
        <p class="dashboard-hero__eyebrow">旗舰总览</p>
        <h2>先看最重要的四个运营维度，再决定今天的处理顺序。</h2>
        <p>
          这块区域保留品牌气质，但只承载今日总览与判断依据，不让视觉效果干扰数据阅读。
        </p>
      </div>

      <div class="dashboard-hero__console">
        <article v-for="item in consoleStats" :key="item.label" class="dashboard-hero__console-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </div>
    </section>

    <div class="metric-grid">
      <AdminMetricCard
        v-for="item in spotlightCards"
        :key="item.label"
        :eyebrow="item.eyebrow"
        :label="item.label"
        :value="item.value"
        :hint="item.hint"
        :tone="item.tone"
      />
    </div>

    <div class="split-grid">
      <article class="card dashboard-panel">
        <p class="dashboard-panel__eyebrow">今日建议</p>
        <h3>先完成哪些整理动作</h3>
        <ul class="dashboard-list">
          <li v-for="item in operations" :key="item">{{ item }}</li>
        </ul>
      </article>

      <article class="card dashboard-panel dashboard-panel--warm">
        <p class="dashboard-panel__eyebrow">工作站提醒</p>
        <h3>别让自动化流程失去追踪</h3>
        <ul class="dashboard-list compact">
          <li v-for="item in watchlist" :key="item">{{ item }}</li>
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

.dashboard-hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 18px;
  padding: 24px;
  background:
    radial-gradient(circle at 0% 0%, rgba(200, 155, 91, 0.18), transparent 24%),
    radial-gradient(circle at 100% 0%, rgba(122, 73, 63, 0.14), transparent 32%),
    linear-gradient(155deg, rgba(255, 255, 255, 0.82), rgba(250, 243, 236, 0.74));
}

.dashboard-hero__eyebrow,
.dashboard-panel__eyebrow {
  margin: 0;
  font-size: 0.74rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--ink-600);
}

.dashboard-hero__main {
  display: grid;
  gap: 12px;
}

.dashboard-hero__main h2 {
  margin: 0;
  font-size: clamp(2rem, 4vw, 3.2rem);
  line-height: 0.94;
}

.dashboard-hero__main p:last-child {
  margin: 0;
  max-width: 52ch;
  color: var(--ink-600);
}

.dashboard-hero__console {
  display: grid;
  gap: 12px;
}

.dashboard-hero__console-card {
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.56);
  border: 1px solid rgba(255, 255, 255, 0.5);
  display: grid;
  gap: 6px;
}

.dashboard-hero__console-card span {
  color: var(--ink-600);
}

.dashboard-hero__console-card strong {
  font-size: 1.5rem;
  font-family: var(--font-mono);
}

.dashboard-panel {
  padding: 24px;
}

.dashboard-panel h3 {
  margin: 10px 0 16px;
  font-size: 1.55rem;
  line-height: 1;
}

.dashboard-panel--warm {
  background:
    radial-gradient(circle at 100% 0%, rgba(200, 155, 91, 0.16), transparent 28%),
    linear-gradient(180deg, rgba(255, 250, 246, 0.84), rgba(246, 236, 228, 0.76));
}

.dashboard-list {
  margin: 0;
  padding-left: 18px;
  display: grid;
  gap: 12px;
  color: var(--ink-800);
}

.dashboard-list.compact {
  gap: 10px;
}

@media (max-width: 1024px) {
  .dashboard-hero {
    grid-template-columns: 1fr;
  }
}
</style>
