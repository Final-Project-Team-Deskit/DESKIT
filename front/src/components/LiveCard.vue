<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { LiveItem } from '../lib/live/types'
import { getLiveStatus, parseLiveDate } from '../lib/live/utils'
import { useNow } from '../lib/live/useNow'

const props = defineProps<{
  item: LiveItem
  isActive?: boolean
}>()

const { now } = useNow(1000)
const elapsed = computed(() => {
  const started = parseLiveDate(props.item.startAt)
  const diffMs = now.value.getTime() - started.getTime()
  const hours = Math.floor(diffMs / (1000 * 60 * 60))
  const minutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60))
  const seconds = Math.floor((diffMs % (1000 * 60)) / 1000)

  const pad = (value: number) => value.toString().padStart(2, '0')

  if (hours > 0) {
    return `${pad(hours)}:${pad(minutes)}`
  }
  return `${pad(minutes)}:${pad(seconds)}`
})
const status = computed(() => getLiveStatus(props.item, now.value))
const buttonLabel = computed(() => {
  if (status.value === 'LIVE') {
    return '입장하기'
  }
  if (status.value === 'ENDED') {
    return 'VOD 다시보기'
  }
  return '예정'
})

const scheduledLabel = computed(() => {
  const start = parseLiveDate(props.item.startAt)
  const dayNames = ['일', '월', '화', '수', '목', '금', '토']
  const month = String(start.getMonth() + 1).padStart(2, '0')
  const date = String(start.getDate()).padStart(2, '0')
  const day = dayNames[start.getDay()]
  const hours = String(start.getHours()).padStart(2, '0')
  const minutes = String(start.getMinutes()).padStart(2, '0')

  return `${month}.${date} (${day}) ${hours}:${minutes} 예정`
})

const router = useRouter()

const handleWatchNow = () => {
  if (status.value === 'LIVE') {
    router.push({ name: 'live-detail', params: { id: props.item.id } })
    return
  }
  if (status.value === 'ENDED') {
    router.push({ name: 'vod', params: { id: props.item.id } })
  }
}
</script>

<template>
  <article class="card" :class="{ 'card--active': props.isActive }">
    <div class="media">
      <img :src="props.item.thumbnailUrl" :alt="props.item.title" />
      <div class="top-badges">
        <span v-if="status === 'LIVE'" class="badge badge-live">LIVE</span>
        <span
          v-if="status === 'LIVE' && props.item.viewerCount != null"
          class="badge badge-viewers"
        >
          시청자 {{ props.item.viewerCount.toLocaleString() }}명
        </span>
      </div>
    </div>
    <div class="content">
      <div class="eyebrow-row">
        <p v-if="status === 'LIVE'" class="eyebrow">현재 방송 중</p>
        <span v-if="status === 'LIVE'" class="eyebrow-time">{{ elapsed }}</span>
        <span v-else-if="status === 'UPCOMING'" class="eyebrow-time">{{ scheduledLabel }}</span>
      </div>
      <h3>{{ props.item.title }}</h3>
      <p class="desc">{{ props.item.description }}</p>
      <div class="meta-row">
        <button
          type="button"
          class="cta"
          :disabled="status === 'UPCOMING'"
          :aria-disabled="status === 'UPCOMING'"
          @click="handleWatchNow"
        >
          {{ buttonLabel }}
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.card {
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  box-shadow: 0 18px 40px rgba(119, 136, 115, 0.12);
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.2s ease;
  display: grid;
  grid-template-columns: minmax(300px, 360px) 1fr;
  column-gap: 2px;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 44px rgba(119, 136, 115, 0.16);
  border-color: var(--accent-color);
}

.card--active {
  border-color: var(--primary-color);
  box-shadow: 0 22px 48px rgba(119, 136, 115, 0.2);
}

.media {
  position: relative;
  background: var(--surface-weak);
}

.media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 600ms cubic-bezier(0.22, 1, 0.36, 1);
}

.media:hover img {
  transform: scale(1.03);
}

.badge {
  position: absolute;
  border-radius: 12px;
  padding: 6px 10px;
  font-weight: 800;
  font-size: 0.85rem;
  color: #fff;
  box-shadow: 0 12px 22px rgba(119, 136, 115, 0.18);
}

.badge-live {
  background: var(--live-color);
}

.top-badges {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.top-badges .badge {
  position: relative;
}

.badge-viewers {
  background: rgba(47, 58, 47, 0.72);
  color: #fff;
  backdrop-filter: blur(6px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  padding: 6px 10px;
  border-radius: 12px;
  font-weight: 800;
  font-size: 0.85rem;
  max-width: calc(100% - 24px);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.eyebrow-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.eyebrow-time {
  color: var(--text-soft);
  font-weight: 800;
  white-space: nowrap;
  font-size: 0.95rem;
}

.content {
  padding: 16px 18px 18px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  word-break: keep-all;
  white-space: normal;
  max-width: 560px;
}

.eyebrow {
  margin: 0;
  color: var(--text-soft);
  font-weight: 800;
  letter-spacing: 0.04em;
}

h3 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: -0.3px;
}

.desc {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.5;
}

.meta-row {
  margin-top: 6px;
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  padding-top: 6px;
}

.cta {
  border: none;
  background: var(--primary-color);
  color: #fff;
  font-weight: 800;
  width: min(320px, 100%);
  height: 52px;
  border-radius: 14px;
  font-size: 1.05rem;
  cursor: pointer;
  box-shadow: 0 14px 30px rgba(119, 136, 115, 0.18);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.cta:hover {
  transform: translateY(-1px);
  box-shadow: 0 16px 32px rgba(119, 136, 115, 0.22);
}

.cta:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

.cta:disabled:hover {
  transform: none;
  box-shadow: none;
}

@media (max-width: 1080px) {
  .card {
    grid-template-columns: 1fr;
  }
}
</style>
