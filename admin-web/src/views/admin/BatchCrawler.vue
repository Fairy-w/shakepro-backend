<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import {
  adminApi,
  type AdminBatchImportHistoryItem,
  type AdminBatchImportJobStatusResult,
  type PageResult,
} from '@/api/admin'
import AdminInspectorPanel from '@/components/admin/AdminInspectorPanel.vue'
import AdminMetricCard from '@/components/admin/AdminMetricCard.vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import AdminSummaryList from '@/components/admin/AdminSummaryList.vue'
import AdminWorkspaceCard from '@/components/admin/AdminWorkspaceCard.vue'
import AdminWorkspacePanel from '@/components/admin/AdminWorkspacePanel.vue'
import AdminWorkspaceShell from '@/components/admin/AdminWorkspaceShell.vue'

const POLLING_INTERVAL_MS = 2000
const TERMINAL_STATUSES = new Set(['SUCCESS', 'PARTIAL', 'FAILED', 'SKIPPED'])

const batchForm = reactive({
  listUrl: '',
  maxItems: 20,
  concurrency: 3,
  autoGenerate: false,
  autoSave: false,
  onlyNew: true,
})

const batchLoading = ref(false)
const historyLoading = ref(false)
const batchError = ref('')
const historyError = ref('')
const currentJobId = ref('')
const jobStatus = ref<AdminBatchImportJobStatusResult | null>(null)
const toastVisible = ref(false)
const toastMessage = ref('')
const toastType = ref<'success' | 'warning' | 'error'>('success')
const lastNotifiedJobId = ref('')
const historyPage = ref<PageResult<AdminBatchImportHistoryItem>>({
  content: [],
  totalElements: 0,
  totalPages: 0,
  size: 10,
  number: 0,
})

let pollingTimer: ReturnType<typeof setInterval> | null = null
let toastTimer: ReturnType<typeof setTimeout> | null = null

const currentSuccessRate = computed(() => {
  const processed = jobStatus.value?.processedCount || 0
  const success = jobStatus.value?.successCount || 0
  if (!processed) return 0
  return Math.round((success / processed) * 100)
})

const progressPercent = computed(() => {
  const raw = jobStatus.value?.progressPercent ?? 0
  if (!Number.isFinite(raw)) return 0
  return Math.max(0, Math.min(100, Math.floor(raw)))
})

const latestHistory = computed(() => historyPage.value.content[0] || null)

const taskSummaryItems = computed(() => [
  { label: '列表页标题', value: jobStatus.value?.listTitle || '未识别标题' },
  { label: '发现链接', value: jobStatus.value?.discoveredCount || 0 },
  { label: '选中处理', value: jobStatus.value?.selectedCount || 0 },
  { label: '耗时', value: formatDuration(jobStatus.value?.durationMs) },
])

const resultSummaryItems = computed(() => [
  { label: '成功', value: jobStatus.value?.successCount || 0 },
  { label: '失败', value: jobStatus.value?.failureCount || 0 },
  { label: '待入库', value: jobStatus.value?.remainingUnimportedCount || 0 },
  { label: '消息', value: jobStatus.value?.message || '等待启动任务' },
])

function normalizeNumber(value: number, min: number, max: number) {
  if (!Number.isFinite(value)) return min
  return Math.min(max, Math.max(min, Math.floor(value)))
}

function historySuccessRate(item: AdminBatchImportHistoryItem) {
  if (!item.processedCount) return 0
  return Math.round((item.successCount / item.processedCount) * 100)
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: '排队中',
    RUNNING: '执行中',
    SUCCESS: '成功',
    PARTIAL: '部分成功',
    FAILED: '失败',
    SKIPPED: '跳过',
  }
  return map[status] || status
}

function statusClass(status?: string | null) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'PARTIAL' || status === 'SKIPPED') return 'warn'
  if (status === 'FAILED') return 'danger'
  return 'warn'
}

function formatDateTime(value?: string | null) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

function formatDuration(durationMs?: number | null) {
  const safe = durationMs && durationMs > 0 ? durationMs : 0
  return `${(safe / 1000).toFixed(2)} 秒`
}

function isTerminalStatus(status?: string | null) {
  return !!status && TERMINAL_STATUSES.has(status)
}

function stopPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

function showToast(message: string, type: 'success' | 'warning' | 'error') {
  if (toastTimer) {
    clearTimeout(toastTimer)
    toastTimer = null
  }
  toastMessage.value = message
  toastType.value = type
  toastVisible.value = true
  toastTimer = setTimeout(() => {
    toastVisible.value = false
    toastTimer = null
  }, 3200)
}

function startPolling() {
  stopPolling()
  pollingTimer = setInterval(() => {
    if (!currentJobId.value) {
      stopPolling()
      return
    }
    pollJobStatus(currentJobId.value, false)
  }, POLLING_INTERVAL_MS)
}

async function pollJobStatus(jobId: string, firstLoad: boolean) {
  try {
    const status = await adminApi.getBatchImportJobStatus(jobId)
    jobStatus.value = status
    if (isTerminalStatus(status.status)) {
      batchLoading.value = false
      stopPolling()
      if (lastNotifiedJobId.value !== jobId) {
        if (status.status === 'SUCCESS') {
          showToast('批量抓取完成：全部成功', 'success')
        } else if (status.status === 'PARTIAL') {
          showToast('批量抓取完成：部分成功，请查看失败数', 'warning')
        } else if (status.status === 'SKIPPED') {
          showToast('批量抓取完成：无可处理的新链接', 'warning')
        } else {
          showToast(status.errorMessage || '批量抓取失败，请查看任务详情', 'error')
        }
        lastNotifiedJobId.value = jobId
      }
      await loadHistories(0)
    } else if (!firstLoad) {
      batchLoading.value = true
    }
  } catch (error) {
    if (firstLoad || !jobStatus.value) {
      batchError.value = error instanceof Error ? error.message : '任务状态查询失败，请稍后重试'
      batchLoading.value = false
      stopPolling()
    }
  }
}

async function loadHistories(page = historyPage.value.number) {
  historyLoading.value = true
  historyError.value = ''
  try {
    historyPage.value = await adminApi.getBatchImportHistories({
      page,
      size: historyPage.value.size || 10,
    })
  } catch (error) {
    historyError.value = error instanceof Error ? error.message : '历史记录加载失败，请稍后重试'
  } finally {
    historyLoading.value = false
  }
}

async function submitBatchImport() {
  const listUrl = batchForm.listUrl.trim()
  if (!listUrl) {
    batchError.value = '请先输入鸡尾酒列表页地址'
    return
  }

  batchLoading.value = true
  batchError.value = ''
  stopPolling()

  try {
    const maxItems = normalizeNumber(batchForm.maxItems, 1, 1000)
    const concurrency = normalizeNumber(batchForm.concurrency, 1, 8)
    batchForm.maxItems = maxItems
    batchForm.concurrency = concurrency

    const started = await adminApi.startBatchImportJob({
      listUrl,
      maxItems,
      concurrency,
      autoGenerate: batchForm.autoGenerate,
      autoSave: batchForm.autoSave,
      onlyNew: batchForm.onlyNew,
    })
    currentJobId.value = started.jobId
    lastNotifiedJobId.value = ''
    jobStatus.value = {
      jobId: started.jobId,
      status: started.status || 'PENDING',
      message: started.message || '任务已提交，等待执行',
      listUrl,
      maxItems,
      concurrency,
      autoGenerate: batchForm.autoGenerate,
      autoSave: batchForm.autoSave,
      onlyNew: batchForm.onlyNew,
      discoveredCount: 0,
      selectedCount: 0,
      processedCount: 0,
      successCount: 0,
      failureCount: 0,
      progressPercent: 0,
      durationMs: 0,
    }
    await pollJobStatus(started.jobId, true)
    if (!isTerminalStatus(jobStatus.value?.status)) {
      startPolling()
    }
  } catch (error) {
    batchError.value = error instanceof Error ? error.message : '批量抓取任务提交失败，请稍后重试'
    batchLoading.value = false
    stopPolling()
  }
}

onMounted(() => {
  loadHistories(0)
})

onUnmounted(() => {
  stopPolling()
  if (toastTimer) {
    clearTimeout(toastTimer)
    toastTimer = null
  }
})
</script>

<template>
  <section class="batch-crawler-page">
    <div v-if="toastVisible" class="status-toast" :class="`toast-${toastType}`">
      {{ toastMessage }}
    </div>

    <AdminPageHeader
      eyebrow="AI Workspace"
      title="批量抓取工作站"
      subtitle="和单页抓取保持同一套工作站结构：左侧任务配置，中间任务面板，右侧结果追踪与历史摘要。"
    >
      <template #meta>
        <span class="badge">任务进度 {{ progressPercent }}%</span>
        <span class="badge subtle">成功率 {{ currentSuccessRate }}%</span>
      </template>
    </AdminPageHeader>

    <div class="metric-grid compact-metrics">
      <AdminMetricCard eyebrow="当前任务" label="已处理" :value="jobStatus?.processedCount ?? '--'" hint="与选中处理数一起判断当前节奏。" tone="strong" />
      <AdminMetricCard eyebrow="当前任务" label="成功 / 失败" :value="jobStatus ? `${jobStatus.successCount || 0} / ${jobStatus.failureCount || 0}` : '--'" hint="任务完成后直接沉淀到历史记录里。" tone="warm" />
      <AdminMetricCard eyebrow="历史最近一次" label="历史状态" :value="latestHistory ? statusLabel(latestHistory.status) : '--'" :hint="latestHistory ? formatDateTime(latestHistory.createdAt) : '暂无历史记录'" />
    </div>

    <AdminWorkspaceShell>
      <template #sidebar>
        <AdminInspectorPanel eyebrow="Batch Deck" title="任务配置" tone="dark">
          <label class="workspace-field">
            <span>列表页地址</span>
            <input v-model="batchForm.listUrl" class="field" type="url" placeholder="例如：https://www.example.com/cocktails" />
          </label>
          <label class="workspace-field">
            <span>最大抓取条数</span>
            <input v-model.number="batchForm.maxItems" class="field" type="number" min="1" max="1000" />
          </label>
          <label class="workspace-field">
            <span>并发数</span>
            <input v-model.number="batchForm.concurrency" class="field" type="number" min="1" max="8" />
          </label>
          <label class="toggle-check">
            <input v-model="batchForm.onlyNew" type="checkbox" />
            <span>仅导入未抓取过</span>
          </label>
          <label class="toggle-check">
            <input v-model="batchForm.autoGenerate" type="checkbox" />
            <span>自动调用 AI 生成中文字段</span>
          </label>
          <label class="toggle-check">
            <input v-model="batchForm.autoSave" type="checkbox" />
            <span>自动保存到鸡尾酒库</span>
          </label>
          <button class="button-primary" :disabled="batchLoading" @click="submitBatchImport">
            {{ batchLoading ? '任务执行中...' : '开始批量抓取' }}
          </button>
          <p v-if="batchError" class="error-text">{{ batchError }}</p>
        </AdminInspectorPanel>
      </template>

      <template #main>
        <AdminWorkspaceCard
          eyebrow="Run Console"
          title="当前任务控制台"
          description="批量抓取开始后，主面板持续展示进度、当前链接和处理摘要。"
        >
          <template #action>
            <span class="status-pill" :class="statusClass(jobStatus?.status)">{{ jobStatus ? statusLabel(jobStatus.status) : '未启动' }}</span>
          </template>
          <div class="progress-block">
            <div class="progress-meta">
              <span>{{ progressPercent }}%</span>
              <span>{{ jobStatus?.processedCount || 0 }} / {{ jobStatus?.selectedCount || 0 }} 已处理</span>
            </div>
            <div class="progress-track">
              <div class="progress-fill" :style="{ width: `${progressPercent}%` }"></div>
            </div>
            <p v-if="jobStatus?.currentUrl" class="progress-hint">
              当前链接：{{ jobStatus.currentUrl }}
              <span v-if="jobStatus.currentStage">（阶段：{{ jobStatus.currentStage }}）</span>
            </p>
          </div>

          <div class="dual-grid summary-panels">
            <AdminWorkspacePanel title="任务摘要">
              <AdminSummaryList :items="taskSummaryItems" />
            </AdminWorkspacePanel>
            <AdminWorkspacePanel title="结果摘要">
              <AdminSummaryList :items="resultSummaryItems" />
              <p v-if="jobStatus?.errorMessage" class="error-text">{{ jobStatus.errorMessage }}</p>
            </AdminWorkspacePanel>
          </div>
        </AdminWorkspaceCard>

        <AdminWorkspaceCard
          eyebrow="History"
          title="历史任务表"
          description="统一查看每次执行的状态、参数和结果概览。"
        >
          <template #action>
            <button class="button-secondary" :disabled="historyLoading" @click="loadHistories()">
              {{ historyLoading ? '刷新中...' : '刷新历史' }}
            </button>
          </template>

          <div v-if="historyLoading" class="loading-panel">
            <span class="spinner" aria-hidden="true"></span>
            <p>历史记录加载中...</p>
          </div>
          <p v-else-if="historyError" class="error-text">{{ historyError }}</p>
          <div v-else class="table-card history-table-shell">
            <table class="table-base">
              <thead>
                <tr>
                  <th>时间</th>
                  <th>状态</th>
                  <th>列表页</th>
                  <th>参数</th>
                  <th>结果</th>
                  <th>耗时</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in historyPage.content" :key="item.id">
                  <td class="mono">{{ formatDateTime(item.createdAt) }}</td>
                  <td>
                    <span class="status-pill" :class="statusClass(item.status)">{{ statusLabel(item.status) }}</span>
                  </td>
                  <td>
                    <div class="history-url">
                      <strong>{{ item.listTitle || '未识别标题' }}</strong>
                      <span>{{ item.listUrl }}</span>
                    </div>
                  </td>
                  <td>
                    <div class="mini-stack">
                      <span>max={{ item.maxItems }}, 并发={{ item.concurrency }}</span>
                      <span>{{ item.onlyNew ? '仅新链接' : '全部链接' }} / {{ item.autoGenerate ? 'AI 开' : 'AI 关' }} / {{ item.autoSave ? '自动入库' : '仅提取' }}</span>
                    </div>
                  </td>
                  <td>
                    <div class="mini-stack">
                      <span>发现 {{ item.discoveredCount }} / 选中 {{ item.selectedCount }} / 处理 {{ item.processedCount }}</span>
                      <span>成功 {{ item.successCount }} / 失败 {{ item.failureCount }}（{{ historySuccessRate(item) }}%）</span>
                      <span>待入库 {{ item.remainingUnimportedCount }}</span>
                    </div>
                  </td>
                  <td>{{ formatDuration(item.durationMs) }}</td>
                </tr>
                <tr v-if="!historyPage.content.length">
                  <td colspan="6" class="empty">暂无批量抓取历史记录</td>
                </tr>
              </tbody>
            </table>
          </div>

          <AdminPagination
            :page="historyPage.number"
            :total-pages="historyPage.totalPages"
            :loading="historyLoading"
            @prev="loadHistories(historyPage.number - 1)"
            @next="loadHistories(historyPage.number + 1)"
          />
        </AdminWorkspaceCard>
      </template>

      <template #inspector>
        <AdminInspectorPanel eyebrow="Inspector" title="当前任务摘要" tone="warm">
          <template v-if="jobStatus">
            <span class="badge">状态 {{ statusLabel(jobStatus.status) }}</span>
            <span class="badge subtle">成功率 {{ currentSuccessRate }}%</span>
            <span class="badge subtle">进度 {{ progressPercent }}%</span>
            <span class="badge subtle">待入库 {{ jobStatus.remainingUnimportedCount || 0 }}</span>
            <p class="workspace-copy">{{ jobStatus.message || '等待执行' }}</p>
          </template>
          <p v-else class="workspace-empty">启动一次批量抓取后，这里会持续更新任务摘要。</p>
        </AdminInspectorPanel>

        <AdminInspectorPanel eyebrow="Latest" title="最近一次历史记录" tone="default">
          <template v-if="latestHistory">
            <p class="workspace-copy">{{ latestHistory.listTitle || '未识别标题' }}</p>
            <span class="status-pill" :class="statusClass(latestHistory.status)">{{ statusLabel(latestHistory.status) }}</span>
            <div class="mini-stack">
              <span>处理 {{ latestHistory.processedCount }} 条</span>
              <span>成功 {{ latestHistory.successCount }} / 失败 {{ latestHistory.failureCount }}</span>
              <span>{{ formatDateTime(latestHistory.createdAt) }}</span>
            </div>
          </template>
          <p v-else class="workspace-empty">还没有历史记录。</p>
        </AdminInspectorPanel>
      </template>
    </AdminWorkspaceShell>
  </section>
</template>

<style scoped>
.batch-crawler-page {
  display: grid;
  gap: 18px;
}

.compact-metrics {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.status-toast {
  position: fixed;
  right: 24px;
  top: 24px;
  z-index: 90;
  min-width: 280px;
  max-width: min(420px, calc(100vw - 32px));
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid transparent;
  box-shadow: var(--shadow-md);
  font-weight: 700;
}

.toast-success {
  background: rgba(236, 253, 245, 0.96);
  color: #065f46;
  border-color: rgba(16, 185, 129, 0.3);
}

.toast-warning {
  background: rgba(255, 251, 235, 0.97);
  color: #92400e;
  border-color: rgba(245, 158, 11, 0.35);
}

.toast-error {
  background: rgba(254, 242, 242, 0.97);
  color: #991b1b;
  border-color: rgba(220, 38, 38, 0.28);
}

.toggle-check {
  font-weight: 700;
}

.toggle-check {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #fff8f0;
}

.toggle-check input {
  width: 18px;
  height: 18px;
  accent-color: var(--accent);
}
.progress-hint,
.history-url span,
.mini-stack span {
  margin: 0;
  color: var(--ink-600);
}

.progress-block {
  padding: 18px;
  border-radius: 20px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.64);
  display: grid;
  gap: 10px;
}

.progress-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-weight: 700;
}

.progress-track {
  width: 100%;
  height: 12px;
  border-radius: 999px;
  overflow: hidden;
  background: rgba(74, 47, 42, 0.12);
}

.progress-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--accent), var(--success));
  transition: width 0.25s ease;
}

.summary-panels,
.history-url,
.mini-stack {
  display: grid;
  gap: 12px;
}

.history-table-shell {
  overflow: hidden;
}

.loading-panel {
  padding: 20px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid var(--line);
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(15, 118, 110, 0.2);
  border-top-color: var(--success);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.error-text {
  margin: 0;
  color: var(--danger);
  font-weight: 700;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 1080px) {
  .compact-metrics {
    grid-template-columns: 1fr;
  }

  .progress-meta {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
