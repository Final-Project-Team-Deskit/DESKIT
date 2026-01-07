import { normalizeBroadcastStatus, type BroadcastStatus } from '../broadcastStatus'

export type AdminLiveSummary = {
  id: string
  title: string
  subtitle: string
  thumb: string
  startedAt: string
  status: BroadcastStatus
  sellerName: string
  viewers: number
  likes: number
  elapsed: string
  reports: number
  category: string
  stopReason?: string
  stopReasonDetail?: string
  stoppedAt?: string
}

export const ADMIN_LIVES_EVENT = 'deskit-admin-lives-updated'
const STORAGE_KEY = 'deskit_mock_admin_lives_v1'

const safeParse = <T>(raw: string | null, fallback: T): T => {
  if (!raw) return fallback
  try {
    return JSON.parse(raw) as T
  } catch {
    return fallback
  }
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

const sellerNames = ['판매자 A', '판매자 B', '판매자 C', '판매자 D']
const categories = ['홈오피스', '주변기기', '정리/수납', '조명'] as const

const today = new Date()
const formatToday = (hours: number, minutes: number) =>
  `${today.getFullYear()}.${String(today.getMonth() + 1).padStart(2, '0')}.${String(today.getDate()).padStart(2, '0')} ${String(
    hours,
  ).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`

const normalizeLive = (item: AdminLiveSummary): AdminLiveSummary => ({
  ...item,
  status: normalizeBroadcastStatus(item.status),
})

const seedLives = (): AdminLiveSummary[] => [
  {
    id: 'live-101',
    title: '모던 오피스 셋업 라이브',
    subtitle: '데스크/의자 조합',
    thumb: gradientThumb('0f172a', '1f2937'),
    startedAt: '2025.12.12 14:00',
    status: 'ON_AIR',
    sellerName: sellerNames[0],
    viewers: 328,
    likes: 124,
    elapsed: '00:15:20',
    reports: 18,
    category: categories[0],
  },
  {
    id: 'live-102',
    title: '게이밍 데스크 집중 리뷰',
    subtitle: '조명 & 주변기기',
    thumb: gradientThumb('111827', '0f172a'),
    startedAt: '2025.12.12 15:30',
    status: 'ON_AIR',
    sellerName: sellerNames[1],
    viewers: 512,
    likes: 210,
    elapsed: '00:42:10',
    reports: 22,
    category: categories[1],
  },
  {
    id: 'live-103',
    title: '미니멀 워크스테이션',
    subtitle: '수납/정리 팁',
    thumb: gradientThumb('1f2937', '111827'),
    startedAt: '2025.12.12 17:00',
    status: 'ON_AIR',
    sellerName: sellerNames[2],
    viewers: 214,
    likes: 86,
    elapsed: '00:08:34',
    reports: 7,
    category: categories[2],
  },
  {
    id: 'live-104',
    title: '무선 주변기기 라이브 Q&A',
    subtitle: '키보드/마우스 실시간 비교와 시연',
    thumb: gradientThumb('1f2937', '0f172a'),
    startedAt: formatToday(today.getHours(), Math.max(today.getMinutes() - 20, 0)),
    status: 'ON_AIR',
    sellerName: sellerNames[1],
    viewers: 920,
    likes: 312,
    elapsed: '00:20:00',
    reports: 14,
    category: categories[1],
  },
  {
    id: 'live-105',
    title: '스탠딩 데스크 실시간 리뷰',
    subtitle: '체형별 맞춤 스탠딩 데스크 세팅',
    thumb: gradientThumb('0b1324', '334155'),
    startedAt: formatToday(today.getHours(), Math.max(today.getMinutes() - 10, 0)),
    status: 'ON_AIR',
    sellerName: sellerNames[3],
    viewers: 1312,
    likes: 508,
    elapsed: '00:10:00',
    reports: 19,
    category: categories[0],
  },
]

const readAll = (): AdminLiveSummary[] => {
  const parsed = safeParse<AdminLiveSummary[]>(localStorage.getItem(STORAGE_KEY), []).map(normalizeLive)
  const seeded = seedLives().map(normalizeLive)

  if (parsed.length > 0) {
    const parsedMap = new Map(parsed.map((item) => [item.id, item]))
    const merged = seeded.map((seed) => {
      const stored = parsedMap.get(seed.id)
      if (!stored) return seed
      if (stored.status !== 'ON_AIR' && seed.status === 'ON_AIR') {
        return { ...stored, ...seed, status: 'ON_AIR' }
      }
      return { ...seed, ...stored }
    })

    parsed.forEach((item) => {
      if (!parsedMap.has(item.id) && !merged.find((m) => m.id === item.id)) {
        merged.push(item)
      }
    })

    localStorage.setItem(STORAGE_KEY, JSON.stringify(merged))
    return merged
  }

  localStorage.setItem(STORAGE_KEY, JSON.stringify(seeded))
  return seeded
}

export const getAdminLiveSummaries = (): AdminLiveSummary[] => {
  return readAll()
}

export const stopAdminLiveBroadcast = (
  liveId: string,
  payload: { reason: string; detail?: string },
): void => {
  const all = readAll()
  const now = new Date()
  const stoppedAt = `${now.getFullYear()}.${String(now.getMonth() + 1).padStart(2, '0')}.${String(
    now.getDate(),
  ).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
  const next = all.map((item) => {
    if (item.id !== liveId) return item
    if (item.status === 'STOPPED') return item
    return {
      ...item,
      status: 'STOPPED',
      stopReason: payload.reason,
      stopReasonDetail: payload.detail,
      stoppedAt,
    }
  })
  localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
  window.dispatchEvent(new Event(ADMIN_LIVES_EVENT))
}
