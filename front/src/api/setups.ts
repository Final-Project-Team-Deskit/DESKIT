import { http } from './http'
import { endpoints } from './endpoints'
import { type SetupWithProducts } from '../lib/setups-data'

const isPlainObject = (value: unknown) => {
  if (!value || typeof value !== 'object') return false
  if (Array.isArray(value)) return false
  return Object.prototype.toString.call(value) === '[object Object]'
}

const normalizeSetup = (raw: any): SetupWithProducts => {
  const productIdsRaw = raw?.product_ids ?? raw?.productIds
  const setupProducts = raw?.setup_products ?? raw?.setupProducts
  const products = raw?.products
  const collectIds = (items: any[]) =>
    items
      .map((item) => {
        if (item && typeof item === 'object') {
          const source = item?.product && typeof item.product === 'object' ? item.product : item
          return Number(source?.product_id ?? source?.productId ?? source?.id)
        }
        return Number(item)
      })
      .filter((id) => Number.isFinite(id))
  const productIdsFromRelations = Array.isArray(setupProducts)
    ? collectIds(setupProducts)
    : Array.isArray(products)
      ? collectIds(products)
      : []
  const product_ids = [
    ...(Array.isArray(productIdsRaw) ? collectIds(productIdsRaw) : []),
    ...productIdsFromRelations,
  ]
  const uniqueProductIds = Array.from(new Set(product_ids))
  const tags = Array.isArray(raw?.tags) ? raw.tags : []
  return {
    setup_id: raw?.setup_id ?? raw?.id ?? 0,
    title: raw?.title ?? raw?.name ?? '',
    short_desc: raw?.short_desc ?? raw?.description ?? '',
    imageUrl: raw?.imageUrl ?? raw?.image_url ?? '/placeholder-setup.jpg',
    product_ids: uniqueProductIds,
    tags,
    tip: raw?.tip_text ?? raw?.tipText ?? raw?.tip ?? '',
    created_dt: raw?.created_dt ?? raw?.created_at ?? '',
    updated_dt: raw?.updated_dt ?? raw?.updated_at ?? '',
  }
}

export const listSetups = async (): Promise<SetupWithProducts[]> => {
  const resolvePayload = (data: unknown) => {
    let parsed: unknown = data
    let parseFailed = false
    if (typeof parsed === 'string' && parsed.trim().length > 0) {
      try {
        parsed = JSON.parse(parsed)
      } catch {
        parsed = undefined
        parseFailed = true
      }
    }
    const payload = Array.isArray(parsed)
      ? parsed
      : isPlainObject(parsed) && Array.isArray((parsed as { data?: unknown }).data)
        ? (parsed as { data: unknown[] }).data
        : []
    return { payload, parseFailed }
  }

  const fetchOnce = async () =>
    http.get<unknown>(endpoints.setups, {
      params: { _ts: Date.now() },
      headers: {
        'Cache-Control': 'no-store',
        Pragma: 'no-cache',
      },
      validateStatus: (status) => (status >= 200 && status < 300) || status === 304,
      responseType: 'text',
      transformResponse: (data) => data,
    })

  let response = await fetchOnce()
  let { payload, parseFailed } = resolvePayload(response.data)
  const isRawEmpty =
    response.data == null || (typeof response.data === 'string' && response.data.trim() === '')
  if (response.status === 304 || isRawEmpty || parseFailed || payload.length === 0) {
    response = await fetchOnce()
    ;({ payload, parseFailed } = resolvePayload(response.data))
  }

  return (payload as any[]).map(normalizeSetup)
}

export const getSetupDetail = async (
  id: string | number
): Promise<SetupWithProducts | null> => {
  const response = await http.get<unknown>(endpoints.setupDetail(id), {
    params: { _ts: Date.now() },
    headers: {
      'Cache-Control': 'no-store',
      Pragma: 'no-cache',
    },
    responseType: 'text',
    transformResponse: (data) => data,
    validateStatus: (status) => (status >= 200 && status < 300) || status === 404,
  })

  if (response.status === 404) return null

  let parsed: unknown = response.data
  if (typeof parsed === 'string' && parsed.trim().length > 0) {
    try {
      parsed = JSON.parse(parsed)
    } catch {
      parsed = undefined
    }
  }

  const item = Array.isArray(parsed)
    ? parsed[0]
    : isPlainObject(parsed) && Array.isArray((parsed as { data?: unknown }).data)
      ? (parsed as { data: unknown[] }).data?.[0]
      : isPlainObject(parsed) && isPlainObject((parsed as { data?: unknown }).data)
        ? (parsed as { data: unknown }).data
        : parsed

  return isPlainObject(item) ? normalizeSetup(item) : null
}
