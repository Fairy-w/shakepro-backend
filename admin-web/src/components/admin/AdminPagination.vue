<script setup lang="ts">
defineProps<{
  page: number
  totalPages: number
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'prev'): void
  (e: 'next'): void
}>()
</script>

<template>
  <div class="admin-pagination card">
    <button class="button-secondary" :disabled="page <= 0 || loading" @click="emit('prev')">上一页</button>
    <span>第 {{ page + 1 }} / {{ Math.max(totalPages, 1) }} 页</span>
    <button class="button-secondary" :disabled="page + 1 >= totalPages || loading || !totalPages" @click="emit('next')">
      下一页
    </button>
  </div>
</template>

<style scoped>
.admin-pagination {
  padding: 12px 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.admin-pagination span {
  color: var(--ink-600);
  font-family: var(--font-mono);
  font-size: 0.9rem;
}

@media (max-width: 720px) {
  .admin-pagination {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
