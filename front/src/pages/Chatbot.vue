<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import PageContainer from '../components/PageContainer.vue'
import PageHeader from '../components/PageHeader.vue'
import { getAuthUser } from '../lib/auth'

type ChatRole = 'user' | 'bot' | 'system'

type ChatMessage = {
  id: string
  role: ChatRole
  content: string
  sources?: string[]
}

type ChatHistoryItem = {
  messageId?: number
  type?: string
  content?: string
}

type ChatResponse = {
  answer?: string
  sources?: string[]
  escalated?: boolean
}

const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const isSending = ref(false)
const isLocked = ref(false)
const statusLabel = ref<string>('BOT_ACTIVE')
const chatListRef = ref<HTMLDivElement | null>(null)

const memberId = computed(() => {
  const user = getAuthUser()
  const rawId = user?.id ?? user?.userId ?? user?.user_id ?? user?.sellerId ?? user?.seller_id
  return rawId ? String(rawId) : '1'
})

const chatId = computed(() => {
  const numeric = Number.parseInt(memberId.value, 10)
  return Number.isFinite(numeric) ? numeric : 1
})

const scrollToBottom = async () => {
  await nextTick()
  if (chatListRef.value) {
    chatListRef.value.scrollTop = chatListRef.value.scrollHeight
  }
}

const appendMessage = (role: ChatRole, content: string, sources?: string[]) => {
  messages.value.push({
    id: `${Date.now()}-${messages.value.length}`,
    role,
    content,
    sources,
  })
  scrollToBottom()
}

const loadChatHistory = async () => {
  try {
    const response = await fetch(`${apiBase}/chat/history/${chatId.value}`, {
      method: 'POST',
    })
    if (!response.ok) return
    const history = (await response.json()) as ChatHistoryItem[]
    messages.value = history.map((item, index) => ({
      id: `${item.messageId ?? index}`,
      role: item.type === 'USER' ? 'user' : 'bot',
      content: item.content ?? '',
    }))
    scrollToBottom()
  } catch (error) {
    console.error('chat history load failed', error)
  }
}

const checkConversationStatus = async () => {
  try {
    const response = await fetch(`${apiBase}/chat/status/${memberId.value}`)
    if (!response.ok) return
    const data = (await response.json()) as { status?: string }
    statusLabel.value = data.status ?? 'BOT_ACTIVE'
    if (statusLabel.value !== 'BOT_ACTIVE') {
      isLocked.value = true
      appendMessage('system', '관리자가 문의사항을 확인하는 중입니다.')
    }
  } catch (error) {
    console.error('status check failed', error)
  }
}

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || isSending.value || isLocked.value) return
  appendMessage('user', text)
  inputText.value = ''
  isSending.value = true
  try {
    const response = await fetch(`${apiBase}/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text }),
    })
    if (!response.ok) {
      appendMessage('system', '메시지 전송에 실패했어요. 다시 시도해 주세요.')
      return
    }
    const data = (await response.json()) as ChatResponse
    appendMessage('bot', data.answer ?? 'No response received.', data.sources)
    if (data.escalated) {
      isLocked.value = true
      appendMessage('system', '고객님의 문의가 관리자에게 이관되었어요.')
    }
  } catch (error) {
    console.error('chat send failed', error)
    appendMessage('system', '네트워크 오류. 다시 시도해 주세요.')
  } finally {
    isSending.value = false
  }
}

onMounted(async () => {
  await loadChatHistory()
  await checkConversationStatus()
})
</script>

<template>
  <PageContainer>
    <PageHeader
      eyebrow="고객 지원"
      title="DESKIT AI"
      subtitle="궁금한 점이 있으시다면 DESKIT AI가 도와드릴게요."
    />

    <section class="chat-shell">
      <header class="chat-head">
        <div class="chat-meta">
          <span class="badge">Status</span>
          <span class="status" :class="{ 'status--locked': isLocked }">
            {{ statusLabel }}
          </span>
        </div>
        <p class="hint">
          관리자에게 대화가 이관될 경우, 입력창이 잠시 비활성화돼요.
        </p>
      </header>

      <div class="chat-body">
        <div ref="chatListRef" class="chat-list">
          <div v-if="messages.length === 0" class="chat-empty">
            DESKIT AI와 대화를 시작해보세요.
          </div>
          <div
            v-for="message in messages"
            :key="message.id"
            class="chat-message"
            :class="`chat-message--${message.role}`"
          >
            <p class="chat-text">{{ message.content }}</p>
            <div v-if="message.sources?.length" class="chat-sources">
              <span class="chat-source" v-for="(source, index) in message.sources" :key="`${message.id}-${index}`">
                {{ source }}
              </span>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <input
            v-model="inputText"
            type="text"
            placeholder="무엇이든 물어보세요"
            :disabled="isSending || isLocked"
            @keydown.enter.prevent="sendMessage"
          />
          <button type="button" class="btn primary" :disabled="isSending || isLocked" @click="sendMessage">
            {{ isSending ? '전송중...' : '전송' }}
          </button>
        </div>
      </div>
    </section>
  </PageContainer>
</template>

<style scoped>
.chat-shell {
  border: 1px solid var(--border-color);
  background: var(--surface);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
}

.chat-head {
  padding: 18px 20px;
  background: linear-gradient(135deg, rgba(209, 232, 208, 0.5), rgba(238, 244, 238, 0.8));
  border-bottom: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chat-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.badge {
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--text-soft);
}

.status {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(16, 163, 127, 0.12);
  color: #0f5132;
  font-weight: 800;
  font-size: 12px;
}

.status--locked {
  background: rgba(239, 68, 68, 0.1);
  color: #b91c1c;
}

.hint {
  margin: 0;
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 600;
}

.chat-body {
  display: flex;
  flex-direction: column;
  min-height: 520px;
}

.chat-list {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  background: radial-gradient(circle at top, rgba(233, 240, 231, 0.5), transparent 65%);
}

.chat-empty {
  color: var(--text-muted);
  font-weight: 700;
  text-align: center;
  margin-top: 80px;
}

.chat-message {
  max-width: 70%;
  padding: 12px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.5;
  background: #fff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}

.chat-message--user {
  align-self: flex-end;
  background: rgba(16, 163, 127, 0.12);
  color: #0f5132;
}

.chat-message--bot {
  align-self: flex-start;
  background: #fff;
  color: #111827;
}

.chat-message--system {
  align-self: center;
  max-width: 80%;
  background: rgba(59, 130, 246, 0.1);
  color: #1d4ed8;
  text-align: center;
}

.chat-text {
  margin: 0;
  font-weight: 600;
}

.chat-sources {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chat-source {
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
}

.chat-input {
  padding: 16px 20px;
  border-top: 1px solid var(--border-color);
  display: flex;
  gap: 10px;
  align-items: center;
  background: var(--surface);
}

.chat-input input {
  flex: 1;
  min-height: 44px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  padding: 10px 12px;
  font-weight: 600;
  color: var(--text-strong);
}

.chat-input input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 8px 20px rgba(119, 136, 115, 0.18);
}

.chat-input input:disabled {
  background: var(--surface-weak);
}

.btn {
  border: none;
  border-radius: 12px;
  padding: 10px 18px;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.btn.primary {
  background: #111827;
  color: #fff;
  box-shadow: 0 10px 24px rgba(17, 24, 39, 0.18);
}

.btn.primary:disabled {
  background: rgba(17, 24, 39, 0.4);
  cursor: not-allowed;
  box-shadow: none;
}

.btn.primary:not(:disabled):hover {
  transform: translateY(-1px);
}

@media (max-width: 720px) {
  .chat-body {
    min-height: 420px;
  }

  .chat-message {
    max-width: 85%;
  }

  .chat-input {
    flex-direction: column;
    align-items: stretch;
  }

  .btn.primary {
    width: 100%;
  }
}
</style>
