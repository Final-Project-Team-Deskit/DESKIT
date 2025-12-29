<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import PageContainer from '../../components/PageContainer.vue'

const route = useRoute()
const showProducts = ref(true)
const showChat = ref(true)
const showSettings = ref(false)
const viewerCount = ref(1010)
const likeCount = ref(1574)
const elapsed = ref('02:01:44')
const micEnabled = ref(true)
const videoEnabled = ref(true)
const volume = ref(43)
const selectedMic = ref('기본 마이크')
const selectedCamera = ref('기본 카메라')
const micInputLevel = ref(70)
const streamId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' && id.trim() ? id : null
})

const streamMap = {
  'live-1': {
    title: '홈오피스 라이브 스냅',
    datetime: '오늘 14:00 - 15:00',
    products: [
      { id: 'lp-1', title: '모던 스탠딩 데스크', option: '1200mm · 오프화이트', status: '판매중' },
      { id: 'lp-2', title: '로우 프로파일 키보드', option: '무선 · 베이지', status: '판매중' },
      { id: 'lp-3', title: '미니멀 데스크 매트', option: '900mm · 샌드', status: '품절' },
      { id: 'lp-4', title: '알루미늄 모니터암', option: '싱글 · 블랙', status: '판매중' },
    ],
    chat: [
      { id: 'c-1', name: '연두', message: '라이브 시작했나요?' },
      { id: 'c-2', name: '민지', message: '가격 할인 언제까지인가요?' },
      { id: 'c-3', name: '도현', message: '블랙 색상 재입고 예정 있나요?' },
      { id: 'c-4', name: '지수', message: '오늘 배송 가능할까요?' },
    ],
  },
  'live-2': {
    title: '게이밍 데스크 셋업',
    datetime: '오늘 16:30 - 17:10',
    products: [
      { id: 'lp-5', title: '게이밍 데스크 패드', option: 'XL · 블랙', status: '판매중' },
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

const stream = computed(() => {
  if (!streamId.value) return null
  return streamMap[streamId.value as keyof typeof streamMap] ?? null
})

const productItems = computed(() => stream.value?.products ?? [])
const chatItems = computed(() => stream.value?.chat ?? [])

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && showSettings.value) {
    showSettings.value = false
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <PageContainer>
    <header class="stream-header">
      <div>
        <h2 class="section-title">{{ stream ? stream.title : '방송 진행' }}</h2>
        <p class="ds-section-sub">
          {{ stream ? stream.datetime : '실시간 송출 화면과 판매 상품, 채팅을 관리합니다.' }}
        </p>
      </div>
      <div class="stream-actions">
        <button type="button" class="stream-btn">기본정보 수정</button>
        <button type="button" class="stream-btn">큐카드 보기</button>
        <button type="button" class="stream-btn stream-btn--danger">방송 종료</button>
      </div>
    </header>

    <section
      class="stream-grid"
      :style="{
        gridTemplateColumns:
          showProducts && showChat
            ? '280px minmax(0, 1fr) 280px'
            : showProducts
              ? '280px minmax(0, 1fr)'
              : showChat
                ? 'minmax(0, 1fr) 280px'
                : 'minmax(0, 1fr)',
      }"
    >
      <aside v-if="showProducts" class="stream-panel ds-surface">
        <div class="panel-head">
          <h3>상품 관리</h3>
          <span class="panel-count">{{ productItems.length }}개</span>
        </div>
        <div class="panel-list">
          <div v-for="item in productItems" :key="item.id" class="panel-item">
            <div class="panel-thumb" aria-hidden="true"></div>
            <div class="panel-meta">
              <p class="panel-title">{{ item.title }}</p>
              <p class="panel-sub">{{ item.option }}</p>
              <span class="panel-status" :class="{ 'is-soldout': item.status === '품절' }">{{ item.status }}</span>
            </div>
            <button type="button" class="pin-btn" aria-label="고정">
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

      <div class="stream-center ds-surface">
        <div class="stream-overlay stream-overlay--left">
          <div class="stream-overlay__row">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path
                d="M8 12a3 3 0 100-6 3 3 0 000 6z"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <path
                d="M16 12a3 3 0 100-6 3 3 0 000 6z"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <path
                d="M4 18c.4-2 2.6-3.5 4.8-3.5"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <path
                d="M20 18c-.4-2-2.6-3.5-4.8-3.5"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <span>{{ viewerCount.toLocaleString('ko-KR') }}명 시청 중</span>
          </div>
          <div class="stream-overlay__row">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <circle cx="12" cy="13" r="7" stroke="currentColor" stroke-width="1.7" />
              <path
                d="M12 3h4"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
              />
              <path
                d="M12 13V9"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
              />
            </svg>
            <span>경과 {{ elapsed }}</span>
          </div>
        </div>
        <div class="stream-overlay stream-overlay--right">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path
              d="M12 20s-7-4.4-7-9a4 4 0 017-2 4 4 0 017 2c0 4.6-7 9-7 9z"
              stroke="currentColor"
              stroke-width="1.7"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <span>{{ likeCount.toLocaleString('ko-KR') }}</span>
        </div>
        <div class="stream-fab">
          <button
            type="button"
            class="fab-btn"
            :class="{ 'is-off': !showProducts }"
            aria-label="상품 패널 토글"
            @click="showProducts = !showProducts"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path
                d="M4 7l4-3h8l4 3-2 4h-3v9H9v-9H6L4 7z"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>
          <button
            type="button"
            class="fab-btn"
            :class="{ 'is-off': !showChat }"
            aria-label="채팅 패널 토글"
            @click="showChat = !showChat"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path
                d="M5 6h14a3 3 0 013 3v6a3 3 0 01-3 3H9l-4 3v-3H5a3 3 0 01-3-3V9a3 3 0 013-3z"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
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
              <path
                d="M12 9a3 3 0 100 6 3 3 0 000-6z"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <path
                d="M4 12a8 8 0 011-3l2 1 2-2-1-2a8 8 0 013-1l1 2h2l1-2a8 8 0 013 1l-1 2 2 2 2-1a8 8 0 011 3l-2 1v2l2 1a8 8 0 01-1 3l-2-1-2 2 1 2a8 8 0 01-3 1l-1-2h-2l-1 2a8 8 0 01-3-1l1-2-2-2-2 1a8 8 0 01-1-3l2-1v-2l-2-1z"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>
        </div>
        <div class="stream-center__body">
          <div v-if="!stream" class="stream-empty">
            <p class="stream-title">방송 정보를 불러올 수 없습니다.</p>
            <p class="stream-sub">라이브 관리 페이지에서 다시 시도해주세요.</p>
          </div>
          <div v-else class="stream-placeholder">
            <p class="stream-title">송출 화면 (WebRTC Stream)</p>
            <p class="stream-sub">현재 송출 중인 화면이 표시됩니다.</p>
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

      <aside v-if="showChat" class="stream-panel stream-chat ds-surface">
        <div class="panel-head">
          <h3>실시간 채팅</h3>
          <span class="panel-count">{{ chatItems.length }}명</span>
        </div>
        <div class="panel-chat">
          <div v-for="item in chatItems" :key="item.id" class="chat-item">
            <span class="chat-name">{{ item.name }}</span>
            <span class="chat-message">{{ item.message }}</span>
          </div>
        </div>
      </aside>
    </section>


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

.stream-btn--danger {
  border-color: rgba(239, 68, 68, 0.35);
  color: #ef4444;
}

.stream-grid {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr) 280px;
  gap: 18px;
  align-items: start;
  --stream-pane-height: clamp(420px, 60vh, 620px);
}

.stream-panel {
  padding: 16px;
  gap: 12px;
  height: var(--stream-pane-height);
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

.pin-btn {
  border: none;
  background: transparent;
  font-size: 1rem;
  cursor: pointer;
}

.stream-center {
  padding: 24px;
  height: var(--stream-pane-height);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  position: relative;
}

.stream-overlay {
  position: absolute;
  top: 14px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  border-radius: 12px;
  padding: 10px 12px;
  display: grid;
  gap: 6px;
}

.stream-overlay--left {
  left: 14px;
}

.stream-overlay--right {
  right: 14px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 800;
}

.stream-overlay__row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 800;
  font-size: 0.9rem;
}

.stream-fab {
  position: absolute;
  bottom: 16px;
  right: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.fab-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  cursor: pointer;
  font-size: 1.05rem;
}

.fab-btn.is-off {
  opacity: 0.6;
}

.stream-center__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  display: block;
  padding-bottom: 140px;
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
  overflow-y: auto;
  padding-right: 4px;
  flex: 1 1 auto;
  min-height: 0;
  align-items: stretch;
  justify-content: flex-start;
}

.chat-item {
  display: grid;
  gap: 4px;
  padding: 8px 10px;
  border-radius: 12px;
  background: var(--surface-weak);
  flex: 0 0 auto;
  align-self: stretch;
}

.chat-name {
  font-weight: 800;
  color: var(--text-strong);
  font-size: 0.85rem;
}

.chat-message {
  display: block;
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.85rem;
  line-height: 1.35;
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

@media (max-width: 960px) {
  .stream-grid {
    grid-template-columns: 1fr;
  }

  .stream-panel {
    height: auto;
    overflow: visible;
    min-height: auto;
  }

  .stream-center {
    order: -1;
    height: auto;
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
