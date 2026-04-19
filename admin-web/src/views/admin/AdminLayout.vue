<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

interface NavItem {
  label: string
  to: string
  group: string
  pulse: string
}

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const navItems: NavItem[] = [
  {
    label: '概览',
    to: '/dashboard',
    group: '旗舰总览',
    pulse: '今日指挥台',
  },
  {
    label: '鸡尾酒',
    to: '/cocktails',
    group: '内容资产',
    pulse: '酒单策展',
  },
  {
    label: '材料',
    to: '/materials',
    group: '内容资产',
    pulse: '基础库存',
  },
  {
    label: '用户材料',
    to: '/user-materials',
    group: '内容资产',
    pulse: '用户画像',
  },
  {
    label: '用户',
    to: '/users',
    group: '用户与收藏',
    pulse: '成员结构',
  },
  {
    label: '收藏',
    to: '/favorites',
    group: '用户与收藏',
    pulse: '灵感沉淀',
  },
  {
    label: '网页抓取',
    to: '/crawler',
    group: 'AI 工作台',
    pulse: '单页流程',
  },
  {
    label: '批量抓取',
    to: '/crawler-batch',
    group: 'AI 工作台',
    pulse: '批处理控制',
  },
]

const navGroups = computed(() => {
  const grouped = new Map<string, NavItem[]>()
  navItems.forEach((item) => {
    const current = grouped.get(item.group) || []
    current.push(item)
    grouped.set(item.group, current)
  })
  return Array.from(grouped.entries()).map(([label, items]) => ({ label, items }))
})

const currentNavItem = computed(() => {
  const matches = navItems.filter((item) => route.path.startsWith(item.to))
  if (!matches.length) {
    return navItems[0]
  }
  return matches.sort((left, right) => right.to.length - left.to.length)[0]
})

function isActiveNav(path: string): boolean {
  return currentNavItem.value?.to === path
}

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

const initials = computed(() => (authStore.nickname || authStore.user?.username || 'SP').slice(0, 2).toUpperCase())

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
  <a class="skip-link" href="#admin-main">跳到主内容</a>
  <div class="admin-shell reserve-layout">
    <aside class="reserve-sidebar">
      <div class="reserve-sidebar__top">
        <p class="reserve-sidebar__eyebrow">ShakePro Lounge</p>
        <h1>Velvet Reserve</h1>
        <p class="reserve-sidebar__copy">把鸡尾酒内容、用户偏好与 AI 处理流程收束到一套旗舰运营中枢里。</p>
      </div>

      <div class="reserve-nav-groups">
        <section v-for="group in navGroups" :key="group.label" class="reserve-nav-group">
          <p class="reserve-nav-group__label">{{ group.label }}</p>
          <nav class="reserve-nav-list">
            <RouterLink
              v-for="item in group.items"
              :key="item.to"
              :to="item.to"
              class="reserve-nav-link"
              :class="{ active: isActiveNav(item.to) }"
            >
              <span>{{ item.label }}</span>
              <small>{{ item.pulse }}</small>
            </RouterLink>
          </nav>
        </section>
      </div>

      <div class="reserve-sidebar__foot">
        <div class="operator-card">
          <div class="operator-card__avatar">{{ initials }}</div>
          <div>
            <span class="operator-card__label">当前账号</span>
            <strong>{{ authStore.nickname || authStore.user?.username || 'ShakePro' }}</strong>
            <small>{{ roleLabel }}</small>
          </div>
        </div>
        <button class="button-secondary" @click="logout">退出登录</button>
      </div>
    </aside>

    <main id="admin-main" class="reserve-main">
      <div class="reserve-content">
        <RouterView />
      </div>
    </main>
  </div>
</template>

<style scoped>
.reserve-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 20px;
  padding: 20px;
}

.reserve-sidebar {
  position: sticky;
  top: 20px;
  align-self: start;
  min-height: calc(100vh - 40px);
  padding: 24px;
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at top right, rgba(200, 155, 91, 0.16), transparent 28%),
    linear-gradient(180deg, rgba(74, 47, 42, 0.98), rgba(49, 31, 28, 0.98));
  color: #fff8f0;
  box-shadow: var(--shadow-lg);
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.reserve-sidebar__eyebrow,
.reserve-nav-group__label,
.operator-card__label {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.reserve-sidebar h1 {
  margin: 10px 0 0;
  font-size: 2.45rem;
  line-height: 0.9;
}

.reserve-sidebar__copy {
  margin: 14px 0 0;
  color: rgba(255, 248, 240, 0.76);
}

.reserve-nav-groups {
  display: grid;
  gap: 20px;
}

.reserve-nav-group {
  display: grid;
  gap: 10px;
}

.reserve-nav-group__label {
  color: rgba(255, 248, 240, 0.48);
}

.reserve-nav-list {
  display: grid;
  gap: 8px;
}

.reserve-nav-link {
  padding: 13px 14px;
  border-radius: 18px;
  display: grid;
  gap: 4px;
  color: rgba(255, 248, 240, 0.92);
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.04);
  transition: transform 0.2s ease, background 0.2s ease, border-color 0.2s ease;
}

.reserve-nav-link small {
  color: rgba(255, 248, 240, 0.58);
  font-size: 0.82rem;
}

.reserve-nav-link:hover {
  transform: translateX(2px);
  background: rgba(255, 255, 255, 0.08);
}

.reserve-nav-link.active {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.16), rgba(200, 155, 91, 0.12));
  border-color: rgba(200, 155, 91, 0.18);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.reserve-sidebar__foot {
  margin-top: auto;
  display: grid;
  gap: 14px;
}

.operator-card {
  padding: 18px;
  border-radius: 22px;
  display: flex;
  gap: 14px;
  align-items: center;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.operator-card__avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-family: var(--font-mono);
  font-weight: 700;
  background: linear-gradient(135deg, rgba(200, 155, 91, 0.82), rgba(255, 255, 255, 0.2));
  color: #fff;
}

.operator-card strong,
.operator-card small {
  display: block;
}

.operator-card small {
  color: rgba(255, 248, 240, 0.68);
}

.reserve-main {
  min-width: 0;
  display: grid;
  gap: 0;
  align-content: start;
}

.reserve-content {
  min-width: 0;
  padding-bottom: 20px;
}

@media (max-width: 1360px) {
  .reserve-layout {
    grid-template-columns: 260px minmax(0, 1fr);
  }
}

@media (max-width: 980px) {
  .reserve-layout {
    grid-template-columns: 1fr;
  }

  .reserve-sidebar {
    position: static;
    min-height: auto;
  }
}

@media (max-width: 720px) {
  .reserve-layout {
    padding: 14px;
  }
}
</style>
