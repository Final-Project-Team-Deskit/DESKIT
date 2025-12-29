<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watchEffect } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '../components/PageContainer.vue'
import PageHeader from '../components/PageHeader.vue'
import { liveItems } from '../lib/live/data'
import {
  filterLivesByDay,
  getDayWindow,
  getLiveStatus,
  parseLiveDate,
  sortLivesByStartAt,
} from '../lib/live/utils'
import type { LiveItem } from '../lib/live/types'
import { useNow } from '../lib/live/useNow'

const router = useRouter()
const today = new Date()
const { now } = useNow(1000)

const NOTIFY_KEY = 'deskit_live_notifications'
const notifiedIds = ref<Set<string>>(new Set())
const normalizeDay = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate())
const toast = ref<{ message: string; variant: 'success' | 'neutral' } | null>(null)
let toastTimer: ReturnType<typeof setTimeout> | null = null

const dayWindow = computed(() => getDayWindow(today))
const selectedDay = ref(normalizeDay(dayWindow.value[3]))

const formatTime = (value: string) => {
  const time = parseLiveDate(value)
  const hours = time.getHours().toString().padStart(2, '0')
  const minutes = time.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

const getStatus = (item: LiveItem) => getLiveStatus(item, now.value)
const getCountdownLabel = (item: LiveItem) => {
  if (getStatus(item) !== 'UPCOMING') {
    return ''
  }
  const start = parseLiveDate(item.startAt)
  const nowValue = now.value
  const diffMs = start.getTime() - nowValue.getTime()
  if (diffMs <= 0) {
    return '시작 예정'
  }

  const dayMs = 86400000
  const startDay = normalizeDay(start)
  const nowDay = normalizeDay(nowValue)
  const dayDiff = Math.floor((startDay.getTime() - nowDay.getTime()) / dayMs)

  if (dayDiff >= 2) {
    return `${dayDiff}일 후 시작`
  }
  if (dayDiff === 1) {
    return '내일 시작'
  }

  const minutes = Math.floor(diffMs / 60000)
  if (minutes < 1) {
    return '곧 시작'
  }
  if (minutes < 60) {
    return `${minutes}분 후 시작`
  }
  const hours = Math.floor(minutes / 60)
  const remaining = minutes % 60
  return remaining === 0 ? `${hours}시간 후 시작` : `${hours}시간 ${remaining}분 후 시작`
}

const itemsForDay = computed(() => {
  return sortLivesByStartAt(filterLivesByDay(liveItems, selectedDay.value))
})

const liveItemsForDay = computed(() => itemsForDay.value.filter((item) => getStatus(item) === 'LIVE'))
const upcomingItemsForDay = computed(() => itemsForDay.value.filter((item) => getStatus(item) === 'UPCOMING'))
const endedItemsForDay = computed(() =>
  [...itemsForDay.value].filter((item) => getStatus(item) === 'ENDED').reverse(),
)

const orderedItems = computed(() => [
  ...liveItemsForDay.value,
  ...upcomingItemsForDay.value,
  ...endedItemsForDay.value,
])

const statusWeight = (item: LiveItem) => {
  const s = getStatus(item)
  if (s === 'LIVE') return 0
  if (s === 'UPCOMING') return 1
  return 2
}

const groupedByTime = computed(() => {
  const groups = new Map<string, LiveItem[]>()
  orderedItems.value.forEach((item) => {
    const key = formatTime(item.startAt)
    const bucket = groups.get(key) ?? []
    bucket.push(item)
    groups.set(key, bucket)
  })
  return Array.from(groups.entries()).map(([time, items]) => {
    const next = [...items].sort((a, b) => {
      const weight = statusWeight(a) - statusWeight(b)
      if (weight !== 0) return weight
      const ta = parseLiveDate(a.startAt).getTime()
      const tb = parseLiveDate(b.startAt).getTime()
      if (ta !== tb) return ta - tb
      return a.id.localeCompare(b.id)
    })
    return [time, next] as [string, LiveItem[]]
  })
})

const isToday = (day: Date) => {
  return (
    day.getFullYear() === today.getFullYear() &&
    day.getMonth() === today.getMonth() &&
    day.getDate() === today.getDate()
  )
}

const isSelectedDay = (day: Date) => {
  return (
    day.getFullYear() === selectedDay.value.getFullYear() &&
    day.getMonth() === selectedDay.value.getMonth() &&
    day.getDate() === selectedDay.value.getDate()
  )
}

const formatDayLabel = (day: Date) => {
  const dayNames = ['일', '월', '화', '수', '목', '금', '토']
  const label = isToday(day) ? '오늘' : dayNames[day.getDay()]
  const date = `${day.getMonth() + 1}.${day.getDate()}`

  return { label, date }
}

const getDayCount = (day: Date) => filterLivesByDay(liveItems, day).length

const selectDay = (day: Date) => {
  selectedDay.value = normalizeDay(day)
}

const selectToday = () => {
  selectedDay.value = normalizeDay(today)
}

const handleRowClick = (item: LiveItem) => {
  const status = getStatus(item)
  if (status === 'UPCOMING') {
    return
  }
  if (status === 'LIVE') {
    router.push({ name: 'live-detail', params: { id: item.id } })
    return
  }
  if (status === 'ENDED') {
    router.push({ name: 'vod', params: { id: item.id } })
  }
}

const isNotified = (id: string) => notifiedIds.value.has(id)

const handleRowKeydown = (event: KeyboardEvent, item: LiveItem) => {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    handleRowClick(item)
  }
}

const showToast = (message: string, variant: 'success' | 'neutral') => {
  toast.value = { message, variant }
  if (toastTimer) {
    clearTimeout(toastTimer)
  }
  toastTimer = setTimeout(() => {
    toast.value = null
    toastTimer = null
  }, 2200)
}

const toggleNotify = (id: string) => {
  const next = new Set(notifiedIds.value)
  const wasNotified = next.has(id)
  if (wasNotified) {
    next.delete(id)
  } else {
    next.add(id)
  }
  notifiedIds.value = next
  showToast(wasNotified ? '알림 해제됨' : '알림 신청 완료', wasNotified ? 'neutral' : 'success')
}

onMounted(() => {
  try {
    const raw = localStorage.getItem(NOTIFY_KEY)
    if (!raw) {
      return
    }
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) {
      notifiedIds.value = new Set(parsed.filter((id) => typeof id === 'string'))
    }
  } catch {
    notifiedIds.value = new Set()
  }
})

watchEffect(() => {
  localStorage.setItem(NOTIFY_KEY, JSON.stringify(Array.from(notifiedIds.value)))
})

onBeforeUnmount(() => {
  if (toastTimer) {
    clearTimeout(toastTimer)
  }
})
</script>

<template>
  <PageContainer>
    <PageHeader title="라이브 일정" eyebrow="DESKIT LIVE" />

    <div class="date-strip">
      <button
        v-for="day in dayWindow"
        :key="day.toISOString()"
        type="button"
        class="date-pill"
        :class="{ 'date-pill--active': isSelectedDay(day) }"
        @click="selectDay(day)"
      >
        <span class="date-pill__label">{{ formatDayLabel(day).label }}</span>
        <span class="date-pill__date">{{ formatDayLabel(day).date }}</span>
        <span v-if="getDayCount(day) >= 2" class="date-pill__count">{{ getDayCount(day) }}</span>
      </button>
    </div>

    <div v-if="!orderedItems.length" class="empty-state-box">
      <p>선택한 날짜에 라이브가 없습니다.</p>
      <button type="button" class="action-btn action-btn--ghost" @click="selectToday">
        오늘 보기
      </button>
    </div>

    <div v-else class="timeline">
      <div v-for="[time, items] in groupedByTime" :key="time" class="time-group">
        <div class="time-label">{{ time }}</div>
        <div class="time-group__list">
          <article
            v-for="item in items"
            :key="item.id"
            class="live-card-row"
            :class="{
              'row--clickable': getStatus(item) !== 'UPCOMING',
              'row--disabled': getStatus(item) === 'UPCOMING',
            }"
            :aria-disabled="getStatus(item) === 'UPCOMING' ? 'true' : undefined"
            :tabindex="getStatus(item) === 'UPCOMING' ? -1 : 0"
            @click="handleRowClick(item)"
            @keydown="(e) => handleRowKeydown(e, item)"
          >
            <img class="thumb" :src="item.thumbnailUrl" :alt="item.title" />
            <div class="meta">
              <div class="meta__title-row">
                <h4 class="meta__title">{{ item.title }}</h4>
                <span
                  v-if="getStatus(item) === 'LIVE'"
                  class="status-pill status-pill--live"
                >
                  LIVE
                  <span v-if="item.viewerCount" class="status-viewers">
                    {{ item.viewerCount.toLocaleString() }}명
                  </span>
                </span>
                <span v-else-if="getStatus(item) === 'UPCOMING'" class="status-pill">예정</span>
                <span v-else class="status-pill status-pill--ended">종료</span>
                <span v-if="getStatus(item) === 'UPCOMING'" class="status-pill status-pill--sub">
                  {{ getCountdownLabel(item) }}
                </span>
              </div>
              <p v-if="item.sellerName" class="meta__seller">{{ item.sellerName }}</p>
              <p v-if="item.description" class="meta__desc">{{ item.description }}</p>
            </div>
            <div v-if="getStatus(item) === 'UPCOMING'" class="right-slot">
              <button
                type="button"
                :class="[
                  'action-btn',
                  isNotified(item.id) ? 'action-btn--tinted' : 'action-btn--ghost',
                ]"
                @click.stop="toggleNotify(item.id)"
              >
                {{ isNotified(item.id) ? '알림 신청됨' : '알림 신청' }}
              </button>
            </div>
          </article>
        </div>
      </div>
    </div>

    <div v-if="toast" class="toast" :class="`toast--${toast.variant}`" role="status" aria-live="polite">
      {{ toast.message }}
    </div>
  </PageContainer>
</template>

<style scoped>
.date-strip {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding: 6px 2px 18px;
}

.date-strip {
  display: flex;
  gap: 10px;
  width: 100%;

  /* ✅ 가운데 정렬 */
  justify-content: center;

  /* ✅ 줄바꿈 허용해서 "10일"이 한 줄로 안 들어가면 자연스럽게 다음 줄로 */
  flex-wrap: wrap;

  padding: 6px 2px 18px;
  margin: 0 auto 18px;
}

.date-pill {
  border: 1px solid var(--border-color);
  background: var(--surface);
  border-radius: 12px;
  padding: 10px 12px;
  min-width: 76px;

  display: grid;
  gap: 4px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;

  flex: 0 0 auto;
}

@media (max-width: 840px) {
  .date-strip {
    justify-content: flex-start;
    flex-wrap: nowrap;
    overflow-x: auto;
  }
}

.date-pill__label {
  font-weight: 800;
  font-size: 0.9rem;
  color: var(--text-strong);
}

.date-pill__date {
  font-size: 0.85rem;
  color: var(--text-muted);
  min-height: 1.1em;
}

.date-pill__count {
  align-self: center;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--text-soft);
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--surface-weak);
}

.date-pill--active {
  border-color: var(--primary-color);
  box-shadow: 0 10px 22px rgba(119, 136, 115, 0.12);
  transform: translateY(-1px);
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.time-group {
  display: grid;
  grid-template-columns: 96px 1fr;
  gap: 16px;
  align-items: start;
}

.time-label {
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--text-strong);
  padding-top: 8px;
}

.time-group__list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.live-card-row {
  display: grid;
  grid-template-columns: 180px 1fr auto;
  gap: 18px;
  align-items: center;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid var(--border-color);
  background: var(--surface);
}

.row--clickable {
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.row--clickable:hover {
  border-color: var(--primary-color);
  box-shadow: 0 10px 22px rgba(119, 136, 115, 0.12);
  transform: translateY(-1px);
}

.row--disabled {
  cursor: default;
  opacity: 0.66;
}

.row--disabled:hover {
  border-color: var(--border-color);
  box-shadow: none;
  transform: none;
}

.thumb {
  width: 180px;
  height: 140px;
  border-radius: 16px;
  object-fit: cover;
}

.meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.meta__title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.meta__title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 800;
  color: var(--text-strong);
  line-height: 1.35;
}

.meta__seller {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.95rem;
}

.meta__desc {
  margin: 0;
  color: var(--text-soft);
  font-size: 0.85rem;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  white-space: normal;
  line-height: 1.4;
}

.status-pill {
  padding: 2px 10px;
  border-radius: 999px;
  background: var(--surface-weak);
  color: var(--text-muted);
  font-size: 0.8rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.status-pill--live {
  background: var(--live-color-soft);
  color: var(--live-color);
}

.status-pill--ended {
  background: var(--border-color);
  color: var(--text-muted);
}

.status-pill--sub {
  background: transparent;
  color: var(--text-muted);
  border: 1px solid var(--border-color);
}

.status-viewers {
  font-size: 0.75rem;
  font-weight: 700;
}

.right-slot {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  min-width: 132px;
}

.action-btn {
  border: none;
  background: var(--primary-color);
  color: #fff;
  font-weight: 800;
  border-radius: 12px;
  padding: 8px 14px;
  cursor: pointer;
}

.action-btn--ghost {
  background: var(--surface);
  color: var(--text-strong);
  border: 1px solid var(--border-color);
}

.action-btn--tinted {
  background: var(--primary-color);
  color: #fff;
}

.toast {
  position: fixed;
  left: 50%;
  bottom: 24px;
  transform: translateX(-50%);
  padding: 10px 16px;
  border-radius: 999px;
  font-weight: 700;
  font-size: 0.9rem;
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.12);
  z-index: 20;
}

.toast--success {
  background: var(--primary-color);
  color: #fff;
}

.toast--neutral {
  background: var(--surface);
  color: var(--text-strong);
  border: 1px solid var(--border-color);
}

.empty-state-box {
  padding: 18px;
  border: 1px dashed var(--border-color);
  border-radius: 14px;
  background: var(--surface);
  color: var(--text-muted);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

@media (max-width: 960px) {
  .time-group {
    grid-template-columns: 72px 1fr;
  }

  .live-card-row {
    grid-template-columns: 150px 1fr auto;
  }

  .thumb {
    width: 150px;
    height: 120px;
  }
}

@media (max-width: 640px) {
  .time-group {
    grid-template-columns: 1fr;
  }

  .time-label {
    padding-top: 0;
  }

  .live-card-row {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .thumb {
    width: 100%;
    height: 180px;
  }

  .right-slot {
    width: 100%;
  }

  .action-btn {
    width: 100%;
  }
}
</style>
