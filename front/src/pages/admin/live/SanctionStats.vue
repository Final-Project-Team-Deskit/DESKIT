<script setup lang="ts">
import { ref } from 'vue'
import PageHeader from '../../../components/PageHeader.vue'
import StatsBarChart from '../../../components/stats/StatsBarChart.vue'
import StatsRankList from '../../../components/stats/StatsRankList.vue'

type Metric = 'daily' | 'monthly' | 'yearly'

const stopMetric = ref<Metric>('daily')
const viewerMetric = ref<Metric>('daily')

const formatDay = (date: Date) => {
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${month}.${day}`
}

const buildDailySeries = (values: number[]) => {
  const today = new Date()
  const start = new Date(today)
  start.setDate(today.getDate() - (values.length - 1))
  return values.map((value, index) => {
    const current = new Date(start)
    current.setDate(start.getDate() + index)
    return { label: formatDay(current), value }
  })
}

const buildMonthlySeries = (values: number[]) => {
  const today = new Date()
  const start = new Date(today)
  start.setMonth(today.getMonth() - (values.length - 1), 1)
  return values.map((value, index) => {
    const current = new Date(start)
    current.setMonth(start.getMonth() + index, 1)
    return { label: `${current.getMonth() + 1}월`, value }
  })
}

const buildYearlySeries = (values: number[]) => {
  const today = new Date()
  const startYear = today.getFullYear() - (values.length - 1)
  return values.map((value, index) => ({ label: `${startYear + index}`, value }))
}

const stopCountData: Record<Metric, Array<{ label: string; value: number }>> = {
  daily: buildDailySeries([6, 5, 7, 4, 6, 5, 8]),
  monthly: buildMonthlySeries([12, 14, 16, 18, 19, 21, 23, 20, 18, 17, 19, 22]),
  yearly: buildYearlySeries([58, 64, 71, 79, 83]),
}

const viewerSanctionData: Record<Metric, Array<{ label: string; value: number }>> = {
  daily: buildDailySeries([18, 15, 12, 14, 17, 13, 19]),
  monthly: buildMonthlySeries([208, 214, 226, 242, 255, 268, 274, 261, 248, 239, 245, 258]),
  yearly: buildYearlySeries([980, 1040, 1115, 1205, 1280]),
}

const topSellerStops = [
  { rank: 1, title: '판매자 A', value: 12 },
  { rank: 2, title: '판매자 B', value: 9 },
  { rank: 3, title: '판매자 C', value: 7 },
  { rank: 4, title: '판매자 D', value: 6 },
  { rank: 5, title: '판매자 E', value: 5 },
]

const topViewerSanctions = [
  { rank: 1, title: '시청자1', value: 18 },
  { rank: 2, title: '시청자2', value: 16 },
  { rank: 3, title: '시청자3', value: 14 },
  { rank: 4, title: '시청자4', value: 12 },
  { rank: 5, title: '시청자5', value: 10 },
]

const summaryCards = [
  { label: '총 송출 중지', value: '95건' },
  { label: '시청자 제재', value: '1,280건' },
  { label: '최근 제재일', value: '2025-12-12' },
]
</script>

<template>
  <div>
    <PageHeader eyebrow="DESKIT" title="제재 통계" />

    <section class="stat-grid">
      <article v-for="card in summaryCards" :key="card.label" class="ds-surface stat-card">
        <p class="stat-label">{{ card.label }}</p>
        <p class="stat-value">{{ card.value }}</p>
      </article>
    </section>

    <section class="chart-grid">
      <article class="ds-surface panel">
        <header class="panel__head">
          <div>
            <h3>송출 중지 횟수</h3>
            <p class="ds-section-sub">기간별 송출 중지 추이</p>
          </div>
          <div class="toggle-group" role="tablist" aria-label="송출 중지 기간">
            <button type="button" class="toggle-btn" :class="{ 'toggle-btn--active': stopMetric === 'daily' }" @click="stopMetric = 'daily'">
              일별
            </button>
            <button type="button" class="toggle-btn" :class="{ 'toggle-btn--active': stopMetric === 'monthly' }" @click="stopMetric = 'monthly'">
              월별
            </button>
            <button type="button" class="toggle-btn" :class="{ 'toggle-btn--active': stopMetric === 'yearly' }" @click="stopMetric = 'yearly'">
              연도별
            </button>
          </div>
        </header>
        <StatsBarChart :data="stopCountData[stopMetric]" />
      </article>

      <article class="ds-surface panel">
        <header class="panel__head">
          <div>
            <h3>시청자 제재 횟수</h3>
            <p class="ds-section-sub">기간별 시청자 제재 추이</p>
          </div>
          <div class="toggle-group" role="tablist" aria-label="시청자 제재 기간">
            <button type="button" class="toggle-btn" :class="{ 'toggle-btn--active': viewerMetric === 'daily' }" @click="viewerMetric = 'daily'">
              일별
            </button>
            <button type="button" class="toggle-btn" :class="{ 'toggle-btn--active': viewerMetric === 'monthly' }" @click="viewerMetric = 'monthly'">
              월별
            </button>
            <button type="button" class="toggle-btn" :class="{ 'toggle-btn--active': viewerMetric === 'yearly' }" @click="viewerMetric = 'yearly'">
              연도별
            </button>
          </div>
        </header>
        <StatsBarChart :data="viewerSanctionData[viewerMetric]" />
      </article>
    </section>

    <section class="ranks-grid">
      <article class="ds-surface panel">
        <h3 class="panel__title">송출 중지 횟수 많은 판매자 TOP 5</h3>
        <StatsRankList :items="topSellerStops" />
      </article>
      <article class="ds-surface panel">
        <h3 class="panel__title">제재 당한 횟수 많은 시청자 TOP 5</h3>
        <StatsRankList :items="topViewerSanctions" />
      </article>
    </section>
  </div>
</template>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.stat-card {
  padding: 14px;
  border-radius: 12px;
}

.stat-label {
  margin: 0 0 6px;
  color: var(--text-muted);
  font-weight: 800;
}

.stat-value {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 900;
  color: var(--text-strong);
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  gap: 14px;
}

.ranks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 14px;
  margin-top: 14px;
}

.panel {
  padding: 16px;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  flex-wrap: wrap;
}

.panel__title {
  margin: 0 0 6px;
  font-weight: 900;
  color: var(--text-strong);
}

.toggle-group {
  display: inline-flex;
  gap: 8px;
}

.toggle-btn {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 999px;
  padding: 8px 12px;
  font-weight: 800;
  cursor: pointer;
}

.toggle-btn--active {
  border-color: var(--primary-color);
  color: var(--primary-color);
  background: rgba(var(--primary-rgb), 0.08);
}
</style>
