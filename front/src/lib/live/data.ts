import type { LiveItem } from './types'

const now = new Date()
const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())

const addDays = (baseDate: Date, offset: number) => {
  return new Date(baseDate.getFullYear(), baseDate.getMonth(), baseDate.getDate() + offset)
}

const createScheduleTime = (dayOffset: number, hour: number, minute: number, durationMinutes: number) => {
  const day = addDays(today, dayOffset)
  const start = new Date(day.getFullYear(), day.getMonth(), day.getDate(), hour, minute)
  const end = new Date(start.getTime() + durationMinutes * 60 * 1000)

  return {
    startAt: start.toISOString(),
    endAt: end.toISOString(),
  }
}

const createLiveNowTime = (minutesAgo: number, minutesAhead: number) => {
  const start = new Date(now.getTime() - minutesAgo * 60 * 1000)
  const end = new Date(now.getTime() + minutesAhead * 60 * 1000)

  return {
    startAt: start.toISOString(),
    endAt: end.toISOString(),
  }
}

export const liveItems: LiveItem[] = [
  {
    id: 'live-1',
    title: '프리미엄 데스크 셋업 라이브',
    description: '인기 유튜버와 함께하는 특별한 데스크 셋업 쇼핑',
    thumbnailUrl: '/modern-desk-setup-live-stream.jpg',
    ...createScheduleTime(-3, 11, 0, 90),
    viewerCount: 842,
    sellerName: '데스키트 스튜디오',
  },
  {
    id: 'live-2',
    title: '게이밍 셋업 특가 쇼핑',
    description: '최고 성능의 게이밍 장비를 특별 가격으로',
    thumbnailUrl: '/gaming-rgb-desk-setup.jpg',
    ...createScheduleTime(-2, 14, 30, 80),
    viewerCount: 1210,
    sellerName: '게이밍 랩',
  },
  {
    id: 'live-3',
    title: '미니멀 오피스 꾸미기',
    description: '심플하고 세련된 업무 공간 만들기',
    thumbnailUrl: '/minimal-white-desk-setup.jpg',
    ...createScheduleTime(-1, 10, 0, 75),
    viewerCount: 968,
    sellerName: '오피스 데일리',
  },
  {
    id: 'live-4',
    title: '오늘의 데스크 셋업 쇼룸',
    description: '지금 진행 중인 인기 셋업을 만나보세요',
    thumbnailUrl: '/wooden-natural-desk-setup.jpg',
    ...createLiveNowTime(30, 90),
    viewerCount: 1547,
    sellerName: '데스크 피플',
  },
  {
    id: 'live-11',
    title: '무선 주변기기 라이브 Q&A',
    description: '키보드/마우스 실시간 비교와 시연',
    thumbnailUrl: '/wireless-gaming-mouse.jpg',
    ...createLiveNowTime(15, 55),
    viewerCount: 920,
    sellerName: '기어랩',
  },
  {
    id: 'live-12',
    title: '스탠딩 데스크 실시간 리뷰',
    description: '체형별 맞춤 스탠딩 데스크 세팅을 바로 보여드려요',
    thumbnailUrl: '/standing-desk-setup.jpg',
    ...createLiveNowTime(5, 70),
    viewerCount: 1312,
    sellerName: '라이브 피트',
  },
  {
    id: 'live-5',
    title: '모던 오피스 라이브 투어',
    description: '프로페셔널한 오피스 스타일링 팁',
    thumbnailUrl: '/professional-office-desk-setup.jpg',
    ...createScheduleTime(1, 13, 0, 60),
    viewerCount: 640,
    sellerName: '워크룸',
  },
  {
    id: 'live-6',
    title: '스탠딩 데스크 스타일',
    description: '올바른 자세를 위한 스탠딩 데스크 구성',
    thumbnailUrl: '/standing-desk-setup.jpg',
    ...createScheduleTime(2, 16, 0, 70),
    viewerCount: 522,
    sellerName: '스탠드업',
  },
  {
    id: 'live-7',
    title: '듀얼 모니터 생산성',
    description: '효율적인 듀얼 모니터 셋업 가이드',
    thumbnailUrl: '/dual-monitor-desk-setup.jpg',
    ...createScheduleTime(3, 11, 30, 85),
    viewerCount: 734,
    sellerName: '프로덕티브 스튜디오',
  },
  {
    id: 'live-8',
    title: '콘텐츠 크리에이터 데스크',
    description: '촬영 장비와 데스크 셋업을 한 번에',
    thumbnailUrl: '/content-creator-studio-desk.jpg',
    ...createScheduleTime(4, 19, 0, 90),
    viewerCount: 980,
    sellerName: '크리에이터랩',
  },
  {
    id: 'live-9',
    title: '컴팩트 데스크 아이디어',
    description: '작은 공간에서 최대 효율을 내는 셋업',
    thumbnailUrl: '/compact-small-desk-setup.jpg',
    ...createScheduleTime(5, 12, 0, 65),
    viewerCount: 410,
    sellerName: '스몰스페이스',
  },
  {
    id: 'live-10',
    title: '케이블 관리와 액세서리',
    description: '데스크 완성도를 높이는 디테일 아이템',
    thumbnailUrl: '/electric-standing-desk.jpg',
    ...createScheduleTime(6, 15, 30, 75),
    viewerCount: 588,
    sellerName: '클린 데스크',
  },
]
