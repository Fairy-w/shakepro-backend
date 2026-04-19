<script setup lang="ts">
import { computed } from 'vue'
import type { AdminMaterial } from '@/api/admin'
import type { GeneratedEditorFormSnapshot } from '../cocktailPayload'
import AdminDialog from '@/components/admin/AdminDialog.vue'

const props = defineProps<{
  visible: boolean
  editing: boolean
  form: GeneratedEditorFormSnapshot
  materialOptions: AdminMaterial[]
  submitting: boolean
  uploadingHeroImage: boolean
  uploadImageError: string
  uploadImageSuccess: string
  submitError: string
  formCompletion: number
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'submit'): void
  (e: 'upload-hero-image', file: File): void
}>()

const heroPreviewStyle = computed(() => ({
  backgroundImage: props.form.heroImage.trim() ? `url(${props.form.heroImage.trim()})` : undefined,
}))

function addMaterialRow() {
  props.form.ingredients.push({ materialId: '', name: '', amount: '', note: '' })
}

function removeMaterialRow(index: number) {
  if (props.form.ingredients.length === 1) return
  props.form.ingredients.splice(index, 1)
}

function addStepRow() {
  props.form.steps.push({ title: '', detail: '' })
}

function removeStepRow(index: number) {
  if (props.form.steps.length === 1) return
  props.form.steps.splice(index, 1)
}

function addMetricRow() {
  props.form.flavorMetrics.push({ name: '', value: '' })
}

function removeMetricRow(index: number) {
  if (props.form.flavorMetrics.length === 1) return
  props.form.flavorMetrics.splice(index, 1)
}

function onIngredientMaterialChange(index: number) {
  const row = props.form.ingredients[index]
  if (!row) return
  const selected = props.materialOptions.find((item) => item.id === Number(row.materialId))
  if (selected && !row.name.trim()) {
    row.name = selected.name
  }
}

function onHeroImageSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  emit('upload-hero-image', file)
  input.value = ''
}
</script>

<template>
  <AdminDialog :visible="visible" size="xl" @close="emit('close')">
    <div class="dialog-shell with-aside cocktail-dialog">
      <div class="dialog-main">
        <header class="dialog-head">
          <div>
            <p class="dialog-head__eyebrow">Cocktail Editor</p>
            <h2 class="dialog-head__title">{{ editing ? '编辑鸡尾酒' : '新建鸡尾酒' }}</h2>
            <p class="dialog-head__subtitle">按“基础信息 → 风味内容 → 材料步骤”的顺序填写，整个编辑体验会更稳定。</p>
          </div>
          <div class="dialog-head__meta">
            <span class="badge">完成度 {{ formCompletion }}%</span>
          </div>
        </header>

        <section class="dialog-section">
          <div class="dialog-section__head">
            <h3>基础信息</h3>
            <p>用于列表展示、详情首屏和品牌感预览。</p>
          </div>
          <div class="dialog-form-grid">
            <label>
              <span>名称</span>
              <input v-model="form.name" class="field" type="text" placeholder="例如：三款朗姆戴克利" />
            </label>
            <label>
              <span>英文名</span>
              <input v-model="form.englishName" class="field" type="text" placeholder="Three Rum Daiquiri" />
            </label>
            <label>
              <span>分类</span>
              <input v-model="form.category" class="field" type="text" placeholder="例如：经典鸡尾酒" />
            </label>
            <label>
              <span>难度</span>
              <input v-model="form.difficulty" class="field" type="text" placeholder="例如：简单" />
            </label>
            <label>
              <span>ABV</span>
              <input v-model="form.abv" class="field" type="text" placeholder="例如：22%" />
            </label>
            <label>
              <span>杯型</span>
              <input v-model="form.glass" class="field" type="text" placeholder="例如：马天尼杯" />
            </label>
            <label>
              <span>装饰</span>
              <input v-model="form.garnish" class="field" type="text" placeholder="例如：青柠角" />
            </label>
            <label class="span-two">
              <span>主图地址</span>
              <input v-model="form.heroImage" class="field" type="text" placeholder="https://..." />
              <div class="hero-upload-row">
                <label class="button-secondary upload-file-button">
                  <input
                    class="upload-file-input"
                    type="file"
                    accept="image/jpeg,image/png,image/gif,image/webp,image/svg+xml"
                    :disabled="submitting || uploadingHeroImage"
                    @change="onHeroImageSelected"
                  />
                  {{ uploadingHeroImage ? '上传中...' : '选择图片并上传到 OSS' }}
                </label>
                <span class="upload-hint">支持 jpg/png/gif/webp/svg，最大 10MB</span>
              </div>
              <p v-if="uploadImageError" class="upload-error">{{ uploadImageError }}</p>
              <p v-else-if="uploadImageSuccess" class="upload-success">{{ uploadImageSuccess }}</p>
            </label>
            <label class="span-two">
              <span>亮点文案</span>
              <textarea v-model="form.highlight" class="textarea" rows="2" placeholder="一句话概括这款酒的独特卖点"></textarea>
            </label>
            <label class="span-two">
              <span>副标题</span>
              <textarea v-model="form.subtitle" class="textarea" rows="2" placeholder="适合卡片或详情页的短介绍"></textarea>
            </label>
            <label class="span-two">
              <span>描述</span>
              <textarea v-model="form.description" class="textarea" rows="3" placeholder="描述风味、口感与整体印象"></textarea>
            </label>
            <label class="span-two">
              <span>故事</span>
              <textarea v-model="form.story" class="textarea" rows="4" placeholder="记录这款酒的来历、背景或创作灵感"></textarea>
            </label>
          </div>
        </section>

        <section class="dialog-section">
          <div class="dialog-section__head">
            <h3>风味与服务信息</h3>
            <p>补全标签、指标和搭配建议，给详情页和推荐逻辑更多语义支撑。</p>
          </div>
          <div class="dialog-form-grid">
            <label>
              <span>风味标签</span>
              <textarea v-model="form.flavorTagsText" class="textarea" rows="4" placeholder="一行一个，例如：清爽&#10;柑橘&#10;酸甜平衡"></textarea>
            </label>
            <label>
              <span>搭配建议</span>
              <textarea v-model="form.pairingsText" class="textarea" rows="4" placeholder="一行一个，例如：海鲜开胃菜"></textarea>
            </label>
            <label class="span-two">
              <span>服务备注</span>
              <textarea v-model="form.serviceNotesText" class="textarea" rows="4" placeholder="一行一个，例如：建议使用大块透明冰，入口更干净"></textarea>
            </label>
          </div>

          <div class="section-headline">
            <h4>风味指标</h4>
            <button class="button-secondary" type="button" @click="addMetricRow">添加一行</button>
          </div>
          <div v-for="(item, index) in form.flavorMetrics" :key="`metric-${index}`" class="metric-row">
            <input v-model="item.name" class="field" type="text" placeholder="例如：酸感" />
            <input v-model="item.value" class="field" type="number" min="0" max="5" placeholder="0-5" />
            <button class="button-danger mini" type="button" @click="removeMetricRow(index)">删除</button>
          </div>
        </section>

        <section class="dialog-section">
          <div class="dialog-section__head">
            <h3>材料与步骤</h3>
            <p>材料建议保持“名称 + 用量”，步骤建议一句一意，便于后续展示和抓取兼容。</p>
          </div>

          <div class="section-headline">
            <h4>材料明细</h4>
            <button class="button-secondary" type="button" @click="addMaterialRow">添加一行</button>
          </div>
          <div v-for="(item, index) in form.ingredients" :key="index" class="ingredient-card">
            <div class="ingredient-grid">
              <select v-model="item.materialId" class="select" @change="onIngredientMaterialChange(index)">
                <option value="">不关联，手动填写名称</option>
                <option v-for="material in materialOptions" :key="material.id" :value="material.id">
                  {{ material.name }}<template v-if="material.category"> / {{ material.category }}</template>
                </option>
              </select>
              <input v-model="item.name" class="field" type="text" placeholder="例如：深色朗姆酒" />
              <input v-model="item.amount" class="field" type="text" placeholder="例如：45ml" />
              <textarea v-model="item.note" class="textarea span-two" rows="2" placeholder="材料备注，例如：建议使用香气更圆润的陈年款"></textarea>
            </div>
            <button class="button-danger mini" type="button" @click="removeMaterialRow(index)">删除</button>
          </div>

          <div class="section-headline">
            <h4>制作步骤</h4>
            <button class="button-secondary" type="button" @click="addStepRow">添加一步</button>
          </div>
          <div v-for="(item, index) in form.steps" :key="`step-${index}`" class="step-card">
            <div class="step-index">步骤 {{ index + 1 }}</div>
            <input v-model="item.title" class="field" type="text" placeholder="例如：摇和冷却" />
            <textarea v-model="item.detail" class="textarea" rows="3" placeholder="详细写明操作方式、时长和关键注意事项"></textarea>
            <button class="button-danger mini" type="button" @click="removeStepRow(index)">删除</button>
          </div>
        </section>

        <p v-if="submitError" class="submit-error">{{ submitError }}</p>

        <div class="dialog-sticky-footer">
          <div class="dialog-footer">
            <button class="button-secondary" type="button" @click="emit('close')">取消</button>
            <button class="button-primary" type="button" :disabled="submitting || uploadingHeroImage" @click="emit('submit')">
              {{ submitting ? '保存中...' : uploadingHeroImage ? '图片上传中...' : '保存到鸡尾酒库' }}
            </button>
          </div>
        </div>
      </div>

      <aside class="dialog-aside">
        <article class="dialog-preview-card preview-card">
          <div class="preview-cover" :style="heroPreviewStyle">
            <span class="badge subtle">{{ form.category || '待填写分类' }}</span>
          </div>
          <div class="preview-body">
            <h3>{{ form.name || '鸡尾酒名称预览' }}</h3>
            <p class="english-name">{{ form.englishName || 'English name preview' }}</p>
            <p class="preview-text">{{ form.description || '填写描述后会在这里显示预览文案。' }}</p>
            <div class="preview-meta">
              <span>{{ form.abv || 'ABV --' }}</span>
              <span>{{ form.difficulty || '难度 --' }}</span>
              <span>{{ form.glass || '杯型 --' }}</span>
            </div>
          </div>
        </article>
      </aside>
    </div>
  </AdminDialog>
</template>

<style scoped>
.cocktail-dialog {
  gap: 20px;
}

.hero-upload-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.upload-file-button {
  position: relative;
  cursor: pointer;
}

.upload-file-input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.upload-hint {
  font-size: 0.82rem;
  color: var(--ink-600);
}

.upload-error,
.upload-success,
.submit-error {
  margin: 0;
  font-size: 0.86rem;
  font-weight: 700;
}

.upload-error,
.submit-error {
  color: var(--danger);
}

.upload-success {
  color: var(--success);
}

.section-headline {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.section-headline h4 {
  margin: 0;
}

.metric-row {
  display: grid;
  grid-template-columns: 1.3fr 0.8fr auto;
  gap: 12px;
}

.ingredient-card,
.step-card {
  border-radius: 16px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.58);
  padding: 14px;
  display: grid;
  gap: 12px;
}

.ingredient-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.step-index {
  color: var(--ink-600);
  font-weight: 700;
  font-size: 0.9rem;
}

.mini {
  padding-inline: 14px;
}

.preview-card {
  position: sticky;
  top: 0;
}

.preview-cover {
  min-height: 210px;
  padding: 14px;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  background:
    linear-gradient(145deg, rgba(74, 47, 42, 0.6), rgba(58, 38, 34, 0.72)),
    linear-gradient(145deg, rgba(200, 155, 91, 0.35), transparent);
  background-size: cover;
  background-position: center;
}

.preview-body {
  padding: 16px;
  display: grid;
  gap: 10px;
}

.preview-body h3,
.english-name,
.preview-text {
  margin: 0;
}

.english-name {
  color: var(--ink-500);
  font-size: 0.9rem;
}

.preview-text {
  color: var(--ink-600);
  line-height: 1.5;
  font-size: 0.92rem;
}

.preview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-meta span {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 6px 10px;
  border: 1px solid rgba(74, 47, 42, 0.1);
  background: rgba(255, 255, 255, 0.75);
  font-size: 0.82rem;
  color: var(--ink-800);
}

@media (max-width: 1120px) {
  .preview-card {
    position: relative;
  }
}

@media (max-width: 960px) {
  .ingredient-grid,
  .metric-row {
    grid-template-columns: 1fr;
  }

  .section-headline {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
