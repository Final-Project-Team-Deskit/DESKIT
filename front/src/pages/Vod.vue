<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '../components/PageContainer.vue'
import PageHeader from '../components/PageHeader.vue'
import { liveItems } from '../lib/live/data'
import { getLiveStatus, parseLiveDate } from '../lib/live/utils'
import { useNow } from '../lib/live/useNow'
import { getProductsForLive, type LiveProductItem } from '../lib/live/detail'

const route = useRoute()
const router = useRouter()
const { now } = useNow(1000)

const vodId = computed(() => {
  const value = route.params.id
  return Array.isArray(value) ? value[0] : value
})

const vodItem = computed(() => {
  if (!vodId.value) {
    return undefined
  }
  return liveItems.find((entry) => entry.id === vodId.value)
})

const status = computed(() => {
  if (!vodItem.value) {
    return undefined
  }
  return getLiveStatus(vodItem.value, now.value)
})

const statusLabel = computed(() => {
  if (status.value === 'LIVE') {
    return 'LIVE'
  }
  if (status.value === 'UPCOMING') {
    return '예정'
  }
  if (status.value === 'ENDED') {
    return '다시보기'
  }
  return 'VOD'
})

const statusBadgeClass = computed(() => {
  if (!status.value) {
    return 'status-badge--ended'
  }
  return `status-badge--${status.value.toLowerCase()}`
})

const playerPanelRef = ref<HTMLElement | null>(null)
const chatPanelRef = ref<HTMLElement | null>(null)
const playerHeight = ref<number | null>(null)
let panelResizeObserver: ResizeObserver | null = null

const showChat = ref(true)
const isFullscreen = ref(false)
const stageRef = ref<HTMLElement | null>(null)
const isLiked = ref(false)
const isSettingsOpen = ref(false)
const settingsButtonRef = ref<HTMLElement | null>(null)
const settingsPanelRef = ref<HTMLElement | null>(null)

const toggleLike = () => {
  isLiked.value = !isLiked.value
}

const toggleChat = () => {
  showChat.value = !showChat.value
}

const toggleSettings = () => {
  isSettingsOpen.value = !isSettingsOpen.value
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

const products = computed<LiveProductItem[]>(() => {
  if (!vodItem.value) {
    return []
  }
  return getProductsForLive(vodItem.value.id)
})

const messages = ref(
  [
    {
      id: 'sys-1',
      user: 'system',
      text: 'VOD 채팅 기록을 보고 계십니다.',
      at: new Date(Date.now() - 1000 * 60 * 6),
      kind: 'system',
    },
    {
      id: 'msg-1',
      user: 'desklover',
      text: '이 라이브 제품 너무 좋았어요!',
      at: new Date(Date.now() - 1000 * 60 * 4),
      kind: 'user',
    },
    {
      id: 'msg-2',
      user: 'setup_master',
      text: '배송도 빨랐습니다 👍',
      at: new Date(Date.now() - 1000 * 60 * 2),
      kind: 'user',
    },
  ] as Array<{ id: string; user: string; text: string; at: Date; kind?: 'system' | 'user' }>,
)

const chatListRef = ref<HTMLDivElement | null>(null)
const chatInput = ref('')

const formatPrice = (price: number) => `${price.toLocaleString('ko-KR')}원`

const scheduledLabel = computed(() => {
  if (!vodItem.value) {
    return ''
  }
  const start = parseLiveDate(vodItem.value.startAt)
  const dayNames = ['일', '월', '화', '수', '목', '금', '토']
  const month = String(start.getMonth() + 1).padStart(2, '0')
  const date = String(start.getDate()).padStart(2, '0')
  const day = dayNames[start.getDay()]
  const hours = String(start.getHours()).padStart(2, '0')
  const minutes = String(start.getMinutes()).padStart(2, '0')
  return `${month}.${date} (${day}) ${hours}:${minutes} 예정`
})

const formatSchedule = (startAt: string, endAt: string) => {
  const dayNames = ['일', '월', '화', '수', '목', '금', '토']
  const start = parseLiveDate(startAt)
  const end = parseLiveDate(endAt)
  const year = start.getFullYear()
  const month = String(start.getMonth() + 1).padStart(2, '0')
  const day = String(start.getDate()).padStart(2, '0')
  const dayLabel = dayNames[start.getDay()]
  const startHours = String(start.getHours()).padStart(2, '0')
  const startMinutes = String(start.getMinutes()).padStart(2, '0')
  const endHours = String(end.getHours()).padStart(2, '0')
  const endMinutes = String(end.getMinutes()).padStart(2, '0')
  return `${year}.${month}.${day} (${dayLabel}) ${startHours}:${startMinutes} ~ ${endHours}:${endMinutes}`
}

const isEmbedUrl = (url: string) => url.includes('youtube.com/embed') || url.includes('player.vimeo.com')

const handleProductClick = (productId: string) => {
  router.push({ name: 'product-detail', params: { id: productId } })
}

watch(
  [status, vodItem],
  ([nextStatus, nextItem]) => {
    if (nextStatus === 'LIVE' && nextItem) {
      router.replace({ name: 'live-detail', params: { id: nextItem.id } })
    }
  },
  { immediate: true },
)

const formatChatTime = (value: Date) => {
  const hours = String(value.getHours()).padStart(2, '0')
  const minutes = String(value.getMinutes()).padStart(2, '0')
  return `${hours}:${minutes}`
}

const scrollChatToBottom = () => {
  nextTick(() => {
    if (chatListRef.value) {
      chatListRef.value.scrollTop = chatListRef.value.scrollHeight
    }
  })
}

onMounted(() => {
  scrollChatToBottom()
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

const handleFullscreenChange = () => {
  isFullscreen.value = Boolean(document.fullscreenElement)
}

onBeforeUnmount(() => {
  if (panelResizeObserver && playerPanelRef.value) {
    panelResizeObserver.unobserve(playerPanelRef.value)
  }
  panelResizeObserver?.disconnect()
  document.removeEventListener('click', handleDocumentClick)
  document.removeEventListener('keydown', handleDocumentKeydown)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
  document.addEventListener('keydown', handleDocumentKeydown)
  document.addEventListener('fullscreenchange', handleFullscreenChange)
})

watch(showChat, (visible) => {
  if (visible) {
    scrollChatToBottom()
  }
})
</script>

<template>
  <PageContainer>
    <PageHeader title="VOD 다시보기" eyebrow="DESKIT VOD" />

    <div v-if="!vodItem" class="empty-state">
      <p>VOD를 찾을 수 없습니다.</p>
      <RouterLink to="/live" class="link-back">라이브 일정으로 돌아가기</RouterLink>
    </div>

    <section v-else class="live-detail-layout">
      <div
        class="live-detail-main"
        :style="{
          gridTemplateColumns: showChat ? 'minmax(0, 1.6fr) minmax(0, 0.95fr)' : 'minmax(0, 1fr)',
        }"
      >
        <section ref="playerPanelRef" class="panel panel--player">
          <div class="player-meta">
            <div class="status-row">
              <span class="status-badge" :class="statusBadgeClass">{{ statusLabel }}</span>
              <span v-if="status === 'LIVE' && vodItem.viewerCount" class="status-viewers">
                {{ vodItem.viewerCount.toLocaleString() }}명 시청 중
              </span>
              <span v-else-if="status === 'UPCOMING'" class="status-schedule">
                {{ scheduledLabel }}
              </span>
              <span v-else-if="status === 'ENDED'" class="status-ended">방송 종료</span>
            </div>
            <h3 class="player-title">{{ vodItem.title }}</h3>
            <span> {{ formatSchedule(vodItem.startAt, vodItem.endAt)}}</span>
            <p v-if="vodItem.description" class="player-desc">{{ vodItem.description }}</p>
            <p v-if="vodItem.sellerName" class="player-seller">{{ vodItem.sellerName }}</p>
          </div>

          <div class="player-frame" ref="stageRef" :class="{ 'player-frame--fullscreen': isFullscreen }">
            <span v-if="status === 'UPCOMING'" class="player-frame__label">아직 시작 전입니다</span>
            <span v-else-if="!vodItem.vodUrl" class="player-frame__label">VOD 준비 중</span>
            <iframe
              v-else-if="vodItem.vodUrl && isEmbedUrl(vodItem.vodUrl)"
              class="player-embed"
              :src="vodItem.vodUrl"
              title="VOD 플레이어"
              frameborder="0"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
              allowfullscreen
            />
            <video v-else class="player-video" :src="vodItem.vodUrl" controls />

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
                      disabled
                    />
                  </label>
                  <label class="settings-row">
                    <span class="settings-label">화질</span>
                    <select class="settings-select" aria-label="화질" disabled>
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
          ref="chatPanelRef"
          class="chat-panel ds-surface"
        :style="{ height: playerHeight ? `${playerHeight}px` : undefined }"
        >
          <header class="chat-head">
            <h4>채팅 기록</h4>
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
              v-model="chatInput"
              type="text"
              readonly
              placeholder="VOD에서는 채팅을 보실 수만 있어요"
            />
            <button type="button" class="btn primary" disabled>전송</button>
          </div>
<!--          <p class="chat-helper">VOD에서는 채팅 기록만 볼 수 있어요.</p>-->
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
            :class="{ 'product-card--sold-out': product.isSoldOut }"
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
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.product-card:hover {
  border-color: var(--primary-color);
  box-shadow: 0 10px 22px rgba(119, 136, 115, 0.12);
  transform: translateY(-1px);
}

.product-card--sold-out {
  opacity: 0.7;
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
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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

.player-seller {
  margin: 0;
  font-weight: 700;
  color: var(--text-strong);
}

.player-frame {
  position: relative;
  width: 100%;
  height: auto;
  aspect-ratio: 16 / 9;
  background: #10131b;
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

.player-frame:fullscreen .player-embed,
.player-frame:fullscreen .player-video {
  object-fit: contain;
}

.player-frame__label {
  position: absolute;
  z-index: 2;
  opacity: 0.9;
  padding: 10px 14px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.55);
}

.player-embed,
.player-video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
  object-fit: contain;
  background: #0b0f18;
}

.player-actions {
  position: absolute;
  right: 14px;
  bottom: 14px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  z-index: 3;
}

.player-settings {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
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

@media (max-width: 640px) {
  .live-detail-main {
    gap: 14px;
  }

  .product-list--grid {
    grid-template-columns: 1fr;
  }

  .product-card {
    grid-template-columns: 1fr;
  }

  .product-card__thumb {
    width: 100%;
    height: 160px;
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
