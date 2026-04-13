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

function getErrorMessage(err: unknown): string {
  if (err instanceof Error && err.message) {
    return err.message
  }

  return '登录失败'
}

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
  } catch (err: unknown) {
    error.value = getErrorMessage(err)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-stage card">
      <div class="stage-copy">
        <p class="stage-tag">SHAKEPRO / 深海调酒台</p>
        <h1>让采集、解析、审核、发布都汇入同一套后台控制台。</h1>
        <p class="stage-intro">
          这一版登录页已经和配方采集工作台统一成深海蓝视觉。管理员登录后可以直接进入同风格流水线后台，演示更完整。
        </p>
      </div>

      <div class="pipeline-preview">
        <article class="preview-card active">
          <span>01</span>
          <div>
            <strong>抓取入池</strong>
            <p>来源站点、入口链接、抓取数量统一发起。</p>
          </div>
        </article>
        <article class="preview-card">
          <span>02</span>
          <div>
            <strong>结构化解析</strong>
            <p>原始标题、配料与步骤自动整理成结构化结果。</p>
          </div>
        </article>
        <article class="preview-card">
          <span>03</span>
          <div>
            <strong>AI 详情生成</strong>
            <p>补齐故事、亮点、风味标签与详情页文案。</p>
          </div>
        </article>
        <article class="preview-card">
          <span>04</span>
          <div>
            <strong>审核发布</strong>
            <p>人工校对后进入后台成品库与终端消费链路。</p>
          </div>
        </article>
      </div>
    </section>

    <section class="login-panel card">
      <div class="panel-head">
        <p class="panel-tag">ADMIN ACCESS</p>
        <h2>管理员登录</h2>
        <p>默认预置了管理员账号，可直接进入后台查看整套内容生产与运营链路。</p>
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
          {{ loading ? '登录中...' : '进入深海调酒台' }}
        </button>
      </form>

      <div class="login-footnote">
        <span class="footnote-chip">后台统一深色视觉</span>
        <span class="footnote-chip subtle">可直接演示配方采集流水线</span>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  gap: 22px;
  padding: 22px;
  background:
    radial-gradient(circle at top left, rgba(72, 215, 255, 0.12), transparent 28%),
    radial-gradient(circle at bottom right, rgba(76, 111, 255, 0.14), transparent 32%);
}

.login-stage,
.login-panel {
  padding: 30px;
  min-height: calc(100vh - 44px);
}

.login-stage {
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 28px;
  background:
    radial-gradient(circle at top right, rgba(72, 215, 255, 0.18), transparent 30%),
    linear-gradient(180deg, rgba(9, 24, 39, 0.98), rgba(7, 18, 31, 0.98));
}

.stage-tag,
.panel-tag {
  margin: 0 0 12px;
  font-size: 0.74rem;
  letter-spacing: 0.24em;
  color: var(--primary);
}

.stage-copy h1 {
  margin: 0;
  font-size: clamp(2.6rem, 5vw, 4.8rem);
  line-height: 0.94;
  letter-spacing: -0.06em;
}

.stage-intro {
  max-width: 620px;
  margin-top: 18px;
  color: var(--ink-600);
  font-size: 1.04rem;
}

.pipeline-preview {
  display: grid;
  gap: 14px;
  align-content: end;
}

.preview-card {
  display: grid;
  grid-template-columns: 64px 1fr;
  gap: 16px;
  padding: 20px;
  border-radius: 22px;
  background: rgba(8, 22, 36, 0.8);
  border: 1px solid rgba(153, 199, 255, 0.1);
}

.preview-card.active {
  border-color: rgba(72, 215, 255, 0.24);
  background: linear-gradient(135deg, rgba(12, 35, 54, 0.96), rgba(8, 22, 36, 0.88));
}

.preview-card span {
  display: inline-flex;
  width: 64px;
  height: 64px;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background: rgba(72, 215, 255, 0.12);
  color: var(--primary);
  font-weight: 700;
}

.preview-card strong {
  display: block;
  margin-bottom: 6px;
  font-size: 1.08rem;
}

.preview-card p,
.panel-head p {
  margin: 0;
  color: var(--ink-600);
}

.login-panel {
  align-self: center;
  min-height: auto;
  background:
    radial-gradient(circle at top left, rgba(76, 111, 255, 0.14), transparent 30%),
    linear-gradient(180deg, rgba(10, 27, 43, 0.96), rgba(7, 18, 31, 0.98));
  display: grid;
  gap: 28px;
}

.panel-head h2 {
  margin: 0 0 10px;
  font-size: 2.1rem;
  letter-spacing: -0.04em;
}

.login-form {
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
  padding: 14px 18px;
}

.error-text {
  margin: 0;
  color: var(--danger);
  font-weight: 700;
}

.login-footnote {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.footnote-chip {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(72, 215, 255, 0.12);
  color: var(--primary);
  border: 1px solid rgba(72, 215, 255, 0.14);
}

.footnote-chip.subtle {
  background: rgba(76, 111, 255, 0.14);
  color: #cdd6ff;
}

@media (max-width: 1080px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-stage,
  .login-panel {
    min-height: auto;
  }
}

@media (max-width: 640px) {
  .login-page {
    padding: 14px;
  }

  .login-stage,
  .login-panel {
    padding: 22px;
  }

  .preview-card {
    grid-template-columns: 1fr;
  }
}
</style>
