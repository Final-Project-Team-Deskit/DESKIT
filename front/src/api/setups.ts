import { http } from './http'
import { endpoints } from './endpoints'
import { type SetupWithProducts } from '../lib/setups-data'
import {
  fetchDetailTextJson,
  fetchListTextJsonWithRetry,
  isPlainObject,
} from './api-text-json'

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
  const payload = await fetchListTextJsonWithRetry(http, endpoints.setups, {
    validateStatus: (status) => (status >= 200 && status < 300) || status === 304,
  })
  return (payload as any[]).map(normalizeSetup)
}

export const getSetupDetail = async (
  id: string | number
): Promise<SetupWithProducts | null> => {
  const item = await fetchDetailTextJson(http, endpoints.setupDetail(id), {
    validateStatus: (status) => (status >= 200 && status < 300) || status === 404,
  })

  return isPlainObject(item) ? normalizeSetup(item) : null
}
