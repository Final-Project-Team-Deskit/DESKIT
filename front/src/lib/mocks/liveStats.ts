import { getAdminVodDetail, getAdminVodSummaries } from './adminVods'
import { getSellerVodDetail, sellerVodSummaries } from './sellerVods'

export type StatsRange = 'daily' | 'monthly' | 'yearly'

export type ChartDatum = { label: string; value: number }

export type RankItem = { rank: number; title: string; value: number }

export type RankGroup = { best: RankItem[]; worst: RankItem[] }

const formatDay = (date: Date) => {
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${month}.${day}`
}

const buildRecentDays = (length: number, base: number, step: number): ChartDatum[] => {
  const today = new Date()
  const start = new Date(today)
  start.setDate(today.getDate() - (length - 1))

  return Array.from({ length }, (_, index) => {
    const current = new Date(start)
    current.setDate(start.getDate() + index)
    return { label: formatDay(current), value: base + index * step }
  })
}

const buildRecentMonths = (length: number, base: number, step: number): ChartDatum[] => {
  const today = new Date()
  const start = new Date(today)
  start.setMonth(today.getMonth() - (length - 1), 1)

  return Array.from({ length }, (_, index) => {
    const current = new Date(start)
    current.setMonth(start.getMonth() + index, 1)
    return { label: `${current.getMonth() + 1}월`, value: base + index * step }
  })
}

const buildRecentYears = (length: number, base: number, step: number): ChartDatum[] => {
  const today = new Date()
  const startYear = today.getFullYear() - (length - 1)
  return Array.from({ length }, (_, index) => {
    const year = startYear + index
    return { label: `${year}`, value: base + index * step }
  })
}

export const revenueData: Record<StatsRange, ChartDatum[]> = {
  daily: buildRecentDays(7, 3_200_000, 160_000),
  monthly: buildRecentMonths(12, 62_000_000, 2_800_000),
  yearly: buildRecentYears(5, 710_000_000, 42_000_000),
}

export const revenuePerViewerData: Record<StatsRange, ChartDatum[]> = {
  daily: buildRecentDays(7, 12_800, 700),
  monthly: buildRecentMonths(12, 28_000, 1_200),
  yearly: buildRecentYears(5, 44_000, 2_000),
}

const createRankGroup = (items: Array<{ title: string; value: number }>): RankGroup => {
  const sortedDesc = [...items].sort((a, b) => b.value - a.value)
  const sortedAsc = [...items].sort((a, b) => a.value - b.value)

  return {
    best: sortedDesc.slice(0, 5).map((item, index) => ({ ...item, rank: index + 1 })),
    worst: sortedAsc.slice(0, 5).map((item, index) => ({ ...item, rank: index + 1 })),
  }
}

const readAdminVodDetails = () => {
  const summaries = getAdminVodSummaries()
  return summaries.map((summary) => getAdminVodDetail(summary.id))
}

export const getAdminRevenueRankings = (): RankGroup => {
  const vodDetails = readAdminVodDetails()
  const revenueItems = vodDetails.map((vod) => ({
    title: `${vod.sellerName} · ${vod.title}`,
    value: vod.metrics.totalRevenue,
  }))
  return createRankGroup(revenueItems)
}

export const getAdminProductRevenueRankings = (): RankGroup => {
  const vodDetails = readAdminVodDetails()
  const productItems = vodDetails.flatMap((vod) =>
    vod.productResults.map((product) => ({
      title: product.name,
      value: product.revenue,
    })),
  )
  return createRankGroup(productItems)
}

const readSellerVodDetails = () => {
  return sellerVodSummaries.map((summary) => getSellerVodDetail(summary.id))
}

export const getSellerRevenueRankings = (): RankGroup => {
  const vodDetails = readSellerVodDetails()
  const revenueItems = vodDetails.map((vod) => ({
    title: vod.title,
    value: vod.metrics.totalRevenue,
  }))
  return createRankGroup(revenueItems)
}

export const getSellerViewerRankings = (): RankGroup => {
  const vodDetails = readSellerVodDetails()
  const viewerItems = vodDetails.map((vod) => ({
    title: vod.title,
    value: vod.metrics.maxViewers,
  }))
  return createRankGroup(viewerItems)
}
