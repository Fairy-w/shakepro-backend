<script setup lang="ts">
withDefaults(
  defineProps<{
    eyebrow?: string
    title: string
    tone?: 'default' | 'warm' | 'dark'
  }>(),
  {
    eyebrow: '',
    tone: 'default',
  },
)
</script>

<template>
  <section class="admin-inspector-panel" :class="`tone-${tone}`">
    <div class="admin-inspector-panel__head">
      <p v-if="eyebrow" class="admin-inspector-panel__eyebrow">{{ eyebrow }}</p>
      <h3 class="admin-inspector-panel__title">{{ title }}</h3>
    </div>
    <div class="admin-inspector-panel__body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.admin-inspector-panel {
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.52);
  display: grid;
  gap: 14px;
}

.admin-inspector-panel__eyebrow {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--ink-600);
}

.admin-inspector-panel__title {
  margin: 8px 0 0;
  font-size: 1.3rem;
  line-height: 1;
}

.admin-inspector-panel__body {
  display: grid;
  gap: 12px;
}

.tone-default {
  background: rgba(255, 255, 255, 0.5);
}

.tone-warm {
  background:
    radial-gradient(circle at 100% 0%, rgba(200, 155, 91, 0.14), transparent 30%),
    rgba(255, 251, 247, 0.6);
}

.tone-dark {
  color: #fff8f0;
  background: linear-gradient(160deg, rgba(74, 47, 42, 0.98), rgba(58, 38, 34, 0.96));
}

.tone-dark .admin-inspector-panel__eyebrow,
.tone-dark .admin-inspector-panel__title {
  color: #fff8f0;
}
</style>
