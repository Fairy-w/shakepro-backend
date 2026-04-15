<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const form = ref({
  username: '',
  password: '',
})
const loading = ref(false)
const error = ref('')

async function submit() {
  if (!form.value.username || !form.value.password) {
    error.value = '请输入账号和密码'
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
      <p class="eyebrow">SHAKEPRO LOUNGE</p>
      <h1>把酒单、材料和收藏整理成一块清爽的玻璃面板。</h1>
      <p class="intro">进入工作台后，你可以统一查看成员数据、维护配方内容，并快速处理用户收藏。</p>
      <div class="brand-grid">
        <article class="brand-card">
          <span>01</span>
          <strong>经营概览</strong>
          <p>快速掌握用户、酒单、材料和收藏的当前规模。</p>
        </article>
        <article class="brand-card">
          <span>02</span>
          <strong>酒单维护</strong>
          <p>直接更新配方、步骤、图片与酒精度，让展示保持一致。</p>
        </article>
        <article class="brand-card">
          <span>03</span>
          <strong>收藏回看</strong>
          <p>查看用户保存的调酒灵感，及时整理或清理不需要的内容。</p>
        </article>
      </div>
    </section>

    <section class="login-panel card">
      <div class="panel-head">
        <p class="eyebrow darker">欢迎登录</p>
        <h2>进入酒饮工作台</h2>
        <p>使用你的管理账号登录后，就可以开始整理内容。</p>
      </div>

      <form class="login-form" @submit.prevent="submit">
        <label>
          <span>账号</span>
          <input v-model="form.username" class="field" type="text" autocomplete="username" placeholder="输入账号" />
        </label>

        <label>
          <span>密码</span>
          <input
            v-model="form.password"
            class="field"
            type="password"
            autocomplete="current-password"
            placeholder="输入密码"
          />
        </label>

        <p v-if="error" class="error-text">{{ error }}</p>

        <button class="button-primary submit-button" type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '进入工作台' }}
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
  border-radius: 34px;
  background:
    linear-gradient(135deg, rgba(11, 93, 87, 0.84), rgba(15, 23, 42, 0.72)),
    radial-gradient(circle at top right, rgba(125, 211, 252, 0.32), transparent 30%),
    radial-gradient(circle at 10% 100%, rgba(245, 158, 11, 0.24), transparent 26%);
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
  width: 320px;
  height: 320px;
  border-radius: 50%;
  right: -80px;
  top: -120px;
  background: rgba(255, 255, 255, 0.2);
  filter: blur(10px);
}

.eyebrow {
  letter-spacing: 0.28em;
  font-size: 0.72rem;
  opacity: 0.82;
  margin: 0;
}

.darker {
  color: var(--ink-600);
}

.login-brand h1 {
  font-size: clamp(2.5rem, 4vw, 4.8rem);
  line-height: 0.95;
  letter-spacing: -0.05em;
  margin: 18px 0 0;
  max-width: 720px;
}

.intro {
  margin-top: 18px;
  font-size: 1.04rem;
  max-width: 560px;
  color: rgba(255, 255, 255, 0.82);
}

.brand-grid {
  margin-top: 36px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.brand-card {
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.14);
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
  color: rgba(255, 255, 255, 0.78);
}

.login-panel {
  align-self: center;
  padding: 30px;
  background: linear-gradient(180deg, rgba(253, 255, 255, 0.88), rgba(240, 246, 250, 0.76));
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
  margin: 0;
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
