<script setup lang="ts">
interface SummaryItem {
  label: string
  value: string | number | null | undefined
}

defineProps<{
  items: SummaryItem[]
}>()
</script>

<template>
  <dl class="admin-summary-list">
    <div v-for="item in items" :key="item.label" class="admin-summary-list__row">
      <dt>{{ item.label }}</dt>
      <dd>{{ item.value ?? '-' }}</dd>
    </div>
  </dl>
</template>

<style scoped>
.admin-summary-list {
  display: grid;
  gap: 12px;
}

.admin-summary-list__row {
  display: grid;
  gap: 2px;
}

.admin-summary-list dt {
  color: var(--ink-600);
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.admin-summary-list dd {
  margin: 0;
}
</style>
