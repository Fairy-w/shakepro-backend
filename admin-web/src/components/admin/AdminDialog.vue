<script setup lang="ts">
withDefaults(
  defineProps<{
    visible: boolean
    size?: 'md' | 'lg' | 'xl'
  }>(),
  {
    size: 'lg',
  },
)

const emit = defineEmits<{
  (e: 'close'): void
}>()
</script>

<template>
  <div v-if="visible" class="modal-backdrop" @click.self="emit('close')">
    <div class="modal-panel admin-dialog" :class="`size-${size}`">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.admin-dialog {
  width: min(980px, 100%);
  background:
    radial-gradient(circle at 100% 0%, rgba(200, 155, 91, 0.14), transparent 26%),
    radial-gradient(circle at 0% 100%, rgba(122, 73, 63, 0.12), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.86), rgba(252, 246, 240, 0.8));
}

.size-md {
  width: min(720px, 100%);
}

.size-lg {
  width: min(980px, 100%);
}

.size-xl {
  width: min(1180px, 100%);
}
</style>
