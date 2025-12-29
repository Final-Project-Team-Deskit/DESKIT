<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import PageHeader from '../../components/PageHeader.vue'

type UserStatus = '활성화' | '비활성화'
type UserType = '일반회원' | '판매자'

type AdminUser = {
  id: string
  email: string
  name: string
  type: UserType
  status: UserStatus
  phone?: string
  joinedAt: string
  provider: 'Kakao' | 'Naver' | 'Google' | 'Email'
  marketingAgreed: boolean
}

const keyword = ref('')
const typeFilter = ref<'전체' | UserType>('전체')
const statusFilter = ref<'전체' | UserStatus>('전체')
const fromDate = ref('')
const toDate = ref('')
const trigger = ref(0)
const showUserModal = ref(false)
const selectedUser = ref<AdminUser | null>(null)

const users = ref<AdminUser[]>([
  {
    id: 'u-1001',
    email: 'jiyoon@example.com',
    name: '김지윤',
    type: '일반회원',
    status: '활성화',
    phone: '010-3212-9981',
    joinedAt: '2025-02-04',
    provider: 'Kakao',
    marketingAgreed: true,
  },
  {
    id: 'u-1002',
    email: 'seojun@example.com',
    name: '박서준',
    type: '판매자',
    status: '활성화',
    phone: '010-5421-1221',
    joinedAt: '2025-01-19',
    provider: 'Naver',
    marketingAgreed: false,
  },
  {
    id: 'u-1003',
    email: 'mira@example.com',
    name: '김미라',
    type: '일반회원',
    status: '비활성화',
    phone: '010-5533-4488',
    joinedAt: '2024-12-02',
    provider: 'Email',
    marketingAgreed: true,
  },
  {
    id: 'u-1004',
    email: 'minsu@example.com',
    name: '정민수',
    type: '판매자',
    status: '활성화',
    phone: '010-7744-1133',
    joinedAt: '2025-02-18',
    provider: 'Google',
    marketingAgreed: true,
  },
  {
    id: 'u-1005',
    email: 'haeun@example.com',
    name: '이하은',
    type: '일반회원',
    status: '활성화',
    phone: '010-8841-3372',
    joinedAt: '2025-02-10',
    provider: 'Kakao',
    marketingAgreed: false,
  },
  {
    id: 'u-1006',
    email: 'yujin@example.com',
    name: '최유진',
    type: '판매자',
    status: '비활성화',
    phone: '010-3301-8831',
    joinedAt: '2024-11-27',
    provider: 'Naver',
    marketingAgreed: false,
  },
  {
    id: 'u-1007',
    email: 'seona@example.com',
    name: '김서나',
    type: '일반회원',
    status: '활성화',
    phone: '010-9012-7733',
    joinedAt: '2025-01-05',
    provider: 'Email',
    marketingAgreed: true,
  },
  {
    id: 'u-1008',
    email: 'donghyun@example.com',
    name: '이동현',
    type: '판매자',
    status: '활성화',
    phone: '010-6622-1994',
    joinedAt: '2024-10-15',
    provider: 'Google',
    marketingAgreed: true,
  },
  {
    id: 'u-1009',
    email: 'nana@example.com',
    name: '홍나나',
    type: '일반회원',
    status: '비활성화',
    phone: '010-1122-3388',
    joinedAt: '2024-09-09',
    provider: 'Kakao',
    marketingAgreed: false,
  },
  {
    id: 'u-1010',
    email: 'owner@example.com',
    name: '오너 관리자',
    type: '판매자',
    status: '활성화',
    phone: '010-4022-6677',
    joinedAt: '2025-02-21',
    provider: 'Email',
    marketingAgreed: true,
  },
])

const keywordLower = computed(() => keyword.value.trim().toLowerCase())

const filteredUsers = computed(() => {
  trigger.value
  const startMs = fromDate.value ? Date.parse(`${fromDate.value}T00:00:00`) : null
  const endMs = toDate.value ? Date.parse(`${toDate.value}T23:59:59`) : null

  return users.value.filter((user) => {
    if (typeFilter.value !== '전체' && user.type !== typeFilter.value) return false
    if (statusFilter.value !== '전체' && user.status !== statusFilter.value) return false
    if (startMs || endMs) {
      const joinedMs = Date.parse(`${user.joinedAt}T00:00:00`)
      if (startMs && joinedMs < startMs) return false
      if (endMs && joinedMs > endMs) return false
    }
    if (!keywordLower.value) return true
    const haystack = `${user.email} ${user.name} ${user.phone ?? ''}`.toLowerCase()
    return haystack.includes(keywordLower.value)
  })
})

const handleSearch = () => {
  trigger.value += 1
}

const openUserModal = (user: AdminUser) => {
  selectedUser.value = user
  showUserModal.value = true
}

const closeUserModal = () => {
  showUserModal.value = false
  selectedUser.value = null
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && showUserModal.value) {
    closeUserModal()
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
  <section class="admin-users">
    <PageHeader eyebrow="DESKIT" title="회원관리" />
    <h2 class="admin-title">회원 관리</h2>

    <section class="filter-card ds-surface">
      <div class="filter-grid">
        <label class="field">
          <span class="field__label">통합검색</span>
          <input
            v-model="keyword"
            type="text"
            class="field-input"
            placeholder="ID/이메일, 이름 또는 전화번호 입력.."
          />
        </label>
        <label class="field">
          <span class="field__label">회원 유형</span>
          <select v-model="typeFilter" class="field-input">
            <option>전체</option>
            <option>일반회원</option>
            <option>판매자</option>
          </select>
        </label>
        <label class="field">
          <span class="field__label">회원 상태</span>
          <select v-model="statusFilter" class="field-input">
            <option>전체</option>
            <option>활성화</option>
            <option>비활성화</option>
          </select>
        </label>
        <div class="field date-range">
          <span class="field__label">가입일</span>
          <div class="date-inputs">
            <input v-model="fromDate" type="date" class="field-input" />
            <span class="date-sep">~</span>
            <input v-model="toDate" type="date" class="field-input" />
          </div>
        </div>
      </div>
      <button type="button" class="search-btn" @click="handleSearch">검색</button>
    </section>

    <section class="table-card ds-surface">
      <div class="table-wrap">
        <table class="admin-table">
          <thead>
            <tr>
              <th>순번</th>
              <th>ID/이메일</th>
              <th>이름</th>
              <th>유형</th>
              <th>상태</th>
              <th>가입일</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(user, idx) in filteredUsers"
              :key="user.id"
              class="table-row"
              role="button"
              tabindex="0"
              @click="openUserModal(user)"
              @keydown.enter.prevent="openUserModal(user)"
              @keydown.space.prevent="openUserModal(user)"
            >
              <td>{{ idx + 1 }}</td>
              <td class="table-email">{{ user.email }}</td>
              <td>{{ user.name }}</td>
              <td>{{ user.type }}</td>
              <td>
                <span class="status-pill" :class="user.status === '활성화' ? 'is-active' : 'is-disabled'">
                  {{ user.status }}
                </span>
              </td>
              <td>{{ user.joinedAt }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div v-if="showUserModal && selectedUser" class="user-modal" role="dialog" aria-modal="true" aria-label="회원 상세 조회">
      <div class="user-modal__backdrop" @click="closeUserModal"></div>
      <div class="user-modal__card ds-surface">
        <div class="user-modal__head">
          <div>
            <h3>회원 상세 조회</h3>
            <p>회원의 상세 정보를 확인합니다.</p>
          </div>
          <button type="button" class="modal-close" @click="closeUserModal" aria-label="닫기">✕</button>
        </div>
        <dl class="detail-grid">
          <div class="detail-item">
            <dt>회원 ID/이메일</dt>
            <dd>{{ selectedUser.email }}</dd>
          </div>
          <div class="detail-item">
            <dt>이름</dt>
            <dd>{{ selectedUser.name }}</dd>
          </div>
          <div class="detail-item">
            <dt>서비스 제공자</dt>
            <dd>{{ selectedUser.provider }}</dd>
          </div>
          <div class="detail-item">
            <dt>유형</dt>
            <dd>{{ selectedUser.type }}</dd>
          </div>
          <div class="detail-item">
            <dt>상태</dt>
            <dd>{{ selectedUser.status }}</dd>
          </div>
          <div class="detail-item">
            <dt>전화번호</dt>
            <dd>{{ selectedUser.phone ?? '-' }}</dd>
          </div>
          <div class="detail-item">
            <dt>가입일</dt>
            <dd>{{ selectedUser.joinedAt }}</dd>
          </div>
          <div class="detail-item">
            <dt>마케팅 및 알림 제공 약관 동의</dt>
            <dd>{{ selectedUser.marketingAgreed ? '동의' : '미동의' }}</dd>
          </div>
        </dl>
        <div class="modal-actions">
          <button type="button" class="btn primary" @click="closeUserModal">확인</button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.admin-users {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.admin-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 900;
  color: var(--text-strong);
}

.filter-card {
  padding: 18px;
  border-radius: 14px;
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field__label {
  font-weight: 800;
  color: var(--text-strong);
  font-size: 0.9rem;
}

.field-input {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 10px 12px;
  font-weight: 700;
  color: var(--text-strong);
  background: var(--surface);
}

.date-range {
  grid-column: span 3;
}

.date-inputs {
  display: flex;
  align-items: center;
  gap: 10px;
}

.date-sep {
  color: var(--text-muted);
  font-weight: 700;
}

.search-btn {
  border: 1px solid var(--primary-color);
  background: var(--primary-color);
  color: #fff;
  border-radius: 12px;
  padding: 12px 14px;
  font-weight: 900;
  cursor: pointer;
  width: 100%;
}

.table-card {
  padding: 0;
  border-radius: 14px;
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.table-wrap {
  overflow-x: auto;
}

.admin-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 720px;
}

.admin-table th,
.admin-table td {
  padding: 14px 12px;
  text-align: left;
  border-bottom: 1px solid var(--border-color);
  font-weight: 700;
  color: var(--text-strong);
  font-size: 0.9rem;
}

.admin-table thead th {
  background: var(--surface-weak);
  font-weight: 900;
}

.table-row {
  cursor: pointer;
}

.table-row:focus-visible {
  outline: 2px solid rgba(var(--primary-rgb), 0.4);
  outline-offset: -2px;
}

.table-email {
  color: var(--text-muted);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-weight: 800;
  font-size: 0.85rem;
}

.status-pill.is-active {
  background: #111827;
  color: #fff;
}

.status-pill.is-disabled {
  background: var(--surface-weak);
  color: var(--text-muted);
  border: 1px solid var(--border-color);
}

.user-modal {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: grid;
  place-items: center;
  padding: 24px;
}

.user-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
}

.user-modal__card {
  position: relative;
  z-index: 1;
  width: min(640px, 100%);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.12);
}

.user-modal__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.user-modal__head h3 {
  margin: 0 0 6px;
  font-size: 1.1rem;
  font-weight: 900;
  color: var(--text-strong);
}

.user-modal__head p {
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

.detail-grid {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-item dt {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.85rem;
}

.detail-item dd {
  margin: 0;
  color: var(--text-strong);
  font-weight: 800;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
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

@media (max-width: 960px) {
  .filter-grid {
    grid-template-columns: 1fr 1fr;
  }

  .date-range {
    grid-column: span 2;
  }
}

@media (max-width: 720px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .date-range {
    grid-column: span 1;
  }

  .date-inputs {
    flex-direction: column;
    align-items: stretch;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
