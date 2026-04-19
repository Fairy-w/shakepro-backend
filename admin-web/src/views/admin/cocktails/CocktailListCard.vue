<script setup lang="ts">
import { computed } from 'vue'
import type { AdminCocktailListItem } from '@/api/admin'

const props = defineProps<{
  item: AdminCocktailListItem
}>()

const emit = defineEmits<{
  (e: 'view', item: AdminCocktailListItem): void
  (e: 'edit', item: AdminCocktailListItem): void
  (e: 'remove', item: AdminCocktailListItem): void
}>()

const imageUrl = computed(() => props.item.heroImage || props.item.imageUrl || '')
const abvLabel = computed(() => props.item.abv || (props.item.alcoholLevel != null ? `${props.item.alcoholLevel}%` : '未标记'))
</script>

<template>
  <article class="cocktail-card card">
    <div class="image-wrap">
      <img v-if="imageUrl" class="cover-image" :src="imageUrl" :alt="item.name" loading="lazy" />
      <div v-else class="cover-empty">
        <span>暂无图片</span>
      </div>
      <div class="cover-meta">
        <span class="badge subtle mono">#{{ item.id }}</span>
        <span class="abv-pill">{{ abvLabel }}</span>
      </div>
    </div>

    <div class="body">
      <div class="title-row">
        <h3>{{ item.name }}</h3>
        <button class="ghost-link" type="button" @click="emit('view', item)">查看详情</button>
      </div>
      <p v-if="item.englishName" class="english-name">{{ item.englishName }}</p>
      <p v-if="item.category || item.difficulty" class="meta-line">
        <span v-if="item.category">{{ item.category }}</span>
        <span v-if="item.category && item.difficulty"> · </span>
        <span v-if="item.difficulty">{{ item.difficulty }}</span>
      </p>
      <p class="created-line">更新于 {{ item.updatedAt?.replace('T', ' ') || '-' }}</p>
      <div class="actions">
        <button class="button-secondary" type="button" @click="emit('edit', item)">编辑</button>
        <button class="button-danger" type="button" @click="emit('remove', item)">删除</button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.cocktail-card {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  overflow: hidden;
  transition: transform 0.24s ease, box-shadow 0.24s ease;
}

.cocktail-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
}

.image-wrap {
  position: relative;
  min-height: 100%;
  padding: 14px;
  display: grid;
  align-items: center;
  background:
    radial-gradient(circle at 10% 10%, rgba(200, 155, 91, 0.16), transparent 30%),
    radial-gradient(circle at 100% 0%, rgba(122, 73, 63, 0.16), transparent 36%),
    linear-gradient(145deg, rgba(74, 47, 42, 0.96), rgba(58, 38, 34, 0.94));
}

.cover-image {
  width: 100%;
  height: 100%;
  min-height: 220px;
  max-height: 240px;
  object-fit: cover;
  border-radius: 18px;
  background: rgba(255, 252, 248, 0.96);
}

.cover-empty {
  width: 100%;
  height: 100%;
  min-height: 220px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  color: rgba(255, 247, 242, 0.74);
  font-weight: 700;
  border: 1px dashed rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.08);
}

.cover-meta {
  position: absolute;
  inset: 14px 14px auto 14px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
  pointer-events: none;
}

.abv-pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  border-radius: 13px;
  padding: 5px 11px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.2), rgba(255, 255, 255, 0.12));
  border: 1px solid rgba(255, 255, 255, 0.14);
  color: #fff8f0;
  font-weight: 700;
  font-size: 0.76rem;
  letter-spacing: 0.04em;
  backdrop-filter: blur(10px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.24);
}

.body {
  padding: 18px;
  display: grid;
  align-content: center;
  gap: 10px;
}

.title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
}

.title-row h3 {
  margin: 0;
  font-size: 1.55rem;
  line-height: 1;
}

.ghost-link {
  border: none;
  padding: 0;
  background: transparent;
  color: var(--primary);
  font-weight: 700;
  cursor: pointer;
}

.ghost-link:hover {
  text-decoration: underline;
}

.english-name {
  margin: 8px 0 0;
  color: var(--ink-500);
  font-size: 0.92rem;
}

.meta-line,
.created-line {
  margin: 0;
  color: var(--ink-600);
}

.created-line {
  font-family: var(--font-mono);
  font-size: 0.84rem;
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 6px;
}

@media (max-width: 900px) {
  .cocktail-card {
    grid-template-columns: 1fr;
  }

  .image-wrap {
    min-height: 268px;
  }

  .cover-image,
  .cover-empty {
    height: 238px;
    max-height: none;
  }
}
</style>
