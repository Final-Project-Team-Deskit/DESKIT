<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '../../components/PageHeader.vue'
import StatsBarChart from '../../components/stats/StatsBarChart.vue'
import StatsRankList from '../../components/stats/StatsRankList.vue'
import {
  getSellerRevenueRankings,
  getSellerViewerRankings,
  revenueData,
  revenuePerViewerData,
  type RankGroup,
  type StatsRange,
} from '../../lib/mocks/liveStats'

type RankView = 'best' | 'worst'

const revenueRange = ref<StatsRange>('monthly')
const perViewerRange = ref<StatsRange>('monthly')
const revenueRankView = ref<RankView>('best')
const viewerRankView = ref<RankView>('best')

const revenueRanks = ref<RankGroup>({ best: [], worst: [] })
const viewerRanks = ref<RankGroup>({ best: [], worst: [] })

const revenueChart = computed(() => revenueData[revenueRange.value])
const perViewerChart = computed(() => revenuePerViewerData[perViewerRange.value])

const formatCurrency = (value: number) => `₩${value.toLocaleString('ko-KR')}`
const formatViewerCount = (value: number) => `${value.toLocaleString('ko-KR')}명`

onMounted(() => {
  revenueRanks.value = getSellerRevenueRankings()
  viewerRanks.value = getSellerViewerRankings()
})
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
