<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const navItems = [
  { label: '仪表盘', to: '/dashboard', tag: '00' },
  { label: '配方采集工作台', to: '/recipe-pipeline', tag: '01' },
  { label: '用户管理', to: '/users', tag: '02' },
  { label: '收藏管理', to: '/favorites', tag: '03' },
  { label: '材料管理', to: '/materials', tag: '04' },
  { label: '鸡尾酒管理', to: '/cocktails', tag: '05' },
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
      <div class="sidebar-head">
        <p class="sidebar-tag">SHAKEPRO / 深海调酒台</p>
        <h1>Admin Pipeline</h1>
        <p class="sidebar-copy">把采集、审核、配方、用户和素材统一进同一套深海控制台里。</p>
      </div>

      <nav class="nav-list">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-link"
          :class="{ active: route.path.startsWith(item.to) }"
        >
          <span class="nav-tag">{{ item.tag }}</span>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-foot">
        <div class="profile-badge">
          <span class="profile-tag">CURRENT OPERATOR</span>
          <strong>{{ authStore.nickname || '管理员' }}</strong>
          <small>{{ authStore.user?.role || 'ADMIN' }}</small>
        </div>
        <button class="button-secondary ghost" @click="logout">退出登录</button>
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
          <span class="badge subtle">统一风格已切换为深海调酒台</span>
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
  grid-template-columns: 300px 1fr;
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
    radial-gradient(circle at top right, rgba(72, 215, 255, 0.18), transparent 32%),
    linear-gradient(180deg, rgba(9, 24, 39, 0.98), rgba(7, 18, 31, 0.98));
}

.sidebar-tag,
.topbar-tag,
.profile-tag {
  letter-spacing: 0.22em;
  font-size: 0.74rem;
  color: var(--primary);
  text-transform: uppercase;
}

.sidebar h1 {
  font-size: 2.1rem;
  line-height: 0.95;
  letter-spacing: -0.05em;
  margin: 14px 0 0;
  color: var(--ink-950);
}

.sidebar-copy {
  margin-top: 12px;
  color: var(--ink-600);
}

.nav-list {
  display: grid;
  gap: 10px;
  margin: 28px 0;
}

.nav-link {
  display: grid;
  grid-template-columns: 46px 1fr;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  border-radius: 18px;
  color: var(--ink-800);
  font-weight: 700;
  border: 1px solid transparent;
  background: rgba(11, 29, 46, 0.62);
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.nav-link:hover {
  transform: translateX(2px);
  border-color: rgba(72, 215, 255, 0.16);
}

.nav-link.active {
  background: linear-gradient(135deg, rgba(14, 37, 59, 0.96), rgba(10, 27, 43, 0.92));
  border-color: rgba(72, 215, 255, 0.24);
  color: var(--ink-950);
}

.nav-tag {
  display: inline-flex;
  width: 46px;
  height: 46px;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: rgba(72, 215, 255, 0.12);
  color: var(--primary);
}

.sidebar-foot {
  display: grid;
  gap: 16px;
}

.profile-badge {
  padding: 16px;
  border-radius: 20px;
  background: rgba(11, 29, 46, 0.82);
  border: 1px solid rgba(153, 199, 255, 0.08);
}

.profile-badge strong {
  display: block;
  margin-top: 10px;
}

.profile-badge small {
  display: block;
  margin-top: 6px;
  color: var(--ink-600);
}

.ghost {
  width: 100%;
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
  background:
    radial-gradient(circle at top left, rgba(72, 215, 255, 0.16), transparent 28%),
    linear-gradient(180deg, rgba(9, 24, 39, 0.94), rgba(7, 18, 31, 0.96));
}

.topbar h2 {
  margin: 8px 0 0;
  font-size: 1.9rem;
  letter-spacing: -0.05em;
  color: var(--ink-950);
}

.topbar-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.subtle {
  background: rgba(76, 111, 255, 0.14);
  color: #bfc9ff;
}

.view-slot {
  padding-bottom: 18px;
}

@media (max-width: 1180px) {
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

  .topbar,
  .topbar-meta {
    flex-direction: column;
    align-items: flex-start;
  }

  .nav-link {
    grid-template-columns: 40px 1fr;
  }

  .nav-tag {
    width: 40px;
    height: 40px;
  }
}
</style>
