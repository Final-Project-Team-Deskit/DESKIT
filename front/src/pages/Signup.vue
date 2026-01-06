<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import PageContainer from '../components/PageContainer.vue'
import PageHeader from '../components/PageHeader.vue'
import { setAuthUser } from '../lib/auth'

type PendingSignup = {
  username?: string
  name: string
  email: string
}

type JobOption = {
  value: string
  label: string
}

type MbtiOption = {
  value: string
  label: string
}

const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const pending = ref<PendingSignup | null>(null)
const signupToken = ref('')
const inviteToken = ref('')
const inviteError = ref('')

const form = reactive({
  phoneNumber: '',
  verificationCode: '',
  memberType: 'GENERAL',
  mbti: 'NONE',
  jobCategory: 'NONE',
  businessNumber: '',
  companyName: '',
  description: '',
  planFileBase64: '',
  planFileName: '',
  agreeToTerms: false,
  message: '',
  isVerified: false,
})

const mbtiOptions: MbtiOption[] = [
  { value: 'NONE', label: '선택 안함' },
  { value: 'INTJ', label: 'INTJ' },
  { value: 'INTP', label: 'INTP' },
  { value: 'ENTJ', label: 'ENTJ' },
  { value: 'ENTP', label: 'ENTP' },
  { value: 'INFJ', label: 'INFJ' },
  { value: 'INFP', label: 'INFP' },
  { value: 'ENFJ', label: 'ENFJ' },
  { value: 'ENFP', label: 'ENFP' },
  { value: 'ISTJ', label: 'ISTJ' },
  { value: 'ISFJ', label: 'ISFJ' },
  { value: 'ESTJ', label: 'ESTJ' },
  { value: 'ESFJ', label: 'ESFJ' },
  { value: 'ISTP', label: 'ISTP' },
  { value: 'ISFP', label: 'ISFP' },
  { value: 'ESTP', label: 'ESTP' },
  { value: 'ESFP', label: 'ESFP' },
]

const jobOptions: JobOption[] = [
  { value: 'NONE', label: '선택 안함' },
  { value: 'CREATIVE_TYPE', label: '크리에이티브' },
  { value: 'FLEXIBLE_TYPE', label: '프리랜서/유연근무' },
  { value: 'EDU_RES_TYPE', label: '교육/연구' },
  { value: 'MED_PRO_TYPE', label: '의료/전문직' },
  { value: 'ADMIN_PLAN_TYPE', label: '기획/관리' },
]

const jobLabelMap = jobOptions.reduce<Record<string, string>>((acc, option) => {
  acc[option.value] = option.label
  return acc
}, {})

const isInviteSignup = computed(() => !!inviteToken.value)

const loadPending = async () => {
  if (!signupToken.value) {
    form.message = '로그인이 필요합니다.'
    return
  }

  const response = await fetch(`${apiBase}/api/signup/social/pending`, {
    headers: { Authorization: `Bearer ${signupToken.value}` },
    credentials: 'include',
  })

  if (!response.ok) {
    form.message = '가입 정보를 불러오지 못했습니다.'
    return
  }

  pending.value = (await response.json()) as PendingSignup
}

const sendCode = async () => {
  if (!signupToken.value) {
    form.message = '로그인이 필요합니다.'
    return
  }

  const response = await fetch(`${apiBase}/api/signup/social/phone/send`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${signupToken.value}`,
    },
    credentials: 'include',
    body: JSON.stringify({ phoneNumber: form.phoneNumber }),
  })

  if (!response.ok) {
    form.message = '인증번호 전송에 실패했습니다.'
    return
  }

  const data = await response.json()
  form.message = `인증번호가 발송되었습니다. (개발용 코드: ${data.code})`
}

const verifyCode = async () => {
  if (!signupToken.value) {
    form.message = '로그인이 필요합니다.'
    return
  }

  const response = await fetch(`${apiBase}/api/signup/social/phone/verify`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${signupToken.value}`,
    },
    credentials: 'include',
    body: JSON.stringify({
      phoneNumber: form.phoneNumber,
      code: form.verificationCode,
    }),
  })

  if (!response.ok) {
    form.message = '인증에 실패했습니다.'
    form.isVerified = false
    return
  }

  form.isVerified = true
  form.message = '전화번호 인증이 완료되었습니다.'
}

const storeAuthUser = () => {
  const memberCategory = form.memberType === 'SELLER' ? '판매자' : '일반회원'
  const mbtiValue = form.mbti === 'NONE' ? '' : form.mbti
  const jobValue = form.jobCategory === 'NONE' ? '' : jobLabelMap[form.jobCategory]

  const authUser = {
    name: pending.value?.name ?? '',
    email: pending.value?.email ?? '',
    signupType: '소셜 회원',
    memberCategory,
    mbti: mbtiValue,
    job: jobValue,
  }

  setAuthUser(authUser)
  // localStorage.setItem('deskit-user', JSON.stringify(authUser))
  // localStorage.setItem('deskit-auth', memberCategory)
}

const submitSignup = async () => {
  if (!signupToken.value) {
    form.message = '로그인이 필요합니다.'
    return
  }

  const response = await fetch(`${apiBase}/api/signup/social/complete`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${signupToken.value}`,
    },
    credentials: 'include',
    body: JSON.stringify({
      memberType: form.memberType,
      phoneNumber: form.phoneNumber,
      mbti: form.mbti,
      jobCategory: form.jobCategory,
      businessNumber: form.businessNumber,
      companyName: form.companyName,
      description: form.description,
      planFileBase64: form.planFileBase64,
      inviteToken: inviteToken.value,
      isAgreed: form.agreeToTerms,
    }),
  })

  if (!response.ok) {
    const errorText = await response.text()
    form.message = errorText || '회원가입에 실패했습니다.'
    return
  }

  const successText = await response.text()
  storeAuthUser()

  if (form.memberType === 'SELLER') {
    form.message =
      successText ||
      '판매자 가입이 접수되었습니다. 관리자 승인을 기다려 주세요.'
    return
  }

  form.message = successText || '회원가입이 완료되었습니다.'
  window.location.href = '/my'
}

const handlePlanFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    form.planFileBase64 = ''
    form.planFileName = ''
    return
  }

  form.planFileName = file.name
  const reader = new FileReader()
  reader.onload = () => {
    form.planFileBase64 = typeof reader.result === 'string' ? reader.result : ''
  }
  reader.readAsDataURL(file)
}

const initializeToken = () => {
  const params = new URLSearchParams(window.location.search)
  const token = params.get('token')
  if (token) {
    signupToken.value = token
    sessionStorage.setItem('signupToken', token)
    window.history.replaceState({}, '', '/signup')
    return
  }

  const storedToken = sessionStorage.getItem('signupToken')
  if (storedToken) {
    signupToken.value = storedToken
  }
}

const initializeInviteToken = () => {
  const params = new URLSearchParams(window.location.search)
  const token = params.get('invite')
  if (token) {
    inviteToken.value = token
    sessionStorage.setItem('inviteToken', token)
    window.history.replaceState({}, '', '/signup')
    form.memberType = 'SELLER'
    return
  }

  const storedToken = sessionStorage.getItem('inviteToken')
  if (storedToken) {
    inviteToken.value = storedToken
    form.memberType = 'SELLER'
  }
}

const validateInviteToken = async () => {
  if (!inviteToken.value) {
    return
  }

  const response = await fetch(
    `${apiBase}/api/invitations/validate?token=${encodeURIComponent(inviteToken.value)}`,
    { credentials: 'include' },
  )

  if (!response.ok) {
    const errorText = await response.text()
    inviteError.value = errorText || '초대 토큰이 유효하지 않습니다.'
  }
}

const startLogin = (provider: 'naver' | 'google' | 'kakao') => {
  window.location.href = `${apiBase}/oauth2/authorization/${provider}`
}

onMounted(() => {
  initializeInviteToken()
  validateInviteToken()
  initializeToken()
  loadPending()
})
</script>

<template>
  <PageContainer>
    <PageHeader eyebrow="DESKIT" title="회원가입" />
    <div class="signup-wrap">
      <section class="signup-card">
        <div v-if="inviteError" class="alert alert-error">
          {{ inviteError }}
        </div>

        <div v-if="!signupToken" class="alert">
          <p>회원가입을 진행하려면 소셜 로그인이 필요합니다.</p>
          <div class="social-row">
            <button type="button" class="btn" @click="startLogin('naver')">네이버 로그인</button>
            <button type="button" class="btn" @click="startLogin('google')">구글 로그인</button>
            <button type="button" class="btn" @click="startLogin('kakao')">카카오 로그인</button>
          </div>
        </div>

        <div v-else class="signup-body">
          <div v-if="pending" class="pending">
            <p>이름: <strong>{{ pending.name }}</strong></p>
            <p>이메일: <strong>{{ pending.email }}</strong></p>
          </div>

          <div class="section">
            <h3>전화번호 인증</h3>
            <div class="field-row">
              <input v-model="form.phoneNumber" type="text" placeholder="전화번호" />
              <button type="button" class="btn" @click="sendCode">인증번호 받기</button>
            </div>
            <div class="field-row">
              <input v-model="form.verificationCode" type="text" placeholder="인증번호" />
              <button type="button" class="btn" @click="verifyCode">인증하기</button>
            </div>
            <p v-if="form.isVerified" class="success">인증 완료</p>
          </div>

          <div class="section">
            <h3>회원 유형</h3>
            <select v-model="form.memberType" :disabled="isInviteSignup">
              <option value="GENERAL">일반 회원</option>
              <option value="SELLER">판매자</option>
            </select>
            <p v-if="isInviteSignup" class="hint">초대받은 판매자는 판매자 유형으로만 가입할 수 있습니다.</p>
          </div>

          <div v-if="form.memberType === 'GENERAL'" class="section">
            <h3>추가 정보</h3>
            <label class="field">
              <span>MBTI</span>
              <select v-model="form.mbti">
                <option v-for="option in mbtiOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
            <label class="field">
              <span>직업</span>
              <select v-model="form.jobCategory">
                <option v-for="option in jobOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
          </div>

          <div v-else-if="form.memberType === 'SELLER'" class="section">
            <h3>판매자 정보</h3>
            <label class="field">
              <span>사업자등록번호</span>
              <input v-model="form.businessNumber" type="text" placeholder="사업자등록번호" />
            </label>
            <label class="field">
              <span>사업자명</span>
              <input v-model="form.companyName" type="text" placeholder="사업자명" />
            </label>
            <template v-if="!isInviteSignup">
              <label class="field">
                <span>사업 설명</span>
                <textarea v-model="form.description" placeholder="사업 설명 (선택)"></textarea>
              </label>
              <label class="field">
                <span>사업계획서</span>
                <input type="file" @change="handlePlanFileChange" />
                <p v-if="form.planFileName" class="file-name">선택된 파일: {{ form.planFileName }}</p>
              </label>
            </template>
            <p v-else class="hint">초대받은 판매자는 사업 설명과 사업계획서를 제출하지 않습니다.</p>
          </div>

          <div class="section">
            <label class="checkbox">
              <input type="checkbox" v-model="form.agreeToTerms" />
              약관에 동의합니다.
            </label>
          </div>

          <button type="button" class="btn primary" @click="submitSignup">회원가입 완료</button>
        </div>

        <p v-if="form.message" class="message">{{ form.message }}</p>
      </section>
    </div>
  </PageContainer>
</template>

<style scoped>
.signup-wrap {
  max-width: 680px;
  margin: 0 auto;
}

.signup-card {
  border: 1px solid var(--border-color);
  background: var(--surface);
  border-radius: 16px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.alert {
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: var(--surface-weak);
  padding: 14px;
  font-weight: 700;
  color: var(--text-strong);
}

.alert-error {
  border-color: #e36a6a;
  background: #fff5f5;
  color: #b42318;
}

.social-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.signup-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pending {
  border-radius: 12px;
  padding: 12px 14px;
  background: rgba(34, 197, 94, 0.08);
  border: 1px solid rgba(34, 197, 94, 0.2);
  font-weight: 700;
}

.section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 900;
  color: var(--text-strong);
}

.field-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-weight: 700;
  color: var(--text-muted);
}

.field input,
.field select,
.field textarea,
.field-row input,
.field-row select,
.field-row textarea,
.section select {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 10px 12px;
  font-weight: 600;
  min-width: 220px;
}

.field textarea {
  min-height: 90px;
  resize: vertical;
}

.hint {
  font-size: 0.85rem;
  color: var(--text-muted);
  margin: 0;
}

.success {
  font-weight: 800;
  color: #15803d;
}

.file-name {
  font-size: 0.85rem;
  color: var(--text-muted);
}

.checkbox {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: var(--text-muted);
}

.btn {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  padding: 10px 14px;
  border-radius: 10px;
  font-weight: 800;
  cursor: pointer;
}

.btn.primary {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: #fff;
}

.message {
  margin: 0;
  font-weight: 700;
  color: var(--text-muted);
}

@media (max-width: 600px) {
  .field-row {
    flex-direction: column;
  }

  .field input,
  .field select,
  .field textarea,
  .field-row input,
  .field-row select,
  .field-row textarea,
  .section select {
    width: 100%;
    min-width: 0;
  }
}
</style>
