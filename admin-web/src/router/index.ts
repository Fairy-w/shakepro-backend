import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/admin/Login.vue'),
  },
  {
    path: '/',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/login' },
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'users', name: 'users', component: () => import('@/views/admin/Users.vue') },
      { path: 'favorites', name: 'favorites', component: () => import('@/views/admin/Favorites.vue') },
      { path: 'community', name: 'community', component: () => import('@/views/admin/Community.vue') },
      { path: 'materials', name: 'materials', component: () => import('@/views/admin/Materials.vue') },
      { path: 'user-materials', name: 'user-materials', component: () => import('@/views/admin/UserMaterials.vue') },
      { path: 'cocktails', name: 'cocktails', component: () => import('@/views/admin/Cocktails.vue') },
      { path: 'crawler', name: 'crawler', component: () => import('@/views/admin/PageCrawler.vue') },
      { path: 'crawler-batch', name: 'crawler-batch', component: () => import('@/views/admin/BatchCrawler.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

function resolveToken() {
  const stored = localStorage.getItem('admin_token')
  if (!stored) return ''
  const normalized = stored.trim()
  if (!normalized || normalized === 'null' || normalized === 'undefined') {
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_user')
    return ''
  }
  return normalized
}

router.beforeEach((to, _from, next) => {
  const token = resolveToken()

  if (to.meta.requiresAuth && !token) {
    const redirectTarget = to.fullPath === '/' ? '/dashboard' : to.fullPath
    next({ name: 'login', query: { redirect: redirectTarget } })
    return
  }

  next()
})

export default router
