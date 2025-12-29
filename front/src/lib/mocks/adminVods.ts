import { getSellerVodDetail, sellerVodSummaries, type SellerVodDetail } from './sellerVods'

export type AdminVodSummary = {
  id: string
  title: string
  thumb: string
  startedAt: string
  endedAt: string
  statusLabel: string
  sellerName: string
}

export type AdminVodDetail = SellerVodDetail & {
  sellerName: string
}

export const ADMIN_VODS_EVENT = 'deskit-admin-vods-updated'
const STORAGE_KEY = 'deskit_mock_admin_vods_v1'

const safeParse = <T>(raw: string | null, fallback: T): T => {
  if (!raw) return fallback
  try {
    return JSON.parse(raw) as T
  } catch {
    return fallback
  }
}

const sellerNames = ['판매자 A', '판매자 B', '판매자 C']

const seedVods = (): AdminVodDetail[] => {
  return sellerVodSummaries.map((summary, index) => {
    const detail = getSellerVodDetail(summary.id)
    return {
      ...detail,
      sellerName: sellerNames[index % sellerNames.length],
    }
  })
}

const readAll = (): AdminVodDetail[] => {
  const parsed = safeParse<AdminVodDetail[]>(localStorage.getItem(STORAGE_KEY), [])
  if (parsed.length > 0) return parsed
  const seeded = seedVods()
  localStorage.setItem(STORAGE_KEY, JSON.stringify(seeded))
  return seeded
}

export const getAdminVodSummaries = (): AdminVodSummary[] => {
  return readAll().map((detail) => ({
    id: detail.id,
    title: detail.title,
    thumb: detail.thumb,
    startedAt: detail.startedAt,
    endedAt: detail.endedAt,
    statusLabel: detail.statusLabel,
    sellerName: detail.sellerName,
  }))
}

export const getAdminVodDetail = (id: string): AdminVodDetail => {
  const all = readAll()
  return all.find((item) => item.id === id) ?? all[0] ?? seedVods()[0]
}
