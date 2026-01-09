<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import PageContainer from '../../components/PageContainer.vue'
import PageHeader from '../../components/PageHeader.vue'
import LiveImageCropModal from '../../components/LiveImageCropModal.vue'
import {
  buildDraftFromReservation,
  clearDraft,
  createEmptyDraft,
  DRAFT_KEY,
  type LiveCreateDraft,
  type LiveCreateProduct,
  loadDraft,
  saveDraft,
} from '../../composables/useLiveCreateDraft'
import {
  type BroadcastCategory,
  createBroadcast,
  fetchCategories,
  fetchReservationSlots,
  fetchSellerProducts,
  type ReservationSlot,
  updateBroadcast,
} from '../../lib/live/api'

const router = useRouter()
const route = useRoute()

const draft = ref<LiveCreateDraft>(createEmptyDraft())
const productSearch = ref('')
const thumbError = ref('')
const standbyError = ref('')
const error = ref('')
const showTermsModal = ref(false)
const showProductModal = ref(false)
const modalProducts = ref<LiveCreateProduct[]>([])
const sellerProducts = ref<LiveCreateProduct[]>([])
const categories = ref<BroadcastCategory[]>([])
const reservationSlots = ref<ReservationSlot[]>([])
const activeDate = ref('')
const cropperOpen = ref(false)
const cropperSource = ref('')
const cropperFileName = ref('')
const cropTarget = ref<'thumb' | 'standby' | null>(null)
const cropperApplied = ref(false)
const thumbInputRef = ref<HTMLInputElement | null>(null)
const standbyInputRef = ref<HTMLInputElement | null>(null)

const reservationId = computed(() => {
  const queryValue = route.query.reservationId
  if (Array.isArray(queryValue)) return queryValue[0] ?? ''
  return typeof queryValue === 'string' ? queryValue : ''
})
const isEditMode = computed(() => route.query.mode === 'edit' && !!reservationId.value)
const modalCount = computed(() => modalProducts.value.length)

const availableProducts = computed(() => sellerProducts.value)

const filteredProducts = computed(() => {
  const q = productSearch.value.trim().toLowerCase()
  if (!q) return availableProducts.value
  return availableProducts.value.filter((product) => {
    const name = product.name.toLowerCase()
    const option = product.option.toLowerCase()
    return name.includes(q) || option.includes(q)
  })
})

const isSelected = (productId: string, source: LiveCreateProduct[] = draft.value.products) =>
  source.some((item) => item.id === productId)

const resolveMaxQuantity = (product: LiveCreateProduct) => {
  const stock = Number.isFinite(product.stock) ? product.stock : 0
  const safetyStock = Number.isFinite(product.safetyStock) ? product.safetyStock : 0
  return Math.max(stock - safetyStock, 0)
}

const resolveMinQuantity = (product: LiveCreateProduct) => (resolveMaxQuantity(product) > 0 ? 1 : 0)

const clampProductQuantity = (product: LiveCreateProduct, value?: number) => {
  const maxQuantity = resolveMaxQuantity(product)
  const minQuantity = resolveMinQuantity(product)
  const rawValue = typeof value === 'number' && Number.isFinite(value) ? value : product.quantity
  const normalized = typeof rawValue === 'number' && Number.isFinite(rawValue) ? rawValue : minQuantity
  const nextValue = maxQuantity > 0
    ? Math.min(Math.max(normalized, minQuantity), maxQuantity)
    : minQuantity
  return { ...product, quantity: nextValue }
}

const addProduct = (product: LiveCreateProduct, target: LiveCreateProduct[]) => {
  if (!isSelected(product.id, target) && target.length >= 10) {
    error.value = '상품은 최대 10개까지 등록할 수 있습니다.'
    return target
  }
  if (isSelected(product.id, target)) return target
  return [...target, clampProductQuantity(product)]
}

const removeProduct = (productId: string, target: LiveCreateProduct[]) => target.filter((item) => item.id !== productId)

const toggleProductInModal = (product: LiveCreateProduct) => {
  modalProducts.value = isSelected(product.id, modalProducts.value)
    ? removeProduct(product.id, modalProducts.value)
    : addProduct(product, modalProducts.value)
}

const updateProductPrice = (productId: string, value: number) => {
  draft.value.products = draft.value.products.map((product) =>
    product.id === productId ? { ...product, broadcastPrice: value < 0 ? 0 : value } : product,
  )
}

const updateProductQuantity = (productId: string, value: number) => {
  draft.value.products = draft.value.products.map((product) =>
    product.id === productId ? clampProductQuantity(product, value) : product,
  )
}

const syncDraft = () => {
  const trimmedQuestions = draft.value.questions.map((q) => ({ ...q, text: q.text.trim() })).filter((q) => q.text.length > 0)
  const shouldUpdateQuestions =
    trimmedQuestions.length !== draft.value.questions.length ||
    trimmedQuestions.some((item, index) => item.text !== draft.value.questions[index]?.text)

  if (shouldUpdateQuestions) {
    draft.value.questions = trimmedQuestions
  }

  saveDraft({
    ...draft.value,
    title: draft.value.title.trim(),
    subtitle: draft.value.subtitle?.trim() ?? '',
    category: draft.value.category.trim(),
    notice: draft.value.notice.trim(),
    questions: trimmedQuestions,
    reservationId: reservationId.value || draft.value.reservationId,
  })
}

const restoreDraft = async () => {
  const storedDraft = sessionStorage.getItem(DRAFT_KEY)
  const savedDraft = storedDraft ? loadDraft() : null
  let baseDraft = createEmptyDraft()
  if (!isEditMode.value && savedDraft && (!savedDraft.reservationId || savedDraft.reservationId === reservationId.value)) {
    const shouldRestore = window.confirm('이전에 작성 중인 내용을 불러올까요?')
    if (shouldRestore) {
      baseDraft = { ...createEmptyDraft(), ...savedDraft }
    } else {
      clearDraft()
    }
  }

  const reservationDraft = isEditMode.value
    ? {
      ...baseDraft,
      ...(await buildDraftFromReservation(reservationId.value)),
      reservationId: reservationId.value,
    }
    : baseDraft

  draft.value = reservationDraft
  draft.value.products = draft.value.products.map((product) => clampProductQuantity(product))
  modalProducts.value = draft.value.products.map((p) => ({ ...p }))
}

const openCropper = (file: File, target: 'thumb' | 'standby') => {
  const reader = new FileReader()
  reader.onload = () => {
    cropperSource.value = typeof reader.result === 'string' ? reader.result : ''
    cropperFileName.value = file.name
    cropTarget.value = target
    cropperApplied.value = false
    cropperOpen.value = true
  }
  reader.readAsDataURL(file)
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
  openCropper(file, 'thumb')
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
  openCropper(file, 'standby')
}

const applyCroppedImage = (payload: { dataUrl: string; fileName: string }) => {
  cropperApplied.value = true
  if (cropTarget.value === 'thumb') {
    draft.value.thumb = payload.dataUrl
  }
  if (cropTarget.value === 'standby') {
    draft.value.standbyThumb = payload.dataUrl
  }
}

const clearThumb = () => {
  draft.value.thumb = ''
  if (thumbInputRef.value) thumbInputRef.value.value = ''
}

const clearStandby = () => {
  draft.value.standbyThumb = ''
  if (standbyInputRef.value) standbyInputRef.value.value = ''
}

const handleThumbError = () => {
  thumbError.value = '이미지를 불러오지 못했습니다.'
  clearThumb()
}

const handleStandbyError = () => {
  standbyError.value = '이미지를 불러오지 못했습니다.'
  clearStandby()
}

const resetCropperState = () => {
  cropperSource.value = ''
  cropperFileName.value = ''
  cropTarget.value = null
  cropperApplied.value = false
}

const clearInputForTarget = (target: 'thumb' | 'standby') => {
  if (target === 'thumb' && thumbInputRef.value) {
    thumbInputRef.value.value = ''
  }
  if (target === 'standby' && standbyInputRef.value) {
    standbyInputRef.value.value = ''
  }
}

const getErrorMessage = (error: unknown, fallback: string) => {
  if (error && typeof error === 'object' && 'message' in error) {
    const message = (error as { message?: unknown }).message
    if (typeof message === 'string' && message.trim()) {
      return message
    }
  }
  return fallback
}

const getErrorStatus = (error: unknown) => {
  if (error && typeof error === 'object' && 'response' in error) {
    const response = (error as { response?: { status?: number } }).response
    if (response && typeof response.status === 'number') {
      return response.status
    }
  }
  return null
}

const submit = () => {
  error.value = ''
  thumbError.value = ''
  standbyError.value = ''

  draft.value.questions = draft.value.questions.map((q) => ({
    ...q,
    text: q.text.trim()
  })).filter((q) => q.text.length > 0)

  if (!draft.value.title.trim() || !draft.value.category || !draft.value.date || !draft.value.time) {
    error.value = '방송 제목, 카테고리, 일정을 입력해주세요.'
    return
  }

  if (!draft.value.products.length) {
    error.value = '최소 1개의 판매 상품을 등록해주세요.'
    return
  }

  const invalidProduct = draft.value.products.find((product) => {
    const maxQuantity = resolveMaxQuantity(product)
    return maxQuantity < 1 || product.quantity < 1 || product.quantity > maxQuantity
  })
  if (invalidProduct) {
    error.value = '판매 수량을 재고 범위 내에서 선택해주세요.'
    return
  }

  if (!draft.value.termsAgreed) {
    error.value = '약관에 동의해주세요.'
    return
  }

  if (!timeOptions.value.includes(draft.value.time)) {
    alert('선택한 시간대의 예약이 마감되었습니다. 다른 시간대를 선택해주세요.')
    void reloadReservationSlots(draft.value.date)
    return
  }

  const confirmed = window.confirm(isEditMode.value ? '예약 수정을 진행할까요?' : '방송 등록을 진행할까요?')
  if (!confirmed) return

  const scheduledAt = `${draft.value.date} ${draft.value.time}:00`
  const payload = {
    title: draft.value.title.trim(),
    notice: draft.value.notice.trim(),
    categoryId: Number(draft.value.category),
    scheduledAt,
    thumbnailUrl: draft.value.thumb,
    waitScreenUrl: draft.value.standbyThumb || null,
    broadcastLayout: 'FULL',
    products: draft.value.products.map((product) => ({
      productId: Number(product.id.replace('prod-', '')),
      salePrice: product.broadcastPrice,
      quantity: product.quantity,
    })),
    qcards: draft.value.questions.map((q) => ({ question: q.text.trim() })).filter((q) => q.question.length > 0),
  }

  const request = isEditMode.value
    ? updateBroadcast(Number(reservationId.value), payload)
    : createBroadcast(payload)

  request
    .then((broadcastId) => {
      clearDraft()
      alert(isEditMode.value ? '예약 수정이 완료되었습니다.' : '방송 등록이 완료되었습니다.')
      const id = broadcastId ?? reservationId.value
      const redirectPath = isEditMode.value
        ? `/seller/broadcasts/reservations/${id}`
        : '/seller/live?tab=scheduled'
      router.push(redirectPath).catch(() => {})
    })
    .catch((apiError) => {
      if (apiError?.code === 'B005') {
        alert('선택한 시간대의 예약이 마감되었습니다. 다른 시간대를 선택해주세요.')
        void reloadReservationSlots(draft.value.date)
        return
      }
      error.value = getErrorMessage(apiError, '방송 등록에 실패했습니다.')
    })
}

const goPrev = () => {
  router.push({ path: '/seller/live/create', query: route.query }).catch(() => {})
}

const cancel = () => {
  const ok = window.confirm('작성 중인 내용을 취소하시겠어요?')
  if (!ok) return
  const redirect = isEditMode.value && reservationId.value
    ? `/seller/broadcasts/reservations/${reservationId.value}`
    : '/seller/live?tab=scheduled'
  router.push(redirect).catch(() => {})
}

const openProductModal = () => {
  modalProducts.value = draft.value.products.map((p) => ({ ...p }))
  productSearch.value = ''
  showProductModal.value = true
}

const cancelProductSelection = () => {
  modalProducts.value = draft.value.products.map((p) => ({ ...p }))
  showProductModal.value = false
}

const saveProductSelection = () => {
  draft.value.products = modalProducts.value.map((p) => clampProductQuantity(p))
  showProductModal.value = false
  alert('상품 선택이 저장되었습니다.')
}

const confirmRemoveProduct = (productId: string) => {
  const ok = window.confirm('이 상품을 리스트에서 제거하시겠어요?')
  if (ok) {
    draft.value.products = removeProduct(productId, draft.value.products)
  }
}

const formatLocalDate = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const addDays = (date: Date, days: number) => {
  const next = new Date(date)
  next.setDate(next.getDate() + days)
  return next
}

const minDate = computed(() => formatLocalDate(addDays(new Date(), 1)))

const maxDate = computed(() => formatLocalDate(addDays(new Date(), 15)))

const normalizeCategorySelection = () => {
  if (!draft.value.category) return
  const current = categories.value.find((category) => category.id.toString() === draft.value.category)
  if (current) return
  const matched = categories.value.find((category) => category.name === draft.value.category)
  if (matched) draft.value.category = matched.id.toString()
}

const loadCategories = async () => {
  try {
    categories.value = await fetchCategories()
    normalizeCategorySelection()
  } catch (apiError) {
    error.value = getErrorMessage(apiError, '카테고리를 불러오지 못했습니다.')
  }
}

const loadProducts = async () => {
  try {
    sellerProducts.value = await fetchSellerProducts()
  } catch (apiError) {
    if (getErrorStatus(apiError) === 404) {
      sellerProducts.value = []
      return
    }
    error.value = getErrorMessage(apiError, '상품 목록을 불러오지 못했습니다.')
  }
}

const reloadReservationSlots = async (date: string) => {
  if (!date) return
  try {
    reservationSlots.value = await fetchReservationSlots(date)
    const current = reservationSlots.value.map((slot) => slot.time)
    if (draft.value.time && !current.includes(draft.value.time)) {
      draft.value.time = ''
    }
  } catch (apiError) {
    error.value = getErrorMessage(apiError, '예약 가능 시간을 불러오지 못했습니다.')
  }
}

const timeOptions = computed(() => reservationSlots.value.map((slot) => slot.time))

watch(
  () => [isEditMode.value, reservationId.value],
  () => {
    restoreDraft()
  },
  { immediate: true },
)

watch(
  () => draft.value.date,
  (value) => {
    if (!value) return
    if (value < minDate.value) {
      draft.value.date = minDate.value
      return
    }
    if (value > maxDate.value) {
      draft.value.date = maxDate.value
      return
    }
    if (value !== activeDate.value) {
      activeDate.value = value
      void reloadReservationSlots(value)
    }
  },
)

watch(cropperOpen, (open, wasOpen) => {
  if (!open && wasOpen) {
    if (!cropperApplied.value && cropTarget.value) {
      clearInputForTarget(cropTarget.value)
    }
    resetCropperState()
  }
})

onMounted(async () => {
  await loadCategories()
  await loadProducts()
  draft.value.products = draft.value.products.map((product) => clampProductQuantity(product))
  if (draft.value.date) {
    activeDate.value = draft.value.date
    void reloadReservationSlots(draft.value.date)
  }
})

watch(
  draft,
  () => {
    syncDraft()
  },
  { deep: true },
)
</script>

<template>
    <PageContainer>
      <PageHeader :eyebrow="isEditMode ? 'DESKIT' : 'DESKIT'" :title="isEditMode ? '예약 수정 - 기본 정보' : '방송 등록 - 기본 정보'" />
      <LiveImageCropModal
        v-model="cropperOpen"
        :image-src="cropperSource"
        :file-name="cropperFileName"
        @confirm="applyCroppedImage"
      />
    <section class="create-card ds-surface">
      <div class="step-meta">
        <span class="step-indicator">2 / 2 단계</span>
        <button type="button" class="btn ghost" @click="goPrev">이전</button>
      </div>
      <label class="field">
        <span class="field__label">방송 제목</span>
        <input v-model="draft.title" type="text" maxlength="30" placeholder="예: 홈오피스 라이브" />
        <span class="field__hint">{{ draft.title.length }}/30</span>
      </label>
      <div class="field-grid">
        <label class="field">
          <span class="field__label">카테고리</span>
          <select v-model="draft.category">
            <option value="" disabled>카테고리를 선택하세요</option>
            <option v-for="category in categories" :key="category.id" :value="category.id.toString()">
              {{ category.name }}
            </option>
          </select>
        </label>
      </div>
      <label class="field">
        <span class="field__label">공지사항</span>
        <textarea
          v-model="draft.notice"
          rows="3"
          maxlength="50"
          placeholder="시청자에게 안내할 공지를 입력하세요 (최대 50자)"
        ></textarea>
        <span class="field__hint">{{ draft.notice.length }}/50</span>
      </label>
      <div class="field-grid">
        <label class="field">
          <span class="field__label">방송 날짜</span>
          <input v-model="draft.date" type="date" :min="minDate" :max="maxDate" />
        </label>
        <label class="field">
          <span class="field__label">방송 시간</span>
          <select v-model="draft.time">
            <option value="" disabled>시간을 선택하세요</option>
            <option v-for="time in timeOptions" :key="time" :value="time">{{ time }}</option>
          </select>
        </label>
      </div>
      <div class="section-block">
        <div class="section-head">
          <h3>판매 상품 등록</h3>
          <span class="count-pill">선택 {{ draft.products.length }}개</span>
        </div>
        <div class="product-search-bar">
          <button type="button" class="btn" @click="openProductModal">상품 선택</button>
          <span class="search-hint">최소 1개, 최대 10개 선택</span>
        </div>
        <div v-if="draft.products.length" class="product-table-wrap">
          <table>
            <thead>
              <tr>
                <th>상품명</th>
                <th>정가</th>
                <th>방송 할인가</th>
                <th>판매 수량</th>
                <th>재고</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="product in draft.products" :key="product.id">
                <td>
                  <div class="product-cell">
                    <div class="thumb" v-if="product.thumb">
                      <img :src="product.thumb" :alt="product.name" />
                    </div>
                    <div class="product-text">
                      <strong>{{ product.name }}</strong>
                      <span class="product-option__meta">{{ product.option }}</span>
                    </div>
                  </div>
                </td>
                <td class="numeric">{{ product.price.toLocaleString() }}원</td>
                <td>
                  <input
                    class="table-input"
                    type="number"
                    min="0"
                    :value="product.broadcastPrice"
                    @input="updateProductPrice(product.id, Number(($event.target as HTMLInputElement).value))"
                  />
                </td>
                <td>
                  <input
                    class="table-input"
                    type="number"
                    :min="resolveMinQuantity(product)"
                    :max="resolveMaxQuantity(product)"
                    :value="product.quantity"
                    @input="updateProductQuantity(product.id, Number(($event.target as HTMLInputElement).value))"
                  />
                </td>
                <td class="numeric">{{ product.stock }}</td>
                <td>
                  <button type="button" class="btn ghost" @click="confirmRemoveProduct(product.id)">
                  제거
                </button>
                </td>
              </tr>
            </tbody>
          </table>
          <p class="table-hint">{{ draft.products.length }}/10 개 선택됨 (최소 1개 필수)</p>
        </div>
      </div>
      <div class="section-block">
        <div class="section-head">
          <h3>썸네일/대기화면</h3>
        </div>
        <div class="field-grid">
          <label class="field">
            <span class="field__label">방송 썸네일 업로드</span>
            <input ref="thumbInputRef" type="file" accept="image/*" @change="handleThumbUpload" />
            <span v-if="thumbError" class="error">{{ thumbError }}</span>
            <div v-if="draft.thumb" class="preview">
              <img :src="draft.thumb" alt="방송 썸네일 미리보기" @error="handleThumbError" />
            </div>
            <button type="button" class="btn ghost upload-clear" @click="clearThumb">이미지 삭제</button>
          </label>
          <label class="field">
            <span class="field__label">대기화면 업로드</span>
            <input ref="standbyInputRef" type="file" accept="image/*" @change="handleStandbyUpload" />
            <span v-if="standbyError" class="error">{{ standbyError }}</span>
            <div v-if="draft.standbyThumb" class="preview">
              <img :src="draft.standbyThumb" alt="대기화면 미리보기" @error="handleStandbyError" />
            </div>
            <button type="button" class="btn ghost upload-clear" @click="clearStandby">이미지 삭제</button>
          </label>
        </div>
      </div>
      <div class="section-block">
        <label class="checkbox">
          <input v-model="draft.termsAgreed" type="checkbox" />
          <span>방송 운영 및 약관에 동의합니다. (필수)</span>
          <button type="button" class="link" @click="showTermsModal = true">자세히보기</button>
        </label>
      </div>
      <p v-if="error" class="error">{{ error }}</p>
      <div class="actions">
        <div class="action-buttons">
          <button type="button" class="btn" @click="cancel">취소</button>
          <button type="button" class="btn primary" @click="submit">{{ isEditMode ? '저장' : '방송 등록' }}</button>
        </div>
      </div>
      <teleport to="body">
        <div v-if="showProductModal" class="modal">
          <div class="modal__backdrop" @click="cancelProductSelection"></div>
          <div class="modal__content">
            <div class="modal__header">
              <h3>상품 선택</h3>
              <button type="button" class="btn ghost" aria-label="닫기" @click="cancelProductSelection">×</button>
            </div>
            <div class="modal__body">
              <div class="product-search-bar modal-search">
                <input v-model="productSearch" class="search-input__plain" type="text" placeholder="상품명을 검색하세요" />
                <span class="search-hint">체크박스로 선택 후 저장을 누르면 반영됩니다.</span>
              </div>
              <div class="product-grid">
                <label
                  v-for="product in filteredProducts"
                  :key="product.id"
                  class="product-card"
                  :class="{ checked: isSelected(product.id, modalProducts) }"
                >
                  <input
                    type="checkbox"
                    :checked="isSelected(product.id, modalProducts)"
                    @change="toggleProductInModal(product)"
                  />
                  <div class="product-thumb" v-if="product.thumb">
                    <img :src="product.thumb" :alt="product.name" />
                  </div>
                  <div class="product-content">
                    <div class="product-name">{{ product.name }}</div>
                    <div class="product-meta">
                      <span>{{ product.option }}</span>
                      <span>정가 {{ product.price.toLocaleString() }}원</span>
                      <span>재고 {{ product.stock }}</span>
                    </div>
                  </div>
                </label>
              </div>
            </div>
            <div class="modal__footer">
              <span class="modal__count">선택 {{ modalCount }}개</span>
              <div class="modal__actions">
                <button type="button" class="btn ghost" @click="cancelProductSelection">취소</button>
                <button type="button" class="btn primary" @click="saveProductSelection">저장</button>
              </div>
            </div>
          </div>
        </div>
      </teleport>
      <div v-if="showTermsModal" class="modal">
        <div class="modal__content">
          <div class="modal__header">
            <h3>방송 운영 및 약관</h3>
            <button type="button" class="btn ghost" @click="showTermsModal = false">닫기</button>
          </div>
          <div class="modal__body">
            <p>방송 운영 시 상품 정보, 가격, 재고를 정확히 안내해야 하며 허위 광고가 금지됩니다.</p>
            <p>방송 중 욕설, 비방 등 운영 정책에 어긋나는 행위는 제재될 수 있습니다.</p>
            <p>취소 및 환불 정책을 명확히 안내하고, 방송 종료 후 문의에 신속히 응답해주세요.</p>
          </div>
        </div>
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

.step-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.step-indicator {
  color: var(--text-muted);
  font-weight: 800;
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

.field__hint {
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.85rem;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

input,
select,
textarea {
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
  display: none;
}

.product-table-wrap {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
}

.product-table-wrap table {
  width: 100%;
  border-collapse: collapse;
}

.product-table-wrap th,
.product-table-wrap td {
  padding: 10px;
  border-bottom: 1px solid var(--border-color);
  text-align: left;
  font-weight: 700;
}

.product-table-wrap th {
  background: var(--surface-weak);
  font-weight: 900;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.thumb {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  overflow: hidden;
  background: var(--surface-weak);
  border: 1px solid var(--border-color);
  flex-shrink: 0;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.numeric {
  text-align: right;
}

.table-input {
  width: 120px;
}

.table-hint {
  margin: 0;
  padding: 8px 12px;
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

.upload-filename {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 13px;
}

.upload-clear {
  margin-top: 6px;
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
  align-items: center;
  flex-wrap: wrap;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.modal__body {
  max-height: 520px;
  overflow: auto;
}

.modal__footer {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.modal__count {
  font-weight: 800;
  color: var(--text-strong);
}

.modal__actions {
  display: flex;
  gap: 8px;
  align-items: center;
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

.btn.ghost {
  color: var(--text-muted);
}

.btn.primary {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.product-search-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.modal-search {
  align-items: flex-start;
  flex-direction: column;
}

.search-input {
  position: relative;
  flex: 1 1 320px;
}

.search-input__plain {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 10px 12px;
  font-weight: 700;
  color: var(--text-strong);
  background: var(--surface);
}

.search-input input {
  width: 100%;
  padding-left: 34px;
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
}

.search-hint {
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.9rem;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.product-card {
  display: grid;
  grid-template-columns: auto 60px 1fr;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--surface);
  align-items: center;
  cursor: pointer;
}

.product-card input {
  justify-self: center;
}

.product-card.checked {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(45, 127, 249, 0.2);
}

.product-thumb {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  overflow: hidden;
  background: var(--surface-weak);
  border: 1px solid var(--border-color);
}

.product-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.product-name {
  font-weight: 900;
  color: var(--text-strong);
}

.product-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.9rem;
}

.link {
  background: none;
  border: none;
  padding: 0;
  color: var(--primary-color);
  cursor: pointer;
  font-weight: 800;
  text-decoration: underline;
}

.modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1300;
  padding: 16px;
}

.modal__content {
  background: var(--surface);
  border-radius: 16px;
  padding: 18px;
  max-width: 520px;
  width: 100%;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.modal__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: var(--text-strong);
  font-weight: 700;
  line-height: 1.5;
}

@media (max-width: 720px) {
  .field-grid {
    grid-template-columns: 1fr;
  }

  .product-card {
    grid-template-columns: auto 50px 1fr;
  }

  .product-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }
}
</style>
