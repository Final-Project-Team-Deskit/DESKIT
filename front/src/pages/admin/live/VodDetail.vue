<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAdminVodDetail } from '../../../lib/mocks/adminVods'

const route = useRoute()
const router = useRouter()

const vodId = computed(() => (typeof route.params.vodId === 'string' ? route.params.vodId : ''))
const detail = computed(() => getAdminVodDetail(vodId.value))

const goBack = () => {
  router.back()
}

const goToList = () => {
  router.push('/admin/live?tab=vod').catch(() => {})
}

const formatNumber = (value: number) => value.toLocaleString('ko-KR')
</script>

<template>
  <div class="vod-wrap">
    <header class="vod-header">
      <button type="button" class="back-link" @click="goBack">← 뒤로 가기</button>
      <button type="button" class="btn" @click="goToList">목록으로</button>
    </header>

    <h2 class="page-title">방송 결과 리포트</h2>

    <section class="vod-card ds-surface">
      <div class="vod-info">
        <div class="vod-thumb">
          <img :src="detail.thumb" :alt="detail.title" />
        </div>
        <div class="vod-meta">
          <h3>{{ detail.title }}</h3>
          <p><span>방송 시작 시간</span>{{ detail.startedAt }}</p>
          <p><span>방송 종료 시간</span>{{ detail.endedAt }}</p>
          <p><span>상태</span>{{ detail.statusLabel }}</p>
          <p><span>판매자</span>{{ detail.sellerName }}</p>
        </div>
      </div>
    </section>

    <section class="kpi-grid">
      <article class="kpi-card ds-surface">
        <p class="kpi-label">최대 시청자 수</p>
        <p class="kpi-value">{{ formatNumber(detail.metrics.maxViewers) }}명</p>
      </article>
      <article class="kpi-card ds-surface">
        <p class="kpi-label">신고 건수</p>
        <p class="kpi-value">{{ formatNumber(detail.metrics.reports) }}</p>
      </article>
      <article class="kpi-card ds-surface">
        <p class="kpi-label">시청자 제재 건수</p>
        <p class="kpi-value">{{ formatNumber(detail.metrics.sanctions) }}</p>
      </article>
      <article class="kpi-card ds-surface">
        <p class="kpi-label">좋아요</p>
        <p class="kpi-value">{{ formatNumber(detail.metrics.likes) }}</p>
      </article>
      <article class="kpi-card ds-surface">
        <p class="kpi-label">총 매출</p>
        <p class="kpi-value">₩{{ formatNumber(detail.metrics.totalRevenue) }}</p>
      </article>
    </section>

    <section class="vod-card ds-surface">
      <div class="card-head">
        <h3>VOD</h3>
      </div>
      <div class="vod-player">
        <video v-if="detail.vod.url" :src="detail.vod.url" controls></video>
        <div v-else class="vod-placeholder">VOD 미리보기 영역</div>
      </div>
    </section>

    <section class="vod-card ds-surface">
      <div class="card-head">
        <h3>상품별 성과</h3>
      </div>
      <div class="table-wrap">
        <table class="product-table">
          <thead>
            <tr>
              <th>상품명</th>
              <th>가격</th>
              <th>판매 수량</th>
              <th>매출</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in detail.productResults" :key="item.id">
              <td>{{ item.name }}</td>
              <td>₩{{ formatNumber(item.price) }}</td>
              <td>{{ formatNumber(item.soldQty) }}</td>
              <td>₩{{ formatNumber(item.revenue) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.vod-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.vod-header {
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

.page-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 900;
  color: var(--text-strong);
}

.vod-card {
  padding: 18px;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.vod-info {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
}

.vod-thumb {
  width: 200px;
  height: 140px;
  border-radius: 12px;
  overflow: hidden;
  background: var(--surface-weak);
}

.vod-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.vod-meta h3 {
  margin: 0 0 8px;
  font-size: 1.2rem;
  font-weight: 900;
  color: var(--text-strong);
}

.vod-meta p {
  margin: 4px 0;
  color: var(--text-muted);
  font-weight: 700;
}

.vod-meta span {
  display: inline-block;
  min-width: 120px;
  color: var(--text-strong);
  font-weight: 800;
  margin-right: 6px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.kpi-card {
  padding: 16px;
  border-radius: 14px;
}

.kpi-label {
  margin: 0 0 6px;
  font-weight: 800;
  color: var(--text-muted);
}

.kpi-value {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 900;
  color: var(--text-strong);
}

.card-head h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 900;
  color: var(--text-strong);
}

.vod-player {
  border-radius: 14px;
  overflow: hidden;
  background: var(--surface-weak);
  aspect-ratio: 16 / 9;
  display: grid;
  place-items: center;
}

.vod-player video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.vod-placeholder {
  color: var(--text-muted);
  font-weight: 700;
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

.btn {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 999px;
  padding: 10px 16px;
  font-weight: 800;
  cursor: pointer;
}

@media (max-width: 900px) {
  .vod-info {
    grid-template-columns: 1fr;
  }

  .vod-thumb {
    width: 100%;
    height: 180px;
  }
}
</style>
