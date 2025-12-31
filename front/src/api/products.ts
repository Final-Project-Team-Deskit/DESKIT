import { http } from './http'
import { endpoints } from './endpoints'
import { USE_MOCK_API } from './config'
import { type DbProduct } from '../lib/products-data'
import { deleteMockProduct, getAllMockProducts } from '../lib/mocks/sellerProducts'
import { normalizeProduct, normalizeProducts } from './products-normalizer'
import {
  fetchDetailTextJson,
  fetchListTextJsonWithRetry,
  isPlainObject,
  parseJsonIfString,
  pickListPayload,
} from './api-text-json'

export const listProducts = async (): Promise<DbProduct[]> => {
  if (USE_MOCK_API) {
    return getAllMockProducts()
  }

  const payload = await fetchListTextJsonWithRetry(http, endpoints.products, {
    validateStatus: (status) => (status >= 200 && status < 300) || status === 304,
  })
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

  const { parsed } = parseJsonIfString(response.data)
  const payload = pickListPayload(parsed)
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

  const item = await fetchDetailTextJson(http, endpoints.productDetail(id), {
    validateStatus: (status) => (status >= 200 && status < 300) || status === 404,
  })

  return isPlainObject(item) ? normalizeProduct(item) : null
}

export const deleteProduct = async (id: string | number): Promise<void> => {
  if (USE_MOCK_API) {
    deleteMockProduct(String(id))
    return
  }

  await http.delete(endpoints.productDetail(id))
}
