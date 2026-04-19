<script setup lang="ts">
import { computed, useSlots } from 'vue'

const slots = useSlots()

const shellClassName = computed(() => ({
  'layout-full': !!slots.sidebar && !!slots.inspector,
  'layout-main-inspector': !slots.sidebar && !!slots.inspector,
  'layout-main-sidebar': !!slots.sidebar && !slots.inspector,
  'layout-main-only': !slots.sidebar && !slots.inspector,
}))
</script>

<template>
  <div class="admin-workspace-shell" :class="shellClassName">
    <aside v-if="$slots.sidebar" class="admin-workspace-shell__sidebar card">
      <slot name="sidebar" />
    </aside>

    <div class="admin-workspace-shell__main">
      <slot name="main" />
    </div>

    <aside v-if="$slots.inspector" class="admin-workspace-shell__inspector card">
      <slot name="inspector" />
    </aside>
  </div>
</template>

<style scoped>
.admin-workspace-shell {
  display: grid;
  gap: 16px;
  align-items: start;
}

.layout-full {
  grid-template-columns: 260px minmax(0, 1fr) 320px;
}

.layout-main-inspector {
  grid-template-columns: minmax(0, 1.72fr) 360px;
}

.layout-main-sidebar {
  grid-template-columns: 260px minmax(0, 1fr);
}

.layout-main-only {
  grid-template-columns: minmax(0, 1fr);
}

.admin-workspace-shell__sidebar,
.admin-workspace-shell__inspector {
  position: sticky;
  top: 0;
  align-self: start;
  padding: 18px;
  display: grid;
  gap: 14px;
}

.admin-workspace-shell__sidebar {
  background:
    radial-gradient(circle at 0% 0%, rgba(200, 155, 91, 0.12), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.76), rgba(248, 240, 233, 0.66));
}

.admin-workspace-shell__inspector {
  background:
    radial-gradient(circle at 100% 0%, rgba(122, 73, 63, 0.12), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.74), rgba(249, 243, 238, 0.68));
}

.admin-workspace-shell__main {
  min-width: 0;
  display: grid;
  gap: 16px;
}

@media (max-width: 1360px) {
  .layout-full,
  .layout-main-sidebar {
    grid-template-columns: 240px minmax(0, 1fr);
  }

  .layout-main-inspector,
  .layout-main-only {
    grid-template-columns: minmax(0, 1fr);
  }

  .admin-workspace-shell__inspector {
    grid-column: 1 / -1;
    position: static;
  }
}

@media (max-width: 960px) {
  .admin-workspace-shell {
    grid-template-columns: 1fr;
  }

  .admin-workspace-shell__sidebar,
  .admin-workspace-shell__inspector {
    position: static;
  }
}
</style>
