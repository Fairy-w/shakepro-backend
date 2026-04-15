<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { adminApi, type AdminExtractedFieldsResult, type AdminPageTextResult, type FieldSource } from '@/api/admin'

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
  { key: 'crawl', label: '抓取网页', description: '输入网址并抓取页面 HTML 原文' },
  { key: 'extract', label: '提取字段', description: '管理员确认 HTML 后解析站点字段' },
  { key: 'review', label: '提取预览', description: '查看已提取字段、来源标记与缺失字段' },
  { key: 'final', label: 'AI 结果', description: '调用 AI 生成中文最终结果，并补齐缺失字段' },
]

const fieldRows: FieldRow[] = [
  { key: 'name', label: '名称' },
  { key: 'englishName', label: '英文名' },
  { key: 'category', label: '分类' },
  { key: 'difficulty', label: '难度' },
  { key: 'abv', label: '酒精度' },
  { key: 'glass', label: '杯型' },
  { key: 'garnish', label: '装饰' },
  { key: 'highlight', label: 'highlight' },
  { key: 'subtitle', label: 'subtitle' },
  { key: 'description', label: 'description' },
  { key: 'story', label: 'story' },
  { key: 'flavorTags', label: 'flavorTags', type: 'list' },
  { key: 'pairings', label: 'pairings', type: 'list' },
  { key: 'serviceNotes', label: 'serviceNotes', type: 'list' },
] as const

const form = reactive({ url: '' })
const activeStage = ref<StageKey>('crawl')
const crawlLoading = ref<boolean>(false)
const extractLoading = ref<boolean>(false)
const generateLoading = ref<boolean>(false)
const crawlError = ref<string>('')
const extractError = ref<string>('')
const generateError = ref<string>('')
const crawlResult = ref<AdminPageTextResult | null>(null)
const extractResult = ref<AdminExtractedFieldsResult | null>(null)
const generateResult = ref<AdminExtractedFieldsResult | null>(null)
const previewImageUrl = ref<string>('')
const previewImageAlt = ref<string>('图片预览')

const antiCrawlerHint = computed<boolean>(() => crawlError.value.includes('403') || crawlError.value.includes('反爬'))

const stageList = computed<PipelineStageView[]>(() => {
  const statuses: Record<StageKey, StageStatus> = {
    crawl: crawlLoading.value ? 'running' : crawlError.value ? 'error' : crawlResult.value ? 'completed' : 'pending',
    extract: extractLoading.value ? 'running' : extractError.value ? 'error' : extractResult.value ? 'completed' : 'pending',
    review: extractResult.value ? 'completed' : 'pending',
    final: generateLoading.value ? 'running' : generateError.value ? 'error' : generateResult.value ? 'completed' : 'pending',
  }

  return stageDefinitions.map((stage: PipelineStageDefinition) => {
    let summary = '等待处理'
    if (stage.key === 'crawl') {
      summary = crawlLoading.value ? '正在抓取页面内容' : crawlResult.value ? `已拿到 ${crawlResult.value.html.length} 字 HTML 原文` : crawlError.value || '等待输入网址'
    } else if (stage.key === 'extract') {
      summary = extractLoading.value ? '正在提取字段' : extractResult.value ? `已提取 ${extractResult.value.ingredients.length} 个材料、${extractResult.value.steps.length} 个步骤` : extractError.value || (crawlResult.value ? '等待开始提取' : '需要先完成网页抓取')
    } else if (stage.key === 'review') {
      summary = extractResult.value ? `${extractResult.value.missingFields.length} 个字段待 AI 补充` : '等待提取结果'
    } else {
      summary = generateLoading.value ? '正在生成中文最终结果' : generateResult.value ? `AI 已生成，剩余 ${generateResult.value.missingFields.length} 个字段未完成` : generateError.value || (extractResult.value ? '等待调用 AI 生成结果' : '需要先完成字段提取')
    }

    const clickable = stage.key === 'crawl'
      || (stage.key === 'extract' && (crawlResult.value !== null || activeStage.value === 'extract'))
      || (stage.key === 'review' && (extractResult.value !== null || activeStage.value === 'review'))
      || (stage.key === 'final' && (generateResult.value !== null || activeStage.value === 'final'))

    return { ...stage, status: statuses[stage.key], summary, clickable }
  })
})

const activeStageInfo = computed<PipelineStageView>(() => stageList.value.find((item: PipelineStageView) => item.key === activeStage.value) ?? stageList.value[0]!)

async function submitCrawl(): Promise<void> {
  const url: string = form.url.trim()
  if (!url) {
    crawlError.value = '请先输入网页地址'
    return
  }

  activeStage.value = 'crawl'
  crawlLoading.value = true
  crawlError.value = ''
  extractError.value = ''
  generateError.value = ''
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

async function submitExtract(): Promise<void> {
  if (!crawlResult.value) {
    extractError.value = '请先完成网页抓取'
    activeStage.value = 'crawl'
    return
  }

  activeStage.value = 'extract'
  extractLoading.value = true
  extractError.value = ''
  generateError.value = ''
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

async function submitGenerate(): Promise<void> {
  if (!extractResult.value) {
    generateError.value = '请先完成字段提取'
    activeStage.value = 'extract'
    return
  }

  activeStage.value = 'final'
  generateLoading.value = true
  generateError.value = ''
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

function switchStage(stage: PipelineStageView): void {
  if (stage.clickable) {
    activeStage.value = stage.key
  }
}

function openImagePreview(imageUrl: string, altText: string): void {
  if (!imageUrl) {
    return
  }
  previewImageUrl.value = imageUrl
  previewImageAlt.value = altText
}

function closeImagePreview(): void {
  previewImageUrl.value = ''
  previewImageAlt.value = '图片预览'
}

function fieldValue(value?: string | null): string {
  return value?.trim() ? value : '未提取到'
}

function listValue(values?: string[] | null): string {
  return values && values.length ? values.join('、') : '未提取到'
}

function metricEntries(metrics?: Record<string, number> | null): Array<{ label: string; value: number }> {
  return metrics ? Object.entries(metrics).map(([label, value]: [string, number]) => ({ label, value })) : []
}

function fieldSource(fieldName: string, result?: AdminExtractedFieldsResult | null): FieldSource | undefined {
  return result?.fieldSources?.[fieldName]
}

function sourceLabel(source?: FieldSource): string {
  if (!source?.mode) {
    return '无标记'
  }
  const modeMap: Record<string, string> = { extracted: '已提取', derived: '派生', missing: '缺失', retained: '沿用', ai_translated: 'AI翻译', ai_generated: 'AI补全' }
  const sourceMap: Record<string, string> = { jsonld: 'JSON-LD', nextData: 'NEXT_DATA', description: 'Description', parser: '解析器', 'ai.qwen': '通义千问' }
  const mode = modeMap[source.mode] || source.mode
  const detail = source.source ? sourceMap[source.source] || source.source : ''
  return detail ? `${mode} · ${detail}` : mode
}

function displayFieldValue(
  result: AdminExtractedFieldsResult,
  fieldKey: string,
  valueType?: string,
): string {
  const record = result as unknown as Record<string, string | string[] | null | undefined>
  const value = record[fieldKey]
  if (valueType === 'list') {
    return Array.isArray(value) ? listValue(value) : '未提取到'
  }
  return typeof value === 'string' || value == null ? fieldValue(value) : '未提取到'
}
</script>

<template>
  <section class="crawler-page">
    <div class="page-head">
      <div>
        <h1 class="page-title">网页抓取流水线</h1>
        <p class="page-subtitle">先抓取 HTML 原文，再提取字段并补充来源标记，最后调用 AI 输出中文最终结果。</p>
      </div>
    </div>

    <div class="pipeline-shell card">
      <div class="pipeline-track">
        <button
          v-for="stage in stageList"
          :key="stage.key"
          class="pipeline-step"
          :class="[`status-${stage.status}`, { active: activeStage === stage.key, clickable: stage.clickable }]"
          type="button"
          :disabled="!stage.clickable"
          @click="switchStage(stage)"
        >
          <span class="step-label">{{ stage.label }}</span>
          <small class="step-summary">{{ stage.summary }}</small>
        </button>
      </div>
      <div class="pipeline-caption">
        <strong>{{ activeStageInfo.label }}</strong>
        <span>{{ activeStageInfo.description }}</span>
      </div>
    </div>

    <article v-if="activeStage === 'crawl'" class="card stage-card">
      <div class="stage-head">
        <div>
          <p class="stage-tag">阶段 1</p>
          <h2>抓取网页源码</h2>
          <p>输入公开可访问的配方网址，后台先抓取页面标题和 HTML 原文供管理员确认。</p>
        </div>
        <span class="badge">源码抓取</span>
      </div>
      <form class="stage-form" @submit.prevent="submitCrawl">
        <label class="field-group wide">
          <span>网页地址</span>
          <input v-model="form.url" class="field" type="url" placeholder="例如：https://www.example.com/cocktail/dry-martini" />
        </label>
        <div class="stage-actions">
          <button class="button-primary" type="submit" :disabled="crawlLoading">{{ crawlLoading ? '抓取中...' : '开始抓取' }}</button>
        </div>
      </form>
      <div v-if="crawlError" class="feedback-block error-block">
        <p class="error-text">{{ crawlError }}</p>
        <p v-if="antiCrawlerHint" class="hint-text">该站点可能限制了程序化访问，后续如有需要建议补浏览器抓取兜底。</p>
      </div>
      <div v-if="crawlResult" class="result-grid two-columns">
        <article class="inner-card">
          <span class="badge">抓取成功</span>
          <h3>{{ crawlResult.title || '未识别到页面标题' }}</h3>
          <dl class="summary-list">
            <div><dt>来源网址</dt><dd>{{ crawlResult.url }}</dd></div>
            <div><dt>HTML 长度</dt><dd>{{ crawlResult.html.length }} 字符</dd></div>
          </dl>
        </article>
        <article class="inner-card">
          <div class="panel-head"><h3>抓取到的 HTML 原文</h3><span class="badge subtle">源码展示</span></div>
          <pre class="text-result">{{ crawlResult.html }}</pre>
        </article>
      </div>
    </article>

    <article v-else-if="activeStage === 'extract'" class="card stage-card">
      <div class="stage-head">
        <div>
          <p class="stage-tag">阶段 2</p>
          <h2>提取字段</h2>
          <p>当前阶段只执行站点解析器，从 HTML 原文中提取字段并记录来源标记。</p>
        </div>
        <span class="badge">结构化处理中</span>
      </div>
      <div v-if="crawlResult" class="extract-layout">
        <article class="inner-card">
          <div class="panel-head"><h3>输入源</h3><span class="badge">{{ crawlResult.title || '未识别标题' }}</span></div>
          <p class="source-meta">网址：{{ crawlResult.url }}</p>
          <p class="source-meta">HTML：{{ crawlResult.html.length }} 字符。确认源码没问题后，再点击右侧开始提取。</p>
          <pre class="text-result compact">{{ crawlResult.html }}</pre>
        </article>
        <article class="inner-card action-card">
          <h3>开始提取</h3>
          <p>这一段不会走 AI，只执行站点解析器；解析不到的字段会直接保留为 null 或空集合。</p>
          <button class="button-primary" type="button" :disabled="extractLoading" @click="submitExtract">{{ extractLoading ? '提取中...' : '提取字段' }}</button>
          <p v-if="extractError" class="error-text inline-gap">{{ extractError }}</p>
        </article>
      </div>
    </article>

    <article v-else-if="activeStage === 'review'" class="card stage-card">
      <div class="stage-head">
        <div>
          <p class="stage-tag">阶段 3</p>
          <h2>提取结果预览</h2>
          <p>这里展示解析器拿到的原始字段、来源标记与缺失字段。确认后再调用 AI 生成中文最终结果。</p>
        </div>
        <span class="badge">字段审核</span>
      </div>
      <div v-if="extractResult" class="preview-layout">
        <article class="inner-card">
          <div class="panel-head"><h3>提取概览</h3><span class="badge">{{ extractResult.extractMode || '未标记模式' }}</span></div>
          <dl class="summary-list">
            <div v-for="item in fieldRows" :key="item.key">
              <dt>{{ item.label }} <span class="source-chip">{{ sourceLabel(fieldSource(item.key, extractResult)) }}</span></dt>
              <dd>{{ displayFieldValue(extractResult, item.key, item.type) }}</dd>
            </div>
          </dl>
          <div v-if="extractResult.heroImage" class="hero-preview">
            <button class="image-preview-trigger" type="button" @click="openImagePreview(extractResult.heroImage, '抓取到的主图')">
              <img :src="extractResult.heroImage" alt="抓取到的主图" />
            </button>
            <p class="field-source-line">heroImage：{{ sourceLabel(fieldSource('heroImage', extractResult)) }}</p>
          </div>
          <div v-else class="missing-hero">未提取到主图地址</div>
        </article>

        <article class="inner-card">
          <div class="panel-head"><h3>缺失字段</h3><span class="badge subtle">{{ extractResult.missingFields.length }} 项</span></div>
          <div v-if="extractResult.missingFields.length" class="missing-list">
            <span v-for="item in extractResult.missingFields" :key="item" class="badge warning">{{ item }}</span>
          </div>
          <p v-else class="hint-text inline-gap">当前可识别字段已全部命中，可以直接进入 AI 中文生成。</p>
          <div class="ai-action">
            <button class="button-primary" type="button" :disabled="generateLoading" @click="submitGenerate">{{ generateLoading ? 'AI 生成中...' : '调用 AI 生成中文最终结果' }}</button>
            <p v-if="generateError" class="error-text inline-gap">{{ generateError }}</p>
          </div>
        </article>

        <article class="inner-card wide-panel">
          <div class="panel-head"><h3>材料清单</h3><span class="badge">{{ extractResult.ingredients.length }} 项</span></div>
          <p class="field-source-line">{{ sourceLabel(fieldSource('ingredients', extractResult)) }}</p>
          <div v-if="extractResult.ingredients.length" class="list-grid">
            <div v-for="(item, index) in extractResult.ingredients" :key="`${item.name || 'material'}-${index}`" class="list-item">
              <strong>{{ item.name || '未识别材料名' }}</strong>
              <span>{{ item.amount || '未识别用量' }}</span>
              <p v-if="item.note" class="ingredient-note">{{ item.note }}</p>
              <p v-else class="ingredient-note muted">未提取到材料说明</p>
            </div>
          </div>
        </article>

        <article class="inner-card wide-panel">
          <div class="panel-head"><h3>制作步骤</h3><span class="badge">{{ extractResult.steps.length }} 步</span></div>
          <p class="field-source-line">{{ sourceLabel(fieldSource('steps', extractResult)) }}</p>
          <div v-if="extractResult.steps.length" class="step-list">
            <article v-for="(item, index) in extractResult.steps" :key="`${item.title || 'step'}-${index}`" class="step-card">
              <strong>{{ item.title || `步骤${index + 1}` }}</strong>
              <p>{{ item.detail || '未识别步骤说明' }}</p>
            </article>
          </div>
        </article>

        <article class="inner-card wide-panel">
          <div class="panel-head"><h3>风味指标</h3><span class="badge subtle">{{ metricEntries(extractResult.flavorMetrics).length }} 项</span></div>
          <p class="field-source-line">{{ sourceLabel(fieldSource('flavorMetrics', extractResult)) }}</p>
          <div v-if="metricEntries(extractResult.flavorMetrics).length" class="metric-grid">
            <div v-for="item in metricEntries(extractResult.flavorMetrics)" :key="item.label" class="metric-item">
              <strong>{{ item.label }}</strong>
              <span>{{ item.value }}</span>
            </div>
          </div>
        </article>
      </div>
    </article>

    <article v-else class="card stage-card">
      <div class="stage-head">
        <div>
          <p class="stage-tag">阶段 4</p>
          <h2>AI 中文最终结果</h2>
          <p>这里展示 AI 翻译并补全后的中文结果。当前会直接复用已提取字段，不再重复解析 HTML。</p>
        </div>
        <span class="badge">最终结果</span>
      </div>
      <div v-if="generateResult" class="preview-layout">
        <article class="inner-card">
          <div class="panel-head"><h3>最终概览</h3><span class="badge">{{ generateResult.generateMode || '未标记模式' }}</span></div>
          <dl class="summary-list">
            <div v-for="item in fieldRows" :key="item.key">
              <dt>{{ item.label }} <span class="source-chip">{{ sourceLabel(fieldSource(item.key, generateResult)) }}</span></dt>
              <dd>{{ displayFieldValue(generateResult, item.key, item.type) }}</dd>
            </div>
          </dl>
          <div v-if="generateResult.heroImage" class="hero-preview">
            <button class="image-preview-trigger" type="button" @click="openImagePreview(generateResult.heroImage, '抓取到的主图')">
              <img :src="generateResult.heroImage" alt="抓取到的主图" />
            </button>
            <p class="field-source-line">heroImage：{{ sourceLabel(fieldSource('heroImage', generateResult)) }}</p>
          </div>
          <div v-else class="missing-hero">未提取到主图地址</div>
        </article>

        <article class="inner-card">
          <div class="panel-head"><h3>最终缺失字段</h3><span class="badge subtle">{{ generateResult.missingFields.length }} 项</span></div>
          <div v-if="generateResult.missingFields.length" class="missing-list">
            <span v-for="item in generateResult.missingFields" :key="item" class="badge warning">{{ item }}</span>
          </div>
          <p v-else class="hint-text inline-gap">最终结果字段已补齐，可继续后续人工审核或入库。</p>
          <div class="result-note">
            <span class="badge">生成说明</span>
            <p>解析模式：{{ generateResult.extractMode || '未标记' }}</p>
            <p>AI 模式：{{ generateResult.generateMode || '未标记' }}</p>
          </div>
        </article>

        <article class="inner-card wide-panel">
          <div class="panel-head"><h3>中文材料清单</h3><span class="badge">{{ generateResult.ingredients.length }} 项</span></div>
          <p class="field-source-line">{{ sourceLabel(fieldSource('ingredients', generateResult)) }}</p>
          <div v-if="generateResult.ingredients.length" class="list-grid">
            <div v-for="(item, index) in generateResult.ingredients" :key="`${item.name || 'material'}-${index}`" class="list-item">
              <strong>{{ item.name || '未生成中文材料名' }}</strong>
              <span>{{ item.amount || '未生成用量' }}</span>
              <p v-if="item.note" class="ingredient-note">{{ item.note }}</p>
              <p v-else class="ingredient-note muted">未生成材料说明</p>
            </div>
          </div>
        </article>

        <article class="inner-card wide-panel">
          <div class="panel-head"><h3>中文制作步骤</h3><span class="badge">{{ generateResult.steps.length }} 步</span></div>
          <p class="field-source-line">{{ sourceLabel(fieldSource('steps', generateResult)) }}</p>
          <div v-if="generateResult.steps.length" class="step-list">
            <article v-for="(item, index) in generateResult.steps" :key="`${item.title || 'step'}-${index}`" class="step-card">
              <strong>{{ item.title || `步骤${index + 1}` }}</strong>
              <p>{{ item.detail || '未生成步骤说明' }}</p>
            </article>
          </div>
        </article>

        <article class="inner-card wide-panel">
          <div class="panel-head"><h3>中文风味指标</h3><span class="badge subtle">{{ metricEntries(generateResult.flavorMetrics).length }} 项</span></div>
          <p class="field-source-line">{{ sourceLabel(fieldSource('flavorMetrics', generateResult)) }}</p>
          <div v-if="metricEntries(generateResult.flavorMetrics).length" class="metric-grid">
            <div v-for="item in metricEntries(generateResult.flavorMetrics)" :key="item.label" class="metric-item">
              <strong>{{ item.label }}</strong>
              <span>{{ item.value }}</span>
            </div>
          </div>
        </article>
      </div>
      <article v-else class="inner-card empty-card">
        <h3>还没有 AI 最终结果</h3>
        <p>请先在“提取预览”阶段确认解析结果，然后点击按钮生成中文最终结果。</p>
      </article>
    </article>

    <div v-if="previewImageUrl" class="image-preview-modal" @click.self="closeImagePreview">
      <div class="image-preview-dialog">
        <button class="image-preview-close" type="button" aria-label="关闭图片预览" @click="closeImagePreview">×</button>
        <img :src="previewImageUrl" :alt="previewImageAlt" />
      </div>
    </div>
  </section>
</template>

<style scoped>
.crawler-page { display: grid; gap: 20px; }
.pipeline-shell, .stage-card { padding: 22px; }
.pipeline-track { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.pipeline-step { min-height: 88px; border: none; padding: 16px 18px; text-align: left; color: #fff; border-radius: 18px; background: linear-gradient(135deg, #7d8b99, #637180); }
.pipeline-step.clickable { cursor: pointer; }
.pipeline-step.status-completed, .pipeline-step.status-running, .pipeline-step.active { background: linear-gradient(135deg, #1da765, #0f766e); }
.pipeline-step.status-error { background: linear-gradient(135deg, #dc2626, #991b1b); }
.pipeline-step.status-pending { background: linear-gradient(135deg, #8ea0b2, #6f8295); }
.step-label { display: block; font-weight: 700; font-size: 1rem; }
.step-summary { display: block; margin-top: 6px; opacity: 0.92; font-size: 0.82rem; }
.pipeline-caption, .stage-head, .panel-head, .stage-actions { display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.pipeline-caption { margin-top: 16px; color: var(--ink-600); }
.pipeline-caption strong { color: var(--ink-950); }
.stage-tag { margin: 0; letter-spacing: 0.16em; font-size: 0.78rem; color: var(--ink-600); }
.stage-head h2, .inner-card h3 { margin: 10px 0 0; font-size: 1.45rem; letter-spacing: -0.04em; }
.stage-head p, .inner-card p, .source-meta { color: var(--ink-600); }
.stage-form, .summary-list, .missing-list, .list-grid, .step-list, .metric-grid { display: grid; gap: 12px; }
.stage-form, .result-grid, .extract-layout, .preview-layout { margin-top: 20px; }
.field-group { display: grid; gap: 8px; }
.field-group span { font-weight: 700; color: var(--ink-800); }
.wide { max-width: 880px; }
.error-text { color: var(--danger); font-weight: 700; }
.result-grid, .extract-layout { display: grid; grid-template-columns: minmax(280px, 360px) minmax(0, 1fr); gap: 18px; }
.preview-layout { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }
.inner-card { padding: 20px; border-radius: 24px; background: rgba(255,255,255,0.52); border: 1px solid rgba(255,255,255,0.55); box-shadow: inset 0 1px 0 rgba(255,255,255,0.58); }
.wide-panel { grid-column: span 2; }
.summary-list dt { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; color: var(--ink-600); font-size: 0.9rem; }
.summary-list dd { margin: 0; color: var(--ink-950); word-break: break-word; }
.text-result { margin: 16px 0 0; padding: 18px; min-height: 340px; max-height: 62vh; overflow: auto; white-space: pre-wrap; word-break: break-word; border-radius: 20px; background: rgba(246,250,252,0.92); border: 1px solid rgba(16,32,46,0.08); color: var(--ink-950); }
.text-result.compact { min-height: 240px; }
.hero-preview, .missing-hero, .result-note, .ai-action { margin-top: 18px; }
.hero-preview { overflow: hidden; border-radius: 22px; background: rgba(255,255,255,0.62); }
.image-preview-trigger { display: block; width: 100%; padding: 0; border: none; background: transparent; cursor: zoom-in; }
.hero-preview img { display: block; width: 100%; height: 220px; object-fit: cover; }
.missing-hero, .result-note, .ai-action { padding: 16px; border-radius: 18px; background: rgba(15,118,110,0.08); color: var(--ink-600); }
.list-grid, .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.list-item, .step-card, .metric-item { padding: 16px; border-radius: 18px; background: rgba(255,255,255,0.62); border: 1px solid rgba(16,32,46,0.06); }
.list-item { display: grid; gap: 6px; }
.ingredient-note { margin: 0; font-size: 0.9rem; line-height: 1.45; color: var(--ink-700); }
.ingredient-note.muted { color: var(--ink-500); }
.list-item span, .step-card p, .metric-item span { color: var(--ink-600); }
.metric-item { display: grid; gap: 6px; }
.source-chip, .field-source-line { color: #0f766e; font-weight: 700; }
.source-chip { display: inline-flex; align-items: center; border-radius: 999px; padding: 2px 10px; background: rgba(15,118,110,0.12); font-size: 0.78rem; }
.warning { background: rgba(245,158,11,0.16); color: #b45309; border-color: rgba(245,158,11,0.12); }
.image-preview-modal { position: fixed; inset: 0; z-index: 60; display: flex; align-items: center; justify-content: center; padding: 24px; background: rgba(8, 15, 24, 0.72); backdrop-filter: blur(6px); }
.image-preview-dialog { position: relative; max-width: min(960px, 92vw); max-height: 90vh; padding: 14px; border-radius: 24px; background: rgba(255,255,255,0.96); box-shadow: 0 22px 70px rgba(8, 15, 24, 0.28); }
.image-preview-dialog img { display: block; max-width: 100%; max-height: calc(90vh - 28px); border-radius: 18px; object-fit: contain; }
.image-preview-close { position: absolute; top: 8px; right: 8px; width: 38px; height: 38px; border: none; border-radius: 999px; background: rgba(8, 15, 24, 0.72); color: #fff; font-size: 1.4rem; line-height: 1; cursor: pointer; }
@media (max-width: 1080px) {
  .pipeline-track, .result-grid, .extract-layout, .preview-layout, .list-grid, .metric-grid { grid-template-columns: 1fr; }
  .wide-panel { grid-column: span 1; }
}
</style>
