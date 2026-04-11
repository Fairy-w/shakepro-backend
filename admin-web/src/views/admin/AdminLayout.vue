<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const navItems = [
  { label: '仪表盘', to: '/dashboard' },
  { label: '用户管理', to: '/users' },
  { label: '收藏管理', to: '/favorites' },
  { label: '材料管理', to: '/materials' },
  { label: '鸡尾酒管理', to: '/cocktails' },
]

const currentLabel = computed(() => navItems.find((item) => route.path.startsWith(item.to))?.label || '后台管理')

onMounted(() => {
  if (!authStore.user) {
    authStore.fetchMe()
  }
})

function logout() {
  authStore.logout()
  router.replace('/login')
}
</script>

<template>
  <div class="admin-shell layout-grid">
    <aside class="sidebar card">
      <div>
        <p class="sidebar-tag">ShakePro</p>
        <h1>Admin Console</h1>
        <p class="sidebar-copy">运营、配方、收藏、用户和素材统一维护。</p>
      </div>

      <nav class="nav-list">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-link"
          :class="{ active: route.path.startsWith(item.to) }"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="sidebar-foot">
        <div class="profile-badge">
          <strong>{{ authStore.nickname || '管理员' }}</strong>
          <span>{{ authStore.user?.role || 'ADMIN' }}</span>
        </div>
        <button class="button-secondary" @click="logout">退出登录</button>
      </div>
    </aside>

    <main class="main-panel">
      <header class="topbar card">
        <div>
          <p class="topbar-tag">CONTROL PANEL</p>
          <h2>{{ currentLabel }}</h2>
        </div>
        <div class="topbar-meta">
          <span class="badge">后台管理端</span>
          <span class="badge subtle">鸿蒙 App 负责 C 端</span>
        </div>
      </header>

      <div class="view-slot">
        <RouterView />
      </div>
    </main>
  </div>
</template>

<style scoped>
.layout-grid {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 18px;
  padding: 18px;
}

.sidebar {
  min-height: calc(100vh - 36px);
  padding: 24px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(242, 246, 245, 0.9)),
    radial-gradient(circle at top right, rgba(15, 118, 110, 0.1), transparent 42%);
}

.sidebar-tag,
.topbar-tag {
  letter-spacing: 0.22em;
  font-size: 0.74rem;
  color: var(--ink-600);
}

.sidebar h1 {
  font-size: 2rem;
  line-height: 0.95;
  letter-spacing: -0.05em;
  margin-top: 14px;
}

.sidebar-copy {
  margin-top: 10px;
  color: var(--ink-600);
}

.nav-list {
  display: grid;
  gap: 10px;
  margin: 26px 0;
}

.nav-link {
  padding: 14px 16px;
  border-radius: 18px;
  color: var(--ink-800);
  font-weight: 700;
  border: 1px solid transparent;
  transition: background 0.2s ease, transform 0.2s ease;
}

.nav-link:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateX(2px);
}

.nav-link.active {
  background: linear-gradient(135deg, rgba(15, 118, 110, 0.14), rgba(245, 158, 11, 0.1));
  border-color: rgba(15, 118, 110, 0.16);
  color: var(--primary-strong);
}

.sidebar-foot {
  display: grid;
  gap: 16px;
}

.profile-badge {
  padding: 16px;
  border-radius: 20px;
  background: rgba(15, 118, 110, 0.08);
}

.profile-badge strong {
  display: block;
}

.profile-badge span {
  color: var(--ink-600);
  font-size: 0.9rem;
}

.main-panel {
  display: grid;
  gap: 18px;
}

.topbar {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  padding: 20px 24px;
}

.topbar h2 {
  margin-top: 6px;
  font-size: 1.8rem;
  letter-spacing: -0.05em;
}

.topbar-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.subtle {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.view-slot {
  padding-bottom: 18px;
}

@media (max-width: 1024px) {
  .layout-grid {
    grid-template-columns: 1fr;
  }

  .sidebar {
    min-height: auto;
  }
}

@media (max-width: 640px) {
  .layout-grid {
    padding: 12px;
  }

  .sidebar,
  .topbar {
    padding: 18px;
  }

  .topbar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
