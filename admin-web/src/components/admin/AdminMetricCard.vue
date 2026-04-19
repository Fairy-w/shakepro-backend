<script setup lang="ts">
withDefaults(
  defineProps<{
    eyebrow?: string
    label: string
    value: string | number
    hint?: string
    tone?: 'default' | 'warm' | 'strong' | 'success'
  }>(),
  {
    eyebrow: '',
    hint: '',
    tone: 'default',
  },
)
</script>

<template>
  <article class="admin-metric-card card" :class="`tone-${tone}`">
    <p v-if="eyebrow" class="admin-metric-card__eyebrow">{{ eyebrow }}</p>
    <span class="admin-metric-card__label">{{ label }}</span>
    <strong class="admin-metric-card__value">{{ value }}</strong>
    <p v-if="hint" class="admin-metric-card__hint">{{ hint }}</p>
    <slot />
  </article>
</template>

<style scoped>
.admin-metric-card {
  position: relative;
  overflow: hidden;
  padding: 22px;
  display: grid;
  gap: 10px;
  min-height: 168px;
}

.admin-metric-card::before {
  content: '';
  position: absolute;
  inset: auto -20% 62% auto;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.72), transparent 64%);
  opacity: 0.6;
  pointer-events: none;
}

.admin-metric-card__eyebrow,
.admin-metric-card__label,
.admin-metric-card__hint {
  margin: 0;
}

.admin-metric-card__eyebrow {
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--ink-600);
}

.admin-metric-card__label {
  color: var(--ink-600);
  font-weight: 600;
}

.admin-metric-card__value {
  font-family: var(--font-mono);
  font-size: clamp(2rem, 4vw, 3.1rem);
  line-height: 0.9;
  letter-spacing: -0.06em;
  color: var(--ink-950);
}

.admin-metric-card__hint {
  color: var(--ink-600);
  font-size: 0.92rem;
  max-width: 28ch;
}

.tone-default {
  background:
    radial-gradient(circle at 100% 0%, rgba(200, 155, 91, 0.12), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(248, 242, 235, 0.78));
}

.tone-warm {
  background:
    radial-gradient(circle at 100% 0%, rgba(122, 73, 63, 0.18), transparent 38%),
    linear-gradient(180deg, rgba(255, 252, 248, 0.88), rgba(246, 235, 227, 0.84));
}

.tone-strong {
  color: #fff;
  background:
    radial-gradient(circle at 10% 15%, rgba(200, 155, 91, 0.32), transparent 28%),
    linear-gradient(160deg, rgba(74, 47, 42, 0.98), rgba(58, 38, 34, 0.96));
}

.tone-strong .admin-metric-card__eyebrow,
.tone-strong .admin-metric-card__label,
.tone-strong .admin-metric-card__hint,
.tone-strong .admin-metric-card__value {
  color: #fff;
}

.tone-success {
  background:
    radial-gradient(circle at 100% 0%, rgba(47, 125, 104, 0.18), transparent 34%),
    linear-gradient(180deg, rgba(249, 255, 252, 0.88), rgba(237, 246, 242, 0.84));
}
</style>
