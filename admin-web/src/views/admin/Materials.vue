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
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">材料管理</h1>
        <p class="page-subtitle">维护基础材料库，让新增配方时能直接复用统一的名称和分类。</p>
      </div>
      <button class="button-primary" @click="openCreate">新建材料</button>
    </div>

    <div class="toolbar">
      <input v-model="keyword" class="field filter-input" type="text" placeholder="搜索材料名称" @keyup.enter="loadMaterials" />
      <select v-model="category" class="select filter-input" @change="loadMaterials">
        <option value="">全部分类</option>
        <option v-for="item in categories" :key="item" :value="item">{{ item }}</option>
      </select>
      <button class="button-secondary" :disabled="loading" @click="loadMaterials">
        {{ loading ? '加载中...' : '刷新列表' }}
      </button>
    </div>

    <div class="material-grid">
      <article v-for="item in materials" :key="item.id" class="material-card card">
        <div>
          <span class="badge">{{ item.category || '未分类' }}</span>
          <h3>{{ item.name }}</h3>
          <p>创建于 {{ item.createdAt?.replace('T', ' ') || '-' }}</p>
        </div>
        <div class="actions">
          <button class="button-secondary" @click="openEdit(item)">编辑</button>
          <button class="button-danger" @click="remove(item)">删除</button>
        </div>
      </article>
      <article v-if="!materials.length" class="material-card card empty-card">
        <h3>还没有材料内容</h3>
        <p>先补充几种常用基酒、果汁或糖浆，后面录入配方会更顺手。</p>
      </article>
    </div>

    <div v-if="showModal" class="modal-backdrop" @click.self="showModal = false">
      <div class="modal-panel">
        <div class="page-head compact">
          <div>
            <h2 class="page-title">{{ editingId ? '编辑材料' : '新建材料' }}</h2>
            <p class="page-subtitle">名称和分类越统一，后续筛选、录入和复用就越轻松。</p>
          </div>
        </div>

        <div class="form-grid">
          <label>
            <span>材料名称</span>
            <input v-model="form.name" class="field" type="text" placeholder="例如：白朗姆酒" />
          </label>
          <label>
            <span>分类</span>
            <input v-model="form.category" class="field" type="text" placeholder="例如：基酒 / 果汁 / 糖浆" />
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
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 18px;
  transition: transform 0.22s ease, box-shadow 0.22s ease, background 0.22s ease;
}

.material-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 24px 50px rgba(16, 32, 46, 0.12);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(240, 246, 250, 0.72));
}

.material-card h3 {
  margin-top: 14px;
  font-size: 1.3rem;
  letter-spacing: -0.04em;
}

.material-card p {
  margin-top: 10px;
  color: var(--ink-600);
}

.actions,
.footer-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
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

.compact {
  margin-bottom: 18px;
}

.empty-card {
  align-items: flex-start;
}

@media (max-width: 960px) {
  .material-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
