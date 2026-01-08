import { getAuthUser } from '../lib/auth'
import { fetchSellerBroadcastDetail, type BroadcastDetailResponse } from '../lib/live/api'

export type LiveCreateProduct = {
  id: string
  name: string
  option: string
  price: number
  broadcastPrice: number
  stock: number
  quantity: number
  thumb?: string
}

export type LiveCreateDraft = {
  questions: Array<{ id: string; text: string }>
  title: string
  subtitle: string
  category: string
  notice: string
  date: string
  time: string
  thumb: string
  standbyThumb: string
  termsAgreed: boolean
  products: LiveCreateProduct[]
  reservationId?: string
}

export const DRAFT_KEY = 'deskit_seller_broadcast_draft_v2'
const DRAFT_OWNER_KEY = 'deskit_seller_broadcast_draft_owner_v2'
const DRAFT_FLOW_KEY = 'deskit_seller_broadcast_draft_flow_v2'

const resolveSellerKey = () => {
  const user = getAuthUser()
  const sellerId = user?.seller_id ?? user?.sellerId ?? user?.id ?? user?.user_id ?? user?.userId
  return typeof sellerId === 'number' ? sellerId.toString() : ''
}

const getDraftStorage = () => sessionStorage

const readOwner = () => getDraftStorage().getItem(DRAFT_OWNER_KEY) ?? ''

const writeOwner = (ownerId: string) => {
  getDraftStorage().setItem(DRAFT_OWNER_KEY, ownerId)
}

const clearDraftStorage = () => {
  getDraftStorage().removeItem(DRAFT_KEY)
  getDraftStorage().removeItem(DRAFT_OWNER_KEY)
  getDraftStorage().removeItem(DRAFT_FLOW_KEY)
}

window.addEventListener('deskit-user-updated', () => {
  const ownerId = resolveSellerKey()
  if (!ownerId) {
    clearDraftStorage()
  }
})

const createId = () => `q-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`

const createQuestion = (text: string) => ({ id: createId(), text })

const mapQuestions = (seeds: string[]) => (seeds.length ? seeds : ['']).map((text) => createQuestion(text))

export const createDefaultQuestions = () => mapQuestions([])

export const createEmptyDraft = (): LiveCreateDraft => ({
  questions: createDefaultQuestions(),
  title: '',
  subtitle: '',
  category: '',
  notice: '',
  date: '',
  time: '',
  thumb: '',
  standbyThumb: '',
  termsAgreed: false,
  products: [],
  reservationId: undefined,
})

export const loadDraft = (): LiveCreateDraft | null => {
  const ownerId = resolveSellerKey()
  if (!ownerId) return null
  if (readOwner() && readOwner() !== ownerId) return null
  const raw = getDraftStorage().getItem(DRAFT_KEY)
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') return null

    const { cueTitle: _ignoredCueTitle, cueNotes: _ignoredCueNotes, ...rest } = parsed as Record<string, any>

    return {
      ...createEmptyDraft(),
      ...rest,
      questions: Array.isArray(rest.questions)
        ? rest.questions
            .filter((item: any) => item && typeof item.id === 'string' && typeof item.text === 'string')
            .map((item: any) => ({ id: item.id, text: item.text }))
        : createEmptyDraft().questions,
      products: Array.isArray(rest.products)
        ? rest.products
            .filter((item: any) => item && typeof item.id === 'string')
            .map((item: any) => ({
              id: item.id,
              name: item.name ?? '',
              option: item.option ?? '',
              price: typeof item.price === 'number' ? item.price : 0,
              broadcastPrice: typeof item.broadcastPrice === 'number' ? item.broadcastPrice : 0,
              stock: typeof item.stock === 'number' ? item.stock : 0,
              quantity: typeof item.quantity === 'number' ? item.quantity : 1,
              thumb: item.thumb ?? '',
            }))
        : [],
    }
  } catch (e) {
    console.error('Failed to load draft', e)
    return null
  }
}

export const saveDraft = (draft: LiveCreateDraft) => {
  const ownerId = resolveSellerKey()
  if (!ownerId) return
  writeOwner(ownerId)
  getDraftStorage().setItem(DRAFT_KEY, JSON.stringify(draft))
}

export const clearDraft = () => {
  clearDraftStorage()
}

export const activateDraftFlow = () => {
  const ownerId = resolveSellerKey()
  if (!ownerId) return
  writeOwner(ownerId)
  getDraftStorage().setItem(DRAFT_FLOW_KEY, 'active')
}

export const deactivateDraftFlow = () => {
  getDraftStorage().removeItem(DRAFT_FLOW_KEY)
}

export const isDraftFlowActive = () => getDraftStorage().getItem(DRAFT_FLOW_KEY) === 'active'

const parseCurrency = (value: string) => {
  const digits = value.replace(/[^\d]/g, '')
  const parsed = Number.parseInt(digits, 10)
  return Number.isNaN(parsed) ? 0 : parsed
}

const formatReservationDate = (scheduledAt?: string) => {
  if (!scheduledAt) return { date: '', time: '' }
  const normalized = scheduledAt.replace('T', ' ')
  const [datePart, timePart] = normalized.split(' ')
  return {
    date: datePart?.replace(/\./g, '-') ?? '',
    time: timePart?.slice(0, 5) ?? '',
  }
}

const mapReservationProducts = (detail: BroadcastDetailResponse) =>
  (detail.products ?? []).map((item) => ({
    id: `prod-${item.productId}`,
    name: item.name,
    option: item.name,
    price: item.originalPrice ?? 0,
    broadcastPrice: item.bpPrice ?? item.originalPrice ?? 0,
    stock: item.stockQty ?? item.bpQuantity ?? 0,
    quantity: item.bpQuantity ?? 1,
    thumb: item.imageUrl ?? '',
  }))

const mapReservationQuestions = (detail: BroadcastDetailResponse) =>
  mapQuestions((detail.qcards ?? []).map((card) => card.question))

export const buildDraftFromReservation = async (reservationId: string): Promise<LiveCreateDraft> => {
  try {
    const detail = await fetchSellerBroadcastDetail(Number(reservationId))
    const { date, time } = formatReservationDate(detail.scheduledAt)

    return {
      ...createEmptyDraft(),
      title: detail.title ?? '',
      subtitle: detail.sellerName ?? '',
      category: detail.categoryId ? String(detail.categoryId) : '',
      notice: detail.notice ?? '',
      date,
      time,
      thumb: detail.thumbnailUrl ?? '',
      standbyThumb: detail.waitScreenUrl ?? '',
      products: mapReservationProducts(detail),
      questions: mapReservationQuestions(detail),
      reservationId,
    }
  } catch (error) {
    console.error('Failed to load reservation draft', error)
    return createEmptyDraft()
  }
}
