export type SellerReservationSummary = {
  id: string
  title: string
  subtitle: string
  thumb: string
  datetime: string
  ctaLabel: string
}

export type SellerReservationDetail = SellerReservationSummary & {
  category: string
  status: string
  notice: string
  products: Array<{
    id: string
    name: string
    price: string
    salePrice: string
    qty: string
    stock: string
  }>
}

const gradientThumb = (from: string, to: string) =>
  `data:image/svg+xml;utf8,` +
  `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 320 200'>` +
  `<defs><linearGradient id='g' x1='0' x2='1' y1='0' y2='1'>` +
  `<stop offset='0' stop-color='%23${from}'/>` +
  `<stop offset='1' stop-color='%23${to}'/>` +
  `</linearGradient></defs>` +
  `<rect width='320' height='200' fill='url(%23g)'/>` +
  `</svg>`

export const sellerReservationSummaries: SellerReservationSummary[] = [
  {
    id: 'sch-1',
    title: '주간 인기 셋업 모음',
    subtitle: 'TOP 셋업 리뷰',
    thumb: gradientThumb('334155', '0f172a'),
    datetime: '2025.12.15 19:00',
    ctaLabel: '방송 시작',
  },
  {
    id: 'sch-2',
    title: '책상 수납 A to Z',
    subtitle: '정리의 기술',
    thumb: gradientThumb('1f2937', '334155'),
    datetime: '2025.12.16 20:00',
    ctaLabel: '방송 시작',
  },
  {
    id: 'sch-3',
    title: '마우스/키보드 추천',
    subtitle: '입문부터 고급까지',
    thumb: gradientThumb('0f172a', '1f2937'),
    datetime: '2025.12.17 18:30',
    ctaLabel: '방송 시작',
  },
  {
    id: 'sch-4',
    title: '의자 선택 가이드',
    subtitle: '허리 편한 셋업',
    thumb: gradientThumb('111827', '0f172a'),
    datetime: '2025.12.18 21:00',
    ctaLabel: '방송 시작',
  },
  {
    id: 'sch-5',
    title: '노트북 거치대 비교',
    subtitle: '자세/높이 세팅',
    thumb: gradientThumb('1f2937', '111827'),
    datetime: '2025.12.19 19:30',
    ctaLabel: '방송 시작',
  },
  {
    id: 'sch-6',
    title: '소형 방 책상 배치',
    subtitle: '공간 최적화',
    thumb: gradientThumb('0b1324', '111827'),
    datetime: '2025.12.20 20:00',
    ctaLabel: '방송 시작',
  },
]

const defaultNotice =
  '판매 상품 외 다른 상품 문의는 받지 않습니다. 방송 중 욕설 및 비방은 제재됩니다.'

const defaultProducts = [
  { id: 'p-1', name: '글로우 쿠션', price: '₩32,000', salePrice: '₩28,000', qty: '120', stock: '40' },
  { id: 'p-2', name: '클린 폼 클렌저', price: '₩18,000', salePrice: '₩15,000', qty: '200', stock: '85' },
  { id: 'p-3', name: '수분 토너 세트', price: '₩26,000', salePrice: '₩22,000', qty: '150', stock: '64' },
  { id: 'p-4', name: '립 틴트 컬렉션', price: '₩14,000', salePrice: '₩12,000', qty: '180', stock: '92' },
  { id: 'p-5', name: '선크림 듀오', price: '₩24,000', salePrice: '₩20,000', qty: '90', stock: '28' },
]

const detailsById: Record<
  string,
  { category: string; status: string; notice: string; products: SellerReservationDetail['products'] }
> = {
  'sch-1': { category: '홈오피스', status: '예약됨', notice: defaultNotice, products: defaultProducts },
  'sch-2': { category: '정리/수납', status: '예약됨', notice: defaultNotice, products: defaultProducts },
  'sch-3': { category: '주변기기', status: '예약됨', notice: defaultNotice, products: defaultProducts },
  'sch-4': { category: '의자', status: '예약됨', notice: defaultNotice, products: defaultProducts },
  'sch-5': { category: '노트북 액세서리', status: '예약됨', notice: defaultNotice, products: defaultProducts },
  'sch-6': { category: '공간 배치', status: '예약됨', notice: defaultNotice, products: defaultProducts },
}

export const sellerReservationDetails: Record<string, SellerReservationDetail> = sellerReservationSummaries.reduce(
  (acc, summary) => {
    const detail = detailsById[summary.id] ?? {
      category: '기타',
      status: '예약됨',
      notice: defaultNotice,
      products: defaultProducts,
    }
    acc[summary.id] = { ...summary, ...detail }
    return acc
  },
  {} as Record<string, SellerReservationDetail>,
)

const firstDetail =
  sellerReservationDetails[sellerReservationSummaries[0]?.id] ??
  ({
    ...sellerReservationSummaries[0],
    category: '기타',
    status: '예약됨',
    notice: defaultNotice,
    products: defaultProducts,
  } as SellerReservationDetail)

export const getSellerReservationDetail = (id: string): SellerReservationDetail => {
  return sellerReservationDetails[id] ?? firstDetail
}
