<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
    description?: string
  }>(),
  {
    title: '',
    description: '',
  },
)
</script>

<template>
  <section class="admin-workspace-panel">
    <div v-if="title || description || $slots.head" class="admin-workspace-panel__head">
      <slot name="head">
        <h3 v-if="title" class="admin-workspace-panel__title">{{ title }}</h3>
        <p v-if="description" class="admin-workspace-panel__description">{{ description }}</p>
      </slot>
    </div>

    <div class="admin-workspace-panel__body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.admin-workspace-panel {
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.56);
  border: 1px solid rgba(255, 255, 255, 0.54);
  display: grid;
  gap: 12px;
}

.admin-workspace-panel__head {
  display: grid;
  gap: 4px;
}

.admin-workspace-panel__title {
  margin: 0;
  font-size: 1.16rem;
  line-height: 1.08;
}

.admin-workspace-panel__description {
  margin: 0;
  color: var(--ink-600);
  font-size: 0.92rem;
}

.admin-workspace-panel__body {
  display: grid;
  gap: 12px;
}
</style>
