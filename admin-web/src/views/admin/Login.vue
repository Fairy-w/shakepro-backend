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
      <p class="login-eyebrow">ShakePro Lounge</p>
      <h1>用一块天鹅绒般的运营面板，打理你的鸡尾酒库、材料库与 AI 工作流。</h1>
      <p class="login-intro">
        这不是普通后台，而是一套面向精品鸡尾酒内容、用户偏好与自动化导入流程的旗舰工作台。
      </p>

      <div class="brand-highlights">
        <article class="brand-tile">
          <span>01</span>
          <strong>今日运营叙事</strong>
          <p>先看到当天最该处理的酒单、材料、收藏和异常任务。</p>
        </article>
        <article class="brand-tile">
          <span>02</span>
          <strong>精品酒单编辑</strong>
          <p>图片、风味、步骤与服务建议统一收束在一个高质感编辑体验里。</p>
        </article>
        <article class="brand-tile">
          <span>03</span>
          <strong>AI 流程中枢</strong>
          <p>抓取、提取、批量导入与异常回放像控制台一样清晰展开。</p>
        </article>
      </div>
    </section>

    <section class="login-panel card">
      <div class="login-panel__glass"></div>
      <div class="login-panel__content">
        <p class="login-eyebrow darker">Velvet Reserve</p>
        <h2>进入酒廊运营中枢</h2>
        <p class="login-panel__copy">使用管理账号登录后，即可继续维护内容资产并推进当日任务。</p>

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

          <p v-if="error" class="error-text" role="alert">{{ error }}</p>

          <button class="button-primary submit-button" type="submit" :disabled="loading">
            {{ loading ? '登录中...' : '进入工作台' }}
          </button>
        </form>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.18fr 0.82fr;
  gap: 28px;
  padding: 28px;
}

.login-brand {
  position: relative;
  overflow: hidden;
  padding: 40px;
  border-radius: 42px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: #fff7f2;
  background:
    radial-gradient(circle at top right, rgba(200, 155, 91, 0.26), transparent 28%),
    radial-gradient(circle at 0% 100%, rgba(255, 255, 255, 0.12), transparent 24%),
    linear-gradient(140deg, rgba(74, 47, 42, 0.98), rgba(58, 38, 34, 0.98));
  box-shadow: var(--shadow-lg);
}

.login-brand::before,
.login-brand::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.login-brand::before {
  top: -80px;
  right: -40px;
  width: 300px;
  height: 300px;
  background: rgba(255, 255, 255, 0.08);
  filter: blur(12px);
}

.login-brand::after {
  bottom: -110px;
  left: -30px;
  width: 280px;
  height: 280px;
  background: rgba(200, 155, 91, 0.14);
  filter: blur(26px);
}

.login-eyebrow {
  margin: 0;
  font-size: 0.74rem;
  letter-spacing: 0.26em;
  text-transform: uppercase;
  opacity: 0.82;
}

.login-brand h1 {
  margin: 18px 0 0;
  font-size: clamp(3rem, 5vw, 5.6rem);
  line-height: 0.92;
  letter-spacing: -0.05em;
  max-width: 820px;
}

.login-intro {
  margin: 22px 0 0;
  max-width: 620px;
  color: rgba(255, 247, 242, 0.78);
  font-size: 1.04rem;
}

.brand-highlights {
  margin-top: 40px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.brand-tile {
  position: relative;
  z-index: 1;
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(14px);
}

.brand-tile span {
  display: inline-flex;
  width: 38px;
  height: 38px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  margin-bottom: 18px;
  font-family: var(--font-mono);
  background: rgba(200, 155, 91, 0.2);
}

.brand-tile strong {
  display: block;
  margin-bottom: 8px;
  font-size: 1.04rem;
}

.brand-tile p {
  margin: 0;
  color: rgba(255, 247, 242, 0.72);
}

.login-panel {
  position: relative;
  align-self: center;
  overflow: hidden;
  padding: 0;
}

.login-panel__glass {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 0% 0%, rgba(200, 155, 91, 0.14), transparent 24%),
    radial-gradient(circle at 100% 0%, rgba(122, 73, 63, 0.12), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.56), rgba(255, 255, 255, 0.34));
}

.login-panel__content {
  position: relative;
  z-index: 1;
  padding: 34px;
}

.darker {
  color: var(--ink-600);
}

.login-panel__content h2 {
  margin: 14px 0 8px;
  font-size: 2.35rem;
  line-height: 0.98;
  letter-spacing: -0.04em;
}

.login-panel__copy {
  margin: 0;
  color: var(--ink-600);
}

.login-form {
  margin-top: 30px;
  display: grid;
  gap: 18px;
}

.login-form label {
  display: grid;
  gap: 8px;
  color: var(--ink-800);
  font-weight: 600;
}

.submit-button {
  width: 100%;
}

.error-text {
  margin: 0;
  color: var(--danger);
  font-weight: 700;
}

@media (max-width: 1120px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .brand-highlights {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .login-page {
    padding: 16px;
  }

  .login-brand,
  .login-panel__content {
    padding: 24px;
  }
}
</style>
