<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminApi, type AdminUser, type PageResult } from '@/api/admin'
import AdminMetricCard from '@/components/admin/AdminMetricCard.vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'

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
const adminCount = computed(() => pageData.value.content.filter((user) => ['ADMIN', 'SUPER_ADMIN'].includes(user.role)).length)

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
  <section class="page-stack users-page">
    <AdminPageHeader
      eyebrow="Members"
      title="用户管理"
      subtitle="查看账号结构、角色状态与创建时间，确保运营侧的成员结构和权限分布始终清晰。"
    >
      <template #meta>
        <span class="badge">已收录 {{ pageData.totalElements }} 位用户</span>
      </template>
    </AdminPageHeader>

    <div class="dual-grid">
      <AdminMetricCard eyebrow="当前页" label="启用账号" :value="enabledCount" hint="优先关注已启用成员的使用情况。" />
      <AdminMetricCard eyebrow="角色" label="管理账号" :value="adminCount" hint="包括 ADMIN 与 SUPER_ADMIN。" tone="warm" />
    </div>

    <AdminToolbar>
      <input v-model="keyword" class="field search" type="text" placeholder="按用户名或昵称搜索" @keyup.enter="loadUsers()" />
      <button class="button-primary" :disabled="loading" @click="loadUsers()">{{ loading ? '查询中...' : '搜索' }}</button>
    </AdminToolbar>

    <div class="card table-card">
      <table class="table-base">
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
            <td class="mono">#{{ user.id }}</td>
            <td>{{ user.username }}</td>
            <td>{{ user.nickname || '-' }}</td>
            <td>{{ user.role }}</td>
            <td>
              <span class="status-pill" :class="user.enabled ? 'success' : 'danger'">
                {{ user.enabled ? '启用' : '禁用' }}
              </span>
            </td>
            <td>{{ user.createdAt?.replace('T', ' ') || '-' }}</td>
          </tr>
          <tr v-if="!pageData.content.length">
            <td colspan="6" class="empty">暂时还没有匹配的用户</td>
          </tr>
        </tbody>
      </table>
    </div>

    <AdminPagination
      :page="pageData.number"
      :total-pages="pageData.totalPages"
      :loading="loading"
      @prev="loadUsers(pageData.number - 1)"
      @next="loadUsers(pageData.number + 1)"
    />
  </section>
</template>

<style scoped>
.users-page {
  display: grid;
  gap: 18px;
}

.search {
  max-width: 340px;
}
</style>
