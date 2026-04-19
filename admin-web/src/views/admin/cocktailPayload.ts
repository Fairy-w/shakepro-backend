import type { AdminExtractedFieldsResult, GeneratedCocktailPayload } from '@/api/admin'

export interface GeneratedEditorMetricInput {
  name: string
  value: string | number
}

export interface GeneratedEditorIngredientInput {
  materialId: string | number
  name: string
  amount: string
  note: string
}

export interface GeneratedEditorStepInput {
  title: string
  detail: string
}

export interface GeneratedEditorFormSnapshot {
  name: string
  englishName: string
  category: string
  heroImage: string
  difficulty: string
  abv: string
  glass: string
  garnish: string
  highlight: string
  subtitle: string
  description: string
  story: string
  flavorTagsText: string
  pairingsText: string
  serviceNotesText: string
  flavorMetrics: GeneratedEditorMetricInput[]
  ingredients: GeneratedEditorIngredientInput[]
  steps: GeneratedEditorStepInput[]
}

export function normalizeMultiLineText(value: string): string[] {
  return value
    .split(/\r?\n|[，,]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

export function normalizeList(values?: string[] | null): string[] {
  return (values || []).map((item) => item.trim()).filter(Boolean)
}

export function buildGeneratedPayloadFromForm(form: GeneratedEditorFormSnapshot): GeneratedCocktailPayload {
  return {
    name: form.name.trim(),
    englishName: form.englishName.trim() || undefined,
    category: form.category.trim() || undefined,
    heroImage: form.heroImage.trim() || undefined,
    difficulty: form.difficulty.trim() || undefined,
    abv: form.abv.trim() || undefined,
    glass: form.glass.trim() || undefined,
    garnish: form.garnish.trim() || undefined,
    highlight: form.highlight.trim() || undefined,
    subtitle: form.subtitle.trim() || undefined,
    description: form.description.trim() || undefined,
    story: form.story.trim() || undefined,
    flavorTags: normalizeMultiLineText(form.flavorTagsText),
    flavorMetrics: form.flavorMetrics
      .map((item) => ({ name: item.name.trim(), value: Number(item.value) }))
      .filter((item) => item.name && !Number.isNaN(item.value)),
    pairings: normalizeMultiLineText(form.pairingsText),
    serviceNotes: normalizeMultiLineText(form.serviceNotesText),
    ingredients: form.ingredients
      .map((item) => ({
        materialId: item.materialId ? Number(item.materialId) : undefined,
        name: item.name.trim(),
        amount: item.amount.trim() || undefined,
        note: item.note.trim() || undefined,
      }))
      .filter((item) => item.name),
    steps: form.steps
      .map((item) => ({ title: item.title.trim() || undefined, detail: item.detail.trim() }))
      .filter((item) => item.detail),
  }
}

export function buildGeneratedPayloadFromExtracted(result: AdminExtractedFieldsResult): GeneratedCocktailPayload {
  return {
    name: result.name?.trim() || '',
    englishName: result.englishName?.trim() || undefined,
    category: result.category?.trim() || undefined,
    heroImage: result.heroImage?.trim() || undefined,
    difficulty: result.difficulty?.trim() || undefined,
    abv: result.abv?.trim() || undefined,
    glass: result.glass?.trim() || undefined,
    garnish: result.garnish?.trim() || undefined,
    highlight: result.highlight?.trim() || undefined,
    subtitle: result.subtitle?.trim() || undefined,
    description: result.description?.trim() || undefined,
    story: result.story?.trim() || undefined,
    flavorTags: normalizeList(result.flavorTags),
    flavorMetrics: Object.entries(result.flavorMetrics || {}).map(([name, value]) => ({ name, value })),
    pairings: normalizeList(result.pairings),
    serviceNotes: normalizeList(result.serviceNotes),
    ingredients: (result.ingredients || [])
      .map((item) => ({
        name: item.name?.trim() || '',
        amount: item.amount?.trim() || undefined,
        note: item.note?.trim() || undefined,
      }))
      .filter((item) => item.name),
    steps: (result.steps || [])
      .map((item) => ({
        title: item.title?.trim() || undefined,
        detail: item.detail?.trim() || '',
      }))
      .filter((item) => item.detail),
  }
}
