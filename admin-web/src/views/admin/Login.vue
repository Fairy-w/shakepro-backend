<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const form = ref({
  username: 'admin',
  password: 'admin123456',
})
const loading = ref(false)
const error = ref('')

async function submit() {
  if (!form.value.username || !form.value.password) {
    error.value = '请输入管理员账号和密码'
    return
  }

  loading.value = true
  error.value = ''

  try {
    await authStore.login(form.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.replace(redirect)
  } catch (err: any) {
    error.value = err.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-brand">
      <p class="eyebrow">SHAKEPRO ADMIN CONSOLE</p>
      <h1>把鸡尾酒内容、用户和素材都收进一个控制台。</h1>
      <p class="intro">
        现在这套 Web 已经切成后台管理端，鸿蒙 App 负责 C 端体验，这里专注数据治理和内容运营。
      </p>
      <div class="brand-grid">
        <article class="brand-card">
          <span>01</span>
          <strong>仪表盘</strong>
          <p>快速看用户、鸡尾酒、材料、收藏和文件规模。</p>
        </article>
        <article class="brand-card">
          <span>02</span>
          <strong>内容维护</strong>
          <p>直接管理鸡尾酒配方与材料库，避免前后台数据割裂。</p>
        </article>
        <article class="brand-card">
          <span>03</span>
          <strong>可继续扩展</strong>
          <p>后续可继续接操作日志、Banner 管理和 AI 调用记录。</p>
        </article>
      </div>
    </section>

    <section class="login-panel card">
      <div class="panel-head">
        <p class="eyebrow darker">ADMIN LOGIN</p>
        <h2>管理员登录</h2>
        <p>默认已预置管理员账号，可直接登录后开始管理。</p>
      </div>

      <form class="login-form" @submit.prevent="submit">
        <label>
          <span>账号</span>
          <input v-model="form.username" class="field" type="text" autocomplete="username" placeholder="admin" />
        </label>

        <label>
          <span>密码</span>
          <input
            v-model="form.password"
            class="field"
            type="password"
            autocomplete="current-password"
            placeholder="输入管理员密码"
          />
        </label>

        <p v-if="error" class="error-text">{{ error }}</p>

        <button class="button-primary submit-button" type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '进入后台' }}
        </button>
      </form>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 28px;
  padding: 32px;
}

.login-brand {
  padding: 34px;
  border-radius: 32px;
  background:
    linear-gradient(135deg, rgba(15, 118, 110, 0.92), rgba(21, 33, 43, 0.92)),
    linear-gradient(135deg, rgba(255, 255, 255, 0.16), transparent);
  color: white;
  box-shadow: var(--shadow-lg);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  position: relative;
}

.login-brand::after {
  content: '';
  position: absolute;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  right: -70px;
  top: -90px;
  background: rgba(245, 158, 11, 0.26);
  filter: blur(10px);
}

.eyebrow {
  letter-spacing: 0.28em;
  font-size: 0.72rem;
  opacity: 0.82;
}

.darker {
  color: var(--ink-600);
}

.login-brand h1 {
  font-size: clamp(2.4rem, 4vw, 4.6rem);
  line-height: 0.95;
  letter-spacing: -0.05em;
  margin-top: 18px;
  max-width: 720px;
}

.intro {
  margin-top: 18px;
  font-size: 1.04rem;
  max-width: 580px;
  color: rgba(255, 255, 255, 0.8);
}

.brand-grid {
  margin-top: 36px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.brand-card {
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(8px);
}

.brand-card span {
  display: inline-flex;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.18);
}

.brand-card strong {
  display: block;
  margin-bottom: 8px;
  font-size: 1.08rem;
}

.brand-card p,
.panel-head p {
  color: rgba(255, 255, 255, 0.75);
}

.login-panel {
  align-self: center;
  padding: 28px;
  background: rgba(255, 253, 248, 0.95);
}

.panel-head h2 {
  font-size: 2rem;
  letter-spacing: -0.04em;
  margin: 12px 0 6px;
}

.panel-head p {
  color: var(--ink-600);
}

.login-form {
  display: grid;
  gap: 18px;
  margin-top: 28px;
}

.login-form label {
  display: grid;
  gap: 8px;
  color: var(--ink-800);
  font-weight: 600;
}

.submit-button {
  width: 100%;
  padding: 14px 18px;
}

.error-text {
  color: var(--danger);
  font-weight: 600;
}

@media (max-width: 1040px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .brand-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .login-page {
    padding: 16px;
  }

  .login-brand,
  .login-panel {
    padding: 22px;
  }
}
</style>
