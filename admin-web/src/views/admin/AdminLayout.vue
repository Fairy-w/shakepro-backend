<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const navItems = [
  { label: '概览', to: '/dashboard' },
  { label: '用户', to: '/users' },
  { label: '收藏', to: '/favorites' },
  { label: '材料', to: '/materials' },
  { label: '鸡尾酒', to: '/cocktails' },
  { label: '网页抓取', to: '/crawler' },
]

const currentLabel = computed(() => navItems.find((item) => route.path.startsWith(item.to))?.label || '概览')
const roleLabel = computed(() => {
  const role = authStore.user?.role

  if (role === 'SUPER_ADMIN') {
    return '超级管理员'
  }

  if (role === 'ADMIN') {
    return '系统管理员'
  }

  return role || '管理账号'
})

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
      <div class="sidebar-main">
        <div class="brand-block">
          <p class="sidebar-tag">ShakePro Lounge</p>
          <h1>酒饮工作台</h1>
          <p class="sidebar-copy">把酒单、材料、收藏和成员信息放在同一块玻璃面板里，切换页面也始终稳稳停在手边。</p>
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
      </div>

      <div class="sidebar-foot">
        <div class="profile-badge">
          <span class="profile-label">当前账号</span>
          <strong>{{ authStore.nickname || '管理账号' }}</strong>
          <span>{{ roleLabel }}</span>
        </div>
        <button class="button-secondary" @click="logout">退出登录</button>
      </div>
    </aside>

    <main class="main-panel">
      <header class="topbar card">
        <div>
          <p class="topbar-tag">欢迎回来</p>
          <h2>{{ currentLabel }}</h2>
          <p class="topbar-copy">查看数据、整理内容、更新酒单，都从这里开始。</p>
        </div>
        <div class="topbar-meta">
          <span class="badge">内容与数据一站管理</span>
          <span class="badge subtle">左侧导航切换时保持固定位置</span>
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
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 20px;
  padding: 20px;
  width: 100%;
}

.sidebar {
  position: sticky;
  top: 20px;
  align-self: start;
  min-height: calc(100vh - 40px);
  padding: 24px;
  display: flex;
  flex-direction: column;
  background:
    linear-gradient(180deg, rgba(252, 255, 255, 0.78), rgba(236, 244, 248, 0.62)),
    radial-gradient(circle at top right, rgba(56, 189, 248, 0.14), transparent 34%),
    radial-gradient(circle at 0% 100%, rgba(245, 158, 11, 0.12), transparent 30%);
}

.sidebar-main {
  display: grid;
  gap: 28px;
}

.sidebar-tag,
.topbar-tag {
  margin: 0;
  letter-spacing: 0.22em;
  font-size: 0.74rem;
  color: var(--ink-600);
}

.sidebar h1 {
  font-size: 2.1rem;
  line-height: 0.95;
  letter-spacing: -0.05em;
  margin: 14px 0 0;
}

.sidebar-copy {
  margin: 12px 0 0;
  color: var(--ink-600);
}

.nav-list {
  display: grid;
  gap: 10px;
  align-content: start;
}

.nav-link {
  position: relative;
  padding: 15px 16px;
  border-radius: 20px;
  color: var(--ink-800);
  font-weight: 700;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.28);
  transition: background 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, color 0.2s ease;
}

.nav-link:hover {
  background: rgba(255, 255, 255, 0.52);
  border-color: rgba(255, 255, 255, 0.55);
}

.nav-link.active {
  background: linear-gradient(135deg, rgba(15, 118, 110, 0.18), rgba(255, 255, 255, 0.58));
  border-color: rgba(15, 118, 110, 0.12);
  color: var(--primary-strong);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.sidebar-foot {
  display: grid;
  gap: 16px;
  margin-top: auto;
}

.profile-badge {
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.42);
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

.profile-label {
  display: inline-flex;
  margin-bottom: 10px;
  color: var(--ink-600);
  font-size: 0.84rem;
}

.profile-badge strong {
  display: block;
  font-size: 1.05rem;
}

.profile-badge span:last-child {
  color: var(--ink-600);
  font-size: 0.92rem;
}

.main-panel {
  display: grid;
  gap: 20px;
  min-width: 0;
}

.topbar {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  padding: 22px 24px;
}

.topbar h2 {
  margin: 8px 0 0;
  font-size: 1.85rem;
  letter-spacing: -0.05em;
}

.topbar-copy {
  margin: 10px 0 0;
  color: var(--ink-600);
}

.topbar-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.subtle {
  background: var(--accent-soft);
  color: #b45309;
  border-color: rgba(245, 158, 11, 0.12);
}

.view-slot {
  padding-bottom: 20px;
}

@media (max-width: 1024px) {
  .layout-grid {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: relative;
    top: 0;
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
