<script setup lang="ts">
defineProps<{
  eyebrow?: string
  title: string
  subtitle?: string
}>()
</script>

<template>
  <header class="admin-page-header">
    <div class="admin-page-header__copy">
      <p v-if="eyebrow" class="admin-page-header__eyebrow">{{ eyebrow }}</p>
      <h1 class="admin-page-header__title">{{ title }}</h1>
      <p v-if="subtitle" class="admin-page-header__subtitle">{{ subtitle }}</p>
    </div>

    <div v-if="$slots.meta || $slots.actions" class="admin-page-header__aside">
      <div v-if="$slots.meta" class="admin-page-header__meta">
        <slot name="meta" />
      </div>
      <div v-if="$slots.actions" class="admin-page-header__actions">
        <slot name="actions" />
      </div>
    </div>
  </header>
</template>

<style scoped>
.admin-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.admin-page-header__copy {
  display: grid;
  gap: 10px;
  max-width: 760px;
}

.admin-page-header__eyebrow {
  margin: 0;
  font-size: 0.74rem;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--ink-600);
}

.admin-page-header__title {
  margin: 0;
  font-size: clamp(2.2rem, 4vw, 3.6rem);
  line-height: 0.94;
  letter-spacing: -0.05em;
}

.admin-page-header__subtitle {
  margin: 0;
  max-width: 680px;
  color: var(--ink-600);
  font-size: 1rem;
}

.admin-page-header__aside {
  display: grid;
  gap: 12px;
  justify-items: end;
  min-width: max-content;
}

.admin-page-header__meta,
.admin-page-header__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

@media (max-width: 960px) {
  .admin-page-header {
    flex-direction: column;
  }

  .admin-page-header__aside {
    justify-items: start;
    min-width: 0;
  }

  .admin-page-header__meta,
  .admin-page-header__actions {
    justify-content: flex-start;
  }
}
</style>
