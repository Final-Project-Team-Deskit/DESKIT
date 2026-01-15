export const resolveWsBase = (apiBase?: string) => {
  const trimmed = (apiBase ?? '').trim()
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  const base = trimmed || origin

  if (!base) {
    return ''
  }

  try {
    const url = new URL(base, origin || 'http://localhost')
    const normalizedPath = url.pathname.replace(/\/api\/?$/, '').replace(/\/$/, '')
    url.pathname = normalizedPath || '/'
    return url.toString().replace(/\/$/, '')
  } catch {
    return base.replace(/\/api\/?$/, '').replace(/\/+$/, '')
  }
}
