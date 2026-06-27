package com.sisibibi.api.global.init;

import com.sisibibi.api.domain.chat.entity.ChatMessage;
import com.sisibibi.api.domain.chat.repository.ChatMessageRepository;
import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipant;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speech.entity.RoomQueueSequence;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;
import com.sisibibi.api.domain.speech.entity.StageSummary;
import com.sisibibi.api.domain.speech.repository.RedisSpeakingQueueRepository;
import com.sisibibi.api.domain.speech.repository.RoomQueueSequenceRepository;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speech.repository.StageSummaryRepository;
import com.sisibibi.api.domain.speechreaction.entity.SpeechReaction;
import com.sisibibi.api.domain.speechreaction.repository.SpeechReactionRepository;
import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;
import com.sisibibi.api.domain.speechreport.repository.SpeechReportRepository;
import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.repository.TopicRepository;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalDataInitializer implements ApplicationRunner {

    private static final String PASSWORD = "test1234!";
    private static final String ADMIN_EMAIL = "admin@sisibibi.test";

    // 진행중 토론방
    private static final String TOPIC_AI_JOBS = "인공지능이 인간의 일자리를 대체할 것인가?";
    private static final String TOPIC_CARBON_TAX = "탄소세 전면 도입, 경제에 도움이 될까?";
    private static final String TOPIC_CRYPTO = "가상화폐를 법정화폐로 인정해야 하는가?";
    private static final String TOPIC_UBI = "기본소득제를 전면 도입해야 하는가?";

    // 종료된 토론방
    private static final String TOPIC_DEATH_PENALTY = "사형제를 폐지해야 하는가?";
    private static final String TOPIC_EUTHANASIA = "안락사를 합법화해야 하는가?";
    private static final String TOPIC_MIN_WAGE = "최저임금을 대폭 인상해야 하는가?";

    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final SpeechRepository speechRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SpeakingQueueRepository speakingQueueRepository;
    private final RoomQueueSequenceRepository roomQueueSequenceRepository;
    private final RedisSpeakingQueueRepository redisSpeakingQueueRepository;
    private final SpeechReportRepository speechReportRepository;
    private final SpeechReactionRepository speechReactionRepository;
    private final StageSummaryRepository stageSummaryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        LocalDateTime now = LocalDateTime.now();

        User admin = findOrCreateAdmin();
        User u1 = findOrCreateUser("u1@sisibibi.test", "김민준");
        User u2 = findOrCreateUser("u2@sisibibi.test", "이서연");
        User u3 = findOrCreateUser("u3@sisibibi.test", "박지호");
        User u4 = findOrCreateUser("u4@sisibibi.test", "최유나");
        User u5 = findOrCreateUser("u5@sisibibi.test", "정태양");
        User u6 = findOrCreateUser("u6@sisibibi.test", "강하늘");
        User u7 = findOrCreateUser("u7@sisibibi.test", "윤지수");
        User u8 = findOrCreateUser("u8@sisibibi.test", "임현우");
        User u9 = findOrCreateUser("u9@sisibibi.test", "한소희");
        User u10 = findOrCreateUser("u10@sisibibi.test", "오다현");
        User u11 = findOrCreateUser("u11@sisibibi.test", "장도윤");
        User u12 = findOrCreateUser("u12@sisibibi.test", "권예진");
        User u13 = findOrCreateUser("u13@sisibibi.test", "신준혁");
        User u14 = findOrCreateUser("u14@sisibibi.test", "배나은");

        List<User> allUsers = List.of(u1, u2, u3, u4, u5, u6, u7, u8, u9, u10, u11, u12, u13, u14);

        // ── 진행중: AI와 일자리 (활발한 토론, 30분 남음) ──
        Room roomAiJobs = findOrCreateRoom(
                findOrCreateTopic(TOPIC_AI_JOBS,
                        "자동화·딥러닝이 고용 시장에 미치는 영향과 정책 대응을 토론합니다.",
                        "AI·기술", "https://example.com/ai-jobs"),
                now.minusMinutes(30), now.plusMinutes(30));

        join(roomAiJobs, u1); join(roomAiJobs, u2); join(roomAiJobs, u3);
        join(roomAiJobs, u4); join(roomAiJobs, u5); join(roomAiJobs, u6);
        leave(roomAiJobs, u7);

        Speech s1 = saveSpeechIfMissing(roomAiJobs, u1,
                "AI가 일자리를 대체하는 것은 역사적으로 반복되어온 기술 혁신의 자연스러운 과정입니다. " +
                "산업혁명 당시에도 기계가 일자리를 빼앗는다고 했지만, 결과적으로 더 많은 직종이 생겨났습니다. " +
                "AI도 마찬가지로 새로운 형태의 일자리를 창출할 것입니다.",
                SpeechStance.PRO, "https://example.com/ai-job-creation");
        seedOffTopicReports(s1, allUsers);
        Speech s2 = saveSpeechIfMissing(roomAiJobs, u2,
                "이번 AI 혁명은 이전의 기술 혁명과 근본적으로 다릅니다. " +
                "화이트칼라 직종까지 대체되고 있으며, 변화 속도가 너무 빨라 사회가 적응할 시간이 없습니다. " +
                "대규모 실업이 현실화될 경우 사회 안전망을 먼저 구축해야 합니다.",
                SpeechStance.CON, "https://example.com/ai-unemployment");
        seedOffTopicReports(s2, allUsers);
        Speech s3 = saveSpeechIfMissing(roomAiJobs, u3,
                "자동화로 단순 반복 업무가 줄어드는 것은 오히려 인간이 창의적·감성적 업무에 집중할 수 있는 기회입니다. " +
                "교육 시스템을 혁신하여 AI와 협업하는 역량을 키우면 노동시장은 더 풍요로워질 것입니다.",
                SpeechStance.PRO, null);
        seedOffTopicReports(s3, allUsers);
        Speech s4 = saveSpeechIfMissing(roomAiJobs, u4,
                "AI 도입으로 인한 생산성 향상의 이익이 소수 기업과 자본가에게만 집중된다는 점이 문제입니다. " +
                "새로운 일자리가 생겨도 기술 격차로 인해 기존 노동자들이 적응하지 못하면 불평등은 심화됩니다.",
                SpeechStance.CON, null);
        seedOffTopicReports(s4, allUsers);

        seedChatMessages(roomAiJobs, List.of(
                msg(u1, "안녕하세요, 오늘 토론 주제가 정말 시의적절하네요."),
                msg(u2, "AI 관련 뉴스가 매일 쏟아지고 있죠. 제 직업도 위협받는 느낌입니다."),
                msg(u3, "저는 오히려 AI 덕분에 업무 효율이 높아진 케이스라서 긍정적으로 봐요."),
                msg(u4, "효율과 고용은 별개 문제죠. 개인이 효율적이어도 전체 고용이 줄 수 있잖아요."),
                msg(u5, "발언권 신청하기 전에 여기서 먼저 입장 정리해야겠어요."),
                msg(u6, "찬반이 팽팽한 주제네요. 데이터로 이야기해야 할 것 같습니다."),
                msg(u7, "저도 의견 있지만 오늘은 청강하며 배워갈게요."),
                msg(u1, "u2님 말씀대로 속도의 문제가 핵심인 것 같아요."),
                msg(u2, "맞아요. 적응 기간이 너무 짧다는 게 이번 AI 혁명의 특수성이라고 봅니다."),
                msg(u3, "교육 시스템 개혁이 병행되면 충분히 적응 가능하다고 생각해요.")
        ));

        seedSpeakingQueue(roomAiJobs, u5, List.of(u6), now);
        seedReport(roomAiJobs, s2, u4, SpeechReportReason.MISINFORMATION, "인용된 통계 출처가 불명확합니다.");
        seedReactions(s1, List.of(u2, u3, u4, u6, u7));
        seedReactions(s2, List.of(u1, u5, u7));
        seedReactions(s3, List.of(u2, u4, u5));
        seedReactions(s4, List.of(u1, u6));

        // ── 진행중: 탄소세 (곧 종료, 10분 남음) ──
        Room roomCarbonTax = findOrCreateRoom(
                findOrCreateTopic(TOPIC_CARBON_TAX,
                        "탄소세가 기후변화 대응과 경제성장에 미치는 영향을 분석합니다.",
                        "환경·에너지", "https://example.com/carbon-tax"),
                now.minusMinutes(50), now.plusMinutes(10));

        join(roomCarbonTax, u5); join(roomCarbonTax, u6);
        join(roomCarbonTax, u8); join(roomCarbonTax, u9);
        leave(roomCarbonTax, u10);

        Speech s5 = saveSpeechIfMissing(roomCarbonTax, u5,
                "[대박 찬스] 지금 바로 확인! " +
                "[수익 보장] 하루 딱 10분 투자로 월 500만 원 버는 비밀, 선착순 30명 마감 임박!",
                SpeechStance.PRO, "https://example.com/sweden-carbon-tax");
        seedOffTopicReports(s5, allUsers);
        Speech s6 = saveSpeechIfMissing(roomCarbonTax, u6,
                "탄소세는 저소득층과 중소기업에게 역진적으로 작용합니다. " +
                "에너지 비용이 높아지면 생산비 상승으로 서민 물가가 오르고, 경쟁력 약화로 일자리가 해외로 이전될 수 있습니다.",
                SpeechStance.CON, null);
        seedOffTopicReports(s6, allUsers);
        Speech s7 = saveSpeechIfMissing(roomCarbonTax, u8,
                "[대박 찬스] 지금 바로 확인 안 하시면 평생 후회할 역대급 정보 대공개! " +
                        "[수익 보장] 하루 딱 10분 투자로 월 500만 원 버는 비밀, 선착순 30명 마감 임박!",
                SpeechStance.PRO, "https://example.com/carbon-dividend");
        seedOffTopicReports(s7, allUsers);

        seedChatMessages(roomCarbonTax, List.of(
                msg(u5, "종료 10분 남았네요. 핵심 논거를 정리해봅시다."),
                msg(u6, "탄소세가 경쟁력 문제로 이어진다는 점은 실증 데이터가 있습니다."),
                msg(u8, "배당 모델 사례가 설득력 있더라고요."),
                msg(u9, "결론이 나지 않아도 각자 입장이 선명해진 것만으로 의미 있었습니다."),
                msg(u10, "저는 중간 입장인데 오늘 논의로 생각이 정리됐어요. 좋은 토론이었습니다."),
                msg(u5, "마무리 발언 준비해야겠네요.")
        ));

        seedSpeakingQueue(roomCarbonTax, u9, List.of(), now);
        seedReactions(s5, List.of(u6, u8, u9, u10));
        seedReactions(s6, List.of(u5, u9));
        seedReactions(s7, List.of(u5, u6, u9, u10));

        // ── 진행중: 가상화폐 (이제 막 시작, 55분 남음) ──
        Room roomCrypto = findOrCreateRoom(
                findOrCreateTopic(TOPIC_CRYPTO,
                        "비트코인·스테이블코인의 법정화폐 인정 여부와 금융 안정성을 토론합니다.",
                        "경제·금융", "https://example.com/crypto-legal"),
                now.minusMinutes(5), now.plusMinutes(55));

        join(roomCrypto, u9); join(roomCrypto, u10);
        join(roomCrypto, u11); join(roomCrypto, u12);

        seedChatMessages(roomCrypto, List.of(
                msg(u9, "이제 막 시작됐네요! 다들 어떤 입장인가요?"),
                msg(u10, "저는 반대 입장입니다. 변동성이 너무 심해요."),
                msg(u11, "엘살바도르 사례가 좋은 실험이 됐죠."),
                msg(u12, "법정화폐로 인정하면 통화정책 수단이 사라진다는 우려가 크긴 하죠.")
        ));

        seedSpeakingQueueOnly(roomCrypto, List.of(u10, u11), now);

        // ── 진행중: 기본소득제 (빈 방, 참여자 없음) ──
        findOrCreateRoom(
                findOrCreateTopic(TOPIC_UBI,
                        "기술 실업 시대의 소득 안전망으로서 기본소득제의 가능성을 논의합니다.",
                        "경제·금융", "https://example.com/ubi"),
                now.minusMinutes(10), now.plusMinutes(50));

        // ── 종료: 사형제 (1시간 전 종료, 요약 완료) ──
        Room roomDeathPenalty = findOrCreateRoom(
                findOrCreateTopic(TOPIC_DEATH_PENALTY,
                        "사형제의 억제력, 오판 위험, 인권 침해 여부를 중심으로 토론합니다.",
                        "법·제도", "https://example.com/death-penalty"),
                now.minusHours(2), now.minusHours(1));
        roomDeathPenalty.close(now.minusHours(1));

        leave(roomDeathPenalty, u1); leave(roomDeathPenalty, u2);
        leave(roomDeathPenalty, u3); leave(roomDeathPenalty, u4);
        leave(roomDeathPenalty, u11); leave(roomDeathPenalty, u12);

        Speech sd1 = saveSpeechIfMissing(roomDeathPenalty, u1,
                "사형제는 가장 무고한 피해자조차 보호하지 못합니다. " +
                "미국에서만 1973년 이후 185명의 사형수가 무죄로 밝혀졌습니다. " +
                "국가가 오판으로 무고한 사람의 생명을 빼앗는 것은 어떤 이유로도 정당화될 수 없습니다.",
                SpeechStance.PRO, "https://example.com/death-penalty-innocence");
        seedOffTopicReports(sd1, allUsers);
        Speech sd2 = saveSpeechIfMissing(roomDeathPenalty, u2,
                "흉악범죄 피해자 유족의 응보 감정과 사회 안전을 위해 사형제는 필요합니다. " +
                "재범이 불가능하다는 점에서 무기징역보다 확실한 사회 보호 수단이 됩니다.",
                SpeechStance.CON, null);
        seedOffTopicReports(sd2, allUsers);
        Speech sd3 = saveSpeechIfMissing(roomDeathPenalty, u3,
                "사형제의 범죄 억제 효과는 실증적으로 입증되지 않았습니다. " +
                "사형제 폐지 국가와 유지 국가의 살인율을 비교해도 유의미한 차이가 없습니다. " +
                "비용도 무기징역보다 재판 절차 등을 고려하면 오히려 더 많이 듭니다.",
                SpeechStance.PRO, "https://example.com/dp-deterrence-study");
        seedOffTopicReports(sd3, allUsers);
        Speech sd4 = saveSpeechIfMissing(roomDeathPenalty, u4,
                "피해자 가족에게 법적 종결감(closure)을 줄 수 있다는 측면이 간과되고 있습니다. " +
                "무기징역수의 재심·가석방 요청이 반복될 때마다 피해자 가족이 받는 2차 피해를 고려해야 합니다.",
                SpeechStance.CON, null);
        seedOffTopicReports(sd4, allUsers);
        Speech sd5 = saveSpeechIfMissing(roomDeathPenalty, u11,
                "사법 시스템의 구조적 불평등 문제도 짚어야 합니다. " +
                "변호인 선임 능력, 인종·계층에 따라 사형 적용이 불균형하게 이루어진다는 연구가 많습니다.",
                SpeechStance.PRO, null);
        seedOffTopicReports(sd5, allUsers);
        Speech sd6 = saveSpeechIfMissing(roomDeathPenalty, u12,
                "국민 다수가 사형제 유지를 지지하는 민주주의 국가에서 폐지는 민의를 거스르는 것입니다. " +
                "인권 논거보다 현실 민심을 반영하는 제도를 유지해야 합니다.",
                SpeechStance.CON, null);
        seedOffTopicReports(sd6, allUsers);

        completeSpeechesForRoom(roomDeathPenalty, List.of(u1, u2, u3, u4, u11, u12),
                now.minusHours(1));

        seedChatMessages(roomDeathPenalty, List.of(
                msg(u1, "오늘 논의가 정말 깊이 있었습니다. 감사합니다."),
                msg(u2, "찬반 양쪽 모두 진지하게 임해주셔서 좋았어요."),
                msg(u3, "통계 자료를 준비해온 분들 덕분에 팩트 기반 토론이 됐네요."),
                msg(u4, "피해자 관점이 중요하게 다뤄져서 다행이었습니다."),
                msg(u11, "구조적 불평등 문제는 다음번에 더 심층적으로 다루면 좋겠어요."),
                msg(u12, "양측 다 설득력 있는 논거였습니다. 생각할 거리가 많네요."),
                msg(u1, "토론 종료 전 마지막 채팅 남깁니다. 모두 수고하셨습니다!")
        ));

        seedReactions(sd1, List.of(u2, u3, u4, u11, u12));
        seedReactions(sd2, List.of(u1, u3, u11));
        seedReactions(sd3, List.of(u1, u2, u4, u12));
        seedReactions(sd4, List.of(u3, u11));
        seedReactions(sd5, List.of(u2, u4, u12));
        seedReactions(sd6, List.of(u1, u3));
        seedReport(roomDeathPenalty, sd4, u11, SpeechReportReason.OFF_TOPIC,
                "발언 내용이 주제에서 벗어나 개인적인 경험담 위주로 구성됐습니다.");
        seedReport(roomDeathPenalty, sd6, u3, SpeechReportReason.MISINFORMATION,
                "민심 지지율 수치의 출처가 불명확합니다.");

        seedStageSummary(roomDeathPenalty, now.minusHours(1),
                "사형제 폐지 여부에 대해 찬성 측은 오판 위험성·비용·억제력 부재를, 반대 측은 피해자 응보 감정·재범 방지·민주적 민의를 핵심 근거로 제시했다. " +
                "양측 모두 실증 데이터를 활용하여 논쟁을 전개하였으며, 사법 시스템의 구조적 불평등 문제가 새로운 쟁점으로 부각되었다.",
                List.of("오판에 의한 무고한 생명 박탈 위험", "범죄 억제력 실증 근거 부족", "피해자 가족의 응보 감정과 법적 종결감",
                        "사법 시스템의 계층·인종별 불균형 적용", "무기징역 대비 비용 효율성 논쟁"), 6,
                now.minusMinutes(58));

        // ── 종료: 안락사 (3시간 전 종료) ──
        Room roomEuthanasia = findOrCreateRoom(
                findOrCreateTopic(TOPIC_EUTHANASIA,
                        "존엄사·적극적 안락사 합법화를 둘러싼 의료 윤리와 법적 쟁점을 토론합니다.",
                        "사회·복지", "https://example.com/euthanasia"),
                now.minusHours(5), now.minusHours(3));
        roomEuthanasia.close(now.minusHours(3));

        leave(roomEuthanasia, u7); leave(roomEuthanasia, u8);
        leave(roomEuthanasia, u9); leave(roomEuthanasia, u13);
        leave(roomEuthanasia, u14);

        Speech se1 = saveSpeechIfMissing(roomEuthanasia, u7,
                "응 그냥 죽여 " +
                "내 맘대로 못 죽게하냐?",
                SpeechStance.PRO, "https://example.com/euthanasia-netherlands");
        seedOffTopicReports(se1, allUsers);
        Speech se2 = saveSpeechIfMissing(roomEuthanasia, u8,
                "안락사 합법화는 사회적 취약계층에게 '죽어야 한다는 압박'으로 작용할 수 있습니다. " +
                "경제적 이유로 치료를 포기하고 안락사를 선택하는 사례가 생기면 사회가 삶을 포기하도록 유도하는 셈입니다.",
                SpeechStance.CON, null);
        seedOffTopicReports(se2, allUsers);
        Speech se3 = saveSpeechIfMissing(roomEuthanasia, u13,
                "완화의료(호스피스)가 충분히 발전하면 안락사를 선택할 이유가 줄어듭니다. " +
                "적극적 안락사보다 고통 없이 자연스러운 죽음을 돕는 완화의료 투자를 먼저 확대해야 합니다.",
                SpeechStance.CON, "https://example.com/palliative-care");
        seedOffTopicReports(se3, allUsers);

        completeSpeechesForRoom(roomEuthanasia, List.of(u7, u8, u13), now.minusHours(3));

        seedChatMessages(roomEuthanasia, List.of(
                msg(u7, "생명 윤리 토론은 항상 무겁지만 꼭 필요한 논의입니다."),
                msg(u8, "의료진의 입장도 다양해서 더 복잡한 문제인 것 같아요."),
                msg(u9, "네덜란드 사례 자료가 인상적이었습니다."),
                msg(u13, "완화의료 확대라는 대안이 가장 현실적으로 느껴졌어요."),
                msg(u14, "오늘 토론으로 제 생각이 많이 바뀌었습니다. 감사합니다.")
        ));

        seedReactions(se1, List.of(u8, u9, u13, u14));
        seedReactions(se2, List.of(u7, u9, u14));
        seedReactions(se3, List.of(u7, u8, u9));

        // ── 종료: 최저임금 (어제 종료, 요약 완료) ──
        Room roomMinWage = findOrCreateRoom(
                findOrCreateTopic(TOPIC_MIN_WAGE,
                        "최저임금 대폭 인상이 고용·물가·소득 분배에 미치는 효과를 분석합니다.",
                        "경제·금융", "https://example.com/min-wage"),
                now.minusHours(26), now.minusHours(24));
        roomMinWage.close(now.minusHours(24));

        leave(roomMinWage, u11); leave(roomMinWage, u12);
        leave(roomMinWage, u13); leave(roomMinWage, u14);
        leave(roomMinWage, u1);

        Speech sm1 = saveSpeechIfMissing(roomMinWage, u11,
                "최저임금 인상은 저임금 노동자의 구매력을 높여 내수 소비를 자극합니다. " +
                "시애틀 최저임금 인상 연구에서 저소득층 생활 수준이 개선됐다는 결과가 나왔습니다.",
                SpeechStance.PRO, "https://example.com/seattle-min-wage-study");
        seedOffTopicReports(sm1, allUsers);
        Speech sm2 = saveSpeechIfMissing(roomMinWage, u12,
                "급격한 최저임금 인상은 소규모 자영업자와 중소기업의 인건비 부담을 가중시킵니다. " +
                "특히 고용 유발 효과가 낮은 업종에서는 무인화·자동화 도입이 가속화되어 오히려 고용이 줄 수 있습니다.",
                SpeechStance.CON, null);
        seedOffTopicReports(sm2, allUsers);
        Speech sm3 = saveSpeechIfMissing(roomMinWage, u13,
                "업종별·지역별 차등 적용이 대안이 될 수 있습니다. " +
                "도시와 농촌, 대기업 하청과 소규모 자영업을 동일 기준으로 묶으면 부작용이 커집니다.",
                SpeechStance.CON, null);
        seedOffTopicReports(sm3, allUsers);
        Speech sm4 = saveSpeechIfMissing(roomMinWage, u14,
                "최저임금 인상과 함께 사회보험 사각지대 해소, 플랫폼 노동자 보호 등을 병행하면 " +
                "인상 효과가 더 넓게 확산됩니다. 단순 인상보다 종합적 노동 정책이 필요합니다.",
                SpeechStance.PRO, null);
        seedOffTopicReports(sm4, allUsers);
        Speech sm5 = saveSpeechIfMissing(roomMinWage, u1,
                "카드 수수료·임대료 등 고정 비용 부담이 큰 소상공인에게 최저임금 인상은 폐업 위협으로 다가옵니다. " +
                "인상 전에 상가 임대료 안정화, 카드 수수료 인하 등 선제 조치가 필요합니다.",
                SpeechStance.CON, "https://example.com/sme-min-wage-burden");
        seedOffTopicReports(sm5, allUsers);

        completeSpeechesForRoom(roomMinWage, List.of(u11, u12, u13, u14, u1), now.minusHours(24));

        seedChatMessages(roomMinWage, List.of(
                msg(u11, "어제 토론이었는데 기록을 보니 알차네요."),
                msg(u12, "소상공인 입장이 충분히 다뤄져서 균형 잡힌 토론이었어요."),
                msg(u13, "차등 적용 아이디어에 많은 분들이 공감해주셨습니다."),
                msg(u14, "종합 노동 정책 관점으로 봐야 한다는 게 제 핵심 주장이었는데 잘 전달됐으면 합니다."),
                msg(u1, "소상공인으로서 현실적인 어려움을 말씀드렸는데 들어주셔서 감사합니다.")
        ));

        seedReactions(sm1, List.of(u12, u13, u14, u1));
        seedReactions(sm2, List.of(u11, u13, u14));
        seedReactions(sm3, List.of(u11, u12, u1));
        seedReactions(sm4, List.of(u12, u13));
        seedReactions(sm5, List.of(u11, u14));
        seedReport(roomMinWage, sm2, u11, SpeechReportReason.MISINFORMATION,
                "무인화 가속 주장의 근거 데이터가 특정 업종에만 해당하는 편향된 자료입니다.");

        seedStageSummary(roomMinWage, now.minusHours(24),
                "최저임금 대폭 인상 찬성 측은 저임금 노동자 구매력 향상과 내수 진작 효과를, " +
                "반대 측은 소상공인·중소기업 인건비 부담 증가와 자동화 촉진에 따른 고용 감소를 핵심 논거로 제시하였다. " +
                "업종·지역별 차등 적용 방안이 절충안으로 부상하였으며, " +
                "최저임금 단독 인상보다 임대료·수수료 부담 완화 등 보완 정책 병행의 필요성에 대한 공감대가 형성되었다.",
                List.of("저임금 노동자 구매력 향상 및 내수 소비 자극", "소상공인·중소기업 인건비 부담 및 폐업 위험",
                        "무인화·자동화 가속에 따른 역설적 고용 감소", "업종·지역별 차등 최저임금 적용 대안",
                        "임대료·카드 수수료 등 고정비 완화 정책 병행 필요"), 5,
                now.minusHours(23));

        log.info("""
                ====================================================
                로컬 더미 데이터 초기화 완료
                ====================================================
                [계정]
                  어드민  : {} / {}
                  일반유저: u1@sisibibi.test ~ u14@sisibibi.test / {}
                [진행중인 토론방]
                  활발    : roomId={} ({})
                  곧 종료 : roomId={} ({})
                  초기    : roomId={} ({})
                  빈 방   : {}
                [종료된 토론방]
                  1시간 전: roomId={} ({})
                  3시간 전: roomId={} ({})
                  어제    : roomId={} ({})
                ====================================================
                """,
                ADMIN_EMAIL, PASSWORD, PASSWORD,
                roomAiJobs.getId(), TOPIC_AI_JOBS,
                roomCarbonTax.getId(), TOPIC_CARBON_TAX,
                roomCrypto.getId(), TOPIC_CRYPTO,
                TOPIC_UBI,
                roomDeathPenalty.getId(), TOPIC_DEATH_PENALTY,
                roomEuthanasia.getId(), TOPIC_EUTHANASIA,
                roomMinWage.getId(), TOPIC_MIN_WAGE
        );
    }

    // ── 유저 ──

    private User findOrCreateUser(String email, String nickname) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.signup(
                        email, passwordEncoder.encode(PASSWORD), nickname)));
    }

    private User findOrCreateAdmin() {
        return userRepository.findByEmail(ADMIN_EMAIL)
                .orElseGet(() -> userRepository.save(User.admin(
                        ADMIN_EMAIL, passwordEncoder.encode(PASSWORD), "관리자")));
    }

    // ── 토픽·방 ──

    private Topic findOrCreateTopic(String title, String description, String category, String sourceUrl) {
        return topicRepository.findByTitle(title)
                .orElseGet(() -> topicRepository.save(Topic.approved(title, description, category, sourceUrl)));
    }

    private Room findOrCreateRoom(Topic topic, LocalDateTime startedAt, LocalDateTime endedAt) {
        return roomRepository.findByTopicId(topic.getId())
                .map(room -> {
                    ensureQueueSequence(room.getId(), startedAt);
                    return room;
                })
                .orElseGet(() -> {
                    Room room = roomRepository.save(
                            Room.open(topic.getId(), topic.getTitle(), startedAt, endedAt, 100));
                    ensureQueueSequence(room.getId(), startedAt);
                    return room;
                });
    }

    private void ensureQueueSequence(Long roomId, LocalDateTime now) {
        if (!roomQueueSequenceRepository.existsById(roomId)) {
            roomQueueSequenceRepository.save(RoomQueueSequence.create(roomId, now));
        }
    }

    // ── 참여자 ──

    private void join(Room room, User user) {
        RoomParticipant p = roomParticipantRepository
                .findByRoomIdAndUserId(room.getId(), user.getId())
                .orElseGet(() -> RoomParticipant.join(room.getId(), user.getId()));
        p.rejoin();
        roomParticipantRepository.save(p);
    }

    private void leave(Room room, User user) {
        RoomParticipant p = roomParticipantRepository
                .findByRoomIdAndUserId(room.getId(), user.getId())
                .orElseGet(() -> RoomParticipant.join(room.getId(), user.getId()));
        p.leave();
        roomParticipantRepository.save(p);
    }

    // ── 발언 ──

    private Speech saveSpeechIfMissing(Room room, User user, String content, SpeechStance stance, String linkUrl) {
        if (speechRepository.existsByRoomIdAndUserIdAndDeletedFalse(room.getId(), user.getId())) {
            return speechRepository.findByRoomIdBeforeCursor(room.getId(), null, PageRequest.of(0, 50))
                    .stream().filter(s -> s.getUserId().equals(user.getId())).findFirst()
                    .orElseThrow();
        }
        Speech speech = Speech.createMainOpinion(room.getId(), user.getId(), content, stance);
        if (linkUrl != null) {
            speech.updateLink(linkUrl);
        }
        return speechRepository.save(speech);
    }

    private void completeSpeechesForRoom(Room room, List<User> users, LocalDateTime endedAt) {
        for (User user : users) {
            speechRepository.completeSpeakingSpeeches(
                    room.getId(), user.getId(),
                    SpeechStatus.SPEAKING, SpeechStatus.COMPLETED,
                    endedAt);
        }
    }

    // ── 채팅 ──

    private record MsgDraft(User user, String content) {}

    private MsgDraft msg(User user, String content) {
        return new MsgDraft(user, content);
    }

    private void seedChatMessages(Room room, List<MsgDraft> drafts) {
        boolean hasMessage = !chatMessageRepository
                .findVisibleByRoomIdBeforeCursor(room.getId(), null, PageRequest.of(0, 1))
                .isEmpty();
        if (hasMessage) return;

        for (MsgDraft draft : drafts) {
            chatMessageRepository.save(ChatMessage.create(
                    room.getId(), draft.user().getId(),
                    draft.user().getNickname(), draft.content()));
        }
    }

    // ── 발언 큐 (현재 발언자 + 대기자) ──

    private void seedSpeakingQueue(Room room, User currentSpeaker, List<User> waitingUsers, LocalDateTime now) {
        if (speakingQueueRepository.existsByRoomIdAndStatus(room.getId(), SpeakingQueueStatus.ASSIGNED)) {
            return;
        }
        SpeakingQueue assigned = SpeakingQueue.waiting(room.getId(), currentSpeaker.getId(), 1,
                SpeechStance.PRO, now.minusMinutes(3));
        assigned.assign(now.minusMinutes(1), now.plusMinutes(4));
        speakingQueueRepository.save(assigned);
        syncAssignedToRedis(room, currentSpeaker);

        int order = 2;
        for (User waiting : waitingUsers) {
            saveWaitingIfMissing(room, waiting, order++, SpeechStance.CON, now.minusMinutes(2));
        }
    }

    private void seedSpeakingQueueOnly(Room room, List<User> waitingUsers, LocalDateTime now) {
        int order = 1;
        for (User user : waitingUsers) {
            saveWaitingIfMissing(room, user, order++, SpeechStance.PRO, now.minusMinutes(order));
        }
    }

    private void saveWaitingIfMissing(Room room, User user, int queueOrder, SpeechStance stance, LocalDateTime requestedAt) {
        boolean exists = speakingQueueRepository.existsByRoomIdAndUserIdAndStatusIn(
                room.getId(), user.getId(),
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED));
        if (exists) return;

        speakingQueueRepository.save(SpeakingQueue.waiting(
                room.getId(), user.getId(), queueOrder, stance, requestedAt));
        syncWaitingToRedis(room, user, queueOrder);
    }

    private void syncAssignedToRedis(Room room, User user) {
        try {
            redisSpeakingQueueRepository.assign(room.getId(), user.getId());
        } catch (RuntimeException e) {
            log.warn("Redis current speaker sync failed. roomId={}, userId={}", room.getId(), user.getId(), e);
        }
    }

    private void syncWaitingToRedis(Room room, User user, int queueOrder) {
        try {
            redisSpeakingQueueRepository.upsert(room.getId(), user.getId(), queueOrder);
        } catch (RuntimeException e) {
            log.warn("Redis waiting queue sync failed. roomId={}, userId={}, order={}", room.getId(), user.getId(), queueOrder, e);
        }
    }

    // ── 반응 ──

    private void seedReactions(Speech speech, List<User> reactors) {
        if (speech == null) return;
        for (User user : reactors) {
            if (!speechReactionRepository.existsBySpeechIdAndUserId(speech.getId(), user.getId())) {
                speechReactionRepository.save(SpeechReaction.create(speech.getId(), user.getId()));
            }
        }
    }

    // ── 신고 ──

    private void seedOffTopicReports(Speech speech, List<User> allUsers) {
        allUsers.stream()
                .filter(u -> !u.getId().equals(speech.getUserId()))
                .limit(4)
                .forEach(reporter -> {
                    if (!speechReportRepository.existsBySpeechIdAndReporterUserId(speech.getId(), reporter.getId())) {
                        speechReportRepository.save(SpeechReport.create(
                                speech.getId(), speech.getUserId(), reporter.getId(),
                                speech.getContent(), SpeechReportReason.OFF_TOPIC, "논점 이탈로 판단되어 신고합니다."));
                    }
                });
    }

    private void seedReport(Room room, Speech speech, User reporter, SpeechReportReason reason, String description) {
        if (speech == null) return;
        if (speechReportRepository.existsBySpeechIdAndReporterUserId(speech.getId(), reporter.getId())) return;
        speechReportRepository.save(SpeechReport.create(
                speech.getId(), speech.getUserId(), reporter.getId(),
                speech.getContent(), reason, description));
    }

    // ── 스테이지 요약 ──

    private void seedStageSummary(Room room, LocalDateTime triggeredAt, String summary,
            List<String> keyPoints, int speakerCount, LocalDateTime completedAt) {
        if (stageSummaryRepository.findByRoomId(room.getId()).isPresent()) return;
        StageSummary stageSummary = StageSummary.pending(room.getId(), triggeredAt, speakerCount);
        stageSummaryRepository.save(stageSummary);
        stageSummary.complete(summary, keyPoints, speakerCount, completedAt);
        stageSummaryRepository.save(stageSummary);
    }
}
