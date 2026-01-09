<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import PageContainer from '../components/PageContainer.vue'
import PageHeader from '../components/PageHeader.vue'
import { getMyOrderDetail } from '../api/orders'

const router = useRouter()
const route = useRoute()
const receipt = ref<{
  orderId: string
  createdAt: string
  items: Array<{
    productId: string
    name: string
    quantity: number
    price: number
  }>
  status?: string
  shipping: {
    buyerName: string
    zipcode: string
    address1: string
    address2: string
  }
  paymentMethodLabel: string
  totals: {
    total: number
  }
} | null>(null)
const isLoading = ref(false)
const errorMessage = ref('')

const formatPrice = (value: number) => `${value.toLocaleString('ko-KR')}??

const statusConfig = computed(() => {
  const status = receipt.value?.status
  if (status === 'PAYMENT_PENDING' || status === 'CREATED') {
    return {
      title: '寃곗젣 ?湲?以?,
      description: '寃곗젣 ?뺤씤 ??二쇰Ц???꾨즺?⑸땲??',
      ctaLabel: '二쇰Ц ?댁뿭 蹂닿린',
      ctaPath: '/my/orders',
    }
  }
  if (status === 'COMPLETED' || status === 'PAID') {
    return {
      title: '二쇰Ц???꾨즺?섏뿀?듬땲??,
      description: '二쇰Ц ?댁뿭?먯꽌 諛곗넚 ?뺣낫瑜??뺤씤?섏꽭??',
      ctaLabel: '二쇰Ц ?댁뿭 蹂닿린',
      ctaPath: '/my/orders',
    }
  }
  if (
    status === 'CANCELED' ||
    status === 'CANCELLED' ||
    status === 'CANCEL_REQUESTED' ||
    status === 'REFUNDED' ||
    status === 'REFUND_REJECTED'
  ) {
    return {
      title: '二쇰Ц??痍⑥냼?섏뿀?듬땲??,
      description: '痍⑥냼??二쇰Ц?낅땲??',
      ctaLabel: '硫붿씤?쇰줈 ?대룞',
      ctaPath: '/',
    }
  }
  return {
    title: '寃곗젣媛 ?꾨즺?섏뿀?댁슂.',
    description: '二쇰Ц ?뺣낫媛 以鍮꾨릺?덉뒿?덈떎.',
    ctaLabel: '硫붿씤?쇰줈 ?대룞',
    ctaPath: '/',
  }
})

const loadOrderDetail = async (orderId: string) => {
  if (!orderId) {
    errorMessage.value = '二쇰Ц ?뺣낫瑜?李얠쓣 ???놁뒿?덈떎.'
    receipt.value = null
    return
  }

  const numericId = Number(orderId)
  if (!Number.isFinite(numericId)) {
    errorMessage.value = '二쇰Ц ?뺣낫瑜?李얠쓣 ???놁뒿?덈떎.'
    receipt.value = null
    return
  }

  isLoading.value = true
  errorMessage.value = ''
  receipt.value = null
  try {
    const response = await getMyOrderDetail(numericId)
    if (!response?.order_id) {
      errorMessage.value = '二쇰Ц ?뺣낫瑜?李얠쓣 ???놁뒿?덈떎.'
      receipt.value = null
      return
    }
    const items = response.items.map((item, index) => ({
      productId: String(item.product_id),
      name: `?곹뭹 ${index + 1}`,
      quantity: item.quantity,
      price: item.unit_price,
    }))
    const total = Number(response.order_amount ?? 0) || 0
    receipt.value = {
      orderId: response.order_number || String(response.order_id),
      createdAt: response.created_at,
      items,
      status: response.status ?? undefined,
      shipping: {
        buyerName: '',
        zipcode: '',
        address1: '',
        address2: '',
      },
      paymentMethodLabel: '토스페이',
      totals: {
        total,
      },
    }
  } catch (error: any) {
    const status = error?.response?.status
    if (status === 401 || status === 403) {
      router.push('/login').catch(() => {})
      return
    }
    if (status === 404) {
      errorMessage.value = '二쇰Ц ?뺣낫瑜?李얠쓣 ???놁뒿?덈떎.'
      receipt.value = null
      return
    }
    errorMessage.value = '二쇰Ц ?뺣낫瑜?遺덈윭?????놁뒿?덈떎.'
    receipt.value = null
  } finally {
    isLoading.value = false
  }
}

watch(
  () => route.params.orderId,
  (orderId) => {
    loadOrderDetail(String(orderId ?? ''))
  },
  { immediate: true },
)

const handlePrimaryAction = () => {
  router.push(statusConfig.value.ctaPath).catch(() => {})
}
</script>

<template>
  <PageContainer>
    <PageHeader eyebrow="DESKIT" title="二쇰Ц ?꾨즺" />

    <div class="checkout-steps">
      <span class="checkout-step">01 ?λ컮援щ땲</span>
      <span class="checkout-step__divider">></span>
      <span class="checkout-step">02 二쇰Ц/寃곗젣</span>
      <span class="checkout-step__divider">></span>
      <span class="checkout-step checkout-step--active">03 二쇰Ц ?꾨즺</span>
    </div>

    <div v-if="isLoading" class="checkout-empty">
      <p>二쇰Ц ?뺣낫瑜?遺덈윭?ㅻ뒗 以묒엯?덈떎.</p>
    </div>

    <div v-else-if="!receipt" class="checkout-empty">
      <p>{{ errorMessage || '二쇰Ц ?뺣낫媛 ?놁뒿?덈떎.' }}</p>
      <RouterLink to="/cart" class="link">?λ컮援щ땲濡??대룞</RouterLink>
      <RouterLink to="/" class="link">硫붿씤?쇰줈 ?대룞</RouterLink>
    </div>

    <section v-else class="panel">
      <div class="success">
        <div class="success-icon">??/div>
        <div>
          <h2 class="success-title">{{ statusConfig.title }}</h2>
          <p class="success-desc">{{ statusConfig.description }} 二쇰Ц踰덊샇 : {{ receipt.orderId }}</p>
        </div>
      </div>

      <p class="meta">
        {{ receipt.shipping.buyerName || '怨좉컼' }} 怨좉컼?섏쓽
        {{ receipt.items[0]?.name }} ??{{ Math.max(receipt.items.length - 1, 0) }}嫄댁뿉 ???寃곗젣 ?댁뿭?낅땲??
      </p>

      <div class="table">
        <div class="table-head">
          <span>?쒕쾲</span>
          <span>?곹뭹紐?/span>
          <span>媛쒖닔</span>
          <span>媛寃?/span>
          <span>寃곗젣 湲덉븸</span>
        </div>
        <div
          v-for="(item, idx) in receipt.items"
          :key="item.productId"
          class="table-row"
        >
          <span>{{ idx + 1 }}</span>
          <span class="ellipsis">{{ item.name }}</span>
          <span>{{ item.quantity }}</span>
          <span>{{ formatPrice(item.price) }}</span>
          <span>{{ formatPrice(item.price * item.quantity) }}</span>
        </div>
      </div>

      <div class="summary">
        <div class="summary-row">
          <span>珥?寃곗젣 湲덉븸</span>
          <strong>{{ formatPrice(receipt.totals.total) }}</strong>
        </div>
        <div class="summary-row">
          <span>寃곗젣 ?섎떒</span>
          <strong>{{ receipt.paymentMethodLabel }}</strong>
        </div>
        <div class="summary-row">
          <span>諛곗넚 ?μ냼</span>
          <strong>
            {{ receipt.shipping.zipcode }} {{ receipt.shipping.address1 }} {{ receipt.shipping.address2 }}
          </strong>
        </div>
        <div class="summary-row">
          <span>?섎졊??/span>
          <strong>{{ receipt.shipping.buyerName || '怨좉컼' }}</strong>
        </div>
      </div>

      <div class="actions">
        <button type="button" class="btn primary" @click="handlePrimaryAction">
          {{ statusConfig.ctaLabel }}
        </button>
      </div>
    </section>
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
  padding: 18px;
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

.panel {
  border: 1px solid var(--border-color);
  background: var(--surface);
  border-radius: 16px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.success {
  display: flex;
  align-items: center;
  gap: 12px;
}

.success-icon {
  font-size: 1.5rem;
}

.success-title {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 900;
}

.success-desc {
  margin: 2px 0 0;
  color: var(--text-muted);
  font-weight: 700;
}

.meta {
  margin: 0;
  color: var(--text-muted);
}

.table {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: 60px 1fr 80px 120px 140px;
  gap: 8px;
  padding: 10px 12px;
  align-items: center;
}

.table-head {
  background: var(--surface-weak);
  font-weight: 800;
}

.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary {
  border-top: 1px solid var(--border-color);
  padding-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.summary-row strong {
  font-variant-numeric: tabular-nums;
}

.actions {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}

.btn {
  border: 1px solid var(--border-color);
  background: var(--primary-color);
  color: #fff;
  font-weight: 900;
  border-radius: 12px;
  padding: 12px 16px;
  cursor: pointer;
}

@media (max-width: 900px) {
  .table-head,
  .table-row {
    grid-template-columns: 40px 1fr 60px 100px 120px;
  }
}

@media (max-width: 640px) {
  .table-head,
  .table-row {
    grid-template-columns: 32px 1fr 52px 80px 100px;
    font-size: 0.92rem;
  }
}
</style>
