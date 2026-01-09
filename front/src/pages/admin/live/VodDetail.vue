<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '../../../components/PageContainer.vue'
import ConfirmModal from '../../../components/ConfirmModal.vue'
import {
  fetchAdminBroadcastDetail,
  fetchAdminBroadcastReport,
  fetchRecentLiveChats,
  type BroadcastDetailResponse,
  type BroadcastResult,
} from '../../../lib/live/api'

const route = useRoute()
const router = useRouter()

const vodId = computed(() => (typeof route.params.vodId === 'string' ? route.params.vodId : ''))

type AdminVodDetail = {
  id: string
  title: string
  startedAt: string
  endedAt: string
  statusLabel: string
  stopReason?: string
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
const isPlaying = ref(false)
const isFullscreen = ref(false)
const showDeleteConfirm = ref(false)

const showChat = ref(false)
const chatText = ref('')
const chatMessages = ref<{ id: string; user: string; text: string; time: string }[]>([])

const videoRef = ref<HTMLVideoElement | null>(null)
const playerContainerRef = ref<HTMLElement | null>(null)

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
  showDeleteConfirm.value = true
}

const confirmDelete = () => {
  window.alert('VOD가 삭제되었습니다. (데모)')
}

const sendChat = () => {
  if (!chatText.value.trim()) return
  chatMessages.value = [...chatMessages.value, { id: `c-${Date.now()}`, user: '관리자', text: chatText.value, time: '방금' }]
  chatText.value = ''
}

const startPlayback = () => {
  if (!isVodPlayable.value) return
  isPlaying.value = true
  nextTick(() => {
    videoRef.value?.play?.()
  })
}

const toggleFullscreen = () => {
  const target = playerContainerRef.value
  if (!target) return
  if (document.fullscreenElement) {
    document.exitFullscreen().catch(() => {})
    return
  }
  target.requestFullscreen?.()
}

const handleFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement
}

onMounted(() => {
  document.addEventListener('fullscreenchange', handleFullscreenChange)
})

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})

watch(isVodPlayable, (playable) => {
  if (!playable) {
    showChat.value = false
    isPlaying.value = false
  }
})

const formatDateTime = (value?: string) => (value ? value.replace('T', ' ') : '')

const formatVisibility = (vodStatus?: string) => (vodStatus === 'PUBLIC' ? '공개' : '비공개')

const toNumber = (value: number | string | undefined) => {
  if (typeof value === 'number') return value
  if (typeof value === 'string') {
    const parsed = Number(value)
    return Number.isNaN(parsed) ? 0 : parsed
  }
  return 0
}

const formatChatTime = (timestamp?: number) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const hours = date.getHours()
  const displayHour = hours % 12 || 12
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${hours >= 12 ? '오후' : '오전'} ${displayHour}:${minutes}`
}

const buildDetail = (broadcast: BroadcastDetailResponse, report: BroadcastResult): AdminVodDetail => ({
  id: String(report.broadcastId),
  title: report.title ?? broadcast.title ?? '',
  startedAt: formatDateTime(report.startAt ?? broadcast.startedAt),
  endedAt: formatDateTime(report.endAt),
  statusLabel: report.status ?? broadcast.status ?? '',
  stopReason: report.stoppedReason ?? broadcast.stoppedReason ?? undefined,
  sellerName: broadcast.sellerName ?? '',
  thumb: broadcast.thumbnailUrl ?? '',
  metrics: {
    maxViewers: report.maxViewers ?? 0,
    reports: report.reportCount ?? 0,
    sanctions: report.sanctionCount ?? 0,
    likes: report.totalLikes ?? 0,
    totalRevenue: toNumber(report.totalSales),
  },
  vod: {
    url: report.vodUrl ?? undefined,
    visibility: formatVisibility(report.vodStatus),
  },
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
  const [broadcast, report, chats] = await Promise.all([
    fetchAdminBroadcastDetail(idValue),
    fetchAdminBroadcastReport(idValue),
    fetchRecentLiveChats(idValue, 3600).catch(() => []),
  ])
  detail.value = buildDetail(broadcast, report)
  chatMessages.value = chats.map((item) => ({
    id: `${item.sentAt}-${item.sender}`,
    user: item.sender || item.memberEmail || '시청자',
    text: item.content,
    time: formatChatTime(item.sentAt),
  }))
}

watch(vodId, () => {
  loadDetail()
}, { immediate: true })
</script>

<template>
  <PageContainer v-if="detail">
    <header class="detail-header">
      <button type="button" class="back-link" @click="goBack">← 뒤로 가기</button>
      <button type="button" class="btn ghost" @click="goToList">목록으로</button>
    </header>

    <h2 class="page-title">방송 결과 리포트</h2>

    <section class="detail-card ds-surface">
      <div class="info-grid">
        <div class="thumb-box">
          <img :src="detail.thumb" :alt="detail.title" />
        </div>
        <div class="info-meta">
          <h3>{{ detail.title }}</h3>
          <p><span>방송 시작 시간</span>{{ detail.startedAt }}</p>
          <p><span>방송 종료 시간</span>{{ detail.endedAt }}</p>
          <p><span>상태</span>{{ detail.statusLabel }}</p>
          <p v-if="detail.statusLabel === 'STOPPED' && detail.stopReason"><span>중지 사유</span>{{ detail.stopReason }}</p>
          <p><span>판매자</span>{{ detail.sellerName }}</p>
        </div>
      </div>
    </section>

    <section class="kpi-grid">
      <article class="kpi-card ds-surface">
        <p class="kpi-label">최대 시청자 수</p>
        <p class="kpi-value">{{ detail.metrics.maxViewers.toLocaleString('ko-KR') }}명</p>
      </article>
      <article class="kpi-card ds-surface">
        <p class="kpi-label">신고 건수</p>
        <p class="kpi-value">{{ detail.metrics.reports }}</p>
      </article>
      <article class="kpi-card ds-surface">
        <p class="kpi-label">시청자 제재 건수</p>
        <p class="kpi-value">{{ detail.metrics.sanctions }}</p>
      </article>
      <article class="kpi-card ds-surface">
        <p class="kpi-label">좋아요</p>
        <p class="kpi-value">{{ detail.metrics.likes.toLocaleString('ko-KR') }}</p>
      </article>
      <article class="kpi-card ds-surface">
        <p class="kpi-label">총 매출</p>
        <p class="kpi-value">₩{{ detail.metrics.totalRevenue.toLocaleString('ko-KR') }}</p>
      </article>
    </section>

    <section class="detail-card ds-surface">
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
      <div class="vod-player" :class="{ 'with-chat': showChat && isVodPlayable }">
        <div ref="playerContainerRef" class="player-shell">
          <div class="player-frame">
            <video
              v-if="isVodPlayable"
              v-show="isPlaying"
              ref="videoRef"
              :src="detail.vod.url"
              controls
              :poster="detail.thumb"
            ></video>
            <div v-else class="vod-placeholder">
              <span>재생할 VOD가 없습니다.</span>
            </div>
            <div v-if="isVodPlayable && !isPlaying" class="player-poster">
              <img :src="detail.thumb" :alt="detail.title" />
              <button type="button" class="play-toggle" @click="startPlayback" title="재생">
                <svg aria-hidden="true" class="icon" viewBox="0 0 24 24" focusable="false">
                  <polygon points="8 5 19 12 8 19 8 5" fill="currentColor" />
                </svg>
              </button>
            </div>
            <div v-if="isVodPlayable" class="player-overlay">
              <div class="overlay-left">
                <button
                  type="button"
                  class="icon-circle ghost"
                  :class="{ active: isFullscreen }"
                  @click="toggleFullscreen"
                  :title="isFullscreen ? '전체화면 종료' : '전체화면'"
                >
                  <svg v-if="!isFullscreen" aria-hidden="true" class="icon" viewBox="0 0 24 24" focusable="false">
                    <path d="M15 3h6v6" />
                    <path d="M9 21H3v-6" />
                    <path d="M21 3 14 10" />
                    <path d="M3 21 10 14" />
                  </svg>
                  <svg v-else aria-hidden="true" class="icon" viewBox="0 0 24 24" focusable="false">
                    <path d="M9 9H3V3" />
                    <path d="m3 9 6-6" />
                    <path d="M15 15h6v6" />
                    <path d="m21 15-6 6" />
                  </svg>
                </button>
              </div>
              <div class="overlay-right">
                <div class="chat-pill">
                  <span class="chat-count">{{ chatMessages.length }}</span>
                  <span class="chat-label">채팅 기록</span>
                </div>
                <button
                  type="button"
                  class="icon-circle"
                  :class="{ active: showChat }"
                  @click="showChat = !showChat"
                  :title="showChat ? '채팅 닫기' : '채팅 열기'"
                >
                  <svg aria-hidden="true" class="icon" viewBox="0 0 24 24" focusable="false">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2Z" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>
        <aside v-if="showChat && isVodPlayable" class="chat-panel ds-surface">
          <header class="chat-head">
            <h4>채팅</h4>
            <button type="button" class="icon-circle ghost" @click="showChat = false" title="채팅 닫기">
              <svg aria-hidden="true" class="icon" viewBox="0 0 24 24" focusable="false">
                <path d="M18 6 6 18" />
                <path d="m6 6 12 12" />
              </svg>
            </button>
          </header>
          <div class="chat-list">
            <div v-for="msg in chatMessages" :key="msg.id" class="chat-row">
              <div class="chat-meta">
                <span class="chat-user">{{ msg.user }}</span>
                <span class="chat-time">{{ msg.time }}</span>
              </div>
              <p class="chat-text">{{ msg.text }}</p>
            </div>
          </div>
          <div class="chat-input">
            <input v-model="chatText" type="text" placeholder="메시지 입력" />
            <button type="button" class="btn primary" @click="sendChat">전송</button>
          </div>
        </aside>
      </div>
    </section>

    <section class="detail-card ds-surface">
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
              <td>₩{{ item.price.toLocaleString('ko-KR') }}</td>
              <td>{{ item.soldQty }}</td>
              <td>₩{{ item.revenue.toLocaleString('ko-KR') }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
    <ConfirmModal
      v-model="showDeleteConfirm"
      title="VOD 삭제"
      description="VOD를 삭제하시겠습니까? 영구 삭제되어 복구할 수 없습니다."
      confirm-text="삭제"
      @confirm="confirmDelete"
    />
  </PageContainer>
</template>

<style scoped>
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.page-title {
  margin: 0 0 16px;
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

.detail-card {
  padding: 18px;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 16px;
}

.info-grid {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.thumb-box {
  width: 180px;
  height: 120px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  background: var(--surface-weak);
}

.thumb-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.info-meta h3 {
  margin: 0 0 10px;
  font-size: 1.1rem;
  font-weight: 900;
  color: var(--text-strong);
}

.info-meta p {
  margin: 0 0 6px;
  color: var(--text-muted);
  font-weight: 700;
  display: flex;
  gap: 12px;
}

.info-meta span {
  min-width: 120px;
  color: var(--text-strong);
  font-weight: 800;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.kpi-card {
  padding: 16px;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.kpi-label {
  color: var(--text-muted);
  font-weight: 700;
}

.kpi-value {
  font-size: 1.2rem;
  font-weight: 900;
  color: var(--text-strong);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.vod-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.visibility-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--text-muted);
}

.visibility-label {
  font-weight: 800;
}

.vod-switch {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.vod-switch input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.switch-track {
  width: 46px;
  height: 26px;
  border-radius: 999px;
  background: var(--surface-weak);
  border: 1px solid var(--border-color);
  display: inline-flex;
  align-items: center;
  padding: 2px;
  transition: background 0.2s ease;
}

.vod-switch input:checked + .switch-track {
  background: rgba(35, 107, 246, 0.25);
}

.switch-thumb {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--primary-color);
  transform: translateX(0);
  transition: transform 0.2s ease;
}

.vod-switch input:checked + .switch-track .switch-thumb {
  transform: translateX(20px);
}

.icon-pill {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.icon-pill.danger {
  color: var(--danger-color);
  border-color: rgba(220, 38, 38, 0.4);
}

.vod-player {
  display: flex;
  gap: 16px;
  align-items: stretch;
}

.vod-player.with-chat {
  align-items: stretch;
}

.player-shell {
  flex: 1;
}

.player-frame {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  background: #000;
  min-height: 320px;
}

.player-frame video {
  width: 100%;
  height: 100%;
  display: block;
}

.vod-placeholder {
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.player-poster {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.player-poster img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.85;
}

.play-toggle {
  position: absolute;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.player-overlay {
  position: absolute;
  inset: 16px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  pointer-events: none;
}

.icon-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.4);
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  pointer-events: auto;
}

.icon-circle.ghost {
  background: rgba(255, 255, 255, 0.1);
}

.icon-circle.active {
  border-color: rgba(255, 255, 255, 0.9);
}

.chat-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 700;
  pointer-events: auto;
}

.chat-count {
  font-weight: 900;
}

.chat-panel {
  width: 320px;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
}

.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 320px;
  overflow-y: auto;
}

.chat-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chat-meta {
  display: flex;
  justify-content: space-between;
  color: var(--text-muted);
  font-size: 0.85rem;
  font-weight: 700;
}

.chat-text {
  margin: 0;
  font-weight: 700;
  color: var(--text-strong);
}

.chat-input {
  display: flex;
  gap: 8px;
}

.chat-input input {
  flex: 1;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  padding: 8px 10px;
}

.table-wrap {
  overflow-x: auto;
}

.product-table {
  width: 100%;
  border-collapse: collapse;
}

.product-table th,
.product-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid var(--border-color);
  font-weight: 700;
}

.product-table th {
  color: var(--text-muted);
}

.icon {
  width: 18px;
  height: 18px;
  stroke: currentColor;
  stroke-width: 1.5;
  fill: none;
}

.icon.muted {
  stroke: currentColor;
}

@media (max-width: 1200px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .vod-player {
    flex-direction: column;
  }

  .chat-panel {
    width: 100%;
  }
}
</style>
