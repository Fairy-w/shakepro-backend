<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminApi, type AdminUser, type PageResult } from '@/api/admin'

const keyword = ref('')
const loading = ref(false)
const pageData = ref<PageResult<AdminUser>>({
  content: [],
  totalElements: 0,
  totalPages: 0,
  size: 10,
  number: 0,
})

const enabledCount = computed(() => pageData.value.content.filter((user) => user.enabled).length)
const adminCount = computed(() => pageData.value.content.filter((user) => user.role === 'ADMIN').length)

async function loadUsers(nextPage = 0) {
  loading.value = true
  try {
    pageData.value = await adminApi.getUsers({
      keyword: keyword.value || undefined,
      page: nextPage,
      size: 10,
    })
  } finally {
    loading.value = false
  }
}

onMounted(() => loadUsers())
</script>

<template>
  <section class="console-page">
    <article class="hero-panel card">
      <div class="page-head hero-head">
        <div>
          <p class="hero-tag">USER CONTROL / 运营账户池</p>
          <h1 class="page-title">用户管理</h1>
          <p class="page-subtitle">
            在同一套深海控制台里查看用户注册、角色分布和账号启停状态，方便答辩时直接展示后台治理能力。
          </p>
        </div>
        <button class="button-primary" :disabled="loading" @click="loadUsers(pageData.number)">
          {{ loading ? '刷新中...' : '刷新用户池' }}
        </button>
      </div>

      <div class="metric-grid">
        <article class="metric-card">
          <span>用户总量</span>
          <strong>{{ pageData.totalElements }}</strong>
          <p>按关键词支持快速检索</p>
        </article>
        <article class="metric-card accent">
          <span>当前页启用</span>
          <strong>{{ enabledCount }}</strong>
          <p>停用账号会在列表中高亮标记</p>
        </article>
        <article class="metric-card secondary">
          <span>当前页管理员</span>
          <strong>{{ adminCount }}</strong>
          <p>管理员与普通用户分层可见</p>
        </article>
      </div>
    </article>

    <article class="filter-panel card">
      <div class="panel-headline">
        <div>
          <p class="panel-tag">FILTER</p>
          <h2>搜索与分页</h2>
        </div>
        <span class="badge">第 {{ pageData.number + 1 }} / {{ Math.max(pageData.totalPages, 1) }} 页</span>
      </div>

      <div class="toolbar">
        <input v-model="keyword" class="field search" type="text" placeholder="按用户名或昵称搜索" @keyup.enter="loadUsers()" />
        <button class="button-secondary" :disabled="loading" @click="loadUsers()">{{ loading ? '查询中...' : '立即查询' }}</button>
      </div>
    </article>

    <article class="table-panel card">
      <div class="panel-headline compact">
        <div>
          <p class="panel-tag">ACCOUNT LIST</p>
          <h2>后台用户池</h2>
        </div>
        <span class="badge subtle">共 {{ pageData.content.length }} 条当前页记录</span>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>昵称</th>
              <th>角色</th>
              <th>状态</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in pageData.content" :key="user.id">
              <td>#{{ user.id }}</td>
              <td>
                <div class="cell-stack strong-cell">
                  <strong>{{ user.username }}</strong>
                  <span>账号主体</span>
                </div>
              </td>
              <td>{{ user.nickname || '-' }}</td>
              <td>
                <span class="role-chip" :class="{ admin: user.role === 'ADMIN' }">{{ user.role }}</span>
              </td>
              <td>
                <span class="status-chip" :class="{ disabled: !user.enabled }">
                  {{ user.enabled ? '启用' : '禁用' }}
                </span>
              </td>
              <td>{{ user.createdAt?.replace('T', ' ') || '-' }}</td>
            </tr>
            <tr v-if="!pageData.content.length">
              <td colspan="6" class="empty">当前没有匹配的用户记录</td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <div class="pager card">
      <button class="button-secondary" :disabled="pageData.number <= 0 || loading" @click="loadUsers(pageData.number - 1)">上一页</button>
      <span>第 {{ pageData.number + 1 }} / {{ Math.max(pageData.totalPages, 1) }} 页</span>
      <button
        class="button-secondary"
        :disabled="pageData.number + 1 >= pageData.totalPages || loading || !pageData.totalPages"
        @click="loadUsers(pageData.number + 1)"
      >
        下一页
      </button>
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
    radial-gradient(circle at top right, rgba(72, 215, 255, 0.18), transparent 34%),
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
.metric-card p {
  color: var(--ink-600);
}

.metric-card strong {
  display: block;
  margin: 10px 0 6px;
  font-size: 2.2rem;
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
  max-width: 360px;
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

.cell-stack {
  display: grid;
  gap: 4px;
}

.cell-stack span {
  color: var(--ink-600);
  font-size: 0.85rem;
}

.strong-cell strong {
  color: var(--ink-950);
}

.role-chip,
.status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 74px;
  padding: 7px 12px;
  border-radius: 12px;
  border: 1px solid transparent;
  font-weight: 700;
}

.role-chip {
  background: rgba(76, 111, 255, 0.12);
  color: #bfd0ff;
}

.role-chip.admin {
  background: rgba(255, 182, 72, 0.14);
  color: #ffd79a;
}

.status-chip {
  background: rgba(45, 212, 191, 0.12);
  color: var(--success);
}

.status-chip.disabled {
  background: rgba(255, 107, 125, 0.14);
  color: var(--danger);
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

@media (max-width: 960px) {
  .metric-grid {
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
  .pager {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
