import { http } from './http'
import { endpoints } from './endpoints'
import { USE_MOCK_API } from './config'
import { type DbProduct } from '../lib/products-data'
import { deleteMockProduct, getAllMockProducts } from '../lib/mocks/sellerProducts'
import { normalizeProduct, normalizeProducts } from './products-normalizer'

const isPlainObject = (value: any) => {
  if (!value || typeof value !== 'object') return false
  if (Array.isArray(value)) return false
  return Object.prototype.toString.call(value) === '[object Object]'
}

export const listProducts = async (): Promise<DbProduct[]> => {
  if (USE_MOCK_API) {
    return getAllMockProducts()
  }

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
    return { payload, parseFailed, raw: parsed }
  }

  const fetchOnce = async () =>
    http.get<unknown>(endpoints.products, {
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
  let { payload, parseFailed, raw } = resolvePayload(response.data)
  const isRawEmpty =
    response.data == null || (typeof response.data === 'string' && response.data.trim() === '')
  if (response.status === 304 || isRawEmpty || parseFailed || payload.length === 0) {
    response = await fetchOnce()
    ;({ payload, parseFailed, raw } = resolvePayload(response.data))
  }

  return normalizeProducts(payload as DbProduct[])
}

export const listProductsWithAuthGuard = async (): Promise<{
  products: DbProduct[]
  authRequired: boolean
}> => {
  if (USE_MOCK_API) {
    return { products: getAllMockProducts(), authRequired: false }
  }

  const response = await http.get<string>(endpoints.products, {
    withCredentials: true,
    responseType: 'text',
    transformResponse: (data) => data,
  })

  const contentType = response.headers?.['content-type'] ?? ''
  const responseUrl = (response.request as XMLHttpRequest | undefined)?.responseURL ?? ''
  const isHtml = contentType.includes('text/html')
  const isLoginRedirect = responseUrl.includes('/login')
  if (isHtml || isLoginRedirect) {
    return { products: [], authRequired: true }
  }

  let parsed: unknown = response.data
  if (typeof parsed === 'string' && parsed.trim().length > 0) {
    try {
      parsed = JSON.parse(parsed)
    } catch {
      parsed = []
    }
  }

  const payload = Array.isArray(parsed)
    ? parsed
    : isPlainObject(parsed) && Array.isArray((parsed as { data?: unknown }).data)
      ? (parsed as { data: DbProduct[] }).data
      : []
  return { products: normalizeProducts(payload), authRequired: false }
}

export const getProductDetail = async (
  id: string | number
): Promise<DbProduct | null> => {
  if (USE_MOCK_API) {
    const products = getAllMockProducts()
    const found = products.find(
      (product) => String(product.product_id ?? product.id) === String(id)
    )
  if (!found) return null
  return found
  }

  const response = await http.get<unknown>(endpoints.productDetail(id), {
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

  return isPlainObject(item) ? normalizeProduct(item) : null
}

export const deleteProduct = async (id: string | number): Promise<void> => {
  if (USE_MOCK_API) {
    deleteMockProduct(String(id))
    return
  }

  await http.delete(endpoints.productDetail(id))
}
