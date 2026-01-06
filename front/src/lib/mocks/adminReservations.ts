import { getSellerReservationDetail, sellerReservationSummaries, type SellerReservationDetail } from './sellerReservations'
import { normalizeBroadcastStatus } from '../broadcastStatus'

export type AdminReservationSummary = {
  id: string
  title: string
  subtitle: string
  thumb: string
  datetime: string
  ctaLabel: string
  status: string
  sellerName: string
  category?: string
}

export type AdminReservationDetail = SellerReservationDetail & {
  sellerName: string
}

export const ADMIN_RESERVATIONS_EVENT = 'deskit-admin-reservations-updated'
const STORAGE_KEY = 'deskit_mock_admin_reservations_v1'

const safeParse = <T>(raw: string | null, fallback: T): T => {
  if (!raw) return fallback
  try {
    return JSON.parse(raw) as T
  } catch {
    return fallback
  }
}

const sellerNames = ['판매자 A', '판매자 B', '판매자 C']

const seedReservations = (): AdminReservationDetail[] => {
  return sellerReservationSummaries.map((summary, index) => {
    const detail = getSellerReservationDetail(summary.id)
    return {
      ...detail,
      sellerName: sellerNames[index % sellerNames.length],
    }
  })
}

const normalizeDetail = (item: AdminReservationDetail): AdminReservationDetail => ({
  ...item,
  status: normalizeBroadcastStatus(item.status),
})

const readAll = (): AdminReservationDetail[] => {
  const parsed = safeParse<AdminReservationDetail[]>(localStorage.getItem(STORAGE_KEY), []).map(normalizeDetail)
  if (parsed.length > 0) return parsed
  const seeded = seedReservations().map(normalizeDetail)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(seeded))
  return seeded
}

const writeAll = (items: AdminReservationDetail[]) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(items))
  window.dispatchEvent(new Event(ADMIN_RESERVATIONS_EVENT))
}

export const getAdminReservationSummaries = (): AdminReservationSummary[] => {
  return readAll().map((detail) => ({
    id: detail.id,
    title: detail.title,
    subtitle: detail.subtitle,
    thumb: detail.thumb,
    datetime: detail.datetime,
    ctaLabel: detail.ctaLabel,
    status: detail.status,
    sellerName: detail.sellerName,
    category: detail.category,
  }))
}

export const getAdminReservationDetail = (id: string): AdminReservationDetail => {
  const all = readAll()
  return all.find((item) => item.id === id) ?? all[0] ?? seedReservations()[0]
}

export const cancelAdminReservation = (id: string): void => {
  const all = readAll()
  const next = all.map((item) => (item.id === id ? { ...item, status: 'CANCELED' } : item))
  writeAll(next)
}
