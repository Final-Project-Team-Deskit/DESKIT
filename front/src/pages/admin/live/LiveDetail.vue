<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ADMIN_LIVES_EVENT, getAdminLiveSummaries, stopAdminLiveBroadcast } from '../../../lib/mocks/adminLives'

const route = useRoute()
const router = useRouter()

const liveId = computed(() => (typeof route.params.liveId === 'string' ? route.params.liveId : ''))

const detail = ref<ReturnType<typeof getAdminLiveSummaries>[number] | null>(null)

const stageRef = ref<HTMLDivElement | null>(null)
const isFullscreen = ref(false)
const showStopModal = ref(false)
const stopReason = ref('')
const stopDetail = ref('')
const error = ref('')
const showChat = ref(false)
const chatText = ref('')
const chatMessages = ref<{ id: string; user: string; text: string; time: string }[]>([])
const chatListRef = ref<HTMLDivElement | null>(null)
const seededLiveId = ref('')
const showModerationModal = ref(false)
const moderationTarget = ref<{ user: string } | null>(null)
const moderationType = ref('')
const moderationReason = ref('')
const moderatedUsers = ref<Record<string, { type: string; reason: string; at: string }>>({})

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

const seedChat = (id: string) => {
  chatMessages.value = [
    { id: `${id}-c1`, user: '관리자', text: '상태 모니터링 중입니다.', time: '오후 2:10' },
    { id: `${id}-c2`, user: '시청자A', text: '화질 좋네요.', time: '오후 2:11' },
    { id: `${id}-c3`, user: '시청자B', text: '상품 정보 공유 부탁해요.', time: '오후 2:12' },
  ]
}

const goBack = () => {
  router.back()
}

const goToList = () => {
  router.push('/admin/live?tab=live').catch(() => {})
}

const loadDetail = () => {
  const items = getAdminLiveSummaries()
  detail.value = items.find((item) => item.id === liveId.value) ?? items[0] ?? null
  const seedId = liveId.value || items[0]?.id || 'live'
  if (seededLiveId.value !== seedId) {
    chatMessages.value = []
    seedChat(seedId)
    seededLiveId.value = seedId
  }
}

const openStopConfirm = () => {
  if (!detail.value || detail.value.status === '송출중지') return
  const ok = window.confirm('해당 방송의 송출을 중지하시겠습니까?\n시청자에게는 대기 화면이 노출됩니다.')
  if (!ok) return
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
  stopAdminLiveBroadcast(detail.value.id, {
    reason: stopReason.value,
    detail: stopReason.value === '기타' ? stopDetail.value.trim() : undefined,
  })
  showStopModal.value = false
}

const syncFullscreen = () => {
  isFullscreen.value = Boolean(document.fullscreenElement)
}

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
  loadDetail()
  document.addEventListener('fullscreenchange', syncFullscreen)
  window.addEventListener(ADMIN_LIVES_EVENT, loadDetail)
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', syncFullscreen)
  window.removeEventListener(ADMIN_LIVES_EVENT, loadDetail)
})

watch(liveId, loadDetail, { immediate: true })
</script>

<template>
  <div v-if="detail" class="live-detail">
    <header class="detail-header">
      <button type="button" class="back-link" @click="goBack">← 뒤로 가기</button>
      <div class="header-actions">
        <button type="button" class="btn" @click="goToList">목록으로</button>
        <button type="button" class="btn danger" :disabled="detail.status === '송출중지'" @click="openStopConfirm">
          {{ detail.status === '송출중지' ? '송출 중지됨' : '방송 송출 중지' }}
        </button>
        <button type="button" class="btn primary" @click="toggleFullscreen">
          {{ isFullscreen ? '전체화면 종료' : '전체화면' }}
        </button>
      </div>
    </header>

    <h2 class="page-title">방송 모니터링</h2>

    <section class="detail-card ds-surface meta-card">
      <div class="detail-meta">
        <h3>{{ detail.title }}</h3>
        <p><span>판매자</span>{{ detail.sellerName }}</p>
        <p><span>방송 시작</span>{{ detail.startedAt }}</p>
        <p><span>시청자 수</span>{{ detail.viewers }}명</p>
        <p><span>상태</span>{{ detail.status }}</p>
      </div>
    </section>

    <section class="player-card">
      <div ref="stageRef" class="monitor-stage" :class="{ 'monitor-stage--chat': showChat }">
        <div class="player-wrap">
          <div class="player-frame">
            <div class="player-overlay">
              <div class="overlay-item">⏱ {{ detail.elapsed }}</div>
              <div class="overlay-item">👥 {{ detail.viewers }}명</div>
              <div class="overlay-item">❤ {{ detail.likes }}</div>
            </div>
            <button type="button" class="chat-toggle" @click="toggleChat">
              {{ showChat ? '채팅 닫기' : '채팅' }}
            </button>
            <div class="player-label">송출 화면</div>
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
                <span v-if="msg.user !== 'SYSTEM' && moderatedUsers[msg.user]" class="chat-badge">{{ moderatedUsers[msg.user].type }}</span>
              </div>
              <p class="chat-text">{{ msg.text }}</p>
            </div>
          </div>
          <div class="chat-input">
            <input v-model="chatText" type="text" placeholder="메시지를 입력하세요" />
            <button type="button" class="btn primary" @click="sendChat">전송</button>
          </div>
        </aside>
      </div>
    </section>

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
  align-items: stretch;
  position: relative;
}

.player-wrap {
  flex: 1;
  min-width: 0;
}

.player-frame {
  position: relative;
  width: 100%;
  min-height: 56vh;
  aspect-ratio: 16 / 9;
  background: #0b0f1a;
  border-radius: 18px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.player-label {
  color: rgba(255, 255, 255, 0.6);
  font-weight: 800;
  letter-spacing: 0.08em;
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

@media (max-width: 900px) {
  .player-frame {
    min-height: 46vh;
  }

  .monitor-stage {
    position: relative;
  }

  .chat-panel {
    position: absolute;
    top: 12px;
    right: 12px;
    bottom: 12px;
    width: min(360px, 88vw);
    z-index: 2;
  }
}
</style>
