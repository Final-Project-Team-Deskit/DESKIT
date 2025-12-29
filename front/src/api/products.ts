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

  const { data } = await http.get<DbProduct[] | { data: DbProduct[] }>(endpoints.products)
  const payload = Array.isArray(data)
    ? data
    : isPlainObject(data) && Array.isArray(data.data)
      ? data.data
      : []
  return normalizeProducts(payload)
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

  const { data } = await http.get<DbProduct | { data: DbProduct }>(endpoints.productDetail(id))
  const payload =
    isPlainObject(data) && 'data' in data && isPlainObject(data.data) ? data.data : data
  return payload ? normalizeProduct(payload) : null
}

export const deleteProduct = async (id: string | number): Promise<void> => {
  if (USE_MOCK_API) {
    deleteMockProduct(String(id))
    return
  }

  await http.delete(endpoints.productDetail(id))
}
