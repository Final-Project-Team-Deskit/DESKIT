<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '../../components/PageContainer.vue'
import PageHeader from '../../components/PageHeader.vue'
import {
  clearProductDraft,
  loadProductDraft,
  saveProductDraft,
  upsertProduct,
  type SellerProductDraft,
} from '../../composables/useSellerProducts'
import { getAuthUser } from '../../lib/auth'

const router = useRouter()

const editorRef = ref<HTMLDivElement | null>(null)
const detailHtml = ref('')
const error = ref('')
const draft = ref<SellerProductDraft | null>(null)

const deriveSellerId = () => {
  const user = getAuthUser() as any
  const candidates = [user?.seller_id, user?.sellerId, user?.id, user?.user_id, user?.userId]
  for (const value of candidates) {
    if (typeof value === 'number' && Number.isFinite(value)) return value
    if (typeof value === 'string') {
      const parsed = Number.parseInt(value, 10)
      if (!Number.isNaN(parsed)) return parsed
    }
  }
  return null
}

const exec = (command: string, value?: string) => {
  document.execCommand(command, false, value)
  syncFromEditorSoon()
}

const addLink = () => {
  const url = window.prompt('링크 주소를 입력하세요.')
  if (!url) return
  exec('createLink', url)
}

const addImage = () => {
  const url = window.prompt('이미지 주소(URL)를 입력하세요.')
  if (!url) return
  exec('insertImage', url)
}

const formatBlock = (tag: 'p' | 'h1' | 'h2' | 'h3') => {
  exec('formatBlock', `<${tag}>`)
}

const saveDraftOnly = () => {
  if (!draft.value) return
  saveProductDraft({
    ...draft.value,
    detailHtml: detailHtml.value,
  })
}

const syncFromEditor = () => {
  detailHtml.value = editorRef.value?.innerHTML ?? ''
}

const syncFromEditorSoon = () => {
  // execCommand 직후에 DOM 반영 타이밍이 있어서 1tick 뒤에 동기화
  window.setTimeout(() => {
    syncFromEditor()
    saveDraftOnly()
  }, 0)
}

const handleInput = () => {
  syncFromEditor()
  saveDraftOnly()
}

const goBack = () => {
  saveDraftOnly()
  router.push('/seller/products/create').catch(() => {})
}

const handleSubmit = () => {
  error.value = ''
  if (!draft.value) {
    error.value = '기본 정보를 먼저 입력해주세요.'
    return
  }

  const name = (draft.value.name || '').trim()
  const shortDesc = (draft.value.shortDesc || '').trim()

  const costPrice = Number(draft.value.costPrice)
  const price = Number(draft.value.price)
  const stock = Number(draft.value.stock)

  if (!name) {
    error.value = '상품명을 입력해주세요.'
    return
  }
  if (!Number.isFinite(costPrice) || costPrice < 0 || !Number.isFinite(price) || price < 0) {
    error.value = '원가/판매가를 올바르게 입력해주세요.'
    return
  }
  if (!Number.isFinite(stock) || stock < 0) {
    error.value = '재고를 올바르게 입력해주세요.'
    return
  }

  const sellerId = draft.value.sellerId ?? deriveSellerId()
  if (!sellerId) {
    error.value = '판매자 정보를 확인할 수 없습니다.'
    return
  }

  // 마지막 동기화
  syncFromEditor()
  const now = new Date().toISOString()
  const id = draft.value.id || `new-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`

  upsertProduct({
    id,
    sellerId,
    name,
    shortDesc,
    costPrice,
    price,
    stock,
    images: Array.isArray(draft.value.images) ? draft.value.images : [],
    detailHtml: detailHtml.value,
    createdAt: now,
    updatedAt: now,
  })

  clearProductDraft()
  router.push('/seller/products').catch(() => {})
}

onMounted(() => {
  const loaded = loadProductDraft()
  if (!loaded) {
    draft.value = null
    error.value = '기본 정보를 먼저 입력해주세요.'
    return
  }
  draft.value = loaded
  detailHtml.value = loaded.detailHtml || ''

  // contenteditable 초기 값 주입
  window.setTimeout(() => {
    if (!editorRef.value) return
    editorRef.value.innerHTML = detailHtml.value || ''
  }, 0)
})
</script>

<template>
  <PageContainer>
    <PageHeader eyebrow="DESKIT" title="상품 등록 - 상세정보 작성" />

    <section class="detail-surface ds-surface">
      <div class="toolbar" role="toolbar" aria-label="상세 설명 편집 툴바">
        <div class="toolbar__group">
          <button type="button" class="tool-btn" @click="exec('bold')" aria-label="굵게"><span aria-hidden="true">B</span></button>
          <button type="button" class="tool-btn" @click="exec('italic')" aria-label="기울임"><span aria-hidden="true">I</span></button>
          <button type="button" class="tool-btn" @click="exec('underline')" aria-label="밑줄"><span aria-hidden="true">U</span></button>
          <button type="button" class="tool-btn" @click="exec('removeFormat')" aria-label="서식 제거">서식</button>
        </div>

        <div class="toolbar__group">
          <button type="button" class="tool-btn" @click="formatBlock('h1')">H1</button>
          <button type="button" class="tool-btn" @click="formatBlock('h2')">H2</button>
          <button type="button" class="tool-btn" @click="formatBlock('h3')">H3</button>
          <button type="button" class="tool-btn" @click="formatBlock('p')">P</button>
        </div>

        <div class="toolbar__group">
          <button type="button" class="tool-btn" @click="exec('insertUnorderedList')">• 리스트</button>
          <button type="button" class="tool-btn" @click="exec('insertOrderedList')">1. 리스트</button>
        </div>

        <div class="toolbar__group">
          <button type="button" class="tool-btn" @click="addLink">링크</button>
          <button type="button" class="tool-btn" @click="addImage">이미지</button>
        </div>
      </div>

      <div class="editor-wrap">
        <div
          ref="editorRef"
          class="editor"
          contenteditable="true"
          spellcheck="false"
          aria-label="상세 설명 편집 영역"
          @input="handleInput"
        ></div>

        <p class="hint">* 간단 에디터(목업)입니다. 저장은 로컬스토리지 기반이며, 실제 서버 연동 전까지 임시로 사용합니다.</p>
      </div>

      <p v-if="error" class="error">{{ error }}</p>

      <div class="actions">
        <button type="button" class="btn" @click="goBack">이전</button>
        <button type="button" class="btn primary" @click="handleSubmit">생성</button>
      </div>
    </section>
  </PageContainer>
</template>

<style scoped>
.detail-surface {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  padding: 12px;
  border-radius: 14px;
  background: var(--surface-weak);
  border: 1px solid var(--border-color);
}

.toolbar__group {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}

.tool-btn {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 10px;
  padding: 8px 10px;
  font-weight: 900;
  cursor: pointer;
  line-height: 1;
}

.editor-wrap {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.editor {
  min-height: 320px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  outline: none;
}

.hint {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.85rem;
}

.error {
  margin: 0;
  color: #b91c1c;
  font-weight: 800;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 999px;
  padding: 10px 16px;
  font-weight: 900;
  cursor: pointer;
}

.btn.primary {
  background: var(--text-strong);
  color: var(--surface);
  border-color: transparent;
}
</style>
