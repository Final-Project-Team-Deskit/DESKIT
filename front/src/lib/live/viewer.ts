import type { AuthUser } from '../auth'

const VIEWER_ID_KEY = 'deskit_live_viewer_id_v1'
let memoryViewerId: string | null = null

const buildViewerId = () =>
  window.crypto?.randomUUID
    ? window.crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`

const getStoredViewerId = (): string | null => {
  try {
    const existing = localStorage.getItem(VIEWER_ID_KEY)
    if (existing) {
      return existing
    }
    const next = buildViewerId()
    localStorage.setItem(VIEWER_ID_KEY, next)
    return next
  } catch {
    if (!memoryViewerId) {
      memoryViewerId = buildViewerId()
    }
    return memoryViewerId
  }
}

export const resolveViewerId = (user: AuthUser | null): string | null => {
  const directId =
    user?.id ??
    user?.userId ??
    user?.user_id ??
    (user as { memberId?: number | string })?.memberId ??
    (user as { member_id?: number | string })?.member_id ??
    user?.sellerId ??
    user?.seller_id
  if (directId !== null && directId !== undefined) {
    return String(directId)
  }

  const access =
    localStorage.getItem('access') ||
    sessionStorage.getItem('access') ||
    localStorage.getItem('access_token') ||
    sessionStorage.getItem('access_token')
  if (!access) {
    return getStoredViewerId()
  }
  const tokenParts = access.split('.')
  const tokenPart = tokenParts[1]
  if (!tokenPart) {
    return getStoredViewerId()
  }
  try {
    const normalized = tokenPart.replace(/-/g, '+').replace(/_/g, '/')
    const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), '=')
    const payload = JSON.parse(atob(padded))
    const tokenId =
      payload?.memberId ??
      payload?.member_id ??
      payload?.id ??
      payload?.userId ??
      payload?.user_id ??
      payload?.sellerId ??
      payload?.seller_id ??
      payload?.sub
    if (tokenId === null || tokenId === undefined) {
      return getStoredViewerId()
    }
    return String(tokenId)
  } catch {
    const storedId = getStoredViewerId()
    if (storedId) {
      return storedId
    }
    return null
  }
}
