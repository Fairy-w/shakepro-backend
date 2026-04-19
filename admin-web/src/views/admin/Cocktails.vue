<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  adminApi,
  type AdminCocktailDetail,
  type AdminCocktailListItem,
  type AdminMaterial,
  type PageResult,
} from '@/api/admin'
import { buildGeneratedPayloadFromForm, type GeneratedEditorFormSnapshot } from './cocktailPayload'
import AdminMetricCard from '@/components/admin/AdminMetricCard.vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'
import CocktailListCard from './cocktails/CocktailListCard.vue'
import CocktailEditorModal from './cocktails/CocktailEditorModal.vue'
import CocktailDetailModal from './cocktails/CocktailDetailModal.vue'

const loading = ref(false)
const materialsLoading = ref(false)
const submitting = ref(false)
const uploadingHeroImage = ref(false)
const uploadImageError = ref('')
const uploadImageSuccess = ref('')
const keyword = ref('')
const category = ref('')
const categoryOptions = ref<string[]>([])
const pageData = ref<PageResult<AdminCocktailListItem>>({
  content: [],
  totalElements: 0,
  totalPages: 0,
  size: 10,
  number: 0,
})

const materialOptions = ref<AdminMaterial[]>([])
const showEditorModal = ref(false)
const showDetailModal = ref(false)
const editingId = ref<number | null>(null)
const editingFromDetail = ref(false)
const submitError = ref('')
const detailLoading = ref(false)
const selectedDetail = ref<AdminCocktailDetail | null>(null)

const MAX_IMAGE_SIZE = 10 * 1024 * 1024
const ALLOWED_IMAGE_TYPES = new Set([
  'image/jpeg',
  'image/png',
  'image/gif',
  'image/webp',
  'image/svg+xml',
])

function createInitialFormState(): GeneratedEditorFormSnapshot {
  return {
    name: '',
    englishName: '',
    category: '',
    heroImage: '',
    difficulty: '',
    abv: '',
    glass: '',
    garnish: '',
    highlight: '',
    subtitle: '',
    description: '',
    story: '',
    flavorTagsText: '',
    pairingsText: '',
    serviceNotesText: '',
    flavorMetrics: [{ name: '', value: '' }],
    ingredients: [{ materialId: '', name: '', amount: '', note: '' }],
    steps: [{ title: '', detail: '' }],
  }
}

const form = reactive<GeneratedEditorFormSnapshot>(createInitialFormState())

const hasMaterialOptions = computed(() => materialOptions.value.length > 0)
const visibleCount = computed(() => pageData.value.content.length)
const formCompletion = computed(() => {
  const checks = [
    Boolean(form.name.trim()),
    Boolean(form.category.trim()),
    Boolean(form.description.trim()),
    Boolean(form.heroImage.trim()),
    form.ingredients.some((item) => item.name.trim()),
    form.steps.some((item) => item.detail.trim()),
  ]
  return Math.round((checks.filter(Boolean).length / checks.length) * 100)
})

async function loadCocktails(nextPage = 0) {
  loading.value = true
  try {
    pageData.value = await adminApi.getCocktails({
      keyword: keyword.value || undefined,
      category: category.value || undefined,
      page: nextPage,
      size: 8,
    })
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  categoryOptions.value = await adminApi.getCocktailCategories()
}

async function loadMaterials() {
  materialsLoading.value = true
  try {
    materialOptions.value = await adminApi.getMaterials()
  } finally {
    materialsLoading.value = false
  }
}

function patchFormState(next: GeneratedEditorFormSnapshot) {
  Object.assign(form, next)
}

function resetForm() {
  editingId.value = null
  submitError.value = ''
  uploadImageError.value = ''
  uploadImageSuccess.value = ''
  patchFormState(createInitialFormState())
}

function openCreate() {
  resetForm()
  editingFromDetail.value = false
  showEditorModal.value = true
}

async function openEdit(item: AdminCocktailListItem) {
  uploadImageError.value = ''
  uploadImageSuccess.value = ''
  const detail = await adminApi.getCocktail(item.id)
  fillForm(detail)
  editingId.value = item.id
  editingFromDetail.value = false
  showEditorModal.value = true
}

function fillForm(detail: AdminCocktailDetail) {
  patchFormState({
    name: detail.name || '',
    englishName: detail.englishName || '',
    category: detail.category || '',
    heroImage: detail.heroImage || detail.imageUrl || '',
    difficulty: detail.difficulty || '',
    abv: detail.abv || (detail.alcoholLevel != null ? `${detail.alcoholLevel}%` : ''),
    glass: detail.glass || '',
    garnish: detail.garnish || '',
    highlight: detail.highlight || '',
    subtitle: detail.subtitle || '',
    description: detail.description || '',
    story: detail.story || '',
    flavorTagsText: (detail.flavorTags || []).join('\n'),
    pairingsText: (detail.pairings || []).join('\n'),
    serviceNotesText: (detail.serviceNotes || []).join('\n'),
    flavorMetrics: detail.flavorMetrics?.length
      ? detail.flavorMetrics.map((item) => ({ name: item.name || '', value: item.value ?? '' }))
      : [{ name: '', value: '' }],
    ingredients: detail.materials.length
      ? detail.materials.map((item) => ({
          materialId: item.materialId ?? '',
          name: item.displayName || item.name || '',
          amount: item.amount || '',
          note: item.note || '',
        }))
      : [{ materialId: '', name: '', amount: '', note: '' }],
    steps: detail.steps?.length
      ? detail.steps.map((item) => ({ title: item.title || '', detail: item.detail || '' }))
      : detail.legacySteps
        ? detail.legacySteps.split(/\r?\n/).filter(Boolean).map((line) => ({ title: '', detail: line }))
        : [{ title: '', detail: '' }],
  })
}

async function submit() {
  submitError.value = ''
  uploadImageError.value = ''
  uploadImageSuccess.value = ''
  if (!form.name.trim()) {
    submitError.value = '名称不能为空'
    return
  }
  if (uploadingHeroImage.value) {
    submitError.value = '图片上传中，请稍后再保存'
    return
  }

  const payload = buildGeneratedPayloadFromForm(form)
  if (!payload.ingredients.length) {
    submitError.value = '至少填写一条材料'
    return
  }
  if (!payload.steps.length) {
    submitError.value = '至少填写一步制作步骤'
    return
  }

  submitting.value = true
  try {
    const currentEditingId = editingId.value
    let saved: AdminCocktailDetail
    if (editingId.value) {
      saved = await adminApi.updateGeneratedCocktail(editingId.value, payload)
    } else {
      saved = await adminApi.createGeneratedCocktail(payload)
    }

    if (currentEditingId && selectedDetail.value?.id === currentEditingId) {
      selectedDetail.value = saved
    }
    if (editingFromDetail.value && currentEditingId) {
      selectedDetail.value = saved
      showDetailModal.value = true
    }

    editingFromDetail.value = false
    showEditorModal.value = false
    await loadCocktails(pageData.value.number)
  } catch (error) {
    submitError.value = error instanceof Error ? error.message : '保存失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

async function uploadHeroImage(file: File) {
  uploadImageError.value = ''
  uploadImageSuccess.value = ''

  if (!ALLOWED_IMAGE_TYPES.has(file.type)) {
    uploadImageError.value = '仅支持 JPEG、PNG、GIF、WebP、SVG 图片'
    return
  }
  if (file.size > MAX_IMAGE_SIZE) {
    uploadImageError.value = '图片大小不能超过 10MB'
    return
  }

  uploadingHeroImage.value = true
  try {
    const presign = await adminApi.createOssPresign({
      filename: file.name,
      contentType: file.type,
      size: file.size,
    })

    await adminApi.uploadToOss(presign.uploadUrl, file, file.type)
    await adminApi.saveOssFileRecord({
      objectKey: presign.objectKey,
      url: presign.publicUrl,
      contentType: file.type,
      size: file.size,
    })

    form.heroImage = presign.publicUrl
    uploadImageSuccess.value = '图片上传成功，主图地址已自动填充'
  } catch (error) {
    uploadImageError.value = error instanceof Error ? error.message : '图片上传失败，请稍后重试'
  } finally {
    uploadingHeroImage.value = false
  }
}

async function remove(item: AdminCocktailListItem) {
  if (!window.confirm(`确认删除鸡尾酒“${item.name}”吗？`)) return
  await adminApi.deleteCocktail(item.id)
  await loadCocktails(Math.max(pageData.value.number - (pageData.value.content.length === 1 ? 1 : 0), 0))
}

async function openDetail(item: AdminCocktailListItem) {
  showDetailModal.value = true
  detailLoading.value = true
  try {
    selectedDetail.value = await adminApi.getCocktail(item.id)
  } finally {
    detailLoading.value = false
  }
}

function editFromDetail() {
  if (!selectedDetail.value) return
  fillForm(selectedDetail.value)
  editingId.value = selectedDetail.value.id
  editingFromDetail.value = true
  showDetailModal.value = false
  showEditorModal.value = true
}

function closeEditor() {
  showEditorModal.value = false
  submitError.value = ''
  uploadImageError.value = ''
  uploadImageSuccess.value = ''
  if (editingFromDetail.value) {
    showDetailModal.value = true
  }
  editingFromDetail.value = false
}

function searchCocktails() {
  loadCocktails(0)
}

function resetFilters() {
  keyword.value = ''
  category.value = ''
  loadCocktails(0)
}

onMounted(async () => {
  await Promise.all([loadCocktails(), loadMaterials(), loadCategories()])
})
</script>

<template>
  <section class="cocktails-page">
    <AdminPageHeader
      eyebrow="Cocktail Library"
      title="鸡尾酒库管理"
      subtitle="保留可视化卡片浏览，但把操作结构收束到更克制的旗舰工作区里。"
    >
      <template #meta>
        <span class="badge">库内总数 {{ pageData.totalElements }}</span>
        <span class="badge subtle">可关联材料 {{ materialOptions.length }}</span>
      </template>
      <template #actions>
        <button class="button-primary create-button" :disabled="materialsLoading || !hasMaterialOptions" @click="openCreate">
          {{ materialsLoading ? '材料准备中...' : '新建鸡尾酒' }}
        </button>
      </template>
    </AdminPageHeader>

    <div class="dual-grid spotlight-grid">
      <AdminMetricCard eyebrow="当前页" label="展示数量" :value="visibleCount" hint="保留图卡浏览，快速发现需要回看的条目。" tone="strong" />
      <AdminMetricCard eyebrow="表单准备" label="材料选项" :value="materialOptions.length" hint="关联材料越完整，编辑过程越顺滑。" tone="warm" />
    </div>

    <AdminToolbar>
      <input v-model="keyword" class="field search" type="text" placeholder="按名称搜索鸡尾酒" @keyup.enter="searchCocktails" />
      <select v-model="category" class="select category-select" @change="searchCocktails">
        <option value="">全部类别</option>
        <option v-for="item in categoryOptions" :key="item" :value="item">{{ item }}</option>
      </select>
      <button class="button-secondary" :disabled="loading" @click="searchCocktails">
        {{ loading ? '查询中...' : '搜索' }}
      </button>
      <button class="button-ghost" :disabled="loading" @click="resetFilters">重置筛选</button>
    </AdminToolbar>

    <div class="cocktail-grid">
      <CocktailListCard
        v-for="item in pageData.content"
        :key="item.id"
        :item="item"
        @view="openDetail"
        @edit="openEdit"
        @remove="remove"
      />

      <article v-if="!pageData.content.length" class="cocktail-card card empty-card">
        <h3>还没有鸡尾酒内容</h3>
        <p>可以先新建 1-2 款主打酒单，后续通过抓取页导入会更高效。</p>
      </article>
    </div>

    <AdminPagination
      :page="pageData.number"
      :total-pages="pageData.totalPages"
      :loading="loading"
      @prev="loadCocktails(pageData.number - 1)"
      @next="loadCocktails(pageData.number + 1)"
    />

    <CocktailEditorModal
      :visible="showEditorModal"
      :editing="editingId !== null"
      :form="form"
      :material-options="materialOptions"
      :submitting="submitting"
      :uploading-hero-image="uploadingHeroImage"
      :upload-image-error="uploadImageError"
      :upload-image-success="uploadImageSuccess"
      :submit-error="submitError"
      :form-completion="formCompletion"
      @close="closeEditor"
      @submit="submit"
      @upload-hero-image="uploadHeroImage"
    />

    <CocktailDetailModal
      :visible="showDetailModal"
      :loading="detailLoading"
      :detail="selectedDetail"
      @close="showDetailModal = false"
      @edit="editFromDetail"
    />
  </section>
</template>

<style scoped>
.cocktails-page {
  display: grid;
  gap: 18px;
}

.spotlight-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.search {
  max-width: 340px;
}

.category-select {
  min-width: 180px;
}

.cocktail-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

.empty-card {
  padding: 24px;
}

.empty-card h3 {
  margin: 0 0 8px;
  font-size: 1.45rem;
}

.empty-card p {
  margin: 0;
  color: var(--ink-600);
}

@media (max-width: 960px) {
  .spotlight-grid,
  .cocktail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
