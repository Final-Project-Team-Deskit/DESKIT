export type SellerVodSummary = {
  id: string
  title: string
  thumb: string
  startedAt: string
  endedAt: string
  statusLabel: string
}

export type SellerVodDetail = SellerVodSummary & {
  metrics: {
    maxViewers: number
    reports: number
    sanctions: number
    likes: number
    totalRevenue: number
  }
  vod: {
    visibility: '공개' | '비공개'
    url?: string
  }
  productResults: Array<{
    id: string
    name: string
    price: number
    soldQty: number
    revenue: number
  }>
}

const gradientThumb = (from: string, to: string) =>
  `data:image/svg+xml;utf8,` +
  `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 320 200'>` +
  `<defs><linearGradient id='g' x1='0' x2='1' y1='0' y2='1'>` +
  `<stop offset='0' stop-color='%23${from}'/>` +
  `<stop offset='1' stop-color='%23${to}'/>` +
  `</linearGradient></defs>` +
  `<rect width='320' height='200' fill='url(%23g)'/>` +
  `</svg>`

export const sellerVodSummaries: SellerVodSummary[] = [
  {
    id: 'vod-1',
    title: 'VOD: 홈오피스 기본기',
    thumb: gradientThumb('0f172a', '334155'),
    startedAt: '2025.12.10 13:00',
    endedAt: '2025.12.10 14:10',
    statusLabel: 'VOD',
  },
  {
    id: 'vod-2',
    title: 'VOD: 조명 세팅 특강',
    thumb: gradientThumb('111827', '1f2937'),
    startedAt: '2025.12.09 19:00',
    endedAt: '2025.12.09 20:05',
    statusLabel: 'VOD',
  },
  {
    id: 'vod-3',
    title: 'VOD: 케이블 정리',
    thumb: gradientThumb('1f2937', '0f172a'),
    startedAt: '2025.12.07 20:00',
    endedAt: '2025.12.07 21:00',
    statusLabel: 'VOD',
  },
  {
    id: 'vod-4',
    title: 'VOD: 게이밍 셋업',
    thumb: gradientThumb('0b1324', '0f172a'),
    startedAt: '2025.12.05 18:30',
    endedAt: '2025.12.05 19:25',
    statusLabel: 'VOD',
  },
]

const detailsById: Record<string, SellerVodDetail> = {
  'vod-1': {
    ...sellerVodSummaries[0],
    metrics: { maxViewers: 1420, reports: 2, sanctions: 0, likes: 320, totalRevenue: 2480000 },
    vod: { visibility: '공개' },
    productResults: [
      { id: 'p-1', name: '모던 스탠딩 데스크', price: 329000, soldQty: 48, revenue: 1580000 },
      { id: 'p-2', name: '알루미늄 모니터암', price: 129000, soldQty: 36, revenue: 464000 },
      { id: 'p-3', name: '데스크 매트', price: 59000, soldQty: 25, revenue: 1475000 },
    ],
  },
  'vod-2': {
    ...sellerVodSummaries[1],
    metrics: { maxViewers: 980, reports: 1, sanctions: 0, likes: 210, totalRevenue: 1860000 },
    vod: { visibility: '공개' },
    productResults: [
      { id: 'p-4', name: 'LED 데스크 램프', price: 89000, soldQty: 32, revenue: 2848000 },
      { id: 'p-5', name: 'RGB 라이트바', price: 59000, soldQty: 28, revenue: 1652000 },
    ],
  },
  'vod-3': {
    ...sellerVodSummaries[2],
    metrics: { maxViewers: 760, reports: 0, sanctions: 0, likes: 180, totalRevenue: 1240000 },
    vod: { visibility: '비공개' },
    productResults: [
      { id: 'p-6', name: '케이블 정리 키트', price: 32000, soldQty: 40, revenue: 1280000 },
      { id: 'p-7', name: '멀티탭 정리함', price: 42000, soldQty: 18, revenue: 756000 },
    ],
  },
  'vod-4': {
    ...sellerVodSummaries[3],
    metrics: { maxViewers: 1880, reports: 3, sanctions: 1, likes: 410, totalRevenue: 3280000 },
    vod: { visibility: '공개' },
    productResults: [
      { id: 'p-8', name: '게이밍 데스크 패드 XL', price: 69000, soldQty: 52, revenue: 3588000 },
      { id: 'p-9', name: '듀얼 모니터암', price: 159000, soldQty: 22, revenue: 3498000 },
    ],
  },
}

export const sellerVodDetails: Record<string, SellerVodDetail> = detailsById

export const getSellerVodDetail = (id: string): SellerVodDetail => {
  const fallback = sellerVodDetails[sellerVodSummaries[0]?.id] ?? detailsById['vod-1']
  return sellerVodDetails[id] ?? fallback
}
