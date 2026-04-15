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
  pageData.value.content.reduce((sum, item) => sum + item.materials.length, 0)
)

function formatSource(source?: string | null) {
  if (!source || source.toLowerCase() === 'ai') {
    return 'AI 生成'
  }

  return source
}

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
  const confirmed = window.confirm(`确认删除收藏「${item.name}」吗？删除后这条记录会从列表中移除。`)
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
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">收藏管理</h1>
        <p class="page-subtitle">集中查看 AI 调酒配方收藏，按用户或配方检索内容，必要时快速清理不再需要的记录。</p>
      </div>
      <div class="head-stats">
        <span class="badge">收藏 {{ pageData.totalElements }} 条</span>
        <span class="badge warm">当前页材料项 {{ totalMaterials }} 条</span>
      </div>
    </div>

    <div class="toolbar">
      <input
        v-model="keyword"
        class="field search"
        type="text"
        placeholder="搜索用户、配方名或生成要求"
        @keyup.enter="loadFavorites()"
      />
      <button class="button-primary" :disabled="loading" @click="loadFavorites()">{{ loading ? '查询中...' : '搜索' }}</button>
    </div>

    <div class="card table-card">
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
          <tr v-for="item in pageData.content" :key="item.id">
            <td>#{{ item.id }}</td>
            <td>
              <div class="user-cell">
                <strong>{{ item.nickname || item.username || `用户${item.userId}` }}</strong>
                <span>{{ item.username || `ID ${item.userId}` }}</span>
              </div>
            </td>
            <td>
              <div class="recipe-cell">
                <strong>{{ item.name }}</strong>
                <p>{{ item.description || '暂无风味描述' }}</p>
              </div>
            </td>
            <td>
              <div class="meta-stack">
                <span>{{ item.materials.length }} 个材料</span>
                <span>{{ item.steps.length }} 个步骤</span>
                <code>{{ item.recipeKey }}</code>
              </div>
            </td>
            <td><span class="source-chip">{{ formatSource(item.source) }}</span></td>
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
            <td colspan="7" class="empty">暂时还没有匹配的收藏记录</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pager">
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
            <p class="detail-tag">收藏详情</p>
            <h2>{{ selectedFavorite.name }}</h2>
            <p class="detail-desc">{{ selectedFavorite.description || '暂无风味描述' }}</p>
          </div>
          <button class="button-secondary" @click="selectedFavorite = null">关闭</button>
        </div>

        <div class="detail-grid">
          <article class="detail-card">
            <h3>收藏信息</h3>
            <ul>
              <li><strong>收藏 ID：</strong>{{ selectedFavorite.id }}</li>
              <li><strong>用户：</strong>{{ selectedFavorite.nickname || selectedFavorite.username || selectedFavorite.userId }}</li>
              <li><strong>用户名：</strong>{{ selectedFavorite.username || '-' }}</li>
              <li><strong>来源：</strong>{{ formatSource(selectedFavorite.source) }}</li>
              <li><strong>收藏时间：</strong>{{ selectedFavorite.createdAt?.replace('T', ' ') || '-' }}</li>
            </ul>
          </article>

          <article class="detail-card">
            <h3>生成要求</h3>
            <p class="prompt-card">{{ selectedFavorite.prompt || '暂无生成要求' }}</p>
            <p class="hash-text"><strong>配方标识：</strong>{{ selectedFavorite.recipeKey }}</p>
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
.head-stats {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.warm {
  background: rgba(245, 158, 11, 0.12);
  color: #b45309;
}

.search {
  max-width: 360px;
}

.table-card {
  overflow: hidden;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(242, 247, 250, 0.66));
}

th,
td {
  padding: 16px 18px;
  text-align: left;
  border-bottom: 1px solid var(--line);
  vertical-align: top;
}

th {
  position: sticky;
  top: 0;
  z-index: 1;
  font-size: 0.84rem;
  letter-spacing: 0.08em;
  color: var(--ink-600);
  background: rgba(241, 247, 250, 0.96);
  backdrop-filter: blur(16px);
}

tbody tr {
  transition: background 0.2s ease;
}

tbody tr:hover {
  background: rgba(255, 255, 255, 0.46);
}

.user-cell,
.recipe-cell,
.meta-stack {
  display: grid;
  gap: 6px;
}

.user-cell span,
.recipe-cell p,
.meta-stack span {
  color: var(--ink-600);
}

.recipe-cell p {
  margin: 0;
  max-width: 320px;
}

.meta-stack code {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(16, 32, 46, 0.06);
  color: var(--ink-600);
  font-size: 0.78rem;
  word-break: break-all;
}

.source-chip {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.12);
  color: var(--primary-strong);
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
}

.pager {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-top: 18px;
}

.detail-panel {
  display: grid;
  gap: 18px;
}

.detail-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.detail-tag {
  margin: 0 0 8px;
  color: var(--ink-600);
  letter-spacing: 0.18em;
  font-size: 0.72rem;
}

.detail-head h2 {
  margin: 0;
  font-size: 1.8rem;
  letter-spacing: -0.04em;
}

.detail-desc {
  margin: 10px 0 0;
  color: var(--ink-600);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.detail-card {
  padding: 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.42);
  border: 1px solid rgba(255, 255, 255, 0.45);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.detail-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 36px rgba(16, 32, 46, 0.08);
}

.detail-card h3 {
  margin: 0 0 12px;
}

.detail-card ul,
.ordered-list {
  margin: 0;
  padding-left: 18px;
  display: grid;
  gap: 10px;
}

.prompt-card {
  margin: 0;
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.74);
  color: var(--ink-800);
}

.hash-text {
  margin: 14px 0 0;
  color: var(--ink-600);
  word-break: break-all;
}

@media (max-width: 1100px) {
  .table-card {
    overflow-x: auto;
  }

  th {
    top: auto;
    position: static;
  }
}

@media (max-width: 900px) {
  .pager,
  .detail-head,
  .detail-grid {
    flex-direction: column;
    grid-template-columns: 1fr;
  }
}
</style>
