<script setup lang="ts">
import { onMounted, ref } from 'vue'
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
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">这里先提供后台最常用的用户查看能力，方便你掌握注册情况、角色情况和用户创建时间。</p>
      </div>
      <span class="badge">共 {{ pageData.totalElements }} 位用户</span>
    </div>

    <div class="toolbar">
      <input v-model="keyword" class="field search" type="text" placeholder="按用户名或昵称搜索" @keyup.enter="loadUsers()" />
      <button class="button-primary" :disabled="loading" @click="loadUsers()">{{ loading ? '查询中...' : '查询' }}</button>
    </div>

    <div class="card table-card">
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
            <td>{{ user.username }}</td>
            <td>{{ user.nickname || '-' }}</td>
            <td>{{ user.role }}</td>
            <td>
              <span class="status" :class="{ disabled: !user.enabled }">
                {{ user.enabled ? '启用' : '禁用' }}
              </span>
            </td>
            <td>{{ user.createdAt?.replace('T', ' ') || '-' }}</td>
          </tr>
          <tr v-if="!pageData.content.length">
            <td colspan="6" class="empty">暂无用户数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pager">
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
.search {
  max-width: 320px;
}

.table-card {
  overflow: hidden;
}

th,
td {
  padding: 16px 18px;
  text-align: left;
  border-bottom: 1px solid var(--line);
}

th {
  font-size: 0.86rem;
  letter-spacing: 0.1em;
  color: var(--ink-600);
}

.status {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(22, 163, 74, 0.12);
  color: var(--success);
  font-weight: 700;
}

.status.disabled {
  background: rgba(220, 38, 38, 0.1);
  color: var(--danger);
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

@media (max-width: 900px) {
  .table-card {
    overflow-x: auto;
  }

  .pager {
    flex-direction: column;
  }
}
</style>
