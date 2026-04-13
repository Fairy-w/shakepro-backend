<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { adminApi, type AdminMaterial } from '@/api/admin'

const loading = ref(false)
const keyword = ref('')
const category = ref('')
const materials = ref<AdminMaterial[]>([])
const editingId = ref<number | null>(null)
const showModal = ref(false)
const form = reactive({
  name: '',
  category: '',
})

const categories = computed(() => {
  const values = new Set(materials.value.map((item) => item.category).filter(Boolean) as string[])
  return Array.from(values)
})
const categorizedCount = computed(() => materials.value.filter((item) => item.category).length)

async function loadMaterials() {
  loading.value = true
  try {
    materials.value = await adminApi.getMaterials({
      keyword: keyword.value || undefined,
      category: category.value || undefined,
    })
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.name = ''
  form.category = ''
  showModal.value = true
}

function openEdit(item: AdminMaterial) {
  editingId.value = item.id
  form.name = item.name
  form.category = item.category || ''
  showModal.value = true
}

async function submit() {
  if (!form.name.trim()) return

  const payload = {
    name: form.name.trim(),
    category: form.category.trim() || undefined,
  }

  if (editingId.value) {
    await adminApi.updateMaterial(editingId.value, payload)
  } else {
    await adminApi.createMaterial(payload)
  }

  showModal.value = false
  await loadMaterials()
}

async function remove(item: AdminMaterial) {
  if (!window.confirm(`确认删除材料“${item.name}”吗？`)) return
  await adminApi.deleteMaterial(item.id)
  await loadMaterials()
}

onMounted(loadMaterials)
</script>

<template>
  <section class="console-page">
    <article class="hero-panel card">
      <div class="page-head hero-head">
        <div>
          <p class="hero-tag">MATERIAL LIBRARY / 配方原料底池</p>
          <h1 class="page-title">材料管理</h1>
          <p class="page-subtitle">
            统一维护基酒、果汁、糖浆和辅料库，保证鸡尾酒页面与配方采集工作台使用同一套原料语言。
          </p>
        </div>
        <button class="button-primary" @click="openCreate">新增材料</button>
      </div>

      <div class="metric-grid">
        <article class="metric-card">
          <span>材料总数</span>
          <strong>{{ materials.length }}</strong>
          <p>当前筛选条件下的结果集</p>
        </article>
        <article class="metric-card accent">
          <span>已归类材料</span>
          <strong>{{ categorizedCount }}</strong>
          <p>便于配方编辑时快速匹配</p>
        </article>
        <article class="metric-card secondary">
          <span>材料分类</span>
          <strong>{{ categories.length }}</strong>
          <p>支持 spirit / syrup / juice 等标签</p>
        </article>
      </div>
    </article>

    <article class="filter-panel card">
      <div class="panel-headline">
        <div>
          <p class="panel-tag">FILTER</p>
          <h2>筛选材料池</h2>
        </div>
        <button class="button-secondary" :disabled="loading" @click="loadMaterials">
          {{ loading ? '加载中...' : '刷新列表' }}
        </button>
      </div>

      <div class="toolbar">
        <input v-model="keyword" class="field filter-input" type="text" placeholder="搜索材料名称" @keyup.enter="loadMaterials" />
        <select v-model="category" class="select filter-input" @change="loadMaterials">
          <option value="">全部分类</option>
          <option v-for="item in categories" :key="item" :value="item">{{ item }}</option>
        </select>
      </div>
    </article>

    <div class="material-grid">
      <article v-for="item in materials" :key="item.id" class="material-card card">
        <div class="material-topline">
          <span class="category-chip">{{ item.category || '未分类' }}</span>
          <span class="material-id">#{{ item.id }}</span>
        </div>

        <div class="material-body">
          <h3>{{ item.name }}</h3>
          <p>创建于 {{ item.createdAt?.replace('T', ' ') || '-' }}</p>
        </div>

        <div class="actions">
          <button class="button-secondary" @click="openEdit(item)">编辑</button>
          <button class="button-danger" @click="remove(item)">删除</button>
        </div>
      </article>

      <article v-if="!materials.length" class="material-card card empty-card">
        <p class="hero-tag">EMPTY</p>
        <h3>当前筛选下暂无材料</h3>
        <p>可以先新建几条基酒、风味糖浆或果汁数据，再回到鸡尾酒与配方采集页面联动演示。</p>
      </article>
    </div>

    <div v-if="showModal" class="modal-backdrop" @click.self="showModal = false">
      <div class="modal-panel material-modal">
        <div class="panel-headline compact">
          <div>
            <p class="panel-tag">{{ editingId ? 'EDIT MATERIAL' : 'CREATE MATERIAL' }}</p>
            <h2>{{ editingId ? '编辑材料' : '新增材料' }}</h2>
          </div>
          <span class="badge">建议统一命名规范</span>
        </div>

        <div class="form-grid">
          <label>
            <span>材料名称</span>
            <input v-model="form.name" class="field" type="text" placeholder="例如：白朗姆酒" />
          </label>
          <label>
            <span>分类</span>
            <input v-model="form.category" class="field" type="text" placeholder="例如：spirit / juice / syrup" />
          </label>
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
.filter-panel {
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
.material-body p {
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
.material-body p {
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

.filter-input {
  max-width: 280px;
}

.material-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.material-card {
  padding: 20px;
  display: grid;
  gap: 20px;
  background:
    radial-gradient(circle at top right, rgba(72, 215, 255, 0.1), transparent 28%),
    linear-gradient(180deg, rgba(10, 27, 43, 0.92), rgba(7, 18, 31, 0.96));
}

.material-topline,
.actions,
.footer-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.category-chip,
.material-id {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 12px;
  font-weight: 700;
}

.category-chip {
  background: rgba(72, 215, 255, 0.12);
  color: var(--primary);
}

.material-id {
  background: rgba(76, 111, 255, 0.12);
  color: #c9d4ff;
}

.material-body h3 {
  margin: 0 0 10px;
  font-size: 1.36rem;
  letter-spacing: -0.04em;
}

.empty-card {
  align-content: center;
}

.material-modal {
  width: min(720px, 100%);
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

@media (max-width: 960px) {
  .metric-grid,
  .material-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero-panel,
  .filter-panel {
    padding: 18px;
  }

  .panel-headline {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
