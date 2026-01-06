<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch, type ComponentPublicInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '../../components/PageHeader.vue'
import {
  ADMIN_RESERVATIONS_EVENT,
  getAdminReservationSummaries,
  type AdminReservationSummary,
} from '../../lib/mocks/adminReservations'
import { ADMIN_LIVES_EVENT, getAdminLiveSummaries, type AdminLiveSummary } from '../../lib/mocks/adminLives'
import { ADMIN_VODS_EVENT, getAdminVodSummaries, type AdminVodSummary } from '../../lib/mocks/adminVods'
import {
  computeLifecycleStatus,
  getScheduledEndMs,
  normalizeBroadcastStatus,
  type BroadcastStatus,
} from '../../lib/broadcastStatus'
import { useInfiniteScroll } from '../../composables/useInfiniteScroll'

type LiveTab = 'all' | 'scheduled' | 'live' | 'vod'
type LoopKind = 'live' | 'scheduled' | 'vod'

type LiveItem = {
  id: string
  title: string
  subtitle?: string
  thumb: string
  datetime?: string
  statusBadge?: string
  viewerBadge?: string
  ctaLabel?: string
  sellerName?: string
  status?: string
  viewers?: number
  likes?: number
  reports?: number
  category?: string
  startedAt?: string
  startedAtMs?: number
  startAtMs?: number
  lifecycleStatus?: BroadcastStatus
}

type ReservationItem = LiveItem & {
  sellerName: string
  status: string
  category: string
  startAtMs?: number
  lifecycleStatus?: BroadcastStatus
}

type AdminVodItem = LiveItem & {
  sellerName: string
  statusLabel: string
  category: string
  metrics: AdminVodSummary['metrics']
  startAtMs?: number
  endAtMs?: number
  visibility?: 'public' | 'private'
  lifecycleStatus?: BroadcastStatus
}

const router = useRouter()
const route = useRoute()

const activeTab = ref<LiveTab>('all')

const LIVE_SECTION_STATUSES: BroadcastStatus[] = ['READY', 'ON_AIR', 'ENDED', 'STOPPED']
const SCHEDULED_SECTION_STATUSES: BroadcastStatus[] = ['RESERVED', 'CANCELED']
const VOD_SECTION_STATUSES: BroadcastStatus[] = ['VOD', 'STOPPED']
const statusPriority: Record<BroadcastStatus, number> = {
  ON_AIR: 0,
  READY: 1,
  STOPPED: 2,
  ENDED: 3,
  RESERVED: 4,
  CANCELED: 5,
  VOD: 6,
}

const liveCategory = ref<string>('all')
const liveSort = ref<'reports_desc' | 'latest' | 'viewers_desc' | 'viewers_asc'>('reports_desc')
const LIVE_PAGE_SIZE = 12
const livePage = ref(1)

const scheduledStatus = ref<'all' | 'reserved' | 'canceled'>('all')
const scheduledCategory = ref<string>('all')
const scheduledSort = ref<'nearest' | 'latest' | 'oldest'>('nearest')
const SCHEDULED_PAGE_SIZE = 12
const scheduledPage = ref(1)

const vodStartDate = ref('')
const vodEndDate = ref('')
const vodVisibility = ref<'all' | 'public' | 'private'>('all')
const vodSort = ref<
  | 'latest'
  | 'oldest'
  | 'reports_desc'
  | 'likes_desc'
  | 'likes_asc'
  | 'revenue_desc'
  | 'revenue_asc'
  | 'viewers_desc'
  | 'viewers_asc'
>('latest')
const vodCategory = ref<string>('all')
const VOD_PAGE_SIZE = 12
const vodPage = ref(1)

const liveItems = ref<AdminLiveSummary[]>([])
const scheduledItems = ref<ReservationItem[]>([])
const vodItems = ref<AdminVodItem[]>([])
const loopGap = 14
const carouselRefs = ref<Record<LoopKind, HTMLElement | null>>({
  live: null,
  scheduled: null,
  vod: null,
})
const slideWidths = ref<Record<LoopKind, number>>({
  live: 0,
  scheduled: 0,
  vod: 0,
})
const loopIndex = ref<Record<LoopKind, number>>({
  live: 1,
  scheduled: 1,
  vod: 1,
})
const loopTransition = ref<Record<LoopKind, boolean>>({
  live: true,
  scheduled: true,
  vod: true,
})
const autoTimers = ref<Record<LoopKind, number | null>>({
  live: null,
  scheduled: null,
  vod: null,
})

const visibleLive = computed(() => activeTab.value === 'all' || activeTab.value === 'live')
const visibleScheduled = computed(() => activeTab.value === 'all' || activeTab.value === 'scheduled')
const visibleVod = computed(() => activeTab.value === 'all' || activeTab.value === 'vod')

const setTab = (tab: LiveTab) => {
  activeTab.value = tab
}

const toDateMs = (raw: string | undefined) => {
  if (!raw) return 0
  const parsed = Date.parse(raw.replace(/\./g, '-').replace(' ', 'T'))
  return Number.isNaN(parsed) ? 0 : parsed
}

const withLifecycleStatus = (item: LiveItem): LiveItem => {
  const startAtMs = item.startAtMs ?? item.startedAtMs ?? toDateMs(item.startedAt ?? item.datetime)
  const endAtMs = getScheduledEndMs(startAtMs, item.endAtMs)
  const lifecycleStatus = computeLifecycleStatus({
    status: item.lifecycleStatus ?? item.status,
    startAtMs,
    endAtMs,
  })
  return {
    ...item,
    startAtMs,
    endAtMs,
    lifecycleStatus,
  }
}

const getLifecycleStatus = (item: LiveItem): BroadcastStatus => normalizeBroadcastStatus(item.lifecycleStatus ?? item.status)

const isPastScheduledEnd = (item: LiveItem): boolean => {
  const endAtMs = getScheduledEndMs(item.startAtMs, item.endAtMs)
  if (!endAtMs) return false
  return Date.now() > endAtMs
}

const syncScheduled = () => {
  const items = getAdminReservationSummaries()
  scheduledItems.value = items
    .map((item: AdminReservationSummary) => ({
      id: item.id,
      title: item.title,
      subtitle: item.subtitle,
      thumb: item.thumb,
      datetime: item.datetime,
      ctaLabel: item.ctaLabel,
      sellerName: item.sellerName,
      status: normalizeBroadcastStatus(item.status),
      category: (item as any).category ?? '기타',
      startAtMs: toDateMs(item.datetime),
      endAtMs: getScheduledEndMs(toDateMs(item.datetime)),
    }))
}

const syncLives = () => {
  const items = getAdminLiveSummaries()
  liveItems.value = items.map((item) => {
    const startAtMs = toDateMs(item.startedAt)
    const status = normalizeBroadcastStatus(item.status)
    return {
      ...item,
      status,
      startAtMs,
      startedAtMs: startAtMs,
      endAtMs: getScheduledEndMs(startAtMs),
    }
  })
}

const syncVods = () => {
  const items = getAdminVodSummaries()
  vodItems.value = items.map((item: AdminVodSummary) => {
    const startAtMs = toDateMs(item.startedAt)
    const endAtMs = toDateMs(item.endedAt)
    const visibility = item.statusLabel === '비공개' ? 'private' : 'public'
    return {
      ...item,
      subtitle: '',
      datetime: `업로드: ${item.startedAt}`,
      ctaLabel: '상세보기',
      status: 'VOD',
      startAtMs,
      endAtMs,
      visibility,
      lifecycleStatus: 'VOD',
    }
  })
}

const liveItemsWithStatus = computed(() => liveItems.value.map(withLifecycleStatus))
const scheduledItemsWithStatus = computed(() => scheduledItems.value.map(withLifecycleStatus))
const vodItemsWithStatus = computed(() => vodItems.value.map(withLifecycleStatus))

const liveDisplayItems = computed(() => {
  const byId = new Map<string, LiveItem>()
  ;[...liveItemsWithStatus.value, ...scheduledItemsWithStatus.value].forEach((item) => {
    const status = getLifecycleStatus(item)
    if (!LIVE_SECTION_STATUSES.includes(status)) return
    if (status === 'STOPPED' && isPastScheduledEnd(item)) return
    byId.set(item.id, { ...item, lifecycleStatus: status })
  })
  return Array.from(byId.values())
})

const stoppedVodItems = computed<AdminVodItem[]>(() => {
  const sources = [...liveItemsWithStatus.value, ...scheduledItemsWithStatus.value]
  return sources
    .filter((item) => getLifecycleStatus(item) === 'STOPPED' && isPastScheduledEnd(item))
    .map((item) => ({
      ...item,
      statusLabel: 'STOPPED',
      lifecycleStatus: 'STOPPED',
      visibility: 'public',
      datetime: item.datetime ?? (item.startedAt ? `업로드: ${item.startedAt}` : ''),
      metrics: (item as any).metrics ?? {
        likes: item.likes ?? 0,
        reports: item.reports ?? 0,
        totalRevenue: 0,
        maxViewers: item.viewers ?? 0,
        watchTime: 0,
      },
      startAtMs: item.startAtMs,
      endAtMs: item.endAtMs,
    }))
})

const vodDisplayItems = computed(() => [...vodItemsWithStatus.value, ...stoppedVodItems.value])

const filteredLive = computed(() => {
  let filtered = [...liveDisplayItems.value]
  if (liveCategory.value !== 'all') {
    filtered = filtered.filter((item) => item.category === liveCategory.value)
  }
  filtered.sort((a, b) => {
    const aStatus = getLifecycleStatus(a)
    const bStatus = getLifecycleStatus(b)
    if (statusPriority[aStatus] !== statusPriority[bStatus]) {
      return statusPriority[aStatus] - statusPriority[bStatus]
    }
    if (liveSort.value === 'reports_desc') return (b.reports ?? 0) - (a.reports ?? 0)
    if (liveSort.value === 'latest') return toDateMs(b.startedAt) - toDateMs(a.startedAt)
    if (liveSort.value === 'viewers_desc') return (b.viewers ?? 0) - (a.viewers ?? 0)
    if (liveSort.value === 'viewers_asc') return (a.viewers ?? 0) - (b.viewers ?? 0)
    return 0
  })
  return filtered
})

const filteredScheduled = computed(() => {
  const base = scheduledItemsWithStatus.value.filter((item) =>
    SCHEDULED_SECTION_STATUSES.includes(getLifecycleStatus(item)),
  )
  const matchesCategory = scheduledCategory.value === 'all' ? base : base.filter((item) => item.category === scheduledCategory.value)
  const reserved = matchesCategory.filter((item) => getLifecycleStatus(item) === 'RESERVED')
  const canceled = matchesCategory.filter((item) => getLifecycleStatus(item) === 'CANCELED')

  const sortScheduled = (items: ReservationItem[]) =>
    items.slice().sort((a, b) => {
      const aDate = a.startAtMs ?? toDateMs(a.datetime)
      const bDate = b.startAtMs ?? toDateMs(b.datetime)
      if (scheduledSort.value === 'latest') return bDate - aDate
      if (scheduledSort.value === 'oldest') return aDate - bDate
      return aDate - bDate
    })

  if (scheduledStatus.value === 'reserved') return sortScheduled(reserved)
  if (scheduledStatus.value === 'canceled') return sortScheduled(canceled)
  return [...sortScheduled(reserved), ...sortScheduled(canceled)]
})

const filteredVods = computed(() => {
  const startMs = vodStartDate.value ? Date.parse(`${vodStartDate.value}T00:00:00`) : null
  const endMs = vodEndDate.value ? Date.parse(`${vodEndDate.value}T23:59:59`) : null

  let filtered = [...vodDisplayItems.value].filter((item) => {
    const dateMs = item.startAtMs ?? toDateMs(item.startedAt)
    if (startMs && dateMs < startMs) return false
    if (endMs && dateMs > endMs) return false
    if (vodVisibility.value !== 'all' && vodVisibility.value !== item.visibility) return false
    if (vodCategory.value !== 'all' && item.category !== vodCategory.value) return false
    return true
  })

  const sortVod = (items: AdminVodItem[]) =>
    items.slice().sort((a, b) => {
      if (vodSort.value === 'latest') return (b.startAtMs ?? 0) - (a.startAtMs ?? 0)
      if (vodSort.value === 'oldest') return (a.startAtMs ?? 0) - (b.startAtMs ?? 0)
      if (vodSort.value === 'reports_desc') return (b.metrics.reports ?? 0) - (a.metrics.reports ?? 0)
      if (vodSort.value === 'likes_desc') return (b.metrics.likes ?? 0) - (a.metrics.likes ?? 0)
      if (vodSort.value === 'likes_asc') return (a.metrics.likes ?? 0) - (b.metrics.likes ?? 0)
      if (vodSort.value === 'revenue_desc') return (b.metrics.totalRevenue ?? 0) - (a.metrics.totalRevenue ?? 0)
      if (vodSort.value === 'revenue_asc') return (a.metrics.totalRevenue ?? 0) - (b.metrics.totalRevenue ?? 0)
      if (vodSort.value === 'viewers_desc') return (b.metrics.maxViewers ?? 0) - (a.metrics.maxViewers ?? 0)
      if (vodSort.value === 'viewers_asc') return (a.metrics.maxViewers ?? 0) - (b.metrics.maxViewers ?? 0)
      return 0
    })

  const vodOnly = filtered.filter((item) => getLifecycleStatus(item) === 'VOD')
  const stoppedOnly = filtered.filter((item) => getLifecycleStatus(item) === 'STOPPED')

  filtered = [...sortVod(vodOnly), ...sortVod(stoppedOnly)]

  return filtered
})

const visibleLiveGridItems = computed(() => filteredLive.value.slice(0, LIVE_PAGE_SIZE * livePage.value))
const visibleScheduledItems = computed(() => filteredScheduled.value.slice(0, SCHEDULED_PAGE_SIZE * scheduledPage.value))
const visibleVodItems = computed(() => filteredVods.value.slice(0, VOD_PAGE_SIZE * vodPage.value))

const { sentinelRef: liveSentinelRef } = useInfiniteScroll({
  canLoadMore: () => filteredLive.value.length > visibleLiveGridItems.value.length,
  loadMore: () => {
    livePage.value += 1
  },
  enabled: () => activeTab.value === 'live',
})

const { sentinelRef: scheduledSentinelRef } = useInfiniteScroll({
  canLoadMore: () => filteredScheduled.value.length > visibleScheduledItems.value.length,
  loadMore: () => {
    scheduledPage.value += 1
  },
  enabled: () => activeTab.value === 'scheduled',
})

const { sentinelRef: vodSentinelRef } = useInfiniteScroll({
  canLoadMore: () => filteredVods.value.length > visibleVodItems.value.length,
  loadMore: () => {
    vodPage.value += 1
  },
  enabled: () => activeTab.value === 'vod',
})

const liveCategories = computed(() => Array.from(new Set(liveDisplayItems.value.map((item) => item.category ?? '기타'))))
const scheduledCategories = computed(() => Array.from(new Set(scheduledItemsWithStatus.value.map((item) => item.category ?? '기타'))))
const vodCategories = computed(() => Array.from(new Set(vodDisplayItems.value.map((item) => item.category ?? '기타'))))

const liveSummary = computed<LiveItem[]>(() =>
  liveDisplayItems.value
    .filter((item) => LIVE_SECTION_STATUSES.includes(getLifecycleStatus(item)))
    .slice()
    .sort((a, b) => (b.viewers ?? 0) - (a.viewers ?? 0))
    .slice(0, 5),
)
const scheduledSummary = computed<ReservationItem[]>(() =>
  scheduledItemsWithStatus.value
    .filter((item) => getLifecycleStatus(item) === 'RESERVED')
    .slice()
    .sort((a, b) => (a.startAtMs ?? toDateMs(a.datetime)) - (b.startAtMs ?? toDateMs(b.datetime)))
    .slice(0, 5),
)
const vodSummary = computed<AdminVodItem[]>(() =>
  vodItemsWithStatus.value
    .filter((item) => getLifecycleStatus(item) === 'VOD')
    .slice()
    .sort((a, b) => (b.startAtMs ?? 0) - (a.startAtMs ?? 0))
    .slice(0, 5),
)

const buildLoopItems = <T>(items: T[]): T[] => {
  if (!items.length) return []
  if (items.length === 1) {
    const single = items[0]!
    return [single, single, single]
  }
  const first = items[0]!
  const last = items[items.length - 1]!
  return [last, ...items, first]
}

const liveLoopItems = computed<AdminLiveSummary[]>(() => buildLoopItems(liveSummary.value))
const scheduledLoopItems = computed<ReservationItem[]>(() => buildLoopItems(scheduledSummary.value))
const vodLoopItems = computed<AdminVodItem[]>(() => buildLoopItems(vodSummary.value))

const openReservationDetail = (id: string) => {
  if (!id) return
  router.push(`/admin/live/reservations/${id}`).catch(() => {})
}

const openLiveDetail = (id: string) => {
  if (!id) return
  router.push(`/admin/live/now/${id}`).catch(() => {})
}

const openVodDetail = (id: string) => {
  if (!id) return
  router.push(`/admin/live/vods/${id}`).catch(() => {})
}

const formatDDay = (item: { startAtMs?: number }) => {
  if (!item.startAtMs) return ''
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const start = new Date(item.startAtMs)
  start.setHours(0, 0, 0, 0)
  const diffDays = Math.round((start.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
  if (diffDays === 0) return 'D-Day'
  if (diffDays > 0) return `D-${diffDays}`
  return `D+${Math.abs(diffDays)}`
}

const refreshTabFromQuery = () => {
  const tab = route.query.tab
  if (tab === 'scheduled' || tab === 'live' || tab === 'vod' || tab === 'all') {
    activeTab.value = tab
  }
}

const setCarouselRef = (kind: LoopKind) => (el: Element | ComponentPublicInstance | null) => {
  const target =
    el && typeof el === 'object' && '$el' in el ? ((el as ComponentPublicInstance).$el as HTMLElement | null) : ((el as HTMLElement) || null)
  carouselRefs.value[kind] = target
  nextTick(() => updateSlideWidth(kind))
}

const updateSlideWidth = (kind: LoopKind) => {
  const root = carouselRefs.value[kind]
  if (!root) return
  const card = root.querySelector<HTMLElement>('.live-card')
  slideWidths.value[kind] = card?.offsetWidth ?? 280
}

const getTrackStyle = (kind: LoopKind) => {
  const width = (slideWidths.value[kind] || 280) + loopGap
  const translate = loopIndex.value[kind] * width
  return {
    transform: `translateX(-${translate}px)`,
    transition: loopTransition.value[kind] ? 'transform 0.6s ease' : 'none',
  }
}

const loopItemsFor = (kind: LoopKind) => {
  if (kind === 'live') return liveLoopItems.value
  if (kind === 'scheduled') return scheduledLoopItems.value
  return vodLoopItems.value
}

const baseItemsFor = (kind: LoopKind) => {
  if (kind === 'live') return liveSummary.value
  if (kind === 'scheduled') return scheduledSummary.value
  return vodSummary.value
}

const handleLoopTransitionEnd = (kind: LoopKind) => {
  const items = loopItemsFor(kind)
  if (!items.length) return
  const lastIndex = items.length - 1
  if (loopIndex.value[kind] === lastIndex) {
    loopTransition.value[kind] = false
    loopIndex.value[kind] = 1
    requestAnimationFrame(() => {
      loopTransition.value[kind] = true
    })
  } else if (loopIndex.value[kind] === 0) {
    loopTransition.value[kind] = false
    loopIndex.value[kind] = lastIndex - 1
    requestAnimationFrame(() => {
      loopTransition.value[kind] = true
    })
  }
}

const stepCarousel = (kind: LoopKind, delta: -1 | 1) => {
  const items = loopItemsFor(kind)
  if (items.length <= 1) return
  const lastIndex = items.length - 1
  loopTransition.value[kind] = true
  const nextIndex = loopIndex.value[kind] + delta
  if (nextIndex > lastIndex) {
    loopIndex.value[kind] = lastIndex
  } else if (nextIndex < 0) {
    loopIndex.value[kind] = 0
  } else {
    loopIndex.value[kind] = nextIndex
  }
  restartAutoLoop(kind)
}

const startAutoLoop = (kind: LoopKind) => {
  stopAutoLoop(kind)
  if (baseItemsFor(kind).length <= 1) return
  autoTimers.value[kind] = window.setInterval(() => {
    stepCarousel(kind, 1)
  }, 3200)
}

const stopAutoLoop = (kind: LoopKind) => {
  const timer = autoTimers.value[kind]
  if (timer) window.clearInterval(timer)
  autoTimers.value[kind] = null
}

const restartAutoLoop = (kind: LoopKind) => {
  stopAutoLoop(kind)
  startAutoLoop(kind)
}

const resetLoop = (kind: LoopKind) => {
  loopIndex.value[kind] = loopItemsFor(kind).length > 1 ? 1 : 0
  loopTransition.value[kind] = true
  nextTick(() => {
    updateSlideWidth(kind)
    startAutoLoop(kind)
  })
}

const resetAllLoops = () => {
  resetLoop('live')
  resetLoop('scheduled')
  resetLoop('vod')
}

const handleResize = () => {
  updateSlideWidth('live')
  updateSlideWidth('scheduled')
  updateSlideWidth('vod')
}

watch(
  () => route.query.tab,
  () => refreshTabFromQuery(),
)

watch([liveCategory, liveSort], () => {
  livePage.value = 1
  resetLoop('live')
})

watch([scheduledStatus, scheduledCategory, scheduledSort], () => {
  scheduledPage.value = 1
})

watch([vodStartDate, vodEndDate, vodVisibility, vodCategory, vodSort], () => {
  vodPage.value = 1
})

watch(
  () => liveSummary.value,
  () => resetLoop('live'),
  { deep: true },
)

watch(
  () => scheduledSummary.value,
  () => resetLoop('scheduled'),
  { deep: true },
)

watch(
  () => vodSummary.value,
  () => resetLoop('vod'),
  { deep: true },
)

onMounted(() => {
  refreshTabFromQuery()
  syncLives()
  syncScheduled()
  syncVods()
  nextTick(() => {
    resetAllLoops()
    handleResize()
  })
  window.addEventListener(ADMIN_LIVES_EVENT, syncLives)
  window.addEventListener(ADMIN_RESERVATIONS_EVENT, syncScheduled)
  window.addEventListener(ADMIN_VODS_EVENT, syncVods)
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener(ADMIN_LIVES_EVENT, syncLives)
  window.removeEventListener(ADMIN_RESERVATIONS_EVENT, syncScheduled)
  window.removeEventListener(ADMIN_VODS_EVENT, syncVods)
  window.removeEventListener('resize', handleResize)
  stopAutoLoop('live')
  stopAutoLoop('scheduled')
  stopAutoLoop('vod')
})
</script>

<template>
  <div>
    <PageHeader eyebrow="DESKIT" title="방송관리" />

    <header class="live-header">
      <div class="live-header__spacer" aria-hidden="true"></div>

      <div class="live-tabs" role="tablist" aria-label="방송 상태">
        <button
          type="button"
          class="live-tab"
          :class="{ 'live-tab--active': activeTab === 'all' }"
          @click="setTab('all')"
        >
          전체
        </button>
        <button
          type="button"
          class="live-tab"
          :class="{ 'live-tab--active': activeTab === 'scheduled' }"
          @click="setTab('scheduled')"
        >
          예약
        </button>
        <button
          type="button"
          class="live-tab"
          :class="{ 'live-tab--active': activeTab === 'live' }"
          @click="setTab('live')"
        >
          방송 중
        </button>
        <button
          type="button"
          class="live-tab"
          :class="{ 'live-tab--active': activeTab === 'vod' }"
          @click="setTab('vod')"
        >
          VOD
        </button>
      </div>

      <div class="live-header__right"></div>
    </header>

    <section v-if="visibleLive" class="live-section">
      <div class="live-section__head">
        <div class="live-section__title">
          <h3>방송 중</h3>
        </div>
        <div class="live-section__controls">
          <p v-if="activeTab === 'live'" class="ds-section-sub">현재 진행 중인 라이브 방송입니다.</p>
          <button v-else class="link-more" type="button" @click="setTab('live')">+ 더보기</button>
        </div>
      </div>

      <div v-if="activeTab === 'live'" class="control-stack">
        <div class="filter-row">
          <label class="inline-filter">
            <span>카테고리</span>
            <select v-model="liveCategory">
              <option value="all">모든 카테고리</option>
              <option v-for="category in liveCategories" :key="category" :value="category">{{ category }}</option>
            </select>
          </label>
          <label class="inline-filter">
            <span>정렬</span>
            <select v-model="liveSort">
              <option value="reports_desc">신고가 많은 순</option>
              <option value="latest">최신순</option>
              <option value="viewers_desc">시청자가 많은 순</option>
              <option value="viewers_asc">시청자가 적은 순</option>
            </select>
          </label>
        </div>
      </div>

      <div
        v-if="activeTab === 'live'"
        class="live-grid"
        :class="{ 'live-grid--empty': !visibleLiveGridItems.length }"
        aria-label="방송 중 목록"
      >
        <template v-if="visibleLiveGridItems.length">
          <article
            v-for="item in visibleLiveGridItems"
            :key="item.id"
            class="live-card ds-surface live-card--clickable"
            @click="openLiveDetail(item.id)"
          >
            <div class="live-thumb">
              <img class="live-thumb__img" :src="item.thumb" :alt="item.title" loading="lazy" />
              <div class="live-badges">
                <span class="badge badge--live">{{ getLifecycleStatus(item) }}</span>
                <span class="badge badge--viewer">시청자 {{ item.viewers }}명</span>
              </div>
            </div>
            <div class="live-body">
              <div class="live-meta">
                <p class="live-title">{{ item.title }}</p>
                <p class="live-date">시작: {{ item.startedAt }}</p>
                <p class="live-seller">{{ item.sellerName }}</p>
                <p class="live-viewers">신고 {{ item.reports ?? 0 }}건 · 좋아요 {{ item.likes }}</p>
              </div>
            </div>
          </article>

          <div
            v-if="filteredLive.length > visibleLiveGridItems.length"
            ref="liveSentinelRef"
            class="scroll-sentinel"
            aria-hidden="true"
          ></div>
        </template>

        <p v-else class="empty-section">진행 중인 방송이 없습니다. 라이브 시작 시 목록이 표시됩니다.</p>
      </div>

      <div v-else class="carousel-wrap">
        <button
          type="button"
          class="carousel-btn carousel-btn--left"
          aria-label="방송 중 왼쪽 이동"
          @click="stepCarousel('live', -1)"
        >
          ‹
        </button>

        <div class="live-carousel live-carousel--loop">
          <div
            class="live-carousel__track"
            :class="{ 'live-carousel__track--empty': !liveLoopItems.length }"
            :style="getTrackStyle('live')"
            :ref="setCarouselRef('live')"
            aria-label="방송 중 목록"
            @transitionend="handleLoopTransitionEnd('live')"
          >
            <template v-if="liveLoopItems.length">
              <article
                v-for="(item, idx) in liveLoopItems"
                :key="`${item.id}-${idx}`"
                class="live-card ds-surface live-card--clickable"
                @click="openLiveDetail(item.id)"
              >
                <div class="live-thumb">
                  <img class="live-thumb__img" :src="item.thumb" :alt="item.title" loading="lazy" />
                  <div class="live-badges">
                    <span class="badge badge--live">{{ getLifecycleStatus(item) }}</span>
                    <span class="badge badge--viewer">시청자 {{ item.viewers }}명</span>
                  </div>
                </div>
                <div class="live-body">
                  <div class="live-meta">
                    <p class="live-title">{{ item.title }}</p>
                    <p class="live-date">시작: {{ item.startedAt }}</p>
                    <p class="live-seller">{{ item.sellerName }}</p>
                    <p class="live-viewers">신고 {{ item.reports ?? 0 }}건 · 좋아요 {{ item.likes }}</p>
                  </div>
                </div>
              </article>
            </template>

            <p v-else class="empty-section live-carousel__empty">
              진행 중인 방송이 없습니다. 라이브 시작 시 목록이 표시됩니다.
            </p>
          </div>
        </div>

        <button
          type="button"
          class="carousel-btn carousel-btn--right"
          aria-label="방송 중 오른쪽 이동"
          @click="stepCarousel('live', 1)"
        >
          ›
        </button>
      </div>
    </section>

    <section v-if="visibleScheduled" class="live-section">
      <div class="live-section__head">
        <div class="live-section__title">
          <h3>예약된 방송</h3>
        </div>
        <div class="live-section__controls">
          <p v-if="activeTab === 'scheduled'" class="ds-section-sub">예정된 라이브 스케줄을 관리하세요.</p>
          <button
            v-else
            class="link-more"
            type="button"
            @click="setTab('scheduled')"
          >
            + 더보기
          </button>
        </div>
      </div>

      <div v-if="activeTab === 'scheduled'" class="control-stack">
        <div class="filter-row">
          <label class="inline-filter">
            <span>상태</span>
            <select v-model="scheduledStatus">
              <option value="all">전체</option>
              <option value="reserved">예약중</option>
              <option value="canceled">취소됨</option>
            </select>
          </label>
          <label class="inline-filter">
            <span>카테고리</span>
            <select v-model="scheduledCategory">
              <option value="all">모든 카테고리</option>
              <option v-for="category in scheduledCategories" :key="category" :value="category">{{ category }}</option>
            </select>
          </label>
          <label class="inline-filter">
            <span>정렬</span>
            <select v-model="scheduledSort">
              <option value="nearest">방송 시간이 가까운 순</option>
              <option value="latest">최신순</option>
              <option value="oldest">오래된 순</option>
            </select>
          </label>
        </div>
      </div>

      <div
        v-if="activeTab === 'scheduled'"
        class="scheduled-grid"
        :class="{ 'scheduled-grid--empty': !visibleScheduledItems.length }"
        aria-label="예약 방송 목록"
      >
        <template v-if="visibleScheduledItems.length">
          <article
            v-for="item in visibleScheduledItems"
            :key="item.id"
            class="live-card ds-surface live-card--clickable"
            @click="openReservationDetail(item.id)"
          >
              <div class="live-thumb">
                <img class="live-thumb__img" :src="item.thumb" :alt="item.title" loading="lazy" />
                <div class="live-badges">
                  <span
                    class="badge badge--scheduled"
                    :class="{ 'badge--cancelled': getLifecycleStatus(item) === 'CANCELED' }"
                  >
                    {{ getLifecycleStatus(item) }}
                  </span>
                  <span class="badge badge--viewer">{{ formatDDay(item) }}</span>
                </div>
              </div>
            <div class="live-body">
              <div class="live-meta">
                <p class="live-title">{{ item.title }}</p>
                <p class="live-date">{{ item.datetime }}</p>
                <p class="live-seller">{{ item.sellerName }}</p>
                <p class="live-viewers">{{ item.category }}</p>
              </div>
            </div>
          </article>

          <div
            v-if="filteredScheduled.length > visibleScheduledItems.length"
            ref="scheduledSentinelRef"
            class="scroll-sentinel"
            aria-hidden="true"
          ></div>
        </template>

        <p v-else class="empty-section">예약된 방송이 없습니다.</p>
      </div>

      <div v-else class="carousel-wrap">
        <button
          type="button"
          class="carousel-btn carousel-btn--left"
          aria-label="예약 방송 왼쪽 이동"
          @click="stepCarousel('scheduled', -1)"
        >
          ‹
        </button>

        <div class="live-carousel live-carousel--loop">
          <div
            class="live-carousel__track"
            :class="{ 'live-carousel__track--empty': !scheduledLoopItems.length }"
            :style="getTrackStyle('scheduled')"
            :ref="setCarouselRef('scheduled')"
            aria-label="예약 방송 목록"
            @transitionend="handleLoopTransitionEnd('scheduled')"
          >
            <template v-if="scheduledLoopItems.length">
              <article
                v-for="(item, idx) in scheduledLoopItems"
                :key="`${item.id}-${idx}`"
                class="live-card ds-surface live-card--clickable"
                @click="openReservationDetail(item.id)"
              >
                <div class="live-thumb">
                  <img class="live-thumb__img" :src="item.thumb" :alt="item.title" loading="lazy" />
                  <div class="live-badges">
                    <span
                      class="badge badge--scheduled"
                      :class="{ 'badge--cancelled': getLifecycleStatus(item) === 'CANCELED' }"
                    >
                      {{ getLifecycleStatus(item) }}
                    </span>
                    <span class="badge badge--viewer">{{ formatDDay(item) }}</span>
                  </div>
                </div>
                <div class="live-body">
                  <div class="live-meta">
                    <p class="live-title">{{ item.title }}</p>
                    <p class="live-date">{{ item.datetime }}</p>
                    <p class="live-seller">{{ item.sellerName }}</p>
                    <p class="live-viewers">{{ item.category }}</p>
                  </div>
                </div>
              </article>
            </template>

            <p v-else class="empty-section live-carousel__empty">
              예약된 방송이 없습니다. 예약 데이터를 불러오면 자동으로 표시됩니다.
            </p>
          </div>
        </div>

        <button
          type="button"
          class="carousel-btn carousel-btn--right"
          aria-label="예약 방송 오른쪽 이동"
          @click="stepCarousel('scheduled', 1)"
        >
          ›
        </button>
      </div>
    </section>

    <section v-if="visibleVod" class="live-section">
      <div class="live-section__head">
        <div class="live-section__title">
          <h3>VOD</h3>
        </div>
        <div class="live-section__controls">
          <p v-if="activeTab === 'vod'" class="ds-section-sub">저장된 다시보기 콘텐츠를 확인합니다.</p>
          <button
            v-else
            class="link-more"
            type="button"
            @click="setTab('vod')"
          >
            + 더보기
          </button>
        </div>
      </div>

      <div v-if="activeTab === 'vod'" class="control-stack">
        <div class="filter-row vod-filter-row">
          <label class="inline-filter">
            <span>날짜 시작</span>
            <input v-model="vodStartDate" type="date" />
          </label>
          <label class="inline-filter">
            <span>날짜 종료</span>
            <input v-model="vodEndDate" type="date" />
          </label>
          <label class="inline-filter">
            <span>공개 여부</span>
            <select v-model="vodVisibility">
              <option value="all">전체</option>
              <option value="public">공개</option>
              <option value="private">비공개</option>
            </select>
          </label>
          <label class="inline-filter">
            <span>카테고리</span>
            <select v-model="vodCategory">
              <option value="all">모든 카테고리</option>
              <option v-for="category in vodCategories" :key="category" :value="category">{{ category }}</option>
            </select>
          </label>
          <label class="inline-filter">
            <span>정렬</span>
            <select v-model="vodSort">
              <option value="latest">최신순</option>
              <option value="reports_desc">신고 건수가 많은 순</option>
              <option value="oldest">오래된 순</option>
              <option value="likes_desc">좋아요가 높은 순</option>
              <option value="likes_asc">좋아요가 낮은 순</option>
              <option value="revenue_desc">매출액이 높은 순</option>
              <option value="revenue_asc">매출액이 낮은 순</option>
              <option value="viewers_desc">총 시청자 수가 높은 순</option>
              <option value="viewers_asc">총 시청자 수가 낮은 순</option>
            </select>
          </label>
        </div>
      </div>

      <div
        v-if="activeTab === 'vod'"
        class="vod-grid"
        :class="{ 'vod-grid--empty': !visibleVodItems.length }"
        aria-label="VOD 목록"
      >
        <template v-if="visibleVodItems.length">
          <article
            v-for="item in visibleVodItems"
            :key="item.id"
            class="live-card ds-surface live-card--clickable"
            @click="openVodDetail(item.id)"
          >
            <div class="live-thumb">
              <img class="live-thumb__img" :src="item.thumb" :alt="item.title" loading="lazy" />
              <div class="live-badges">
                <span class="badge badge--vod">{{ item.statusLabel }}</span>
                <span class="badge badge--viewer">신고 {{ item.metrics.reports }}</span>
              </div>
            </div>
            <div class="live-body">
              <div class="live-meta">
                <p class="live-title">{{ item.title }}</p>
                <p class="live-date">{{ item.datetime }}</p>
                <p class="live-seller">{{ item.sellerName }}</p>
                <p class="live-viewers">좋아요 {{ item.metrics.likes }} · 시청 {{ item.metrics.maxViewers }}</p>
              </div>
            </div>
          </article>

          <div
            v-if="filteredVods.length > visibleVodItems.length"
            ref="vodSentinelRef"
            class="scroll-sentinel"
            aria-hidden="true"
          ></div>
        </template>

        <p v-else class="empty-section">등록된 VOD가 없습니다.</p>
      </div>

      <div v-else class="carousel-wrap">
        <button
          type="button"
          class="carousel-btn carousel-btn--left"
          aria-label="VOD 왼쪽 이동"
          @click="stepCarousel('vod', -1)"
        >
          ‹
        </button>

        <div class="live-carousel live-carousel--loop">
          <div
            class="live-carousel__track"
            :class="{ 'live-carousel__track--empty': !vodLoopItems.length }"
            :style="getTrackStyle('vod')"
            :ref="setCarouselRef('vod')"
            aria-label="VOD 목록"
            @transitionend="handleLoopTransitionEnd('vod')"
          >
            <template v-if="vodLoopItems.length">
              <article
                v-for="(item, idx) in vodLoopItems"
                :key="`${item.id}-${idx}`"
                class="live-card ds-surface live-card--clickable"
                @click="openVodDetail(item.id)"
              >
                <div class="live-thumb">
                  <img class="live-thumb__img" :src="item.thumb" :alt="item.title" loading="lazy" />
                  <div class="live-badges">
                    <span class="badge badge--vod">{{ item.statusLabel }}</span>
                    <span class="badge badge--viewer">신고 {{ item.metrics.reports }}</span>
                  </div>
                </div>
                <div class="live-body">
                  <div class="live-meta">
                    <p class="live-title">{{ item.title }}</p>
                    <p class="live-date">{{ item.datetime }}</p>
                    <p class="live-seller">{{ item.sellerName }}</p>
                    <p class="live-viewers">좋아요 {{ item.metrics.likes }} · 시청 {{ item.metrics.maxViewers }}</p>
                  </div>
                </div>
              </article>
            </template>

            <p v-else class="empty-section live-carousel__empty">
              등록된 VOD가 없습니다. 영상이 업로드되면 자동으로 표시됩니다.
            </p>
          </div>
        </div>

        <button
          type="button"
          class="carousel-btn carousel-btn--right"
          aria-label="VOD 오른쪽 이동"
          @click="stepCarousel('vod', 1)"
        >
          ›
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.live-header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.live-header__spacer {
  min-height: 1px;
}

.live-header__right {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.inline-filter {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  min-width: 140px;
  font-weight: 800;
  color: var(--text-strong);
}

.inline-filter select,
.inline-filter input {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 8px 10px;
  font-weight: 700;
  color: var(--text-strong);
  background: var(--surface);
}

.filter-row {
  display: flex;
  width: 100%;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-start;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--surface);
}

.live-tabs {
  display: inline-flex;
  gap: 10px;
  justify-content: center;
}

.live-tab {
  border: 1px solid var(--border-color);
  background: #fff;
  color: var(--text-strong);
  border-radius: 999px;
  padding: 10px 18px;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.live-tab:hover {
  border-color: var(--primary-color);
  box-shadow: 0 8px 18px rgba(var(--primary-rgb), 0.12);
  transform: translateY(-1px);
}

.live-tab--active {
  background: var(--surface-weak);
  border-color: var(--primary-color);
}

.live-section {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
}

.live-section:first-of-type {
  margin-top: 0;
  padding-top: 0;
  border-top: none;
}

.live-section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.live-section__head h3 {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 900;
  color: var(--text-strong);
}

.live-section__controls {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.link-more {
  border: none;
  background: transparent;
  color: var(--primary-color);
  font-weight: 900;
  cursor: pointer;
  padding: 4px 6px;
}

.control-stack {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
  width: 100%;
  margin-bottom: 12px;
}

.vod-filter-row {
  align-items: flex-end;
}

.live-grid,
.scheduled-grid,
.vod-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.scheduled-grid--empty,
.vod-grid--empty,
.live-grid--empty {
  display: flex;
  justify-content: center;
  align-items: center;
}

.empty-section {
  text-align: center;
  color: var(--text-muted);
  font-weight: 800;
  padding: 18px 12px;
}

.live-carousel__empty {
  width: 100%;
}

.carousel-wrap {
  position: relative;
  padding: 0 8px;
}

.carousel-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--border-color);
  background: #fff;
  font-weight: 900;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.12);
}

.carousel-btn--left {
  left: -6px;
}

.carousel-btn--right {
  right: -6px;
}

.live-carousel {
  overflow: hidden;
  padding: 10px 6px;
}

.live-carousel__track {
  display: flex;
  gap: 14px;
  will-change: transform;
}

.live-carousel__track--empty {
  justify-content: center;
  width: 100%;
}

.live-carousel__track .live-card {
  flex: 0 0 clamp(260px, 26vw, 320px);
  min-width: 260px;
}

.live-card {
  scroll-snap-align: start;
  border-radius: 14px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 260px;
}

.live-card--clickable {
  cursor: pointer;
}

.live-thumb {
  position: relative;
  height: 170px;
  background: var(--surface-weak);
}

.live-thumb__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.live-badges {
  position: absolute;
  top: 10px;
  left: 10px;
  display: inline-flex;
  gap: 8px;
  align-items: center;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  padding: 8px 10px;
  font-weight: 900;
  line-height: 1;
  font-size: 0.9rem;
}

.badge--viewer {
  background: rgba(15, 23, 42, 0.85);
  color: #fff;
}

.badge--live {
  background: #ef4444;
  color: #fff;
}

.badge--scheduled {
  background: rgba(15, 23, 42, 0.8);
  color: #fff;
}

.badge--cancelled {
  background: var(--surface-weak);
  color: var(--text-muted);
  border: 1px solid var(--border-color);
}

.badge--vod {
  background: rgba(15, 23, 42, 0.8);
  color: #fff;
}

.live-body {
  padding: 14px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  flex: 1;
}

.live-meta {
  min-width: 0;
}

.live-title {
  margin: 0 0 8px;
  font-weight: 900;
  color: var(--text-strong);
  font-size: 1.02rem;
}

.live-date {
  margin: 0 0 6px;
  color: var(--text-soft);
  font-weight: 700;
  font-size: 0.95rem;
}

.live-seller {
  margin: 0;
  color: var(--text-muted);
  font-weight: 800;
  font-size: 0.9rem;
}

.live-viewers {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.85rem;
}

.live-card--empty {
  justify-content: center;
  padding: 18px;
}

.live-card__title {
  margin: 0;
  font-weight: 900;
  color: var(--text-strong);
}

.live-card__meta {
  margin: 8px 0 0;
  color: var(--text-muted);
  font-weight: 700;
}

.live-more {
  grid-column: 1 / -1;
  padding: 12px;
  border-radius: 12px;
  border: 1px dashed var(--border-color);
  background: var(--surface);
  font-weight: 900;
  cursor: pointer;
}

.scroll-sentinel {
  width: 100%;
  height: 1px;
  grid-column: 1 / -1;
}

@media (max-width: 1200px) {
  .live-grid,
  .scheduled-grid,
  .vod-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .live-header {
    grid-template-columns: 1fr;
    justify-items: start;
  }

  .live-grid,
  .scheduled-grid,
  .vod-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .carousel-wrap {
    padding: 0;
  }

  .live-grid,
  .scheduled-grid,
  .vod-grid {
    grid-template-columns: 1fr;
  }

  .live-section__controls {
    align-items: flex-start;
  }
}
</style>
