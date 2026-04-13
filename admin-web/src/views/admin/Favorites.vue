<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminApi, type AdminAiFavorite, type PageResult } from '@/api/admin'

const keyword = ref('')
const loading = ref(false)
const deletingId = ref<number | null>(null)
const selectedFavorite = ref<AdminAiFavorite | null>(null)
const pageData = ref<PageResult<AdminAiFavorite>>({
  content: [],
  totalElements: 0,
  totalPages: 0,
  size: 10,
  number: 0,
})

const totalMaterials = computed(() =>
  pageData.value.content.reduce((sum, item) => sum + item.materials.length, 0),
)
const totalSteps = computed(() => pageData.value.content.reduce((sum, item) => sum + item.steps.length, 0))

async function loadFavorites(nextPage = 0) {
  loading.value = true
  try {
    pageData.value = await adminApi.getAiFavorites({
      keyword: keyword.value || undefined,
      page: nextPage,
      size: 10,
    })
  } finally {
    loading.value = false
  }
}

async function removeFavorite(item: AdminAiFavorite) {
  const confirmed = window.confirm(`确认删除收藏「${item.name}」吗？删除后该记录将从后台移除。`)
  if (!confirmed) {
    return
  }

  deletingId.value = item.id
  try {
    await adminApi.deleteAiFavorite(item.id)
    if (selectedFavorite.value?.id === item.id) {
      selectedFavorite.value = null
    }
    const nextPage = pageData.value.content.length === 1 && pageData.value.number > 0 ? pageData.value.number - 1 : pageData.value.number
    await loadFavorites(nextPage)
  } finally {
    deletingId.value = null
  }
}

onMounted(() => loadFavorites())
</script>

<template>
  <section class="console-page">
    <article class="hero-panel card">
      <div class="page-head hero-head">
        <div>
          <p class="hero-tag">AI FAVORITES / 收藏配方沉淀池</p>
          <h1 class="page-title">收藏管理</h1>
          <p class="page-subtitle">
            统一查看 AI 调酒配方收藏记录、用户来源和完整内容，方便做删改、回溯和内容质量盘点。
          </p>
        </div>
        <button class="button-primary" :disabled="loading" @click="loadFavorites(pageData.number)">
          {{ loading ? '刷新中...' : '刷新收藏池' }}
        </button>
      </div>

      <div class="metric-grid">
        <article class="metric-card">
          <span>收藏记录</span>
          <strong>{{ pageData.totalElements }}</strong>
          <p>当前后台可检索到的 AI 收藏条数</p>
        </article>
        <article class="metric-card accent">
          <span>当前页材料项</span>
          <strong>{{ totalMaterials }}</strong>
          <p>用于估算收藏配方复杂度</p>
        </article>
        <article class="metric-card secondary">
          <span>当前页步骤数</span>
          <strong>{{ totalSteps }}</strong>
          <p>便于快速辨别长短配方</p>
        </article>
      </div>
    </article>

    <article class="filter-panel card">
      <div class="panel-headline">
        <div>
          <p class="panel-tag">SEARCH</p>
          <h2>搜索收藏记录</h2>
        </div>
        <span class="badge">第 {{ pageData.number + 1 }} / {{ Math.max(pageData.totalPages, 1) }} 页</span>
      </div>

      <div class="toolbar">
        <input
          v-model="keyword"
          class="field search"
          type="text"
          placeholder="搜索用户名、配方名、描述、prompt"
          @keyup.enter="loadFavorites()"
        />
        <button class="button-secondary" :disabled="loading" @click="loadFavorites()">{{ loading ? '查询中...' : '立即查询' }}</button>
      </div>
    </article>

    <article class="table-panel card">
      <div class="panel-headline compact">
        <div>
          <p class="panel-tag">FAVORITE STREAM</p>
          <h2>收藏记录列表</h2>
        </div>
        <span class="badge subtle">双击查看完整配方结构</span>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>收藏用户</th>
              <th>配方</th>
              <th>摘要</th>
              <th>来源</th>
              <th>收藏时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in pageData.content" :key="item.id" @dblclick="selectedFavorite = item">
              <td>#{{ item.id }}</td>
              <td>
                <div class="cell-stack">
                  <strong>{{ item.nickname || item.username || `用户${item.userId}` }}</strong>
                  <span>{{ item.username || `ID ${item.userId}` }}</span>
                </div>
              </td>
              <td>
                <div class="cell-stack">
                  <strong>{{ item.name }}</strong>
                  <span>{{ item.description || '暂无风味描述' }}</span>
                </div>
              </td>
              <td>
                <div class="summary-stack">
                  <span>{{ item.materials.length }} 个材料</span>
                  <span>{{ item.steps.length }} 个步骤</span>
                  <code>{{ item.recipeKey }}</code>
                </div>
              </td>
              <td><span class="source-chip">{{ item.source || 'ai' }}</span></td>
              <td>{{ item.createdAt?.replace('T', ' ') || '-' }}</td>
              <td>
                <div class="actions">
                  <button class="button-secondary" @click="selectedFavorite = item">查看</button>
                  <button class="button-danger" :disabled="deletingId === item.id" @click="removeFavorite(item)">
                    {{ deletingId === item.id ? '删除中...' : '删除' }}
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!pageData.content.length">
              <td colspan="7" class="empty">当前没有收藏记录</td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <div class="pager card">
      <button class="button-secondary" :disabled="pageData.number <= 0 || loading" @click="loadFavorites(pageData.number - 1)">上一页</button>
      <span>第 {{ pageData.number + 1 }} / {{ Math.max(pageData.totalPages, 1) }} 页</span>
      <button
        class="button-secondary"
        :disabled="pageData.number + 1 >= pageData.totalPages || loading || !pageData.totalPages"
        @click="loadFavorites(pageData.number + 1)"
      >
        下一页
      </button>
    </div>

    <div v-if="selectedFavorite" class="modal-backdrop" @click.self="selectedFavorite = null">
      <div class="modal-panel detail-panel">
        <div class="detail-head">
          <div>
            <p class="detail-tag">FAVORITE DETAIL</p>
            <h2>{{ selectedFavorite.name }}</h2>
            <p class="detail-desc">{{ selectedFavorite.description || '暂无风味描述' }}</p>
          </div>
          <button class="button-secondary" @click="selectedFavorite = null">关闭</button>
        </div>

        <div class="detail-metrics">
          <article class="detail-metric">
            <span>收藏用户</span>
            <strong>{{ selectedFavorite.nickname || selectedFavorite.username || selectedFavorite.userId }}</strong>
          </article>
          <article class="detail-metric">
            <span>材料数量</span>
            <strong>{{ selectedFavorite.materials.length }}</strong>
          </article>
          <article class="detail-metric">
            <span>步骤数量</span>
            <strong>{{ selectedFavorite.steps.length }}</strong>
          </article>
        </div>

        <div class="detail-grid">
          <article class="detail-card">
            <h3>收藏信息</h3>
            <ul>
              <li><strong>收藏 ID：</strong>{{ selectedFavorite.id }}</li>
              <li><strong>用户：</strong>{{ selectedFavorite.nickname || selectedFavorite.username || selectedFavorite.userId }}</li>
              <li><strong>用户名：</strong>{{ selectedFavorite.username || '-' }}</li>
              <li><strong>来源：</strong>{{ selectedFavorite.source || 'ai' }}</li>
              <li><strong>收藏时间：</strong>{{ selectedFavorite.createdAt?.replace('T', ' ') || '-' }}</li>
            </ul>
          </article>

          <article class="detail-card">
            <h3>生成需求</h3>
            <p class="prompt-card">{{ selectedFavorite.prompt || '暂无 prompt' }}</p>
            <p class="hash-text"><strong>recipeKey：</strong>{{ selectedFavorite.recipeKey }}</p>
          </article>
        </div>

        <div class="detail-grid">
          <article class="detail-card">
            <h3>材料</h3>
            <ol class="ordered-list">
              <li v-for="material in selectedFavorite.materials" :key="material">{{ material }}</li>
            </ol>
          </article>

          <article class="detail-card">
            <h3>步骤</h3>
            <ol class="ordered-list">
              <li v-for="step in selectedFavorite.steps" :key="step">{{ step }}</li>
            </ol>
          </article>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.console-page {
  display: grid;
  gap: 18px;
}

.hero-panel,
.filter-panel,
.table-panel,
.pager {
  padding: 22px;
}

.hero-panel {
  background:
    radial-gradient(circle at top right, rgba(72, 215, 255, 0.16), transparent 30%),
    linear-gradient(180deg, rgba(11, 29, 46, 0.96), rgba(7, 18, 31, 0.98));
}

.hero-head {
  margin-bottom: 18px;
}

.hero-tag,
.panel-tag,
.detail-tag {
  margin: 0 0 10px;
  font-size: 0.72rem;
  letter-spacing: 0.22em;
  color: var(--primary);
}

.metric-grid,
.detail-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.metric-card,
.detail-metric {
  padding: 18px;
  border-radius: 20px;
  background: rgba(8, 22, 36, 0.8);
  border: 1px solid rgba(72, 215, 255, 0.12);
}

.metric-card span,
.metric-card p,
.detail-metric span,
.detail-desc,
.summary-stack span,
.cell-stack span {
  color: var(--ink-600);
}

.metric-card strong,
.detail-metric strong {
  display: block;
  margin: 10px 0 6px;
  font-size: 2.1rem;
  line-height: 1;
  letter-spacing: -0.06em;
}

.metric-card p {
  margin: 0;
}

.metric-card.accent {
  border-color: rgba(255, 182, 72, 0.16);
}

.metric-card.secondary {
  border-color: rgba(76, 111, 255, 0.2);
}

.panel-headline {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.panel-headline.compact {
  margin-bottom: 16px;
}

.panel-headline h2 {
  margin: 0;
  font-size: 1.28rem;
  letter-spacing: -0.04em;
}

.search {
  max-width: 380px;
}

.subtle {
  background: rgba(76, 111, 255, 0.14);
  color: #cdd6ff;
}

.table-wrap {
  overflow-x: auto;
}

th,
 td {
  padding: 16px 14px;
  text-align: left;
  border-bottom: 1px solid var(--line);
  vertical-align: top;
}

th {
  color: var(--ink-600);
  font-size: 0.82rem;
  letter-spacing: 0.12em;
}

tbody tr {
  background: rgba(7, 18, 31, 0.16);
}

tbody tr:hover {
  background: rgba(72, 215, 255, 0.06);
}

.cell-stack,
.summary-stack {
  display: grid;
  gap: 6px;
}

.summary-stack code,
.hash-text {
  word-break: break-all;
}

.summary-stack code {
  display: inline-flex;
  width: fit-content;
  padding: 6px 10px;
  border-radius: 12px;
  background: rgba(76, 111, 255, 0.12);
  color: #c9d4ff;
}

.source-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 68px;
  padding: 7px 12px;
  border-radius: 12px;
  background: rgba(45, 212, 191, 0.12);
  color: var(--success);
  font-weight: 700;
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.empty {
  text-align: center;
  color: var(--ink-600);
  padding: 28px 16px;
}

.pager {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.detail-panel {
  width: min(980px, 100%);
  display: grid;
  gap: 18px;
}

.detail-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.detail-head h2 {
  margin: 0;
  font-size: 2rem;
  letter-spacing: -0.05em;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.detail-card {
  padding: 20px;
  border-radius: 22px;
  background: rgba(8, 22, 36, 0.76);
  border: 1px solid rgba(153, 199, 255, 0.1);
}

.detail-card h3 {
  margin: 0 0 14px;
  font-size: 1.16rem;
}

.detail-card ul,
.ordered-list {
  margin: 0;
  padding-left: 18px;
  display: grid;
  gap: 10px;
  color: var(--ink-800);
}

.prompt-card {
  margin: 0 0 14px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(76, 111, 255, 0.1);
  color: var(--ink-950);
  white-space: pre-wrap;
}

.hash-text {
  margin: 0;
  color: var(--ink-600);
}

@media (max-width: 960px) {
  .metric-grid,
  .detail-metrics,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero-panel,
  .filter-panel,
  .table-panel,
  .pager {
    padding: 18px;
  }

  .panel-headline,
  .detail-head,
  .pager {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
