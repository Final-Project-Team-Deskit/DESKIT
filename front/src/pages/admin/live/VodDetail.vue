<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchAdminBroadcastDetail, fetchAdminBroadcastReport, type BroadcastDetailResponse, type BroadcastResult } from '../../../lib/live/api'

const route = useRoute()
const router = useRouter()

const vodId = computed(() => (typeof route.params.vodId === 'string' ? route.params.vodId : ''))

type AdminVodDetail = {
  id: string
  title: string
  startedAt: string
  endedAt: string
  statusLabel: string
  sellerName: string
  thumb: string
  metrics: {
    maxViewers: number
    reports: number
    sanctions: number
    likes: number
    totalRevenue: number
  }
  vod: { url?: string; visibility: string }
  productResults: Array<{ id: string; name: string; price: number; soldQty: number; revenue: number }>
}

const detail = ref<AdminVodDetail | null>(null)
const isVodPlayable = computed(() => !!detail.value?.vod?.url)
const isVodPublic = computed(() => detail.value?.vod.visibility === '공개')

const formatDateTime = (value?: string) => (value ? value.replace('T', ' ') : '')

const toNumber = (value: number | string | undefined) => {
  if (typeof value === 'number') return value
  if (typeof value === 'string') {
    const parsed = Number(value)
    return Number.isNaN(parsed) ? 0 : parsed
  }
  return 0
}

const buildDetail = (broadcast: BroadcastDetailResponse, report: BroadcastResult): AdminVodDetail => ({
  id: String(report.broadcastId),
  title: report.title ?? broadcast.title ?? '',
  startedAt: formatDateTime(report.startAt ?? broadcast.startedAt),
  endedAt: formatDateTime(report.endAt),
  statusLabel: report.status ?? broadcast.status ?? '',
  sellerName: broadcast.sellerName ?? '',
  thumb: broadcast.thumbnailUrl ?? '',
  metrics: {
    maxViewers: report.maxViewers ?? 0,
    reports: report.reportCount ?? 0,
    sanctions: report.sanctionCount ?? 0,
    likes: report.totalLikes ?? 0,
    totalRevenue: toNumber(report.totalSales),
  },
  vod: { url: report.vodUrl ?? undefined, visibility: '공개' },
  productResults: (report.productStats ?? []).map((item) => ({
    id: String(item.productId),
    name: item.productName,
    price: item.price ?? 0,
    soldQty: item.salesQuantity ?? 0,
    revenue: toNumber(item.salesAmount),
  })),
})

const loadDetail = async () => {
  const idValue = Number(vodId.value)
  if (!vodId.value || Number.isNaN(idValue)) {
    detail.value = null
    return
  }
  const [broadcast, report] = await Promise.all([
    fetchAdminBroadcastDetail(idValue),
    fetchAdminBroadcastReport(idValue),
  ])
  detail.value = buildDetail(broadcast, report)
}

const goBack = () => {
  router.back()
}

const goToList = () => {
  router.push('/admin/live?tab=vod').catch(() => {})
}

const toggleVisibility = () => {
  if (!detail.value) return
  const next = detail.value.vod.visibility === '공개' ? '비공개' : '공개'
  detail.value = { ...detail.value, vod: { ...detail.value.vod, visibility: next } }
}

const handleDownload = () => {
  window.alert('VOD 파일 다운로드를 시작합니다. (데모)')
}

const handleDelete = () => {
  if (!window.confirm('VOD를 삭제할까요?')) return
  window.alert('VOD가 삭제되었습니다. (데모)')
}

const formatNumber = (value: number) => value.toLocaleString('ko-KR')

watch(vodId, () => {
  loadDetail()
}, { immediate: true })
</script>

<template>
  <div v-if="detail" class="vod-wrap">
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
        <div class="vod-actions" v-if="isVodPlayable">
          <div class="visibility-toggle" aria-label="VOD 공개 설정">
            <svg aria-hidden="true" class="icon muted" viewBox="0 0 24 24" focusable="false">
              <path
                d="M14.12 14.12a3 3 0 0 1-4.24-4.24m6.83 6.83A9.6 9.6 0 0 1 12 17c-5 0-9-5-9-5a15.63 15.63 0 0 1 5.12-4.88m3.41-1.5A9.4 9.4 0 0 1 12 7c5 0 9 5 9 5a15.78 15.78 0 0 1-2.6 2.88"
              />
              <path d="m3 3 18 18" />
            </svg>
            <span class="visibility-label">비공개</span>
            <label class="vod-switch">
              <input type="checkbox" :checked="isVodPublic" @change="toggleVisibility" />
              <span class="switch-track"><span class="switch-thumb"></span></span>
            </label>
            <span class="visibility-label">공개</span>
            <svg aria-hidden="true" class="icon muted" viewBox="0 0 24 24" focusable="false">
              <path d="M1 12s4.5-7 11-7 11 7 11 7-4.5 7-11 7S1 12 1 12Z" />
              <circle cx="12" cy="12" r="3.5" />
            </svg>
          </div>
          <div class="vod-icon-actions">
            <button type="button" class="icon-pill" @click="handleDownload" title="다운로드">
              <svg aria-hidden="true" class="icon" viewBox="0 0 24 24" focusable="false">
                <path d="M12 3v12" />
                <path d="m6 11 6 6 6-6" />
                <path d="M5 19h14" />
              </svg>
            </button>
            <button type="button" class="icon-pill danger" @click="handleDelete" title="삭제">
              <svg aria-hidden="true" class="icon" viewBox="0 0 24 24" focusable="false">
                <path d="M3 6h18" />
                <path d="M19 6v12a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" />
                <path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                <line x1="10" x2="10" y1="11" y2="17" />
                <line x1="14" x2="14" y1="11" y2="17" />
              </svg>
            </button>
          </div>
        </div>
      </div>
      <div class="vod-player">
        <video v-if="detail.vod.url" :src="detail.vod.url" controls></video>
        <div v-else class="vod-placeholder">VOD가 존재하지 않습니다.</div>
      </div>
      <p class="vod-note">관리자가 비공개로 전환한 VOD는 판매자가 다시 공개로 바꿀 수 없습니다.</p>
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

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.card-head h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 900;
  color: var(--text-strong);
}

.vod-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.visibility-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.08), rgba(15, 23, 42, 0.02));
}

.vod-switch input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.vod-switch .switch-track {
  position: relative;
  display: inline-flex;
  align-items: center;
  width: 44px;
  height: 22px;
  background: var(--border-color);
  border-radius: 999px;
  transition: background 0.2s ease;
}

.vod-switch .switch-thumb {
  position: absolute;
  left: 3px;
  top: 3px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s ease;
}

.vod-switch input:checked + .switch-track {
  background: #22c55e;
}

.vod-switch input:checked + .switch-track .switch-thumb {
  transform: translateX(22px);
}

.visibility-label {
  font-weight: 900;
  color: var(--text-strong);
  font-size: 0.9rem;
}

.vod-icon-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.icon-pill {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: var(--surface);
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: transform 0.1s ease, box-shadow 0.1s ease, background 0.2s ease, border-color 0.2s ease;
}

.icon-pill:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.12);
}

.icon-pill.danger {
  border-color: rgba(239, 68, 68, 0.28);
  color: #ef4444;
  background: rgba(239, 68, 68, 0.05);
}

.icon {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.icon.muted {
  color: var(--text-muted);
}

.vod-player {
  border-radius: 14px;
  overflow: hidden;
  background: var(--surface-weak);
  aspect-ratio: 16 / 9;
  display: grid;
  place-items: center;
}

.vod-note {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
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
