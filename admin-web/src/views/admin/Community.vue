<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminApi, type AdminCommunityPostDetail, type AdminCommunityPostListItem, type PageResult } from '@/api/admin'
import AdminMetricCard from '@/components/admin/AdminMetricCard.vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'

const loadingList = ref(false)
const loadingDetail = ref(false)
const actionLoading = ref(false)
const actionMessage = ref('')
const actionError = ref('')

const keyword = ref('')
const authorKeyword = ref('')
const statusFilter = ref<'ALL' | 'PUBLISHED' | 'OFFLINE'>('ALL')

const pageData = ref<PageResult<AdminCommunityPostListItem>>({
  content: [],
  totalElements: 0,
  totalPages: 0,
  size: 10,
  number: 0,
})

const selectedPostId = ref('')
const detail = ref<AdminCommunityPostDetail | null>(null)

const publishedCount = computed(() => pageData.value.content.filter((item) => item.status === 'PUBLISHED').length)
const offlineCount = computed(() => pageData.value.content.filter((item) => item.status === 'OFFLINE').length)

async function loadPosts(page = pageData.value.number) {
  loadingList.value = true
  actionError.value = ''
  try {
    pageData.value = await adminApi.getCommunityPosts({
      keyword: keyword.value.trim() || undefined,
      authorKeyword: authorKeyword.value.trim() || undefined,
      status: statusFilter.value === 'ALL' ? undefined : statusFilter.value,
      page,
      size: pageData.value.size || 10,
    })

    const existsInPage = pageData.value.content.some((item) => item.postId === selectedPostId.value)
    if (!existsInPage) {
      selectedPostId.value = pageData.value.content[0]?.postId || ''
    }
    if (selectedPostId.value) {
      await loadDetail(selectedPostId.value)
    } else {
      detail.value = null
    }
  } catch (error: any) {
    actionError.value = error?.message || '加载社区帖子失败'
  } finally {
    loadingList.value = false
  }
}

async function loadDetail(postId: string) {
  loadingDetail.value = true
  selectedPostId.value = postId
  actionError.value = ''
  try {
    detail.value = await adminApi.getCommunityPostDetail(postId)
  } catch (error: any) {
    detail.value = null
    actionError.value = error?.message || '加载帖子详情失败'
  } finally {
    loadingDetail.value = false
  }
}

function toDisplayTime(value?: string | null): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ')
}

function statusLabel(status?: string): string {
  return status === 'OFFLINE' ? '已下线' : '已发布'
}

async function updateStatus(post: AdminCommunityPostListItem, status: 'PUBLISHED' | 'OFFLINE') {
  if (post.status === status) {
    return
  }
  actionLoading.value = true
  actionError.value = ''
  actionMessage.value = ''
  try {
    await adminApi.updateCommunityPostStatus(post.postId, status)
    actionMessage.value = status === 'OFFLINE' ? '帖子已下线' : '帖子已恢复发布'
    await loadPosts(pageData.value.number)
  } catch (error: any) {
    actionError.value = error?.message || '更新状态失败'
  } finally {
    actionLoading.value = false
  }
}

async function deletePost(post: AdminCommunityPostListItem) {
  const confirmed = window.confirm(`确认删除帖子「${post.title || post.postId}」吗？删除后不可恢复。`)
  if (!confirmed) {
    return
  }

  actionLoading.value = true
  actionError.value = ''
  actionMessage.value = ''
  try {
    await adminApi.deleteCommunityPost(post.postId)
    actionMessage.value = '帖子已删除'

    const hasOnlyOneRow = pageData.value.content.length === 1
    const hasPrevPage = pageData.value.number > 0
    const targetPage = hasOnlyOneRow && hasPrevPage ? pageData.value.number - 1 : pageData.value.number
    await loadPosts(targetPage)
  } catch (error: any) {
    actionError.value = error?.message || '删除帖子失败'
  } finally {
    actionLoading.value = false
  }
}

onMounted(() => {
  loadPosts(0)
})
</script>

<template>
  <section class="page-stack community-page">
    <AdminPageHeader
      eyebrow="Community Moderation"
      title="社区内容审核管理"
      subtitle="管理用户发布内容：支持关键词搜索、按作者筛选、详情查看、下线与删除。"
    >
      <template #meta>
        <span class="badge">共 {{ pageData.totalElements }} 篇</span>
      </template>
    </AdminPageHeader>

    <div class="dual-grid">
      <AdminMetricCard eyebrow="当前页" label="已发布" :value="publishedCount" hint="状态为 PUBLISHED 的帖子。" />
      <AdminMetricCard eyebrow="当前页" label="已下线" :value="offlineCount" hint="状态为 OFFLINE 的帖子。" tone="warm" />
    </div>

    <AdminToolbar>
      <input v-model.trim="keyword" class="field search" type="text" placeholder="搜索标题 / 摘要 / 正文关键词" @keyup.enter="loadPosts(0)" />
      <input v-model.trim="authorKeyword" class="field search" type="text" placeholder="按作者用户名 / 昵称筛选" @keyup.enter="loadPosts(0)" />
      <select v-model="statusFilter" class="select compact">
        <option value="ALL">全部状态</option>
        <option value="PUBLISHED">已发布</option>
        <option value="OFFLINE">已下线</option>
      </select>
      <button class="button-primary" :disabled="loadingList" @click="loadPosts(0)">{{ loadingList ? '查询中...' : '搜索' }}</button>
      <span v-if="actionMessage" class="status-pill success">{{ actionMessage }}</span>
      <span v-if="actionError" class="status-pill danger">{{ actionError }}</span>
    </AdminToolbar>

    <div class="split-grid stack-top">
      <div class="card table-card">
        <table class="table-base">
          <thead>
            <tr>
              <th>帖子</th>
              <th>作者</th>
              <th>状态</th>
              <th>发布时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="post in pageData.content"
              :key="post.postId"
              :class="{ selected: post.postId === selectedPostId }"
              @click="loadDetail(post.postId)"
            >
              <td>
                <strong>{{ post.title || '(无标题)' }}</strong>
                <p class="post-summary">{{ post.summary || '-' }}</p>
                <span class="mono tiny">{{ post.postId }}</span>
              </td>
              <td>
                <div>{{ post.author?.nickname || '-' }}</div>
                <small class="mono tiny">{{ post.author?.username || '-' }}</small>
              </td>
              <td>
                <span class="status-pill" :class="post.status === 'OFFLINE' ? 'warn' : 'success'">
                  {{ statusLabel(post.status) }}
                </span>
              </td>
              <td>{{ toDisplayTime(post.publishTime) }}</td>
              <td @click.stop>
                <div class="row-actions">
                  <button
                    class="button-ghost mini"
                    :disabled="actionLoading || post.status === 'PUBLISHED'"
                    @click="updateStatus(post, 'PUBLISHED')"
                  >
                    恢复发布
                  </button>
                  <button
                    class="button-secondary mini"
                    :disabled="actionLoading || post.status === 'OFFLINE'"
                    @click="updateStatus(post, 'OFFLINE')"
                  >
                    下线
                  </button>
                  <button class="button-danger mini" :disabled="actionLoading" @click="deletePost(post)">删除</button>
                </div>
              </td>
            </tr>
            <tr v-if="!pageData.content.length">
              <td colspan="5" class="empty">暂无符合条件的社区帖子</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card detail-card">
        <h3>帖子详情</h3>
        <p v-if="loadingDetail" class="workspace-copy">正在加载详情...</p>
        <template v-else-if="detail">
          <div class="detail-head">
            <strong>{{ detail.title || '(无标题)' }}</strong>
            <span class="status-pill" :class="detail.status === 'OFFLINE' ? 'warn' : 'success'">
              {{ statusLabel(detail.status) }}
            </span>
          </div>
          <p class="workspace-copy">{{ detail.summary || '暂无摘要' }}</p>
          <pre class="detail-content">{{ detail.content }}</pre>
          <div class="meta-block">
            <span>作者：{{ detail.author?.nickname || '-' }}（{{ detail.author?.username || '-' }}）</span>
            <span>标签：{{ detail.tags?.join('、') || '无' }}</span>
            <span>发布时间：{{ toDisplayTime(detail.publishTime) }}</span>
            <span>更新时间：{{ toDisplayTime(detail.updatedAt) }}</span>
          </div>
          <div class="metrics-inline">
            <span>赞 {{ detail.likeCount || 0 }}</span>
            <span>评 {{ detail.commentCount || 0 }}</span>
            <span>藏 {{ detail.favoriteCount || 0 }}</span>
          </div>
          <div class="image-grid" v-if="detail.images?.length">
            <a v-for="url in detail.images" :key="url" :href="url" target="_blank" rel="noreferrer">
              <img :src="url" alt="community" />
            </a>
          </div>
        </template>
        <p v-else class="workspace-empty">请从左侧列表选择帖子查看详情</p>
      </div>
    </div>

    <AdminPagination
      :page="pageData.number"
      :total-pages="pageData.totalPages"
      :loading="loadingList"
      @prev="loadPosts(pageData.number - 1)"
      @next="loadPosts(pageData.number + 1)"
    />
  </section>
</template>

<style scoped>
.community-page {
  display: grid;
  gap: 18px;
}

.stack-top {
  align-items: start;
}

.search {
  min-width: 240px;
}

.compact {
  width: 120px;
  border-radius: 14px;
  padding: 10px 12px;
}

.table-base tbody tr {
  cursor: pointer;
}

.table-base tbody tr.selected {
  background: rgba(200, 155, 91, 0.16);
}

.post-summary {
  margin: 6px 0 8px;
  color: var(--ink-600);
}

.tiny {
  font-size: 0.75rem;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.mini {
  min-height: 28px;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 0.75rem;
}

.detail-card {
  padding: 16px;
  display: grid;
  gap: 12px;
  position: sticky;
  top: 0;
  align-self: start;
}

.detail-card h3 {
  margin: 0;
}

.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.detail-content {
  margin: 0;
  max-height: 280px;
  overflow: auto;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid var(--line);
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--ink-800);
}

.meta-block {
  display: grid;
  gap: 4px;
  color: var(--ink-600);
  font-size: 0.9rem;
}

.metrics-inline {
  display: flex;
  gap: 12px;
  color: var(--ink-600);
  font-size: 0.88rem;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(112px, 1fr));
  gap: 10px;
}

.image-grid a {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.78);
}

.image-grid img {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
}

@media (max-width: 1024px) {
  .detail-card {
    position: static;
  }
}
</style>
