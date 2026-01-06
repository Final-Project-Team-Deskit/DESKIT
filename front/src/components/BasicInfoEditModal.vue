<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

type BroadcastInfo = {
  title: string
  category: string
  notice?: string
  thumbnail?: string
  waitingScreen?: string
}

const props = defineProps<{
  modelValue: boolean
  broadcast: BroadcastInfo | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', value: BroadcastInfo): void
}>()

const title = ref('')
const category = ref('가구')
const notice = ref('판매 상품 외 다른 상품 문의는 받지 않습니다.')
const thumbnailPreview = ref('')
const waitingPreview = ref('')

const isOpen = computed(() => props.modelValue)

const hydrateFromBroadcast = () => {
  if (!props.broadcast) return
  title.value = props.broadcast.title
  category.value = props.broadcast.category
  notice.value = props.broadcast.notice ?? '판매 상품 외 다른 상품 문의는 받지 않습니다.'
  thumbnailPreview.value = props.broadcast.thumbnail ?? ''
  waitingPreview.value = props.broadcast.waitingScreen ?? ''
}

watch(isOpen, (open) => {
  if (open) {
    hydrateFromBroadcast()
  }
})

watch(
  () => props.broadcast,
  (next) => {
    if (props.modelValue && next) {
      hydrateFromBroadcast()
    }
  },
  { deep: true },
)

onMounted(() => {
  if (props.broadcast) hydrateFromBroadcast()
})

const close = () => emit('update:modelValue', false)

const handleFile = (event: Event, target: 'thumbnail' | 'waiting') => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onloadend = () => {
    const result = reader.result as string
    if (target === 'thumbnail') {
      thumbnailPreview.value = result
    } else {
      waitingPreview.value = result
    }
  }
  reader.readAsDataURL(file)
}

const handleSave = () => {
  if (!props.broadcast) return close()
  const payload: BroadcastInfo = {
    title: title.value.trim() || props.broadcast.title,
    category: category.value,
    notice: notice.value,
    thumbnail: thumbnailPreview.value,
    waitingScreen: waitingPreview.value,
  }
  emit('save', payload)
  alert('기본정보가 수정되었습니다.')
  close()
}
</script>

<template>
  <div v-if="modelValue" class="ds-modal" role="dialog" aria-modal="true">
    <div class="ds-modal__backdrop" @click="close"></div>
    <div class="ds-modal__card ds-surface">
      <header class="ds-modal__head">
        <div>
          <p class="ds-modal__eyebrow">방송 관리</p>
          <h3 class="ds-modal__title">기본정보 수정</h3>
        </div>
        <button type="button" class="ds-modal__close" aria-label="닫기" @click="close">×</button>
      </header>

      <div class="ds-modal__body">
        <label class="field">
          <span class="field__label">방송 제목</span>
          <input v-model="title" type="text" maxlength="30" class="field__input" placeholder="방송 제목을 입력하세요" />
          <span class="field__hint">{{ title.length }}/30</span>
        </label>

        <label class="field">
          <span class="field__label">카테고리</span>
          <select v-model="category" class="field__input">
            <option value="가구">가구</option>
            <option value="전자기기">전자기기</option>
            <option value="패션">패션</option>
            <option value="뷰티">뷰티</option>
            <option value="악세사리">악세사리</option>
          </select>
        </label>

        <label class="field">
          <span class="field__label">공지사항</span>
          <textarea
            v-model="notice"
            rows="3"
            maxlength="50"
            class="field__input"
            placeholder="공지사항을 입력하세요"
          ></textarea>
          <span class="field__hint">{{ notice.length }}/50</span>
        </label>

        <div class="upload-grid">
          <label class="field">
            <span class="field__label">썸네일</span>
            <label class="upload-tile">
              <input type="file" accept="image/*" class="upload-input" @change="(event) => handleFile(event, 'thumbnail')" />
              <div class="upload-preview">
                <img v-if="thumbnailPreview" :src="thumbnailPreview" alt="썸네일" />
                <div v-else class="upload-placeholder">
                  <span class="upload-icon">⬆</span>
                  <p class="upload-label">클릭하여 업로드</p>
                </div>
              </div>
            </label>
          </label>

          <label class="field">
            <span class="field__label">대기화면</span>
            <label class="upload-tile">
              <input type="file" accept="image/*" class="upload-input" @change="(event) => handleFile(event, 'waiting')" />
              <div class="upload-preview">
                <img v-if="waitingPreview" :src="waitingPreview" alt="대기화면" />
                <div v-else class="upload-placeholder">
                  <span class="upload-icon">⬆</span>
                  <p class="upload-label">클릭하여 업로드</p>
                </div>
              </div>
            </label>
          </label>
        </div>
      </div>

      <footer class="ds-modal__actions">
        <button type="button" class="ds-btn ghost" @click="close">취소</button>
        <button type="button" class="ds-btn primary" @click="handleSave">저장</button>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.ds-modal {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1300;
}

.ds-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(2px);
}

.ds-modal__card {
  position: relative;
  width: min(720px, 94vw);
  max-height: 92vh;
  padding: 20px;
  border-radius: 16px;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.ds-modal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.ds-modal__eyebrow {
  margin: 0 0 4px;
  color: var(--text-muted);
  font-weight: 800;
  letter-spacing: 0.04em;
}

.ds-modal__title {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 900;
  color: var(--text-strong);
}

.ds-modal__close {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  width: 36px;
  height: 36px;
  border-radius: 10px;
  font-size: 1.1rem;
  font-weight: 900;
  cursor: pointer;
}

.ds-modal__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field__label {
  font-weight: 800;
  color: var(--text-strong);
}

.field__input {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 10px;
  padding: 10px 12px;
  font-weight: 700;
  font-size: 0.95rem;
}

.field__input:focus {
  outline: 2px solid var(--primary-color);
  outline-offset: 1px;
}

.field__hint {
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.8rem;
}

.upload-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.upload-tile {
  display: block;
  border: 2px dashed var(--border-color);
  border-radius: 12px;
  padding: 10px;
  cursor: pointer;
  background: var(--surface-weak);
}

.upload-input {
  display: none;
}

.upload-preview {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  border-radius: 10px;
}

.upload-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: inherit;
}

.upload-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  text-align: center;
  color: var(--text-muted);
  gap: 6px;
}

.upload-icon {
  font-size: 1.6rem;
}

.upload-label {
  margin: 0;
  font-weight: 800;
}

.ds-modal__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.ds-btn {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 10px;
  padding: 10px 16px;
  font-weight: 800;
  cursor: pointer;
}

.ds-btn.primary {
  background: var(--primary-color);
  color: #fff;
  border-color: transparent;
}

.ds-btn.ghost {
  background: transparent;
  color: var(--text-muted);
}
</style>
