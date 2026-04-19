<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { adminApi, type AdminMaterial, type AdminUser, type UserMaterialItem } from '@/api/admin'
import AdminInspectorPanel from '@/components/admin/AdminInspectorPanel.vue'
import AdminMetricCard from '@/components/admin/AdminMetricCard.vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'
import AdminWorkspaceShell from '@/components/admin/AdminWorkspaceShell.vue'

const userLoading = ref(false)
const loading = ref(false)
const userKeyword = ref('')
const materialKeyword = ref('')
const categoryId = ref('')
const users = ref<AdminUser[]>([])
const selectedUserId = ref<number | null>(null)
const selectedItemId = ref<number | null>(null)
const userMaterials = ref<UserMaterialItem[]>([])
const baseMaterials = ref<AdminMaterial[]>([])

const appUsers = computed(() => {
  const nonAdmin = users.value.filter((item) => item.role !== 'ADMIN' && item.role !== 'SUPER_ADMIN')
  return nonAdmin.length ? nonAdmin : users.value
})

const selectedUser = computed(() => appUsers.value.find((item) => item.id === selectedUserId.value) || null)

const categories = computed(() => {
  const values = new Set(
    userMaterials.value
      .map((item) => item.categoryId)
      .filter((value): value is string => Boolean(value)),
  )
  return Array.from(values)
})

const filteredMaterials = computed(() => {
  return userMaterials.value.filter((item) => {
    if (categoryId.value && item.categoryId !== categoryId.value) return false
    if (!materialKeyword.value.trim()) return true
    const q = materialKeyword.value.trim().toLowerCase()
    const hay = [item.name, item.brand, item.categoryId, ...(item.tags || [])].filter(Boolean).join(' ').toLowerCase()
    return hay.includes(q)
  })
})

const selectedItem = computed(() => filteredMaterials.value.find((item) => item.id === selectedItemId.value) || filteredMaterials.value[0] || null)

const totalCount = computed(() => filteredMaterials.value.length)
const inStockCount = computed(() => filteredMaterials.value.filter((item) => item.hasItem).length)
const coveredMaterialIds = computed(() => {
  const values = new Set<number>()
  filteredMaterials.value.forEach((item) => {
    if (typeof item.materialId === 'number') {
      values.add(item.materialId)
    }
  })
  return values
})
const baseCoverage = computed(() => {
  if (!baseMaterials.value.length) return 0
  return Math.round((coveredMaterialIds.value.size / baseMaterials.value.length) * 100)
})

const topTags = computed(() => {
  const counter = new Map<string, number>()
  filteredMaterials.value.forEach((item) => {
    ;(item.tags || []).forEach((tag) => {
      const key = tag.trim()
      if (!key) return
      counter.set(key, (counter.get(key) || 0) + 1)
    })
  })
  return Array.from(counter.entries())
    .sort((a, b) => b[1] - a[1])
    .slice(0, 8)
})

const categoryStats = computed(() => {
  const counter = new Map<string, number>()
  filteredMaterials.value.forEach((item) => {
    const key = item.categoryId || 'other'
    counter.set(key, (counter.get(key) || 0) + 1)
  })
  return Array.from(counter.entries())
    .sort((a, b) => b[1] - a[1])
    .map(([name, count]) => ({
      name,
      count,
      percent: totalCount.value ? Math.round((count / totalCount.value) * 100) : 0,
    }))
})

const inStockRatio = computed(() => (totalCount.value ? Math.round((inStockCount.value / totalCount.value) * 100) : 0))

watch(filteredMaterials, (items) => {
  if (!items.length) {
    selectedItemId.value = null
    return
  }
  const firstItem = items[0]
  if (firstItem && !items.some((item) => item.id === selectedItemId.value)) {
    selectedItemId.value = firstItem.id
  }
}, { immediate: true })

function requireUserId() {
  if (!selectedUserId.value) {
    userMaterials.value = []
    return null
  }
  return selectedUserId.value
}

async function loadUsers() {
  userLoading.value = true
  try {
    const page = await adminApi.getUsers({
      keyword: userKeyword.value || undefined,
      page: 0,
      size: 100,
    })
    users.value = page.content || []
    const firstAppUser = appUsers.value[0]
    if (!selectedUserId.value && firstAppUser) {
      selectedUserId.value = firstAppUser.id
    } else if (selectedUserId.value && !appUsers.value.some((item) => item.id === selectedUserId.value)) {
      selectedUserId.value = firstAppUser?.id || null
    }
  } finally {
    userLoading.value = false
  }
}

async function loadBaseMaterials() {
  baseMaterials.value = await adminApi.getMaterials()
}

async function loadUserMaterials() {
  const userId = requireUserId()
  if (!userId) return

  loading.value = true
  try {
    userMaterials.value = await adminApi.getUserMaterials(userId)
  } finally {
    loading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadUsers(), loadBaseMaterials()])
  await loadUserMaterials()
}

watch(selectedUserId, () => {
  materialKeyword.value = ''
  categoryId.value = ''
  loadUserMaterials()
})

onMounted(refreshAll)
</script>

<template>
  <section class="user-materials-page">
    <AdminPageHeader
      eyebrow="User Materials"
      title="用户材料分析工作区"
      subtitle="保持只读分析视角：左边看高密度清单，右边用 Inspector 承接用户画像、覆盖率和单项详情。"
    >
      <template #meta>
        <span class="badge">当前对象 {{ selectedUser ? selectedUser.nickname || selectedUser.username : '未选择' }}</span>
      </template>
      <template #actions>
        <button class="button-secondary" :disabled="loading || userLoading" @click="refreshAll">
          {{ loading || userLoading ? '刷新中...' : '刷新数据' }}
        </button>
      </template>
    </AdminPageHeader>

    <div class="metric-grid compact-metrics">
      <AdminMetricCard eyebrow="分析范围" label="材料总数" :value="totalCount" hint="当前筛选后可用于画像分析的材料条目。" tone="strong" />
      <AdminMetricCard eyebrow="拥有率" label="持有比例" :value="`${inStockRatio}%`" :hint="`${inStockCount} / ${totalCount || 0} 标记为已拥有。`" tone="warm" />
      <AdminMetricCard eyebrow="标准命中" label="基础库覆盖" :value="`${baseCoverage}%`" :hint="`${coveredMaterialIds.size} / ${baseMaterials.length || 0} 标准材料已映射。`" />
    </div>

    <AdminToolbar>
      <input v-model="userKeyword" class="field filter-input" placeholder="搜索用户（用户名 / 昵称）" @keyup.enter="loadUsers" />
      <button class="button-secondary" :disabled="userLoading" @click="loadUsers">{{ userLoading ? '查询中...' : '查询用户' }}</button>
      <select v-model.number="selectedUserId" class="select user-select">
        <option :value="null">请选择用户</option>
        <option v-for="user in appUsers" :key="user.id" :value="user.id">
          #{{ user.id }} {{ user.nickname || user.username }}（{{ user.username }}）
        </option>
      </select>
      <input v-model="materialKeyword" class="field filter-input" placeholder="搜索材料名称 / 品牌 / 标签" />
      <select v-model="categoryId" class="select filter-input">
        <option value="">全部分类</option>
        <option v-for="item in categories" :key="item" :value="item">{{ item }}</option>
      </select>
    </AdminToolbar>

    <AdminWorkspaceShell>
      <template #sidebar>
        <AdminInspectorPanel eyebrow="Persona" title="当前分析对象" tone="warm">
          <template v-if="selectedUser">
            <div class="persona-head">
              <strong>{{ selectedUser.nickname || selectedUser.username }}</strong>
              <span>#{{ selectedUser.id }} / {{ selectedUser.username }}</span>
            </div>
            <p class="persona-copy">本页只读，不直接修改用户材料，重点用于分析覆盖率、标签和配方推荐基础。</p>
            <div class="tag-wrap" v-if="topTags.length">
              <span v-for="[tag, count] in topTags" :key="tag" class="badge subtle">{{ tag }} · {{ count }}</span>
            </div>
            <p v-else class="empty-copy">当前筛选下暂无高频标签。</p>
          </template>
          <p v-else class="empty-copy">请先从上方选择一个用户。</p>
        </AdminInspectorPanel>
      </template>

      <template #main>
        <div class="card table-card material-table-shell">
          <div class="material-table-shell__head">
            <div>
              <p class="material-table-shell__eyebrow">Persona Table</p>
              <h2>用户材料总表</h2>
            </div>
            <span class="badge subtle">点击行查看 Inspector</span>
          </div>

          <table class="table-base">
            <thead>
              <tr>
                <th>材料</th>
                <th>品牌</th>
                <th>分类</th>
                <th>状态</th>
                <th>标签</th>
                <th>更新时间</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="item in filteredMaterials"
                :key="item.id"
                class="user-material-row"
                :class="{ selected: selectedItem?.id === item.id }"
                @click="selectedItemId = item.id"
              >
                <td>
                  <div class="material-main">
                    <strong>{{ item.name }}</strong>
                    <p>{{ item.capacityText || '未标记容量' }}</p>
                  </div>
                </td>
                <td>{{ item.brand || '-' }}</td>
                <td>{{ item.categoryId || 'other' }}</td>
                <td>
                  <span class="status-pill" :class="item.hasItem ? 'success' : 'danger'">
                    {{ item.hasItem ? '已拥有' : '未拥有' }}
                  </span>
                </td>
                <td>{{ (item.tags || []).join(' / ') || '-' }}</td>
                <td class="mono">{{ item.updatedAt?.replace('T', ' ') || '-' }}</td>
              </tr>
              <tr v-if="!filteredMaterials.length">
                <td colspan="6" class="empty">该用户在当前筛选条件下没有材料数据。</td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>

      <template #inspector>
        <AdminInspectorPanel eyebrow="Coverage" title="分类分布" tone="default">
          <div v-if="categoryStats.length" class="stats-list">
            <div v-for="item in categoryStats" :key="item.name" class="stats-row">
              <span>{{ item.name }}</span>
              <span>{{ item.count }}（{{ item.percent }}%）</span>
            </div>
          </div>
          <p v-else class="empty-copy">当前筛选下暂无分类数据。</p>
        </AdminInspectorPanel>

        <AdminInspectorPanel eyebrow="Inspector" title="材料详情" tone="default">
          <template v-if="selectedItem">
            <div class="detail-head">
              <strong>{{ selectedItem.name }}</strong>
              <span>{{ selectedItem.brand || '未标记品牌' }}</span>
            </div>
            <dl class="inspector-list">
              <div><dt>状态</dt><dd>{{ selectedItem.hasItem ? '已拥有' : '未拥有' }}</dd></div>
              <div><dt>分类</dt><dd>{{ selectedItem.categoryId || 'other' }}</dd></div>
              <div><dt>容量</dt><dd>{{ selectedItem.capacityText || '-' }}</dd></div>
              <div><dt>余量</dt><dd>{{ selectedItem.remainLevel || '-' }}</dd></div>
              <div><dt>是否开封</dt><dd>{{ selectedItem.opened ? '已开封' : '未开封' }}</dd></div>
              <div><dt>标准映射</dt><dd>{{ selectedItem.materialId || '未命中标准材料' }}</dd></div>
              <div><dt>更新时间</dt><dd class="mono">{{ selectedItem.updatedAt?.replace('T', ' ') || '-' }}</dd></div>
            </dl>
            <div class="tag-wrap" v-if="selectedItem.tags?.length">
              <span v-for="tag in selectedItem.tags" :key="tag" class="badge">{{ tag }}</span>
            </div>
          </template>
          <p v-else class="empty-copy">先在左侧表格里选择一项材料。</p>
        </AdminInspectorPanel>
      </template>
    </AdminWorkspaceShell>
  </section>
</template>

<style scoped>
.user-materials-page {
  display: grid;
  gap: 18px;
}

.compact-metrics {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.user-select {
  min-width: 320px;
}

.filter-input {
  max-width: 260px;
}

.persona-head,
.detail-head,
.material-main {
  display: grid;
  gap: 4px;
}

.persona-head span,
.detail-head span,
.material-main p,
.empty-copy,
.persona-copy {
  margin: 0;
  color: var(--ink-600);
}

.material-table-shell {
  padding: 18px;
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

.user-material-row {
  cursor: pointer;
}

.user-material-row.selected {
  background: rgba(200, 155, 91, 0.08);
}

.stats-list {
  display: grid;
  gap: 8px;
}

.stats-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed var(--line);
}

.tag-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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
}

@media (max-width: 1080px) {
  .compact-metrics {
    grid-template-columns: 1fr;
  }

  .user-select,
  .filter-input {
    min-width: 0;
    max-width: none;
  }
}
</style>
