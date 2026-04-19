<script setup lang="ts">
withDefaults(
  defineProps<{
    eyebrow?: string
    title: string
    description?: string
  }>(),
  {
    eyebrow: '',
    description: '',
  },
)
</script>

<template>
  <article class="card admin-workspace-card">
    <div class="admin-workspace-card__head">
      <div class="admin-workspace-card__title-wrap">
        <p v-if="eyebrow" class="admin-workspace-card__eyebrow">{{ eyebrow }}</p>
        <h2 class="admin-workspace-card__title">{{ title }}</h2>
        <p v-if="description" class="admin-workspace-card__description">{{ description }}</p>
      </div>

      <div v-if="$slots.action" class="admin-workspace-card__action">
        <slot name="action" />
      </div>
    </div>

    <div class="admin-workspace-card__body">
      <slot />
    </div>
  </article>
</template>

<style scoped>
.admin-workspace-card {
  padding: 20px;
  display: grid;
  gap: 18px;
}

.admin-workspace-card__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.admin-workspace-card__title-wrap {
  display: grid;
  gap: 8px;
}

.admin-workspace-card__eyebrow {
  margin: 0;
  font-size: 0.74rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--ink-600);
}

.admin-workspace-card__title {
  margin: 0;
  font-size: clamp(1.56rem, 2.2vw, 1.82rem);
  line-height: 0.98;
}

.admin-workspace-card__description {
  margin: 0;
  max-width: 66ch;
  color: var(--ink-600);
}

.admin-workspace-card__action {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.admin-workspace-card__body {
  display: grid;
  gap: 16px;
}

@media (max-width: 1080px) {
  .admin-workspace-card__head {
    flex-direction: column;
    align-items: stretch;
  }

  .admin-workspace-card__action {
    justify-content: flex-start;
  }
}
</style>
