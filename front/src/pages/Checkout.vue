<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import PageContainer from '../components/PageContainer.vue'
import PageHeader from '../components/PageHeader.vue'
import {
  loadCheckout,
  clearCheckout,
  updateShipping,
  updatePaymentMethod,
  type CheckoutDraft,
  type ShippingInfo,
  type PaymentMethod,
} from '../lib/checkout/checkout-storage'
import { removeCartItemsByProductIds } from '../lib/cart/cart-storage'
import { saveLastOrder, appendOrder, type OrderReceipt } from '../lib/order/order-storage'

const router = useRouter()
const route = useRoute()

const draft = ref<CheckoutDraft | null>(null)

const form = reactive<ShippingInfo>({
  buyerName: '',
  zipcode: '',
  address1: '',
  address2: '',
})
const paymentMethod = ref<PaymentMethod | null>(null)

const errors = reactive<Record<keyof ShippingInfo, string>>({
  buyerName: '',
  zipcode: '',
  address1: '',
  address2: '',
})

const step = computed<'shipping' | 'payment'>(() =>
    route.query.step === 'payment' ? 'payment' : 'shipping',
)

const items = computed(() => draft.value?.items ?? [])
const listPriceTotal = computed(() =>
    items.value.reduce((sum, item) => {
      const base =
          item.originalPrice && item.originalPrice > item.price ? item.originalPrice : item.price
      return sum + base * item.quantity
    }, 0),
)
const salePriceTotal = computed(() =>
    items.value.reduce((sum, item) => sum + item.price * item.quantity, 0),
)
const discountTotal = computed(() => {
  const diff = listPriceTotal.value - salePriceTotal.value
  return diff > 0 ? diff : 0
})
const shippingFee = computed(() => {
  if (items.value.length === 0) return 0
  return salePriceTotal.value >= 50000 ? 0 : 3000
})
const total = computed(() => salePriceTotal.value + shippingFee.value)
const totalQuantity = computed(() =>
    items.value.reduce((sum, item) => sum + item.quantity, 0),
)

const formatPrice = (value: number) => `${value.toLocaleString('ko-KR')}원`

const refreshDraft = () => {
  draft.value = loadCheckout()
  if (!draft.value) return

  form.buyerName = draft.value.shipping?.buyerName ?? ''
  form.zipcode = draft.value.shipping?.zipcode ?? ''
  form.address1 = draft.value.shipping?.address1 ?? ''
  form.address2 = draft.value.shipping?.address2 ?? ''
  paymentMethod.value = draft.value.paymentMethod ?? null
}

const persistField = (field: keyof ShippingInfo, value: string) => {
  let sanitized = value
  if (field === 'buyerName') {
    sanitized = sanitized.replace(/^\s+/, '').slice(0, 6)
  } else if (field === 'zipcode') {
    sanitized = sanitized.replace(/[^0-9]/g, '').slice(0, 5)
  }

  form[field] = sanitized
  const updated = updateShipping({[field]: sanitized} as Partial<ShippingInfo>)
  if (updated) {
    draft.value = updated
  }
}

const persistPayment = (method: PaymentMethod) => {
  paymentMethod.value = method
  const updated = updatePaymentMethod(method)
  if (updated) {
    draft.value = updated
  }
}

const validate = () => {
  errors.buyerName = ''
  errors.zipcode = ''
  errors.address1 = ''
  errors.address2 = ''

  const name = form.buyerName.trim()
  const zip = form.zipcode.trim()
  const addr1 = form.address1.trim()
  const addr2 = form.address2.trim()

  if (!name) errors.buyerName = '주문자 이름을 입력해주세요.'
  else if (!/^[a-zA-Z가-힣 ]{1,6}$/.test(name)) {
    errors.buyerName = '이름은 한글/영문 6글자 이내로 입력해주세요.'
  }

  if (!zip) errors.zipcode = '우편번호를 입력해주세요.'
  else if (!/^[0-9]{5}$/.test(zip)) errors.zipcode = '우편번호는 5자리 숫자입니다.'

  if (!addr1) errors.address1 = '주소를 입력해주세요.'
  // address2 optional

  form.buyerName = name
  form.zipcode = zip
  form.address1 = addr1
  form.address2 = addr2

  return !errors.buyerName && !errors.zipcode && !errors.address1 && !errors.address2
}

const canProceed = computed(() => {
  const name = form.buyerName.trim()
  const zip = form.zipcode.trim()
  const addr1 = form.address1.trim()
  return (
      /^[a-zA-Z가-힣 ]{1,6}$/.test(name) &&
      /^[0-9]{5}$/.test(zip) &&
      !!addr1 &&
      paymentMethod.value !== null
  )
})

const handleNext = () => {
  if (!validate()) return
  updateShipping({...form})
  router.push({path: '/checkout', query: {step: 'payment'}}).catch(() => {
  })
}

const handleBack = () => {
  router.back()
}

const goShipping = () => {
  router.push({path: '/checkout'}).catch(() => {
  })
}

const generateOrderId = () => {
  const now = new Date()
  const yy = String(now.getFullYear()).slice(2)
  const mm = String(now.getMonth() + 1).padStart(2, '0')
  const dd = String(now.getDate()).padStart(2, '0')
  const rand = Math.random().toString(16).slice(2, 6).toUpperCase()
  return `ORD-${yy}${mm}${dd}-${rand}`
}

const handlePaymentComplete = () => {
  const current = draft.value ?? loadCheckout()
  if (!current || !current.items || current.items.length === 0) {
    router.push('/cart')
    return
  }

  const listPriceTotal = current.items.reduce((sum, item) => {
    const base =
        item.originalPrice && item.originalPrice > item.price ? item.originalPrice : item.price
    return sum + base * item.quantity
  }, 0)
  const salePriceTotal = current.items.reduce(
      (sum, item) => sum + item.price * item.quantity,
      0,
  )
  const discountTotal = Math.max(0, listPriceTotal - salePriceTotal)
  const shippingFee =
      salePriceTotal >= 50000 ? 0 : current.items.length > 0 ? 3000 : 0

  const receipt: OrderReceipt = {
    orderId: generateOrderId(),
    createdAt: new Date().toISOString(),
    items: current.items.map((item) => ({
      productId: item.productId,
      name: item.name,
      quantity: item.quantity,
      price: item.price,
      originalPrice:
          item.originalPrice && item.originalPrice > item.price
              ? item.originalPrice
              : item.price,
      discountRate: item.discountRate ?? 0,
    })),
    shipping: {...current.shipping},
    status: 'PAID',
    paymentMethodLabel: '토스페이(예정)',
    totals: {
      listPriceTotal,
      salePriceTotal,
      discountTotal,
      shippingFee,
      total: salePriceTotal + shippingFee,
    },
  }

  saveLastOrder(receipt)
  appendOrder(receipt)
  removeCartItemsByProductIds(current.items.map((it) => it.productId))
  clearCheckout()
  router.push({name: 'order-complete'}).catch(() => router.push('/order/complete'))
}

const storageRefreshHandler = () => refreshDraft()

onMounted(() => {
  refreshDraft()
  window.addEventListener('deskit-checkout-updated', storageRefreshHandler)
  window.addEventListener('storage', storageRefreshHandler)
})

onBeforeUnmount(() => {
  window.removeEventListener('deskit-checkout-updated', storageRefreshHandler)
  window.removeEventListener('storage', storageRefreshHandler)
})
</script>

<template>
  <PageContainer>
    <PageHeader eyebrow="DESKIT" title="주문/결제"/>

    <div class="checkout-steps">
      <span class="checkout-step">01 장바구니</span>
      <span class="checkout-step__divider">></span>
      <span class="checkout-step checkout-step--active">02 주문/결제</span>
      <span class="checkout-step__divider">></span>
      <span class="checkout-step">03 주문 완료</span>
    </div>

    <div v-if="!draft" class="checkout-empty">
      <p>준비된 체크아웃 정보가 없습니다.</p>
      <RouterLink to="/cart" class="link">장바구니로 돌아가기</RouterLink>
    </div>

    <div v-else class="checkout-layout">
      <div v-if="step === 'shipping'" class="left-col">
        <div class="left-stack">
          <section class="panel panel--form">
            <div class="panel__header">
              <div>
                <p class="eyebrow">배송 정보</p>
                <h3 class="panel__title">배송지 정보를 입력해주세요.</h3>
              </div>
            </div>

            <div class="form">
              <div class="field">
                <label for="buyerName">주문자 이름</label>
                <input
                    id="buyerName"
                    type="text"
                    :value="form.buyerName"
                    placeholder="한글 6글자 이내"
                    maxlength="6"
                    @input="persistField('buyerName', ($event.target as HTMLInputElement).value)"
                    @blur="validate"
                />
                <p v-if="errors.buyerName" class="error">{{ errors.buyerName }}</p>
              </div>

              <div class="field">
                <label for="zipcode">우편번호</label>
                <input
                    id="zipcode"
                    type="text"
                    :value="form.zipcode"
                    placeholder="12345"
                    maxlength="5"
                    inputmode="numeric"
                    pattern="\\d{5}"
                    @input="persistField('zipcode', ($event.target as HTMLInputElement).value)"
                    @blur="validate"
                />
                <p v-if="errors.zipcode" class="error">{{ errors.zipcode }}</p>
              </div>

              <div class="field">
                <label for="address1">주소</label>
                <input
                    id="address1"
                    type="text"
                    :value="form.address1"
                    placeholder="서울시 강남구 강남대로 123"
                    @input="persistField('address1', ($event.target as HTMLInputElement).value)"
                    @blur="validate"
                />
                <p v-if="errors.address1" class="error">{{ errors.address1 }}</p>
              </div>

              <div class="field">
                <label for="address2">상세주소</label>
                <input
                    id="address2"
                    type="text"
                    :value="form.address2"
                    placeholder="아파트 101호"
                    @input="persistField('address2', ($event.target as HTMLInputElement).value)"
                />
              </div>
            </div>
          </section>

          <section class="panel panel--form">
            <div class="panel__header">
              <div>
                <p class="eyebrow">결제 수단</p>
                <h3 class="panel__title">결제 방법을 선택해주세요.</h3>
              </div>
            </div>

            <div class="payment-options">
              <label
                  class="payment-option"
                  :class="{ 'payment-option--active': paymentMethod === 'CARD' }"
              >
                <input
                    type="radio"
                    name="payment"
                    value="CARD"
                    :checked="paymentMethod === 'CARD'"
                    @change="persistPayment('CARD')"
                />
                <div>
                  <p class="option-title">신용카드</p>
                  <p class="option-desc">일반 카드 결제</p>
                </div>
              </label>
              <label
                  class="payment-option"
                  :class="{ 'payment-option--active': paymentMethod === 'EASY_PAY' }"
              >
                <input
                    type="radio"
                    name="payment"
                    value="EASY_PAY"
                    :checked="paymentMethod === 'EASY_PAY'"
                    @change="persistPayment('EASY_PAY')"
                />
                <div>
                  <p class="option-title">간편결제</p>
                  <p class="option-desc">간편 결제 서비스</p>
                </div>
              </label>
              <label
                  class="payment-option"
                  :class="{ 'payment-option--active': paymentMethod === 'TRANSFER' }"
              >
                <input
                    type="radio"
                    name="payment"
                    value="TRANSFER"
                    :checked="paymentMethod === 'TRANSFER'"
                    @change="persistPayment('TRANSFER')"
                />
                <div>
                  <p class="option-title">계좌이체</p>
                  <p class="option-desc">무통장/계좌이체</p>
                </div>
              </label>
            </div>
          </section>
        </div>

        <div class="actions actions--left">
          <button type="button" class="btn ghost" @click="handleBack">뒤로가기</button>
          <button type="button" class="btn primary" :disabled="!canProceed" @click="handleNext">
            다음
          </button>
        </div>
      </div>

      <section v-else class="panel panel--form">
        <div class="panel__header">
          <div>
            <p class="eyebrow">결제 방법</p>
            <h3 class="panel__title">결제 영역은 토스페이 API 연동 후 구성됩니다.</h3>
          </div>
        </div>

        <div class="payment-placeholder">
          <p>결제 영역은 토스페이 API 연동 후 구성됩니다.</p>
        </div>

        <div class="actions">
          <button type="button" class="btn ghost" @click="goShipping">뒤로가기</button>
          <button type="button" class="btn primary" @click="handlePaymentComplete">결제 완료</button>
        </div>
      </section>

      <aside class="panel panel--summary">

        <h3 class="panel__title">주문 예상 금액</h3>
        <p class="summary-meta-text">총 {{ items.length }}종 / {{ totalQuantity }}개</p>

        <div class="summary-row">
          <span>총 상품 금액(정가)</span>
          <strong class="amount">{{ formatPrice(listPriceTotal) }}</strong>
        </div>

        <div class="summary-row">
          <span>총 할인 금액</span>
          <strong class="amount" :class="{ discount: discountTotal > 0 }">
            {{ discountTotal > 0 ? `-${formatPrice(discountTotal)}` : '-' }}
          </strong>
        </div>

        <div class="summary-row">
          <span>상품 금액(할인 적용)</span>
          <strong class="amount">{{ formatPrice(salePriceTotal) }}</strong>
        </div>

        <div class="summary-row">
          <span>배송비</span>
          <strong class="amount">{{ items.length === 0 ? '-' : formatPrice(shippingFee) }}</strong>
        </div>
        <p v-if="items.length" class="summary-helper">5만원 이상 무료배송</p>

        <div class="summary-total">
          <span>총 결제 금액</span>
          <strong class="amount total-amount">
            {{ items.length === 0 ? '-' : formatPrice(total) }}
          </strong>
        </div>
      </aside>
    </div>
  </PageContainer>
</template>

<style scoped>
.checkout-steps {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 14px;
  color: var(--text-muted);
  font-weight: 700;
}

.checkout-step {
  padding: 4px 8px;
  border-radius: 8px;
  background: var(--surface-weak);
}

.checkout-step--active {
  background: var(--hover-bg);
  color: var(--primary-color);
}

.checkout-step__divider {
  color: var(--text-soft);
}

.checkout-empty {
  border: 1px dashed var(--border-color);
  padding: 16px;
  border-radius: 12px;
  color: var(--text-muted);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.link {
  color: var(--primary-color);
  font-weight: 800;
}

.checkout-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.65fr);
  gap: 18px;
  align-items: start;
}

.panel {
  border: 1px solid var(--border-color);
  background: var(--surface);
  border-radius: 16px;
  padding: 16px;
  box-sizing: border-box;
}

.left-col {
  display: flex;
  flex-direction: column;
}

.left-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.eyebrow {
  margin: 0;
  color: var(--text-soft);
  font-weight: 800;
  letter-spacing: 0.04em;
}

.panel__title {
  margin: 6px 0 0;
  font-size: 1.15rem;
  font-weight: 900;
  color: var(--text-strong);
}

.form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 10px;
}

.field label {
  display: block;
  margin-bottom: 6px;
  font-weight: 800;
  color: var(--text-strong);
}

.field input {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 10px 12px;
  background: var(--surface-weak);
  color: var(--text-strong);
  outline: none;
  box-sizing: border-box;
}

.field input:focus {
  border-color: var(--primary-color);
  background: var(--surface);
}

.error {
  margin: 6px 0 0;
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--danger-color, #dc2626);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}

.btn {
  padding: 10px 14px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: var(--surface);
  font-weight: 900;
  cursor: pointer;
}

.btn.ghost {
  color: var(--text-strong);
}

.btn.primary {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: #fff;
}

.btn.primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.payment-placeholder {
  border: 1px dashed var(--border-color);
  border-radius: 12px;
  padding: 16px;
  color: var(--text-muted);
  background: var(--surface-weak);
  margin-top: 10px;
}

.payment-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.payment-option {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 12px 14px;
  background: var(--surface);
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.payment-option:hover {
  background: var(--surface-weak);
}

.payment-option--active {
  border-color: var(--primary-color);
  background: var(--hover-bg, var(--surface-weak));
}

.payment-option input {
  margin: 0;
}

.option-title {
  margin: 0;
  font-weight: 800;
}

.option-desc {
  margin: 2px 0 0;
  color: var(--text-muted);
  font-size: 0.9rem;
}

.panel--summary {
  position: sticky;
  top: 80px;
  align-self: start;
}

.summary-meta-text {
  margin: 6px 0 14px;
  color: var(--text-muted);
  font-weight: 800;
}

.summary-row,
.summary-total {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: var(--text-strong);
  padding: 8px 0;
}

.summary-helper {
  margin: 2px 0 10px;
  color: var(--text-muted);
  font-size: 0.9rem;
}

.summary-total {
  margin-top: 6px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
  font-size: 1.1rem;
  font-weight: 800;
}

.amount {
  font-variant-numeric: tabular-nums;
}

.discount {
  color: var(--primary-color);
  font-weight: 800;
}

.total-amount {
  font-size: 1.1rem;
  font-weight: 800;
}

@media (max-width: 1080px) {
  .checkout-layout {
    grid-template-columns: 1fr;
  }

  .panel--summary {
    position: static;
    order: -1;
  }

  .actions--left {
    justify-content: flex-start;
  }
}
</style>
