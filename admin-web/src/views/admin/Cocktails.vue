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
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">鸡尾酒管理</h1>
        <p class="page-subtitle">集中维护配方、步骤和材料，让酒单内容在每次更新后都保持一致。</p>
      </div>
      <button class="button-primary" :disabled="materialsLoading || !hasMaterialOptions" @click="openCreate">
        {{ materialsLoading ? '材料准备中...' : '新建鸡尾酒' }}
      </button>
    </div>

    <div class="toolbar">
      <input v-model="keyword" class="field search" type="text" placeholder="按名称搜索鸡尾酒" @keyup.enter="loadCocktails()" />
      <button class="button-secondary" :disabled="loading" @click="loadCocktails()">
        {{ loading ? '查询中...' : '搜索' }}
      </button>
    </div>

    <div class="cocktail-grid">
      <article v-for="item in pageData.content" :key="item.id" class="cocktail-card card">
        <div class="cover" :style="{ backgroundImage: item.imageUrl ? `url(${item.imageUrl})` : undefined }">
          <span class="badge">#{{ item.id }}</span>
        </div>
        <div class="body">
          <div class="title-row">
            <h3>{{ item.name }}</h3>
            <span>{{ item.alcoholLevel ?? '-' }}%</span>
          </div>
          <p>创建于 {{ item.createdAt?.replace('T', ' ') || '-' }}</p>
          <div class="actions">
            <button class="button-secondary" @click="openEdit(item)">编辑</button>
            <button class="button-danger" @click="remove(item)">删除</button>
          </div>
        </div>
      </article>
      <article v-if="!pageData.content.length" class="cocktail-card card empty-card">
        <h3>还没有鸡尾酒内容</h3>
        <p>先添加几款招牌酒单，列表就会很快充实起来。</p>
      </article>
    </div>

    <div class="pager">
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
        <div class="page-head compact">
          <div>
            <h2 class="page-title">{{ editingId ? '编辑鸡尾酒' : '新建鸡尾酒' }}</h2>
            <p class="page-subtitle">逐项补充图片、简介、步骤和材料，展示内容会更完整清晰。</p>
          </div>
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
            <textarea v-model="form.description" class="textarea" rows="3" placeholder="写下这款酒的风味亮点"></textarea>
          </label>
          <label class="span-two">
            <span>制作步骤</span>
            <textarea v-model="form.steps" class="textarea" rows="6" placeholder="每一步单独换行录入"></textarea>
          </label>
        </div>

        <div class="material-editor">
          <div class="material-head">
            <h3>材料明细</h3>
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
.search {
  max-width: 320px;
}

.cocktail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.cocktail-card {
  overflow: hidden;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.cocktail-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 28px 56px rgba(16, 32, 46, 0.14);
}

.cover {
  min-height: 190px;
  padding: 18px;
  background:
    linear-gradient(135deg, rgba(15, 118, 110, 0.42), rgba(16, 32, 46, 0.46)),
    linear-gradient(135deg, rgba(245, 158, 11, 0.24), transparent);
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: flex-start;
  transition: transform 0.28s ease;
}

.cocktail-card:hover .cover {
  transform: scale(1.02);
}

.body {
  padding: 18px;
}

.title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.title-row h3 {
  font-size: 1.35rem;
  letter-spacing: -0.04em;
}

.body p {
  margin: 10px 0 18px;
  color: var(--ink-600);
}

.actions,
.footer-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.pager {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-top: 18px;
}

.cocktail-modal {
  width: min(920px, 100%);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-grid label {
  display: grid;
  gap: 8px;
}

.span-two {
  grid-column: span 2;
}

.material-editor {
  margin-top: 22px;
  padding-top: 20px;
  border-top: 1px solid var(--line);
}

.material-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
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

.empty-card {
  padding: 22px;
}

.compact {
  margin-bottom: 18px;
}

@media (max-width: 960px) {
  .cocktail-grid,
  .form-grid,
  .material-row {
    grid-template-columns: 1fr;
  }

  .span-two {
    grid-column: auto;
  }

  .pager,
  .material-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
