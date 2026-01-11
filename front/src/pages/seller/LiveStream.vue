<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Client, type StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'
import BasicInfoEditModal from '../../components/BasicInfoEditModal.vue'
import ChatSanctionModal from '../../components/ChatSanctionModal.vue'
import ConfirmModal from '../../components/ConfirmModal.vue'
import PageContainer from '../../components/PageContainer.vue'
import QCardModal from '../../components/QCardModal.vue'
import { getAuthUser } from '../../lib/auth'

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
  senderRole?: string
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

type EditableBroadcastInfo = Pick<StreamData, 'title' | 'category' | 'notice' | 'thumbnail' | 'waitingScreen'>

const defaultNotice = '판매 상품 외 다른 상품 문의는 받지 않습니다.'

const route = useRoute()
const router = useRouter()
const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const showProducts = ref(true)
const showChat = ref(true)
const showSettings = ref(false)
const viewerCount = ref<number | null>(null)
const displayViewerCount = computed(() => viewerCount.value ?? 0)
const likeCount = ref(1574)
const elapsed = ref('02:01:44')
const monitorRef = ref<HTMLElement | null>(null)
const streamGridRef = ref<HTMLElement | null>(null)
const isFullscreen = ref(false)
const modalHostTarget = computed(() => (isFullscreen.value && monitorRef.value ? monitorRef.value : 'body'))
const micEnabled = ref(true)
const videoEnabled = ref(true)
const volume = ref(43)
const selectedMic = ref('기본 마이크')
const selectedCamera = ref('기본 카메라')
const micInputLevel = ref(70)
const input = ref('')
const chatListRef = ref<HTMLElement | null>(null)
let gridObserver: ResizeObserver | null = null

const showQCards = ref(false)
const showBasicInfo = ref(false)
const showSanctionModal = ref(false)
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

const pinnedProductId = ref<string | null>(null)
const sanctionTarget = ref<string | null>(null)
const sanctionedUsers = ref<Record<string, { type: string; reason: string }>>({})
const broadcastInfo = ref<(EditableBroadcastInfo & { qCards: string[] }) | null>(null)
const stream = ref<StreamData | null>(null)
const messages = ref<StreamChat[]>([])

const defaultChatTimes = ['오후 2:10', '오후 2:12', '오후 2:14']

const streamId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' && id.trim() ? id : null
})

const broadcastId = computed(() => {
  if (!streamId.value) {
    return undefined
  }
  const raw = String(streamId.value)
  const numeric = Number.parseInt(raw.replace(/[^0-9]/g, ''), 10)
  return Number.isFinite(numeric) ? numeric : undefined
})

const streamMap: Record<string, StreamData> = {
  'live-1': {
    title: '홈오피스 라이브 스냅',
    datetime: '오늘 14:00 - 15:00',
    category: '홈오피스',
    notice: defaultNotice,
    qCards: ['오늘의 대표 상품은 무엇인가요?', '배송 스케줄과 사은품 안내 부탁드립니다.'],
    products: [
      { id: 'lp-1', title: '모던 스탠딩 데스크', option: '1200mm · 오프화이트', status: '판매중', pinned: true },
      { id: 'lp-2', title: '로우 프로파일 키보드', option: '무선 · 베이지', status: '판매중' },
      { id: 'lp-3', title: '미니멀 데스크 매트', option: '900mm · 샌드', status: '품절' },
      { id: 'lp-4', title: '알루미늄 모니터암', option: '싱글 · 블랙', status: '판매중' },
    ],
    chat: [
    ],
  },
  'live-2': {
    title: '게이밍 데스크 셋업',
    datetime: '오늘 16:30 - 17:10',
    category: '주변기기',
    notice: defaultNotice,
    qCards: ['방송 순서와 할인 적용 시점을 안내해주세요.', '특정 색상 재입고 일정이 궁금합니다.'],
    products: [
      { id: 'lp-5', title: '게이밍 데스크 패드', option: 'XL · 블랙', status: '판매중', pinned: true },
      { id: 'lp-6', title: 'RGB 데스크 램프', option: 'USB · 네온', status: '판매중' },
      { id: 'lp-7', title: '헤드셋 스탠드', option: '알루미늄', status: '품절' },
    ],
    chat: [
      { id: 'c-5', name: '지훈', message: 'LED 밝기 조절도 되나요?' },
      { id: 'c-6', name: '소연', message: '블랙 데스크 매트 재입고 계획 있나요?' },
      { id: 'c-7', name: '준호', message: '다음 방송 일정 알려주세요.' },
    ],
  },
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

const chatItems = computed(() => messages.value)

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

const hydrateStream = () => {
  isLoadingStream.value = true
  const id = streamId.value
  const next = id ? streamMap[id] ?? null : null
  stream.value = next

  if (!next) {
    pinnedProductId.value = null
    broadcastInfo.value = null
    messages.value = []
    isLoadingStream.value = false
    return
  }

  pinnedProductId.value = next.products.find((item) => item.pinned)?.id ?? null
  broadcastInfo.value = {
    title: next.title,
    category: next.category,
    notice: next.notice ?? defaultNotice,
    thumbnail: next.thumbnail,
    waitingScreen: next.waitingScreen,
    qCards: next.qCards,
  }
  messages.value = (next.chat ?? []).map((item, index) => ({
    ...item,
    time: item.time ?? defaultChatTimes[index % defaultChatTimes.length],
  }))
  isLoadingStream.value = false
  scrollChatToBottom()
}

watch(
  () => route.params.id,
  () => {
    hydrateStream()
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
  monitorRef.value = streamGridRef.value
  updateGridWidth()
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
  gridObserver?.disconnect()
})

const openConfirm = (options: Partial<typeof confirmState>, onConfirm: () => void) => {
  confirmState.title = options.title ?? ''
  confirmState.description = options.description ?? ''
  confirmState.confirmText = options.confirmText ?? '확인'
  confirmState.cancelText = options.cancelText ?? '취소'
  confirmAction.value = onConfirm
  confirmState.open = true
}

const handleConfirmAction = () => {
  confirmAction.value?.()
  confirmAction.value = () => {}
}

const setPinnedProduct = (productId: string | null) => {
  pinnedProductId.value = productId
}

const handlePinProduct = (productId: string) => {
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
  messages.value = [
    ...messages.value,
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

const formatChatTime = () => {
  const now = new Date()
  const hours = now.getHours()
  const displayHour = hours % 12 || 12
  const minutes = String(now.getMinutes()).padStart(2, '0')
  return `${hours >= 12 ? '오후' : '오전'} ${displayHour}:${minutes}`
}

// const handleSendChat = () => {
//   if (!input.value.trim()) return
//   chatMessages.value = [
//     ...chatMessages.value,
//     {
//       id: `seller-${Date.now()}`,
//       name: '판매자',
//       message: input.value.trim(),
//       time: formatChatTime(),
//     },
//   ]
//   input.value = ''
//   scrollChatToBottom()
// }

watch(showChat, (open) => {
  if (open) {
    scrollChatToBottom()
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
  alert('방송이 종료되었습니다.')
  router.push({ name: 'seller-live' })
}

const requestEndBroadcast = () => {
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

type LiveMessageType = 'TALK' | 'ENTER' | 'EXIT' | 'PURCHASE' | 'NOTICE'

type LiveChatMessageDTO = {
  broadcastId: number
  memberEmail: string
  type: LiveMessageType
  sender: string
  content: string
  senderRole?: string
  vodPlayTime: number
  sentAt?: number
}

const isChatConnected = ref(false)
const stompClient = ref<Client | null>(null)
let stompSubscription: StompSubscription | null = null
let viewerSubscription: StompSubscription | null = null

const isLoggedIn = ref(true)
const nickname = ref('판매자')
const memberEmail = ref('seller@deskit.com')
const ENTER_SENT_KEY_PREFIX = 'deskit_live_enter_sent_v1'

const getAccessToken = () => {
  return localStorage.getItem('access') || sessionStorage.getItem('access')
}

const refreshAuth = () => {
  const user = getAuthUser()
  isLoggedIn.value = user !== null
  if (user?.name) {
    nickname.value = user.name
  }
  memberEmail.value = user?.email || memberEmail.value || 'seller@deskit.com'
}

const handleAuthUpdate = () => {
  refreshAuth()
}

const getEnterSentKey = () => {
  if (!broadcastId.value) {
    return null
  }
  return `${ENTER_SENT_KEY_PREFIX}:${broadcastId.value}`
}

const shouldSendEnterMessage = () => {
  const key = getEnterSentKey()
  if (!key) {
    return false
  }
  try {
    return localStorage.getItem(key) !== 'true'
  } catch {
    return true
  }
}

const markEnterMessageSent = () => {
  const key = getEnterSentKey()
  if (!key) {
    return
  }
  try {
    localStorage.setItem(key, 'true')
  } catch {
    return
  }
}

// 1. 메시지를 화면 배열에 추가
const appendMessage = (payload: LiveChatMessageDTO) => {
  const isSystem = payload.type !== 'TALK'
  messages.value.push({
    id: `chat-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    name: isSystem ? 'SYSTEM' : payload.sender || 'unknown',
    message: payload.content ?? '',
    senderRole: payload.senderRole,
    time: payload.sentAt ? new Date(payload.sentAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }) : formatChatTime()
  })
  scrollChatToBottom()
}

const formatChatUser = (item: StreamChat) => {
  if (item.name === 'SYSTEM') {
    return item.name
  }
  if (item.senderRole) {
    if (item.senderRole === 'ROLE_ADMIN') {
      return `${item.name}(관리자)`
    }
    if (item.senderRole.startsWith('ROLE_SELLER')) {
      return `${item.name}(판매자)`
    }
    if (item.senderRole === 'ROLE_MEMBER' && item.name === nickname.value) {
      return `${item.name}(나)`
    }
  }
  return item.name === nickname.value ? `${item.name}(판매자)` : item.name
}

const fetchRecentMessages = async () => {
  if (!broadcastId.value) {
    return
  }
  try {
    const response = await fetch(`${apiBase}/livechats/${broadcastId.value}/recent?seconds=60`)
    if (!response.ok) {
      return
    }
    const recent = (await response.json()) as LiveChatMessageDTO[]
    if (!Array.isArray(recent) || recent.length === 0) {
      return
    }
    messages.value = recent
      .filter((item) => item.type === 'TALK')
      .map((item) => {
        const at = new Date(item.sentAt ?? Date.now())
          return {
            id: `${item.sentAt ?? Date.now()}-${Math.random().toString(16).slice(2)}`,
            name: item.sender || 'unknown',
            message: item.content ?? '',
            senderRole: item.senderRole,
            time: at.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
          }
        })
    scrollChatToBottom()
  } catch (error) {
    console.error('[livechat] recent fetch failed', error)
  }
}

// 2. 소켓을 통해 메시지 전송
const sendSocketMessage = (type: LiveMessageType, content: string) => {
  if (!stompClient.value?.connected || !broadcastId.value) return

  const payload: LiveChatMessageDTO = {
    broadcastId: broadcastId.value,
    memberEmail: memberEmail.value,
    type,
    sender: nickname.value,
    content,
    vodPlayTime: 0,
    sentAt: Date.now()
  }

  stompClient.value.publish({
    destination: '/pub/chat/message',
    body: JSON.stringify(payload)
  })
}

// 3. 채팅방 연결
const connectChat = () => {
  if (!broadcastId.value || stompClient.value?.active) return

  const client = new Client({
    webSocketFactory: () =>
      new SockJS(`${apiBase}/ws`, undefined, {
        withCredentials: true,
      }),
    reconnectDelay: 5000,
    debug: (str) => console.log('[STOMP]', str)
  })
  const access = getAccessToken()
  if (access) {
    client.connectHeaders = {
      access,
      Authorization: `Bearer ${access}`
    }
  }

  client.onConnect = () => {
    isChatConnected.value = true
    // 구독 시작
    stompSubscription?.unsubscribe()
    stompSubscription = client.subscribe(`/sub/chat/${broadcastId.value}`, (frame) => {
      try {
        const payload = JSON.parse(frame.body) as LiveChatMessageDTO
        appendMessage(payload)
      } catch (e) {
        console.error('메시지 수신 에러:', e)
      }
    })
    viewerSubscription?.unsubscribe()
    viewerSubscription = client.subscribe(`/sub/live/${broadcastId.value}/viewers`, (frame) => {
      try {
        const payload = JSON.parse(frame.body) as { broadcastId: number; viewers: number }
        if (typeof payload.viewers === 'number') {
          viewerCount.value = payload.viewers
        }
      } catch {
        const count = Number.parseInt(frame.body, 10)
        if (Number.isFinite(count)) {
          viewerCount.value = count
        }
      }
    })

    // 입장 알림
    if (shouldSendEnterMessage()) {
      sendSocketMessage('ENTER', `${nickname.value}님이 입장했습니다.`)
      markEnterMessageSent()
    }
  }

  client.onStompError = (frame) => {
    console.error('[livechat] stomp error', frame.headers, frame.body)
  }

  client.onWebSocketClose = () => { isChatConnected.value = false }
  client.onDisconnect = () => { isChatConnected.value = false }
  stompClient.value = client
  client.activate()
}

// 4. 채팅방 해제
const disconnectChat = () => {
  if (stompClient.value?.connected) {
    sendSocketMessage('EXIT', `${nickname.value}님이 퇴장했습니다.`)
  }
  stompSubscription?.unsubscribe()
  stompSubscription = null
  viewerSubscription?.unsubscribe()
  viewerSubscription = null
  stompClient.value?.deactivate()
  stompClient.value = null
  isChatConnected.value = false
}

// 5. 메시지 전송 버튼 핸들러 (UI와 연결)
const handleSendChat = () => {
  if (!input.value.trim() || !isChatConnected.value || !isLoggedIn.value) return
  sendSocketMessage('TALK', input.value.trim())
  input.value = ''
}

onMounted(() => {
  refreshAuth()
  window.addEventListener('deskit-user-updated', handleAuthUpdate)
})

// 6. 방송 ID 변경 시 재연결 및 정리
watch(broadcastId, (newId) => {
  messages.value = []
  viewerCount.value = null
  disconnectChat()
  if (newId) {
    fetchRecentMessages()
    connectChat()
  }
}, { immediate: true })

onBeforeUnmount(() => {
  window.removeEventListener('deskit-user-updated', handleAuthUpdate)
  disconnectChat()
})
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
              :disabled="item.status === '품절'"
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
              <div class="stream-overlay__row">👥 {{ displayViewerCount.toLocaleString('ko-KR') }}명</div>
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
            <div v-else class="stream-placeholder">
              <p class="stream-title">송출 화면 (WebRTC Stream)</p>
              <p class="stream-sub">현재 송출 중인 화면이 표시됩니다.</p>
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
                <option>기본 마이크</option>
                <option>USB 마이크</option>
                <option>블루투스 마이크</option>
              </select>
            </div>
            <div class="stream-settings__group">
              <label class="stream-settings__label">카메라</label>
              <select v-model="selectedCamera" class="stream-settings__select" aria-label="카메라 선택">
                <option>기본 카메라</option>
                <option>외장 카메라</option>
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
              <span class="chat-user">{{ formatChatUser(item) }}</span>
              <span class="chat-time">{{ item.time }}</span>
              <span v-if="sanctionedUsers[item.name]" class="chat-badge">{{ sanctionedUsers[item.name].type }}</span>
            </div>
            <p class="chat-text">{{ item.message }}</p>
          </div>
        </div>
        <div class="chat-input">
          <input
            v-model="input"
            type="text"
            placeholder="메시지를 입력하세요"
            :disabled="!isLoggedIn || !isChatConnected"
            @keyup.enter="handleSendChat"
          />
          <button type="button" class="stream-btn primary" :disabled="!isLoggedIn || !isChatConnected" @click="handleSendChat">전송</button>
        </div>
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
      />
      <QCardModal v-model="showQCards" :q-cards="qCards" :initial-index="qCardIndex" @update:initialIndex="qCardIndex = $event" />
      <BasicInfoEditModal v-if="broadcastInfo" v-model="showBasicInfo" :broadcast="broadcastInfo" @save="handleBasicInfoSave" />
      <ChatSanctionModal v-model="showSanctionModal" :username="sanctionTarget" @save="applySanction" />
    </Teleport>
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
  overflow: auto;
  min-height: 320px;
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




