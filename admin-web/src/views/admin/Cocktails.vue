<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  adminApi,
  type AdminCocktailDetail,
  type AdminCocktailListItem,
  type AdminMaterial,
  type PageResult,
} from '@/api/admin'

const loading = ref(false)
const materialsLoading = ref(false)
const keyword = ref('')
const pageData = ref<PageResult<AdminCocktailListItem>>({
  content: [],
  totalElements: 0,
  totalPages: 0,
  size: 10,
  number: 0,
})

const materialOptions = ref<AdminMaterial[]>([])
const showModal = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  description: '',
  imageUrl: '',
  alcoholLevel: '' as string | number,
  steps: '',
  materials: [{ materialId: '', amount: '' }] as Array<{ materialId: string | number; amount: string }>,
})

const hasMaterialOptions = computed(() => materialOptions.value.length > 0)
const currentPageAlcoholAvg = computed(() => {
  const values = pageData.value.content
    .map((item) => item.alcoholLevel)
    .filter((value): value is number => typeof value === 'number')

  if (!values.length) {
    return '--'
  }

  return (values.reduce((sum, value) => sum + value, 0) / values.length).toFixed(1)
})

async function loadCocktails(nextPage = 0) {
  loading.value = true
  try {
    pageData.value = await adminApi.getCocktails({
      keyword: keyword.value || undefined,
      page: nextPage,
      size: 8,
    })
  } finally {
    loading.value = false
  }
}

async function loadMaterials() {
  materialsLoading.value = true
  try {
    materialOptions.value = await adminApi.getMaterials()
  } finally {
    materialsLoading.value = false
  }
}

function resetForm() {
  editingId.value = null
  form.name = ''
  form.description = ''
  form.imageUrl = ''
  form.alcoholLevel = ''
  form.steps = ''
  form.materials = [{ materialId: '', amount: '' }]
}

function openCreate() {
  resetForm()
  showModal.value = true
}

async function openEdit(item: AdminCocktailListItem) {
  const detail = await adminApi.getCocktail(item.id)
  fillForm(detail)
  editingId.value = item.id
  showModal.value = true
}

function fillForm(detail: AdminCocktailDetail) {
  form.name = detail.name
  form.description = detail.description || ''
  form.imageUrl = detail.imageUrl || ''
  form.alcoholLevel = detail.alcoholLevel ?? ''
  form.steps = detail.steps || ''
  form.materials = detail.materials.length
    ? detail.materials.map((item) => ({
        materialId: item.materialId,
        amount: item.amount,
      }))
    : [{ materialId: '', amount: '' }]
}

function addMaterialRow() {
  form.materials.push({ materialId: '', amount: '' })
}

function removeMaterialRow(index: number) {
  if (form.materials.length === 1) return
  form.materials.splice(index, 1)
}

async function submit() {
  if (!form.name.trim()) return

  const payload = {
    name: form.name.trim(),
    description: form.description.trim() || undefined,
    imageUrl: form.imageUrl.trim() || undefined,
    alcoholLevel: form.alcoholLevel === '' ? null : Number(form.alcoholLevel),
    steps: form.steps.trim() || undefined,
    materials: form.materials
      .filter((item) => item.materialId && item.amount.trim())
      .map((item) => ({
        materialId: Number(item.materialId),
        amount: item.amount.trim(),
      })),
  }

  if (editingId.value) {
    await adminApi.updateCocktail(editingId.value, payload)
  } else {
    await adminApi.createCocktail(payload)
  }

  showModal.value = false
  await loadCocktails(pageData.value.number)
}

async function remove(item: AdminCocktailListItem) {
  if (!window.confirm(`确认删除鸡尾酒“${item.name}”吗？`)) return
  await adminApi.deleteCocktail(item.id)
  await loadCocktails(Math.max(pageData.value.number - (pageData.value.content.length === 1 ? 1 : 0), 0))
}

onMounted(async () => {
  await Promise.all([loadCocktails(), loadMaterials()])
})
</script>

<template>
  <section class="console-page">
    <article class="hero-panel card">
      <div class="page-head hero-head">
        <div>
          <p class="hero-tag">COCKTAIL LIBRARY / 成品配方展示池</p>
          <h1 class="page-title">鸡尾酒管理</h1>
          <p class="page-subtitle">
            配方采集工作台产出的最终内容会汇入这里，后台可继续手动编辑、补图和维护材料映射，形成可展示的成品库。
          </p>
        </div>
        <button class="button-primary" :disabled="materialsLoading || !hasMaterialOptions" @click="openCreate">
          {{ materialsLoading ? '材料加载中...' : '新增鸡尾酒' }}
        </button>
      </div>

      <div class="metric-grid">
        <article class="metric-card">
          <span>总条目</span>
          <strong>{{ pageData.totalElements }}</strong>
          <p>支持关键词分页检索</p>
        </article>
        <article class="metric-card accent">
          <span>当前页均值 ABV</span>
          <strong>{{ currentPageAlcoholAvg }}</strong>
          <p>按当前展示配方计算</p>
        </article>
        <article class="metric-card secondary">
          <span>材料池规模</span>
          <strong>{{ materialOptions.length }}</strong>
          <p>新增配方前先保证材料池完整</p>
        </article>
      </div>
    </article>

    <article class="filter-panel card">
      <div class="panel-headline">
        <div>
          <p class="panel-tag">SEARCH</p>
          <h2>筛选鸡尾酒配方</h2>
        </div>
        <span class="badge">第 {{ pageData.number + 1 }} / {{ Math.max(pageData.totalPages, 1) }} 页</span>
      </div>

      <div class="toolbar">
        <input v-model="keyword" class="field search" type="text" placeholder="按名称搜索鸡尾酒" @keyup.enter="loadCocktails()" />
        <button class="button-secondary" :disabled="loading" @click="loadCocktails()">
          {{ loading ? '查询中...' : '立即查询' }}
        </button>
      </div>
    </article>

    <div class="cocktail-grid">
      <article v-for="item in pageData.content" :key="item.id" class="cocktail-card card">
        <div class="cover" :style="{ backgroundImage: item.imageUrl ? `url(${item.imageUrl})` : undefined }">
          <div class="cover-overlay">
            <span class="badge">#{{ item.id }}</span>
            <span class="abv-chip">{{ item.alcoholLevel ?? '-' }}%</span>
          </div>
        </div>

        <div class="body">
          <div class="title-row">
            <h3>{{ item.name }}</h3>
          </div>
          <p>创建于 {{ item.createdAt?.replace('T', ' ') || '-' }}</p>
          <div class="actions">
            <button class="button-secondary" @click="openEdit(item)">编辑</button>
            <button class="button-danger" @click="remove(item)">删除</button>
          </div>
        </div>
      </article>

      <article v-if="!pageData.content.length" class="cocktail-card card empty-card">
        <p class="hero-tag">EMPTY</p>
        <h3>当前筛选下暂无鸡尾酒</h3>
        <p>可以先从配方采集工作台发布候选，再回到这里做成品库展示。</p>
      </article>
    </div>

    <div class="pager card">
      <button class="button-secondary" :disabled="pageData.number <= 0 || loading" @click="loadCocktails(pageData.number - 1)">上一页</button>
      <span>第 {{ pageData.number + 1 }} / {{ Math.max(pageData.totalPages, 1) }} 页</span>
      <button
        class="button-secondary"
        :disabled="pageData.number + 1 >= pageData.totalPages || loading || !pageData.totalPages"
        @click="loadCocktails(pageData.number + 1)"
      >
        下一页
      </button>
    </div>

    <div v-if="showModal" class="modal-backdrop" @click.self="showModal = false">
      <div class="modal-panel cocktail-modal">
        <div class="panel-headline compact">
          <div>
            <p class="panel-tag">{{ editingId ? 'EDIT COCKTAIL' : 'CREATE COCKTAIL' }}</p>
            <h2>{{ editingId ? '编辑鸡尾酒' : '新增鸡尾酒' }}</h2>
          </div>
          <span class="badge">建议步骤写成多行文本</span>
        </div>

        <div class="form-grid">
          <label>
            <span>名称</span>
            <input v-model="form.name" class="field" type="text" placeholder="例如：Mojito" />
          </label>
          <label>
            <span>图片地址</span>
            <input v-model="form.imageUrl" class="field" type="text" placeholder="https://..." />
          </label>
          <label>
            <span>酒精度</span>
            <input v-model="form.alcoholLevel" class="field" type="number" min="0" max="100" placeholder="0-100" />
          </label>
          <label class="span-two">
            <span>描述</span>
            <textarea v-model="form.description" class="textarea" rows="3" placeholder="输入简介"></textarea>
          </label>
          <label class="span-two">
            <span>制作步骤</span>
            <textarea v-model="form.steps" class="textarea" rows="6" placeholder="每一步换行录入"></textarea>
          </label>
        </div>

        <div class="material-editor">
          <div class="material-head">
            <div>
              <p class="panel-tag">MATERIAL MAPPING</p>
              <h3>材料明细</h3>
            </div>
            <button class="button-secondary" type="button" @click="addMaterialRow">添加一行</button>
          </div>

          <div v-for="(item, index) in form.materials" :key="index" class="material-row">
            <select v-model="item.materialId" class="select">
              <option value="">选择材料</option>
              <option v-for="material in materialOptions" :key="material.id" :value="material.id">
                {{ material.name }}<template v-if="material.category"> / {{ material.category }}</template>
              </option>
            </select>
            <input v-model="item.amount" class="field" type="text" placeholder="例如：45ml" />
            <button class="button-danger mini" type="button" @click="removeMaterialRow(index)">删除</button>
          </div>
        </div>

        <div class="footer-actions">
          <button class="button-secondary" @click="showModal = false">取消</button>
          <button class="button-primary" @click="submit">保存</button>
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
.panel-tag {
  margin: 0 0 10px;
  font-size: 0.72rem;
  letter-spacing: 0.22em;
  color: var(--primary);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  padding: 18px;
  border-radius: 20px;
  background: rgba(8, 22, 36, 0.8);
  border: 1px solid rgba(72, 215, 255, 0.12);
}

.metric-card span,
.metric-card p,
.body p {
  color: var(--ink-600);
}

.metric-card strong {
  display: block;
  margin: 10px 0 6px;
  font-size: 2.1rem;
  line-height: 1;
  letter-spacing: -0.06em;
}

.metric-card p,
.body p {
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
  margin-bottom: 20px;
}

.panel-headline h2 {
  margin: 0;
  font-size: 1.28rem;
  letter-spacing: -0.04em;
}

.search {
  max-width: 360px;
}

.cocktail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.cocktail-card {
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(72, 215, 255, 0.1), transparent 30%),
    linear-gradient(180deg, rgba(10, 27, 43, 0.92), rgba(7, 18, 31, 0.96));
}

.cover {
  min-height: 220px;
  padding: 18px;
  background:
    linear-gradient(135deg, rgba(8, 22, 36, 0.2), rgba(8, 22, 36, 0.78)),
    linear-gradient(135deg, rgba(72, 215, 255, 0.24), rgba(76, 111, 255, 0.16));
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: flex-start;
}

.cover-overlay {
  width: 100%;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.abv-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  border-radius: 12px;
  background: rgba(255, 182, 72, 0.14);
  color: #ffd79a;
  font-weight: 700;
}

.body {
  padding: 18px;
  display: grid;
  gap: 16px;
}

.title-row h3 {
  margin: 0;
  font-size: 1.42rem;
  letter-spacing: -0.04em;
}

.actions,
.footer-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.empty-card {
  align-content: center;
}

.pager {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.cocktail-modal {
  width: min(980px, 100%);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-grid label {
  display: grid;
  gap: 8px;
  color: var(--ink-800);
}

.span-two {
  grid-column: span 2;
}

.material-editor {
  margin-top: 22px;
  padding-top: 22px;
  border-top: 1px solid var(--line);
}

.material-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.material-head h3 {
  margin: 0;
  font-size: 1.18rem;
}

.material-row {
  display: grid;
  grid-template-columns: 1.4fr 1fr auto;
  gap: 12px;
  margin-bottom: 12px;
}

.mini {
  padding-inline: 14px;
}

@media (max-width: 960px) {
  .metric-grid,
  .cocktail-grid,
  .form-grid,
  .material-row {
    grid-template-columns: 1fr;
  }

  .span-two {
    grid-column: auto;
  }
}

@media (max-width: 720px) {
  .hero-panel,
  .filter-panel,
  .pager {
    padding: 18px;
  }

  .panel-headline,
  .pager,
  .material-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
