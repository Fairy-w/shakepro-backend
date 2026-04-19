<script setup lang="ts">
import { computed } from 'vue'
import type { AdminCocktailDetail } from '@/api/admin'
import AdminDialog from '@/components/admin/AdminDialog.vue'

const props = defineProps<{
  visible: boolean
  loading: boolean
  detail: AdminCocktailDetail | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'edit'): void
}>()

const imageUrl = computed(() => props.detail?.heroImage || props.detail?.imageUrl || '')
</script>

<template>
  <AdminDialog :visible="visible" size="xl" @close="emit('close')">
    <div v-if="loading" class="dialog-shell">
      <div class="dialog-info-card state-shell">
        <h3>加载中...</h3>
        <p>正在读取鸡尾酒详情。</p>
      </div>
    </div>

    <div v-else-if="detail" class="dialog-shell detail-dialog">
      <header class="dialog-head">
        <div>
          <p class="dialog-head__eyebrow">Cocktail Profile</p>
          <h2 class="dialog-head__title">{{ detail.name }}</h2>
          <p v-if="detail.englishName" class="dialog-head__subtitle">{{ detail.englishName }}</p>
        </div>
        <div class="dialog-actions">
          <button class="button-secondary" type="button" @click="emit('close')">关闭</button>
          <button class="button-primary" type="button" @click="emit('edit')">编辑这款酒</button>
        </div>
      </header>

      <section class="detail-hero-grid">
        <article class="dialog-preview-card hero-image-shell">
          <img v-if="imageUrl" :src="imageUrl" :alt="detail.name" />
          <div v-else class="empty-image">暂无图片</div>
        </article>
        <article class="dialog-info-card hero-meta-card">
          <div class="dialog-section__head">
            <h3>首屏信息</h3>
            <p>用于卡片展示、详情页开头和品牌描述。</p>
          </div>
          <dl class="inspector-list">
            <div><dt>分类</dt><dd>{{ detail.category || '未标注' }}</dd></div>
            <div><dt>难度</dt><dd>{{ detail.difficulty || '未标注' }}</dd></div>
            <div><dt>ABV</dt><dd>{{ detail.abv || (detail.alcoholLevel != null ? `${detail.alcoholLevel}%` : '未标注') }}</dd></div>
            <div><dt>杯型</dt><dd>{{ detail.glass || '未标注' }}</dd></div>
            <div><dt>装饰</dt><dd>{{ detail.garnish || '未标注' }}</dd></div>
            <div><dt>主图链接</dt><dd class="mono break-all">{{ imageUrl || '未填写' }}</dd></div>
          </dl>
        </article>
      </section>

      <section class="dialog-section">
        <div class="dialog-section__head">
          <h3>内容说明</h3>
          <p>集中查看副标题、亮点文案、描述和故事背景。</p>
        </div>
        <p class="detail-copy">{{ detail.subtitle || detail.highlight || detail.description || '暂无说明' }}</p>
        <p v-if="detail.story" class="detail-copy">{{ detail.story }}</p>
      </section>

      <section class="dual-grid">
        <article class="dialog-section">
          <div class="dialog-section__head">
            <h3>风味标签</h3>
          </div>
          <div class="chip-list">
            <span v-for="tag in detail.flavorTags" :key="tag" class="badge">{{ tag }}</span>
            <span v-if="!detail.flavorTags.length" class="empty-text">暂无风味标签</span>
          </div>
        </article>

        <article class="dialog-section">
          <div class="dialog-section__head">
            <h3>风味指标</h3>
          </div>
          <div class="chip-list">
            <span v-for="metric in detail.flavorMetrics" :key="`${metric.name}-${metric.sortOrder ?? ''}`" class="status-pill warn">
              {{ metric.name }} {{ metric.value }}
            </span>
            <span v-if="!detail.flavorMetrics.length" class="empty-text">暂无风味指标</span>
          </div>
        </article>
      </section>

      <section class="dual-grid">
        <article class="dialog-section">
          <div class="dialog-section__head">
            <h3>材料明细</h3>
          </div>
          <ul class="stack-list">
            <li v-for="(item, index) in detail.materials" :key="`${index}-${item.name || item.displayName || 'material'}`">
              <strong>{{ item.displayName || item.name || '未命名材料' }}</strong>
              <span>{{ item.amount || '未填写用量' }}</span>
              <p v-if="item.note">{{ item.note }}</p>
            </li>
            <li v-if="!detail.materials.length" class="empty-text">暂无材料信息</li>
          </ul>
        </article>

        <article class="dialog-section">
          <div class="dialog-section__head">
            <h3>制作步骤</h3>
          </div>
          <ul class="stack-list">
            <li v-for="(step, index) in detail.steps" :key="`${index}-${step.title || 'step'}`">
              <strong>{{ step.title || `步骤 ${index + 1}` }}</strong>
              <p>{{ step.detail }}</p>
            </li>
            <li v-if="!detail.steps.length && detail.legacySteps" class="legacy-step">{{ detail.legacySteps }}</li>
            <li v-if="!detail.steps.length && !detail.legacySteps" class="empty-text">暂无步骤信息</li>
          </ul>
        </article>
      </section>
    </div>

    <div v-else class="dialog-shell">
      <div class="dialog-info-card state-shell">
        <h3>暂无可展示数据</h3>
        <p>请选择一条鸡尾酒记录后再查看详情。</p>
        <div class="dialog-actions">
          <button class="button-secondary" type="button" @click="emit('close')">关闭</button>
        </div>
      </div>
    </div>
  </AdminDialog>
</template>

<style scoped>
.detail-dialog {
  gap: 18px;
}

.state-shell,
.hero-meta-card,
.stack-list,
.chip-list {
  display: grid;
  gap: 12px;
}

.state-shell h3,
.state-shell p,
.detail-copy {
  margin: 0;
}

.detail-hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(260px, 1fr);
  gap: 16px;
}

.hero-image-shell {
  min-height: 320px;
  padding: 14px;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at 12% 8%, rgba(200, 155, 91, 0.22), transparent 34%),
    linear-gradient(140deg, rgba(74, 47, 42, 0.2), rgba(58, 38, 34, 0.34));
}

.hero-image-shell img {
  width: 100%;
  max-height: 360px;
  object-fit: contain;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.94);
}

.empty-image {
  width: 100%;
  height: 300px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  color: var(--ink-600);
  border: 1px dashed rgba(74, 47, 42, 0.3);
  background: rgba(255, 255, 255, 0.66);
}

.break-all {
  word-break: break-all;
}

.detail-copy,
.stack-list li span,
.stack-list li p,
.empty-text {
  color: var(--ink-600);
}

.stack-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.stack-list li {
  border-radius: 14px;
  padding: 12px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.72);
  display: grid;
  gap: 4px;
}

.stack-list li strong {
  color: var(--ink-950);
}

.legacy-step {
  white-space: pre-wrap;
}

@media (max-width: 980px) {
  .detail-hero-grid {
    grid-template-columns: 1fr;
  }
}
</style>
