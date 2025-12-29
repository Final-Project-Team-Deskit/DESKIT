<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '../../components/PageHeader.vue'
import { type DbProduct } from '../../lib/products-data'
import { getAuthUser } from '../../lib/auth'
import { getSellerProducts } from '../../composables/useSellerProducts'
import { getSellerMockProducts, SELLER_PRODUCTS_EVENT } from '../../lib/mocks/sellerProducts'
import { USE_MOCK_API } from '../../api/config'

type ProductStatus = 'selling' | 'soldout' | 'hidden'

type StatusFilter = 'all' | ProductStatus

type SortOption = 'name' | 'status'

const STATUS_KEY = 'deskit_seller_product_status_v1'

const router = useRouter()
const sellerId = ref<number | null>(null)
const statusFilter = ref<StatusFilter>('all')
const sortOption = ref<SortOption>('name')
const searchQuery = ref('')
const statusMap = ref<Record<string, ProductStatus>>({})
const baseProducts = ref<DbProduct[]>([])

const statusLabelMap: Record<ProductStatus, string> = {
  selling: '판매중',
  soldout: '품절',
  hidden: '숨김',
}

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

const loadStatusMap = () => {
  const raw = localStorage.getItem(STATUS_KEY)
  if (!raw) return
  try {
    const parsed = JSON.parse(raw) as Record<string, string>
    const next: Record<string, ProductStatus> = {}
    Object.entries(parsed).forEach(([key, value]) => {
      if (value === 'selling' || value === 'soldout' || value === 'hidden') {
        next[key] = value
      }
    })
    statusMap.value = next
  } catch {
    return
  }
}

const saveStatusMap = () => {
  localStorage.setItem(STATUS_KEY, JSON.stringify(statusMap.value))
}

const getProductKey = (product: any) => {
  return String(product?.product_id ?? product?.id ?? '')
}

const getStatus = (productKey: string | number): ProductStatus => {
  return statusMap.value[String(productKey)] || 'selling'
}

const setStatus = (productKey: string | number, status: ProductStatus) => {
  statusMap.value = {
    ...statusMap.value,
    [String(productKey)]: status,
  }
  saveStatusMap()
}

const sellerProducts = computed(() => {
  return baseProducts.value
})

const localProducts = computed(() => {
  if (!sellerId.value) return []
  return getSellerProducts(sellerId.value)
})

const mergedProducts = computed(() => {
  const map = new Map<string, any>()
  sellerProducts.value.forEach((product) => {
    map.set(getProductKey(product), product)
  })
  localProducts.value.forEach((product) => {
    map.set(getProductKey(product), product)
  })
  return Array.from(map.values())
})

const filteredProducts = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  const filtered = mergedProducts.value.filter((product: any) => {
    const name = (product.name || '').toLowerCase()
    const desc = (product.short_desc ?? product.shortDesc ?? '').toLowerCase()
    const match = !q || name.includes(q) || desc.includes(q)
    if (!match) return false
    const status = getStatus(getProductKey(product))
    if (statusFilter.value !== 'all' && statusFilter.value !== status) return false
    return true
  })

  if (sortOption.value === 'name') {
    return filtered.slice().sort((a, b) => a.name.localeCompare(b.name))
  }
  if (sortOption.value === 'status') {
    const order: Record<ProductStatus, number> = { selling: 0, soldout: 1, hidden: 2 }
    return filtered.slice().sort((a, b) => order[getStatus(getProductKey(a))] - order[getStatus(getProductKey(b))])
  }
  return filtered
})

const formatPrice = (value: number) => `${value.toLocaleString('ko-KR')}원`

const getDiscountPercent = (product: DbProduct | any) => {
  const cost = product.cost_price ?? product.costPrice ?? 0
  const price = product.price ?? 0
  if (!cost || cost <= price) return 0
  return Math.round(((cost - price) / cost) * 100)
}

const getStockCount = (product: DbProduct | any) => {
  if (typeof product.stock === 'number') return product.stock
  const base = product.salesVolume ?? product.product_id * 7
  return Math.max(0, 200 - (base % 200))
}

const handleCreate = () => {
  router.push('/seller/products/create').catch(() => {})
}

const handleEdit = (product: any) => {
  const key = getProductKey(product)
  if (!key) return
  router.push(`/seller/products/${key}/edit`).catch(() => {})
}

const refreshProducts = () => {
  if (!sellerId.value) {
    baseProducts.value = []
    return
  }
  baseProducts.value = getSellerMockProducts(sellerId.value)
}

onMounted(() => {
  sellerId.value = deriveSellerId()
  loadStatusMap()
  if (USE_MOCK_API) {
    window.addEventListener(SELLER_PRODUCTS_EVENT, refreshProducts)
  }
})

watch(sellerId, refreshProducts, { immediate: true })

onBeforeUnmount(() => {
  if (USE_MOCK_API) {
    window.removeEventListener(SELLER_PRODUCTS_EVENT, refreshProducts)
  }
})
</script>

<template>
  <div>
    <PageHeader eyebrow="DESKIT" title="상품관리" />
    <header class="page-head">
      <div>
        <h2 class="section-title">나의 판매 목록({{ filteredProducts.length }})</h2>
        <p class="ds-section-sub">판매 중인 상품 상태를 관리할 수 있습니다.</p>
      </div>
      <div class="head-actions">
        <button type="button" class="btn primary" @click="handleCreate">상품등록</button>
      </div>
    </header>

    <section class="controls ds-surface">
      <label class="control-field">
        <span class="control-label">상태</span>
        <select v-model="statusFilter">
          <option value="all">전체</option>
          <option value="selling">판매중</option>
          <option value="soldout">품절</option>
          <option value="hidden">숨김</option>
        </select>
      </label>
      <label class="control-field">
        <span class="control-label">정렬</span>
        <select v-model="sortOption">
          <option value="name">상품이름순</option>
          <option value="status">상태순</option>
        </select>
      </label>
      <label class="control-field search">
        <span class="control-label">검색</span>
        <input v-model="searchQuery" type="search" placeholder="상품명 또는 설명 검색" />
      </label>
    </section>

    <section v-if="!sellerId" class="empty-state ds-surface">
      <p>로그인이 필요합니다. 판매자 계정으로 로그인해주세요.</p>
    </section>
    <section v-else-if="filteredProducts.length === 0" class="empty-state ds-surface">
      <p>등록된 판매 상품이 없습니다.</p>
    </section>
    <section v-else class="product-list">
      <article v-for="product in filteredProducts" :key="getProductKey(product)" class="product-card ds-surface">
        <div class="thumb">
          <img v-if="product.imageUrl || product.images?.[0]" :src="product.imageUrl || product.images?.[0]" :alt="product.name" />
          <div v-else class="thumb__placeholder"></div>
        </div>
        <div class="product-main">
          <div class="product-title">{{ product.name }}</div>
          <p class="product-desc">{{ product.short_desc ?? product.shortDesc }}</p>
          <div class="product-prices">
            <span class="price-original">{{ formatPrice(product.cost_price ?? product.costPrice ?? 0) }}</span>
            <span class="price-sale">{{ formatPrice(product.price) }}</span>
            <span v-if="getDiscountPercent(product) > 0" class="price-discount">
              -{{ getDiscountPercent(product) }}%
            </span>
          </div>
        </div>
        <div class="product-side">
          <div class="stock">재고: {{ getStockCount(product) }}개</div>
          <select
            :value="getStatus(getProductKey(product))"
            class="status-select"
            @change="setStatus(getProductKey(product), ($event.target as HTMLSelectElement).value as ProductStatus)"
          >
            <option value="selling">판매중</option>
            <option value="soldout">품절</option>
            <option value="hidden">숨김</option>
          </select>
          <div class="edit-group">
            <button
              type="button"
              class="btn btn-compact"
              @click="handleEdit(product)"
            >
              수정
            </button>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.breadcrumb {
  font-size: 0.85rem;
  color: var(--text-muted);
  font-weight: 700;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.head-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.controls {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
}

.control-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 140px;
}

.control-field.search {
  flex: 1 1 220px;
}

.control-label {
  font-weight: 800;
  color: var(--text-strong);
  font-size: 0.85rem;
}

select,
input[type='search'] {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 8px 10px;
  font-weight: 700;
  color: var(--text-strong);
  background: var(--surface);
}

.product-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.product-card {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr) 200px;
  gap: 16px;
  padding: 16px;
  align-items: center;
}

.thumb {
  width: 140px;
  height: 110px;
  border-radius: 12px;
  overflow: hidden;
  background: var(--surface-weak);
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.thumb__placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #1f2937, #0f172a);
}

.product-main {
  min-width: 0;
}

.product-title {
  font-weight: 900;
  color: var(--text-strong);
  font-size: 1rem;
}

.product-desc {
  margin: 6px 0 10px;
  color: var(--text-muted);
  font-weight: 700;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-prices {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.price-original {
  text-decoration: line-through;
  color: var(--text-soft);
  font-weight: 700;
}

.price-sale {
  color: var(--text-strong);
  font-weight: 900;
}

.price-discount {
  color: #ef4444;
  font-weight: 800;
}

.product-side {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-end;
}

.stock {
  font-weight: 800;
  color: var(--text-strong);
}

.status-select {
  min-width: 120px;
}

.edit-group {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.inline-hint {
  font-size: 0.8rem;
  color: var(--text-muted);
  font-weight: 700;
}

.empty-state {
  padding: 18px;
  text-align: center;
  font-weight: 700;
  color: var(--text-muted);
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

.btn-compact {
  padding: 6px 12px;
  line-height: 1.1;
  font-size: 0.85rem;
  min-height: 28px;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 960px) {
  .product-card {
    grid-template-columns: 120px minmax(0, 1fr);
  }

  .product-side {
    align-items: flex-start;
  }
}

@media (max-width: 720px) {
  .page-head {
    flex-direction: column;
  }

  .head-actions {
    align-items: flex-start;
  }

  .product-card {
    grid-template-columns: 1fr;
  }

  .thumb {
    width: 100%;
    height: 180px;
  }
}
</style>
