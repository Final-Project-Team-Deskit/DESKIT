import { productsData, type ProductStatus } from '../products-data'

export type LiveProductItem = {
  id: string
  name: string
  imageUrl: string
  price: number
  status: ProductStatus
  isSoldOut: boolean
}

export const liveProductMap: Record<string, string[]> = {
  'live-1': ['7', '1', '9'],
  'live-2': ['3', '4', '12'],
  'live-3': ['5', '8', '2'],
  'live-4': ['10', '6', '11'],
  'live-5': ['1', '2', '5'],
  'live-6': ['7', '9', '6'],
  'live-7': ['12', '5', '3'],
  'live-8': ['10', '2', '8'],
  'live-9': ['9', '4', '6'],
  'live-10': ['11', '7', '1'],
}

export const getProductsForLive = (liveId: string): LiveProductItem[] => {
  const ids = liveProductMap[liveId] ?? []
  if (!ids.length) {
    return []
  }

  const productMap = new Map(productsData.map((product) => [String(product.product_id), product]))

  return ids
    .map((id) => productMap.get(id))
    .filter((product): product is (typeof productsData)[number] => Boolean(product))
    .map((product) => ({
      id: String(product.product_id),
      name: product.name,
      imageUrl: product.imageUrl ?? '/placeholder-product.jpg',
      price: product.price,
      status: product.status,
      isSoldOut: product.status === 'SOLD_OUT',
    }))
}
