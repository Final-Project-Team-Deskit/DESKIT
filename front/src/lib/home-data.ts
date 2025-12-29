import { liveItems as allLiveItems } from './live/data'
import { getLiveStatus, parseLiveDate } from './live/utils'
import type { LiveItem } from './live/types'
import type { ProductTags } from './products-data'
import { productsData } from './products-data'
import { setupsData } from './setups-data'

export type { LiveItem }

export type SetupItem = {
  id: string
  title: string
  description: string
  imageUrl: string
}

export type ProductItem = {
  id: string
  name: string
  imageUrl: string
  price: number
  originalPrice?: number
  tags: ProductTags
  isSoldOut?: boolean
}

const now = new Date()
const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
const windowEnd = new Date(
  todayStart.getFullYear(),
  todayStart.getMonth(),
  todayStart.getDate() + 6,
  23,
  59,
  59,
  999,
)

export { allLiveItems }

export const liveItems: LiveItem[] = allLiveItems
  .filter((item) => {
    const status = getLiveStatus(item, now)
    if (status === 'LIVE') {
      return true
    }
    if (status === 'UPCOMING') {
      const startAt = parseLiveDate(item.startAt).getTime()
      return startAt >= todayStart.getTime() && startAt <= windowEnd.getTime()
    }
    return false
  })
  .sort((a, b) => {
    const statusA = getLiveStatus(a, now)
    const statusB = getLiveStatus(b, now)

    if (statusA !== statusB) {
      return statusA === 'LIVE' ? -1 : 1
    }

    return parseLiveDate(a.startAt).getTime() - parseLiveDate(b.startAt).getTime()
  })
  .slice(0, 8)

export const popularSetups: SetupItem[] = setupsData.slice(0, 6).map((setup) => ({
  id: String(setup.setup_id),
  title: setup.title,
  description: setup.short_desc,
  imageUrl: setup.imageUrl || '/placeholder-setup.jpg',
}))

export const popularProducts: ProductItem[] = productsData
  .filter((product) => product.status === 'ON_SALE' || product.status === 'LIMITED_SALE')
  .sort((a, b) => (b.popularity ?? 0) - (a.popularity ?? 0))
  .slice(0, 12)
  .map((product) => ({
    id: String(product.product_id),
    name: product.name,
    imageUrl: product.imageUrl ?? '/placeholder-product.jpg',
    price: product.price,
    originalPrice: product.cost_price > product.price ? product.cost_price : undefined,
    tags: product.tags ?? { space: [], tone: [], situation: [], mood: [] },
    isSoldOut: product.status === 'SOLD_OUT',
  }))
