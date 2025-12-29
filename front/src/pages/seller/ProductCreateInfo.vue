<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import PageContainer from '../../components/PageContainer.vue'
import PageHeader from '../../components/PageHeader.vue'
import {getAuthUser} from '../../lib/auth'
import {clearProductDraft, loadProductDraft, saveProductDraft} from '../../composables/useSellerProducts'

const route = useRoute()
const router = useRouter()

const name = ref('')
const shortDesc = ref('')
const costPrice = ref(0)
const price = ref(0)
const stock = ref(0)
const images = ref<string[]>(['', '', '', '', ''])
const error = ref('')

const sellerId = ref<number | null>(null)

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

const loadDraft = () => {
  const draft = loadProductDraft()
  if (!draft) return
  name.value = draft.name
  shortDesc.value = draft.shortDesc
  costPrice.value = draft.costPrice
  price.value = draft.price
  stock.value = draft.stock
  if (Array.isArray(draft.images)) {
    images.value = [...draft.images].slice(0, 5)
  }
  while (images.value.length < 5) {
    images.value.push('')
  }
}

const saveDraft = () => {
  saveProductDraft({
    sellerId: sellerId.value ?? undefined,
    name: name.value.trim(),
    shortDesc: shortDesc.value.trim(),
    costPrice: costPrice.value,
    price: price.value,
    stock: stock.value,
    images: images.value,
    detailHtml: '',
  })
}

const setImageAt = (index: number, event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    error.value = '이미지 파일만 업로드할 수 있습니다.'
    input.value = ''
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    if (typeof reader.result === 'string') {
      const next = [...images.value]
      next[index] = reader.result
      images.value = next
    }
  }
  reader.readAsDataURL(file)
}

const clearImageAt = (index: number) => {
  const next = [...images.value]
  next[index] = ''
  images.value = next
}

const goNext = () => {
  error.value = ''
  if (!name.value.trim() || !shortDesc.value.trim()) {
    error.value = '상품명과 한 줄 소개를 입력해주세요.'
    return
  }
  saveDraft()
  router.push('/seller/products/create/detail').catch(() => {
  })
}

const cancel = () => {
  router.push('/seller/products').catch(() => {
  })
}

onMounted(() => {
  sellerId.value = deriveSellerId()
  const resume = route.query.resume
  const shouldResume = resume === '1' || (Array.isArray(resume) && resume[0] === '1')
  if (shouldResume) {
    loadDraft()
    return
  }
  clearProductDraft()
  name.value = ''
  shortDesc.value = ''
  costPrice.value = 0
  price.value = 0
  stock.value = 0
  images.value = ['', '', '', '', '']
})
</script>

<template>
  <PageContainer>
    <PageHeader eyebrow="DESKIT" title="상품 등록 - 기본 정보"/>
    <section class="create-card ds-surface">
      <label class="field">
        <span class="field__label">상품명</span>
        <input v-model="name" type="text" placeholder="예: 모던 데스크 매트"/>
      </label>
      <div class="section-block">
        <div class="section-head">
          <h3>상품 이미지</h3>
        </div>
        <div class="image-slots">
          <div v-for="(img, idx) in images" :key="idx" class="image-slot">
            <div class="image-slot__label">{{ idx === 0 ? '썸네일' : String(idx) }}</div>
            <div class="image-slot__preview">
              <img v-if="img" :src="img" :alt="`상품 이미지 ${idx}`"/>
              <label v-if="!img" class="btn ghost image-slot__upload">
                업로드
                <input type="file" accept="image/*" @change="setImageAt(idx, $event)" hidden/>
              </label>
            </div>
            <div class="image-slot__actions">
              <button v-if="img" type="button" class="btn ghost" @click="clearImageAt(idx)">
                삭제
              </button>
            </div>
          </div>
        </div>
      </div>
      <label class="field">
        <span class="field__label">한 줄 소개</span>
        <input v-model="shortDesc" type="text" placeholder="예: 감성적인 데스크테리어"/>
      </label>
      <div class="field-grid">
        <label class="field">
          <span class="field__label">원가</span>
          <input v-model.number="costPrice" type="number" min="0"/>
        </label>
        <label class="field">
          <span class="field__label">판매가</span>
          <input v-model.number="price" type="number" min="0"/>
        </label>
        <label class="field">
          <span class="field__label">재고 수량</span>
          <input v-model.number="stock" type="number" min="0"/>
        </label>
      </div>
      <p v-if="error" class="error">{{ error }}</p>
      <div class="actions">
        <button type="button" class="btn" @click="cancel">취소</button>
        <button type="button" class="btn primary" @click="goNext">상세 작성</button>
      </div>
    </section>
  </PageContainer>
</template>

<style scoped>
.create-card {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field__label {
  font-weight: 800;
  color: var(--text-strong);
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.section-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-head h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 900;
  color: var(--text-strong);
}

.empty-hint {
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.9rem;
}

.image-slots {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.image-slot {
  display: flex;
  flex-direction: column;
  gap: 8px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 10px;
  background: var(--surface);
}

.image-slot__label {
  font-weight: 800;
  color: var(--text-strong);
  font-size: 0.9rem;
}

.image-slot__preview {
  border-radius: 10px;
  overflow: hidden;
  background: var(--surface-weak);
  height: 140px;
  display: grid;
  place-items: center;
}

.image-slot__preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.image-slot__placeholder {
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.85rem;
}

.image-slot__actions {
  display: flex;
  gap: 8px;
  justify-content: center;
  min-height: 28px;
}

.image-slot__actions .btn,
.image-slot__upload {
  padding: 6px 12px;
  line-height: 1.1;
  font-size: 0.85rem;
}

.image-slot__upload {
  padding: 6px 12px;
}

input {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 10px 12px;
  font-weight: 700;
  color: var(--text-strong);
  background: var(--surface);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn {
  border-radius: 999px;
  padding: 10px 18px;
  font-weight: 900;
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  cursor: pointer;
}

.btn.primary {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.btn.ghost {
  border-color: var(--border-color);
  color: var(--text-muted);
}

.error {
  margin: 0;
  color: #ef4444;
  font-weight: 700;
}

@media (max-width: 720px) {
  .field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
