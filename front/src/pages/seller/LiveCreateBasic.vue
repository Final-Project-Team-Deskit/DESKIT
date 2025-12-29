<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '../../components/PageContainer.vue'
import PageHeader from '../../components/PageHeader.vue'
import { addScheduledBroadcast } from '../../composables/useSellerBroadcasts'
import { getAuthUser } from '../../lib/auth'
import { productsData } from '../../lib/products-data'

const router = useRouter()
const DRAFT_KEY = 'deskit_seller_broadcast_draft_v1'

const title = ref('')
const subtitle = ref('')
const date = ref('')
const time = ref('')
const error = ref('')
const thumb = ref('')
const standbyThumb = ref('')
const termsAgreed = ref(false)
const products = ref<Array<{ id: string; title: string; option: string }>>([])
const thumbError = ref('')
const standbyError = ref('')
const productSearch = ref('')
const sellerId = ref<number | null>(null)

const gradientThumb = (from: string, to: string) =>
  `data:image/svg+xml;utf8,` +
  `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 320 200'>` +
  `<defs><linearGradient id='g' x1='0' x2='1' y1='0' y2='1'>` +
  `<stop offset='0' stop-color='%23${from}'/>` +
  `<stop offset='1' stop-color='%23${to}'/>` +
  `</linearGradient></defs>` +
  `<rect width='320' height='200' fill='url(%23g)'/>` +
  `</svg>`

const loadDraft = () => {
  const raw = localStorage.getItem(DRAFT_KEY)
  if (!raw) return
  try {
    const parsed = JSON.parse(raw)
    title.value = typeof parsed?.title === 'string' ? parsed.title : ''
    subtitle.value = typeof parsed?.subtitle === 'string' ? parsed.subtitle : ''
    date.value = typeof parsed?.date === 'string' ? parsed.date : ''
    time.value = typeof parsed?.time === 'string' ? parsed.time : ''
    thumb.value = typeof parsed?.thumb === 'string' ? parsed.thumb : ''
    standbyThumb.value = typeof parsed?.standbyThumb === 'string' ? parsed.standbyThumb : ''
    termsAgreed.value = typeof parsed?.termsAgreed === 'boolean' ? parsed.termsAgreed : false
    if (Array.isArray(parsed?.products)) {
      products.value = parsed.products.filter((item: any) => {
        return item && typeof item.id === 'string' && typeof item.title === 'string' && typeof item.option === 'string'
      })
    }
  } catch {
    return
  }
}

const saveDraft = () => {
  const next = {
    title: title.value.trim(),
    subtitle: subtitle.value.trim(),
    date: date.value,
    time: time.value,
    products: products.value,
    thumb: thumb.value,
    standbyThumb: standbyThumb.value,
    termsAgreed: termsAgreed.value,
  }
  localStorage.setItem(DRAFT_KEY, JSON.stringify(next))
}

const deriveSellerId = () => {
  const user = getAuthUser() as any
  const candidates = [
    user?.seller_id,
    user?.sellerId,
    user?.id,
    user?.user_id,
    user?.userId,
  ]
  for (const value of candidates) {
    if (typeof value === 'number' && Number.isFinite(value)) return value
    if (typeof value === 'string') {
      const parsed = Number.parseInt(value, 10)
      if (!Number.isNaN(parsed)) return parsed
    }
  }
  return null
}

const sellerProducts = computed(() => {
  if (!sellerId.value) return []
  const ownerId = sellerId.value
  return productsData.filter((product: any) => {
    const candidates = [
      product?.seller_id,
      product?.sellerId,
      product?.owner_id,
      product?.ownerId,
      product?.user_id,
      product?.userId,
    ]
    return candidates.some((value) => {
      if (typeof value === 'number' && Number.isFinite(value)) return value === ownerId
      if (typeof value === 'string') {
        const parsed = Number.parseInt(value, 10)
        return !Number.isNaN(parsed) && parsed === ownerId
      }
      return false
    })
  })
})

const filteredProducts = computed(() => {
  const q = productSearch.value.trim().toLowerCase()
  if (!q) return sellerProducts.value
  return sellerProducts.value.filter((product) => {
    const name = (product.name || '').toLowerCase()
    const desc = (product.short_desc ?? '').toLowerCase()
    return name.includes(q) || desc.includes(q)
  })
})

const isSelected = (productId: number) => {
  return products.value.some((item) => item.id === `prod-${productId}`)
}

const toggleProduct = (product: (typeof productsData)[number]) => {
  const id = `prod-${product.product_id}`
  if (isSelected(product.product_id)) {
    products.value = products.value.filter((item) => item.id !== id)
    return
  }
  products.value.push({
    id,
    title: product.name,
    option: product.short_desc?.trim() || '-',
  })
}

const handleThumbUpload = (event: Event) => {
  thumbError.value = ''
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    thumbError.value = '이미지 파일만 업로드할 수 있습니다.'
    input.value = ''
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    thumb.value = typeof reader.result === 'string' ? reader.result : ''
  }
  reader.readAsDataURL(file)
}

const handleStandbyUpload = (event: Event) => {
  standbyError.value = ''
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    standbyError.value = '이미지 파일만 업로드할 수 있습니다.'
    input.value = ''
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    standbyThumb.value = typeof reader.result === 'string' ? reader.result : ''
  }
  reader.readAsDataURL(file)
}

const submit = () => {
  error.value = ''
  thumbError.value = ''
  standbyError.value = ''
  if (!title.value.trim() || !date.value || !time.value) {
    error.value = '방송 제목과 일정을 입력해주세요.'
    return
  }
  if (!termsAgreed.value) {
    error.value = '약관에 동의해주세요.'
    return
  }

  const id = `schedule-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`
  const datetime = `${date.value} ${time.value}`
  const scheduled = {
    id,
    title: title.value.trim(),
    subtitle: subtitle.value.trim() || '예약 방송',
    thumb: thumb.value || gradientThumb('0f172a', '1f2937'),
    datetime,
    ctaLabel: '방송 시작',
    products: products.value,
    standbyThumb: standbyThumb.value || undefined,
    termsAgreed: termsAgreed.value,
  }

  addScheduledBroadcast(scheduled)
  localStorage.removeItem(DRAFT_KEY)
  router.push({ path: '/seller/live', query: { tab: 'scheduled' } }).catch(() => {})
}

onMounted(() => {
  loadDraft()
  sellerId.value = deriveSellerId()
})
</script>

<template>
  <PageContainer>
    <PageHeader eyebrow="DESKIT" title="방송 등록 - 기본 정보" />
    <section class="create-card ds-surface">
      <label class="field">
        <span class="field__label">방송 제목</span>
        <input v-model="title" type="text" placeholder="예: 홈오피스 라이브" />
      </label>
      <label class="field">
        <span class="field__label">간단 설명</span>
        <input v-model="subtitle" type="text" placeholder="예: 셋업 추천" />
      </label>
      <div class="field-grid">
        <label class="field">
          <span class="field__label">방송 날짜</span>
          <input v-model="date" type="date" />
        </label>
        <label class="field">
          <span class="field__label">방송 시간</span>
          <input v-model="time" type="time" />
        </label>
      </div>
      <div class="section-block">
        <div class="section-head">
          <h3>판매 상품 등록</h3>
          <span class="count-pill">선택 {{ products.length }}개</span>
        </div>
        <label class="field">
          <span class="field__label">상품 검색</span>
          <input v-model="productSearch" type="text" placeholder="상품명을 검색하세요" />
        </label>
        <div v-if="!sellerId" class="empty-hint">로그인이 필요합니다.</div>
        <div v-else-if="sellerProducts.length === 0" class="empty-hint">등록된 판매 상품이 없습니다.</div>
        <div v-else class="product-select">
          <label v-for="product in filteredProducts" :key="product.product_id" class="product-option">
            <input
              type="checkbox"
              :checked="isSelected(product.product_id)"
              @change="toggleProduct(product)"
            />
            <span class="product-option__text">
              <strong>{{ product.name }}</strong>
              <span class="product-option__meta">{{ product.short_desc ?? '-' }}</span>
            </span>
          </label>
        </div>
      </div>
      <div class="section-block">
        <div class="section-head">
          <h3>썸네일/대기화면</h3>
        </div>
        <div class="field-grid">
          <label class="field">
            <span class="field__label">방송 썸네일 업로드</span>
            <input type="file" accept="image/*" @change="handleThumbUpload" />
            <span v-if="thumbError" class="error">{{ thumbError }}</span>
            <div v-if="thumb" class="preview">
              <img :src="thumb" alt="방송 썸네일 미리보기" />
            </div>
          </label>
          <label class="field">
            <span class="field__label">대기화면 업로드</span>
            <input type="file" accept="image/*" @change="handleStandbyUpload" />
            <span v-if="standbyError" class="error">{{ standbyError }}</span>
            <div v-if="standbyThumb" class="preview">
              <img :src="standbyThumb" alt="대기화면 미리보기" />
            </div>
          </label>
        </div>
      </div>
      <div class="section-block">
        <label class="checkbox">
          <input v-model="termsAgreed" type="checkbox" />
          <span>방송 운영 및 약관에 동의합니다. (필수)</span>
        </label>
      </div>
      <p v-if="error" class="error">{{ error }}</p>
      <div class="actions">
        <button type="button" class="btn" @click="saveDraft">임시 저장</button>
        <button type="button" class="btn primary" @click="submit">방송 등록</button>
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
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

input {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 10px 12px;
  font-weight: 700;
  color: var(--text-strong);
  background: var(--surface);
}

input[type='file'] {
  padding: 8px 0;
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

.count-pill {
  border: 1px solid var(--border-color);
  background: var(--surface-weak);
  color: var(--text-strong);
  padding: 6px 10px;
  border-radius: 999px;
  font-weight: 800;
  font-size: 0.85rem;
}

.empty-hint {
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.9rem;
}

.product-select {
  max-height: 220px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.product-option {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: var(--surface);
  cursor: pointer;
}

.product-option input {
  margin-top: 4px;
}

.product-option__text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: var(--text-strong);
  font-weight: 800;
}

.product-option__meta {
  font-size: 0.85rem;
  color: var(--text-muted);
  font-weight: 700;
}

.preview {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  background: var(--surface-weak);
}

.preview img {
  width: 100%;
  height: 140px;
  object-fit: cover;
  display: block;
}

.checkbox {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: var(--text-strong);
}

.error {
  margin: 0;
  color: #ef4444;
  font-weight: 700;
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

@media (max-width: 720px) {
  .field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
