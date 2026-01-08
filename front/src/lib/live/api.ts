import { http } from '../../api/http'

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

export const createBroadcast = async (payload: BroadcastPayload): Promise<number> => {
  const { data } = await http.post<ApiResult<number>>('/api/seller/broadcasts', payload)
  return ensureSuccess(data)
}

export const updateBroadcast = async (broadcastId: number, payload: BroadcastPayload): Promise<number> => {
  const { data } = await http.put<ApiResult<number>>(`/api/seller/broadcasts/${broadcastId}`, payload)
  return ensureSuccess(data)
}
