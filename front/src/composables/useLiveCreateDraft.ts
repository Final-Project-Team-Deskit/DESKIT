import { getSellerReservationDetail, type SellerReservationDetail } from '../lib/mocks/sellerReservations'

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
  const raw = localStorage.getItem(DRAFT_KEY)
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
  localStorage.setItem(DRAFT_KEY, JSON.stringify(draft))
}

const parseCurrency = (value: string) => {
  const digits = value.replace(/[^\d]/g, '')
  const parsed = Number.parseInt(digits, 10)
  return Number.isNaN(parsed) ? 0 : parsed
}

export const buildDraftFromReservation = (reservationId: string): LiveCreateDraft => {
  const detail: SellerReservationDetail = getSellerReservationDetail(reservationId)
  const [datePart, timePart] = detail.datetime.split(' ')
  const mappedDate = datePart?.replace(/\./g, '-') ?? ''
  const mappedTime = timePart ?? ''

  return {
    ...createEmptyDraft(),
    title: detail.title,
    subtitle: detail.subtitle,
    category: detail.category,
    notice: detail.notice,
    date: mappedDate,
    time: mappedTime,
    thumb: detail.thumb,
    standbyThumb: (detail as any).standbyThumb ?? '',
    products: detail.products.map((item) => ({
      id: item.id,
      name: item.name,
      option: item.option ?? item.name,
      price: parseCurrency(item.price),
      broadcastPrice: parseCurrency(item.salePrice),
      stock: parseCurrency(item.stock),
      quantity: parseCurrency(item.qty) || 1,
      thumb: item.thumb,
    })),
    questions: mapQuestions(detail.cueQuestions ?? []),
    reservationId,
  }
}
