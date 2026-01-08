import { http } from '../../api/http'
import { parseLiveDate } from './utils'

export type BroadcastCategory = {
  id: number
  name: string
}

export type ReservationSlot = {
  time: string
  remainingCapacity: number
}

export type SellerProduct = {
  id: string
  name: string
  option: string
  price: number
  broadcastPrice: number
  stock: number
  quantity: number
  thumb?: string
}

export type BroadcastListItem = {
  broadcastId: number
  title: string
  notice?: string
  sellerName?: string
  categoryName?: string
  thumbnailUrl?: string
  status?: string
  startAt?: string
  endAt?: string
  viewerCount?: number
  liveViewerCount?: number
  reportCount?: number
  totalSales?: number | string
  totalLikes?: number
  isPublic?: boolean
  adminLock?: boolean
}

export type BroadcastDetail = {
  broadcastId: number
  sellerName?: string
  title: string
  notice?: string
  status?: string
  categoryName?: string
  scheduledAt?: string
  startedAt?: string
  thumbnailUrl?: string
  vodUrl?: string
  totalViews?: number
  totalLikes?: number
  totalReports?: number
}

export type BroadcastProductItem = {
  id: string
  name: string
  imageUrl: string
  price: number
  isSoldOut: boolean
  stockQty: number
}

export type BroadcastStats = {
  viewerCount: number
  likeCount: number
  reportCount: number
}

export type BroadcastResult = {
  totalSales?: number | string
  totalViews?: number
  totalLikes?: number
}

type ApiResult<T> = {
  success: boolean
  data: T
  error?: { code: string; message: string }
}

type BroadcastPayload = {
  title: string
  notice: string
  categoryId: number
  scheduledAt: string
  thumbnailUrl: string
  waitScreenUrl: string | null
  broadcastLayout: string
  products: Array<{ productId: number; salePrice: number; quantity: number }>
  qcards: Array<{ question: string }>
}

type BroadcastAllResponse = {
  onAir: BroadcastListItem[]
  reserved: BroadcastListItem[]
  vod: BroadcastListItem[]
}

const addMinutes = (value: string, minutes: number) => {
  const base = parseLiveDate(value)
  if (Number.isNaN(base.getTime())) {
    return value
  }
  const next = new Date(base.getTime() + minutes * 60 * 1000)
  return next.toISOString()
}

const ensureEndAt = (startAt?: string, endAt?: string, status?: string) => {
  if (endAt) return endAt
  if (!startAt) return ''
  if (status === 'ENDED' || status === 'VOD' || status === 'STOPPED') {
    return addMinutes(startAt, 60)
  }
  return addMinutes(startAt, 60)
}

const ensureSuccess = <T>(response: ApiResult<T>) => {
  if (response?.success) return response.data
  const error = response?.error
  const message = error?.message ?? '요청 처리에 실패했습니다.'
  const apiError = { message, code: error?.code }
  throw apiError
}

export const fetchCategories = async (): Promise<BroadcastCategory[]> => {
  const { data } = await http.get<ApiResult<Array<{ id: number; name: string }>>>('/api/categories')
  const payload = ensureSuccess(data)
  return payload.map((category) => ({ id: category.id, name: category.name }))
}

export const fetchSellerProducts = async (): Promise<SellerProduct[]> => {
  const { data } = await http.get<ApiResult<Array<{ productId: number; productName: string; price: number; stockQty: number; imageUrl?: string }>>>(
    '/api/seller/broadcasts/products',
  )
  const payload = ensureSuccess(data)
  return payload.map((item) => ({
    id: `prod-${item.productId}`,
    name: item.productName,
    option: '-',
    price: item.price,
    broadcastPrice: item.price,
    stock: item.stockQty,
    quantity: 1,
    thumb: item.imageUrl ?? '',
  }))
}

export const fetchReservationSlots = async (date: string): Promise<ReservationSlot[]> => {
  const { data } = await http.get<ApiResult<Array<{ slotDateTime: string; remainingCapacity: number; selectable: boolean }>>>(
    '/api/seller/broadcasts/reservation-slots',
    { params: { date } },
  )
  const payload = ensureSuccess(data)
  return payload
    .filter((slot) => slot.selectable)
    .map((slot) => ({
      time: slot.slotDateTime.slice(11, 16),
      remainingCapacity: slot.remainingCapacity,
    }))
}

export const fetchPublicBroadcastOverview = async (): Promise<BroadcastListItem[]> => {
  const { data } = await http.get<ApiResult<BroadcastAllResponse>>('/api/broadcasts', { params: { tab: 'ALL' } })
  const payload = ensureSuccess(data)
  return [...(payload.onAir ?? []), ...(payload.reserved ?? []), ...(payload.vod ?? [])].map((item) => ({
    ...item,
    startAt: item.startAt ?? '',
    endAt: ensureEndAt(item.startAt, item.endAt, item.status),
  }))
}

export const fetchPublicBroadcastDetail = async (broadcastId: number): Promise<BroadcastDetail> => {
  const { data } = await http.get<ApiResult<BroadcastDetail>>(`/api/broadcasts/${broadcastId}`)
  return ensureSuccess(data)
}

export const fetchBroadcastProducts = async (broadcastId: number): Promise<BroadcastProductItem[]> => {
  const { data } = await http.get<
    ApiResult<Array<{ productId: number; name: string; imageUrl?: string; bpPrice: number; bpQuantity: number; status: string }>>
  >(`/api/broadcasts/${broadcastId}/products`)
  const payload = ensureSuccess(data)
  return payload.map((item) => ({
    id: String(item.productId),
    name: item.name,
    imageUrl: item.imageUrl ?? '/placeholder-product.jpg',
    price: item.bpPrice,
    isSoldOut: item.status === 'SOLDOUT' || item.bpQuantity <= 0,
    stockQty: item.bpQuantity,
  }))
}

export const fetchBroadcastStats = async (broadcastId: number): Promise<BroadcastStats> => {
  const { data } = await http.get<ApiResult<BroadcastStats>>(`/api/broadcasts/${broadcastId}/stats`)
  return ensureSuccess(data)
}

export const fetchSellerBroadcastReport = async (broadcastId: number): Promise<BroadcastResult> => {
  const { data } = await http.get<ApiResult<BroadcastResult>>(`/api/seller/broadcasts/${broadcastId}/report`)
  return ensureSuccess(data)
}

export const fetchSellerBroadcasts = async (params: { tab?: string; statusFilter?: string; sortType?: string; page?: number; size?: number }) => {
  const { data } = await http.get<ApiResult<{ content?: BroadcastListItem[]; slice?: BroadcastListItem[] }>>('/api/seller/broadcasts', { params })
  const payload = ensureSuccess(data)
  if (Array.isArray(payload)) {
    return payload as BroadcastListItem[]
  }
  const content = (payload as any)?.content
  if (Array.isArray(content)) return content
  const slice = (payload as any)?.slice
  if (Array.isArray(slice)) return slice
  return []
}

export const fetchAdminBroadcasts = async (params: { tab?: string; statusFilter?: string; sortType?: string; page?: number; size?: number }) => {
  const { data } = await http.get<ApiResult<{ content?: BroadcastListItem[]; slice?: BroadcastListItem[] }>>('/api/admin/broadcasts', { params })
  const payload = ensureSuccess(data)
  if (Array.isArray(payload)) {
    return payload as BroadcastListItem[]
  }
  const content = (payload as any)?.content
  if (Array.isArray(content)) return content
  const slice = (payload as any)?.slice
  if (Array.isArray(slice)) return slice
  return []
}

export const createBroadcast = async (payload: BroadcastPayload): Promise<number> => {
  const { data } = await http.post<ApiResult<number>>('/api/seller/broadcasts', payload)
  return ensureSuccess(data)
}

export const updateBroadcast = async (broadcastId: number, payload: BroadcastPayload): Promise<number> => {
  const { data } = await http.put<ApiResult<number>>(`/api/seller/broadcasts/${broadcastId}`, payload)
  return ensureSuccess(data)
}
