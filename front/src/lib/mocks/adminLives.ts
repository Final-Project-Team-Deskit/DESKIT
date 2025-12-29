export type AdminLiveSummary = {
  id: string
  title: string
  subtitle: string
  thumb: string
  startedAt: string
  status: '방송중' | '송출중지'
  sellerName: string
  viewers: number
  likes: number
  elapsed: string
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

const sellerNames = ['판매자 A', '판매자 B', '판매자 C']

const seedLives = (): AdminLiveSummary[] => [
  {
    id: 'live-101',
    title: '모던 오피스 셋업 라이브',
    subtitle: '데스크/의자 조합',
    thumb: gradientThumb('0f172a', '1f2937'),
    startedAt: '2025.12.12 14:00',
    status: '방송중',
    sellerName: sellerNames[0],
    viewers: 328,
    likes: 124,
    elapsed: '00:15:20',
  },
  {
    id: 'live-102',
    title: '게이밍 데스크 집중 리뷰',
    subtitle: '조명 & 주변기기',
    thumb: gradientThumb('111827', '0f172a'),
    startedAt: '2025.12.12 15:30',
    status: '방송중',
    sellerName: sellerNames[1],
    viewers: 512,
    likes: 210,
    elapsed: '00:42:10',
  },
  {
    id: 'live-103',
    title: '미니멀 워크스테이션',
    subtitle: '수납/정리 팁',
    thumb: gradientThumb('1f2937', '111827'),
    startedAt: '2025.12.12 17:00',
    status: '방송중',
    sellerName: sellerNames[2],
    viewers: 214,
    likes: 86,
    elapsed: '00:08:34',
  },
]

const readAll = (): AdminLiveSummary[] => {
  const parsed = safeParse<AdminLiveSummary[]>(localStorage.getItem(STORAGE_KEY), [])
  if (parsed.length > 0) return parsed
  const seeded = seedLives()
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
    if (item.status === '송출중지') return item
    return {
      ...item,
      status: '송출중지',
      stopReason: payload.reason,
      stopReasonDetail: payload.detail,
      stoppedAt,
    }
  })
  localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
  window.dispatchEvent(new Event(ADMIN_LIVES_EVENT))
}
