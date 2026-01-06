<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = defineProps<{
  modelValue: boolean
  broadcastTitle?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'start'): void
}>()

const selectedCamera = ref('camera-1')
const selectedMic = ref('mic-1')
const volumeLevel = ref(70)

watch(
  () => props.modelValue,
  (value) => {
    if (value) {
      selectedCamera.value = 'camera-1'
      selectedMic.value = 'mic-1'
      volumeLevel.value = 70
    }
  },
)

const close = () => emit('update:modelValue', false)

const handleStart = () => {
  emit('start')
  close()
}

const modalTitle = computed(() => (props.broadcastTitle ? `${props.broadcastTitle} 장치 설정` : '방송 장치 설정'))
</script>

<template>
  <div v-if="modelValue" class="ds-modal" role="dialog" aria-modal="true">
    <div class="ds-modal__backdrop" @click="close"></div>
    <div class="ds-modal__card ds-surface">
      <header class="ds-modal__head">
        <div>
          <p class="ds-modal__eyebrow">방송 준비</p>
          <h3 class="ds-modal__title">{{ modalTitle }}</h3>
        </div>
        <button type="button" class="ds-modal__close" aria-label="닫기" @click="close">×</button>
      </header>

      <div class="ds-modal__body">
        <div class="preview-box">
          <div class="preview-placeholder">
            <span class="preview-icon">📷</span>
            <p class="preview-label">카메라 미리보기</p>
          </div>
        </div>

        <div class="control-grid">
          <label class="field">
            <span class="field__label">카메라 선택</span>
            <select v-model="selectedCamera" class="field__input">
              <option value="camera-1">기본 웹캠</option>
              <option value="camera-2">외부 카메라 1</option>
              <option value="camera-3">외부 카메라 2</option>
            </select>
          </label>

          <label class="field">
            <span class="field__label">마이크 선택</span>
            <select v-model="selectedMic" class="field__input">
              <option value="mic-1">기본 마이크</option>
              <option value="mic-2">외부 마이크 1</option>
              <option value="mic-3">외부 마이크 2</option>
            </select>
          </label>

          <label class="field">
            <span class="field__label">입력 볼륨</span>
            <div class="volume-meter" role="progressbar" aria-valuemin="0" aria-valuemax="100" :aria-valuenow="volumeLevel">
              <div class="volume-meter__fill" :style="{ width: `${volumeLevel}%` }"></div>
            </div>
            <p class="volume-meter__hint">소리가 입력되고 있는지 확인하세요.</p>
          </label>
        </div>
      </div>

      <footer class="ds-modal__actions">
        <button type="button" class="btn ghost" @click="close">취소</button>
        <button type="button" class="btn primary" @click="handleStart">방송 스튜디오 입장</button>
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
  z-index: 1000;
}

.ds-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(2px);
}

.ds-modal__card {
  position: relative;
  width: min(720px, 92vw);
  max-height: 90vh;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  z-index: 1;
}

.ds-modal__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
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
  border-radius: 10px;
  width: 36px;
  height: 36px;
  font-size: 1.2rem;
  font-weight: 900;
  cursor: pointer;
}

.ds-modal__body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.preview-box {
  width: 100%;
  aspect-ratio: 16 / 9;
  border: 1px dashed var(--border-color);
  border-radius: 14px;
  overflow: hidden;
  background: var(--surface-weak);
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-placeholder {
  text-align: center;
  color: var(--text-muted);
}

.preview-icon {
  font-size: 2rem;
  display: block;
}

.preview-label {
  margin: 6px 0 0;
  font-weight: 800;
}

.control-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
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

.field__input {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 10px 12px;
  font-weight: 700;
  color: var(--text-strong);
  background: var(--surface);
}

.volume-meter {
  position: relative;
  height: 12px;
  border-radius: 999px;
  background: var(--surface-weak);
  overflow: hidden;
}

.volume-meter__fill {
  height: 100%;
  background: linear-gradient(90deg, #22c55e, #3b82f6);
  transition: width 0.2s ease;
}

.volume-meter__hint {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.9rem;
}

.ds-modal__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn {
  border-radius: 10px;
  padding: 10px 14px;
  font-weight: 900;
  cursor: pointer;
  border: 1px solid var(--border-color);
  background: #fff;
  color: var(--text-strong);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(var(--primary-rgb), 0.14);
  border-color: var(--primary-color);
}

.btn.primary {
  background: var(--primary-color);
  color: #fff;
  border-color: var(--primary-color);
}

.btn.ghost {
  background: var(--surface);
}

@media (max-width: 640px) {
  .ds-modal__card {
    padding: 16px;
  }
}
</style>
