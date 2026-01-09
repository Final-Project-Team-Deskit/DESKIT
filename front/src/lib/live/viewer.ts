import type { AuthUser } from '../auth'

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
  if (!access) return null
  const tokenParts = access.split('.')
  if (tokenParts.length < 2) return null
  try {
    const normalized = tokenParts[1].replace(/-/g, '+').replace(/_/g, '/')
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
    if (tokenId === null || tokenId === undefined) return null
    return String(tokenId)
  } catch {
    return null
  }
}
