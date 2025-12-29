<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '../../components/PageContainer.vue'
import PageHeader from '../../components/PageHeader.vue'

const router = useRouter()
const DRAFT_KEY = 'deskit_seller_broadcast_draft_v1'

const cueTitle = ref('')
const cueNotes = ref('')
const questions = ref<Array<{ id: string; text: string }>>([])
const maxQuestions = 10

const createQuestion = () => ({
  id: `q-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
  text: '',
})

const loadDraft = () => {
  const raw = localStorage.getItem(DRAFT_KEY)
  if (!raw) return
  try {
    const parsed = JSON.parse(raw)
    cueTitle.value = typeof parsed?.cueTitle === 'string' ? parsed.cueTitle : ''
    cueNotes.value = typeof parsed?.cueNotes === 'string' ? parsed.cueNotes : ''
    if (Array.isArray(parsed?.questions) && parsed.questions.length > 0) {
      questions.value = parsed.questions
        .filter((item: any) => item && typeof item.id === 'string' && typeof item.text === 'string')
        .map((item: any) => ({ id: item.id, text: item.text }))
    }
  } catch {
    return
  }
}

const saveDraft = () => {
  const next = {
    cueTitle: cueTitle.value.trim(),
    cueNotes: cueNotes.value.trim(),
    questions: questions.value.map((item) => ({
      id: item.id,
      text: item.text.trim(),
    })),
  }
  localStorage.setItem(DRAFT_KEY, JSON.stringify(next))
}

const addQuestion = () => {
  if (questions.value.length >= maxQuestions) return
  questions.value.push(createQuestion())
}

const removeQuestion = (id: string) => {
  if (questions.value.length <= 1) return
  questions.value = questions.value.filter((item) => item.id !== id)
}

const goNext = () => {
  saveDraft()
  router.push('/seller/live/create/basic').catch(() => {})
}

onMounted(() => {
  loadDraft()
  if (questions.value.length === 0) {
    questions.value = [createQuestion()]
  }
})
</script>

<template>
  <PageContainer>
    <PageHeader eyebrow="DESKIT" title="방송 등록 - 큐 카드 작성" />
    <section class="create-card ds-surface">
      <label class="field">
        <span class="field__label">큐 카드 제목</span>
        <input v-model="cueTitle" type="text" placeholder="예: 오프닝 멘트" />
      </label>
      <label class="field">
        <span class="field__label">큐 카드 메모</span>
        <textarea v-model="cueNotes" rows="5" placeholder="방송 흐름을 간단히 적어주세요."></textarea>
      </label>
      <div class="section-head">
        <h3>큐 카드 질문</h3>
        <span class="count-pill">{{ questions.length }}/{{ maxQuestions }}</span>
      </div>
      <div class="question-list">
        <div v-for="(item, index) in questions" :key="item.id" class="question-card">
          <div class="question-head">
            <span class="question-title">질문 {{ index + 1 }}</span>
            <button type="button" class="btn ghost" :disabled="questions.length <= 1" @click="removeQuestion(item.id)">
              삭제
            </button>
          </div>
          <textarea v-model="item.text" rows="3" placeholder="질문 내용을 입력하세요."></textarea>
        </div>
      </div>
      <div class="question-actions">
        <button type="button" class="btn" :disabled="questions.length >= maxQuestions" @click="addQuestion">
          + 질문 추가
        </button>
        <span v-if="questions.length >= maxQuestions" class="hint">최대 10개까지 추가할 수 있어요.</span>
      </div>
      <div class="actions">
        <button type="button" class="btn primary" @click="goNext">다음 단계</button>
      </div>
    </section>
  </PageContainer>
</template>

<style scoped>
.create-card {
  padding: 18px;
  display: flex;
  flex-direction: column;
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
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-head h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 900;
  color: var(--text-strong);
}

.count-pill {
  border: 1px solid var(--border-color);
  background: var(--surface-weak);
  color: var(--text-strong);
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 800;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 12px;
  background: var(--surface);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.question-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.question-title {
  font-weight: 800;
  color: var(--text-strong);
}

.question-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hint {
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.85rem;
}

input,
textarea {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 10px 12px;
  font-weight: 700;
  color: var(--text-strong);
  background: var(--surface);
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.btn {
  border-radius: 999px;
  padding: 10px 18px;
  font-weight: 900;
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  cursor: pointer;
}

.btn.ghost {
  border-color: var(--border-color);
  color: var(--text-muted);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn.primary {
  border-color: var(--primary-color);
  color: var(--primary-color);
}
</style>
