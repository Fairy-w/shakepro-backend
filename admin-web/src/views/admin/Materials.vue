<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { adminApi, type AdminMaterial } from '@/api/admin'
import AdminDialog from '@/components/admin/AdminDialog.vue'
import AdminInspectorPanel from '@/components/admin/AdminInspectorPanel.vue'
import AdminMetricCard from '@/components/admin/AdminMetricCard.vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'
import AdminWorkspaceShell from '@/components/admin/AdminWorkspaceShell.vue'

const loading = ref(false)
const saveLoading = ref(false)
const keyword = ref('')
const category = ref('')
const sourceFilter = ref<'all' | 'thecocktaildb' | 'manual' | 'other'>('all')
const imageFilter = ref<'all' | 'with_image' | 'without_image'>('all')
const materials = ref<AdminMaterial[]>([])
const errorMessage = ref('')
const editingId = ref<number | null>(null)
const showModal = ref(false)
const selectedMaterialId = ref<number | null>(null)
const form = reactive({
  name: '',
  category: '',
})

const categories = computed(() => {
  const values = new Set(materials.value.map((item) => item.category).filter(Boolean) as string[])
  return Array.from(values)
})

const materialStats = computed(() => {
  const total = materials.value.length
  const synced = materials.value.filter((item) => item.source === 'thecocktaildb').length
  const withImage = materials.value.filter((item) => Boolean(item.imageUrl)).length
  const manual = materials.value.filter((item) => !item.source || item.source === 'manual').length
  return { total, synced, withImage, manual }
})

const filteredMaterials = computed(() => {
  return materials.value.filter((item) => {
    const source = (item.source || '').toLowerCase()
    const sourceMatched = sourceFilter.value === 'all'
      || (sourceFilter.value === 'thecocktaildb' && source === 'thecocktaildb')
      || (sourceFilter.value === 'manual' && (!source || source === 'manual'))
      || (sourceFilter.value === 'other' && source && source !== 'thecocktaildb' && source !== 'manual')
    if (!sourceMatched) return false

    if (category.value && item.category !== category.value) return false
    if (keyword.value.trim()) {
      const q = keyword.value.trim().toLowerCase()
      const hay = [item.name, item.nameEn, item.category, item.source, item.sourceId].filter(Boolean).join(' ').toLowerCase()
      if (!hay.includes(q)) return false
    }

    if (imageFilter.value === 'with_image' && !item.imageUrl) return false
    if (imageFilter.value === 'without_image' && item.imageUrl) return false
    return true
  })
})

const selectedMaterial = computed(() => filteredMaterials.value.find((item) => item.id === selectedMaterialId.value) || filteredMaterials.value[0] || null)

const filteredStats = computed(() => {
  const total = filteredMaterials.value.length
  const withImage = filteredMaterials.value.filter((item) => item.imageUrl).length
  const synced = filteredMaterials.value.filter((item) => item.source === 'thecocktaildb').length
  return { total, withImage, synced }
})

watch(filteredMaterials, (items) => {
  if (!items.length) {
    selectedMaterialId.value = null
    return
  }
  const firstItem = items[0]
  if (firstItem && !items.some((item) => item.id === selectedMaterialId.value)) {
    selectedMaterialId.value = firstItem.id
  }
}, { immediate: true })

async function loadMaterials() {
  loading.value = true
  errorMessage.value = ''
  try {
    materials.value = await adminApi.getMaterials()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '材料列表加载失败'
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
  saveLoading.value = true
  errorMessage.value = ''
  try {
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
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存失败'
  } finally {
    saveLoading.value = false
  }
}

async function remove(item: AdminMaterial) {
  if (!window.confirm(`确认删除材料“${item.name}”吗？`)) return
  errorMessage.value = ''
  try {
    await adminApi.deleteMaterial(item.id)
    await loadMaterials()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '删除失败'
  }
}

function sourceLabel(source?: string | null) {
  if (!source || source === 'manual') return '手动维护'
  if (source === 'thecocktaildb') return '词典同步'
  return source
}

onMounted(loadMaterials)
</script>

<template>
  <section class="materials-page">
    <AdminPageHeader
      eyebrow="Materials"
      title="材料管理工作区"
      subtitle="把材料库切成高密度表格和右侧 Inspector：主区处理列表，右侧承接上下文、图片与基础信息。"
    >
      <template #meta>
        <span class="badge">总库 {{ materialStats.total }} 项</span>
        <span class="badge subtle">已筛选 {{ filteredStats.total }} 项</span>
      </template>
      <template #actions>
        <button class="button-primary" @click="openCreate">新建材料</button>
      </template>
    </AdminPageHeader>

    <div class="metric-grid material-metrics">
      <AdminMetricCard eyebrow="基础库" label="总材料数" :value="materialStats.total" hint="酒单、用户画像和抓取结果都会回流到这里。" tone="strong" />
      <AdminMetricCard eyebrow="素材完整度" label="有图片" :value="materialStats.withImage" hint="图片越完整，Cocktails 和 Inspector 的展示面越强。" />
      <AdminMetricCard eyebrow="维护构成" label="手动 / 标准来源" :value="`${materialStats.manual} / ${materialStats.synced}`" hint="弱化同步视角，更多关注当前库的维护结构和一致性。" tone="warm" />
    </div>

    <AdminToolbar>
      <input v-model="keyword" class="field filter-input" type="text" placeholder="搜索中文名 / 英文名 / sourceId" />
      <select v-model="category" class="select filter-input">
        <option value="">全部分类</option>
        <option v-for="item in categories" :key="item" :value="item">{{ item }}</option>
      </select>
      <select v-model="sourceFilter" class="select filter-input">
        <option value="all">全部来源</option>
        <option value="thecocktaildb">词典同步</option>
        <option value="manual">手动维护</option>
        <option value="other">其他来源</option>
      </select>
      <select v-model="imageFilter" class="select filter-input">
        <option value="all">全部图片状态</option>
        <option value="with_image">仅有图片</option>
        <option value="without_image">仅无图片</option>
      </select>
      <button class="button-secondary" :disabled="loading" @click="loadMaterials">
        {{ loading ? '刷新中...' : '刷新列表' }}
      </button>
    </AdminToolbar>

    <p v-if="errorMessage" class="error-tip">{{ errorMessage }}</p>

    <AdminWorkspaceShell>
      <template #main>
        <div class="card table-card material-table-shell">
          <div class="material-table-shell__head">
            <div>
              <p class="material-table-shell__eyebrow">Flagship Table</p>
              <h2>材料总表</h2>
            </div>
            <span class="badge subtle">点击任意行查看 Inspector</span>
          </div>

          <table class="table-base">
            <thead>
              <tr>
                <th>材料</th>
                <th>分类</th>
                <th>来源</th>
                <th>图片</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="item in filteredMaterials"
                :key="item.id"
                class="material-row"
                :class="{ selected: selectedMaterial?.id === item.id }"
                @click="selectedMaterialId = item.id"
              >
                <td>
                  <div class="material-cell">
                    <div class="material-thumb">
                      <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.name" />
                      <span v-else>No Image</span>
                    </div>
                    <div>
                      <strong>{{ item.name }}</strong>
                      <p>{{ item.nameEn || '暂无英文名' }}</p>
                    </div>
                  </div>
                </td>
                <td>{{ item.category || '未分类' }}</td>
                <td><span class="status-pill warn">{{ sourceLabel(item.source) }}</span></td>
                <td>{{ item.imageUrl ? '已上传' : '缺失' }}</td>
                <td class="mono">{{ item.createdAt?.replace('T', ' ') || '-' }}</td>
                <td>
                  <div class="actions" @click.stop>
                    <button class="button-secondary" @click="openEdit(item)">编辑</button>
                    <button class="button-danger" @click="remove(item)">删除</button>
                  </div>
                </td>
              </tr>
              <tr v-if="!filteredMaterials.length">
                <td colspan="6" class="empty">没有符合条件的材料，可以调整筛选条件或新建材料。</td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>

      <template #inspector>
        <AdminInspectorPanel eyebrow="Inspector" title="材料详情" tone="default">
          <template v-if="selectedMaterial">
            <div class="selected-material-head">
              <div class="selected-material-image">
                <img v-if="selectedMaterial.imageUrl" :src="selectedMaterial.imageUrl" :alt="selectedMaterial.name" />
                <span v-else>No Image</span>
              </div>
              <div>
                <h4>{{ selectedMaterial.name }}</h4>
                <p>{{ selectedMaterial.nameEn || '暂无英文名' }}</p>
              </div>
            </div>
            <dl class="inspector-list">
              <div><dt>ID</dt><dd class="mono">#{{ selectedMaterial.id }}</dd></div>
              <div><dt>分类</dt><dd>{{ selectedMaterial.category || '未分类' }}</dd></div>
              <div><dt>来源</dt><dd>{{ sourceLabel(selectedMaterial.source) }}</dd></div>
              <div><dt>sourceId</dt><dd class="mono">{{ selectedMaterial.sourceId || '-' }}</dd></div>
              <div><dt>创建时间</dt><dd>{{ selectedMaterial.createdAt?.replace('T', ' ') || '-' }}</dd></div>
              <div><dt>图片状态</dt><dd>{{ selectedMaterial.imageUrl ? '已上传到 OSS' : '未上传图片' }}</dd></div>
            </dl>
            <div class="actions">
              <button class="button-secondary" @click="openEdit(selectedMaterial)">编辑这项材料</button>
              <button class="button-danger" @click="remove(selectedMaterial)">删除这项材料</button>
            </div>
          </template>
          <p v-else class="empty-copy">还没有可展示的材料，先放宽筛选条件或创建一条新记录。</p>
        </AdminInspectorPanel>
      </template>
    </AdminWorkspaceShell>

    <AdminDialog :visible="showModal" size="md" @close="showModal = false">
      <div class="material-dialog">
        <div class="dialog-head">
          <div>
            <p class="dialog-head__eyebrow">Material Editor</p>
            <h2 class="dialog-head__title">{{ editingId ? '编辑材料' : '新建材料' }}</h2>
            <p class="dialog-head__subtitle">手动维护场景只维护中文名和分类，词典同步字段由系统自动补齐。</p>
          </div>
        </div>

        <section class="dialog-section">
          <div class="dialog-form-grid">
            <label>
              <span>材料名称</span>
              <input v-model="form.name" class="field" type="text" placeholder="例如：白朗姆酒" />
            </label>
            <label>
              <span>分类</span>
              <input v-model="form.category" class="field" type="text" placeholder="例如：基酒 / 果汁 / 糖浆" />
            </label>
          </div>
        </section>

        <div class="dialog-footer">
          <button class="button-secondary" :disabled="saveLoading" @click="showModal = false">取消</button>
          <button class="button-primary" :disabled="saveLoading" @click="submit">
            {{ saveLoading ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </AdminDialog>
  </section>
</template>

<style scoped>
.materials-page {
  display: grid;
  gap: 18px;
}

.material-metrics {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.filter-input {
  max-width: 220px;
}

.error-tip {
  margin: 0;
  color: var(--danger);
  font-weight: 700;
}

.empty-copy {
  margin: 0;
  color: var(--ink-600);
}

.material-dialog {
  display: grid;
  gap: 12px;
}

.material-table-shell {
  padding: 18px;
  min-width: 0;
}

.material-table-shell__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
}

.material-table-shell__eyebrow {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--ink-600);
}

.material-table-shell__head h2 {
  margin: 8px 0 0;
  font-size: 1.55rem;
  line-height: 1;
}

.material-row {
  cursor: pointer;
}

.material-row.selected {
  background: rgba(200, 155, 91, 0.08);
}

.material-cell {
  display: flex;
  gap: 12px;
  align-items: center;
}

.material-thumb {
  width: 54px;
  height: 54px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.68);
  display: grid;
  place-items: center;
  color: var(--ink-500);
  font-size: 0.72rem;
}

.material-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.material-cell p {
  margin: 4px 0 0;
  color: var(--ink-600);
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.selected-material-head {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.selected-material-image {
  width: 92px;
  height: 92px;
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.68);
  display: grid;
  place-items: center;
  color: var(--ink-500);
}

.selected-material-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.selected-material-head h4 {
  margin: 0;
  font-size: 1.42rem;
}

.selected-material-head p {
  margin: 6px 0 0;
  color: var(--ink-600);
}

.inspector-list {
  margin: 0;
  display: grid;
  gap: 10px;
}

.inspector-list div {
  display: grid;
  gap: 4px;
  padding-bottom: 10px;
  border-bottom: 1px dashed var(--line);
}

.inspector-list dt {
  color: var(--ink-600);
  font-size: 0.84rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.inspector-list dd {
  margin: 0;
  color: var(--ink-950);
}

@media (max-width: 960px) {
  .material-metrics {
    grid-template-columns: 1fr;
  }

  .filter-input {
    max-width: none;
  }

  .material-table-shell__head,
  .selected-material-head {
    grid-template-columns: 1fr;
  }
}
</style>
