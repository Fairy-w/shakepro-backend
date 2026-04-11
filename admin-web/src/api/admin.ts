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
  createdAt: string
}

export interface CocktailMaterialItem {
  materialId: number
  name?: string
  category?: string | null
  amount: string
}

export interface AdminCocktailListItem {
  id: number
  name: string
  imageUrl?: string | null
  alcoholLevel?: number | null
  createdAt: string
  updatedAt: string
}

export interface AdminCocktailDetail {
  id: number
  name: string
  description?: string | null
  imageUrl?: string | null
  alcoholLevel?: number | null
  steps?: string | null
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

export interface MaterialPayload {
  name: string
  category?: string
}

export interface CocktailPayload {
  name: string
  description?: string
  imageUrl?: string
  alcoholLevel?: number | null
  steps?: string
  materials: Array<{
    materialId: number
    amount: string
  }>
}

export const adminApi = {
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
  getCocktails(params: { keyword?: string; page?: number; size?: number }) {
    return http.get('/admin/cocktails', { params }) as Promise<PageResult<AdminCocktailListItem>>
  },
  getCocktail(id: number) {
    return http.get(`/admin/cocktails/${id}`) as Promise<AdminCocktailDetail>
  },
  createCocktail(data: CocktailPayload) {
    return http.post('/admin/cocktails', data) as Promise<AdminCocktailDetail>
  },
  updateCocktail(id: number, data: CocktailPayload) {
    return http.put(`/admin/cocktails/${id}`, data) as Promise<AdminCocktailDetail>
  },
  deleteCocktail(id: number) {
    return http.delete(`/admin/cocktails/${id}`) as Promise<void>
  },
}
