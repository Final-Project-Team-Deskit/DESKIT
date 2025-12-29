<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { cancelAdminReservation, getAdminReservationDetail, type AdminReservationDetail } from '../../../lib/mocks/adminReservations'

const route = useRoute()
const router = useRouter()

const detail = ref<AdminReservationDetail | null>(null)

const reservationId = computed(() => (typeof route.params.reservationId === 'string' ? route.params.reservationId : ''))

const loadDetail = () => {
  detail.value = getAdminReservationDetail(reservationId.value)
}

const goBack = () => {
  router.back()
}

const goToList = () => {
  router.push('/admin/live?tab=scheduled').catch(() => {})
}

const handleCancel = () => {
  if (!detail.value) return
  if (detail.value.status === '취소됨') return
  if (!window.confirm('정말 예약을 취소하시겠습니까?')) return
  cancelAdminReservation(detail.value.id)
  detail.value = { ...detail.value, status: '취소됨' }
  router.push('/admin/live?tab=scheduled').catch(() => {})
}

const isCancelled = computed(() => detail.value?.status === '취소됨')

watch(reservationId, loadDetail, { immediate: true })
</script>

<template>
  <div v-if="detail" class="detail-wrap">
    <h2 class="page-title">예약 상세</h2>
    <header class="detail-header">
      <button type="button" class="back-link" @click="goBack">← 뒤로 가기</button>
      <div class="detail-actions">
        <button type="button" class="btn" @click="goToList">목록으로</button>
      </div>
    </header>

    <section class="detail-card ds-surface">
      <div class="detail-title">
        <h3>{{ detail.title }}</h3>
        <span class="status-pill">{{ detail.status }}</span>
      </div>
      <div class="detail-meta">
        <p><span>방송 예정 시간</span>{{ detail.datetime }}</p>
        <p><span>카테고리</span>{{ detail.category }}</p>
        <p><span>판매자</span>{{ detail.sellerName }}</p>
      </div>
    </section>

    <section class="detail-card ds-surface notice-box">
      <h4>공지사항</h4>
      <p>{{ detail.notice }}</p>
    </section>

    <section class="detail-card ds-surface">
      <div class="card-head">
        <h4>판매 상품 리스트</h4>
      </div>
      <div class="table-wrap">
        <table class="product-table">
          <thead>
            <tr>
              <th>상품명</th>
              <th>정가</th>
              <th>할인가</th>
              <th>수량</th>
              <th>재고</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in detail.products" :key="item.id">
              <td>{{ item.name }}</td>
              <td>{{ item.price }}</td>
              <td class="sale">{{ item.salePrice }}</td>
              <td>{{ item.qty }}</td>
              <td>{{ item.stock }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div class="detail-footer">
      <div class="footer-actions">
        <button
          type="button"
          class="btn danger"
          :disabled="isCancelled"
          @click="handleCancel"
        >
          {{ isCancelled ? '취소됨' : '예약 취소' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.detail-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 900;
  color: var(--text-strong);
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.back-link {
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-weight: 800;
  cursor: pointer;
  padding: 6px 0;
}

.detail-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.detail-card {
  padding: 18px;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-title h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 900;
  color: var(--text-strong);
}

.status-pill {
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--surface-weak);
  color: var(--text-strong);
  font-weight: 800;
  font-size: 0.85rem;
}

.detail-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 8px 16px;
  font-weight: 700;
  color: var(--text-muted);
}

.detail-meta span {
  display: inline-block;
  min-width: 120px;
  color: var(--text-strong);
  font-weight: 800;
  margin-right: 6px;
}

.notice-box h4 {
  margin: 0;
  font-size: 1rem;
  font-weight: 900;
  color: var(--text-strong);
}

.notice-box p {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
}

.card-head h4 {
  margin: 0;
  font-size: 1rem;
  font-weight: 900;
  color: var(--text-strong);
}

.table-wrap {
  overflow-x: auto;
}

.product-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 520px;
}

.product-table th,
.product-table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--border-color);
  text-align: left;
  font-weight: 700;
  color: var(--text-strong);
  white-space: nowrap;
}

.product-table th {
  font-size: 0.9rem;
  color: var(--text-muted);
}

.sale {
  color: #ef4444;
  font-weight: 900;
}

.detail-footer {
  display: flex;
  justify-content: flex-end;
}

.footer-actions {
  display: flex;
  gap: 10px;
}

.btn {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 999px;
  padding: 10px 16px;
  font-weight: 800;
  cursor: pointer;
}

.btn.danger {
  background: #ef4444;
  border-color: transparent;
  color: #fff;
}

@media (max-width: 720px) {
  .detail-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
