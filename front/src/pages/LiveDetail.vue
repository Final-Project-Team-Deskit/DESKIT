<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '../components/PageContainer.vue'
import PageHeader from '../components/PageHeader.vue'
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

const formatPrice = (price: number) => {
  return `${price.toLocaleString('ko-KR')}원`
}

const handleProductClick = (productId: string) => {
  router.push({ name: 'product-detail', params: { id: productId } })
}

const handleVod = () => {
  if (!liveItem.value) {
    return
  }
  router.push({ name: 'vod', params: { id: liveItem.value.id } })
}

const isLiked = ref(false)
const toggleLike = () => {
  isLiked.value = !isLiked.value
}

const isSettingsOpen = ref(false)
const settingsButtonRef = ref<HTMLElement | null>(null)
const settingsPanelRef = ref<HTMLElement | null>(null)
const playerPanelRef = ref<HTMLElement | null>(null)
const chatPanelRef = ref<HTMLElement | null>(null)
const playerHeight = ref<number | null>(null)
let panelResizeObserver: ResizeObserver | null = null

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

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
  document.addEventListener('keydown', handleDocumentKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  document.removeEventListener('keydown', handleDocumentKeydown)
  if (panelResizeObserver && playerPanelRef.value) {
    panelResizeObserver.unobserve(playerPanelRef.value)
  }
  panelResizeObserver?.disconnect()
})
</script>

<template>
  <PageContainer>
    <PageHeader eyebrow="DESKIT LIVE" title="라이브 상세" />

    <div v-if="!liveItem" class="empty-state">
      <p>라이브를 찾을 수 없습니다.</p>
      <RouterLink to="/live" class="link-back">라이브 일정으로 돌아가기</RouterLink>
    </div>

    <section v-else class="live-detail-layout">
      <div class="live-detail-main">
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

        <div class="player-frame">
          <span class="player-frame__label">LIVE 플레이어</span>
        </div>

        <div class="player-toolbar">
          <div class="player-toolbar__group player-toolbar__group--left">
            <button
              type="button"
              class="toolbar-btn"
              :class="{ 'toolbar-btn--active': isLiked }"
              :aria-label="isLiked ? '좋아요' : '좋아요'"
              @click="toggleLike"
            >
              <svg class="toolbar-svg" viewBox="0 0 24 24" aria-hidden="true">
                <path v-if="isLiked" d="M12.1 21.35l-1.1-1.02C5.14 15.24 2 12.39 2 8.99 2 6.42 4.02 4.5 6.58 4.5c1.54 0 3.04.74 3.92 1.91C11.38 5.24 12.88 4.5 14.42 4.5 16.98 4.5 19 6.42 19 8.99c0 3.4-3.14 6.25-8.9 11.34l-1.1 1.02z" fill="currentColor" />
                <path v-else d="M12.1 21.35l-1.1-1.02C5.14 15.24 2 12.39 2 8.99 2 6.42 4.02 4.5 6.58 4.5c1.54 0 3.04.74 3.92 1.91C11.38 5.24 12.88 4.5 14.42 4.5 16.98 4.5 19 6.42 19 8.99c0 3.4-3.14 6.25-8.9 11.34l-1.1 1.02z" fill="none" stroke="currentColor" stroke-width="1.8" />
              </svg>
              <span class="toolbar-label">{{ isLiked ? '좋아요' : '좋아요' }}</span>
            </button>
          </div>
          <div class="player-toolbar__group player-toolbar__group--right">
            <div class="toolbar-settings">
              <button
                ref="settingsButtonRef"
                type="button"
                class="toolbar-btn"
                aria-controls="player-settings"
                :aria-expanded="isSettingsOpen ? 'true' : 'false'"
                aria-label="설정"
                @click="toggleSettings"
              >
                <svg class="toolbar-svg" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M4 6h16M4 12h16M4 18h16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                  <circle cx="9" cy="6" r="2" fill="none" stroke="currentColor" stroke-width="1.8" />
                  <circle cx="14" cy="12" r="2" fill="none" stroke="currentColor" stroke-width="1.8" />
                  <circle cx="7" cy="18" r="2" fill="none" stroke="currentColor" stroke-width="1.8" />
                </svg>
                <span class="toolbar-label">설정</span>
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
            <button type="button" class="toolbar-btn" aria-label="전체화면">
              <svg class="toolbar-svg" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
              <span class="toolbar-label">전체화면</span>
            </button>
          </div>
        </div>

        <button v-if="status === 'ENDED'" type="button" class="vod-btn" @click="handleVod">
          VOD 다시보기
        </button>
        </section>

        <aside
          ref="chatPanelRef"
          class="panel panel--chat"
          :style="{ height: playerHeight ? `${playerHeight}px` : undefined }"
        >
          <div class="panel__header">
            <h3 class="panel__title">실시간 채팅</h3>
          </div>
          <div ref="chatListRef" class="chat-list">
            <div
              v-for="message in messages"
              :key="message.id"
              class="chat-message"
              :class="{ 'chat-message--system': message.kind === 'system' }"
            >
              <span class="chat-message__user">{{ message.user }}</span>
              <p class="chat-message__text">{{ message.text }}</p>
              <span class="chat-message__time">{{ formatChatTime(message.at) }}</span>
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
            <button type="button" :disabled="!isLoggedIn || !input.trim()" @click="sendMessage">
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
            v-for="product in products"
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

.panel--player {
  gap: 16px;
}

.panel--chat {
  gap: 12px;
  min-height: 0;
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

.player-frame {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #10131b;
  border-radius: 16px;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 700;
}

.player-frame__label {
  opacity: 0.8;
}

.player-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-top: 1px solid var(--border-color);
  background: var(--surface-weak);
  border-radius: 12px;
  flex-wrap: nowrap;
}

.player-toolbar__group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.player-toolbar__group--left {
  justify-content: flex-start;
}

.player-toolbar__group--center {
  justify-content: center;
}

.player-toolbar__group--right {
  justify-content: flex-end;
}

.toolbar-btn,
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

.toolbar-btn {
  cursor: pointer;
}

.toolbar-btn:hover,
.settings-select:hover {
  border-color: var(--primary-color);
}

.toolbar-btn:focus-visible,
.settings-select:focus-visible,
.toolbar-slider:focus-visible {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
}

.toolbar-btn--active {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.toolbar-svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.toolbar-label {
  white-space: nowrap;
}

.toolbar-slider {
  width: 100%;
  height: 36px;
  accent-color: var(--primary-color);
  background: transparent;
}

.toolbar-settings {
  position: relative;
}

.settings-popover {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  min-width: 220px;
  background: var(--surface);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12);
  display: grid;
  gap: 10px;
  z-index: 5;
}

.settings-row {
  display: grid;
  gap: 6px;
}

.settings-label {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--text-muted);
}

.settings-select {
  appearance: none;
  background-image: linear-gradient(45deg, transparent 50%, var(--text-muted) 50%),
    linear-gradient(135deg, var(--text-muted) 50%, transparent 50%);
  background-position: calc(100% - 14px) 50%, calc(100% - 8px) 50%;
  background-size: 6px 6px, 6px 6px;
  background-repeat: no-repeat;
  padding-right: 28px;
  cursor: pointer;
}

.vod-btn {
  border: none;
  background: var(--primary-color);
  color: #fff;
  font-weight: 800;
  border-radius: 12px;
  padding: 12px 16px;
  cursor: pointer;
  align-self: flex-start;
}

.chat-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.chat-message {
  display: grid;
  gap: 4px;
  padding: 8px 10px;
  border-radius: 12px;
  background: var(--surface-weak);
}

.chat-message--system {
  background: var(--hover-bg);
  color: var(--text-muted);
}

.chat-message__user {
  font-weight: 800;
  font-size: 0.85rem;
}

.chat-message__text {
  margin: 0;
  color: var(--text-strong);
}

.chat-message__time {
  font-size: 0.75rem;
  color: var(--text-soft);
}

.chat-input {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

.chat-input input {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 0.95rem;
}

.chat-input button {
  border: none;
  background: var(--primary-color);
  color: #fff;
  font-weight: 800;
  border-radius: 10px;
  padding: 10px 16px;
  cursor: pointer;
}

.chat-input button:disabled,
.chat-input input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.chat-helper {
  margin: 0;
  color: var(--text-soft);
  font-size: 0.85rem;
}

.empty-state {
  display: flex;
  flex-direction: column;
  gap: 12px;
  color: var(--text-muted);
}

.link-back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: var(--primary-color);
}

.product-list--grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
}

@media (max-width: 1080px) {
  .live-detail-main {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .live-detail-main {
    gap: 14px;
  }

  .player-toolbar {
    gap: 8px;
    padding: 8px 10px;
  }

  .toolbar-label {
    display: none;
  }

  .toolbar-btn {
    height: 36px;
    padding: 0 8px;
  }

  .chat-input {
    grid-template-columns: 1fr;
  }
}
</style>
