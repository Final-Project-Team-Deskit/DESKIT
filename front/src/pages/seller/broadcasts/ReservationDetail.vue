<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '../../../components/PageContainer.vue'
import { getSellerReservationDetail, type SellerReservationDetail } from '../../../lib/mocks/sellerReservations'

const route = useRoute()
const router = useRouter()

const detail = computed<SellerReservationDetail>(() => {
  const id = typeof route.params.reservationId === 'string' ? route.params.reservationId : ''
  return getSellerReservationDetail(id)
})

const goBack = () => {
  router.back()
}

const goToList = () => {
  router.push('/seller/live?tab=scheduled').catch(() => {})
}

const openCueCard = () => {
  console.log('[reservation] cue card', detail.value.id)
}

const thumbPreview = ref<string | null>(null)
const standbyPreview = ref<string | null>(null)

const setPreview = (type: 'thumb' | 'standby', event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const url = URL.createObjectURL(file)
  if (type === 'thumb') {
    if (thumbPreview.value) URL.revokeObjectURL(thumbPreview.value)
    thumbPreview.value = url
  } else {
    if (standbyPreview.value) URL.revokeObjectURL(standbyPreview.value)
    standbyPreview.value = url
  }
}

const handleEdit = () => {
  console.log('[reservation] edit', detail.value.id)
}

const handleCancel = () => {
  console.log('[reservation] cancel', detail.value.id)
}

onBeforeUnmount(() => {
  if (thumbPreview.value) URL.revokeObjectURL(thumbPreview.value)
  if (standbyPreview.value) URL.revokeObjectURL(standbyPreview.value)
})
</script>

<template>
  <PageContainer>
    <h2 class="page-title">예약 상세</h2>
    <header class="detail-header">
      <button type="button" class="back-link" @click="goBack">← 뒤로 가기</button>
      <div class="detail-actions">
        <button type="button" class="btn ghost" @click="openCueCard">큐카드 보기</button>
        <button type="button" class="btn" @click="goToList">목록으로</button>
      </div>
    </header>

    <section class="detail-card ds-surface">
      <div class="detail-title">
        <h2>{{ detail.title }}</h2>
        <span class="status-pill">{{ detail.status }}</span>
      </div>
      <div class="detail-meta">
        <p><span>방송 예정 시간</span>{{ detail.datetime }}</p>
        <p><span>카테고리</span>{{ detail.category }}</p>
      </div>
    </section>

    <section class="detail-card ds-surface notice-box">
      <h3>공지사항</h3>
      <p>{{ detail.notice }}</p>
    </section>

    <section class="detail-card ds-surface">
      <div class="card-head">
        <h3>방송 준비</h3>
      </div>
      <div class="upload-grid">
        <div class="upload-col">
          <p class="upload-label">썸네일</p>
          <div class="upload-preview">
            <img v-if="thumbPreview" :src="thumbPreview" alt="썸네일 미리보기" />
          </div>
          <input type="file" accept="image/*" @change="setPreview('thumb', $event)" />
          <p class="upload-help">권장: 16:9, JPG/PNG</p>
        </div>
        <div class="upload-col">
          <p class="upload-label">대기화면</p>
          <div class="upload-preview">
            <img v-if="standbyPreview" :src="standbyPreview" alt="대기화면 미리보기" />
          </div>
          <input type="file" accept="image/*" @change="setPreview('standby', $event)" />
          <p class="upload-help">권장: 16:9, JPG/PNG</p>
        </div>
      </div>
    </section>

    <section class="detail-card ds-surface">
      <div class="card-head">
        <h3>판매 상품 리스트</h3>
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
        <button type="button" class="btn primary" @click="handleEdit">예약 수정</button>
        <button type="button" class="btn danger" @click="handleCancel">예약 취소</button>
      </div>
    </div>
  </PageContainer>
</template>

<style scoped>
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-title {
  margin: 0 0 8px;
  font-size: 1.5rem;
  font-weight: 900;
  color: var(--text-strong);
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

.btn {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 999px;
  padding: 10px 16px;
  font-weight: 800;
  cursor: pointer;
}

.btn.ghost {
  background: transparent;
  color: var(--text-muted);
}

.btn.primary {
  background: var(--primary-color);
  color: #fff;
  border-color: transparent;
}

.btn.danger {
  background: #ef4444;
  color: #fff;
  border-color: transparent;
}

.detail-card {
  padding: 18px;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-title h2 {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 900;
  color: var(--text-strong);
}

.status-pill {
  border: 1px solid var(--border-color);
  border-radius: 999px;
  padding: 4px 10px;
  font-weight: 800;
  color: var(--primary-color);
  background: var(--surface-weak);
}

.detail-meta p {
  margin: 0 0 6px;
  color: var(--text-muted);
  font-weight: 700;
  display: flex;
  gap: 12px;
}

.detail-meta span {
  min-width: 120px;
  color: var(--text-strong);
  font-weight: 800;
}

.notice-box h3 {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 900;
  color: var(--text-strong);
}

.notice-box p {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
  line-height: 1.5;
}

.upload-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.upload-col {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.upload-label {
  margin: 0;
  font-weight: 800;
  color: var(--text-strong);
}

.upload-preview {
  height: 180px;
  border-radius: 12px;
  background: var(--surface-weak);
  border: 1px solid var(--border-color);
  display: grid;
  place-items: center;
  overflow: hidden;
}

.upload-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.upload-help {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.85rem;
}

.card-head h3 {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 900;
  color: var(--text-strong);
}

.table-wrap {
  overflow-x: auto;
}

.product-table {
  width: 100%;
  min-width: 520px;
  border-collapse: collapse;
}

.product-table th,
.product-table td {
  padding: 12px;
  border-bottom: 1px solid var(--border-color);
  text-align: left;
  font-weight: 700;
  color: var(--text-strong);
}

.product-table thead th {
  background: var(--surface-weak);
  font-weight: 900;
}

.sale {
  color: #ef4444;
  font-weight: 900;
}

.detail-footer {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 16px;
}

.footer-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

@media (max-width: 720px) {
  .detail-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .detail-actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .upload-grid {
    grid-template-columns: 1fr;
  }

  .footer-actions {
    flex-direction: column;
    align-items: stretch;
    width: 100%;
  }
}
</style>
