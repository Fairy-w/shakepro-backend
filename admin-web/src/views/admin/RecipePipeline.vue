<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import {
  RECIPE_PIPELINE_STATUSES,
  recipePipelineApi,
  type RecipeAiDetailGenerateResponse,
  type RecipeCandidateUpdateRequest,
  type RecipeCrawlTaskRequest,
  type RecipeCrawlTaskResponse,
  type RecipeDetailPage,
  type RecipeDetailValidationResult,
  type RecipeFlavorMetricItem,
  type RecipeIngredientItem,
  type RecipeReviewPublishResponse,
  type RecipeSourceRecord,
  type RecipeStepItem,
  type RecipeStructuredRecord,
} from '@/api/recipePipeline'

type NoticeTone = 'success' | 'error' | 'info'
type LoadingKey =
  | 'createTask'
  | 'sourceList'
  | 'sourceDetail'
  | 'structuredList'
  | 'structuredDetail'
  | 'parse'
  | 'sourceReject'
  | 'candidateList'
  | 'candidateDetail'
  | 'ai'
  | 'save'
  | 'publish'
  | 'reject'

type StageKey = 'crawl' | 'parse' | 'ai' | 'review'
type StageVisualState = 'done' | 'current' | 'pending'
type NextActionKey = 'parse' | 'ai' | 'save' | 'publish' | 'none'

interface NoticeState {
  tone: NoticeTone
  text: string
}

interface ReviewDraft {
  id: string
  name: string
  englishName: string
  category: string
  heroImage: string
  highlight: string
  subtitle: string
  description: string
  story: string
  bestFor: string
  difficulty: string
  duration: string
  abv: string
  volume: string
  glass: string
  garnish: string
  serveTemperature: string
  flavorTags: string[]
  flavorMetrics: RecipeFlavorMetricItem[]
  pairings: string[]
  serviceNotes: string[]
  ingredients: RecipeIngredientItem[]
  steps: RecipeStepItem[]
}

interface ValidationView {
  label: string
  passed: boolean
  errors: string[]
}

interface AiPreview {
  detailContentId: number | null
  structuredRecordId: number | null
  recipeKey: string
  status: string
  aiGeneratedAt?: string | null
  sourceSite?: string | null
  sourceUrl?: string | null
  validation: RecipeDetailValidationResult
  detail: RecipeDetailPage
}

interface StageNode {
  key: StageKey
  index: string
  label: string
  hint: string
  status: string
  state: StageVisualState
}

interface NextActionMeta {
  action: NextActionKey
  label: string
  hint: string
  disabled: boolean
}

const statusOptions = ['', ...RECIPE_PIPELINE_STATUSES]
const defaultFlavorMetricLabels = ['酒感', '清爽', '酸度', '甜感']

const loading = reactive<Record<LoadingKey, boolean>>({
  createTask: false,
  sourceList: false,
  sourceDetail: false,
  structuredList: false,
  structuredDetail: false,
  parse: false,
  sourceReject: false,
  candidateList: false,
  candidateDetail: false,
  ai: false,
  save: false,
  publish: false,
  reject: false,
})

const notice = ref<NoticeState | null>(null)
const lastTask = ref<RecipeCrawlTaskResponse | null>(null)

const crawlForm = reactive<RecipeCrawlTaskRequest>({
  sourceSite: '',
  entryUrl: '',
  crawlMode: 'detail',
  maxPages: 1,
  maxItems: 20,
  fetchDetailPages: true,
})

const sourceStatusFilter = ref('')
const reviewComment = ref('')

const sourceRecords = ref<RecipeSourceRecord[]>([])
const structuredRecords = ref<RecipeStructuredRecord[]>([])
const candidates = ref<RecipeReviewPublishResponse[]>([])

const activeSourceId = ref<number | null>(null)
const activeStructuredId = ref<number | null>(null)
const activeCandidateId = ref<number | null>(null)

const activeSource = ref<RecipeSourceRecord | null>(null)
const activeStructured = ref<RecipeStructuredRecord | null>(null)
const activeCandidate = ref<RecipeReviewPublishResponse | null>(null)
const activeAiResult = ref<RecipeAiDetailGenerateResponse | null>(null)

const reviewForm = ref<ReviewDraft>(createEmptyReviewDraft())
const savedReviewSignature = ref(getDraftSignature(reviewForm.value))
const stageFocus = ref<StageKey>('crawl')

const crawlStageRef = ref<HTMLElement | null>(null)
const parseStageRef = ref<HTMLElement | null>(null)
const aiStageRef = ref<HTMLElement | null>(null)
const reviewStageRef = ref<HTMLElement | null>(null)

const recentCandidateSummaries = computed(() => candidates.value.slice(0, 6))
const hasDirtyReview = computed(() => getDraftSignature(reviewForm.value) !== savedReviewSignature.value)
const localValidationErrors = computed(() => validateReviewDraft(reviewForm.value))

const validationView = computed<ValidationView | null>(() => {
  if (activeCandidate.value) {
    if (hasDirtyReview.value) {
      return {
        label: '当前草稿校验',
        passed: localValidationErrors.value.length === 0,
        errors: localValidationErrors.value,
      }
    }

    return {
      label: '候选详情校验',
      passed: activeCandidate.value.validation.passed,
      errors: activeCandidate.value.validation.errors ?? [],
    }
  }

  if (activeAiResult.value) {
    return {
      label: 'AI 生成结果校验',
      passed: activeAiResult.value.validation.passed,
      errors: activeAiResult.value.validation.errors ?? [],
    }
  }

  return null
})

const aiPreview = computed<AiPreview | null>(() => {
  if (activeAiResult.value) {
    return activeAiResult.value
  }

  if (!activeCandidate.value) {
    return null
  }

  return {
    detailContentId: activeCandidate.value.detailContentId,
    structuredRecordId: activeStructured.value?.id ?? null,
    recipeKey: activeCandidate.value.recipeKey,
    status: activeCandidate.value.status,
    aiGeneratedAt: null,
    sourceSite: activeSource.value?.sourceSite ?? null,
    sourceUrl: activeSource.value?.sourceUrl ?? null,
    validation: activeCandidate.value.validation,
    detail: activeCandidate.value.detail,
  }
})

const canPublish = computed(() => {
  if (!activeCandidate.value) {
    return false
  }

  return !hasDirtyReview.value && localValidationErrors.value.length === 0 && activeCandidate.value.validation.passed
})

const selectedTitle = computed(() => {
  if (activeCandidate.value?.detail.name) {
    return activeCandidate.value.detail.name
  }
  if (activeStructured.value?.englishName) {
    return activeStructured.value.englishName
  }
  return activeSource.value?.sourceUrl || '等待进入流水线的配方记录'
})

const selectedSubline = computed(() => {
  if (!activeSource.value) {
    return '从左侧任务池选择一条原始记录，先完成人工抓取审核，再进入 AI 事实解析、AI 详情生成和审核发布。'
  }

  const parts = [
    activeSource.value.sourceSite,
    activeStructured.value?.recipeKey,
    activeCandidate.value?.status || aiPreview.value?.status,
  ].filter((item): item is string => Boolean(item))

  return parts.join(' · ') || '已锁定当前处理链路'
})

const currentStageKey = computed<StageKey>(() => resolvePreferredStage())

const stageNodes = computed<StageNode[]>(() => {
  const completedAll = activeCandidate.value?.status === '已发布' || activeCandidate.value?.status === '已驳回'
  const order: StageKey[] = ['crawl', 'parse', 'ai', 'review']
  const currentIndex = order.indexOf(currentStageKey.value)

  return [
    {
      key: 'crawl',
      index: '01',
      label: '原始抓取入池',
      hint: activeSource.value ? compactText(activeSource.value.sourceUrl, '原始记录已锁定') : '等待抓取或选择原始记录',
      status: activeSource.value?.status || '待处理',
      state: completedAll || currentIndex > 0 ? 'done' : currentIndex === 0 ? 'current' : 'pending',
    },
    {
      key: 'parse',
      index: '02',
      label: 'AI 事实解析',
      hint: activeStructured.value ? compactText(activeStructured.value.englishName, '结构化结果已生成') : '等待抓取审核通过后进入 AI 事实解析',
      status: activeStructured.value?.status || '待处理',
      state: completedAll || currentIndex > 1 ? 'done' : currentIndex === 1 ? 'current' : 'pending',
    },
    {
      key: 'ai',
      index: '03',
      label: 'AI 详情生成',
      hint: aiPreview.value ? compactText(aiPreview.value.detail.subtitle, 'AI 详情已生成') : '等待 AI 补齐展示文案',
      status: aiPreview.value?.status || '待处理',
      state: completedAll || currentIndex > 2 ? 'done' : currentIndex === 2 ? 'current' : 'pending',
    },
    {
      key: 'review',
      index: '04',
      label: '审核发布',
      hint: activeCandidate.value
        ? activeCandidate.value.status === '已发布'
          ? '已同步正式配方'
          : activeCandidate.value.status === '已驳回'
            ? '当前候选已驳回'
            : '等待人工复核与发布'
        : '等待进入审核区',
      status: activeCandidate.value?.status || '待处理',
      state: completedAll ? 'done' : currentIndex === 3 ? 'current' : 'pending',
    },
  ]
})

const nextAction = computed<NextActionMeta>(() => {
  if (!activeSource.value) {
    return {
      action: 'none',
      label: '先选择原始记录',
      hint: '从左侧任务池进入一条记录后，流水线才会开始工作。',
      disabled: true,
    }
  }

  if (activeSource.value.status === '已驳回') {
    return {
      action: 'none',
      label: '当前抓取已驳回',
      hint: '这条原始记录已经被人工否决，不会继续进入 AI 事实解析。',
      disabled: true,
    }
  }

  if (!activeStructured.value) {
    return {
      action: 'parse',
      label: loading.parse ? 'AI 事实解析中...' : '人工通过后执行 AI 事实解析',
      hint: '管理员确认抓取内容无误后，再让 AI 抽取事实字段。',
      disabled: loading.parse,
    }
  }

  if (!aiPreview.value) {
    return {
      action: 'ai',
      label: loading.ai ? 'AI 详情生成中...' : '执行 AI 详情生成',
      hint: '基于结构化事实补齐适合展示的详情文案。',
      disabled: loading.ai,
    }
  }

  if (!activeCandidate.value) {
    return {
      action: 'none',
      label: '等待候选详情载入',
      hint: '当前链路还没有进入可审核的候选详情。',
      disabled: true,
    }
  }

  if (activeCandidate.value.status === '已发布') {
    return {
      action: 'none',
      label: '当前链路已发布',
      hint: '这条配方已经完成审核并同步正式数据。',
      disabled: true,
    }
  }

  if (activeCandidate.value.status === '已驳回') {
    return {
      action: 'none',
      label: '当前链路已驳回',
      hint: '如果需要继续处理，可重新抓取或重新生成。',
      disabled: true,
    }
  }

  if (hasDirtyReview.value) {
    return {
      action: 'save',
      label: loading.save ? '保存草稿中...' : '先保存当前草稿',
      hint: '当前表单有未保存修改，保存后才能进入发布动作。',
      disabled: loading.save,
    }
  }

  if (!canPublish.value) {
    return {
      action: 'none',
      label: '修正校验问题后发布',
      hint: '右侧审核区仍有校验错误，暂不允许误发布。',
      disabled: true,
    }
  }

  return {
    action: 'publish',
    label: loading.publish ? '通过审核并发布中...' : '通过审核并发布',
    hint: '候选详情已满足发布条件，可直接进入正式配方库。',
    disabled: loading.publish,
  }
})

function createEmptyIngredient(): RecipeIngredientItem {
  return {
    name: '',
    amount: '',
    note: '',
  }
}

function createEmptyStep(): RecipeStepItem {
  return {
    title: '',
    detail: '',
    hint: '',
  }
}

function createDefaultFlavorMetrics(): RecipeFlavorMetricItem[] {
  return defaultFlavorMetricLabels.map((label, index) => ({
    label,
    value: index === 0 ? 4 : 3,
  }))
}

function createEmptyReviewDraft(): ReviewDraft {
  return {
    id: '',
    name: '',
    englishName: '',
    category: '',
    heroImage: '',
    highlight: '',
    subtitle: '',
    description: '',
    story: '',
    bestFor: '',
    difficulty: '',
    duration: '',
    abv: '',
    volume: '',
    glass: '',
    garnish: '',
    serveTemperature: '',
    flavorTags: [''],
    flavorMetrics: createDefaultFlavorMetrics(),
    pairings: [''],
    serviceNotes: [''],
    ingredients: [createEmptyIngredient()],
    steps: [createEmptyStep()],
  }
}

function getDraftSignature(draft: ReviewDraft): string {
  return JSON.stringify(draft)
}

function showNotice(tone: NoticeTone, text: string) {
  notice.value = { tone, text }
}

function getErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return '操作失败，请稍后重试'
}

async function withLoading<T>(key: LoadingKey, action: () => Promise<T>): Promise<T> {
  loading[key] = true
  try {
    return await action()
  } finally {
    loading[key] = false
  }
}

function clampMetricValue(value: number | null | undefined): number {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return 3
  }
  return Math.min(5, Math.max(1, Math.round(value)))
}

function normalizeStringList(values: string[]): string[] {
  return values.map((item) => item.trim()).filter((item) => item.length > 0)
}

function mapDetailToDraft(detail: RecipeDetailPage): ReviewDraft {
  return {
    id: detail.id || '',
    name: detail.name || '',
    englishName: detail.englishName || '',
    category: detail.category || '',
    heroImage: detail.heroImage || '',
    highlight: detail.highlight || '',
    subtitle: detail.subtitle || '',
    description: detail.description || '',
    story: detail.story || '',
    bestFor: detail.bestFor || '',
    difficulty: detail.difficulty || '',
    duration: detail.duration || '',
    abv: detail.abv || '',
    volume: detail.volume || '',
    glass: detail.glass || '',
    garnish: detail.garnish || '',
    serveTemperature: detail.serveTemperature || '',
    flavorTags: detail.flavorTags?.length ? [...detail.flavorTags] : [''],
    flavorMetrics: detail.flavorMetrics?.length
      ? detail.flavorMetrics.map((item) => ({
          label: item.label || '',
          value: clampMetricValue(item.value),
        }))
      : createDefaultFlavorMetrics(),
    pairings: detail.pairings?.length ? [...detail.pairings] : [''],
    serviceNotes: detail.serviceNotes?.length ? [...detail.serviceNotes] : [''],
    ingredients: detail.ingredients?.length
      ? detail.ingredients.map((item) => ({
          name: item.name || '',
          amount: item.amount || '',
          note: item.note || '',
        }))
      : [createEmptyIngredient()],
    steps: detail.steps?.length
      ? detail.steps.map((item) => ({
          title: item.title || '',
          detail: item.detail || '',
          hint: item.hint || '',
        }))
      : [createEmptyStep()],
  }
}

function resetReviewDraft() {
  reviewForm.value = createEmptyReviewDraft()
  savedReviewSignature.value = getDraftSignature(reviewForm.value)
  reviewComment.value = ''
}

function clearCandidateSelection() {
  activeCandidateId.value = null
  activeCandidate.value = null
  activeAiResult.value = null
  resetReviewDraft()
}

function clearStructuredSelection() {
  activeStructuredId.value = null
  activeStructured.value = null
  clearCandidateSelection()
}

function clearSourceSelection() {
  activeSourceId.value = null
  activeSource.value = null
  clearStructuredSelection()
}

function formatDateTime(value?: string | null): string {
  return value ? value.replace('T', ' ') : '-'
}

function compactText(value?: string | null, fallback = '暂无内容'): string {
  const text = value?.trim()
  return text ? text : fallback
}

function statusClass(status?: string | null): string {
  if (status === '已发布') {
    return 'published'
  }
  if (status === '已驳回') {
    return 'rejected'
  }
  if (status === 'AI已生成') {
    return 'generated'
  }
  if (status === '已解析') {
    return 'parsed'
  }
  if (status === '待审核') {
    return 'pending'
  }
  return 'scraped'
}

function stageStateClass(state: StageVisualState): string {
  if (state === 'done') {
    return 'done'
  }
  if (state === 'current') {
    return 'current'
  }
  return 'pending'
}

function resolvePreferredStage(): StageKey {
  if (!activeSource.value) {
    return 'crawl'
  }
  if (!activeStructured.value) {
    return 'parse'
  }
  if (!aiPreview.value) {
    return 'ai'
  }
  return 'review'
}

function getStageElement(stage: StageKey): HTMLElement | null {
  if (stage === 'crawl') return crawlStageRef.value
  if (stage === 'parse') return parseStageRef.value
  if (stage === 'ai') return aiStageRef.value
  return reviewStageRef.value
}

async function setStageFocus(stage: StageKey, shouldScroll = true) {
  stageFocus.value = stage
  if (!shouldScroll) {
    return
  }

  await nextTick()
  const element = getStageElement(stage)
  element?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function syncStageFocus(shouldScroll = false) {
  return setStageFocus(resolvePreferredStage(), shouldScroll)
}

function validateReviewDraft(draft: ReviewDraft): string[] {
  if (!activeCandidate.value && !activeAiResult.value) {
    return []
  }

  const errors: string[] = []

  if (!draft.id.trim()) errors.push('配方 ID 不能为空')
  if (!draft.name.trim()) errors.push('中文名不能为空')
  if (!draft.category.trim()) errors.push('分类不能为空')
  if (!draft.heroImage.trim()) errors.push('主图不能为空')
  if (!draft.subtitle.trim()) errors.push('副标题不能为空')
  if (!draft.description.trim()) errors.push('简介不能为空')
  if (!draft.bestFor.trim()) errors.push('适合场景不能为空')
  if (!draft.difficulty.trim()) errors.push('制作难度不能为空')
  if (!draft.duration.trim()) errors.push('制作时长不能为空')
  if (!draft.abv.trim()) errors.push('酒精度不能为空')
  if (!draft.volume.trim()) errors.push('出杯容量不能为空')
  if (!draft.glass.trim()) errors.push('杯型不能为空')
  if (!draft.garnish.trim()) errors.push('装饰不能为空')
  if (!draft.serveTemperature.trim()) errors.push('饮用温度不能为空')

  if (!normalizeStringList(draft.flavorTags).length) {
    errors.push('至少填写一个风味标签')
  }

  const validMetrics = draft.flavorMetrics.filter((item) => item.label.trim())
  if (!validMetrics.length) {
    errors.push('至少填写一个风味维度')
  }
  if (validMetrics.some((item) => clampMetricValue(item.value) !== item.value)) {
    errors.push('风味维度值必须在 1 到 5 之间')
  }

  const validIngredients = draft.ingredients.filter((item) => item.name.trim() || item.amount.trim() || (item.note || '').trim())
  if (!validIngredients.length) {
    errors.push('至少填写一条配料')
  }
  if (validIngredients.some((item) => !item.name.trim() || !item.amount.trim())) {
    errors.push('配料名称和用量都不能为空')
  }

  const validSteps = draft.steps.filter((item) => item.title.trim() || item.detail.trim() || (item.hint || '').trim())
  if (!validSteps.length) {
    errors.push('至少填写一步制作步骤')
  }
  if (validSteps.some((item) => !item.title.trim() || !item.detail.trim())) {
    errors.push('步骤标题和说明都不能为空')
  }

  return [...new Set(errors)]
}

function buildCandidatePayload(draft: ReviewDraft): RecipeCandidateUpdateRequest {
  return {
    id: draft.id.trim(),
    name: draft.name.trim(),
    englishName: draft.englishName.trim() || undefined,
    category: draft.category.trim(),
    heroImage: draft.heroImage.trim(),
    highlight: draft.highlight.trim() || undefined,
    subtitle: draft.subtitle.trim(),
    description: draft.description.trim(),
    story: draft.story.trim() || undefined,
    bestFor: draft.bestFor.trim(),
    difficulty: draft.difficulty.trim(),
    duration: draft.duration.trim(),
    abv: draft.abv.trim(),
    volume: draft.volume.trim(),
    glass: draft.glass.trim(),
    garnish: draft.garnish.trim(),
    serveTemperature: draft.serveTemperature.trim(),
    flavorTags: normalizeStringList(draft.flavorTags),
    flavorMetrics: draft.flavorMetrics
      .map((item) => ({
        label: item.label.trim(),
        value: clampMetricValue(item.value),
      }))
      .filter((item) => item.label),
    pairings: normalizeStringList(draft.pairings),
    serviceNotes: normalizeStringList(draft.serviceNotes),
    ingredients: draft.ingredients
      .map((item) => ({
        name: item.name.trim(),
        amount: item.amount.trim(),
        note: item.note?.trim() || undefined,
      }))
      .filter((item) => item.name || item.amount || item.note),
    steps: draft.steps
      .map((item) => ({
        title: item.title.trim(),
        detail: item.detail.trim(),
        hint: item.hint?.trim() || undefined,
      }))
      .filter((item) => item.title || item.detail || item.hint),
  }
}

function removeStringItem(list: string[], index: number) {
  if (list.length === 1) {
    list[0] = ''
    return
  }
  list.splice(index, 1)
}

function removeIngredient(index: number) {
  if (reviewForm.value.ingredients.length === 1) {
    reviewForm.value.ingredients[0] = createEmptyIngredient()
    return
  }
  reviewForm.value.ingredients.splice(index, 1)
}

function removeStep(index: number) {
  if (reviewForm.value.steps.length === 1) {
    reviewForm.value.steps[0] = createEmptyStep()
    return
  }
  reviewForm.value.steps.splice(index, 1)
}

function removeFlavorMetric(index: number) {
  if (reviewForm.value.flavorMetrics.length === 1) {
    reviewForm.value.flavorMetrics[0] = { label: defaultFlavorMetricLabels[0] ?? '酒感', value: 3 }
    return
  }
  reviewForm.value.flavorMetrics.splice(index, 1)
}

function findStructuredSummaryBySource(sourceId: number): RecipeStructuredRecord | null {
  return structuredRecords.value.find((item) => item.sourceRecordId === sourceId) || null
}

function findStructuredSummaryByRecipeKey(recipeKey: string): RecipeStructuredRecord | null {
  return structuredRecords.value.find((item) => item.recipeKey === recipeKey) || null
}

async function loadStructuredRecords() {
  try {
    structuredRecords.value = await withLoading('structuredList', () => recipePipelineApi.getStructuredRecords())
  } catch (error) {
    showNotice('error', getErrorMessage(error))
  }
}

async function loadCandidates() {
  try {
    candidates.value = await withLoading('candidateList', () => recipePipelineApi.getCandidates())
  } catch (error) {
    showNotice('error', getErrorMessage(error))
  }
}

async function loadSourceRecords(autoSelect = false) {
  try {
    sourceRecords.value = await withLoading('sourceList', () =>
      recipePipelineApi.getSourceRecords({ status: sourceStatusFilter.value || undefined })
    )
    if (!sourceRecords.value.length) {
      clearSourceSelection()
      await setStageFocus('crawl', false)
      return
    }

    if (autoSelect) {
      const targetId =
        activeSourceId.value && sourceRecords.value.some((item) => item.id === activeSourceId.value)
          ? activeSourceId.value
          : sourceRecords.value[0]?.id ?? null

      if (targetId) {
        await selectSource(targetId)
      }
    }
  } catch (error) {
    showNotice('error', getErrorMessage(error))
  }
}

async function loadCandidateDetail(id: number, shouldFocus = true) {
  activeCandidateId.value = id
  activeCandidate.value = null

  try {
    const detail = await withLoading('candidateDetail', () => recipePipelineApi.getCandidate(id))
    activeCandidate.value = detail
    reviewForm.value = mapDetailToDraft(detail.detail)
    savedReviewSignature.value = getDraftSignature(reviewForm.value)
    reviewComment.value = detail.reviewComment || ''
    if (shouldFocus) {
      await setStageFocus('review')
    }
  } catch (error) {
    showNotice('error', getErrorMessage(error))
  }
}

async function loadStructuredDetail(id: number, shouldFocus = true) {
  activeStructuredId.value = id
  activeStructured.value = null
  clearCandidateSelection()

  try {
    const detail = await withLoading('structuredDetail', () => recipePipelineApi.getStructuredRecord(id))
    activeStructured.value = detail

    const candidateSummary = candidates.value.find((item) => item.recipeKey === detail.recipeKey)
    if (candidateSummary) {
      await loadCandidateDetail(candidateSummary.detailContentId, shouldFocus)
      return
    }

    if (shouldFocus) {
      await setStageFocus('ai')
    }
  } catch (error) {
    showNotice('error', getErrorMessage(error))
  }
}

async function selectSource(id: number, shouldFocus = true) {
  activeSourceId.value = id
  activeSource.value = null
  clearStructuredSelection()

  try {
    const detail = await withLoading('sourceDetail', () => recipePipelineApi.getSourceRecord(id))
    activeSource.value = detail

    const structuredSummary = findStructuredSummaryBySource(id)
    if (structuredSummary) {
      await loadStructuredDetail(structuredSummary.id, shouldFocus)
      return
    }

    if (shouldFocus) {
      await setStageFocus('crawl')
    }
  } catch (error) {
    showNotice('error', getErrorMessage(error))
  }
}

async function openCandidateFlow(item: RecipeReviewPublishResponse) {
  const structuredSummary = findStructuredSummaryByRecipeKey(item.recipeKey)
  if (structuredSummary) {
    const sourceSummary = sourceRecords.value.find((record) => record.id === structuredSummary.sourceRecordId)
    if (sourceSummary) {
      await selectSource(sourceSummary.id, false)
      if (activeCandidateId.value !== item.detailContentId) {
        await loadCandidateDetail(item.detailContentId, false)
      }
      await setStageFocus('review')
      return
    }

    await loadStructuredDetail(structuredSummary.id, false)
    if (activeCandidateId.value !== item.detailContentId) {
      await loadCandidateDetail(item.detailContentId, false)
    }
    await setStageFocus('review')
    return
  }

  await loadCandidateDetail(item.detailContentId)
}

async function refreshChain(preferredSourceId?: number | null, shouldFocus = false) {
  const targetSourceId = preferredSourceId ?? activeSourceId.value

  await Promise.all([loadStructuredRecords(), loadCandidates(), loadSourceRecords(false)])

  if (targetSourceId && sourceRecords.value.some((item) => item.id === targetSourceId)) {
    await selectSource(targetSourceId, shouldFocus)
    return
  }

  const firstRecord = sourceRecords.value[0]
  if (firstRecord) {
    await selectSource(firstRecord.id, shouldFocus)
    return
  }

  await setStageFocus('crawl', false)
}

async function createCrawlTask() {
  if (!crawlForm.sourceSite?.trim()) {
    showNotice('error', '请先填写来源站点')
    return
  }

  if (!crawlForm.entryUrl?.trim()) {
    showNotice('error', '请先填写入口 URL')
    return
  }

  try {
    const payload: RecipeCrawlTaskRequest = {
      sourceSite: crawlForm.sourceSite.trim(),
      entryUrl: crawlForm.entryUrl.trim(),
      crawlMode: crawlForm.crawlMode?.trim() || undefined,
      maxPages: crawlForm.maxPages ?? 1,
      maxItems: crawlForm.maxItems ?? 20,
      fetchDetailPages: Boolean(crawlForm.fetchDetailPages),
    }

    const result = await withLoading('createTask', () => recipePipelineApi.createCrawlTask(payload))
    lastTask.value = result
    await refreshChain(result.records[0]?.id ?? null, true)
    await setStageFocus('crawl')
    showNotice('success', `抓取任务已完成，保存了 ${result.totalSaved} 条原始记录，请先完成人工抓取审核，再进入下一站。`)
  } catch (error) {
    showNotice('error', getErrorMessage(error))
  }
}

async function parseCurrentSource() {
  if (!activeSource.value) {
    showNotice('info', '请先从左侧任务池选择一条原始记录')
    return
  }

  if (activeSource.value.status === '已驳回') {
    showNotice('info', '当前原始记录已驳回，不能继续进入 AI 事实解析')
    return
  }

  try {
    const sourceId = activeSource.value.id
    await withLoading('parse', () => recipePipelineApi.parseSourceRecord(sourceId))
    await refreshChain(sourceId, false)
    await setStageFocus('ai')
    showNotice('success', '抓取审核已通过，AI 事实解析完成，已自动跳转到 AI 详情生成站。')
  } catch (error) {
    showNotice('error', getErrorMessage(error))
  }
}

async function rejectCurrentSource() {
  if (!activeSource.value) {
    showNotice('info', '请先选择一条原始记录')
    return
  }

  if (!window.confirm(`确认驳回原始记录「${activeSource.value.sourceUrl}」吗？驳回后不会进入下一步。`)) {
    return
  }

  try {
    const sourceId = activeSource.value.id
    await withLoading('sourceReject', () => recipePipelineApi.rejectSourceRecord(sourceId))
    await refreshChain(sourceId, false)
    await setStageFocus('crawl')
    showNotice('success', '原始抓取记录已驳回，当前链路不会进入 AI 事实解析。')
  } catch (error) {
    showNotice('error', getErrorMessage(error))
  }
}

async function generateAiDetail() {
  if (!activeStructured.value) {
    showNotice('info', '请先完成 AI 事实解析，再执行 AI 详情生成')
    return
  }

  try {
    const structuredId = activeStructured.value.id
    const result = await withLoading('ai', () => recipePipelineApi.generateAiDetail(structuredId))
    await refreshChain(activeSourceId.value, false)
    if (result.detailContentId) {
      await loadCandidateDetail(result.detailContentId, false)
    }
    activeAiResult.value = result
    await setStageFocus('review')
    showNotice(
      result.validation.passed ? 'success' : 'info',
      result.validation.passed ? 'AI 详情生成完成，已自动跳转到审核发布站。' : 'AI 已生成详情，已跳转到审核站继续修正校验问题。'
    )
  } catch (error) {
    showNotice('error', getErrorMessage(error))
  }
}

async function saveCandidate() {
  if (!activeCandidate.value) {
    showNotice('info', '请先生成 AI 候选详情')
    return
  }

  if (localValidationErrors.value.length) {
    showNotice('error', localValidationErrors.value[0] || '当前草稿未通过校验')
    return
  }

  try {
    const payload = buildCandidatePayload(reviewForm.value)
    const result = await withLoading('save', () =>
      recipePipelineApi.updateCandidate(activeCandidate.value!.detailContentId, payload)
    )
    activeAiResult.value = null
    activeCandidate.value = result
    reviewForm.value = mapDetailToDraft(result.detail)
    savedReviewSignature.value = getDraftSignature(reviewForm.value)
    await loadCandidates()
    await setStageFocus('review', false)
    showNotice('success', '候选详情已保存为最新草稿，继续在审核站处理即可。')
  } catch (error) {
    showNotice('error', getErrorMessage(error))
  }
}

async function publishCandidate() {
  if (!activeCandidate.value) {
    showNotice('info', '请先生成候选详情')
    return
  }

  if (hasDirtyReview.value) {
    showNotice('error', '当前有未保存修改，请先保存草稿后再发布')
    return
  }

  if (!canPublish.value) {
    showNotice('error', '校验未通过，暂不可发布')
    return
  }

  if (!window.confirm(`确认发布配方“${reviewForm.value.name || activeCandidate.value.detail.name}”吗？`)) {
    return
  }

  try {
    const result = await withLoading('publish', () =>
      recipePipelineApi.publishCandidate(activeCandidate.value!.detailContentId, {
        reviewComment: reviewComment.value.trim() || undefined,
      })
    )
    activeAiResult.value = null
    await refreshChain(activeSourceId.value, false)
    if (result.detailContentId) {
      await loadCandidateDetail(result.detailContentId, false)
    }
    await setStageFocus('review')
    showNotice(result.status === '已发布' ? 'success' : 'error', result.status === '已发布' ? '配方已发布，流水线已完成。' : '发布未通过，当前链路保留在审核站。')
  } catch (error) {
    showNotice('error', getErrorMessage(error))
  }
}

async function rejectCandidate() {
  if (!activeCandidate.value) {
    showNotice('info', '请先生成候选详情')
    return
  }

  if (!window.confirm(`确认驳回配方“${reviewForm.value.name || activeCandidate.value.detail.name}”吗？`)) {
    return
  }

  try {
    const result = await withLoading('reject', () =>
      recipePipelineApi.rejectCandidate(activeCandidate.value!.detailContentId, {
        reviewComment: reviewComment.value.trim() || undefined,
      })
    )
    activeAiResult.value = null
    await refreshChain(activeSourceId.value, false)
    if (result.detailContentId) {
      await loadCandidateDetail(result.detailContentId, false)
    }
    await setStageFocus('review')
    showNotice('success', `候选详情已驳回：${result.detail.name}`)
  } catch (error) {
    showNotice('error', getErrorMessage(error))
  }
}

async function runNextAction() {
  if (nextAction.value.action === 'parse') {
    await parseCurrentSource()
    return
  }

  if (nextAction.value.action === 'ai') {
    await generateAiDetail()
    return
  }

  if (nextAction.value.action === 'save') {
    await saveCandidate()
    return
  }

  if (nextAction.value.action === 'publish') {
    await publishCandidate()
  }
}

onMounted(async () => {
  await refreshChain(null, false)
  await syncStageFocus(false)
})
</script>

<template>
  <section class="pipeline-page">
    <header class="hero-shell">
      <div class="hero-copy">
        <p class="eyebrow">SHAKEPRO PIPELINE / 深海调酒台</p>
        <h1>配方采集工作台</h1>
        <p class="hero-subtitle">
          现在整条内容生产链路按流水线来跑：任务入池后先做抓取审核，再进入 AI 事实解析、AI 详情、审核发布四个站点，避免错误网页直接流入后续阶段。
        </p>
      </div>
      <div class="hero-stats">
        <article class="metric-card">
          <span>原始记录池</span>
          <strong>{{ sourceRecords.length }}</strong>
          <p>{{ sourceStatusFilter || '全部状态' }}</p>
        </article>
        <article class="metric-card">
          <span>结构化记录</span>
          <strong>{{ structuredRecords.length }}</strong>
          <p>解析站点</p>
        </article>
        <article class="metric-card accent">
          <span>候选详情</span>
          <strong>{{ candidates.length }}</strong>
          <p>待审核与已发布链路</p>
        </article>
      </div>
    </header>

    <div v-if="notice" class="notice-strip" :class="notice.tone">
      <strong>{{ notice.tone === 'error' ? '异常提示' : '流水线提示' }}</strong>
      <span>{{ notice.text }}</span>
    </div>

    <div class="workspace-grid">
      <aside class="queue-rail">
        <article class="dark-card launch-card">
          <div class="card-head">
            <div>
              <p class="section-kicker">任务发起</p>
              <h3>抓取任务区</h3>
            </div>
            <span class="pill">入口任务</span>
          </div>

          <div class="form-grid compact-grid">
            <label>
              <span>来源站点</span>
              <input v-model="crawlForm.sourceSite" class="field" type="text" placeholder="例如：Liquor.com" />
            </label>
            <label>
              <span>抓取模式</span>
              <select v-model="crawlForm.crawlMode" class="select">
                <option value="detail">detail</option>
                <option value="list">list</option>
              </select>
            </label>
            <label class="span-two">
              <span>入口 URL</span>
              <input v-model="crawlForm.entryUrl" class="field" type="text" placeholder="https://..." />
            </label>
            <label>
              <span>最大页数</span>
              <input v-model.number="crawlForm.maxPages" class="field" type="number" min="1" max="100" />
            </label>
            <label>
              <span>最大数量</span>
              <input v-model.number="crawlForm.maxItems" class="field" type="number" min="1" max="500" />
            </label>
          </div>

          <label class="check-row">
            <input v-model="crawlForm.fetchDetailPages" type="checkbox" />
            <span>抓取详情页内容</span>
          </label>

          <button class="button-primary action-button" :disabled="loading.createTask" @click="createCrawlTask">
            {{ loading.createTask ? '抓取中...' : '开始抓取并入池' }}
          </button>

          <div v-if="lastTask" class="task-footnote">
            <span>最近任务</span>
            <strong>{{ lastTask.sourceSite }}</strong>
            <p>保存 {{ lastTask.totalSaved }} 条 · {{ lastTask.entryUrl }}</p>
          </div>
        </article>

        <article class="dark-card queue-card">
          <div class="card-head">
            <div>
              <p class="section-kicker">任务池</p>
              <h3>原始记录队列</h3>
            </div>
            <span class="pill outline">{{ sourceRecords.length }} 条</span>
          </div>

          <div class="filter-row">
            <select v-model="sourceStatusFilter" class="select filter-select">
              <option v-for="item in statusOptions" :key="item || 'all'" :value="item">
                {{ item || '全部状态' }}
              </option>
            </select>
            <button class="button-secondary" :disabled="loading.sourceList" @click="loadSourceRecords(true)">
              {{ loading.sourceList ? '刷新中...' : '筛选' }}
            </button>
          </div>

          <div class="queue-list">
            <button
              v-for="record in sourceRecords"
              :key="record.id"
              class="queue-item"
              :class="{ active: activeSourceId === record.id }"
              @click="selectSource(record.id)"
            >
              <div class="queue-topline">
                <strong>{{ compactText(record.sourceUrl, `原始记录 #${record.id}`) }}</strong>
                <span class="status-chip" :class="statusClass(record.status)">{{ record.status }}</span>
              </div>
              <p>{{ record.sourceSite }} · {{ record.pageType || '未分类' }}</p>
              <div class="queue-meta">
                <span>#{{ record.id }}</span>
                <span>{{ formatDateTime(record.scrapedAt) }}</span>
              </div>
            </button>

            <div v-if="!sourceRecords.length" class="empty-state dark-empty">当前筛选下暂无原始记录。</div>
          </div>
        </article>
      </aside>

      <main class="pipeline-stage">
        <article class="dark-card overview-card">
          <div class="overview-head">
            <div>
              <p class="section-kicker">当前处理链路</p>
              <h2>{{ selectedTitle }}</h2>
              <p class="overview-subline">{{ selectedSubline }}</p>
            </div>
            <div class="next-action-panel">
              <span>推荐下一步</span>
              <strong>{{ nextAction.label }}</strong>
              <p>{{ nextAction.hint }}</p>
              <button class="button-primary" :disabled="nextAction.disabled" @click="runNextAction">
                {{ nextAction.label }}
              </button>
            </div>
          </div>

          <div class="stage-rail">
            <button
              v-for="node in stageNodes"
              :key="node.key"
              class="stage-node"
              :class="[stageStateClass(node.state), { focused: stageFocus === node.key }]"
              @click="setStageFocus(node.key)"
            >
              <span class="stage-index">{{ node.index }}</span>
              <div class="stage-content">
                <strong>{{ node.label }}</strong>
                <p>{{ node.hint }}</p>
              </div>
              <span class="stage-status">{{ node.status }}</span>
            </button>
          </div>
        </article>

        <div class="pipeline-stack">
          <article ref="crawlStageRef" class="dark-card station-card" :class="{ active: stageFocus === 'crawl' }">
            <div class="station-head">
              <div>
                <p class="section-kicker">站点 01</p>
                <h3>原始记录详情</h3>
              </div>
              <div class="station-actions">
                <span v-if="activeSource" class="status-chip" :class="statusClass(activeSource.status)">{{ activeSource.status }}</span>
                <button class="button-secondary mini" @click="setStageFocus('crawl')">聚焦此站</button>
              </div>
            </div>

            <div v-if="loading.sourceDetail" class="empty-state dark-empty">原始记录详情加载中...</div>
            <div v-else-if="!activeSource" class="empty-state dark-empty">从左侧任务池选择一条原始记录后，这里显示采集详情。</div>
            <template v-else>
              <div class="station-summary-grid">
                <div class="summary-box span-two">
                  <span>抓取来源</span>
                  <strong class="break-all">{{ compactText(activeSource.sourceUrl, '暂无来源地址') }}</strong>
                </div>
                <div class="summary-box">
                  <span>来源站点</span>
                  <strong>{{ compactText(activeSource.sourceSite, '未标记站点') }}</strong>
                </div>
                <div class="summary-box">
                  <span>页面类型</span>
                  <strong>{{ compactText(activeSource.pageType, '-') }}</strong>
                </div>
              </div>

              <div v-if="stageFocus === 'crawl'" class="station-body raw-body">
                <div class="stack-panels">
                  <article class="info-panel">
                    <span>审核说明</span>
                    <pre>请先检查原始纯文本和 HTML 是否抓到正确页面，再决定是否放行进入 AI 事实解析。</pre>
                  </article>
                </div>
              </div>
              <div v-else class="station-collapsed-note">原始抓取审核已结束，可继续推进到 AI 事实解析站。</div>

              <details v-if="stageFocus === 'crawl'" class="collapse-panel">
                <summary>查看原始 HTML</summary>
                <pre>{{ compactText(activeSource.rawHtml, '暂无原始 HTML') }}</pre>
              </details>
              <details v-if="stageFocus === 'crawl'" class="collapse-panel">
                <summary>查看原始纯文本</summary>
                <pre>{{ compactText(activeSource.rawText, '暂无原始纯文本') }}</pre>
              </details>

              <div class="station-footer">
                <button class="button-secondary" :disabled="loading.sourceReject || activeSource.status === '已驳回'" @click="rejectCurrentSource">
                  {{ loading.sourceReject ? '驳回中...' : activeSource.status === '已驳回' ? '当前记录已驳回' : '驳回此条抓取' }}
                </button>
                <button class="button-primary" :disabled="loading.parse || activeSource.status === '已驳回'" @click="parseCurrentSource">
                  {{ loading.parse ? 'AI 解析中...' : '审核通过并进入 AI 事实解析' }}
                </button>
              </div>
            </template>
          </article>

          <article ref="parseStageRef" class="dark-card station-card" :class="{ active: stageFocus === 'parse' }">
            <div class="station-head">
              <div>
                <p class="section-kicker">站点 02</p>
                <h3>AI 事实解析</h3>
              </div>
              <div class="station-actions">
                <span v-if="activeStructured" class="status-chip" :class="statusClass(activeStructured.status)">{{ activeStructured.status }}</span>
                <button class="button-secondary mini" @click="setStageFocus('parse')">聚焦此站</button>
              </div>
            </div>

            <div v-if="!activeSource" class="empty-state dark-empty">先完成上一站：从任务池选中一条原始记录。</div>
            <div v-else-if="loading.structuredDetail" class="empty-state dark-empty">结构化结果加载中...</div>
            <template v-else>
              <div v-if="activeStructured" class="station-summary-grid">
                <div class="summary-box">
                  <span>英文名</span>
                  <strong>{{ compactText(activeStructured.englishName, '-') }}</strong>
                </div>
                <div class="summary-box">
                  <span>中文草稿名</span>
                  <strong>{{ compactText(activeStructured.chineseNameDraft, '-') }}</strong>
                </div>
                <div class="summary-box">
                  <span>估算酒精度</span>
                  <strong>{{ compactText(activeStructured.estimatedAbv, '-') }}</strong>
                </div>
                <div class="summary-box">
                  <span>估算容量</span>
                  <strong>{{ compactText(activeStructured.estimatedVolume, '-') }}</strong>
                </div>
              </div>

              <div v-if="stageFocus === 'parse' && activeStructured" class="station-body">
                <div class="dual-panels">
                  <article class="info-panel small-panel">
                    <span>配料数组</span>
                    <ul class="content-list">
                      <li v-for="(item, index) in activeStructured.ingredients" :key="`${item.name}-${index}`">
                        <strong>{{ item.name }}</strong>
                        <p>{{ item.amount }}<template v-if="item.note"> · {{ item.note }}</template></p>
                      </li>
                      <li v-if="!activeStructured.ingredients.length">暂无结构化配料</li>
                    </ul>
                  </article>
                  <article class="info-panel small-panel">
                    <span>步骤数组</span>
                    <ul class="content-list">
                      <li v-for="(item, index) in activeStructured.steps" :key="`${item.title}-${index}`">
                        <strong>{{ item.orderNo || index + 1 }}. {{ item.title }}</strong>
                        <p>{{ item.detail }}<template v-if="item.hint"> · {{ item.hint }}</template></p>
                      </li>
                      <li v-if="!activeStructured.steps.length">暂无结构化步骤</li>
                    </ul>
                  </article>
                </div>
                <div class="note-strip">
                  <span>解析备注</span>
                  <p>{{ compactText(activeStructured.parseNotes, '暂无解析备注') }}</p>
                </div>
              </div>
              <div v-else-if="activeStructured" class="station-collapsed-note">AI 事实解析结果已就绪，可继续进入 AI 详情生成站。</div>
              <div v-else class="empty-state dark-empty">当前链路还没有结构化结果，点击下方按钮开始执行 AI 事实解析。</div>

              <div class="station-footer">
                <button class="button-secondary" :disabled="loading.parse" @click="parseCurrentSource">
                  {{ loading.parse ? '解析中...' : activeStructured ? '重新执行 AI 事实解析' : '执行 AI 事实解析' }}
                </button>
                <button class="button-primary" :disabled="!activeStructured || loading.ai" @click="generateAiDetail">
                  {{ loading.ai ? '生成中...' : '完成此站并进入 AI 详情生成' }}
                </button>
              </div>
            </template>
          </article>

          <article ref="aiStageRef" class="dark-card station-card" :class="{ active: stageFocus === 'ai' }">
            <div class="station-head">
              <div>
                <p class="section-kicker">站点 03</p>
                <h3>AI 详情生成</h3>
              </div>
              <div class="station-actions">
                <span v-if="aiPreview" class="status-chip" :class="statusClass(aiPreview.status)">{{ aiPreview.status }}</span>
                <button class="button-secondary mini" @click="setStageFocus('ai')">聚焦此站</button>
              </div>
            </div>

            <div v-if="!activeStructured" class="empty-state dark-empty">先完成上一站：通过 AI 事实解析生成结构化结果。</div>
            <template v-else>
              <div v-if="aiPreview" class="station-summary-grid">
                <div class="summary-box span-two">
                  <span>候选名</span>
                  <strong>{{ compactText(aiPreview.detail.name, '等待生成候选名') }}</strong>
                </div>
                <div class="summary-box span-two">
                  <span>副标题</span>
                  <strong>{{ compactText(aiPreview.detail.subtitle, '等待生成副标题') }}</strong>
                </div>
              </div>

              <div v-if="stageFocus === 'ai' && aiPreview" class="station-body ai-body">
                <div class="ai-preview-box">
                  <h4>{{ compactText(aiPreview.detail.name, '等待生成候选名') }}</h4>
                  <p>{{ compactText(aiPreview.detail.description, '等待生成详情简介') }}</p>
                  <dl class="fact-grid">
                    <div>
                      <dt>适合场景</dt>
                      <dd>{{ compactText(aiPreview.detail.bestFor, '-') }}</dd>
                    </div>
                    <div>
                      <dt>难度</dt>
                      <dd>{{ compactText(aiPreview.detail.difficulty, '-') }}</dd>
                    </div>
                    <div>
                      <dt>时长</dt>
                      <dd>{{ compactText(aiPreview.detail.duration, '-') }}</dd>
                    </div>
                    <div>
                      <dt>风味标签</dt>
                      <dd>{{ aiPreview.detail.flavorTags.join(' / ') || '-' }}</dd>
                    </div>
                  </dl>
                </div>
              </div>
              <div v-else-if="aiPreview" class="station-collapsed-note">AI 详情已就绪，可继续进入审核发布站。</div>
              <div v-else class="empty-state dark-empty">当前链路还没有 AI 详情，点击下方按钮开始生成。</div>

              <div class="validation-card" :class="{ invalid: validationView && !validationView.passed }" v-if="validationView">
                <div class="validation-topline">
                  <strong>{{ validationView.passed ? '校验通过' : '校验未通过' }}</strong>
                  <span>{{ validationView.label }}</span>
                </div>
                <ul v-if="validationView.errors.length" class="error-list">
                  <li v-for="error in validationView.errors" :key="error">{{ error }}</li>
                </ul>
                <p v-else>当前没有校验错误，可以进入审核发布站继续处理。</p>
              </div>

              <div class="station-footer">
                <button class="button-primary" :disabled="loading.ai" @click="generateAiDetail">
                  {{ loading.ai ? '生成中...' : aiPreview ? '重新生成 AI 详情' : '执行 AI 详情生成' }}
                </button>
                <button class="button-secondary" :disabled="!aiPreview" @click="setStageFocus('review')">跳到审核发布站</button>
              </div>
            </template>
          </article>

          <article ref="reviewStageRef" class="dark-card station-card" :class="{ active: stageFocus === 'review' }">
            <div class="station-head">
              <div>
                <p class="section-kicker">站点 04</p>
                <h3>审核发布</h3>
              </div>
              <div class="station-actions wide-actions">
                <button class="button-secondary mini" :disabled="loading.candidateList" @click="loadCandidates()">
                  {{ loading.candidateList ? '刷新中...' : '刷新候选池' }}
                </button>
                <button class="button-secondary mini" @click="setStageFocus('review')">聚焦此站</button>
              </div>
            </div>

            <div class="candidate-rack">
              <button
                v-for="item in recentCandidateSummaries"
                :key="item.detailContentId"
                class="candidate-chip"
                :class="{ active: activeCandidateId === item.detailContentId }"
                @click="openCandidateFlow(item)"
              >
                <strong>{{ compactText(item.detail.name, item.recipeKey) }}</strong>
                <span>{{ item.status }}</span>
              </button>
              <div v-if="!recentCandidateSummaries.length" class="empty-state dark-empty small-empty">暂无候选详情。</div>
            </div>

            <div v-if="loading.candidateDetail" class="empty-state dark-empty">候选详情加载中...</div>
            <div v-else-if="!activeCandidate" class="empty-state dark-empty">先完成上一站：生成 AI 候选详情后才会进入人工审核。</div>
            <template v-else>
              <div class="review-banner">
                <div>
                  <strong>{{ compactText(activeCandidate.detail.name, activeCandidate.recipeKey) }}</strong>
                  <p>#{{ activeCandidate.detailContentId }} · {{ activeCandidate.recipeKey }} · {{ activeCandidate.action }}</p>
                </div>
                <span class="status-chip" :class="statusClass(activeCandidate.status)">{{ activeCandidate.status }}</span>
              </div>

              <div class="review-alert" v-if="hasDirtyReview">当前有未保存修改，发布动作会自动锁住。</div>

              <div v-if="stageFocus === 'review'" class="review-sections">
                <section class="editor-block">
                  <div class="editor-head">
                    <h4>基础信息</h4>
                    <span>核心展示字段</span>
                  </div>
                  <div class="form-grid review-grid">
                    <label>
                      <span>recipeKey</span>
                      <input v-model="reviewForm.id" class="field" type="text" />
                    </label>
                    <label>
                      <span>中文名</span>
                      <input v-model="reviewForm.name" class="field" type="text" />
                    </label>
                    <label>
                      <span>英文名</span>
                      <input v-model="reviewForm.englishName" class="field" type="text" />
                    </label>
                    <label>
                      <span>分类</span>
                      <input v-model="reviewForm.category" class="field" type="text" />
                    </label>
                    <label class="span-two">
                      <span>主图</span>
                      <input v-model="reviewForm.heroImage" class="field" type="text" placeholder="https://..." />
                    </label>
                    <label class="span-two">
                      <span>highlight</span>
                      <input v-model="reviewForm.highlight" class="field" type="text" />
                    </label>
                    <label class="span-two">
                      <span>subtitle</span>
                      <input v-model="reviewForm.subtitle" class="field" type="text" />
                    </label>
                    <label class="span-two">
                      <span>description</span>
                      <textarea v-model="reviewForm.description" class="textarea" rows="3"></textarea>
                    </label>
                    <label class="span-two">
                      <span>story</span>
                      <textarea v-model="reviewForm.story" class="textarea" rows="3"></textarea>
                    </label>
                    <label>
                      <span>bestFor</span>
                      <input v-model="reviewForm.bestFor" class="field" type="text" />
                    </label>
                    <label>
                      <span>difficulty</span>
                      <input v-model="reviewForm.difficulty" class="field" type="text" />
                    </label>
                    <label>
                      <span>duration</span>
                      <input v-model="reviewForm.duration" class="field" type="text" />
                    </label>
                    <label>
                      <span>abv</span>
                      <input v-model="reviewForm.abv" class="field" type="text" />
                    </label>
                    <label>
                      <span>volume</span>
                      <input v-model="reviewForm.volume" class="field" type="text" />
                    </label>
                    <label>
                      <span>glass</span>
                      <input v-model="reviewForm.glass" class="field" type="text" />
                    </label>
                    <label>
                      <span>garnish</span>
                      <input v-model="reviewForm.garnish" class="field" type="text" />
                    </label>
                    <label>
                      <span>serveTemperature</span>
                      <input v-model="reviewForm.serveTemperature" class="field" type="text" />
                    </label>
                  </div>
                </section>

                <section class="editor-block">
                  <div class="editor-head">
                    <h4>标签与建议</h4>
                    <span>风味、搭配与服务提示</span>
                  </div>
                  <div class="stack-grid">
                    <div class="mini-editor">
                      <div class="mini-head">
                        <strong>flavorTags</strong>
                        <button class="button-secondary mini" type="button" @click="reviewForm.flavorTags.push('')">添加标签</button>
                      </div>
                      <div v-for="(item, index) in reviewForm.flavorTags" :key="`flavor-${index}-${item}`" class="inline-row">
                        <input v-model="reviewForm.flavorTags[index]" class="field" type="text" placeholder="例如：经典" />
                        <button class="button-danger mini" type="button" @click="removeStringItem(reviewForm.flavorTags, index)">删除</button>
                      </div>
                    </div>

                    <div class="mini-editor">
                      <div class="mini-head">
                        <strong>pairings</strong>
                        <button class="button-secondary mini" type="button" @click="reviewForm.pairings.push('')">添加搭配</button>
                      </div>
                      <div v-for="(item, index) in reviewForm.pairings" :key="`pairing-${index}-${item}`" class="inline-row">
                        <input v-model="reviewForm.pairings[index]" class="field" type="text" placeholder="例如：咸味坚果" />
                        <button class="button-danger mini" type="button" @click="removeStringItem(reviewForm.pairings, index)">删除</button>
                      </div>
                    </div>

                    <div class="mini-editor">
                      <div class="mini-head">
                        <strong>serviceNotes</strong>
                        <button class="button-secondary mini" type="button" @click="reviewForm.serviceNotes.push('')">添加提示</button>
                      </div>
                      <div v-for="(item, index) in reviewForm.serviceNotes" :key="`service-${index}-${item}`" class="inline-row">
                        <input v-model="reviewForm.serviceNotes[index]" class="field" type="text" placeholder="例如：建议提前冷杯" />
                        <button class="button-danger mini" type="button" @click="removeStringItem(reviewForm.serviceNotes, index)">删除</button>
                      </div>
                    </div>

                    <div class="mini-editor">
                      <div class="mini-head">
                        <strong>flavorMetrics</strong>
                        <button class="button-secondary mini" type="button" @click="reviewForm.flavorMetrics.push({ label: '', value: 3 })">添加维度</button>
                      </div>
                      <div v-for="(item, index) in reviewForm.flavorMetrics" :key="`metric-${index}`" class="metric-row">
                        <input v-model="item.label" class="field" type="text" placeholder="例如：甜感" />
                        <input v-model.number="item.value" class="field" type="number" min="1" max="5" />
                        <button class="button-danger mini" type="button" @click="removeFlavorMetric(index)">删除</button>
                      </div>
                    </div>
                  </div>
                </section>

                <section class="editor-block">
                  <div class="editor-head">
                    <h4>配料与步骤</h4>
                    <span>发布前最终确认生产内容</span>
                  </div>
                  <div class="mini-editor">
                    <div class="mini-head">
                      <strong>ingredients</strong>
                      <button class="button-secondary mini" type="button" @click="reviewForm.ingredients.push(createEmptyIngredient())">添加配料</button>
                    </div>
                    <div v-for="(item, index) in reviewForm.ingredients" :key="`ingredient-${index}`" class="ingredient-row">
                      <input v-model="item.name" class="field" type="text" placeholder="材料名" />
                      <input v-model="item.amount" class="field" type="text" placeholder="用量" />
                      <div class="row-with-action">
                        <input v-model="item.note" class="field" type="text" placeholder="说明（可选）" />
                        <button class="button-danger mini" type="button" @click="removeIngredient(index)">删除</button>
                      </div>
                    </div>
                  </div>

                  <div class="mini-editor">
                    <div class="mini-head">
                      <strong>steps</strong>
                      <button class="button-secondary mini" type="button" @click="reviewForm.steps.push(createEmptyStep())">添加步骤</button>
                    </div>
                    <div v-for="(item, index) in reviewForm.steps" :key="`step-${index}`" class="step-row">
                      <div class="step-header">
                        <input v-model="item.title" class="field" type="text" :placeholder="`步骤 ${index + 1} 标题`" />
                        <button class="button-danger mini" type="button" @click="removeStep(index)">删除</button>
                      </div>
                      <textarea v-model="item.detail" class="textarea" rows="3" placeholder="步骤说明"></textarea>
                      <input v-model="item.hint" class="field" type="text" placeholder="步骤提示（可选）" />
                    </div>
                  </div>
                </section>

                <section class="editor-block action-block">
                  <div class="editor-head">
                    <h4>审核动作</h4>
                    <span>保存、发布或驳回</span>
                  </div>

                  <label>
                    <span>审核备注</span>
                    <textarea v-model="reviewComment" class="textarea" rows="2" placeholder="发布或驳回时可附加备注"></textarea>
                  </label>

                  <div class="action-row">
                    <button class="button-secondary" :disabled="loading.save" @click="saveCandidate">
                      {{ loading.save ? '保存中...' : '保存草稿' }}
                    </button>
                    <button class="button-danger" :disabled="loading.reject" @click="rejectCandidate">
                      {{ loading.reject ? '驳回中...' : '驳回' }}
                    </button>
                    <button class="button-primary" :disabled="loading.publish || !canPublish" @click="publishCandidate">
                      {{ loading.publish ? '发布中...' : '发布' }}
                    </button>
                  </div>

                  <p class="action-tip" v-if="hasDirtyReview">当前有未保存修改，发布按钮保持禁用。</p>
                  <p class="action-tip danger" v-else-if="activeCandidate && !activeCandidate.validation.passed">
                    候选详情校验未通过，请先修正错误字段。
                  </p>
                </section>
              </div>
              <div v-else class="station-collapsed-note">当前链路已进入审核站，点击“聚焦此站”可展开完整审核表单。</div>
            </template>
          </article>
        </div>
      </main>
    </div>
  </section>
</template>

<style scoped>
.pipeline-page {
  --page-bg: radial-gradient(circle at top left, rgba(32, 211, 255, 0.18), transparent 28%),
    radial-gradient(circle at top right, rgba(39, 102, 255, 0.16), transparent 36%),
    linear-gradient(180deg, #07111c 0%, #091827 44%, #0d2133 100%);
  --panel-bg: linear-gradient(180deg, rgba(10, 27, 43, 0.92), rgba(7, 18, 31, 0.96));
  --line-soft: rgba(153, 199, 255, 0.12);
  --text-strong: #eff7ff;
  --text-main: #c7d8ea;
  --text-soft: #88a4c2;
  --cyan: #48d7ff;
  --cyan-strong: #14b8ff;
  --blue: #4c6fff;
  --mineral: #10263a;
  min-height: 100%;
  display: grid;
  gap: 18px;
  padding: 18px;
  border-radius: 30px;
  background: var(--page-bg);
  color: var(--text-main);
}

.pipeline-page h1,
.pipeline-page h2,
.pipeline-page h3,
.pipeline-page h4,
.pipeline-page strong {
  color: var(--text-strong);
}

.pipeline-page .field,
.pipeline-page .textarea,
.pipeline-page .select {
  border: 1px solid rgba(153, 199, 255, 0.14);
  background: rgba(8, 22, 36, 0.78);
  color: var(--text-strong);
  border-radius: 16px;
}

.pipeline-page .field::placeholder,
.pipeline-page .textarea::placeholder {
  color: rgba(136, 164, 194, 0.72);
}

.pipeline-page .field:focus,
.pipeline-page .textarea:focus,
.pipeline-page .select:focus {
  outline: 2px solid rgba(72, 215, 255, 0.16);
  border-color: rgba(72, 215, 255, 0.42);
}

.pipeline-page .button-primary,
.pipeline-page .button-secondary,
.pipeline-page .button-danger {
  border-radius: 14px;
  box-shadow: none;
}

.pipeline-page .button-primary {
  background: linear-gradient(135deg, var(--cyan-strong), var(--blue));
  color: #04131f;
}

.pipeline-page .button-secondary {
  background: rgba(11, 29, 46, 0.82);
  color: var(--text-strong);
  border: 1px solid rgba(153, 199, 255, 0.16);
}

.pipeline-page .button-danger {
  background: linear-gradient(135deg, #ff6177, #ff946c);
}

.hero-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 460px);
  gap: 18px;
  align-items: stretch;
}

.hero-copy,
.hero-stats,
.dark-card,
.notice-strip {
  border: 1px solid var(--line-soft);
  box-shadow: 0 24px 70px rgba(3, 10, 18, 0.38);
}

.hero-copy {
  padding: 24px 26px;
  border-radius: 26px;
  background: linear-gradient(135deg, rgba(7, 20, 32, 0.9), rgba(14, 37, 59, 0.88));
}

.eyebrow,
.section-kicker {
  margin: 0;
  color: var(--cyan);
  font-size: 0.78rem;
  letter-spacing: 0.22em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 12px 0 10px;
  font-size: 2.4rem;
  line-height: 0.98;
  letter-spacing: -0.05em;
}

.hero-subtitle {
  margin: 0;
  max-width: 760px;
  color: var(--text-main);
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 14px;
  border-radius: 26px;
  background: linear-gradient(180deg, rgba(7, 20, 32, 0.92), rgba(10, 27, 43, 0.88));
}

.metric-card {
  padding: 16px;
  border-radius: 20px;
  background: rgba(11, 29, 46, 0.78);
  border: 1px solid rgba(153, 199, 255, 0.08);
}

.metric-card.accent {
  background: linear-gradient(180deg, rgba(14, 34, 56, 0.92), rgba(8, 22, 36, 0.92));
}

.metric-card span,
.metric-card p {
  color: var(--text-soft);
}

.metric-card strong {
  display: block;
  margin: 10px 0 4px;
  font-size: 2rem;
}

.metric-card p {
  margin: 0;
}

.notice-strip {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 14px 18px;
  border-radius: 18px;
  background: rgba(10, 27, 43, 0.86);
}

.notice-strip.error {
  border-color: rgba(255, 107, 125, 0.3);
}

.workspace-grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.queue-rail,
.pipeline-stage,
.pipeline-stack,
.review-sections,
.stack-grid,
.stack-panels {
  display: grid;
  gap: 18px;
}

.dark-card {
  padding: 20px;
  border-radius: 24px;
  background: var(--panel-bg);
  backdrop-filter: blur(18px);
}

.card-head,
.station-head,
.station-actions,
.overview-head,
.editor-head,
.mini-head,
.queue-topline,
.queue-meta,
.validation-topline,
.review-banner,
.step-header,
.row-with-action,
.metric-row,
.action-row,
.station-footer,
.station-summary-grid,
.station-actions.wide-actions,
.station-head .button-secondary,
.next-action-panel {
  gap: 12px;
}

.card-head,
.station-head,
.overview-head,
.editor-head,
.mini-head,
.queue-topline,
.queue-meta,
.validation-topline,
.review-banner,
.step-header,
.row-with-action,
.action-row,
.station-footer {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.station-actions,
.station-actions.wide-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.card-head h3,
.overview-head h2,
.editor-head h4,
.station-head h3 {
  margin: 6px 0 0;
}

.pill,
.status-chip,
.stage-status {
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
  border-radius: 999px;
}

.pill {
  padding: 8px 12px;
  background: rgba(72, 215, 255, 0.12);
  color: var(--cyan);
}

.pill.outline {
  background: transparent;
  border: 1px solid rgba(72, 215, 255, 0.18);
}

.action-button {
  width: 100%;
  margin-top: 14px;
}

.check-row {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-top: 6px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.form-grid label,
.editor-block label,
.mini-editor {
  display: grid;
  gap: 8px;
}

.compact-grid {
  margin-top: 12px;
}

.span-two {
  grid-column: span 2;
}

.task-footnote {
  margin-top: 14px;
  padding: 14px;
  border-radius: 18px;
  background: rgba(7, 18, 31, 0.72);
  border: 1px solid rgba(153, 199, 255, 0.08);
}

.task-footnote span,
.task-footnote p,
.overview-subline,
.empty-state,
.queue-item p,
.queue-meta,
.note-strip p,
.content-list p,
.review-banner p,
.action-tip,
.fact-grid dt,
.summary-box span,
.info-panel span,
.collapse-panel summary,
.stage-node p,
.stage-status,
.next-action-panel span,
.next-action-panel p,
.editor-head span,
.mini-head span,
.station-collapsed-note {
  color: var(--text-soft);
}

.filter-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  margin: 14px 0 12px;
}

.queue-list {
  display: grid;
  gap: 10px;
  max-height: calc(100vh - 360px);
  overflow: auto;
  padding-right: 4px;
}

.queue-item,
.candidate-chip,
.stage-node {
  width: 100%;
  text-align: left;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(153, 199, 255, 0.1);
  background: rgba(9, 24, 39, 0.78);
}

.queue-item.active,
.candidate-chip.active,
.stage-node.focused {
  border-color: rgba(72, 215, 255, 0.42);
  background: linear-gradient(135deg, rgba(17, 39, 60, 0.96), rgba(10, 27, 43, 0.92));
  box-shadow: inset 0 0 0 1px rgba(72, 215, 255, 0.12);
}

.queue-item strong,
.candidate-chip strong {
  display: block;
}

.queue-item p {
  margin: 8px 0;
}

.status-chip {
  padding: 6px 12px;
  background: rgba(153, 199, 255, 0.1);
  color: var(--text-main);
  font-size: 0.84rem;
}

.status-chip.scraped {
  background: rgba(39, 102, 255, 0.16);
  color: #a5bdff;
}

.status-chip.parsed {
  background: rgba(255, 182, 72, 0.16);
  color: #ffd48d;
}

.status-chip.generated,
.status-chip.pending {
  background: rgba(72, 215, 255, 0.14);
  color: var(--cyan);
}

.status-chip.published {
  background: rgba(45, 212, 191, 0.16);
  color: #8ff8ea;
}

.status-chip.rejected {
  background: rgba(255, 107, 125, 0.14);
  color: #ffafbc;
}

.overview-card {
  padding: 22px;
}

.next-action-panel {
  min-width: 320px;
  display: grid;
  padding: 16px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(10, 29, 47, 0.92), rgba(9, 21, 33, 0.92));
  border: 1px solid rgba(72, 215, 255, 0.18);
}

.next-action-panel strong {
  display: block;
  margin-top: 8px;
  font-size: 1.1rem;
}

.next-action-panel p {
  margin: 8px 0 14px;
}

.stage-rail {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 20px;
}

.stage-node {
  display: grid;
  gap: 10px;
  cursor: pointer;
}

.stage-node.done {
  border-color: rgba(45, 212, 191, 0.26);
}

.stage-node.current {
  border-color: rgba(72, 215, 255, 0.34);
}

.stage-index {
  display: inline-flex;
  width: 38px;
  height: 38px;
  border-radius: 14px;
  align-items: center;
  justify-content: center;
  background: rgba(72, 215, 255, 0.12);
  color: var(--cyan);
  font-weight: 700;
}

.stage-status {
  padding: 6px 10px;
  background: rgba(153, 199, 255, 0.08);
  width: fit-content;
}

.station-card {
  scroll-margin-top: 16px;
  border: 1px solid rgba(153, 199, 255, 0.1);
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.station-card.active {
  border-color: rgba(72, 215, 255, 0.28);
}

.station-summary-grid,
.fact-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.summary-box,
.info-panel,
.note-strip,
.ai-preview-box,
.validation-card,
.editor-block,
.review-banner,
.station-collapsed-note {
  border-radius: 18px;
  border: 1px solid rgba(153, 199, 255, 0.08);
  background: rgba(8, 22, 36, 0.68);
}

.summary-box,
.station-collapsed-note {
  padding: 14px;
}

.summary-box strong {
  display: block;
  margin-top: 6px;
}

.break-all {
  word-break: break-all;
}

.station-body,
.raw-body,
.review-sections,
.dual-panels {
  display: grid;
  gap: 14px;
  margin-top: 14px;
}

.raw-body {
  grid-template-columns: 240px minmax(0, 1fr);
}

.raw-preview,
.raw-preview img {
  width: 100%;
  border-radius: 20px;
}

.raw-preview {
  overflow: hidden;
  min-height: 260px;
  border: 1px solid rgba(153, 199, 255, 0.08);
  background: rgba(8, 22, 36, 0.68);
}

.raw-preview img {
  height: 100%;
  object-fit: cover;
  display: block;
}

.info-panel {
  padding: 14px;
}

.info-panel pre,
.collapse-panel pre {
  margin: 10px 0 0;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--text-main);
}

.small-panel {
  min-height: 220px;
}

.collapse-panel {
  margin-top: 12px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(153, 199, 255, 0.08);
  background: rgba(8, 22, 36, 0.58);
}

.collapse-panel summary {
  cursor: pointer;
  font-weight: 700;
}

.content-list {
  margin: 10px 0 0;
  padding-left: 18px;
}

.content-list li + li {
  margin-top: 10px;
}

.content-list p {
  margin: 6px 0 0;
}

.note-strip {
  padding: 14px;
}

.note-strip p {
  margin: 6px 0 0;
}

.ai-preview-box {
  padding: 16px;
}

.ai-preview-box h4 {
  margin: 0 0 8px;
  font-size: 1.18rem;
}

.ai-preview-box p {
  margin: 0;
  color: var(--text-main);
}

.fact-grid {
  margin-top: 14px;
}

.fact-grid div {
  padding-top: 10px;
  border-top: 1px solid rgba(153, 199, 255, 0.08);
}

.fact-grid dt,
.fact-grid dd {
  margin: 0;
}

.fact-grid dd {
  margin-top: 6px;
  color: var(--text-strong);
}

.validation-card {
  padding: 14px;
  margin-top: 14px;
}

.validation-card.invalid {
  border-color: rgba(255, 107, 125, 0.24);
}

.validation-card p {
  margin: 10px 0 0;
}

.error-list {
  margin: 10px 0 0;
  padding-left: 18px;
  color: #ffd3da;
}

.candidate-rack {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 14px 0;
}

.candidate-chip span {
  display: block;
  margin-top: 6px;
  color: var(--text-soft);
  font-size: 0.86rem;
}

.review-banner {
  padding: 14px 16px;
}

.review-banner p {
  margin: 6px 0 0;
}

.review-alert {
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(255, 182, 72, 0.12);
  color: #ffd48d;
}

.editor-block {
  padding: 16px;
}

.editor-head h4 {
  margin: 0;
}

.editor-head span {
  font-size: 0.9rem;
}

.review-grid {
  margin-top: 14px;
}

.inline-row,
.metric-row,
.ingredient-row,
.row-with-action {
  display: grid;
  gap: 10px;
}

.inline-row {
  grid-template-columns: 1fr auto;
}

.metric-row {
  grid-template-columns: 1.2fr 110px auto;
}

.ingredient-row {
  grid-template-columns: 1fr 140px 1.2fr;
}

.row-with-action {
  grid-template-columns: 1fr auto;
}

.step-row {
  padding: 14px;
  border-radius: 18px;
  background: rgba(7, 18, 31, 0.54);
  border: 1px solid rgba(153, 199, 255, 0.08);
}

.step-row .textarea,
.step-row .field {
  margin-top: 10px;
}

.action-row {
  margin-top: 14px;
  flex-wrap: wrap;
}

.action-tip {
  margin: 12px 0 0;
}

.action-tip.danger {
  color: #ffafbc;
}

.station-footer {
  margin-top: 14px;
  flex-wrap: wrap;
}

.station-collapsed-note {
  margin-top: 14px;
}

.empty-state {
  padding: 18px;
  border-radius: 18px;
  border: 1px dashed rgba(153, 199, 255, 0.14);
}

.dark-empty {
  background: rgba(7, 18, 31, 0.48);
}

.small-empty {
  min-height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.mini {
  padding-inline: 14px;
}

@media (max-width: 1520px) {
  .hero-shell,
  .workspace-grid,
  .raw-body,
  .stage-rail,
  .candidate-rack {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1100px) {
  .form-grid,
  .station-summary-grid,
  .fact-grid,
  .ingredient-row,
  .metric-row,
  .inline-row,
  .row-with-action,
  .dual-panels {
    grid-template-columns: 1fr;
  }

  .span-two {
    grid-column: auto;
  }
}

@media (max-width: 720px) {
  .pipeline-page {
    padding: 12px;
    border-radius: 22px;
  }

  .hero-copy h1 {
    font-size: 1.9rem;
  }

  .card-head,
  .station-head,
  .overview-head,
  .editor-head,
  .queue-topline,
  .queue-meta,
  .validation-topline,
  .review-banner,
  .step-header,
  .action-row,
  .station-footer,
  .station-actions,
  .station-actions.wide-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .next-action-panel {
    min-width: 0;
  }
}
</style>
