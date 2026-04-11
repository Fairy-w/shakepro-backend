import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api'
import type { AdminProfile, LoginRequest } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const user = ref<AdminProfile | null>(JSON.parse(localStorage.getItem('admin_user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const nickname = computed(() => user.value?.nickname || user.value?.username || '')

  async function login(data: LoginRequest) {
    const response = await authApi.login(data)
    token.value = response.token
    user.value = response.user
    localStorage.setItem('admin_token', token.value)
    localStorage.setItem('admin_user', JSON.stringify(user.value))
  }

  async function fetchMe() {
    if (!token.value) return
    try {
      user.value = await authApi.getMe()
      localStorage.setItem('admin_user', JSON.stringify(user.value))
    } catch {
      logout()
    }
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_user')
  }

  return { token, user, isLoggedIn, nickname, login, fetchMe, logout }
})
