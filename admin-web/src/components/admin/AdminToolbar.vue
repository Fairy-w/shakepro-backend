<template>
  <div class="admin-toolbar card">
    <slot />
  </div>
</template>

<style scoped>
.admin-toolbar {
  padding: 14px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.6), rgba(255, 255, 255, 0.42));
}
</style>
