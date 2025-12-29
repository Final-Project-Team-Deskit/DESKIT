export type ScheduledBroadcast = {
  id: string
  title: string
  subtitle: string
  thumb: string
  datetime: string
  ctaLabel: string
  products?: Array<{ id: string; title: string; option: string }>
  standbyThumb?: string
  termsAgreed?: boolean
}

const STORAGE_KEY = 'deskit_seller_scheduled_broadcasts_v1'

const isProductItem = (value: any): value is { id: string; title: string; option: string } => {
  return (
    value &&
    typeof value.id === 'string' &&
    typeof value.title === 'string' &&
    typeof value.option === 'string'
  )
}

const isScheduledBroadcast = (value: any): value is ScheduledBroadcast => {
  if (
    !value ||
    typeof value.id !== 'string' ||
    typeof value.title !== 'string' ||
    typeof value.subtitle !== 'string' ||
    typeof value.thumb !== 'string' ||
    typeof value.datetime !== 'string' ||
    typeof value.ctaLabel !== 'string'
  ) {
    return false
  }
  if (value.standbyThumb !== undefined && typeof value.standbyThumb !== 'string') {
    return false
  }
  if (value.termsAgreed !== undefined && typeof value.termsAgreed !== 'boolean') {
    return false
  }
  if (value.products !== undefined) {
    if (!Array.isArray(value.products)) return false
    if (!value.products.every(isProductItem)) return false
  }
  return true
}

export const getScheduledBroadcasts = (): ScheduledBroadcast[] => {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed.filter(isScheduledBroadcast)
  } catch {
    return []
  }
}

export const addScheduledBroadcast = (item: ScheduledBroadcast): void => {
  const current = getScheduledBroadcasts()
  const next = [item, ...current]
  localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
}

export const clearScheduledBroadcasts = (): void => {
  localStorage.removeItem(STORAGE_KEY)
}
