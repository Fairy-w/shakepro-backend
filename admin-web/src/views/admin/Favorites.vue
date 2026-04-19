<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminApi, type AdminAiFavorite, type PageResult } from '@/api/admin'
import AdminDialog from '@/components/admin/AdminDialog.vue'
import AdminMetricCard from '@/components/admin/AdminMetricCard.vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'

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

const totalMaterials = computed(() => pageData.value.content.reduce((sum, item) => sum + item.materials.length, 0))
const totalSteps = computed(() => pageData.value.content.reduce((sum, item) => sum + item.steps.length, 0))

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
  <section class="page-stack favorites-page">
    <AdminPageHeader
      eyebrow="Favorites"
      title="收藏管理"
      subtitle="集中回看 AI 调酒配方收藏，按用户、配方名或生成要求检索内容，必要时快速清理冗余记录。"
    >
      <template #meta>
        <span class="badge">收藏 {{ pageData.totalElements }} 条</span>
      </template>
    </AdminPageHeader>

    <div class="metric-grid compact-grid">
      <AdminMetricCard eyebrow="当前页" label="材料总项" :value="totalMaterials" hint="可快速感知当前收藏页的复杂度。" />
      <AdminMetricCard eyebrow="当前页" label="步骤总数" :value="totalSteps" hint="步骤越多，越适合重点回看高复杂度配方。" tone="warm" />
    </div>

    <AdminToolbar>
      <input
        v-model="keyword"
        class="field search"
        type="text"
        placeholder="搜索用户、配方名或生成要求"
        @keyup.enter="loadFavorites()"
      />
      <button class="button-primary" :disabled="loading" @click="loadFavorites()">{{ loading ? '查询中...' : '搜索' }}</button>
    </AdminToolbar>

    <div class="card table-card">
      <table class="table-base">
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
            <td class="mono">#{{ item.id }}</td>
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
            <td><span class="status-pill warn">{{ formatSource(item.source) }}</span></td>
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

    <AdminPagination
      :page="pageData.number"
      :total-pages="pageData.totalPages"
      :loading="loading"
      @prev="loadFavorites(pageData.number - 1)"
      @next="loadFavorites(pageData.number + 1)"
    />

    <AdminDialog :visible="selectedFavorite !== null" size="xl" @close="selectedFavorite = null">
      <div v-if="selectedFavorite" class="favorite-dialog">
        <div class="favorite-dialog__head">
          <div>
            <p class="favorite-dialog__eyebrow">收藏详情</p>
            <h2>{{ selectedFavorite.name }}</h2>
            <p>{{ selectedFavorite.description || '暂无风味描述' }}</p>
          </div>
          <button class="button-secondary" @click="selectedFavorite = null">关闭</button>
        </div>

        <div class="dual-grid">
          <article class="favorite-sheet card">
            <h3>收藏信息</h3>
            <ul>
              <li><strong>收藏 ID：</strong>{{ selectedFavorite.id }}</li>
              <li><strong>用户：</strong>{{ selectedFavorite.nickname || selectedFavorite.username || selectedFavorite.userId }}</li>
              <li><strong>来源：</strong>{{ formatSource(selectedFavorite.source) }}</li>
              <li><strong>收藏时间：</strong>{{ selectedFavorite.createdAt?.replace('T', ' ') || '-' }}</li>
              <li><strong>配方标识：</strong><code>{{ selectedFavorite.recipeKey }}</code></li>
            </ul>
          </article>

          <article class="favorite-sheet card">
            <h3>生成要求</h3>
            <p class="prompt-card">{{ selectedFavorite.prompt || '暂无生成要求' }}</p>
          </article>
        </div>

        <div class="dual-grid">
          <article class="favorite-sheet card">
            <h3>材料</h3>
            <ol class="ordered-list">
              <li v-for="material in selectedFavorite.materials" :key="material">{{ material }}</li>
            </ol>
          </article>

          <article class="favorite-sheet card">
            <h3>步骤</h3>
            <ol class="ordered-list">
              <li v-for="step in selectedFavorite.steps" :key="step">{{ step }}</li>
            </ol>
          </article>
        </div>
      </div>
    </AdminDialog>
  </section>
</template>

<style scoped>
.favorites-page {
  display: grid;
  gap: 18px;
}

.compact-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.search {
  max-width: 420px;
}

.user-cell,
.recipe-cell,
.meta-stack {
  display: grid;
  gap: 4px;
}

.user-cell span,
.recipe-cell p,
.meta-stack span {
  margin: 0;
  color: var(--ink-600);
}

.meta-stack code {
  color: var(--primary);
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.favorite-dialog {
  display: grid;
  gap: 18px;
}

.favorite-dialog__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.favorite-dialog__eyebrow {
  margin: 0;
  color: var(--ink-600);
  font-size: 0.74rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.favorite-dialog__head h2 {
  margin: 10px 0 8px;
  font-size: 2.2rem;
  line-height: 0.96;
}

.favorite-dialog__head p:last-child {
  margin: 0;
  color: var(--ink-600);
}

.favorite-sheet {
  padding: 20px;
}

.favorite-sheet h3 {
  margin: 0 0 12px;
  font-size: 1.3rem;
}

.favorite-sheet ul,
.ordered-list {
  margin: 0;
  padding-left: 18px;
  display: grid;
  gap: 10px;
  color: var(--ink-800);
}

.prompt-card {
  margin: 0;
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.62);
  color: var(--ink-800);
}

@media (max-width: 900px) {
  .compact-grid {
    grid-template-columns: 1fr;
  }

  .favorite-dialog__head {
    flex-direction: column;
  }
}
</style>
