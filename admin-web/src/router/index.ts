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
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'users', name: 'users', component: () => import('@/views/admin/Users.vue') },
      { path: 'favorites', name: 'favorites', component: () => import('@/views/admin/Favorites.vue') },
      { path: 'materials', name: 'materials', component: () => import('@/views/admin/Materials.vue') },
      { path: 'cocktails', name: 'cocktails', component: () => import('@/views/admin/Cocktails.vue') },
      { path: 'crawler', name: 'crawler', component: () => import('@/views/admin/PageCrawler.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('admin_token')

  if (to.meta.requiresAuth && !token) {
    next({ name: 'login', query: { redirect: to.fullPath } })
    return
  }

  if (to.name === 'login' && token) {
    next({ name: 'dashboard' })
    return
  }

  next()
})

export default router
