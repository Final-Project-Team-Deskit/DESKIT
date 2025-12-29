import { flattenTags, type DbProduct, type ProductTags } from './products-data'

export type UiProduct = {
  id: string
  name: string
  imageUrl: string
  price: number
  originalPrice?: number
  description?: string
  popularity: number
  salesVolume: number
  category: string
  productCategory: 'furniture' | 'computer' | 'accessory'
  tags: ProductTags
  tagsFlat: string[]
  status: DbProduct['status']
  isSoldOut: boolean
  order: number
}

const categoryLabelMap: Record<'furniture' | 'computer' | 'accessory', string> = {
  furniture: '가구',
  computer: '전자기기',
  accessory: '악세서리',
}

const categoryFromId = (categoryId?: number | null): 'furniture' | 'computer' | 'accessory' => {
  switch (categoryId) {
    case 1:
      return 'furniture'
    case 2:
      return 'computer'
    case 3:
    default:
      return 'accessory'
  }
}

export const mapProducts = (items: DbProduct[]): UiProduct[] =>
  items
    .filter((product) => product.status !== 'DELETED')
    .map((product, index) => {
      const productCategory = product.productCategory ?? categoryFromId(product.category_id)
      const tags: ProductTags = product.tags ?? { space: [], tone: [], situation: [], mood: [] }
      const originalPrice = product.cost_price > product.price ? product.cost_price : undefined
      return {
        id: String(product.product_id),
        name: product.name,
        imageUrl: product.imageUrl ?? '/placeholder-product.jpg',
        price: product.price,
        originalPrice,
        description: product.short_desc,
        popularity: product.popularity ?? 0,
        salesVolume: product.salesVolume ?? 0,
        category: categoryLabelMap[productCategory],
        productCategory,
        tags,
        tagsFlat: product.tagsFlat ?? flattenTags(tags),
        status: product.status,
        isSoldOut: product.status === 'SOLD_OUT',
        order: index,
      }
    })
