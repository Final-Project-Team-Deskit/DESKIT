<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '../../components/PageContainer.vue'
import PageHeader from '../../components/PageHeader.vue'
import ProductBasicFields from '../../components/seller/ProductBasicFields.vue'

const router = useRouter()
const route = useRoute()
const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

type ProductStatus = 'DRAFT' | 'READY' | 'ON_SALE' | 'LIMITED_SALE' | 'SOLD_OUT' | 'PAUSED' | 'HIDDEN'

const name = ref('')
const shortDesc = ref('')
const price = ref(0)
const stock = ref(0)
const images = ref<string[]>(['', '', '', '', ''])
const error = ref('')
const status = ref<ProductStatus | null>(null)
const isDeleting = ref(false)

const statusLabelMap: Record<ProductStatus, string> = {
  DRAFT: '작성중',
  READY: '준비',
  ON_SALE: '판매중',
  LIMITED_SALE: '한정판매',
  SOLD_OUT: '품절',
  PAUSED: '일시중지',
  HIDDEN: '숨김',
}

const buildAuthHeaders = (): Record<string, string> => {
  const access = localStorage.getItem('access') || sessionStorage.getItem('access')
  if (!access) return {}
  return { Authorization: `Bearer ${access}` }
}

const loadInitial = async () => {
  const id = typeof route.params.id === 'string' ? route.params.id : ''
  if (!id) {
    error.value = '상품 정보를 불러올 수 없습니다.'
    return
  }
  try {
    const response = await fetch(`${apiBase}/api/seller/products/${id}`, {
      method: 'GET',
      headers: {
        ...buildAuthHeaders(),
      },
      credentials: 'include',
    })
    if (!response.ok) {
      error.value = '상품 정보를 불러올 수 없습니다.'
      return
    }
    const data = (await response.json()) as {
      product_name?: string
      short_desc?: string
      price?: number
      stock_qty?: number
      status?: ProductStatus
    }
    name.value = data.product_name ?? ''
    shortDesc.value = data.short_desc ?? ''
    price.value = typeof data.price === 'number' ? data.price : 0
    stock.value = typeof data.stock_qty === 'number' ? data.stock_qty : 0
    status.value = data.status ?? null
  } catch {
    error.value = '상품 정보를 불러올 수 없습니다.'
  }
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

const cancel = () => {
  router.push({ name: 'seller-products' }).catch(() => {})
}

const handleDelete = async () => {
  const id = typeof route.params.id === 'string' ? route.params.id : ''
  if (!id) {
    error.value = '상품 정보를 확인할 수 없습니다.'
    return
  }
  if (!window.confirm('정말 삭제하시겠습니까?')) return

  error.value = ''
  isDeleting.value = true
  try {
    const response = await fetch(`${apiBase}/api/seller/products/${id}`, {
      method: 'DELETE',
      headers: {
        ...buildAuthHeaders(),
      },
      credentials: 'include',
    })
    if (response.status === 401 || response.status === 403) {
      error.value = '권한이 없습니다. 다시 로그인해주세요.'
      return
    }
    if (response.status === 404) {
      window.alert('이미 삭제되었거나 상품을 찾을 수 없습니다.')
      router.push({ name: 'seller-products' }).catch(() => {})
      return
    }
    if (!response.ok && response.status !== 204) {
      error.value = '상품 삭제에 실패했습니다.'
      return
    }
    window.alert('상품이 삭제되었습니다.')
    router.push({ name: 'seller-products' }).catch(() => {})
  } catch {
    error.value = '상품 삭제에 실패했습니다.'
  } finally {
    isDeleting.value = false
  }
}

const canEditAll = computed(() => {
  return status.value === 'DRAFT' || status.value === 'READY' || status.value === 'PAUSED'
})
const canEditPartial = computed(() => {
  return status.value === 'ON_SALE'
})
const disableName = computed(() => !canEditAll.value)
const disableShortDesc = computed(() => !(canEditAll.value || canEditPartial.value))
const disablePrice = computed(() => !(canEditAll.value || canEditPartial.value))
const disableStock = computed(() => !canEditAll.value)

const statusMessage = computed(() => {
  if (status.value === 'ON_SALE') {
    return '판매중인 상품은 상품명과 재고 수량을 수정할 수 없습니다.'
  }
  if (status.value && !canEditAll.value && !canEditPartial.value) {
    return '현재 상태에서는 상품 정보를 수정할 수 없습니다.'
  }
  return ''
})

const handleSubmit = async () => {
  error.value = ''

  if (!name.value.trim()) {
    error.value = '상품명을 입력해주세요.'
    return
  }
  if (!Number.isFinite(price.value) || price.value < 0) {
    error.value = '판매가를 올바르게 입력해주세요.'
    return
  }
  if (!Number.isFinite(stock.value) || stock.value < 0) {
    error.value = '재고를 올바르게 입력해주세요.'
    return
  }
  if (!canEditAll.value && !canEditPartial.value) {
    error.value = '수정할 수 없는 상품 상태입니다.'
    return
  }

  const id = typeof route.params.id === 'string' ? route.params.id : ''
  if (!id) {
    error.value = '상품 정보를 확인할 수 없습니다.'
    return
  }

  const payload: Record<string, unknown> = {}
  if (canEditAll.value) {
    payload.product_name = name.value.trim()
    payload.short_desc = shortDesc.value.trim()
    payload.price = price.value
    payload.stock_qty = stock.value
  } else if (canEditPartial.value) {
    payload.short_desc = shortDesc.value.trim()
    payload.price = price.value
  }

  try {
    const response = await fetch(`${apiBase}/api/seller/products/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        ...buildAuthHeaders(),
      },
      credentials: 'include',
      body: JSON.stringify(payload),
    })
    if (!response.ok) {
      error.value = '상품 수정에 실패했습니다.'
      return
    }
    router.push({ name: 'seller-products-edit-detail', params: { id } }).catch(() => {})
  } catch {
    error.value = '상품 수정에 실패했습니다.'
  }
}

onMounted(() => {
  loadInitial()
})
</script>

<template>
  <PageContainer>
    <PageHeader eyebrow="DESKIT" title="상품 수정 - 기본 정보" />
    <section class="create-card ds-surface">
      <div v-if="status" class="status-info">
        <span class="status-label">현재 상태: {{ statusLabelMap[status] }}</span>
        <span v-if="statusMessage" class="status-help">{{ statusMessage }}</span>
      </div>
      <ProductBasicFields
        v-model:name="name"
        v-model:shortDesc="shortDesc"
        v-model:price="price"
        v-model:stock="stock"
        :disableName="disableName"
        :disableShortDesc="disableShortDesc"
        :disablePrice="disablePrice"
        :disableStock="disableStock"
      />
      <div class="section-block">
        <div class="section-head">
          <h3>상품 이미지</h3>
        </div>
        <div class="image-slots">
          <div v-for="(img, idx) in images" :key="idx" class="image-slot">
            <div class="image-slot__label">{{ idx === 0 ? '썸네일' : String(idx) }}</div>
            <div class="image-slot__preview">
              <img v-if="img" :src="img" :alt="`상품 이미지 ${idx}`" />
              <label v-if="!img" class="btn ghost image-slot__upload">
                업로드
                <input type="file" accept="image/*" @change="setImageAt(idx, $event)" hidden />
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
      <p v-if="error" class="error">{{ error }}</p>
      <div class="actions">
        <button type="button" class="btn" @click="cancel">취소</button>
        <button type="button" class="btn danger" :disabled="isDeleting" @click="handleDelete">삭제</button>
        <button type="button" class="btn primary" @click="handleSubmit">상세 수정</button>
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

.status-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  border-radius: 12px;
  background: var(--surface-weak);
  border: 1px solid var(--border-color);
}

.status-label {
  font-weight: 800;
  color: var(--text-strong);
}

.status-help {
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.85rem;
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

.btn.danger {
  border-color: #ef4444;
  color: #ef4444;
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
