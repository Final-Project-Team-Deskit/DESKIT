<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '../../components/PageContainer.vue'
import PageHeader from '../../components/PageHeader.vue'
import { getAuthUser, requestLogout } from '../../lib/auth'

const router = useRouter()

const display = computed(() => {
  const user = getAuthUser()
  return {
    name: user?.name || '판매자',
    email: user?.email || '',
    signupType: user?.signupType || '',
    memberCategory: user?.memberCategory || '판매자',
    sellerRole: user?.sellerRole || '오너',
  }
})

const handleLogout = async () => {
  const success = await requestLogout()
  if (success) {
    window.alert('로그아웃되었습니다.')
  }
  router.push('/').catch(() => {})
}

const managers = ref([
  {
    id: 'manager-1',
    name: '김지우',
    email: 'jiwoo.manager@test.com',
    role: '매니저',
  },
  {
    id: 'manager-2',
    name: '박서준',
    email: 'seojun.manager@test.com',
    role: '부매니저',
  },
])

const showManagerModal = ref(false)
const showConfirmModal = ref(false)
const showSent = ref(false)
const managerEmail = ref('')
const pendingEmail = ref('')
const emailInputRef = ref<HTMLInputElement | null>(null)
const isEmailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(managerEmail.value.trim()))

const openManagerModal = () => {
  showManagerModal.value = true
  managerEmail.value = ''
  pendingEmail.value = ''
  showSent.value = false
  nextTick(() => {
    emailInputRef.value?.focus()
  })
}

const closeManagerModal = () => {
  showManagerModal.value = false
  managerEmail.value = ''
  pendingEmail.value = ''
  showConfirmModal.value = false
  showSent.value = false
}

const openConfirm = () => {
  if (!isEmailValid.value) return
  pendingEmail.value = managerEmail.value.trim()
  showConfirmModal.value = true
}

const closeConfirm = () => {
  showConfirmModal.value = false
}

const confirmSend = () => {
  if (!pendingEmail.value) return
  console.log('[manager login link]', pendingEmail.value)
  showConfirmModal.value = false
  showSent.value = true
}

const handleModalClose = () => {
  if (showSent.value) {
    closeManagerModal()
    return
  }
  if (showConfirmModal.value) {
    closeConfirm()
    return
  }
  closeManagerModal()
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && showManagerModal.value) {
    if (showSent.value) {
      closeManagerModal()
      return
    }
    if (showConfirmModal.value) {
      closeConfirm()
      return
    }
    closeManagerModal()
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
    <PageHeader eyebrow="DESKIT" title="판매자 마이페이지" />
    <section class="seller-card ds-surface">
      <div class="seller-card__top">
        <div class="seller-avatar" aria-hidden="true">S</div>
        <div class="seller-meta">
          <p class="seller-name">{{ display.name }}</p>
          <p class="seller-email">{{ display.email }}</p>
        </div>
      </div>
      <dl class="seller-info">
        <div class="seller-info__row">
          <dt>가입 유형</dt>
          <dd>{{ display.signupType }}</dd>
        </div>
        <div class="seller-info__row">
          <dt>회원 분류</dt>
          <dd>{{ display.memberCategory }}</dd>
        </div>
        <div class="seller-info__row">
          <dt>판매자 역할</dt>
          <dd>{{ display.sellerRole }}</dd>
        </div>
      </dl>
      <button type="button" class="seller-logout" @click="handleLogout">로그아웃</button>
    </section>
    <section class="manager-card ds-surface">
      <div class="manager-head">
        <button type="button" class="manager-add" @click="openManagerModal">매니저 등록</button>
      </div>
      <div class="manager-body">
        <div class="manager-list">
          <p class="manager-title">등록된 매니저</p>
          <ul v-if="managers.length" class="manager-items">
            <li v-for="manager in managers" :key="manager.id" class="manager-item">
              <div class="manager-meta">
                <span class="manager-name">{{ manager.name }}</span>
                <span class="manager-role">{{ manager.role }}</span>
              </div>
              <span class="manager-email">{{ manager.email }}</span>
            </li>
          </ul>
          <p v-else class="manager-empty">등록된 매니저가 없습니다.</p>
        </div>
      </div>
    </section>

    <div v-if="showManagerModal" class="manager-modal" role="dialog" aria-modal="true" aria-label="매니저 등록">
      <div class="manager-modal__backdrop" @click="handleModalClose"></div>
      <div class="manager-modal__card ds-surface">
        <div class="manager-modal__head">
          <div>
            <h3>매니저 등록</h3>
            <p>매니저 이메일로 로그인 링크를 보내드립니다.</p>
          </div>
          <button type="button" class="modal-close" @click="handleModalClose" aria-label="닫기">✕</button>
        </div>
        <form v-if="!showConfirmModal && !showSent" class="manager-form" @submit.prevent="openConfirm">
          <label class="field">
            <span class="field__label">매니저 이메일</span>
            <input
              ref="emailInputRef"
              v-model="managerEmail"
              class="field-input"
              :class="{ 'field-input--error': managerEmail.trim() && !isEmailValid }"
              type="email"
              placeholder="manager@company.com"
              autocomplete="email"
              required
            />
          </label>
          <p class="manager-error" :class="{ 'is-visible': managerEmail.trim() && !isEmailValid }">
            이메일 형식을 확인해 주세요.
          </p>
          <div class="manager-actions">
            <button type="button" class="btn ghost" @click="closeManagerModal">취소</button>
            <button type="submit" class="btn primary" :disabled="!isEmailValid">로그인 링크 보내기</button>
          </div>
        </form>
        <div v-else-if="showConfirmModal" class="confirm-body">
          <p class="confirm-title">다음 이메일로 로그인 링크를 보낼까요?</p>
          <p class="confirm-email">{{ pendingEmail }}</p>
          <div class="manager-actions">
            <button type="button" class="btn ghost" @click="closeConfirm">뒤로</button>
            <button type="button" class="btn primary" @click="confirmSend">보내기</button>
          </div>
        </div>
        <div v-else class="sent-body">
          <p class="sent-title">로그인 링크를 보냈습니다.</p>
          <p class="sent-email">{{ pendingEmail }}</p>
          <div class="manager-actions">
            <button type="button" class="btn primary" @click="closeManagerModal">확인</button>
          </div>
        </div>
      </div>
    </div>
  </PageContainer>
</template>

<style scoped>
.manager-card {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 18px;
}

.manager-head {
  display: flex;
  justify-content: flex-end;
}

.manager-add {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 999px;
  padding: 10px 16px;
  font-weight: 800;
  cursor: pointer;
}

.manager-body {
  display: flex;
}

.manager-list {
  min-width: 0;
}

.manager-title {
  margin: 0 0 10px;
  font-weight: 800;
  color: var(--text-strong);
}

.manager-items {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 10px;
}

.manager-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: var(--surface);
}

.manager-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 800;
  color: var(--text-strong);
}

.manager-name {
  font-weight: 900;
}

.manager-role {
  font-size: 0.85rem;
  color: var(--text-muted);
  font-weight: 700;
}

.manager-email {
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.9rem;
}

.manager-empty {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.9rem;
}

.manager-modal {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: grid;
  place-items: center;
  padding: 24px;
}

.manager-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
}

.manager-modal__card {
  position: relative;
  z-index: 1;
  width: min(520px, 100%);
  border-radius: 16px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.12);
}

.manager-modal__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.manager-modal__head h3 {
  margin: 0 0 6px;
  font-size: 1.1rem;
  font-weight: 900;
  color: var(--text-strong);
}

.manager-modal__head p {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.9rem;
}

.modal-close {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 10px;
  padding: 6px 10px;
  font-weight: 900;
  cursor: pointer;
}

.modal-close:hover,
.modal-close:focus-visible {
  border-color: var(--text-strong);
  outline: none;
}

.social-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.social-btn {
  width: 100%;
  border-radius: 12px;
  padding: 12px;
  border: 1px solid var(--border-color);
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 900;
  cursor: pointer;
  background: var(--surface);
}

.brand-ico {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
}

.social-btn.kakao {
  background: #fee500;
  color: #1f2937;
  border-color: transparent;
}

.social-btn.kakao .brand-ico {
  background: rgba(15, 23, 42, 0.08);
}

.social-btn.naver {
  background: #2db400;
  color: #fff;
  border-color: transparent;
}

.social-btn.naver .brand-ico {
  background: rgba(255, 255, 255, 0.2);
}

.social-btn.google {
  background: #fff;
  color: var(--text-strong);
}

.social-btn.google .brand-ico {
  background: var(--surface-weak);
}

.seller-card {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.seller-card__top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.seller-avatar {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: var(--surface-weak);
  color: var(--text-strong);
  font-weight: 900;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.seller-name {
  margin: 0;
  color: var(--text-strong);
  font-weight: 900;
  font-size: 16px;
}

.seller-email {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-weight: 700;
  font-size: 13px;
}

.seller-info {
  margin: 0;
  display: grid;
  gap: 10px;
}

.seller-info__row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.seller-info__row dt {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
}

.seller-info__row dd {
  margin: 0;
  color: var(--text-strong);
  font-weight: 800;
}

.seller-logout {
  align-self: flex-start;
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 999px;
  padding: 10px 16px;
  font-weight: 800;
  cursor: pointer;
}

.seller-logout:hover,
.seller-logout:focus-visible {
  border-color: var(--text-strong);
  outline: none;
}

.manager-form {
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
  width: 100%;
}

.field-input::placeholder {
  color: var(--text-muted);
}

.field-input--error {
  border-color: rgba(239, 68, 68, 0.8);
}

.manager-error {
  margin: 0;
  font-size: 0.85rem;
  font-weight: 700;
  color: #ef4444;
  min-height: 18px;
  visibility: hidden;
}

.manager-error.is-visible {
  visibility: visible;
}

.manager-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.confirm-body,
.sent-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 6px;
}

.confirm-title,
.sent-title {
  margin: 0;
  font-weight: 800;
  color: var(--text-strong);
}

.confirm-email,
.sent-email {
  margin: 0;
  font-weight: 900;
  color: var(--text-strong);
  word-break: break-all;
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--surface-weak);
}

.btn {
  border-radius: 999px;
  padding: 10px 16px;
  font-weight: 800;
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  cursor: pointer;
}

.btn.primary {
  background: var(--primary-color);
  color: #fff;
  border-color: transparent;
}

.btn.ghost {
  border-color: var(--border-color);
  color: var(--text-muted);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
