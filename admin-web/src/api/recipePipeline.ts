import http from './http'

export const RECIPE_PIPELINE_STATUSES = ['已抓取', '已解析', 'AI已生成', '待审核', '已发布', '已驳回'] as const

export type RecipePipelineStatus = (typeof RECIPE_PIPELINE_STATUSES)[number] | string

export interface RecipeCrawlTaskRequest {
  sourceSite: string
  entryUrl: string
  crawlMode?: string
  maxPages?: number
  maxItems?: number
  fetchDetailPages?: boolean
}

export interface RecipeSourceRecord {
  id: number
  sourceSite: string
  sourceUrl: string
  pageType: string
  rawHtml?: string | null
  rawText?: string | null
  status: RecipePipelineStatus
  scrapedAt?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface RecipeStructuredIngredientItem {
  name: string
  amount: string
  note?: string | null
  category?: string | null
}

export interface RecipeStructuredStepItem {
  orderNo?: number | null
  title: string
  detail: string
  hint?: string | null
}

export interface RecipeStructuredRecord {
  id: number
  sourceRecordId: number
  recipeKey: string
  englishName: string
  chineseNameDraft?: string | null
  category?: string | null
  heroImage?: string | null
  garnish?: string | null
  glassDraft?: string | null
  methodText?: string | null
  estimatedAbv?: string | null
  estimatedVolume?: string | null
  ingredients: RecipeStructuredIngredientItem[]
  steps: RecipeStructuredStepItem[]
  parseNotes?: string | null
  status: RecipePipelineStatus
  parsedAt?: string | null
  sourceSite?: string | null
  sourceUrl?: string | null
}

export interface RecipeDetailValidationResult {
  passed: boolean
  errors: string[]
}

export interface RecipeFlavorMetricItem {
  label: string
  value: number
}

export interface RecipeIngredientItem {
  name: string
  amount: string
  note?: string | null
}

export interface RecipeStepItem {
  title: string
  detail: string
  hint?: string | null
}

export interface RecipeDetailPage {
  id: string
  name: string
  englishName?: string | null
  category: string
  heroImage: string
  highlight?: string | null
  subtitle: string
  description: string
  story?: string | null
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

export interface RecipeAiDetailGenerateResponse {
  detailContentId: number
  structuredRecordId: number
  recipeKey: string
  status: RecipePipelineStatus
  aiGeneratedAt?: string | null
  sourceSite?: string | null
  sourceUrl?: string | null
  validation: RecipeDetailValidationResult
  detail: RecipeDetailPage
}

export interface RecipeReviewPublishResponse {
  detailContentId: number
  recipeKey: string
  action: string
  status: RecipePipelineStatus
  reviewComment?: string | null
  reviewedAt?: string | null
  publishedAt?: string | null
  validation: RecipeDetailValidationResult
  detail: RecipeDetailPage
}

export interface RecipeCrawlTaskResponse {
  sourceSite: string
  entryUrl: string
  crawlMode?: string | null
  maxPages?: number | null
  maxItems?: number | null
  fetchDetailPages?: boolean | null
  totalSaved: number
  records: RecipeSourceRecord[]
}

export interface RecipeCandidateUpdateRequest {
  id: string
  name: string
  englishName?: string
  category: string
  heroImage: string
  highlight?: string
  subtitle: string
  description: string
  story?: string
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

export interface RecipeReviewActionRequest {
  reviewComment?: string
}

export const recipePipelineApi = {
  createCrawlTask(data: RecipeCrawlTaskRequest) {
    return http.post('/admin/recipe-pipeline/crawl-tasks', data) as Promise<RecipeCrawlTaskResponse>
  },
  getSourceRecords(params?: { status?: string }) {
    return http.get('/admin/recipe-pipeline/crawl-tasks', { params }) as Promise<RecipeSourceRecord[]>
  },
  getSourceRecord(id: number) {
    return http.get(`/admin/recipe-pipeline/source-records/${id}`) as Promise<RecipeSourceRecord>
  },
  parseSourceRecord(id: number) {
    return http.post(`/admin/recipe-pipeline/source-records/${id}/parse`) as Promise<RecipeStructuredRecord>
  },
  rejectSourceRecord(id: number) {
    return http.post(`/admin/recipe-pipeline/source-records/${id}/reject`) as Promise<RecipeSourceRecord>
  },
  getStructuredRecords(params?: { status?: string }) {
    return http.get('/admin/recipe-pipeline/structured-records', { params }) as Promise<RecipeStructuredRecord[]>
  },
  getStructuredRecord(id: number) {
    return http.get(`/admin/recipe-pipeline/structured-records/${id}`) as Promise<RecipeStructuredRecord>
  },
  generateAiDetail(id: number) {
    return http.post(`/admin/recipe-pipeline/structured-records/${id}/ai-generate`) as Promise<RecipeAiDetailGenerateResponse>
  },
  getCandidates(params?: { status?: string }) {
    return http.get('/admin/recipe-pipeline/candidates', { params }) as Promise<RecipeReviewPublishResponse[]>
  },
  getCandidate(id: number) {
    return http.get(`/admin/recipe-pipeline/candidates/${id}`) as Promise<RecipeReviewPublishResponse>
  },
  updateCandidate(id: number, data: RecipeCandidateUpdateRequest) {
    return http.put(`/admin/recipe-pipeline/candidates/${id}`, data) as Promise<RecipeReviewPublishResponse>
  },
  publishCandidate(id: number, data?: RecipeReviewActionRequest) {
    return http.post(`/admin/recipe-pipeline/candidates/${id}/publish`, data ?? {}) as Promise<RecipeReviewPublishResponse>
  },
  rejectCandidate(id: number, data?: RecipeReviewActionRequest) {
    return http.post(`/admin/recipe-pipeline/candidates/${id}/reject`, data ?? {}) as Promise<RecipeReviewPublishResponse>
  },
}
