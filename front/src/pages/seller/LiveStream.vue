<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import BasicInfoEditModal from '../../components/BasicInfoEditModal.vue'
import ChatSanctionModal from '../../components/ChatSanctionModal.vue'
import ConfirmModal from '../../components/ConfirmModal.vue'
import DeviceSetupModal from '../../components/DeviceSetupModal.vue'
import PageContainer from '../../components/PageContainer.vue'
import QCardModal from '../../components/QCardModal.vue'
import {
  fetchBroadcastStats,
  fetchMediaConfig,
  fetchRecentLiveChats,
  fetchSellerBroadcastDetail,
  joinBroadcast,
  leaveBroadcast,
  startSellerBroadcast,
  endSellerBroadcast,
  pinSellerBroadcastProduct,
} from '../../lib/live/api'
import { parseLiveDate } from '../../lib/live/utils'
import { useNow } from '../../lib/live/useNow'
import { getAuthUser } from '../../lib/auth'
import { resolveViewerId } from '../../lib/live/viewer'
import { computeLifecycleStatus, getScheduledEndMs, normalizeBroadcastStatus, type BroadcastStatus } from '../../lib/broadcastStatus'

type StreamProduct = {
  id: string
  title: string
  option: string
  status: string
  pinned?: boolean
}

type StreamChat = {
  id: string
  name: string
  message: string
  time?: string
}

type StreamData = {
  title: string
  datetime: string
  category: string
  notice: string
  products: StreamProduct[]
  chat: StreamChat[]
  qCards: string[]
  thumbnail?: string
  waitingScreen?: string
}

type EditableBroadcastInfo = {
  title: string
  category: string
  notice?: string
  thumbnail?: string
  waitingScreen?: string
}

const defaultNotice = ''

const route = useRoute()
const router = useRouter()

const showProducts = ref(true)
const showChat = ref(true)
const showSettings = ref(false)
const viewerCount = ref(0)
const likeCount = ref(0)
const elapsed = ref('00:00:00')
const monitorRef = ref<HTMLElement | null>(null)
const streamGridRef = ref<HTMLElement | null>(null)
const streamCenterRef = ref<HTMLElement | null>(null)
const isFullscreen = ref(false)
const modalHostTarget = computed(() => (isFullscreen.value && monitorRef.value ? monitorRef.value : 'body'))
const micEnabled = ref(true)
const videoEnabled = ref(true)
const volume = ref(43)
const selectedMic = ref('기본 마이크')
const selectedCamera = ref('기본 카메라')
const micInputLevel = ref<number>(0)
const micStream = ref<MediaStream | null>(null)
const micAudioContext = ref<AudioContext | null>(null)
const micAnalyser = ref<AnalyserNode | null>(null)
const micMeterFrame = ref<number | null>(null)
const chatText = ref('')
const chatListRef = ref<HTMLElement | null>(null)
let gridObserver: ResizeObserver | null = null
const availableMics = ref<Array<{ id: string; label: string }>>([])
const availableCameras = ref<Array<{ id: string; label: string }>>([])

const showQCards = ref(false)
const showBasicInfo = ref(false)
const showSanctionModal = ref(false)
const showDeviceModal = ref(false)
const hasOpenedDeviceModal = ref(false)
const isLoadingStream = ref(true)
const qCardIndex = ref(0)
const handleFullscreenChange = () => {
  isFullscreen.value = Boolean(document.fullscreenElement)
}

const gridWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 0)
const gridHeight = computed(() => (gridWidth.value ? (gridWidth.value * 9) / 16 : null))
const isStackedLayout = computed(() => gridWidth.value <= 960)

const confirmState = reactive({
  open: false,
  title: '',
  description: '',
  confirmText: '확인',
  cancelText: '취소',
})
const confirmAction = ref<() => void>(() => {})
const confirmCancelAction = ref<() => void>(() => {})

const pinnedProductId = ref<string | null>(null)
const sanctionTarget = ref<string | null>(null)
const sanctionedUsers = ref<Record<string, { type: string; reason: string }>>({})
const broadcastInfo = ref<(EditableBroadcastInfo & { qCards: string[] }) | null>(null)
const stream = ref<StreamData | null>(null)
const chatMessages = ref<StreamChat[]>([])
const streamStatus = ref<BroadcastStatus>('RESERVED')
const { now } = useNow(1000)
const scheduleStartAtMs = ref<number | null>(null)
const scheduleEndAtMs = ref<number | null>(null)
const sseSource = ref<EventSource | null>(null)
const sseConnected = ref(false)
const sseRetryCount = ref(0)
const sseRetryTimer = ref<number | null>(null)
const statsTimer = ref<number | null>(null)
const refreshTimer = ref<number | null>(null)
const startTimer = ref<number | null>(null)
const joinInFlight = ref(false)
const startRequested = ref(false)
const endRequested = ref(false)
const endRequestTimer = ref<number | null>(null)
const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const viewerId = ref<string | null>(resolveViewerId(getAuthUser()))
const joinedBroadcastId = ref<number | null>(null)
const leaveRequested = ref(false)

const streamId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' && id.trim() ? id : null
})

const formatElapsed = (startedAt?: string) => {
  if (!startedAt) return '00:00:00'
  const started = parseLiveDate(startedAt)
  if (Number.isNaN(started.getTime())) return '00:00:00'
  const diff = Math.max(0, Date.now() - started.getTime())
  const totalSeconds = Math.floor(diff / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

const formatScheduleWindow = (scheduledAt?: string, startedAt?: string) => {
  const baseRaw = scheduledAt ?? startedAt
  if (!baseRaw) return ''
  const base = parseLiveDate(baseRaw)
  if (Number.isNaN(base.getTime())) return ''
  const end = new Date(base.getTime() + 30 * 60 * 1000)
  const pad = (value: number) => String(value).padStart(2, '0')
  const dateLabel = `${base.getFullYear()}.${pad(base.getMonth() + 1)}.${pad(base.getDate())}`
  return `${dateLabel} ${pad(base.getHours())}:${pad(base.getMinutes())} - ${pad(end.getHours())}:${pad(end.getMinutes())}`
}

const formatChatTime = (timestamp: number = Date.now()) => {
  const date = new Date(timestamp)
  const hours = date.getHours()
  const displayHour = hours % 12 || 12
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${hours >= 12 ? '오후' : '오전'} ${displayHour}:${minutes}`
}

const productItems = computed(() => stream.value?.products ?? [])
const sortedProducts = computed(() => {
  const items = [...productItems.value]
  items.sort((a, b) => {
    const aSoldOut = a.status === '품절'
    const bSoldOut = b.status === '품절'
    if (aSoldOut !== bSoldOut) return aSoldOut ? 1 : -1
    if (pinnedProductId.value) {
      if (a.id === pinnedProductId.value) return -1
      if (b.id === pinnedProductId.value) return 1
    }
    return 0
  })
  return items
})

const chatItems = computed(() => chatMessages.value)

const hasSidePanels = computed(() => showProducts.value || showChat.value)
const gridStyles = computed(() => ({
  '--grid-template-columns': monitorColumns.value,
  '--stream-pane-height': streamPaneHeight.value,
  '--center-height': gridHeight.value ? `${gridHeight.value}px` : undefined,
}))
const stackedOrders = computed(() =>
  isStackedLayout.value ? { stream: 0, chat: 1, products: 2 } : null,
)

const monitorColumns = computed(() => {
  if (showProducts.value && showChat.value) return '320px minmax(0, 1fr) 320px'
  if (showProducts.value) return '320px minmax(0, 1fr)'
  if (showChat.value) return 'minmax(0, 1fr) 320px'
  return 'minmax(0, 1fr)'
})

const streamPaneHeight = computed(() => {
  const dynamic = gridHeight.value
  if (dynamic) {
    const min = 320
    const max = 675
    return `${Math.min(Math.max(dynamic, min), max)}px`
  }
  if (showProducts.value && showChat.value) return 'clamp(460px, 62vh, 680px)'
  if (showProducts.value || showChat.value) return 'clamp(520px, 68vh, 760px)'
  return 'clamp(560px, 74vh, 880px)'
})

const qCards = computed(() => broadcastInfo.value?.qCards ?? stream.value?.qCards ?? [])
const displayTitle = computed(() => broadcastInfo.value?.title ?? stream.value?.title ?? '방송 진행')
const displayDatetime = computed(
  () => stream.value?.datetime ?? '실시간 송출 화면과 판매 상품, 채팅을 관리합니다.',
)
const lifecycleStatus = computed(() =>
  computeLifecycleStatus({
    status: streamStatus.value,
    startAtMs: scheduleStartAtMs.value ?? undefined,
    endAtMs: scheduleEndAtMs.value ?? undefined,
  }),
)
const isInteractive = computed(() => lifecycleStatus.value === 'ON_AIR')
const isReadOnly = computed(() => lifecycleStatus.value !== 'ON_AIR')
const waitingScreenUrl = computed(() => stream.value?.waitingScreen ?? '')
const readyCountdownLabel = computed(() => {
  if (lifecycleStatus.value !== 'READY' || !scheduleStartAtMs.value) return ''
  const diffMs = scheduleStartAtMs.value - now.value.getTime()
  if (diffMs <= 0) return '방송 시작 대기 중'
  const totalSeconds = Math.ceil(diffMs / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${minutes}분 ${String(seconds).padStart(2, '0')}초 뒤 방송 시작`
})
const streamPlaceholderMessage = computed(() => {
  if (lifecycleStatus.value === 'STOPPED') {
    return '방송이 운영 정책 위반으로 송출 중지되었습니다.'
  }
  if (lifecycleStatus.value === 'ENDED') {
    return '방송이 종료되었습니다.'
  }
  if (lifecycleStatus.value === 'READY') {
    return readyCountdownLabel.value || '방송 시작 대기 중'
  }
  return '송출 화면 (WebRTC Stream)'
})

const loadMediaDevices = async () => {
  if (!navigator.mediaDevices?.enumerateDevices) {
    availableMics.value = []
    availableCameras.value = []
    return
  }
  try {
    const devices = await navigator.mediaDevices.enumerateDevices()
    availableMics.value = devices
      .filter((device) => device.kind === 'audioinput')
      .map((device, idx) => ({
        id: device.deviceId,
        label: device.label || `마이크 ${idx + 1}`,
      }))
    availableCameras.value = devices
      .filter((device) => device.kind === 'videoinput')
      .map((device, idx) => ({
        id: device.deviceId,
        label: device.label || `카메라 ${idx + 1}`,
      }))
  } catch {
    availableMics.value = []
    availableCameras.value = []
  }
}

const ensureLocalMediaAccess = async () => {
  if (!navigator.mediaDevices?.getUserMedia) {
    return
  }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true, video: true })
    stream.getTracks().forEach((track) => track.stop())
  } catch {
    return
  }
}

const clearStartTimer = () => {
  if (startTimer.value) {
    window.clearTimeout(startTimer.value)
    startTimer.value = null
  }
}

const clearEndRequestTimer = () => {
  if (endRequestTimer.value) {
    window.clearTimeout(endRequestTimer.value)
    endRequestTimer.value = null
  }
}

const stopMicMeter = () => {
  if (micMeterFrame.value !== null) {
    cancelAnimationFrame(micMeterFrame.value)
    micMeterFrame.value = null
  }
  if (micAudioContext.value) {
    micAudioContext.value.close()
    micAudioContext.value = null
  }
  micAnalyser.value = null
  if (micStream.value) {
    micStream.value.getTracks().forEach((track) => track.stop())
    micStream.value = null
  }
}

const startMicMeter = async () => {
  if (!navigator.mediaDevices?.getUserMedia) {
    micInputLevel.value = 0
    return
  }
  if (!micEnabled.value) {
    micInputLevel.value = 0
    stopMicMeter()
    return
  }
  stopMicMeter()
  try {
    const constraints: MediaStreamConstraints = {
      audio: selectedMic.value !== '기본 마이크' ? { deviceId: { exact: selectedMic.value } } : true,
    }
    const stream = await navigator.mediaDevices.getUserMedia(constraints)
    micStream.value = stream
    const [track] = stream.getAudioTracks()
    if (!track) {
      micInputLevel.value = 0
      return
    }
    const context = new AudioContext()
    const analyserNode = context.createAnalyser()
    analyserNode.fftSize = 512
    const source = context.createMediaStreamSource(stream)
    source.connect(analyserNode)
    micAudioContext.value = context
    micAnalyser.value = analyserNode
    const buffer = new Uint8Array(analyserNode.fftSize)
    const update = () => {
      analyserNode.getByteTimeDomainData(buffer)
      let sum = 0
      for (const sample of buffer) {
        const normalized = (sample - 128) / 128
        sum += normalized * normalized
      }
      const rms = Math.sqrt(sum / buffer.length)
      micInputLevel.value = Math.min(100, Math.round(rms * 140))
      micMeterFrame.value = requestAnimationFrame(update)
    }
    update()
  } catch {
    micInputLevel.value = 0
  }
}

const resetRealtimeState = () => {
  sseSource.value?.close()
  sseSource.value = null
  sseConnected.value = false
  if (sseRetryTimer.value) window.clearTimeout(sseRetryTimer.value)
  sseRetryTimer.value = null
  if (statsTimer.value) window.clearInterval(statsTimer.value)
  statsTimer.value = null
  if (refreshTimer.value) window.clearTimeout(refreshTimer.value)
  refreshTimer.value = null
  clearStartTimer()
  clearEndRequestTimer()
  startRequested.value = false
  endRequested.value = false
}

const requestStartBroadcast = async (broadcastId: number) => {
  if (startRequested.value) return
  startRequested.value = true
  try {
    await startSellerBroadcast(broadcastId)
    scheduleRefresh(broadcastId)
  } catch {
    startRequested.value = false
  }
}

const requestJoinToken = async (broadcastId: number) => {
  if (!['READY', 'ON_AIR'].includes(lifecycleStatus.value)) return
  if (joinInFlight.value) return
  if (joinedBroadcastId.value === broadcastId) return
  joinInFlight.value = true
  try {
    await joinBroadcast(broadcastId, viewerId.value)
    joinedBroadcastId.value = broadcastId
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

const handleBeforeUnload = (event: BeforeUnloadEvent) => {
  if (!isInteractive.value) return
  event.preventDefault()
  event.returnValue = ''
}

const scheduleAutoStart = (broadcastId: number, scheduledAtMs: number | null, status: BroadcastStatus) => {
  clearStartTimer()
  if (!scheduledAtMs || status !== 'READY') return
  const delay = Math.max(0, scheduledAtMs - Date.now())
  if (delay === 0) {
    void requestStartBroadcast(broadcastId)
    return
  }
  startTimer.value = window.setTimeout(() => {
    void requestStartBroadcast(broadcastId)
  }, delay)
}

const updateGridWidth = (width?: number) => {
  if (typeof width === 'number') {
    gridWidth.value = width
    return
  }
  const rectWidth = streamGridRef.value?.clientWidth
  if (rectWidth) {
    gridWidth.value = rectWidth
    return
  }
  gridWidth.value = typeof window !== 'undefined' ? window.innerWidth : 0
}

const scrollChatToBottom = () => {
  nextTick(() => {
    const el = chatListRef.value
    if (!el) return
    el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
  })
}

const hydrateStream = async () => {
  isLoadingStream.value = true
  const id = streamId.value
  if (!id) {
    stream.value = null
    pinnedProductId.value = null
    broadcastInfo.value = null
    chatMessages.value = []
    viewerCount.value = 0
    likeCount.value = 0
    elapsed.value = '00:00:00'
    streamStatus.value = 'RESERVED'
    scheduleStartAtMs.value = null
    scheduleEndAtMs.value = null
    isLoadingStream.value = false
    clearStartTimer()
    clearEndRequestTimer()
    startRequested.value = false
    endRequested.value = false
    return
  }

  const idValue = Number(id)
  if (Number.isNaN(idValue)) {
    stream.value = null
    viewerCount.value = 0
    likeCount.value = 0
    elapsed.value = '00:00:00'
    streamStatus.value = 'RESERVED'
    scheduleStartAtMs.value = null
    scheduleEndAtMs.value = null
    isLoadingStream.value = false
    clearStartTimer()
    clearEndRequestTimer()
    startRequested.value = false
    endRequested.value = false
    return
  }

  try {
    const [detail, stats, chats, mediaConfig] = await Promise.all([
      fetchSellerBroadcastDetail(idValue),
      fetchBroadcastStats(idValue).catch(() => null),
      fetchRecentLiveChats(idValue, 300).catch(() => []),
      fetchMediaConfig(idValue).catch(() => null),
    ])
    const baseTime = detail.scheduledAt ?? detail.startedAt ?? ''
    const startAtMs = baseTime ? parseLiveDate(baseTime).getTime() : NaN
    scheduleStartAtMs.value = Number.isNaN(startAtMs) ? null : startAtMs
    scheduleEndAtMs.value = scheduleStartAtMs.value ? getScheduledEndMs(scheduleStartAtMs.value) ?? null : null
    streamStatus.value = normalizeBroadcastStatus(detail.status)

    const products = (detail.products ?? []).map((product) => ({
      id: String(product.bpId ?? product.productId),
      title: product.name,
      option: product.name,
      status: product.status === 'SOLDOUT' ? '품절' : '판매중',
      pinned: product.pinned,
    }))

    stream.value = {
      title: detail.title ?? '',
      datetime: formatScheduleWindow(detail.scheduledAt, detail.startedAt),
      category: detail.categoryName ?? '',
      notice: detail.notice ?? defaultNotice,
      products,
      chat: [],
      qCards: (detail.qcards ?? []).map((card) => card.question),
      thumbnail: detail.thumbnailUrl,
      waitingScreen: detail.waitScreenUrl,
    }

    pinnedProductId.value = products.find((item) => item.pinned)?.id ?? null
    broadcastInfo.value = {
      title: detail.title ?? '',
      category: detail.categoryName ?? '',
      notice: detail.notice ?? defaultNotice,
      thumbnail: detail.thumbnailUrl,
      waitingScreen: detail.waitScreenUrl,
      qCards: (detail.qcards ?? []).map((card) => card.question),
    }

    viewerCount.value = stats?.viewerCount ?? detail.totalViews ?? 0
    likeCount.value = stats?.likeCount ?? detail.totalLikes ?? 0
    elapsed.value = formatElapsed(detail.startedAt)

    if (mediaConfig) {
      selectedMic.value = mediaConfig.microphoneId?.trim() ? mediaConfig.microphoneId : '기본 마이크'
      selectedCamera.value = mediaConfig.cameraId?.trim() ? mediaConfig.cameraId : '기본 카메라'
      micEnabled.value = mediaConfig.microphoneOn
      videoEnabled.value = mediaConfig.cameraOn
      volume.value = mediaConfig.volume
    }

    chatMessages.value = chats.map((item) => ({
      id: `${item.sentAt}-${item.sender}`,
      name: item.sender || item.memberEmail || '시청자',
      message: item.content,
      time: formatChatTime(item.sentAt),
    }))
    isLoadingStream.value = false
    scrollChatToBottom()
    scheduleAutoStart(idValue, scheduleStartAtMs.value, streamStatus.value)
  } catch {
    stream.value = null
    chatMessages.value = []
    viewerCount.value = 0
    likeCount.value = 0
    elapsed.value = '00:00:00'
    streamStatus.value = 'RESERVED'
    scheduleStartAtMs.value = null
    scheduleEndAtMs.value = null
    isLoadingStream.value = false
    clearStartTimer()
    clearEndRequestTimer()
    startRequested.value = false
    endRequested.value = false
  }
}

const refreshStats = async (broadcastId: number) => {
  try {
    const stats = await fetchBroadcastStats(broadcastId)
    viewerCount.value = stats.viewerCount ?? 0
    likeCount.value = stats.likeCount ?? 0
  } catch {
    return
  }
}

const refreshProducts = async (broadcastId: number) => {
  try {
    const detail = await fetchSellerBroadcastDetail(broadcastId)
    const products = (detail.products ?? []).map((product) => ({
      id: String(product.bpId ?? product.productId),
      title: product.name,
      option: product.name,
      status: product.status === 'SOLDOUT' ? '품절' : '판매중',
      pinned: product.pinned,
    }))
    pinnedProductId.value = products.find((item) => item.pinned)?.id ?? null
    if (stream.value) {
      stream.value = { ...stream.value, products }
    }
  } catch {
    return
  }
}

const refreshInfo = async (broadcastId: number) => {
  try {
    const detail = await fetchSellerBroadcastDetail(broadcastId)
    const baseTime = detail.scheduledAt ?? detail.startedAt ?? ''
    const startAtMs = baseTime ? parseLiveDate(baseTime).getTime() : NaN
    scheduleStartAtMs.value = Number.isNaN(startAtMs) ? null : startAtMs
    scheduleEndAtMs.value = scheduleStartAtMs.value ? getScheduledEndMs(scheduleStartAtMs.value) ?? null : null
    streamStatus.value = normalizeBroadcastStatus(detail.status)
    if (stream.value) {
      stream.value = {
        ...stream.value,
        title: detail.title ?? '',
        datetime: formatScheduleWindow(detail.scheduledAt, detail.startedAt),
        category: detail.categoryName ?? '',
        notice: detail.notice ?? defaultNotice,
        thumbnail: detail.thumbnailUrl,
        waitingScreen: detail.waitScreenUrl,
        qCards: (detail.qcards ?? []).map((card) => card.question),
      }
    }
    if (broadcastInfo.value) {
      broadcastInfo.value = {
        ...broadcastInfo.value,
        title: detail.title ?? '',
        category: detail.categoryName ?? '',
        notice: detail.notice ?? defaultNotice,
        thumbnail: detail.thumbnailUrl,
        waitingScreen: detail.waitScreenUrl,
        qCards: (detail.qcards ?? []).map((card) => card.question),
      }
    }
    scheduleAutoStart(broadcastId, scheduleStartAtMs.value, streamStatus.value)
  } catch {
    return
  }
}

const parseSseData = (event: MessageEvent) => {
  if (!event.data) return null
  try {
    return JSON.parse(event.data)
  } catch {
    return event.data
  }
}

const scheduleRefresh = (broadcastId: number) => {
  if (refreshTimer.value) window.clearTimeout(refreshTimer.value)
  refreshTimer.value = window.setTimeout(() => {
    void refreshInfo(broadcastId)
    void refreshStats(broadcastId)
    void refreshProducts(broadcastId)
  }, 500)
}

const handleSseEvent = (event: MessageEvent) => {
  const id = streamId.value ? Number(streamId.value) : NaN
  if (Number.isNaN(id)) return
  const data = parseSseData(event)
  switch (event.type) {
    case 'BROADCAST_READY':
    case 'BROADCAST_UPDATED':
    case 'BROADCAST_STARTED':
      scheduleRefresh(id)
      break
    case 'PRODUCT_PINNED':
      pinnedProductId.value = typeof data === 'number' ? String(data) : pinnedProductId.value
      scheduleRefresh(id)
      break
    case 'PRODUCT_SOLD_OUT':
      scheduleRefresh(id)
      break
    case 'SANCTION_UPDATED':
      scheduleRefresh(id)
      break
    case 'BROADCAST_ENDING_SOON':
      alert('방송 종료 1분 전입니다.')
      break
    case 'BROADCAST_CANCELED':
      alert('방송이 자동 취소되었습니다.')
      handleGoToList()
      break
    case 'BROADCAST_ENDED':
      if (endRequested.value) {
        endRequested.value = false
        clearEndRequestTimer()
        scheduleRefresh(id)
        break
      }
      alert('방송이 종료되었습니다.')
      scheduleRefresh(id)
      break
    case 'BROADCAST_SCHEDULED_END':
      if (window.confirm('방송이 종료되었습니다.')) {
        handleGoToList()
      }
      break
    case 'BROADCAST_STOPPED':
      streamStatus.value = 'STOPPED'
      scheduleRefresh(id)
      if (window.confirm(typeof data === 'string' ? data : '관리자에 의해 방송이 중지되었습니다.')) {
        handleGoToList()
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
  const user = getAuthUser()
  const viewerId = resolveViewerId(user)
  const query = viewerId ? `?viewerId=${encodeURIComponent(viewerId)}` : ''
  const source = new EventSource(`${apiBase}/api/broadcasts/${broadcastId}/subscribe${query}`)
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
  events.forEach((name) => {
    source.addEventListener(name, handleSseEvent)
  })
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

watch(
  () => route.params.id,
  () => {
    hydrateStream()
  },
  { immediate: true },
)

watch(
  () => streamId.value,
  (value) => {
    resetRealtimeState()
    hasOpenedDeviceModal.value = false
    if (joinedBroadcastId.value) {
      void sendLeaveSignal()
    }
    leaveRequested.value = false
    joinedBroadcastId.value = null
    if (!value) {
      return
    }
    const idValue = Number(value)
    if (Number.isNaN(idValue)) {
      return
    }
    connectSse(idValue)
    startStatsPolling(idValue)
    void requestJoinToken(idValue)
  },
  { immediate: true },
)

const handleResize = () => updateGridWidth()

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && showSettings.value) {
    showSettings.value = false
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  window.addEventListener('resize', handleResize)
  window.addEventListener('pagehide', handlePageHide)
  window.addEventListener('beforeunload', handleBeforeUnload)
  viewerId.value = resolveViewerId(getAuthUser())
  monitorRef.value = streamGridRef.value
  updateGridWidth()
  void loadMediaDevices()
  if (navigator.mediaDevices?.addEventListener) {
    navigator.mediaDevices.addEventListener('devicechange', loadMediaDevices)
  }
  if (streamGridRef.value) {
    gridObserver = new ResizeObserver((entries) => {
      const entry = entries[0]
      if (entry?.contentRect?.width) {
        updateGridWidth(entry.contentRect.width)
      }
    })
    gridObserver.observe(streamGridRef.value)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('pagehide', handlePageHide)
  window.removeEventListener('beforeunload', handleBeforeUnload)
  void sendLeaveSignal()
  if (navigator.mediaDevices?.removeEventListener) {
    navigator.mediaDevices.removeEventListener('devicechange', loadMediaDevices)
  }
  gridObserver?.disconnect()
  stopMicMeter()
  resetRealtimeState()
})

const openConfirm = (options: Partial<typeof confirmState>, onConfirm: () => void, onCancel: () => void = () => {}) => {
  confirmState.title = options.title ?? ''
  confirmState.description = options.description ?? ''
  confirmState.confirmText = options.confirmText ?? '확인'
  confirmState.cancelText = options.cancelText ?? '취소'
  confirmAction.value = onConfirm
  confirmCancelAction.value = onCancel
  confirmState.open = true
}

const handleConfirmAction = () => {
  confirmAction.value?.()
  confirmAction.value = () => {}
}

const handleConfirmCancel = () => {
  confirmCancelAction.value?.()
  confirmCancelAction.value = () => {}
}

const setPinnedProduct = async (productId: string | null) => {
  if (!isInteractive.value) return
  pinnedProductId.value = productId
  if (!productId) return
  const broadcastValue = streamId.value ? Number(streamId.value) : NaN
  const productValue = Number(productId)
  if (Number.isNaN(broadcastValue) || Number.isNaN(productValue)) return
  await pinSellerBroadcastProduct(broadcastValue, productValue).catch(() => {})
}

const handlePinProduct = (productId: string) => {
  if (!isInteractive.value) return
  if (pinnedProductId.value && pinnedProductId.value !== productId) {
    openConfirm(
      {
        title: '상품 PIN 변경',
        description: 'PIN 상품을 변경하시겠습니까?',
        confirmText: '변경',
      },
      () => setPinnedProduct(productId),
    )
    return
  }
  setPinnedProduct(pinnedProductId.value === productId ? null : productId)
}

const openSanction = (username: string) => {
  if (username === 'SYSTEM') return
  sanctionTarget.value = username
  showSanctionModal.value = true
}

const applySanction = (payload: { type: string; reason: string }) => {
  if (!sanctionTarget.value) return
  sanctionedUsers.value = {
    ...sanctionedUsers.value,
    [sanctionTarget.value]: { type: payload.type, reason: payload.reason },
  }
  alert(`${sanctionTarget.value}님에게 제재가 적용되었습니다.`)
  sanctionTarget.value = null
  const now = new Date()
  const at = `${now.getHours()}시 ${String(now.getMinutes()).padStart(2, '0')}분`
  chatMessages.value = [
    ...chatMessages.value,
    {
      id: `sys-${Date.now()}`,
      name: 'SYSTEM',
      message: `${payload.type} 처리됨 (사유: ${payload.reason})`,
      time: at,
    },
  ]
  scrollChatToBottom()
}

watch(showSanctionModal, (open) => {
  if (!open) {
    sanctionTarget.value = null
  }
})

const handleSendChat = () => {
  if (!isInteractive.value) return
  if (!chatText.value.trim()) return
  chatMessages.value = [
    ...chatMessages.value,
    {
      id: `seller-${Date.now()}`,
      name: '판매자',
      message: chatText.value.trim(),
      time: formatChatTime(),
    },
  ]
  chatText.value = ''
  scrollChatToBottom()
}

watch(showChat, (open) => {
  if (open) {
    scrollChatToBottom()
  }
})

watch(showSettings, async (open) => {
  if (open) {
    await ensureLocalMediaAccess()
    await loadMediaDevices()
    await startMicMeter()
    return
  }
  stopMicMeter()
})

watch(lifecycleStatus, () => {
  const idValue = streamId.value ? Number(streamId.value) : NaN
  if (Number.isNaN(idValue)) return
  void requestJoinToken(idValue)
})

onBeforeRouteLeave(async () => {
  if (!isInteractive.value) return true
  return await confirmLeaveBroadcast()
})

watch([selectedMic, micEnabled], () => {
  if (showSettings.value) {
    void startMicMeter()
  }
})

watch(stream, (value) => {
  if (value && !hasOpenedDeviceModal.value) {
    showDeviceModal.value = true
    hasOpenedDeviceModal.value = true
  }
})

const handleBasicInfoSave = (payload: EditableBroadcastInfo) => {
  if (!broadcastInfo.value) return
  broadcastInfo.value = { ...broadcastInfo.value, ...payload }
}

const handleGoToList = () => {
  router.push({ name: 'seller-live' }).catch(() => {})
}

const handleEndBroadcast = () => {
  const idValue = streamId.value ? Number(streamId.value) : NaN
  if (Number.isNaN(idValue)) return
  endRequested.value = true
  clearEndRequestTimer()
  endRequestTimer.value = window.setTimeout(() => {
    endRequested.value = false
    endRequestTimer.value = null
  }, 10000)
  void endSellerBroadcast(idValue)
    .then(() => {
      alert('방송이 종료되었습니다.')
    })
    .catch(() => {
      endRequested.value = false
      clearEndRequestTimer()
      alert('방송 종료에 실패했습니다.')
    })
}

const requestEndBroadcast = () => {
  if (!isInteractive.value) return
  openConfirm(
    {
      title: '방송 종료',
      description: '방송 종료 시 송출이 중단되며, 시청자 화면은 대기화면으로 전환됩니다. VOD 인코딩이 자동으로 시작됩니다.',
      confirmText: '종료',
      cancelText: '취소',
    },
    handleEndBroadcast,
  )
}

const confirmLeaveBroadcast = () =>
  new Promise<boolean>((resolve) => {
    openConfirm(
      {
        title: '방송 종료',
        description: '방송 페이지를 나가면 방송이 종료됩니다. 계속 진행하시겠습니까?',
        confirmText: '종료 후 이동',
        cancelText: '취소',
      },
      () => {
        handleEndBroadcast()
        resolve(true)
      },
      () => resolve(false),
    )
  })

const toggleFullscreen = async () => {
  const el = monitorRef.value
  if (!el) return
  try {
    if (document.fullscreenElement) {
      await document.exitFullscreen()
    } else {
      await el.requestFullscreen()
    }
  } catch {
    return
  }
}
</script>

<template>
  <PageContainer>
    <header class="stream-header">
      <div>
        <h2 class="section-title">{{ displayTitle }}</h2>
        <p class="ds-section-sub">{{ displayDatetime }}</p>
      </div>
      <div class="stream-actions">
        <button type="button" class="stream-btn" :disabled="!stream" @click="showBasicInfo = true">기본정보 수정</button>
        <button type="button" class="stream-btn" :disabled="!stream || !qCards.length" @click="showQCards = true">큐카드 보기</button>
        <button type="button" class="stream-btn stream-btn--danger" :disabled="!stream" @click="requestEndBroadcast">
          방송 종료
        </button>
      </div>
    </header>

    <section
      ref="streamGridRef"
      class="stream-grid"
      :class="{
        'stream-grid--chat': showChat,
        'stream-grid--products': showProducts,
        'stream-grid--stacked': isStackedLayout,
      }"
      :style="gridStyles"
    >
      <aside
        v-if="showProducts"
        class="stream-panel stream-panel--products ds-surface"
        :class="{ 'stream-panel--readonly': isReadOnly }"
        :style="stackedOrders ? { order: stackedOrders.products } : undefined"
      >
        <div class="panel-head">
          <div class="panel-head__left">
            <h3>상품 관리</h3>
          </div>
          <button type="button" class="panel-close" aria-label="상품 관리 닫기" @click="showProducts = false">×</button>
        </div>
        <div class="panel-list">
          <div
            v-for="item in sortedProducts"
            :key="item.id"
            class="panel-item"
            :class="{ 'is-pinned': pinnedProductId === item.id }"
          >
            <div class="panel-thumb" aria-hidden="true"></div>
            <div class="panel-meta">
              <p class="panel-title">
                {{ item.title }}
                <span v-if="pinnedProductId === item.id" class="pin-badge">PIN</span>
              </p>
              <p class="panel-sub">{{ item.option }}</p>
              <span class="panel-status" :class="{ 'is-soldout': item.status === '품절' }">{{ item.status }}</span>
            </div>
            <button
              type="button"
              class="pin-btn"
              :disabled="!isInteractive || item.status === '품절'"
              :class="{ 'is-active': pinnedProductId === item.id }"
              aria-label="고정"
              @click="handlePinProduct(item.id)"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path
                  d="M9 4h6l1 5-2 2v6l-2-1-2 1v-6l-2-2 1-5z"
                  stroke="currentColor"
                  stroke-width="1.7"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </button>
          </div>
        </div>
      </aside>

      <div
        ref="streamCenterRef"
        class="stream-center ds-surface"
        :style="stackedOrders ? { order: stackedOrders.stream } : undefined"
      >
        <div class="stream-center__body">
          <div
            class="stream-player"
            :class="{
              'stream-player--fullscreen': isFullscreen,
              'stream-player--constrained': hasSidePanels,
            }"
          >
            <div class="stream-overlay stream-overlay--stack">
              <div class="stream-overlay__row">⏱ 경과 {{ elapsed }}</div>
              <div class="stream-overlay__row">👥 {{ viewerCount.toLocaleString('ko-KR') }}명</div>
              <div class="stream-overlay__row">❤ {{ likeCount.toLocaleString('ko-KR') }}</div>
            </div>
            <div class="stream-fab">
              <button
                type="button"
                class="fab-btn"
                :class="{ 'is-off': !showProducts }"
                :aria-label="showProducts ? '상품 패널 닫기' : '상품 패널 열기'"
                @click="showProducts = !showProducts"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M3 7h18l-2 12H5L3 7z" stroke="currentColor" stroke-width="1.7" />
                  <path d="M10 11v4M14 11v4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
                  <circle cx="9" cy="19" r="1" fill="currentColor" />
                  <circle cx="15" cy="19" r="1" fill="currentColor" />
                </svg>
              </button>
              <button
                type="button"
                class="fab-btn"
                :class="{ 'is-off': !showChat }"
                :aria-label="showChat ? '채팅 패널 닫기' : '채팅 패널 열기'"
                @click="showChat = !showChat"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M3 20l1.62-3.24A2 2 0 0 1 6.42 16H20a1 1 0 0 0 1-1V5a1 1 0 0 0-1-1H4a1 1 0 0 0-1 1v15z" stroke="currentColor" stroke-width="1.7" />
                  <path d="M7 9h10M7 12h6" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
                </svg>
              </button>
              <button
                type="button"
                class="fab-btn"
                :class="{ 'is-off': !showSettings }"
                aria-label="방송 설정 토글"
                @click="showSettings = !showSettings"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M4 6h16M4 12h16M4 18h16" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
                  <circle cx="9" cy="6" r="2" stroke="currentColor" stroke-width="1.7" />
                  <circle cx="14" cy="12" r="2" stroke="currentColor" stroke-width="1.7" />
                  <circle cx="7" cy="18" r="2" stroke="currentColor" stroke-width="1.7" />
                </svg>
              </button>
              <button type="button" class="fab-btn" aria-label="전체 화면" @click="toggleFullscreen">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </button>
            </div>
            <div v-if="isLoadingStream" class="stream-empty">
              <p class="stream-title">방송 정보를 불러오는 중입니다.</p>
              <p class="stream-sub">잠시만 기다려주세요.</p>
            </div>
            <div v-else-if="!stream" class="stream-empty">
              <p class="stream-title">방송 정보를 불러올 수 없습니다.</p>
              <p class="stream-sub">라이브 관리 페이지에서 다시 시도해주세요.</p>
              <div class="stream-actions">
                <button type="button" class="stream-btn" @click="handleGoToList">목록으로 이동</button>
              </div>
            </div>
            <div v-else class="stream-placeholder" :class="{ 'stream-placeholder--waiting': lifecycleStatus !== 'ON_AIR' }">
              <img
                v-if="waitingScreenUrl && lifecycleStatus !== 'ON_AIR'"
                class="stream-placeholder__image"
                :src="waitingScreenUrl"
                alt="대기 화면"
              />
              <p class="stream-title">{{ streamPlaceholderMessage }}</p>
              <p v-if="lifecycleStatus === 'ON_AIR'" class="stream-sub">현재 송출 중인 화면이 표시됩니다.</p>
              <p v-else-if="!waitingScreenUrl" class="stream-sub">대기 화면 이미지가 없습니다.</p>
            </div>
          </div>
        </div>
        <div v-if="showSettings" class="stream-settings ds-surface" role="dialog" aria-label="방송 설정">
          <div class="stream-settings__grid">
            <div class="stream-settings__group">
              <div class="stream-settings__toggles">
                <button
                  type="button"
                  class="stream-toggle"
                  :class="{ 'is-off': !micEnabled }"
                  :aria-pressed="micEnabled"
                  @click="micEnabled = !micEnabled"
                >
                  <span class="stream-toggle__icon" aria-hidden="true">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                      <rect x="9" y="4" width="6" height="10" rx="3" stroke="currentColor" stroke-width="1.7" />
                      <path d="M6 11a6 6 0 0012 0" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
                      <path d="M12 17v3" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
                    </svg>
                  </span>
                  <span>마이크</span>
                </button>
                <button
                  type="button"
                  class="stream-toggle"
                  :class="{ 'is-off': !videoEnabled }"
                  :aria-pressed="videoEnabled"
                  @click="videoEnabled = !videoEnabled"
                >
                  <span class="stream-toggle__icon" aria-hidden="true">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                      <rect x="4" y="7" width="11" height="10" rx="2" stroke="currentColor" stroke-width="1.7" />
                      <path d="M15 10l5-3v10l-5-3v-4z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                  </span>
                  <span>카메라</span>
                </button>
              </div>
            </div>
            <div class="stream-settings__group">
              <div class="stream-settings__slider">
                <span class="stream-settings__icon" aria-label="볼륨">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                    <path
                      d="M5 10h4l5-4v12l-5-4H5z"
                      stroke="currentColor"
                      stroke-width="1.7"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                    <path d="M17 9a3 3 0 010 6" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
                  </svg>
                </span>
                <input v-model.number="volume" type="range" min="0" max="100" aria-label="볼륨 조절" />
                <span class="stream-settings__value">{{ volume }}%</span>
              </div>
            </div>
            <div class="stream-settings__group stream-settings__group--end">
              <button type="button" class="stream-settings__close" aria-label="설정 닫기" @click="showSettings = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M6 6l12 12" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
                  <path d="M18 6l-12 12" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
                </svg>
              </button>
            </div>
            <div class="stream-settings__group">
              <label class="stream-settings__label">마이크</label>
              <select v-model="selectedMic" class="stream-settings__select" aria-label="마이크 선택">
                <option value="기본 마이크">기본 마이크</option>
                <option v-for="device in availableMics" :key="device.id" :value="device.id">
                  {{ device.label }}
                </option>
              </select>
            </div>
            <div class="stream-settings__group">
              <label class="stream-settings__label">카메라</label>
              <select v-model="selectedCamera" class="stream-settings__select" aria-label="카메라 선택">
                <option value="기본 카메라">기본 카메라</option>
                <option v-for="device in availableCameras" :key="device.id" :value="device.id">
                  {{ device.label }}
                </option>
              </select>
            </div>
            <div class="stream-settings__group">
              <span class="stream-settings__label">입력 레벨</span>
              <div class="stream-settings__meter" role="progressbar" :aria-valuenow="micInputLevel" aria-valuemin="0" aria-valuemax="100">
                <span class="stream-settings__meter-fill" :style="{ width: `${micInputLevel}%` }"></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <aside
        v-if="showChat"
        class="stream-panel stream-chat stream-panel--chat ds-surface"
        :class="{ 'stream-panel--readonly': isReadOnly }"
        :style="stackedOrders ? { order: stackedOrders.chat } : undefined"
      >
        <div class="panel-head">
          <div class="panel-head__left">
            <h3>실시간 채팅</h3>
          </div>
          <button type="button" class="panel-close" aria-label="채팅 패널 닫기" @click="showChat = false">×</button>
        </div>
        <div ref="chatListRef" class="panel-chat chat-messages">
          <div
            v-for="item in chatItems"
            :key="item.id"
            class="chat-message"
            :class="{ 'chat-message--muted': sanctionedUsers[item.name], 'chat-message--system': item.name === 'SYSTEM' }"
            @contextmenu.prevent="openSanction(item.name)"
          >
            <div class="chat-meta">
              <span class="chat-user">{{ item.name }}</span>
              <span class="chat-time">{{ item.time }}</span>
              <span v-if="sanctionedUsers[item.name]" class="chat-badge">{{ sanctionedUsers[item.name]?.type }}</span>
            </div>
            <p class="chat-text">{{ item.message }}</p>
          </div>
        </div>
        <div class="chat-input">
          <input
            v-model="chatText"
            type="text"
            placeholder="메시지를 입력하세요"
            :disabled="!isInteractive"
            @keyup.enter="handleSendChat"
          />
          <button type="button" class="stream-btn primary" :disabled="!isInteractive" @click="handleSendChat">전송</button>
        </div>
        <p v-if="isReadOnly" class="chat-helper">방송 중에만 채팅을 이용할 수 있습니다.</p>
      </aside>
    </section>
  <Teleport :to="modalHostTarget">
      <ConfirmModal
        v-model="confirmState.open"
        :title="confirmState.title"
        :description="confirmState.description"
        :confirm-text="confirmState.confirmText"
        :cancel-text="confirmState.cancelText"
        @confirm="handleConfirmAction"
        @cancel="handleConfirmCancel"
      />
      <QCardModal v-model="showQCards" :q-cards="qCards" :initial-index="qCardIndex" @update:initialIndex="qCardIndex = $event" />
      <BasicInfoEditModal v-if="broadcastInfo" v-model="showBasicInfo" :broadcast="broadcastInfo" @save="handleBasicInfoSave" />
      <ChatSanctionModal v-model="showSanctionModal" :username="sanctionTarget" @save="applySanction" />
    </Teleport>
    <DeviceSetupModal v-model="showDeviceModal" :broadcast-title="displayTitle" />
  </PageContainer>
</template>

<style scoped>
.stream-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.stream-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.stream-btn {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 999px;
  padding: 10px 16px;
  font-weight: 800;
  cursor: pointer;
}

.stream-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.stream-btn--danger {
  border-color: rgba(239, 68, 68, 0.35);
  color: #ef4444;
}

.stream-grid {
  display: grid;
  grid-template-columns: var(--grid-template-columns, 320px minmax(0, 1fr) 320px);
  gap: 18px;
  align-items: start;
  --stream-pane-height: clamp(300px, auto, 675px);
}

.stream-panel {
  padding: 16px;
  gap: 12px;
  height: var(--stream-pane-height);
  max-height: var(--stream-pane-height);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.stream-panel--readonly {
  opacity: 0.6;
}

.chat-helper {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 0.9rem;
  font-weight: 700;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex: 0 0 auto;
}

.panel-head__left {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.panel-close {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  width: 32px;
  height: 32px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 900;
}

.panel-head h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 900;
  color: var(--text-strong);
}

.panel-count {
  border: 1px solid var(--border-color);
  background: var(--surface-weak);
  color: var(--text-strong);
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 800;
}

.panel-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow: auto;
  flex: 1 1 auto;
  min-height: 0;
}

.panel-item {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border-radius: 12px;
  background: var(--surface-weak);
}

.panel-item.is-pinned {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.4);
  background: rgba(59, 130, 246, 0.08);
}

.panel-thumb {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  background: linear-gradient(135deg, #1f2937, #0f172a);
}

.panel-meta {
  min-width: 0;
}

.panel-title {
  margin: 0;
  color: var(--text-strong);
  font-weight: 800;
  font-size: 0.9rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: flex;
  gap: 6px;
  align-items: center;
}

.panel-sub {
  margin: 4px 0;
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.8rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.panel-status {
  display: inline-flex;
  padding: 3px 6px;
  border-radius: 999px;
  font-size: 0.7rem;
  font-weight: 800;
  color: #0f766e;
  background: rgba(16, 185, 129, 0.12);
}

.panel-status.is-soldout {
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.12);
}

.pin-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 6px;
  border-radius: 999px;
  background: var(--primary-color);
  color: #fff;
  font-size: 0.7rem;
  font-weight: 900;
}

.pin-btn {
  border: none;
  background: transparent;
  font-size: 1rem;
  cursor: pointer;
}

.pin-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pin-btn.is-active {
  color: var(--primary-color);
}

.stream-overlay {
  position: absolute;
  top: 16px;
  right: 16px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  border-radius: 12px;
  padding: 10px 12px;
  display: inline-flex;
  flex-direction: column;
  gap: 4px;
  z-index: 2;
  width: fit-content;
  min-width: 0;
}

.stream-overlay__row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 800;
  font-size: 0.8rem;
}

.stream-fab {
  position: absolute;
  bottom: 16px;
  right: 16px;
  display: grid;
  grid-auto-rows: 1fr;
  gap: 6px;
  justify-items: end;
}

.fab-btn {
  width: 39px;
  height: 39px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  cursor: pointer;
  font-size: 1.05rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
}

.fab-btn.is-off {
  opacity: 0.6;
}

.stream-center {
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 0;
  min-height: var(--stream-pane-height);
  height: var(--stream-pane-height);
  max-height: var(--stream-pane-height);
  position: relative;
  background: #1c1d21;
  width: 100%;
}

.stream-center__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

.stream-player {
  position: relative;
  width: 100%;
  height: auto;
  aspect-ratio: 16 / 9;
  border-radius: 16px;
  background: #0b0f1a;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  min-height: 320px;
  max-width: 100%;
}

.stream-player--fullscreen {
  max-height: none;
  width: min(100vw, calc(100vh * (16 / 9)));
  height: min(100vh, calc(100vw * (9 / 16)));
  border-radius: 0;
  background: #000;
}

.stream-player--constrained {
  max-width: min(100%, calc((100vh - 120px) * (16 / 9)));
}

.stream-placeholder {
  display: grid;
  gap: 8px;
  padding-top: 24px;
  text-align: center;
}

.stream-placeholder--waiting {
  gap: 12px;
  padding: 0;
  width: 100%;
  height: 100%;
  place-items: center;
}

.stream-placeholder__image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 12px;
  background: #000;
}

.stream-empty {
  display: grid;
  gap: 6px;
  padding-top: 24px;
  text-align: center;
}

.stream-title {
  margin: 0;
  font-weight: 900;
  color: var(--text-strong);
  font-size: 1.1rem;
}

.stream-sub {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
}

.panel-chat {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1 1 auto;
  min-height: 0;
  align-items: stretch;
  justify-content: flex-start;
}

.chat-messages {
  flex: 1 1 auto;
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
  gap: 6px;
}

.chat-message--system .chat-user {
  color: #ef4444;
}

.chat-message--muted .chat-text {
  color: var(--text-muted);
}

.chat-meta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.85rem;
  color: var(--text-muted);
  font-weight: 700;
}

.chat-user {
  color: var(--text-strong);
  font-weight: 800;
}

.chat-time {
  color: var(--text-muted);
  font-size: 0.8rem;
  font-weight: 700;
}

.chat-text {
  margin: 0;
  color: var(--text-strong);
  font-weight: 700;
  font-size: 0.9rem;
  line-height: 1.45;
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
  flex: 0 0 auto;
}

.chat-input input {
  flex: 1;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 10px 12px;
  font-weight: 700;
  color: var(--text-strong);
  background: var(--surface);
}

.stream-btn.primary {
  border-color: var(--primary-color);
  color: var(--primary-color);
}


.stream-settings {
  position: absolute;
  left: 50%;
  bottom: 16px;
  transform: translateX(-50%);
  width: min(920px, calc(100% - 32px));
  display: block;
  padding: 16px 20px;
  border-radius: 16px;
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.16);
  z-index: 5;
}

.stream-settings__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  align-items: center;
}

.stream-settings__group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stream-settings__group--end {
  align-items: flex-end;
}

.stream-settings__toggles {
  display: flex;
  gap: 8px;
}

.stream-toggle {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 10px;
  padding: 8px 12px;
  font-weight: 800;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  font-size: 0.9rem;
}

.stream-toggle.is-off {
  background: rgba(239, 68, 68, 0.16);
  border-color: rgba(239, 68, 68, 0.6);
  color: #b91c1c;
}

.stream-toggle.is-off::after {
  content: '';
  position: absolute;
  left: -20%;
  top: 50%;
  width: 140%;
  height: 2px;
  transform: rotate(-20deg);
  background: rgba(239, 68, 68, 0.9);
  z-index: 0;
}

.stream-toggle > * {
  position: relative;
  z-index: 1;
}

.stream-settings__slider {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stream-settings__slider input[type='range'] {
  width: 200px;
  accent-color: var(--primary-color);
}

.stream-settings__value {
  font-weight: 800;
  color: var(--text-strong);
}

.stream-settings__label {
  font-weight: 800;
  color: var(--text-strong);
}

.stream-settings__select {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 10px;
  padding: 8px 10px;
  font-weight: 700;
}

.stream-settings__meter {
  height: 10px;
  border-radius: 999px;
  background: var(--surface-weak);
  overflow: hidden;
}

.stream-settings__meter-fill {
  display: block;
  height: 100%;
  background: var(--primary-color);
  border-radius: inherit;
}

.stream-settings__close {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 10px;
  padding: 8px 10px;
  font-weight: 800;
  cursor: pointer;
}

.stream-grid:fullscreen {
  gap: 14px;
}

.stream-grid:fullscreen .stream-panel,
.stream-grid:fullscreen .stream-center {
  height: 100vh;
  max-height: 100vh;
  min-height: 0;
}

.stream-grid:fullscreen .stream-player {
  max-height: 100vh;
  width: min(100vw, calc(100vh * (16 / 9)));
  height: min(100vh, calc(100vw * (9 / 16)));
  border-radius: 0;
  background: #000;
}

.stream-grid:fullscreen.stream-grid--chat .stream-player {
  width: min(max(320px, calc(100vw - 380px)), calc(100vh * (16 / 9)));
  height: min(100vh, max(200px, calc((100vw - 380px) * (9 / 16))));
}

.stream-grid:fullscreen.stream-grid--products:not(.stream-grid--chat) .stream-player {
  width: min(max(320px, calc(100vw - 340px)), calc(100vh * (16 / 9)));
  height: min(100vh, max(200px, calc((100vw - 340px) * (9 / 16))));
}

.stream-grid:fullscreen.stream-grid--products.stream-grid--chat .stream-player {
  width: min(max(320px, calc(100vw - 720px)), calc(100vh * (16 / 9)));
  height: min(100vh, max(200px, calc((100vw - 720px) * (9 / 16))));
}

.stream-grid:not(.stream-grid--products):not(.stream-grid--chat) .stream-player {
  max-width: 100%;
}

.stream-grid:not(.stream-grid--products):not(.stream-grid--chat) {
  gap: 0;
}

.stream-grid--stacked {
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-items: stretch;
}

.stream-grid--stacked .stream-center,
.stream-grid--stacked .stream-panel {
  height: auto;
  max-height: none;
  min-height: 0;
  width: 100%;
}

.stream-grid--stacked .stream-center {
  order: 0 !important;
}

.stream-grid--stacked .stream-panel--chat {
  order: 1 !important;
}

.stream-grid--stacked .stream-panel--products {
  order: 2 !important;
}

.stream-grid--stacked .panel-head {
  flex-wrap: wrap;
  gap: 8px;
}

.stream-grid--stacked .panel-head__left {
  flex: 1 1 auto;
}

@media (max-width: 960px) {
  .stream-panel {
    overflow: visible;
  }
}

@media (max-width: 720px) {
  .stream-settings {
    flex-direction: column;
  }

  .stream-settings__grid {
    grid-template-columns: 1fr;
  }

  .stream-settings__slider input[type='range'] {
    width: 100%;
  }
}
</style>
