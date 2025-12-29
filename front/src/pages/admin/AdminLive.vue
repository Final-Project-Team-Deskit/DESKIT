<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '../../components/PageHeader.vue'
import {
  ADMIN_RESERVATIONS_EVENT,
  getAdminReservationSummaries,
  type AdminReservationSummary,
} from '../../lib/mocks/adminReservations'
import { ADMIN_LIVES_EVENT, getAdminLiveSummaries, type AdminLiveSummary } from '../../lib/mocks/adminLives'
import { ADMIN_VODS_EVENT, getAdminVodSummaries, type AdminVodSummary } from '../../lib/mocks/adminVods'

type LiveTab = 'all' | 'scheduled' | 'live' | 'vod'

type LiveItem = {
  id: string
  title: string
  subtitle: string
  thumb: string
  datetime: string
  statusBadge?: string
  viewerBadge?: string
  ctaLabel: string
}

type ReservationItem = LiveItem & {
  sellerName: string
  status: string
}

type VodItem = LiveItem

type AdminVodItem = LiveItem & {
  sellerName: string
  statusLabel: string
}

type AdminLiveItem = LiveItem & {
  sellerName: string
  status: string
  viewers: number
}

const router = useRouter()
const route = useRoute()
const activeTab = ref<LiveTab>('all')

const liveItems = ref<AdminLiveItem[]>([])

const scheduledItems = ref<ReservationItem[]>([])

const vodItems = ref<AdminVodItem[]>([])

const visibleLive = computed(() => activeTab.value === 'all' || activeTab.value === 'live')
const visibleScheduled = computed(() => activeTab.value === 'all' || activeTab.value === 'scheduled')
const visibleVod = computed(() => activeTab.value === 'all' || activeTab.value === 'vod')

const setTab = (tab: LiveTab) => {
  activeTab.value = tab
}

const syncScheduled = () => {
  const items = getAdminReservationSummaries()
  scheduledItems.value = items.map((item: AdminReservationSummary) => ({
    id: item.id,
    title: item.title,
    subtitle: item.subtitle,
    thumb: item.thumb,
    datetime: item.datetime,
    ctaLabel: item.ctaLabel,
    sellerName: item.sellerName,
    status: item.status,
  }))
}

const syncLives = () => {
  const items = getAdminLiveSummaries().filter((item) => item.status === '방송중')
  liveItems.value = items.map((item: AdminLiveSummary) => ({
    id: item.id,
    title: item.title,
    subtitle: item.subtitle,
    thumb: item.thumb,
    datetime: `시작: ${item.startedAt}`,
    ctaLabel: '상세보기',
    sellerName: item.sellerName,
    status: item.status,
    viewers: item.viewers,
  }))
}

const syncVods = () => {
  const items = getAdminVodSummaries()
  vodItems.value = items.map((item: AdminVodSummary) => ({
    id: item.id,
    title: item.title,
    subtitle: '',
    thumb: item.thumb,
    datetime: `업로드: ${item.startedAt}`,
    ctaLabel: '상세보기',
    sellerName: item.sellerName,
    statusLabel: item.statusLabel,
  }))
}

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

const refreshTabFromQuery = () => {
  const tab = route.query.tab
  if (tab === 'scheduled' || tab === 'live' || tab === 'vod' || tab === 'all') {
    activeTab.value = tab
  }
}

watch(
  () => route.query.tab,
  () => refreshTabFromQuery(),
)

onMounted(() => {
  refreshTabFromQuery()
  syncLives()
  syncScheduled()
  syncVods()
  window.addEventListener(ADMIN_LIVES_EVENT, syncLives)
  window.addEventListener(ADMIN_RESERVATIONS_EVENT, syncScheduled)
  window.addEventListener(ADMIN_VODS_EVENT, syncVods)
})

onBeforeUnmount(() => {
  window.removeEventListener(ADMIN_LIVES_EVENT, syncLives)
  window.removeEventListener(ADMIN_RESERVATIONS_EVENT, syncScheduled)
  window.removeEventListener(ADMIN_VODS_EVENT, syncVods)
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
        <h3>방송 중</h3>
        <p class="ds-section-sub">현재 진행 중인 방송 목록입니다.</p>
      </div>

      <div v-if="activeTab === 'live'" class="live-grid" aria-label="방송 중 목록">
        <template v-if="liveItems.length">
          <article
            v-for="item in liveItems"
            :key="item.id"
            class="live-card ds-surface live-card--clickable"
            @click="openLiveDetail(item.id)"
          >
            <div class="live-thumb">
              <img class="live-thumb__img" :src="item.thumb" :alt="item.title" loading="lazy" />
              <div class="live-badges">
                <span class="badge badge--live">{{ item.status }}</span>
              </div>
            </div>
            <div class="live-body">
              <div class="live-meta">
                <p class="live-title">{{ item.title }}</p>
                <p class="live-date">{{ item.datetime }}</p>
                <p class="live-seller">{{ item.sellerName }}</p>
                <p class="live-viewers">시청자 {{ item.viewers }}명</p>
              </div>
            </div>
          </article>
        </template>

        <article v-else class="live-card ds-surface live-card--empty">
          <p class="live-card__title">진행 중인 방송이 없습니다.</p>
          <p class="live-card__meta">현재 라이브 방송이 비어 있습니다.</p>
        </article>
      </div>

      <div v-else class="carousel-wrap">
        <div class="live-carousel" aria-label="방송 중 목록">
          <template v-if="liveItems.length">
            <article
              v-for="item in liveItems"
              :key="item.id"
              class="live-card ds-surface live-card--clickable"
              @click="openLiveDetail(item.id)"
            >
              <div class="live-thumb">
                <img class="live-thumb__img" :src="item.thumb" :alt="item.title" loading="lazy" />
                <div class="live-badges">
                  <span class="badge badge--live">{{ item.status }}</span>
                </div>
              </div>
              <div class="live-body">
                <div class="live-meta">
                  <p class="live-title">{{ item.title }}</p>
                  <p class="live-date">{{ item.datetime }}</p>
                  <p class="live-seller">{{ item.sellerName }}</p>
                  <p class="live-viewers">시청자 {{ item.viewers }}명</p>
                </div>
              </div>
            </article>
          </template>

          <article v-else class="live-card ds-surface live-card--empty">
            <p class="live-card__title">진행 중인 방송이 없습니다.</p>
            <p class="live-card__meta">현재 라이브 방송이 비어 있습니다.</p>
          </article>
        </div>
      </div>
    </section>

    <section v-if="visibleScheduled" class="live-section">
      <div class="live-section__head">
        <h3>예약된 방송</h3>
        <p class="ds-section-sub">예약된 방송 목록을 확인합니다.</p>
      </div>

      <div v-if="activeTab === 'scheduled'" class="scheduled-grid" aria-label="예약 방송 목록">
        <template v-if="scheduledItems.length">
          <article
            v-for="item in scheduledItems"
            :key="item.id"
            class="live-card ds-surface live-card--clickable"
            @click="openReservationDetail(item.id)"
          >
            <div class="live-thumb">
              <img class="live-thumb__img" :src="item.thumb" :alt="item.title" loading="lazy" />
              <div class="live-badges">
                <span class="badge badge--scheduled" :class="{ 'badge--cancelled': item.status === '취소됨' }">{{ item.status }}</span>
              </div>
            </div>
            <div class="live-body">
              <div class="live-meta">
                <p class="live-title">{{ item.title }}</p>
                <p class="live-date">{{ item.datetime }}</p>
                <p class="live-seller">{{ item.sellerName }}</p>
              </div>
            </div>
          </article>
        </template>

        <article v-else class="live-card ds-surface live-card--empty">
          <p class="live-card__title">등록된 방송이 없습니다.</p>
          <p class="live-card__meta">예약 방송이 비어 있습니다.</p>
        </article>
      </div>

      <div v-else class="carousel-wrap">
        <div class="live-carousel" aria-label="예약 방송 목록">
          <template v-if="scheduledItems.length">
            <article
              v-for="item in scheduledItems"
              :key="item.id"
              class="live-card ds-surface live-card--clickable"
              @click="openReservationDetail(item.id)"
            >
              <div class="live-thumb">
                <img class="live-thumb__img" :src="item.thumb" :alt="item.title" loading="lazy" />
                <div class="live-badges">
                  <span class="badge badge--scheduled" :class="{ 'badge--cancelled': item.status === '취소됨' }">{{ item.status }}</span>
                </div>
              </div>
              <div class="live-body">
                <div class="live-meta">
                  <p class="live-title">{{ item.title }}</p>
                  <p class="live-date">{{ item.datetime }}</p>
                  <p class="live-seller">{{ item.sellerName }}</p>
                </div>
              </div>
            </article>
          </template>

          <article v-else class="live-card ds-surface live-card--empty">
            <p class="live-card__title">등록된 방송이 없습니다.</p>
            <p class="live-card__meta">예약 방송이 비어 있습니다.</p>
          </article>
        </div>
      </div>
    </section>

    <section v-if="visibleVod" class="live-section">
      <div class="live-section__head">
        <h3>VOD</h3>
        <p class="ds-section-sub">저장된 다시보기 콘텐츠를 확인합니다.</p>
      </div>

      <div v-if="activeTab === 'vod'" class="vod-grid" aria-label="VOD 목록">
        <template v-if="vodItems.length">
          <article
            v-for="item in vodItems"
            :key="item.id"
            class="live-card ds-surface live-card--clickable"
            @click="openVodDetail(item.id)"
          >
            <div class="live-thumb">
              <img class="live-thumb__img" :src="item.thumb" :alt="item.title" loading="lazy" />
              <div class="live-badges">
                <span class="badge badge--vod">{{ item.statusLabel }}</span>
              </div>
            </div>
            <div class="live-body">
              <div class="live-meta">
                <p class="live-title">{{ item.title }}</p>
                <p class="live-date">{{ item.datetime }}</p>
                <p class="live-seller">{{ item.sellerName }}</p>
              </div>
            </div>
          </article>
        </template>

        <article v-else class="live-card ds-surface live-card--empty">
          <p class="live-card__title">등록된 VOD가 없습니다.</p>
          <p class="live-card__meta">방송이 종료되면 자동 등록됩니다.</p>
        </article>
      </div>

      <div v-else class="carousel-wrap">
        <div class="live-carousel" aria-label="VOD 목록">
        <template v-if="vodItems.length">
          <article
            v-for="item in vodItems"
            :key="item.id"
            class="live-card ds-surface live-card--clickable"
            @click="openVodDetail(item.id)"
          >
            <div class="live-thumb">
              <img class="live-thumb__img" :src="item.thumb" :alt="item.title" loading="lazy" />
              <div class="live-badges">
                <span class="badge badge--vod">{{ item.statusLabel }}</span>
              </div>
            </div>
            <div class="live-body">
              <div class="live-meta">
                <p class="live-title">{{ item.title }}</p>
                <p class="live-date">{{ item.datetime }}</p>
                <p class="live-seller">{{ item.sellerName }}</p>
              </div>
            </div>
          </article>
        </template>

          <article v-else class="live-card ds-surface live-card--empty">
            <p class="live-card__title">등록된 VOD가 없습니다.</p>
            <p class="live-card__meta">방송이 종료되면 자동 등록됩니다.</p>
          </article>
        </div>
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
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.live-section__head h3 {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 900;
  color: var(--text-strong);
}

.carousel-wrap {
  position: relative;
}

.live-carousel {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(280px, 320px);
  gap: 14px;
  overflow-x: auto;
  padding: 10px 10px;
  scroll-snap-type: x mandatory;
  -webkit-overflow-scrolling: touch;
}

.live-carousel::-webkit-scrollbar {
  height: 10px;
}

.live-carousel::-webkit-scrollbar-thumb {
  background: rgba(15, 23, 42, 0.12);
  border-radius: 999px;
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
  right: 10px;
  top: 10px;
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

.live-grid,
.scheduled-grid,
.vod-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 1200px) {
  .live-grid,
  .scheduled-grid,
  .vod-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .live-grid,
  .scheduled-grid,
  .vod-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .live-header {
    grid-template-columns: 1fr;
    justify-items: start;
  }

  .live-carousel {
    padding: 10px 10px;
  }

  .live-grid,
  .scheduled-grid,
  .vod-grid {
    grid-template-columns: 1fr;
  }
}
</style>
