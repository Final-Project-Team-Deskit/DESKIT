<script setup lang="ts">
import { ref, watch } from 'vue'
import PageHeader from '../../components/PageHeader.vue'
import StatsBarChart from '../../components/stats/StatsBarChart.vue'
import StatsRankList from '../../components/stats/StatsRankList.vue'
import { fetchSellerStatistics } from '../../lib/live/api'

type RankView = 'best' | 'worst'
type StatsRange = 'daily' | 'monthly' | 'yearly'
type ChartDatum = { label: string; value: number }
type RankItem = { rank: number; title: string; value: number }
type RankGroup = { best: RankItem[]; worst: RankItem[] }

const revenueRange = ref<StatsRange>('monthly')
const perViewerRange = ref<StatsRange>('monthly')
const revenueRankView = ref<RankView>('best')
const viewerRankView = ref<RankView>('best')

const revenueRanks = ref<RankGroup>({ best: [], worst: [] })
const viewerRanks = ref<RankGroup>({ best: [], worst: [] })
const revenueChart = ref<ChartDatum[]>([])
const perViewerChart = ref<ChartDatum[]>([])

const periodMap: Record<StatsRange, string> = {
  daily: 'DAILY',
  monthly: 'MONTHLY',
  yearly: 'YEARLY',
}

const toNumber = (value: number | string | undefined) => {
  if (typeof value === 'number') return value
  if (typeof value === 'string') {
    const parsed = Number(value)
    return Number.isNaN(parsed) ? 0 : parsed
  }
  return 0
}

const mapChart = (items: Array<{ label: string; value: number | string }> = []) =>
  items.map((item) => ({
    label: item.label,
    value: toNumber(item.value),
  }))

const mapRankGroup = (
  best: Array<{ title: string; totalSales: number | string }> = [],
  worst: Array<{ title: string; totalSales: number | string }> = [],
): RankGroup => ({
  best: best.map((item, index) => ({ rank: index + 1, title: item.title, value: toNumber(item.totalSales) })),
  worst: worst.map((item, index) => ({ rank: index + 1, title: item.title, value: toNumber(item.totalSales) })),
})

const mapViewerRankGroup = (
  best: Array<{ title: string; totalViews: number }> = [],
  worst: Array<{ title: string; totalViews: number }> = [],
): RankGroup => ({
  best: best.map((item, index) => ({ rank: index + 1, title: item.title, value: item.totalViews })),
  worst: worst.map((item, index) => ({ rank: index + 1, title: item.title, value: item.totalViews })),
})

const formatCurrency = (value: number) => `₩${value.toLocaleString('ko-KR')}`
const formatViewerCount = (value: number) => `${value.toLocaleString('ko-KR')}명`

const loadRevenueStats = async () => {
  const payload = await fetchSellerStatistics(periodMap[revenueRange.value])
  revenueChart.value = mapChart(payload.salesChart)
  revenueRanks.value = mapRankGroup(payload.bestBroadcasts, payload.worstBroadcasts)
  viewerRanks.value = mapViewerRankGroup(payload.topViewerBroadcasts, payload.worstViewerBroadcasts ?? [])
}

const loadViewerStats = async () => {
  const payload = await fetchSellerStatistics(periodMap[perViewerRange.value])
  perViewerChart.value = mapChart(payload.arpuChart)
}

watch(revenueRange, () => {
  loadRevenueStats()
}, { immediate: true })

watch(perViewerRange, () => {
  loadViewerStats()
}, { immediate: true })
</script>

<template>
  <div class="stats-page">
    <PageHeader eyebrow="DESKIT" title="방송 통계" />

    <section class="stats-section">
      <article class="ds-surface stats-card">
        <header class="stats-card__head">
          <h3 class="stats-card__title">판매자 매출</h3>
          <div class="toggle-group" role="tablist" aria-label="판매자 매출 기간">
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': revenueRange === 'daily' }"
              @click="revenueRange = 'daily'"
            >
              일별
            </button>
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': revenueRange === 'monthly' }"
              @click="revenueRange = 'monthly'"
            >
              월별
            </button>
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': revenueRange === 'yearly' }"
              @click="revenueRange = 'yearly'"
            >
              연도별
            </button>
          </div>
        </header>
        <StatsBarChart :data="revenueChart" :value-formatter="formatCurrency" />
      </article>

      <article class="ds-surface stats-card">
        <header class="stats-card__head">
          <h3 class="stats-card__title">시청자 당 매출액</h3>
          <div class="toggle-group" role="tablist" aria-label="시청자당 매출 기간">
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': perViewerRange === 'daily' }"
              @click="perViewerRange = 'daily'"
            >
              일별
            </button>
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': perViewerRange === 'monthly' }"
              @click="perViewerRange = 'monthly'"
            >
              월별
            </button>
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': perViewerRange === 'yearly' }"
              @click="perViewerRange = 'yearly'"
            >
              연도별
            </button>
          </div>
        </header>
        <StatsBarChart :data="perViewerChart" :value-formatter="formatCurrency" />
      </article>
    </section>

    <section class="stats-section stats-section--ranks">
      <article class="ds-surface stats-card">
        <header class="stats-card__head">
          <h3 class="stats-card__title">매출 베스트/워스트 방송 5순위</h3>
          <div class="toggle-group" role="tablist" aria-label="매출 순위">
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': revenueRankView === 'best' }"
              @click="revenueRankView = 'best'"
            >
              베스트
            </button>
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': revenueRankView === 'worst' }"
              @click="revenueRankView = 'worst'"
            >
              워스트
            </button>
          </div>
        </header>
        <StatsRankList :items="revenueRanks[revenueRankView]" :value-formatter="formatCurrency" />
      </article>

      <article class="ds-surface stats-card">
        <header class="stats-card__head">
          <h3 class="stats-card__title">시청자 수 베스트/워스트 5순위</h3>
          <div class="toggle-group" role="tablist" aria-label="시청자 순위">
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': viewerRankView === 'best' }"
              @click="viewerRankView = 'best'"
            >
              베스트
            </button>
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': viewerRankView === 'worst' }"
              @click="viewerRankView = 'worst'"
            >
              워스트
            </button>
          </div>
        </header>
        <StatsRankList :items="viewerRanks[viewerRankView]" :value-formatter="formatViewerCount" />
      </article>
    </section>
  </div>
</template>

<style scoped>
.stats-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-section {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
}

.stats-section--ranks {
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
}

.stats-card {
  padding: 18px;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.stats-card__title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 800;
}

.toggle-group {
  display: inline-flex;
  gap: 8px;
}

.toggle-btn {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  padding: 8px 12px;
  border-radius: 10px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.15s ease;
}

.toggle-btn--active {
  background: rgba(var(--primary-rgb), 0.12);
  border-color: rgba(var(--primary-rgb), 0.6);
  color: var(--primary-color);
  box-shadow: 0 4px 12px rgba(var(--primary-rgb), 0.14);
}

@media (max-width: 640px) {
  .stats-card__head {
    flex-direction: column;
    align-items: flex-start;
  }

  .toggle-group {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
