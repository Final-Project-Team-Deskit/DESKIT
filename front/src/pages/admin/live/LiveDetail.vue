<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchAdminBroadcastDetail,
  fetchBroadcastProducts,
  fetchBroadcastStats,
  fetchRecentLiveChats,
  joinBroadcast,
  leaveBroadcast,
  stopAdminBroadcast,
} from '../../../lib/live/api'
import { parseLiveDate } from '../../../lib/live/utils'
import { useNow } from '../../../lib/live/useNow'
import { computeLifecycleStatus, getBroadcastStatusLabel, getScheduledEndMs, normalizeBroadcastStatus } from '../../../lib/broadcastStatus'
import { getAuthUser } from '../../../lib/auth'
import { resolveViewerId } from '../../../lib/live/viewer'

const route = useRoute()
const router = useRouter()

const liveId = computed(() => (typeof route.params.liveId === 'string' ? route.params.liveId : ''))

type AdminDetail = {
  id: string
  title: string
  sellerName: string
  startedAt: string
  scheduledAt?: string
  status: string
  waitScreenUrl?: string
  stoppedReason?: string
  viewers: number
  reports: number
  likes: number
  elapsed: string
}

const detail = ref<AdminDetail | null>(null)
const { now } = useNow(1000)

const stageRef = ref<HTMLDivElement | null>(null)
const isFullscreen = ref(false)
const showStopModal = ref(false)
const stopReason = ref('')
const stopDetail = ref('')
const error = ref('')
const showChat = ref(true)
const chatText = ref('')
const chatMessages = ref<{ id: string; user: string; text: string; time: string }[]>([])
const chatListRef = ref<HTMLDivElement | null>(null)
const showModerationModal = ref(false)
const moderationTarget = ref<{ user: string } | null>(null)
const moderationType = ref('')
const moderationReason = ref('')
const moderatedUsers = ref<Record<string, { type: string; reason: string; at: string }>>({})
const activePane = ref<'monitor' | 'products'>('monitor')
const liveProducts = ref<
  Array<{
    id: string
    name: string
    option: string
    price: string
    sale: string
    status: string
    thumb: string
    sold: number
    stock: number
  }>
>([])
const sseSource = ref<EventSource | null>(null)
const sseConnected = ref(false)
const sseRetryCount = ref(0)
const sseRetryTimer = ref<number | null>(null)
const statsTimer = ref<number | null>(null)
const refreshTimer = ref<number | null>(null)
const joinInFlight = ref(false)
const streamToken = ref<string | null>(null)
const viewerId = ref<string | null>(resolveViewerId(getAuthUser()))
const joinedBroadcastId = ref<number | null>(null)
const leaveRequested = ref(false)
const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const FALLBACK_IMAGE = 'data:image/gif;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs='

const reasonOptions = [
  '음란물',
  '폭력',
  '국가기밀 누설',
  '불쾌감/공포심/불안감 조성',
  '비방',
  '취급 불가 상품 판매',
  '사이트 운영정책에 맞지 않는 상품',
  '기타',
]

const goBack = () => {
  router.back()
}

const goToList = () => {
  router.push('/admin/live?tab=live').catch(() => {})
}

const formatElapsed = (startAt?: string) => {
  if (!startAt) return '00:00:00'
  const started = parseLiveDate(startAt)
  if (Number.isNaN(started.getTime())) return '00:00:00'
  const diff = Math.max(0, Date.now() - started.getTime())
  const totalSeconds = Math.floor(diff / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement | null
  if (!target || target.dataset.fallbackApplied) return
  target.dataset.fallbackApplied = 'true'
  target.src = FALLBACK_IMAGE
}

const formatChatTime = (timestamp?: number) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const hours = date.getHours()
  const displayHour = hours % 12 || 12
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${hours >= 12 ? '오후' : '오전'} ${displayHour}:${minutes}`
}

const mapLiveProduct = (item: {
  id: string
  name: string
  price: number
  isSoldOut: boolean
  imageUrl?: string
  totalQty?: number
  stockQty?: number
}) => {
  const totalQty = item.totalQty ?? item.stockQty ?? 0
  const stockQty = item.stockQty ?? totalQty
  const sold = Math.max(0, totalQty - stockQty)
  return {
    id: item.id,
    name: item.name,
    option: item.name,
    price: `₩${item.price.toLocaleString('ko-KR')}`,
    sale: `₩${item.price.toLocaleString('ko-KR')}`,
    status: item.isSoldOut ? '품절' : '판매중',
    thumb: item.imageUrl ?? '',
    sold,
    stock: stockQty,
  }
}

const sortedLiveProducts = computed(() => {
  return [...liveProducts.value].sort((a, b) => {
    const aSoldOut = a.status === '품절'
    const bSoldOut = b.status === '품절'
    if (aSoldOut !== bSoldOut) return aSoldOut ? 1 : -1
    return 0
  })
})

const loadDetail = async () => {
  if (!liveId.value) {
    detail.value = null
    liveProducts.value = []
    chatMessages.value = []
    return
  }
  const idValue = Number(liveId.value)
  if (Number.isNaN(idValue)) {
    detail.value = null
    return
  }

  try {
    const [detailResponse, statsResponse, productsResponse, chatResponse] = await Promise.all([
      fetchAdminBroadcastDetail(idValue),
      fetchBroadcastStats(idValue).catch(() => null),
      fetchBroadcastProducts(idValue).catch(() => []),
      fetchRecentLiveChats(idValue, 300).catch(() => []),
    ])

    const viewers = statsResponse?.viewerCount ?? detailResponse.totalViews ?? 0
    const likes = statsResponse?.likeCount ?? detailResponse.totalLikes ?? 0
    const reports = statsResponse?.reportCount ?? detailResponse.totalReports ?? 0
    const startedAt = detailResponse.startedAt ?? detailResponse.scheduledAt ?? ''
    const scheduledAt = detailResponse.scheduledAt ?? ''

    detail.value = {
      id: String(detailResponse.broadcastId),
      title: detailResponse.title,
      sellerName: detailResponse.sellerName ?? '',
      startedAt,
      scheduledAt,
      status: detailResponse.status ?? '',
      waitScreenUrl: detailResponse.waitScreenUrl ?? '',
      stoppedReason: detailResponse.stoppedReason ?? '',
      viewers,
      reports,
      likes,
      elapsed: formatElapsed(startedAt),
    }

    liveProducts.value = productsResponse.map((item) => mapLiveProduct(item))

    chatMessages.value = chatResponse.map((item) => ({
      id: `${item.sentAt}-${item.sender}`,
      user: item.sender || item.memberEmail || '시청자',
      text: item.content,
      time: formatChatTime(item.sentAt),
    }))
    await nextTick()
    const el = chatListRef.value
    if (el) {
      el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
    }
  } catch {
    detail.value = null
    liveProducts.value = []
    chatMessages.value = []
  }
}

const refreshStats = async (broadcastId: number) => {
  if (!detail.value) return
  try {
    const stats = await fetchBroadcastStats(broadcastId)
    detail.value = {
      ...detail.value,
      viewers: stats.viewerCount ?? detail.value.viewers,
      likes: stats.likeCount ?? detail.value.likes,
      reports: stats.reportCount ?? detail.value.reports,
    }
  } catch {
    return
  }
}

const refreshProducts = async (broadcastId: number) => {
  try {
    const products = await fetchBroadcastProducts(broadcastId)
    liveProducts.value = products.map((item) => mapLiveProduct(item))
  } catch {
    return
  }
}

const refreshDetail = async (broadcastId: number) => {
  try {
    const detailResponse = await fetchAdminBroadcastDetail(broadcastId)
    if (!detail.value) return
    const startedAt = detailResponse.startedAt ?? detailResponse.scheduledAt ?? ''
    const scheduledAt = detailResponse.scheduledAt ?? ''
    detail.value = {
      ...detail.value,
      title: detailResponse.title,
      sellerName: detailResponse.sellerName ?? detail.value.sellerName,
      startedAt,
      scheduledAt,
      status: detailResponse.status ?? detail.value.status,
      waitScreenUrl: detailResponse.waitScreenUrl ?? detail.value.waitScreenUrl,
      stoppedReason: detailResponse.stoppedReason ?? detail.value.stoppedReason,
      elapsed: formatElapsed(startedAt),
    }
  } catch {
    return
  }
}

const lifecycleStatus = computed(() => {
  if (!detail.value) return 'RESERVED'
  const baseTime = detail.value.startedAt || detail.value.scheduledAt
  const startAtMs = baseTime ? parseLiveDate(baseTime).getTime() : NaN
  const endAtMs = Number.isNaN(startAtMs) ? undefined : getScheduledEndMs(startAtMs)
  return computeLifecycleStatus({
    status: normalizeBroadcastStatus(detail.value.status),
    startAtMs: Number.isNaN(startAtMs) ? undefined : startAtMs,
    endAtMs,
  })
})

const statusLabel = computed(() => getBroadcastStatusLabel(lifecycleStatus.value))
const isInteractive = computed(() => lifecycleStatus.value === 'ON_AIR')
const isReadOnly = computed(() => lifecycleStatus.value !== 'ON_AIR')
const waitingScreenUrl = computed(() => detail.value?.waitScreenUrl ?? '')
const readyCountdownLabel = computed(() => {
  if (lifecycleStatus.value !== 'READY') return ''
  const baseTime = detail.value?.scheduledAt ?? detail.value?.startedAt
  if (!baseTime) return '방송 시작 대기 중'
  const startAtMs = parseLiveDate(baseTime).getTime()
  if (Number.isNaN(startAtMs)) return '방송 시작 대기 중'
  const diffMs = startAtMs - now.value.getTime()
  if (diffMs <= 0) return '방송 시작 대기 중'
  const totalSeconds = Math.ceil(diffMs / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${minutes}분 ${String(seconds).padStart(2, '0')}초 뒤 방송 시작`
})
const endedCountdownLabel = computed(() => {
  if (lifecycleStatus.value !== 'ENDED') return ''
  const baseTime = detail.value?.scheduledAt ?? detail.value?.startedAt
  if (!baseTime) return '방송 종료'
  const startAtMs = parseLiveDate(baseTime).getTime()
  if (Number.isNaN(startAtMs)) return '방송 종료'
  const scheduledEndMs = getScheduledEndMs(startAtMs)
  if (!scheduledEndMs) return '방송 종료'
  const diffMs = scheduledEndMs - now.value.getTime()
  if (diffMs <= 0) return '방송 종료'
  const totalSeconds = Math.ceil(diffMs / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `종료까지 ${minutes}분 ${String(seconds).padStart(2, '0')}초`
})
const playerMessage = computed(() => {
  if (lifecycleStatus.value === 'STOPPED') {
    return detail.value?.stoppedReason
      ? `방송이 운영 정책 위반으로 송출 중지되었습니다. (${detail.value.stoppedReason})`
      : '방송이 운영 정책 위반으로 송출 중지되었습니다.'
  }
  if (lifecycleStatus.value === 'ENDED') {
    return '방송이 종료되었습니다.'
  }
  if (lifecycleStatus.value === 'READY') {
    return readyCountdownLabel.value || '방송 시작 대기 중'
  }
  return ''
})

const requestJoinToken = async () => {
  if (!detail.value) return
  if (!['READY', 'ON_AIR'].includes(lifecycleStatus.value)) return
  if (joinInFlight.value) return
  joinInFlight.value = true
  try {
    streamToken.value = await joinBroadcast(Number(detail.value.id), viewerId.value)
    joinedBroadcastId.value = Number(detail.value.id)
  } catch {
    return
  } finally {
    joinInFlight.value = false
  }
}

const sendLeaveSignal = async (useBeacon = false) => {
  if (!joinedBroadcastId.value || !viewerId.value || leaveRequested.value) return
  leaveRequested.value = true
  const url = `${apiBase}/api/broadcasts/${joinedBroadcastId.value}/leave?viewerId=${encodeURIComponent(viewerId.value)}`
  if (useBeacon && navigator.sendBeacon) {
    navigator.sendBeacon(url)
    return
  }
  await leaveBroadcast(joinedBroadcastId.value, viewerId.value).catch(() => {})
}

const handlePageHide = () => {
  void sendLeaveSignal(true)
}

const parseSseData = (event: MessageEvent) => {
  if (!event.data) return null
  try {
    return JSON.parse(event.data)
  } catch {
    return event.data
  }
}

const buildStopConfirmMessage = (data: unknown) => {
  const reason = typeof data === 'string' && data.trim() ? data.trim() : '관리자에 의해 방송이 중지되었습니다.'
  return `${reason}\n방송에서 나가겠습니까?`
}

const scheduleRefresh = (broadcastId: number) => {
  if (refreshTimer.value) window.clearTimeout(refreshTimer.value)
  refreshTimer.value = window.setTimeout(() => {
    void refreshDetail(broadcastId)
    void refreshStats(broadcastId)
    void refreshProducts(broadcastId)
  }, 500)
}

const handleSseEvent = (event: MessageEvent) => {
  const idValue = Number(liveId.value)
  if (Number.isNaN(idValue)) return
  const data = parseSseData(event)
  switch (event.type) {
    case 'BROADCAST_READY':
    case 'BROADCAST_UPDATED':
    case 'BROADCAST_STARTED':
      scheduleRefresh(idValue)
      break
    case 'PRODUCT_PINNED':
    case 'PRODUCT_SOLD_OUT':
      scheduleRefresh(idValue)
      break
    case 'SANCTION_UPDATED':
      scheduleRefresh(idValue)
      break
    case 'BROADCAST_CANCELED':
      alert('방송이 자동 취소되었습니다.')
      goToList()
      break
    case 'BROADCAST_ENDED':
      alert('방송이 종료되었습니다.')
      void refreshDetail(idValue)
      break
    case 'BROADCAST_SCHEDULED_END':
      if (window.confirm('방송이 종료되었습니다.')) {
        goToList()
      }
      break
    case 'BROADCAST_STOPPED':
      if (detail.value) {
        detail.value.status = 'STOPPED'
      }
      scheduleRefresh(idValue)
      if (window.confirm(buildStopConfirmMessage(data))) {
        goToList()
      }
      break
    default:
      break
  }
}

const scheduleReconnect = (broadcastId: number) => {
  if (sseRetryTimer.value) window.clearTimeout(sseRetryTimer.value)
  const delay = Math.min(30000, 1000 * 2 ** sseRetryCount.value)
  const jitter = Math.floor(Math.random() * 500)
  sseRetryTimer.value = window.setTimeout(() => {
    connectSse(broadcastId)
  }, delay + jitter)
  sseRetryCount.value += 1
}

const connectSse = (broadcastId: number) => {
  if (sseSource.value) {
    sseSource.value.close()
  }
  const source = new EventSource(`${apiBase}/api/broadcasts/${broadcastId}/subscribe`)
  const events = [
    'BROADCAST_READY',
    'BROADCAST_UPDATED',
    'BROADCAST_STARTED',
    'PRODUCT_PINNED',
    'PRODUCT_SOLD_OUT',
    'SANCTION_UPDATED',
    'BROADCAST_ENDING_SOON',
    'BROADCAST_CANCELED',
    'BROADCAST_ENDED',
    'BROADCAST_SCHEDULED_END',
    'BROADCAST_STOPPED',
  ]
  events.forEach((name) => source.addEventListener(name, handleSseEvent))
  source.onopen = () => {
    sseConnected.value = true
    sseRetryCount.value = 0
    scheduleRefresh(broadcastId)
  }
  source.onerror = () => {
    sseConnected.value = false
    source.close()
    if (document.visibilityState === 'visible') {
      scheduleReconnect(broadcastId)
    }
  }
  sseSource.value = source
}

const startStatsPolling = (broadcastId: number) => {
  if (statsTimer.value) window.clearInterval(statsTimer.value)
  statsTimer.value = window.setInterval(() => {
    if (['READY', 'ON_AIR', 'ENDED'].includes(lifecycleStatus.value) || !sseConnected.value) {
      void refreshStats(broadcastId)
      if (!sseConnected.value) {
        void refreshProducts(broadcastId)
      }
    }
  }, 5000)
}

const openStopConfirm = () => {
  if (!detail.value || detail.value.status === 'STOPPED' || !isInteractive.value) return
  showStopModal.value = true
  error.value = ''
}

const closeStopModal = () => {
  showStopModal.value = false
  stopReason.value = ''
  stopDetail.value = ''
  error.value = ''
}

const handleStopSave = () => {
  if (!detail.value) return
  if (!stopReason.value) {
    error.value = '유형을 선택해주세요.'
    return
  }
  if (stopReason.value === '기타' && !stopDetail.value.trim()) {
    error.value = '중지 사유를 입력해주세요.'
    return
  }
  const ok = window.confirm('방송 송출을 중지하시겠습니까?')
  if (!ok) return
  const reason = stopReason.value === '기타' ? stopDetail.value.trim() : stopReason.value
  stopAdminBroadcast(Number(detail.value.id), reason)
    .then(() => {
      if (detail.value) {
        detail.value.status = 'STOPPED'
        detail.value.stoppedReason = reason
      }
      goToList()
    })
    .catch(() => {})
    .finally(() => {
      showStopModal.value = false
    })
}

const syncFullscreen = () => {
  isFullscreen.value = Boolean(document.fullscreenElement)
}

const modalHostTarget = computed(() => (isFullscreen.value && stageRef.value ? stageRef.value : 'body'))

const toggleFullscreen = async () => {
  const el = stageRef.value
  if (!el) return
  try {
    if (document.fullscreenElement) {
      await document.exitFullscreen()
      return
    }
    if (el.requestFullscreen) {
      await el.requestFullscreen()
    }
  } catch {
    return
  }
}

const toggleChat = () => {
  showChat.value = !showChat.value
}

const closeChat = () => {
  showChat.value = false
}

const sendChat = () => {
  if (!isInteractive.value) return
  if (!chatText.value.trim()) return
  const now = new Date()
  const time = `${now.getHours()}시 ${String(now.getMinutes()).padStart(2, '0')}분`
  chatMessages.value = [
    ...chatMessages.value,
    { id: `${liveId.value}-${Date.now()}`, user: '관리자', text: chatText.value.trim(), time },
  ]
  chatText.value = ''
  nextTick(() => {
    const el = chatListRef.value
    if (!el) return
    el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
  })
}

const openModeration = (msg: { user: string }) => {
  if (!isInteractive.value) return
  if (msg.user === 'SYSTEM' || msg.user === '관리자') return
  console.log('[admin chat] moderation open', msg.user)
  moderationTarget.value = { user: msg.user }
  moderationType.value = ''
  moderationReason.value = ''
  showModerationModal.value = true
}

const closeModeration = () => {
  showModerationModal.value = false
  moderationTarget.value = null
  moderationType.value = ''
  moderationReason.value = ''
}

const saveModeration = () => {
  if (!moderationType.value) {
    window.alert('제재 유형을 선택해주세요.')
    return
  }
  if (!moderationReason.value.trim()) {
    window.alert('제재 사유를 입력해주세요.')
    return
  }
  const confirmModeration = window.confirm('입력한 내용으로 시청자를 제재하시겠습니까?')
  if (!confirmModeration) return
  const target = moderationTarget.value
  if (!target) return
  const now = new Date()
  const at = `${now.getHours()}시 ${String(now.getMinutes()).padStart(2, '0')}분`
  moderatedUsers.value = {
    ...moderatedUsers.value,
    [target.user]: { type: moderationType.value, reason: moderationReason.value.trim(), at },
  }
  chatMessages.value = [
    ...chatMessages.value,
    {
      id: `sys-${Date.now()}`,
      user: 'SYSTEM',
      text: `관리자가 ${target.user}님을 '${moderationType.value}' 처리했습니다. (사유: ${moderationReason.value.trim()})`,
      time: at,
    },
  ]
  closeModeration()
  nextTick(() => {
    const el = chatListRef.value
    if (!el) return
    el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
  })
}

onMounted(() => {
  document.addEventListener('fullscreenchange', syncFullscreen)
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', syncFullscreen)
  window.removeEventListener('pagehide', handlePageHide)
  void sendLeaveSignal()
  sseSource.value?.close()
  sseSource.value = null
  sseConnected.value = false
  if (sseRetryTimer.value) window.clearTimeout(sseRetryTimer.value)
  sseRetryTimer.value = null
  if (statsTimer.value) window.clearInterval(statsTimer.value)
  statsTimer.value = null
  if (refreshTimer.value) window.clearTimeout(refreshTimer.value)
  refreshTimer.value = null
})

onMounted(() => {
  viewerId.value = resolveViewerId(getAuthUser())
  window.addEventListener('pagehide', handlePageHide)
})

watch(
  liveId,
  (value) => {
    if (joinedBroadcastId.value) {
      void sendLeaveSignal()
    }
    leaveRequested.value = false
    joinedBroadcastId.value = null
    loadDetail()
    const idValue = Number(value)
    if (!Number.isNaN(idValue)) {
      connectSse(idValue)
      startStatsPolling(idValue)
      void requestJoinToken()
    } else {
      sseSource.value?.close()
      sseSource.value = null
      sseConnected.value = false
      if (sseRetryTimer.value) window.clearTimeout(sseRetryTimer.value)
      sseRetryTimer.value = null
      if (statsTimer.value) window.clearInterval(statsTimer.value)
      statsTimer.value = null
      if (refreshTimer.value) window.clearTimeout(refreshTimer.value)
      refreshTimer.value = null
    }
  },
  { immediate: true },
)

watch(
  lifecycleStatus,
  () => {
    void requestJoinToken()
  },
)
</script>

<template>
  <div v-if="detail" class="live-detail">
    <header class="detail-header">
      <button type="button" class="back-link" @click="goBack">← 뒤로 가기</button>
      <div class="header-actions">
        <button type="button" class="btn" @click="goToList">목록으로</button>
        <button type="button" class="btn danger" :disabled="detail.status === 'STOPPED'" @click="openStopConfirm">
          {{ detail.status === 'STOPPED' ? '송출 중지됨' : '방송 송출 중지' }}
        </button>
      </div>
    </header>

    <h2 class="page-title">방송 모니터링</h2>

    <section class="detail-card ds-surface meta-card">
      <div class="detail-meta">
        <h3>{{ detail.title }}</h3>
        <p><span>판매자</span>{{ detail.sellerName }}</p>
        <p><span>방송 시작</span>{{ detail.startedAt }}</p>
        <p v-if="detail.scheduledAt"><span>예약 시간</span>{{ detail.scheduledAt }}</p>
        <p><span>시청자 수</span>{{ detail.viewers }}명</p>
        <p><span>신고 건수</span>{{ detail.reports ?? 0 }}건</p>
        <p><span>상태</span>{{ statusLabel }}</p>
        <p v-if="lifecycleStatus === 'READY'"><span>카운트다운</span>{{ readyCountdownLabel }}</p>
        <p v-if="lifecycleStatus === 'ENDED'"><span>종료까지</span>{{ endedCountdownLabel }}</p>
      </div>
    </section>

    <section class="player-card">
      <div class="player-tabs">
        <div class="tab-list" role="tablist" aria-label="모니터링 패널">
          <button
              type="button"
              class="tab"
              :class="{ 'tab--active': activePane === 'monitor' }"
              role="tab"
              aria-controls="monitor-pane"
              :aria-selected="activePane === 'monitor'"
              @click="activePane = 'monitor'"
          >
            모니터링
          </button>
          <button
              type="button"
              class="tab"
              :class="{ 'tab--active': activePane === 'products' }"
              role="tab"
              aria-controls="products-pane"
              :aria-selected="activePane === 'products'"
              @click="activePane = 'products'"
          >
            상품
          </button>
        </div>

        <div v-show="activePane === 'monitor'" id="monitor-pane">
          <div ref="stageRef" class="monitor-stage" :class="{ 'monitor-stage--chat': showChat }">
            <div class="player-wrap">
              <div class="player-frame" :class="{ 'player-frame--fullscreen': isFullscreen }">
                <div class="player-overlay">
                  <div class="overlay-item">⏱ {{ detail.elapsed }}</div>
                  <div class="overlay-item">👥 {{ detail.viewers }}명</div>
                  <div class="overlay-item">❤ {{ detail.likes }}</div>
                  <div class="overlay-item">🚩 {{ detail.reports ?? 0 }}건</div>
                </div>
                <div class="overlay-actions">
                  <button type="button" class="icon-circle" :class="{ active: showChat }" @click="toggleChat" :title="showChat ? '채팅 닫기' : '채팅 보기'">
                    <svg aria-hidden="true" class="icon" viewBox="0 0 24 24" focusable="false">
                      <path d="M3 20l1.62-3.24A2 2 0 0 1 6.42 16H20a1 1 0 0 0 1-1V5a1 1 0 0 0-1-1H4a1 1 0 0 0-1 1v15z" stroke="currentColor" stroke-width="1.7" />
                      <path d="M7 9h10M7 12h6" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
                    </svg>
                  </button>
                  <button type="button" class="icon-circle ghost" :class="{ active: isFullscreen }" @click="toggleFullscreen" :title="isFullscreen ? '전체화면 종료' : '전체화면'">
                    <svg v-if="!isFullscreen" aria-hidden="true" class="icon" viewBox="0 0 24 24" focusable="false">
                      <path d="M4 9V4h5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
                      <path d="M20 9V4h-5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
                      <path d="M4 15v5h5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
                      <path d="M20 15v5h-5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                    <svg v-else aria-hidden="true" class="icon" viewBox="0 0 24 24" focusable="false">
                      <path d="M9 5H5v4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
                      <path d="M15 19h4v-4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
                      <path d="M9 19H5v-4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
                      <path d="M15 5h4v4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                  </button>
                </div>
                <div v-if="isReadOnly" class="player-placeholder">
                  <img
                    v-if="waitingScreenUrl"
                    class="player-placeholder__image"
                    :src="waitingScreenUrl"
                    alt="대기 화면"
                    @error="handleImageError"
                  />
                  <p v-if="playerMessage" class="player-placeholder__message">{{ playerMessage }}</p>
                </div>
                <div v-else class="player-label">송출 화면</div>
              </div>
            </div>

            <aside v-if="showChat" class="chat-panel ds-surface">
              <header class="chat-head">
                <h4>실시간 채팅</h4>
                <button type="button" class="chat-close" @click="closeChat">×</button>
              </header>
              <div ref="chatListRef" class="chat-messages">
                <div
                    v-for="msg in chatMessages"
                    :key="msg.id"
                    class="chat-message"
                    :class="{ 'chat-message--system': msg.user === 'SYSTEM', 'chat-message--muted': moderatedUsers[msg.user] }"
                    @contextmenu.prevent="openModeration(msg)"
                >
                  <div class="chat-meta">
                    <span class="chat-user">{{ msg.user }}</span>
                    <span class="chat-time">{{ msg.time }}</span>
                    <span v-if="msg.user !== 'SYSTEM' && moderatedUsers[msg.user]" class="chat-badge">{{ moderatedUsers[msg.user]?.type }}</span>
                  </div>
                  <p class="chat-text">{{ msg.text }}</p>
                </div>
              </div>
              <div class="chat-input">
                <input v-model="chatText" type="text" placeholder="메시지를 입력하세요" :disabled="isReadOnly" />
                <button type="button" class="btn primary" :disabled="isReadOnly" @click="sendChat">전송</button>
              </div>
              <p v-if="isReadOnly" class="chat-helper">방송 중에만 채팅을 이용할 수 있습니다.</p>
            </aside>
          </div>
        </div>

        <div v-show="activePane === 'products'" id="products-pane" class="products-pane ds-surface" :class="{ 'products-pane--readonly': isReadOnly }">
          <header class="products-head">
            <div>
              <h4>상품 정보</h4>
              <p class="ds-section-sub">방송에 연결된 상품 현황을 확인하세요.</p>
            </div>
            <span class="pill">총 {{ liveProducts.length }}개</span>
          </header>
          <div class="product-list">
            <article v-for="product in sortedLiveProducts" :key="product.id" class="product-row">
              <div class="product-thumb">
                <img :src="product.thumb" :alt="product.name" loading="lazy" @error="handleImageError" />
              </div>
              <div class="product-meta">
                <p class="product-name">{{ product.name }}</p>
                <p class="product-option">{{ product.option }}</p>
                <p class="product-price">
                  <span class="product-sale">{{ product.sale }}</span>
                  <span class="product-origin">{{ product.price }}</span>
                </p>
                <p class="product-stats">판매 {{ product.sold }} · 재고 {{ product.stock }}</p>
              </div>
              <span class="product-status" :class="{ 'is-soldout': product.status === '품절' }">{{ product.status }}</span>
            </article>
          </div>
        </div>
      </div>
    </section>

    <Teleport :to="modalHostTarget">
      <div v-if="showStopModal" class="stop-modal">
        <div class="stop-modal__backdrop" @click="closeStopModal"></div>
        <div class="stop-modal__card ds-surface">
          <header class="stop-modal__head">
            <h3>방송 송출 중지</h3>
            <button type="button" class="close-btn" @click="closeStopModal">×</button>
          </header>
          <div class="stop-modal__body">
            <label class="field">
              <span class="field__label">유형</span>
              <select v-model="stopReason" class="field-input">
                <option value="">선택해주세요</option>
                <option v-for="option in reasonOptions" :key="option" :value="option">{{ option }}</option>
              </select>
            </label>
            <label v-if="stopReason === '기타'" class="field">
              <span class="field__label">중지 사유(기타 선택 시)</span>
              <textarea v-model="stopDetail" class="field-input" rows="4" placeholder="사유를 입력해주세요."></textarea>
            </label>
            <p v-if="error" class="error">{{ error }}</p>
          </div>
          <div class="stop-modal__actions">
            <button type="button" class="btn ghost" @click="closeStopModal">취소</button>
            <button type="button" class="btn primary" @click="handleStopSave">저장</button>
          </div>
        </div>
      </div>

      <div v-if="showModerationModal" class="moderation-modal">
        <div class="moderation-modal__backdrop" @click="closeModeration"></div>
        <div class="moderation-modal__card ds-surface">
          <header class="moderation-modal__head">
            <h3>채팅 관리</h3>
            <button type="button" class="close-btn" @click="closeModeration">×</button>
          </header>
          <div class="moderation-modal__body">
            <p class="moderation-target">대상: {{ moderationTarget?.user }}</p>
            <label class="field">
              <span class="field__label">제재 유형</span>
              <select v-model="moderationType" class="field-input">
                <option value="">선택해주세요</option>
                <option value="채팅 금지">채팅 금지</option>
                <option value="강제 퇴장">강제 퇴장</option>
              </select>
            </label>
            <label class="field">
              <span class="field__label">제재 사유</span>
              <textarea v-model="moderationReason" class="field-input" rows="4" placeholder="사유를 입력해주세요."></textarea>
            </label>
          </div>
          <div class="moderation-modal__actions">
            <button type="button" class="btn ghost" @click="closeModeration">취소</button>
            <button type="button" class="btn primary" @click="saveModeration">저장</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.live-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
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

.detail-card {
  padding: 18px;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.meta-card {
  padding: 14px 18px;
}

.detail-meta h3 {
  margin: 0 0 8px;
  font-size: 1.2rem;
  font-weight: 900;
  color: var(--text-strong);
}

.detail-meta p {
  margin: 4px 0;
  color: var(--text-muted);
  font-weight: 700;
}

.detail-meta span {
  display: inline-block;
  min-width: 120px;
  color: var(--text-strong);
  font-weight: 800;
  margin-right: 6px;
}

.player-card {
  width: 100%;
}

.monitor-stage {
  display: flex;
  gap: 16px;
  align-items: center;
  position: relative;
  width: 100%;
  margin: 0 auto;
}

.player-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.player-frame {
  position: relative;
  width: 100%;
  height: auto;
  max-width: calc((100vh - 120px) * (16 / 9));
  max-height: calc(100vh - 120px);
  min-height: clamp(360px, auto, 760px);
  aspect-ratio: 16 / 9;
  background: #0b0f1a;
  border-radius: 18px;
  overflow: auto;
  display: flex;
  align-items: center;
  justify-content: center;
}

.player-frame--fullscreen {
  max-height: none;
  max-width: none;
  height: min(100vh, calc(100vw * (9 / 16)));
  width: min(100vw, calc(100vh * (16 / 9)));
  border-radius: 0;
  background: #000;
}

.player-frame iframe,
.player-frame video,
.player-frame img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  border: 0;
  background: #000;
}

.player-label {
  color: rgba(255, 255, 255, 0.6);
  font-weight: 800;
  letter-spacing: 0.08em;
}

.player-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  text-align: center;
  background: rgba(6, 10, 18, 0.92);
  z-index: 1;
}

.player-placeholder__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.player-placeholder__message {
  color: #f8fafc;
  font-weight: 800;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.35);
  max-width: min(520px, 100%);
}

.player-overlay {
  position: absolute;
  top: 14px;
  right: 14px;
  display: grid;
  gap: 8px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  padding: 10px 12px;
  border-radius: 12px;
  font-weight: 800;
  font-size: 0.9rem;
  z-index: 2;
}

.overlay-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.chat-toggle {
  position: absolute;
  right: 14px;
  bottom: 14px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  border-radius: 999px;
  padding: 8px 12px;
  font-weight: 800;
  cursor: pointer;
}

.overlay-actions {
  position: absolute;
  right: 14px;
  bottom: 14px;
  display: inline-flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-end;
  z-index: 2;
}

.icon-circle {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.4);
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  cursor: pointer;
}

.icon-circle.ghost {
  background: rgba(255, 255, 255, 0.16);
  color: #0f172a;
  border-color: rgba(255, 255, 255, 0.4);
}

.icon-circle.active {
  border-color: var(--primary-color);
  color: var(--primary-color);
  background: rgba(var(--primary-rgb), 0.12);
}

.icon {
  width: 18px;
  height: 18px;
  stroke: currentColor;
  fill: none;
  stroke-width: 2px;
}

.chat-panel {
  width: 360px;
  display: flex;
  flex-direction: column;
  border-radius: 16px;
  padding: 12px;
  gap: 10px;
}

.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.chat-head h4 {
  margin: 0;
  font-size: 1rem;
  font-weight: 900;
  color: var(--text-strong);
}

.chat-close {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-muted);
  width: 28px;
  height: 28px;
  border-radius: 999px;
  cursor: pointer;
}

.chat-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.chat-message {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chat-message--system .chat-user {
  color: #ef4444;
}

.chat-message--muted .chat-text {
  color: var(--text-muted);
}

.chat-meta {
  display: flex;
  gap: 8px;
  font-size: 0.85rem;
  color: var(--text-muted);
  font-weight: 700;
}

.chat-user {
  color: var(--text-strong);
  font-weight: 800;
}

.chat-text {
  margin: 0;
  color: var(--text-strong);
  font-weight: 700;
  line-height: 1.4;
}

.chat-badge {
  padding: 2px 6px;
  border-radius: 999px;
  background: var(--surface-weak);
  color: var(--text-muted);
  font-weight: 800;
  font-size: 0.75rem;
}

.chat-input {
  display: flex;
  gap: 8px;
}

.monitor-stage--chat .player-wrap {
  margin-right: 372px;
}

.monitor-stage:fullscreen {
  height: 100vh;
  max-height: 100vh;
  align-items: center;
  justify-content: center;
}

.monitor-stage:fullscreen .player-wrap {
  height: 100vh;
  max-height: 100vh;
  display: flex;
  justify-content: center;
}

.monitor-stage:fullscreen .player-frame {
  max-height: 100vh;
  max-width: none;
  height: min(100vh, calc(100vw * (9 / 16)));
  width: min(100vw, calc(100vh * (16 / 9)));
  border-radius: 0;
  background: #000;
}

.monitor-stage:fullscreen.monitor-stage--chat .player-frame {
  width: min(max(320px, calc(100vw - 380px)), calc(100vh * (16 / 9)));
  height: min(100vh, max(200px, calc((100vw - 380px) * (9 / 16))));
}

.monitor-stage--chat .chat-panel {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 360px;
  height: auto;
  overflow: hidden;
}

.chat-input input {
  flex: 1;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 8px 10px;
  font-weight: 700;
  color: var(--text-strong);
  background: var(--surface);
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

.btn.primary {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.btn.danger {
  border-color: #ef4444;
  color: #ef4444;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.stop-modal {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: grid;
  place-items: center;
}

.stop-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
}

.stop-modal__card {
  position: relative;
  width: min(520px, 92vw);
  border-radius: 16px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  z-index: 1;
}

.stop-modal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.stop-modal__head h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 900;
  color: var(--text-strong);
}

.close-btn {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-muted);
  width: 32px;
  height: 32px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 1.1rem;
  line-height: 1;
}

.stop-modal__body {
  display: flex;
  flex-direction: column;
  gap: 12px;
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

.field-input {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 10px 12px;
  font-weight: 700;
  color: var(--text-strong);
  background: var(--surface);
}

.stop-modal__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn.ghost {
  border-color: var(--border-color);
  color: var(--text-muted);
  background: transparent;
}

.error {
  margin: 0;
  color: #ef4444;
  font-weight: 700;
}

.moderation-modal {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: grid;
  place-items: center;
}

.moderation-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
}

.moderation-modal__card {
  position: relative;
  width: min(520px, calc(100vw - 32px));
  border-radius: 16px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  z-index: 1;
}

.moderation-modal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.moderation-modal__head h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 900;
  color: var(--text-strong);
}

.moderation-modal__body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.moderation-target {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
}

.moderation-modal__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* Monitoring tabs & products */
.player-tabs {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tab-list {
  display: inline-flex;
  background: rgba(15, 23, 42, 0.08);
  padding: 4px;
  border-radius: 12px;
  gap: 6px;
  width: fit-content;
}

.tab {
  border: none;
  padding: 8px 14px;
  border-radius: 10px;
  background: transparent;
  color: var(--text-muted);
  font-weight: 800;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.tab--active {
  background: var(--surface);
  color: var(--text-strong);
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.06);
}

.products-pane {
  border-radius: 16px;
  padding: 16px;
  background: var(--surface);
  border: 1px solid var(--border-color);
}

.products-pane--readonly {
  opacity: 0.6;
}

.chat-helper {
  margin: 8px 0 0;
  color: var(--text-muted);
  font-size: 0.9rem;
  font-weight: 700;
}

.products-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.products-head h4 {
  margin: 0;
  color: var(--text-strong);
}

.pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--surface-weak);
  border-radius: 999px;
  padding: 6px 12px;
  font-weight: 800;
  color: var(--text-muted);
}

.product-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-row {
  display: grid;
  grid-template-columns: 120px 1fr 100px;
  gap: 12px;
  align-items: center;
  background: var(--surface-weak);
  padding: 12px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
}

.product-thumb img {
  width: 120px;
  height: 80px;
  object-fit: cover;
  border-radius: 10px;
}

.product-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-name {
  margin: 0;
  font-weight: 900;
  color: var(--text-strong);
}

.product-option {
  margin: 0;
  color: var(--text-muted);
}

.product-price {
  margin: 0;
  display: flex;
  gap: 8px;
  align-items: baseline;
}

.product-sale {
  font-weight: 900;
  color: #f59e0b;
}

.product-origin {
  color: var(--text-soft);
  text-decoration: line-through;
}

.product-stats {
  margin: 0;
  color: var(--text-muted);
}

.product-status {
  justify-self: end;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(34, 197, 94, 0.12);
  color: #16a34a;
  font-weight: 800;
}

.product-status.is-soldout {
  background: rgba(248, 113, 113, 0.15);
  color: #ef4444;
}

.monitor-stage {
  --stacked-max-width: 1040px;
}

@media (max-width: 1200px) {
  .monitor-stage {
    flex-direction: column;
    align-items: center;
  }

  .monitor-stage--chat .player-wrap {
    margin-right: 0;
  }

  .player-wrap {
    width: 100%;
    max-width: var(--stacked-max-width);
  }

  .monitor-stage--chat .chat-panel {
    position: static;
    inset: auto;
    width: 100%;
    max-width: var(--stacked-max-width);
    max-height: 40vh;
    box-shadow: none;
  }

  .chat-messages {
    max-height: 28vh;
  }
}

@media (max-width: 900px) {
  .player-frame {
    min-height: 46vh;
  }

  .monitor-stage {
    align-items: stretch;
  }

  .player-wrap,
  .chat-panel {
    max-width: none;
  }

  .chat-panel {
    max-height: 48vh;
  }

  .chat-messages {
    max-height: 36vh;
  }
}
</style>
