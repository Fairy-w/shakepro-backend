<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import {
  adminApi,
  type AdminExtractedFieldsResult,
  type AdminPageTextResult,
  type FieldSource,
} from '@/api/admin'
import { buildGeneratedPayloadFromExtracted } from './cocktailPayload'
import AdminInspectorPanel from '@/components/admin/AdminInspectorPanel.vue'
import AdminMetricCard from '@/components/admin/AdminMetricCard.vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import AdminWorkspaceCard from '@/components/admin/AdminWorkspaceCard.vue'
import AdminWorkspacePanel from '@/components/admin/AdminWorkspacePanel.vue'
import AdminWorkspaceShell from '@/components/admin/AdminWorkspaceShell.vue'

type StageKey = 'crawl' | 'extract' | 'review' | 'final'
type StageStatus = 'pending' | 'running' | 'completed' | 'error'

interface PipelineStageDefinition {
  key: StageKey
  label: string
  description: string
}

interface PipelineStageView extends PipelineStageDefinition {
  status: StageStatus
  summary: string
  clickable: boolean
}

interface FieldRow {
  key: string
  label: string
  type?: 'list'
}

const stageDefinitions: PipelineStageDefinition[] = [
  { key: 'crawl', label: '抓取网页', description: '输入网址并获取页面标题与 HTML 原文。' },
  { key: 'extract', label: '提取字段', description: '执行解析器，把页面内容转为结构化字段。' },
  { key: 'review', label: '审核结果', description: '查看解析结果、来源标记和缺失字段。' },
  { key: 'final', label: 'AI 最终稿', description: '补全中文字段并决定是否入库。' },
]

const fieldRows: FieldRow[] = [
  { key: 'name', label: '名称' },
  { key: 'englishName', label: '英文名' },
  { key: 'category', label: '分类' },
  { key: 'difficulty', label: '难度' },
  { key: 'abv', label: '酒精度' },
  { key: 'glass', label: '杯型' },
  { key: 'garnish', label: '装饰' },
  { key: 'highlight', label: '亮点文案' },
  { key: 'subtitle', label: '副标题' },
  { key: 'description', label: '描述' },
  { key: 'story', label: '故事' },
  { key: 'flavorTags', label: '风味标签', type: 'list' },
  { key: 'pairings', label: '搭配建议', type: 'list' },
  { key: 'serviceNotes', label: '服务备注', type: 'list' },
] as const

const form = reactive({ url: '' })
const activeStage = ref<StageKey>('crawl')
const crawlLoading = ref(false)
const extractLoading = ref(false)
const generateLoading = ref(false)
const saveLoading = ref(false)
const crawlError = ref('')
const extractError = ref('')
const generateError = ref('')
const saveError = ref('')
const saveSuccess = ref('')
const crawlResult = ref<AdminPageTextResult | null>(null)
const extractResult = ref<AdminExtractedFieldsResult | null>(null)
const generateResult = ref<AdminExtractedFieldsResult | null>(null)
const previewImageUrl = ref('')
const previewImageAlt = ref('图片预览')

const antiCrawlerHint = computed(() => crawlError.value.includes('403') || crawlError.value.includes('反爬'))

const stageList = computed<PipelineStageView[]>(() => {
  const statuses: Record<StageKey, StageStatus> = {
    crawl: crawlLoading.value ? 'running' : crawlError.value ? 'error' : crawlResult.value ? 'completed' : 'pending',
    extract: extractLoading.value ? 'running' : extractError.value ? 'error' : extractResult.value ? 'completed' : 'pending',
    review: extractResult.value ? 'completed' : 'pending',
    final: generateLoading.value ? 'running' : generateError.value ? 'error' : generateResult.value ? 'completed' : 'pending',
  }

  return stageDefinitions.map((stage) => {
    let summary = '等待处理'
    if (stage.key === 'crawl') {
      summary = crawlLoading.value ? '正在抓取页面源码' : crawlResult.value ? `已抓取 ${crawlResult.value.html.length} 字符` : crawlError.value || '等待输入网址'
    } else if (stage.key === 'extract') {
      summary = extractLoading.value ? '正在执行解析器' : extractResult.value ? `已提取 ${extractResult.value.ingredients.length} 个材料` : extractError.value || (crawlResult.value ? '等待开始提取' : '需要先抓取网页')
    } else if (stage.key === 'review') {
      summary = extractResult.value ? `${extractResult.value.missingFields.length} 个字段待补全` : '等待提取结果'
    } else {
      summary = generateLoading.value ? '正在生成中文最终稿' : generateResult.value ? `AI 已生成，剩余 ${generateResult.value.missingFields.length} 项` : generateError.value || (extractResult.value ? '等待调用 AI' : '需要先完成提取')
    }

    const clickable = stage.key === 'crawl'
      || (stage.key === 'extract' && !!crawlResult.value)
      || (stage.key === 'review' && !!extractResult.value)
      || (stage.key === 'final' && (!!generateResult.value || !!extractResult.value))

    return { ...stage, status: statuses[stage.key], summary, clickable }
  })
})

const pipelineProgress = computed(() => {
  const completed = stageList.value.filter((item) => item.status === 'completed').length
  const running = stageList.value.some((item) => item.status === 'running') ? 0.5 : 0
  return Math.round(((completed + running) / stageList.value.length) * 100)
})

const activeStageInfo = computed<PipelineStageView>(() => stageList.value.find((item) => item.key === activeStage.value) ?? stageList.value[0]!)
const inspectorResult = computed(() => generateResult.value || extractResult.value)

async function submitCrawl() {
  const url = form.url.trim()
  if (!url) {
    crawlError.value = '请先输入网页地址'
    return
  }

  activeStage.value = 'crawl'
  crawlLoading.value = true
  crawlError.value = ''
  extractError.value = ''
  generateError.value = ''
  saveError.value = ''
  saveSuccess.value = ''
  crawlResult.value = null
  extractResult.value = null
  generateResult.value = null

  try {
    crawlResult.value = await adminApi.crawlPageText({ url })
    activeStage.value = 'extract'
  } catch (error) {
    crawlError.value = error instanceof Error ? error.message : '网页抓取失败，请稍后重试'
  } finally {
    crawlLoading.value = false
  }
}

async function submitExtract() {
  if (!crawlResult.value) {
    extractError.value = '请先完成网页抓取'
    activeStage.value = 'crawl'
    return
  }

  activeStage.value = 'extract'
  extractLoading.value = true
  extractError.value = ''
  generateError.value = ''
  saveError.value = ''
  saveSuccess.value = ''
  extractResult.value = null
  generateResult.value = null

  try {
    extractResult.value = await adminApi.extractPageFields({
      url: crawlResult.value.url,
      title: crawlResult.value.title,
      html: crawlResult.value.html,
    })
    activeStage.value = 'review'
  } catch (error) {
    extractError.value = error instanceof Error ? error.message : '字段提取失败，请稍后重试'
  } finally {
    extractLoading.value = false
  }
}

async function submitGenerate() {
  if (!extractResult.value) {
    generateError.value = '请先完成字段提取'
    activeStage.value = 'extract'
    return
  }

  activeStage.value = 'final'
  generateLoading.value = true
  generateError.value = ''
  saveError.value = ''
  saveSuccess.value = ''
  generateResult.value = null

  try {
    generateResult.value = await adminApi.generatePageFields(extractResult.value)
  } catch (error) {
    generateError.value = error instanceof Error ? error.message : 'AI 生成失败，请稍后重试'
    activeStage.value = 'review'
  } finally {
    generateLoading.value = false
  }
}

async function saveGeneratedCocktail() {
  if (!generateResult.value) {
    saveError.value = '请先生成 AI 最终结果'
    return
  }

  const payload = buildGeneratedPayloadFromExtracted(generateResult.value)
  if (!payload.name) {
    saveError.value = '名称为空，暂时无法入库'
    return
  }

  saveLoading.value = true
  saveError.value = ''
  saveSuccess.value = ''
  try {
    const saved = await adminApi.createGeneratedCocktail(payload)
    saveSuccess.value = `已保存到鸡尾酒库，ID：${saved.id}`
  } catch (error) {
    saveError.value = error instanceof Error ? error.message : '保存失败，请稍后重试'
  } finally {
    saveLoading.value = false
  }
}

function switchStage(stage: PipelineStageView) {
  if (stage.clickable) {
    activeStage.value = stage.key
  }
}

function fieldValue(value?: string | null) {
  return value?.trim() ? value : '未提取到'
}

function listValue(values?: string[] | null) {
  return values && values.length ? values.join('、') : '未提取到'
}

function metricEntries(metrics?: Record<string, number> | null): Array<{ label: string; value: number }> {
  return metrics ? Object.entries(metrics).map(([label, value]) => ({ label, value })) : []
}

function fieldSource(fieldName: string, result?: AdminExtractedFieldsResult | null): FieldSource | undefined {
  return result?.fieldSources?.[fieldName]
}

function sourceLabel(source?: FieldSource) {
  if (!source?.mode) return '无标记'
  const modeMap: Record<string, string> = {
    extracted: '已提取',
    derived: '派生',
    missing: '缺失',
    retained: '沿用',
    ai_translated: 'AI 翻译',
    ai_generated: 'AI 补全',
  }
  const sourceMap: Record<string, string> = {
    jsonld: 'JSON-LD',
    nextData: 'NEXT_DATA',
    description: 'Description',
    parser: '解析器',
    'ai.qwen': '通义千问',
  }
  const mode = modeMap[source.mode] || source.mode
  const detail = source.source ? sourceMap[source.source] || source.source : ''
  return detail ? `${mode} · ${detail}` : mode
}

function displayFieldValue(result: AdminExtractedFieldsResult, fieldKey: string, valueType?: string) {
  const record = result as unknown as Record<string, string | string[] | null | undefined>
  const value = record[fieldKey]
  if (valueType === 'list') {
    return Array.isArray(value) ? listValue(value) : '未提取到'
  }
  return typeof value === 'string' || value == null ? fieldValue(value) : '未提取到'
}

function openImagePreview(imageUrl: string, altText: string) {
  previewImageUrl.value = imageUrl
  previewImageAlt.value = altText
}

function closeImagePreview() {
  previewImageUrl.value = ''
  previewImageAlt.value = '图片预览'
}
</script>

<template>
  <section class="crawler-page">
    <AdminPageHeader
      eyebrow="AI Workspace"
      title="网页抓取工作站"
      subtitle="统一成左侧阶段导航、中间结果面板、右侧字段来源追踪的工作站结构。"
    >
      <template #meta>
        <span class="badge">整体进度 {{ pipelineProgress }}%</span>
        <span class="badge subtle">单页流程</span>
      </template>
    </AdminPageHeader>

    <div class="metric-grid compact-metrics">
      <AdminMetricCard eyebrow="抓取结果" label="HTML 原文" :value="crawlResult ? crawlResult.html.length : '--'" hint="成功抓取后即可进入解析器阶段。" tone="strong" />
      <AdminMetricCard eyebrow="提取结果" label="材料数量" :value="extractResult ? extractResult.ingredients.length : '--'" hint="字段提取阶段不会调用 AI。" tone="warm" />
      <AdminMetricCard eyebrow="AI 最终稿" label="缺失字段" :value="(generateResult || extractResult)?.missingFields.length ?? '--'" hint="先在 Review 阶段确认，再决定是否调用 AI。" />
    </div>

    <AdminWorkspaceShell>
      <template #sidebar>
        <AdminInspectorPanel eyebrow="Pipeline" title="阶段导航" tone="dark">
          <button
            v-for="stage in stageList"
            :key="stage.key"
            class="stage-link"
            :class="[`status-${stage.status}`, { active: activeStage === stage.key }]"
            :disabled="!stage.clickable"
            @click="switchStage(stage)"
          >
            <strong>{{ stage.label }}</strong>
            <span>{{ stage.summary }}</span>
          </button>
        </AdminInspectorPanel>
      </template>

      <template #main>
        <AdminWorkspaceCard
          v-if="activeStage === 'crawl'"
          eyebrow="Stage 1"
          title="抓取网页源码"
          description="输入公开可访问的网址，先获取页面标题与 HTML 原文。"
        >
          <template #action>
            <span class="badge">源码抓取</span>
          </template>
          <div class="workspace-form">
            <label class="workspace-field workspace-field--wide">
              <span>网页地址</span>
              <input v-model="form.url" class="field" type="url" placeholder="例如：https://www.example.com/cocktail/dry-martini" />
            </label>
            <button class="button-primary" :disabled="crawlLoading" @click="submitCrawl">
              {{ crawlLoading ? '抓取中...' : '开始抓取' }}
            </button>
          </div>

          <p v-if="crawlError" class="error-text">{{ crawlError }}</p>
          <p v-if="antiCrawlerHint" class="hint-text">该站点可能有限制访问，必要时需要浏览器抓取兜底。</p>

          <div v-if="crawlResult" class="dual-grid">
            <AdminWorkspacePanel :title="crawlResult.title || '未识别标题'">
              <p class="workspace-copy">{{ crawlResult.url }}</p>
              <span class="badge subtle">HTML 长度 {{ crawlResult.html.length }}</span>
            </AdminWorkspacePanel>
            <AdminWorkspacePanel title="HTML 原文">
              <pre class="text-result">{{ crawlResult.html }}</pre>
            </AdminWorkspacePanel>
          </div>
        </AdminWorkspaceCard>

        <AdminWorkspaceCard
          v-else-if="activeStage === 'extract'"
          eyebrow="Stage 2"
          title="提取字段"
          description="当前阶段只执行解析器，不调用 AI。"
        >
          <template #action>
            <span class="badge">结构化处理中</span>
          </template>
          <div class="dual-grid">
            <AdminWorkspacePanel title="输入源">
              <p>{{ crawlResult?.title || '未识别标题' }}</p>
              <p class="mono workspace-copy source-url">{{ crawlResult?.url || '-' }}</p>
              <p class="workspace-copy">确认源码无误后即可开始提取。</p>
            </AdminWorkspacePanel>
            <AdminWorkspacePanel title="开始提取" class="action-panel">
              <p class="workspace-copy">解析不到的字段会保留为空，随后在 Review 阶段集中核对。</p>
              <button class="button-primary" :disabled="extractLoading || !crawlResult" @click="submitExtract">
                {{ extractLoading ? '提取中...' : '提取字段' }}
              </button>
              <p v-if="extractError" class="error-text">{{ extractError }}</p>
            </AdminWorkspacePanel>
          </div>
        </AdminWorkspaceCard>

        <AdminWorkspaceCard
          v-else-if="activeStage === 'review'"
          eyebrow="Stage 3"
          title="提取结果预览"
          description="确认解析器输出，再决定是否调用 AI 补全最终中文结果。"
        >
          <template #action>
            <button class="button-primary" :disabled="generateLoading || !extractResult" @click="submitGenerate">
              {{ generateLoading ? 'AI 生成中...' : '调用 AI 生成中文最终结果' }}
            </button>
          </template>

          <p v-if="generateError" class="error-text">{{ generateError }}</p>

          <div v-if="extractResult" class="review-grid">
            <AdminWorkspacePanel title="字段总览" class="field-panel">
              <div v-for="item in fieldRows" :key="item.key" class="field-row">
                <div>
                  <strong>{{ item.label }}</strong>
                  <span class="status-pill warn source-pill">{{ sourceLabel(fieldSource(item.key, extractResult)) }}</span>
                </div>
                <p>{{ displayFieldValue(extractResult, item.key, item.type) }}</p>
              </div>
            </AdminWorkspacePanel>

            <AdminWorkspacePanel title="材料与步骤">
              <div class="collection-stack">
                <div class="collection-block">
                  <strong>材料 {{ extractResult.ingredients.length }}</strong>
                  <div class="mini-list">
                    <span v-for="(item, index) in extractResult.ingredients" :key="`${item.name || 'material'}-${index}`">
                      {{ item.name || '未识别材料名' }} / {{ item.amount || '未识别用量' }}
                    </span>
                  </div>
                </div>
                <div class="collection-block">
                  <strong>步骤 {{ extractResult.steps.length }}</strong>
                  <div class="mini-list">
                    <span v-for="(item, index) in extractResult.steps" :key="`${item.title || 'step'}-${index}`">
                      {{ item.title || `步骤 ${index + 1}` }}：{{ item.detail || '未识别步骤说明' }}
                    </span>
                  </div>
                </div>
                <div class="collection-block">
                  <strong>风味指标 {{ metricEntries(extractResult.flavorMetrics).length }}</strong>
                  <div class="mini-list compact-tags">
                    <span v-for="item in metricEntries(extractResult.flavorMetrics)" :key="item.label" class="badge subtle">
                      {{ item.label }} · {{ item.value }}
                    </span>
                  </div>
                </div>
              </div>
            </AdminWorkspacePanel>
          </div>
        </AdminWorkspaceCard>

        <AdminWorkspaceCard
          v-else
          eyebrow="Stage 4"
          title="AI 中文最终结果"
          description="查看 AI 补全后的结果，并决定是否保存到鸡尾酒库。"
        >
          <template #action>
            <button class="button-primary" :disabled="saveLoading || generateLoading || !generateResult" @click="saveGeneratedCocktail">
              {{ generateLoading ? 'AI 生成中...' : saveLoading ? '保存中...' : '保存到鸡尾酒库' }}
            </button>
          </template>

          <p v-if="saveError" class="error-text">{{ saveError }}</p>
          <p v-if="saveSuccess" class="success-text">{{ saveSuccess }}</p>

          <div v-if="generateLoading" class="loading-panel">
            <span class="spinner" aria-hidden="true"></span>
            <p>AI 正在生成最终结果，请稍候...</p>
          </div>

          <div v-else-if="generateResult" class="review-grid">
            <AdminWorkspacePanel title="最终字段" class="field-panel">
              <div v-for="item in fieldRows" :key="item.key" class="field-row">
                <div>
                  <strong>{{ item.label }}</strong>
                  <span class="status-pill warn source-pill">{{ sourceLabel(fieldSource(item.key, generateResult)) }}</span>
                </div>
                <p>{{ displayFieldValue(generateResult, item.key, item.type) }}</p>
              </div>
            </AdminWorkspacePanel>

            <AdminWorkspacePanel title="AI 结果补充">
              <div class="collection-stack">
                <div class="collection-block">
                  <strong>缺失字段 {{ generateResult.missingFields.length }}</strong>
                  <div class="mini-list compact-tags">
                    <span v-if="!generateResult.missingFields.length" class="badge signal">字段已补齐</span>
                    <span v-for="item in generateResult.missingFields" :key="item" class="badge subtle">{{ item }}</span>
                  </div>
                </div>
                <div class="collection-block" v-if="generateResult.heroImage">
                  <strong>主图</strong>
                  <button class="image-preview-trigger" @click="openImagePreview(generateResult.heroImage, '抓取到的主图')">
                    <img :src="generateResult.heroImage" alt="抓取到的主图" />
                  </button>
                </div>
              </div>
            </AdminWorkspacePanel>
          </div>
        </AdminWorkspaceCard>
      </template>

      <template #inspector>
        <AdminInspectorPanel eyebrow="Inspector" title="字段来源追踪" tone="default">
          <p class="workspace-copy">当前阶段：{{ activeStageInfo.label }}</p>
          <p class="workspace-copy">{{ activeStageInfo.description }}</p>
          <div v-if="inspectorResult" class="source-list">
            <div v-for="item in fieldRows" :key="item.key" class="source-item">
              <strong>{{ item.label }}</strong>
              <span>{{ sourceLabel(fieldSource(item.key, inspectorResult)) }}</span>
            </div>
          </div>
          <p v-else class="workspace-empty">完成提取后，这里会展示每个字段的来源与补全过程。</p>
        </AdminInspectorPanel>

        <AdminInspectorPanel eyebrow="Output" title="当前结果摘要" tone="warm">
          <template v-if="inspectorResult">
            <span class="badge">材料 {{ inspectorResult.ingredients.length }}</span>
            <span class="badge subtle">步骤 {{ inspectorResult.steps.length }}</span>
            <span class="badge subtle">缺失 {{ inspectorResult.missingFields.length }}</span>
            <span class="badge subtle">模式 {{ inspectorResult.generateMode || inspectorResult.extractMode || '未标记' }}</span>
          </template>
          <p v-else class="workspace-empty">当前还没有结构化结果。</p>
        </AdminInspectorPanel>
      </template>
    </AdminWorkspaceShell>

    <div v-if="previewImageUrl" class="image-preview-modal" @click.self="closeImagePreview">
      <div class="image-preview-dialog">
        <button class="image-preview-close" @click="closeImagePreview">×</button>
        <img :src="previewImageUrl" :alt="previewImageAlt" />
      </div>
    </div>
  </section>
</template>

<style scoped>
.crawler-page {
  display: grid;
  gap: 18px;
}

.compact-metrics {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.stage-link {
  width: 100%;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.04);
  color: inherit;
  text-align: left;
  display: grid;
  gap: 6px;
}

.stage-link strong {
  font-size: 0.98rem;
}

.stage-link span {
  color: rgba(255, 248, 240, 0.74);
  font-size: 0.84rem;
}

.stage-link.active {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.14), rgba(200, 155, 91, 0.12));
}

.hint-text,
.source-url,
.field-row p,
.source-item span {
  margin: 0;
  color: var(--ink-600);
}

.workspace-form {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: end;
}

.workspace-field--wide {
  flex: 1 1 520px;
}

.text-result {
  margin: 0;
  min-height: 240px;
  max-height: 52vh;
  overflow: auto;
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--line);
  white-space: pre-wrap;
  word-break: break-word;
}

.action-panel {
  align-content: start;
}

.review-grid {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 16px;
}

.field-panel {
  max-height: 70vh;
  overflow: auto;
}

.field-row {
  display: grid;
  gap: 6px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--line);
}

.field-row > div {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.collection-stack,
.mini-list,
.source-list {
  display: grid;
  gap: 12px;
}

.collection-block {
  display: grid;
  gap: 10px;
}

.mini-list span {
  padding: 10px 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.58);
  border: 1px solid var(--line);
  color: var(--ink-800);
}

.compact-tags {
  display: flex;
  flex-wrap: wrap;
}

.source-item {
  display: grid;
  gap: 4px;
  padding-bottom: 10px;
  border-bottom: 1px dashed var(--line);
}

.source-pill {
  font-size: 0.74rem;
  padding: 4px 10px;
}

.image-preview-trigger {
  padding: 0;
  border: none;
  background: transparent;
  border-radius: 18px;
  overflow: hidden;
  cursor: zoom-in;
}

.image-preview-trigger img {
  width: 100%;
  height: 220px;
  object-fit: cover;
}

.error-text {
  margin: 0;
  color: var(--danger);
  font-weight: 700;
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

.success-text {
  margin: 0;
  color: var(--success);
  font-weight: 700;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.image-preview-modal {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(34, 28, 26, 0.72);
  backdrop-filter: blur(6px);
}

.image-preview-dialog {
  position: relative;
  max-width: min(960px, 92vw);
  max-height: 90vh;
  padding: 14px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: var(--shadow-lg);
}

.image-preview-dialog img {
  display: block;
  max-width: 100%;
  max-height: calc(90vh - 28px);
  border-radius: 18px;
  object-fit: contain;
}

.image-preview-close {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 38px;
  height: 38px;
  border-radius: 999px;
  background: rgba(34, 28, 26, 0.72);
  color: #fff;
  font-size: 1.4rem;
}

@media (max-width: 1080px) {
  .compact-metrics,
  .review-grid {
    grid-template-columns: 1fr;
  }

  .workspace-form {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
