import axios from 'axios'
import http from './http'

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface DashboardStats {
  totalUsers: number
  totalAdmins: number
  totalCocktails: number
  totalMaterials: number
  totalFavorites: number
  totalAiCocktailFavorites: number
  totalFiles: number
}

export interface AdminUser {
  id: number
  username: string
  nickname: string
  avatarUrl?: string | null
  role: string
  enabled: boolean
  createdAt: string
}

export interface AdminMaterial {
  id: number
  name: string
  category?: string | null
  nameEn?: string | null
  imageUrl?: string | null
  source?: string | null
  sourceId?: string | null
  createdAt: string
}

export interface AdminMaterialSyncPayload {
  maxItems?: number
  dryRun?: boolean
  overwriteImage?: boolean
}

export interface AdminMaterialSyncResult {
  totalFetched: number
  processed: number
  matchedByDictionary: number
  created: number
  updated: number
  skippedNoDictionary: number
  skippedImageExists: number
  failed: number
  dryRun: boolean
}

export interface UserMaterialItem {
  id: number
  userId: number
  source?: string | null
  name: string
  brand?: string | null
  categoryId?: string | null
  barcode: string
  capacityText?: string | null
  remainLevel?: string | null
  opened: boolean
  hasItem: boolean
  tags: string[]
  materialId?: number | null
  createdAt: string
  updatedAt: string
}

export interface CocktailMaterialItem {
  materialId?: number | null
  name?: string
  category?: string | null
  displayName?: string | null
  amount?: string | null
  note?: string | null
  sortOrder?: number | null
}

export interface CocktailStepItem {
  order: number
  title?: string | null
  detail: string
}

export interface CocktailFlavorMetricItem {
  sortOrder?: number | null
  name: string
  value: number
}

export interface AdminCocktailListItem {
  id: number
  name: string
  englishName?: string | null
  category?: string | null
  heroImage?: string | null
  difficulty?: string | null
  abv?: string | null
  imageUrl?: string | null
  alcoholLevel?: number | null
  createdAt: string
  updatedAt: string
}

export interface AdminCocktailDetail {
  id: number
  name: string
  englishName?: string | null
  category?: string | null
  heroImage?: string | null
  difficulty?: string | null
  abv?: string | null
  glass?: string | null
  garnish?: string | null
  highlight?: string | null
  subtitle?: string | null
  description?: string | null
  story?: string | null
  imageUrl?: string | null
  alcoholLevel?: number | null
  legacySteps?: string | null
  flavorTags: string[]
  flavorMetrics: CocktailFlavorMetricItem[]
  pairings: string[]
  serviceNotes: string[]
  steps: CocktailStepItem[]
  materials: CocktailMaterialItem[]
  createdAt: string
  updatedAt: string
}

export interface AdminAiFavorite {
  id: number
  userId: number
  username?: string | null
  nickname?: string | null
  recipeKey: string
  name: string
  description?: string | null
  materials: string[]
  steps: string[]
  prompt?: string | null
  source?: string | null
  createdAt: string
  updatedAt: string
}

export interface AdminPageTextResult {
  url: string
  title: string
  html: string
}

export interface ExtractedIngredientItem {
  name?: string | null
  amount?: string | null
  note?: string | null
}

export interface ExtractedStepItem {
  title?: string | null
  detail?: string | null
}

export interface FieldSource {
  mode?: string | null
  source?: string | null
  note?: string | null
}

export interface AdminExtractedFieldsResult {
  url: string
  title?: string | null
  extractMode?: string | null
  generateMode?: string | null
  name?: string | null
  englishName?: string | null
  category?: string | null
  heroImage?: string | null
  difficulty?: string | null
  abv?: string | null
  glass?: string | null
  garnish?: string | null
  highlight?: string | null
  subtitle?: string | null
  description?: string | null
  story?: string | null
  flavorTags: string[]
  flavorMetrics: Record<string, number>
  pairings: string[]
  serviceNotes: string[]
  ingredients: ExtractedIngredientItem[]
  steps: ExtractedStepItem[]
  fieldSources: Record<string, FieldSource>
  missingFields: string[]
}

export interface AdminBatchImportItemResult {
  index: number
  url: string
  status: 'SUCCESS' | 'FAILED' | string
  stage?: string | null
  title?: string | null
  name?: string | null
  savedCocktailId?: number | null
  errorMessage?: string | null
  missingFields: string[]
  fields?: AdminExtractedFieldsResult | null
}

export interface AdminBatchImportResult {
  listUrl: string
  listTitle?: string | null
  discoveredCount: number
  selectedCount: number
  processedCount: number
  successCount: number
  failureCount: number
  remainingUnimportedCount: number
  durationMs: number
  items: AdminBatchImportItemResult[]
}

export interface AdminBatchImportHistoryItem {
  id: number
  listUrl: string
  listTitle?: string | null
  onlyNew: boolean
  maxItems: number
  concurrency: number
  autoGenerate: boolean
  autoSave: boolean
  discoveredCount: number
  selectedCount: number
  processedCount: number
  successCount: number
  failureCount: number
  remainingUnimportedCount: number
  durationMs: number
  status: 'SUCCESS' | 'PARTIAL' | 'FAILED' | 'SKIPPED' | string
  errorMessage?: string | null
  createdAt: string
}

export interface AdminBatchImportJobStartResult {
  jobId: string
  status: 'PENDING' | string
  message?: string | null
}

export interface AdminBatchImportJobStatusResult {
  jobId: string
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'PARTIAL' | 'FAILED' | 'SKIPPED' | string
  message?: string | null
  listUrl?: string | null
  listTitle?: string | null
  maxItems?: number | null
  concurrency?: number | null
  autoGenerate?: boolean | null
  autoSave?: boolean | null
  onlyNew?: boolean | null
  discoveredCount?: number | null
  selectedCount?: number | null
  processedCount?: number | null
  successCount?: number | null
  failureCount?: number | null
  remainingUnimportedCount?: number | null
  progressPercent?: number | null
  durationMs?: number | null
  currentUrl?: string | null
  currentStage?: string | null
  startedAtEpochMs?: number | null
  updatedAtEpochMs?: number | null
  errorMessage?: string | null
}

export interface OssPresignPayload {
  filename: string
  contentType: string
  size: number
}

export interface OssPresignResult {
  uploadUrl: string
  objectKey: string
  publicUrl: string
  expireSeconds: number
}

export interface OssFileRecordPayload {
  objectKey: string
  url: string
  contentType?: string
  size?: number
}

export interface OssFileRecordResult {
  fileId: number
}

const AI_GENERATE_TIMEOUT_MS = 60000

export interface MaterialPayload {
  name: string
  category?: string
}

export interface GeneratedCocktailPayload {
  name: string
  englishName?: string
  category?: string
  heroImage?: string
  difficulty?: string
  abv?: string
  glass?: string
  garnish?: string
  highlight?: string
  subtitle?: string
  description?: string
  story?: string
  sourceUrl?: string
  flavorTags: string[]
  flavorMetrics: Array<{
    name: string
    value: number
  }>
  pairings: string[]
  serviceNotes: string[]
  ingredients: Array<{
    materialId?: number
    name: string
    amount?: string
    note?: string
  }>
  steps: Array<{
    title?: string
    detail: string
  }>
}

export const adminApi = {
  createOssPresign(data: OssPresignPayload) {
    return http.post('/oss/presign', data) as Promise<OssPresignResult>
  },
  async uploadToOss(uploadUrl: string, file: Blob, contentType: string) {
    await axios.put(uploadUrl, file, {
      headers: {
        'Content-Type': contentType,
      },
      timeout: 60000,
    })
  },
  saveOssFileRecord(data: OssFileRecordPayload) {
    return http.post('/files', data) as Promise<OssFileRecordResult>
  },
  getDashboard() {
    return http.get('/admin/dashboard') as Promise<DashboardStats>
  },
  getUsers(params: { keyword?: string; page?: number; size?: number }) {
    return http.get('/admin/users', { params }) as Promise<PageResult<AdminUser>>
  },
  getAiFavorites(params: { keyword?: string; page?: number; size?: number }) {
    return http.get('/admin/favorites/ai-cocktails', { params }) as Promise<PageResult<AdminAiFavorite>>
  },
  deleteAiFavorite(id: number) {
    return http.delete(`/admin/favorites/ai-cocktails/${id}`) as Promise<void>
  },
  crawlPageText(data: { url: string }) {
    return http.post('/admin/crawl/page-text', data) as Promise<AdminPageTextResult>
  },
  extractPageFields(data: { url: string; title?: string; html: string }) {
    return http.post('/admin/crawl/extract-fields', data) as Promise<AdminExtractedFieldsResult>
  },
  generatePageFields(data: AdminExtractedFieldsResult) {
    return http.post('/admin/crawl/generate-fields', data, { timeout: AI_GENERATE_TIMEOUT_MS }) as Promise<AdminExtractedFieldsResult>
  },
  importFromList(data: { listUrl: string; maxItems?: number; concurrency?: number; autoGenerate?: boolean; autoSave?: boolean; onlyNew?: boolean }) {
    return http.post('/admin/crawl/import-from-list', data, { timeout: 180000 }) as Promise<AdminBatchImportResult>
  },
  startBatchImportJob(data: { listUrl: string; maxItems?: number; concurrency?: number; autoGenerate?: boolean; autoSave?: boolean; onlyNew?: boolean }) {
    return http.post('/admin/crawl/import-from-list/jobs', data, { timeout: 30000 }) as Promise<AdminBatchImportJobStartResult>
  },
  getBatchImportJobStatus(jobId: string) {
    return http.get(`/admin/crawl/import-from-list/jobs/${encodeURIComponent(jobId)}`) as Promise<AdminBatchImportJobStatusResult>
  },
  getBatchImportHistories(params: { page?: number; size?: number }) {
    return http.get('/admin/crawl/import-histories', { params }) as Promise<PageResult<AdminBatchImportHistoryItem>>
  },
  getMaterials(params?: { keyword?: string; category?: string }) {
    return http.get('/admin/materials', { params }) as Promise<AdminMaterial[]>
  },
  createMaterial(data: MaterialPayload) {
    return http.post('/admin/materials', data) as Promise<AdminMaterial>
  },
  updateMaterial(id: number, data: MaterialPayload) {
    return http.put(`/admin/materials/${id}`, data) as Promise<AdminMaterial>
  },
  deleteMaterial(id: number) {
    return http.delete(`/admin/materials/${id}`) as Promise<void>
  },
  syncCocktailDbMaterials(data?: AdminMaterialSyncPayload) {
    return http.post('/admin/materials/sync/cocktaildb', data || {}) as Promise<AdminMaterialSyncResult>
  },
  getCocktails(params: { keyword?: string; category?: string; page?: number; size?: number }) {
    return http.get('/admin/cocktails', { params }) as Promise<PageResult<AdminCocktailListItem>>
  },
  getCocktailCategories() {
    return http.get('/admin/cocktails/categories') as Promise<string[]>
  },
  getCocktail(id: number) {
    return http.get(`/admin/cocktails/${id}`) as Promise<AdminCocktailDetail>
  },
  createGeneratedCocktail(data: GeneratedCocktailPayload) {
    return http.post('/admin/cocktails/generated', data) as Promise<AdminCocktailDetail>
  },
  updateGeneratedCocktail(id: number, data: GeneratedCocktailPayload) {
    return http.put(`/admin/cocktails/generated/${id}`, data) as Promise<AdminCocktailDetail>
  },
  deleteCocktail(id: number) {
    return http.delete(`/admin/cocktails/${id}`) as Promise<void>
  },
  getUserMaterials(userId: number, params?: { keyword?: string; categoryId?: string }) {
    return http.get('/admin/user-materials', { params: { userId, ...(params || {}) } }) as Promise<UserMaterialItem[]>
  },
}
