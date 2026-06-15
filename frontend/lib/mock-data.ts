import type { Topic } from "@/components/topic-card"

export type ScheduledRoom = {
  id: string
  title: string
  description: string
  category: string
  scheduledAt: Date
  tags?: string[]
  estimatedParticipants?: number
  notifyCount?: number
}

// Scheduled rooms: approved by admin, not yet open — opens at scheduledAt
export const scheduledRooms: ScheduledRoom[] = [
  {
    id: "s1",
    title: "딥페이크 규제 강화, 표현의 자유와 충돌하는가?",
    description:
      "딥페이크 범죄 급증으로 규제 강화 목소리가 높아졌습니다. 창작의 자유와 피해 방지, 어디까지 규제해야 할까요?",
    category: "AI·기술",
    scheduledAt: new Date(Date.now() + 2 * 60 * 60 * 1000), // 2시간 후
    tags: ["딥페이크", "AI", "규제"],
    estimatedParticipants: 8000,
    notifyCount: 1243,
  },
  {
    id: "s2",
    title: "의대 증원 정책, 의료 공백 해소될 것인가?",
    description:
      "정부의 의대 정원 확대 방침에 의료계 반발이 거셉니다. 장기적 의료 인력 수급과 지역 의료 격차를 어떻게 볼 것인가요?",
    category: "사회·복지",
    scheduledAt: new Date(Date.now() + 26 * 60 * 60 * 1000), // 내일
    tags: ["의대", "의료", "정책"],
    estimatedParticipants: 15000,
    notifyCount: 3812,
  },
  {
    id: "s3",
    title: "한국 OTT 콘텐츠 글로벌화, 넷플릭스 의존도 괜찮은가?",
    description:
      "K-드라마 흥행이 넷플릭스 플랫폼에 집중되고 있습니다. 국내 콘텐츠 주권과 수익 배분 구조를 재검토해야 할까요?",
    category: "문화·연예",
    scheduledAt: new Date(Date.now() + 50 * 60 * 60 * 1000), // 모레
    tags: ["OTT", "K콘텐츠", "넷플릭스"],
    estimatedParticipants: 6500,
    notifyCount: 921,
  },
]

export const mockTopics: Topic[] = [
  {
    id: "1",
    title: "AI 생성 뉴스 시대, 신뢰를 어떻게 설계할까?",
    description:
      "ChatGPT 등 생성형 AI가 만든 기사가 언론을 장악하고 있습니다. 팩트체크 불가능한 AI 뉴스 시대에 미디어 신뢰를 어떻게 회복할 수 있을까요?",
    category: "AI·기술",
    status: "OPEN",
    participants: 12482,
    messages: 8400,
    likes: 317,
    timeLeft: "24:18",
    tags: ["AI", "미디어", "신뢰"],
    isLive: true,
    isTrending: true,
  },
  {
    id: "2",
    title: "주 4일제 도입 시 조직 문화는 어떻게 달라질까?",
    description:
      "국내 일부 기업에서 주 4일제를 시험 도입 중입니다. 생산성과 워라밸, 기업 경쟁력에 미치는 영향을 다각도로 분석합니다.",
    category: "경제·금융",
    status: "OPEN",
    participants: 4108,
    messages: 2100,
    likes: 129,
    timeLeft: "1:52:30",
    tags: ["노동", "주4일제", "워라밸"],
    isLive: true,
  },
  {
    id: "3",
    title: "청년 주거 문제를 해결하기 위한 새로운 접근은?",
    description:
      "고금리와 전셋값 폭등으로 청년들의 주거 불안이 심화되고 있습니다. 공공임대 확대 vs 시장 자율화, 어떤 방향이 맞을까요?",
    category: "사회·복지",
    status: "OPEN",
    participants: 18904,
    messages: 11200,
    likes: 442,
    tags: ["부동산", "청년", "주거"],
    isTrending: true,
  },
  {
    id: "4",
    title: "글로벌 AI 규제 프레임워크, 한국은 어디로 가야 하나?",
    description:
      "EU AI Act 시행으로 글로벌 AI 규제 경쟁이 시작됐습니다. 한국의 AI 규제 방향성과 산업 육성 전략에 대해 토의합니다.",
    category: "AI·기술",
    status: "OPEN",
    participants: 6731,
    messages: 3200,
    likes: 201,
    tags: ["AI규제", "EU", "정책"],
  },
  {
    id: "5",
    title: "기후 위기 대응, 개인의 선택 vs 기업·국가의 책임",
    description:
      "탄소 발자국 계산기 앱이 유행이지만, 실질적 감축은 기업과 국가 정책에 달려 있다는 반론도 있습니다. 책임의 주체를 논의합니다.",
    category: "환경·과학",
    status: "OPEN",
    participants: 9201,
    messages: 4800,
    likes: 358,
    tags: ["기후변화", "탄소", "ESG"],
  },
  {
    id: "6",
    title: "축법소년 연령, 낮춰야 할까? 처벌보다 교화인가",
    description:
      "최근 청소년 강력범죄 증가로 촉법소년 연령 하향 논의가 뜨겁습니다. 처벌 강화 vs 교화·재활 우선, 어떤 방향이 옳을까요?",
    category: "사회·복지",
    status: "OPEN",
    participants: 22100,
    messages: 15600,
    likes: 891,
    tags: ["청소년", "법", "촉법"],
    isTrending: true,
  },
  {
    id: "7",
    title: "한국 프로야구 FA 시장, 거품인가 정당한 보상인가?",
    description:
      "억대 연봉 FA 계약이 잇따르며 야구팬 사이에서 논쟁이 벌어지고 있습니다. 선수 가치와 시장 논리를 어떻게 봐야 할까요?",
    category: "스포츠",
    status: "OPEN",
    participants: 7832,
    messages: 5100,
    likes: 267,
    tags: ["KBO", "FA", "연봉"],
  },
  {
    id: "8",
    title: "넷플릭스 계정 공유 단속, 정당한가 과도한가?",
    description:
      "넷플릭스의 계정 공유 단속이 본격화됐습니다. 소비자 권리 침해인지, 정당한 비즈니스 결정인지 다양한 시각을 나눠봅니다.",
    category: "문화·연예",
    status: "CLOSED",
    participants: 31405,
    messages: 22000,
    likes: 1203,
    tags: ["넷플릭스", "OTT", "구독"],
  },
]

export const mockChatMessages = [
  {
    id: "m1",
    userId: "u1",
    nickname: "@logic_hunter",
    content: "AI 뉴스 신뢰는 결국 플랫폼 책임 문제입니다. 개인이 팩트체크를 다 할 수는 없죠.",
    timestamp: "14:23",
    isHighlighted: false,
  },
  {
    id: "m2",
    userId: "u2",
    nickname: "@media_critic",
    content: "동의합니다. 구글, 메타 같은 빅테크가 AI 생성 콘텐츠에 명확한 라벨링을 의무화해야 한다고 봐요.",
    timestamp: "14:24",
    isHighlighted: true,
  },
  {
    id: "m3",
    userId: "u3",
    nickname: "@tech_skeptic",
    content: "근데 그 라벨링 기준 자체를 누가 정하나요? 빅테크가 정하면 편향될 수 있잖아요",
    timestamp: "14:24",
    isHighlighted: false,
  },
  {
    id: "m4",
    userId: "u4",
    nickname: "@journalist_k",
    content: "독립적인 미디어 리터러시 교육 기관과 국제 기준이 필요합니다. UNESCO도 관련 논의 중이에요.",
    timestamp: "14:25",
    isHighlighted: false,
  },
  {
    id: "m5",
    userId: "u5",
    nickname: "@curious_mind",
    content: "AI가 생성한 뉴스라도 내용이 맞으면 괜찮지 않나요? 출처보다 내용의 정확성이 더 중요한 거 아닌가요?",
    timestamp: "14:25",
    isHighlighted: false,
  },
  {
    id: "m6",
    userId: "u6",
    nickname: "@prof_kim",
    content: "AI는 현재 존재하지 않는 '그럴듯한 사실'을 만들어냅니다. 할루시네이션 문제가 핵심이에요.",
    timestamp: "14:26",
    isHighlighted: true,
  },
  {
    id: "m7",
    userId: "u7",
    nickname: "@newbie_here",
    content: "저는 뉴스를 AI로만 보는데... 이제부터 더 조심해야겠네요",
    timestamp: "14:26",
    isHighlighted: false,
  },
  {
    id: "m8",
    userId: "u8",
    nickname: "@policy_wonk",
    content: "언론중재법 개정안에 AI 생성 콘텐츠 관련 조항이 필요합니다. 법적 공백이 너무 커요.",
    timestamp: "14:27",
    isHighlighted: false,
  },
]

export const speakingQueue = [
  { id: "sq1", nickname: "@quantum_logic", order: 1 },
  { id: "sq2", nickname: "@dream_catcher", order: 2 },
  { id: "sq3", nickname: "@neon_wave_kr", order: 3 },
  { id: "sq4", nickname: "@open_mind_99", order: 4 },
]

export type SpeechRecord = {
  id: string
  speakerOrder: number
  nickname: string
  initials: string
  content: string
  agreedCount: number
  durationSec: number
  endedAt: string
  isCurrent: boolean
}

export const mockSpeechHistory: SpeechRecord[] = [
  {
    id: "sp1",
    speakerOrder: 1,
    nickname: "@media_critic",
    initials: "MC",
    content:
      "AI 생성 뉴스에 대한 라벨링 의무화는 당장 가능한 현실적인 첫 번째 조치입니다. 구글과 메타는 이미 기술력이 있으므로, 빅테크가 먼저 자율 규제를 시작해야 합니다.",
    agreedCount: 82,
    durationSec: 98,
    endedAt: "14:21",
    isCurrent: false,
  },
  {
    id: "sp2",
    speakerOrder: 2,
    nickname: "@prof_kim",
    initials: "PK",
    content:
      "할루시네이션 문제가 핵심입니다. AI는 현재 존재하지 않는 그럴듯한 사실을 만들어내는데, 독자가 이를 구분할 수 없다면 신뢰 체계 자체가 무너집니다. 교육 시스템 개편이 선행되어야 합니다.",
    agreedCount: 114,
    durationSec: 115,
    endedAt: "14:23",
    isCurrent: false,
  },
  {
    id: "sp3",
    speakerOrder: 3,
    nickname: "@logic_hunter",
    initials: "LH",
    content:
      "AI 뉴스를 신뢰하는 선택은 독자 개인의 역량입니다. 하지만 검증 구조를 통해 신뢰할 수 있는 AI는 언론사가 검증해야 합니다.",
    agreedCount: 47,
    durationSec: 0,
    endedAt: "",
    isCurrent: true,
  },
]
