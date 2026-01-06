<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '../components/PageContainer.vue'
import PageHeader from '../components/PageHeader.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import { allLiveItems } from '../lib/home-data'
import { getLiveStatus, parseLiveDate } from '../lib/live/utils'
import { useNow } from '../lib/live/useNow'
import { getProductsForLive, type LiveProductItem } from '../lib/live/detail'

const route = useRoute()
const router = useRouter()
const { now } = useNow(1000)

const liveId = computed(() => {
  const value = route.params.id
  return Array.isArray(value) ? value[0] : value
})

const liveItem = computed(() => {
  if (!liveId.value) {
    return undefined
  }
  return allLiveItems.find((item) => item.id === liveId.value)
})

const status = computed(() => {
  if (!liveItem.value) {
    return undefined
  }
  return getLiveStatus(liveItem.value, now.value)
})

const statusLabel = computed(() => {
  if (status.value === 'LIVE') {
    return 'LIVE'
  }
  if (status.value === 'ENDED') {
    return '종료'
  }
  return '예정'
})

const scheduledLabel = computed(() => {
  if (!liveItem.value) {
    return ''
  }
  const start = parseLiveDate(liveItem.value.startAt)
  const dayNames = ['일', '월', '화', '수', '목', '금', '토']
  const month = String(start.getMonth() + 1).padStart(2, '0')
  const date = String(start.getDate()).padStart(2, '0')
  const day = dayNames[start.getDay()]
  const hours = String(start.getHours()).padStart(2, '0')
  const minutes = String(start.getMinutes()).padStart(2, '0')
  return `${month}.${date} (${day}) ${hours}:${minutes} 예정`
})

const products = computed<LiveProductItem[]>(() => {
  if (!liveId.value) {
    return []
  }
  return getProductsForLive(liveId.value)
})
const sortedProducts = computed(() => {
  const list = products.value.slice()
  const withPinned = list.map((item, index) => ({
    ...item,
    isPinned: index === 0,
  }))
  return withPinned.sort((a, b) => {
    if (a.isPinned && !b.isPinned) return -1
    if (!a.isPinned && b.isPinned) return 1
    if (a.isSoldOut && !b.isSoldOut) return 1
    if (!a.isSoldOut && b.isSoldOut) return -1
    return a.name.localeCompare(b.name)
  })
})

const formatPrice = (price: number) => {
  return `${price.toLocaleString('ko-KR')}원`
}

const handleProductClick = (productId: string) => {
  router.push({ name: 'product-detail', params: { id: productId } })
}

const showChat = ref(true)
const isFullscreen = ref(false)
const stageRef = ref<HTMLElement | null>(null)
const isLiked = ref(false)
const toggleLike = () => {
  isLiked.value = !isLiked.value
}

const isSettingsOpen = ref(false)
const settingsButtonRef = ref<HTMLElement | null>(null)
const settingsPanelRef = ref<HTMLElement | null>(null)
const playerPanelRef = ref<HTMLElement | null>(null)
const playerHeight = ref<number | null>(null)
let panelResizeObserver: ResizeObserver | null = null

const toggleChat = () => {
  showChat.value = !showChat.value
}

const toggleFullscreen = async () => {
  const el = stageRef.value
  if (!el) return
  try {
    if (document.fullscreenElement) {
      await document.exitFullscreen()
      isFullscreen.value = false
    } else if (el.requestFullscreen) {
      await el.requestFullscreen()
      isFullscreen.value = true
    }
  } catch {
    return
  }
}

const syncChatHeight = () => {
  if (!playerPanelRef.value) {
    return
  }
  playerHeight.value = playerPanelRef.value.getBoundingClientRect().height
}

const toggleSettings = () => {
  isSettingsOpen.value = !isSettingsOpen.value
}

type ChatMessage = {
  id: string
  user: string
  text: string
  at: Date
  kind?: 'system' | 'user'
}

const messages = ref<ChatMessage[]>([
  {
    id: 'sys-1',
    user: 'system',
    text: '라이브에 오신 것을 환영합니다.',
    at: new Date(Date.now() - 1000 * 60 * 6),
    kind: 'system',
  },
  {
    id: 'msg-1',
    user: 'desklover',
    text: '오늘 소개하는 제품이 기대돼요!',
    at: new Date(Date.now() - 1000 * 60 * 4),
    kind: 'user',
  },
  {
    id: 'msg-2',
    user: 'setup_master',
    text: '채팅 참여하실 분 손들기 🙌',
    at: new Date(Date.now() - 1000 * 60 * 2),
    kind: 'user',
  },
])

const input = ref('')
const isLoggedIn = ref(true)
const chatListRef = ref<HTMLDivElement | null>(null)

const formatChatTime = (value: Date) => {
  const hours = String(value.getHours()).padStart(2, '0')
  const minutes = String(value.getMinutes()).padStart(2, '0')
  return `${hours}:${minutes}`
}

const scrollToBottom = () => {
  nextTick(() => {
    if (!chatListRef.value) {
      return
    }
    chatListRef.value.scrollTop = chatListRef.value.scrollHeight
  })
}

const sendMessage = () => {
  if (!isLoggedIn.value) {
    return
  }
  const trimmed = input.value.trim()
  if (!trimmed) {
    return
  }
  messages.value.push({
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    user: '나',
    text: trimmed,
    at: new Date(),
    kind: 'user',
  })
  input.value = ''
  scrollToBottom()
}

onMounted(() => {
  scrollToBottom()
})

const WATCH_HISTORY_CONSENT_KEY = 'deskit_live_watch_history_consent_v1'
const hasWatchHistoryConsent = ref(false)
const showWatchHistoryConsent = ref(false)

const requestWatchHistoryConsent = () => {
  try {
    hasWatchHistoryConsent.value =
      typeof localStorage !== 'undefined' && localStorage.getItem(WATCH_HISTORY_CONSENT_KEY) === 'true'
  } catch {
    hasWatchHistoryConsent.value = false
  }

  if (!hasWatchHistoryConsent.value) {
    showWatchHistoryConsent.value = true
  }
}

const handleConfirmWatchHistory = () => {
  hasWatchHistoryConsent.value = true
  showWatchHistoryConsent.value = false
  try {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(WATCH_HISTORY_CONSENT_KEY, 'true')
    }
  } catch {
    return
  }
}

const handleCancelWatchHistory = () => {
  showWatchHistoryConsent.value = false
  router.push({ name: 'live' }).catch(() => {})
}

onMounted(() => {
  panelResizeObserver = new ResizeObserver(() => {
    syncChatHeight()
  })
  if (playerPanelRef.value) {
    panelResizeObserver.observe(playerPanelRef.value)
  }
  nextTick(() => {
    syncChatHeight()
  })
  requestWatchHistoryConsent()
})

const handleDocumentClick = (event: MouseEvent) => {
  if (!isSettingsOpen.value) {
    return
  }
  const target = event.target as Node | null
  if (
    settingsButtonRef.value?.contains(target) ||
    settingsPanelRef.value?.contains(target)
  ) {
    return
  }
  isSettingsOpen.value = false
}

const handleDocumentKeydown = (event: KeyboardEvent) => {
  if (!isSettingsOpen.value) {
    return
  }
  if (event.key === 'Escape') {
    isSettingsOpen.value = false
  }
}

const handleFullscreenChange = () => {
  isFullscreen.value = Boolean(document.fullscreenElement)
}

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
  document.addEventListener('keydown', handleDocumentKeydown)
  document.addEventListener('fullscreenchange', handleFullscreenChange)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  document.removeEventListener('keydown', handleDocumentKeydown)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  if (panelResizeObserver && playerPanelRef.value) {
    panelResizeObserver.unobserve(playerPanelRef.value)
  }
  panelResizeObserver?.disconnect()
})
</script>

<template>
  <PageContainer>
    <ConfirmModal
      v-model="showWatchHistoryConsent"
      title="시청 기록 수집 안내"
      description="라이브 방송 입장 시 시청 기록이 수집됩니다. 계속 진행하시겠습니까?"
      confirm-text="동의하고 입장하기"
      cancel-text="취소"
      @confirm="handleConfirmWatchHistory"
      @cancel="handleCancelWatchHistory"
    />
    <PageHeader eyebrow="DESKIT LIVE" title="라이브 상세" />

    <div v-if="!liveItem" class="empty-state">
      <p>라이브를 찾을 수 없습니다.</p>
      <RouterLink to="/live" class="link-back">라이브 일정으로 돌아가기</RouterLink>
    </div>

    <section v-else class="live-detail-layout">
      <div
        class="live-detail-main"
        :class="{ 'live-detail-main--chat': showChat }"
        :style="{
          gridTemplateColumns: showChat ? 'minmax(0, 1.6fr) minmax(0, 0.95fr)' : 'minmax(0, 1fr)',
        }"
      >
        <section ref="playerPanelRef" class="panel panel--player">
          <div class="player-meta">
            <div class="status-row">
              <span class="status-badge" :class="`status-badge--${status?.toLowerCase()}`">
                {{ statusLabel }}
              </span>
              <span v-if="status === 'LIVE' && liveItem.viewerCount" class="status-viewers">
                {{ liveItem.viewerCount.toLocaleString() }}명 시청 중
              </span>
              <span v-else-if="status === 'UPCOMING'" class="status-schedule">
                {{ scheduledLabel }}
              </span>
              <span v-else-if="status === 'ENDED'" class="status-ended">방송 종료</span>
            </div>
            <h3 class="player-title">{{ liveItem.title }}</h3>
            <p v-if="liveItem.description" class="player-desc">{{ liveItem.description }}</p>
          </div>

          <div ref="stageRef" class="player-frame" :class="{ 'player-frame--fullscreen': isFullscreen }">
            <span class="player-frame__label" v-if="status === 'ENDED'">대기 화면</span>
            <span class="player-frame__label" v-else>LIVE 플레이어</span>
            <div class="player-actions">
              <button
                type="button"
                class="icon-circle"
                :class="{ active: isLiked }"
                aria-label="좋아요"
                @click="toggleLike"
              >
                <svg class="icon" viewBox="0 0 24 24" aria-hidden="true">
                  <path
                    v-if="isLiked"
                    d="M12.1 21.35l-1.1-1.02C5.14 15.24 2 12.39 2 8.99 2 6.42 4.02 4.5 6.58 4.5c1.54 0 3.04.74 3.92 1.91C11.38 5.24 12.88 4.5 14.42 4.5 16.98 4.5 19 6.42 19 8.99c0 3.4-3.14 6.25-8.9 11.34l-1.1 1.02z"
                    fill="currentColor"
                  />
                  <path
                    v-else
                    d="M12.1 21.35l-1.1-1.02C5.14 15.24 2 12.39 2 8.99 2 6.42 4.02 4.5 6.58 4.5c1.54 0 3.04.74 3.92 1.91C11.38 5.24 12.88 4.5 14.42 4.5 16.98 4.5 19 6.42 19 8.99c0 3.4-3.14 6.25-8.9 11.34l-1.1 1.02z"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.8"
                  />
                </svg>
              </button>
              <button
                type="button"
                class="icon-circle"
                :class="{ active: showChat }"
                aria-label="채팅 패널 토글"
                @click="toggleChat"
              >
                <svg class="icon" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M3 20l1.62-3.24A2 2 0 0 1 6.42 16H20a1 1 0 0 0 1-1V5a1 1 0 0 0-1-1H4a1 1 0 0 0-1 1v15z" fill="none" stroke="currentColor" stroke-width="1.8" />
                  <path d="M7 9h10M7 12h6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                </svg>
              </button>
              <div class="player-settings">
                <button
                  ref="settingsButtonRef"
                  type="button"
                  class="icon-circle"
                  aria-controls="player-settings"
                  :aria-expanded="isSettingsOpen ? 'true' : 'false'"
                  aria-label="설정"
                  @click="toggleSettings"
                >
                  <svg class="icon" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M4 6h16M4 12h16M4 18h16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                    <circle cx="9" cy="6" r="2" fill="none" stroke="currentColor" stroke-width="1.8" />
                    <circle cx="14" cy="12" r="2" fill="none" stroke="currentColor" stroke-width="1.8" />
                    <circle cx="7" cy="18" r="2" fill="none" stroke="currentColor" stroke-width="1.8" />
                  </svg>
                </button>
                <div
                  v-if="isSettingsOpen"
                  id="player-settings"
                  ref="settingsPanelRef"
                  class="settings-popover"
                >
                  <label class="settings-row">
                    <span class="settings-label">볼륨</span>
                    <input
                      class="toolbar-slider"
                      type="range"
                      min="0"
                      max="100"
                      value="60"
                      aria-label="볼륨 조절"
                    />
                  </label>
                  <label class="settings-row">
                    <span class="settings-label">화질</span>
                    <select class="settings-select" aria-label="화질">
                      <option>자동</option>
                      <option>1080p</option>
                      <option>720p</option>
                      <option>480p</option>
                    </select>
                  </label>
                </div>
              </div>
              <button type="button" class="icon-circle" aria-label="전체 화면" @click="toggleFullscreen">
                <svg class="icon" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </button>
            </div>
          </div>
        </section>

        <aside
          v-if="showChat"
          class="chat-panel ds-surface"
          :style="{ height: playerHeight ? `${playerHeight}px` : undefined }"
        >
          <header class="chat-head">
            <h4>실시간 채팅</h4>
            <button type="button" class="chat-close" aria-label="채팅 닫기" @click="toggleChat">×</button>
          </header>
          <div ref="chatListRef" class="chat-messages">
            <div
              v-for="message in messages"
              :key="message.id"
              class="chat-message"
              :class="{ 'chat-message--system': message.kind === 'system' }"
            >
              <div class="chat-meta">
                <span class="chat-user">{{ message.user }}</span>
                <span class="chat-time">{{ formatChatTime(message.at) }}</span>
              </div>
              <p class="chat-text">{{ message.text }}</p>
            </div>
          </div>
          <div class="chat-input">
            <input
              v-model="input"
              type="text"
              placeholder="메시지를 입력하세요"
              :disabled="!isLoggedIn"
              @keydown.enter="sendMessage"
            />
            <button
              type="button"
              class="btn primary"
              :disabled="!isLoggedIn || !input.trim()"
              @click="sendMessage"
            >
              전송
            </button>
          </div>
          <p v-if="!isLoggedIn" class="chat-helper">로그인 후 이용하실 수 있습니다.</p>
        </aside>
      </div>

      <section class="panel panel--products">
        <div class="panel__header">
          <h3 class="panel__title">라이브 상품</h3>
          <span class="panel__count">{{ products.length }}개</span>
        </div>
        <div v-if="!products.length" class="panel__empty">등록된 상품이 없습니다.</div>
        <div v-else class="product-list product-list--grid">
          <button
            v-for="product in sortedProducts"
            :key="product.id"
            type="button"
            class="product-card"
            @click="handleProductClick(product.id)"
          >
            <img class="product-card__thumb" :src="product.imageUrl" :alt="product.name" />
            <div class="product-card__info">
              <p class="product-card__name">{{ product.name }}</p>
              <p class="product-card__price">{{ formatPrice(product.price) }}</p>
              <span v-if="product.isSoldOut" class="product-card__badge">품절</span>
            </div>
          </button>
        </div>
      </section>
    </section>
  </PageContainer>
</template>

<style scoped>
.live-detail-layout {
  display: flex;
  flex-direction: column;
  gap: 18px;
  overflow-x: hidden;
}

.live-detail-main {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.panel {
  border: 1px solid var(--border-color);
  background: var(--surface);
  border-radius: 16px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel__title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 800;
  color: var(--text-strong);
}

.panel__count {
  font-weight: 700;
  color: var(--text-soft);
}

.panel__empty {
  color: var(--text-muted);
  padding: 10px 0;
}

.panel--products {
  overflow: hidden;
}

.product-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-list--grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
}

.product-card {
  border: 1px solid var(--border-color);
  background: var(--surface);
  border-radius: 14px;
  padding: 10px;
  display: grid;
  grid-template-columns: 64px 1fr;
  gap: 12px;
  cursor: pointer;
  text-align: left;
}

.product-card__thumb {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  object-fit: cover;
}

.product-card__info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-card__name {
  margin: 0;
  font-weight: 700;
  color: var(--text-strong);
}

.product-card__price {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.95rem;
}

.product-card__badge {
  align-self: flex-start;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--surface-weak);
  color: var(--text-muted);
  font-size: 0.75rem;
  font-weight: 700;
}

.settings-popover {
  position: absolute;
  top: 0;
  right: calc(100% + 10px);
  background: var(--surface);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12);
  min-width: 220px;
  display: grid;
  gap: 10px;
}

.settings-select {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 10px;
  height: 36px;
  padding: 0 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, color 0.2s ease;
}

.settings-select:hover {
  border-color: var(--primary-color);
}

.settings-select:focus-visible,
.toolbar-slider:focus-visible {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
}

.toolbar-slider {
  accent-color: var(--primary-color);
  width: 140px;
}

.settings-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.settings-label {
  font-weight: 800;
  color: var(--text-strong);
}

.viewer-stage {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.player-frame {
  position: relative;
  width: 100%;
  height: auto;
  aspect-ratio: 16 / 9;
  background: #272d3b;
  border-radius: 16px;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 700;
  min-height: clamp(160px, auto, 560px);
  max-width: min(100%, calc((100vh - 180px) * (16 / 9)));
  overflow: hidden;
}

.player-frame--fullscreen,
.player-frame:fullscreen {
  width: min(100vw, calc(100vh * (16 / 9)));
  height: min(100vh, calc(100vw * (9 / 16)));
  max-height: 100vh;
  max-width: 100vw;
  border-radius: 0;
  background: #000;
}

.player-frame:fullscreen iframe,
.player-frame:fullscreen video,
.player-frame:fullscreen img {
  object-fit: contain;
}

.player-frame__label {
  opacity: 0.8;
}

.player-actions {
  position: absolute;
  right: 14px;
  bottom: 14px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  z-index: 2;
}

.player-settings {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
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
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
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
  stroke-width: 1.7px;
}

.chat-panel {
  width: 360px;
  max-width: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 16px;
  padding: 12px;
  gap: 10px;
  background: var(--surface);
  border: 1px solid var(--border-color);
  min-height: 0;
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

.chat-time {
  color: var(--text-muted);
}

.chat-input {
  display: flex;
  gap: 8px;
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

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.chat-helper {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
}

.panel--player {
  gap: 16px;
}

.player-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.status-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 999px;
  font-weight: 800;
  font-size: 0.85rem;
  background: var(--surface-weak);
  color: var(--text-strong);
}

.status-badge--live {
  background: var(--live-color-soft);
  color: var(--live-color);
}

.status-badge--upcoming {
  background: var(--hover-bg);
  color: var(--primary-color);
}

.status-badge--ended {
  background: var(--border-color);
  color: var(--text-muted);
}

.status-viewers {
  color: var(--text-muted);
  font-weight: 700;
}

.status-schedule {
  color: var(--text-muted);
  font-weight: 700;
}

.status-ended {
  color: var(--text-soft);
  font-weight: 700;
}

.player-title {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 800;
}

.player-desc {
  margin: 0;
  color: var(--text-muted);
}

@media (max-width: 640px) {
  .live-detail-main {
    gap: 14px;
  }

  .chat-input {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1120px) {
  .live-detail-main {
    grid-template-columns: 1fr !important;
  }

  .chat-panel {
    width: 100%;
    height: auto !important;
  }
}
</style>
