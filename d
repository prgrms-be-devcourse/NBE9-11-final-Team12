[1mdiff --git a/backend/src/main/java/com/sisibibi/api/global/init/LocalDataInitializer.java b/backend/src/main/java/com/sisibibi/api/global/init/LocalDataInitializer.java[m
[1mindex 685332a0..a1718638 100644[m
[1m--- a/backend/src/main/java/com/sisibibi/api/global/init/LocalDataInitializer.java[m
[1m+++ b/backend/src/main/java/com/sisibibi/api/global/init/LocalDataInitializer.java[m
[36m@@ -1,646 +1,646 @@[m
[31m-package com.sisibibi.api.global.init;[m
[31m-[m
[31m-import com.sisibibi.api.domain.chat.entity.ChatMessage;[m
[31m-import com.sisibibi.api.domain.chat.repository.ChatMessageRepository;[m
[31m-import com.sisibibi.api.domain.room.entity.Room;[m
[31m-import com.sisibibi.api.domain.room.repository.RoomRepository;[m
[31m-import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipant;[m
[31m-import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;[m
[31m-import com.sisibibi.api.domain.speech.entity.RoomQueueSequence;[m
[31m-import com.sisibibi.api.domain.speech.entity.SpeakingQueue;[m
[31m-import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;[m
[31m-import com.sisibibi.api.domain.speech.entity.Speech;[m
[31m-import com.sisibibi.api.domain.speech.entity.SpeechStance;[m
[31m-import com.sisibibi.api.domain.speech.entity.SpeechStatus;[m
[31m-import com.sisibibi.api.domain.speech.entity.StageSummary;[m
[31m-import com.sisibibi.api.domain.speech.repository.RedisSpeakingQueueRepository;[m
[31m-import com.sisibibi.api.domain.speech.repository.RoomQueueSequenceRepository;[m
[31m-import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;[m
[31m-import com.sisibibi.api.domain.speech.repository.SpeechRepository;[m
[31m-import com.sisibibi.api.domain.speech.repository.StageSummaryRepository;[m
[31m-import com.sisibibi.api.domain.speechreaction.entity.SpeechReaction;[m
[31m-import com.sisibibi.api.domain.speechreaction.repository.SpeechReactionRepository;[m
[31m-import com.sisibibi.api.domain.speechreport.entity.SpeechReport;[m
[31m-import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;[m
[31m-import com.sisibibi.api.domain.speechreport.repository.SpeechReportRepository;[m
[31m-import com.sisibibi.api.domain.topic.entity.Topic;[m
[31m-import com.sisibibi.api.domain.topic.repository.TopicRepository;[m
[31m-import com.sisibibi.api.domain.user.entity.User;[m
[31m-import com.sisibibi.api.domain.user.repository.UserRepository;[m
[31m-import java.time.LocalDateTime;[m
[31m-import java.util.List;[m
[31m-import lombok.RequiredArgsConstructor;[m
[31m-import lombok.extern.slf4j.Slf4j;[m
[31m-import org.springframework.boot.ApplicationArguments;[m
[31m-import org.springframework.boot.ApplicationRunner;[m
[31m-import org.springframework.context.annotation.Profile;[m
[31m-import org.springframework.data.domain.PageRequest;[m
[31m-import org.springframework.security.crypto.password.PasswordEncoder;[m
[31m-import org.springframework.stereotype.Component;[m
[31m-import org.springframework.transaction.annotation.Transactional;[m
[31m-[m
[31m-@Slf4j[m
[31m-@Component[m
[31m-@RequiredArgsConstructor[m
[31m-public class LocalDataInitializer implements ApplicationRunner {[m
[31m-[m
[31m-    private static final String PASSWORD = "test1234!";[m
[31m-    private static final String ADMIN_EMAIL = "admin@sisibibi.test";[m
[31m-[m
[31m-    // 진행중 토론방[m
[31m-    private static final String TOPIC_AI_JOBS = "인공지능이 인간의 일자리를 대체할 것인가?";[m
[31m-    private static final String TOPIC_CARBON_TAX = "탄소세 전면 도입, 경제에 도움이 될까?";[m
[31m-    private static final String TOPIC_CRYPTO = "가상화폐를 법정화폐로 인정해야 하는가?";[m
[31m-    private static final String TOPIC_UBI = "기본소득제를 전면 도입해야 하는가?";[m
[31m-    private static final String TOPIC_CLEAN_STAGE_TEST = "도심 내 개인형 이동장치 규제를 강화해야 하는가?";[m
[31m-[m
[31m-    // 종료된 토론방[m
[31m-    private static final String TOPIC_DEATH_PENALTY = "사형제를 폐지해야 하는가?";[m
[31m-    private static final String TOPIC_EUTHANASIA = "안락사를 합법화해야 하는가?";[m
[31m-    private static final String TOPIC_MIN_WAGE = "최저임금을 대폭 인상해야 하는가?";[m
[31m-[m
[31m-    private final UserRepository userRepository;[m
[31m-    private final TopicRepository topicRepository;[m
[31m-    private final RoomRepository roomRepository;[m
[31m-    private final RoomParticipantRepository roomParticipantRepository;[m
[31m-    private final SpeechRepository speechRepository;[m
[31m-    private final ChatMessageRepository chatMessageRepository;[m
[31m-    private final SpeakingQueueRepository speakingQueueRepository;[m
[31m-    private final RoomQueueSequenceRepository roomQueueSequenceRepository;[m
[31m-    private final RedisSpeakingQueueRepository redisSpeakingQueueRepository;[m
[31m-    private final SpeechReportRepository speechReportRepository;[m
[31m-    private final SpeechReactionRepository speechReactionRepository;[m
[31m-    private final StageSummaryRepository stageSummaryRepository;[m
[31m-    private final PasswordEncoder passwordEncoder;[m
[31m-[m
[31m-    @Override[m
[31m-    @Transactional[m
[31m-    public void run(ApplicationArguments args) {[m
[31m-        LocalDateTime now = LocalDateTime.now();[m
[31m-[m
[31m-        User admin = findOrCreateAdmin();[m
[31m-        User u1 = findOrCreateUser("u1@sisibibi.test", "김민준");[m
[31m-        User u2 = findOrCreateUser("u2@sisibibi.test", "이서연");[m
[31m-        User u3 = findOrCreateUser("u3@sisibibi.test", "박지호");[m
[31m-        User u4 = findOrCreateUser("u4@sisibibi.test", "최유나");[m
[31m-        User u5 = findOrCreateUser("u5@sisibibi.test", "정태양");[m
[31m-        User u6 = findOrCreateUser("u6@sisibibi.test", "강하늘");[m
[31m-        User u7 = findOrCreateUser("u7@sisibibi.test", "윤지수");[m
[31m-        User u8 = findOrCreateUser("u8@sisibibi.test", "임현우");[m
[31m-        User u9 = findOrCreateUser("u9@sisibibi.test", "한소희");[m
[31m-        User u10 = findOrCreateUser("u10@sisibibi.test", "오다현");[m
[31m-        User u11 = findOrCreateUser("u11@sisibibi.test", "장도윤");[m
[31m-        User u12 = findOrCreateUser("u12@sisibibi.test", "권예진");[m
[31m-        User u13 = findOrCreateUser("u13@sisibibi.test", "신준혁");[m
[31m-        User u14 = findOrCreateUser("u14@sisibibi.test", "배나은");[m
[31m-[m
[31m-        List<User> allUsers = List.of(u1, u2, u3, u4, u5, u6, u7, u8, u9, u10, u11, u12, u13, u14);[m
[31m-[m
[31m-        // ── 진행중: AI와 일자리 (활발한 토론, 1분 남음) ──[m
[31m-        Room roomAiJobs = findOrCreateRoom([m
[31m-                findOrCreateTopic(TOPIC_AI_JOBS,[m
[31m-                        "자동화·딥러닝이 고용 시장에 미치는 영향과 정책 대응을 토론합니다.",[m
[31m-                        "AI·기술", "https://example.com/ai-jobs"),[m
[31m-                now.minusMinutes(30), now.plusSeconds(30));[m
[31m-[m
[31m-        join(roomAiJobs, u1); join(roomAiJobs, u2); join(roomAiJobs, u3);[m
[31m-        join(roomAiJobs, u4); join(roomAiJobs, u5); join(roomAiJobs, u6);[m
[31m-        leave(roomAiJobs, u7);[m
[31m-[m
[31m-        Speech s1 = saveSpeechIfMissing(roomAiJobs, u1,[m
[31m-                "AI가 일자리를 대체하는 것은 역사적으로 반복되어온 기술 혁신의 자연스러운 과정입니다. " +[m
[31m-                "산업혁명 당시에도 기계가 일자리를 빼앗는다고 했지만, 결과적으로 더 많은 직종이 생겨났습니다. " +[m
[31m-                "AI도 마찬가지로 새로운 형태의 일자리를 창출할 것입니다.",[m
[31m-                SpeechStance.PRO, "https://example.com/ai-job-creation");[m
[31m-        seedOffTopicReports(s1, allUsers);[m
[31m-        Speech s2 = saveSpeechIfMissing(roomAiJobs, u2,[m
[31m-                "이번 AI 혁명은 이전의 기술 혁명과 근본적으로 다릅니다. " +[m
[31m-                "화이트칼라 직종까지 대체되고 있으며, 변화 속도가 너무 빨라 사회가 적응할 시간이 없습니다. " +[m
[31m-                "대규모 실업이 현실화될 경우 사회 안전망을 먼저 구축해야 합니다.",[m
[31m-                SpeechStance.CON, "https://example.com/ai-unemployment");[m
[31m-        seedOffTopicReports(s2, allUsers);[m
[31m-        Speech s3 = saveSpeechIfMissing(roomAiJobs, u3,[m
[31m-                "자동화로 단순 반복 업무가 줄어드는 것은 오히려 인간이 창의적·감성적 업무에 집중할 수 있는 기회입니다. " +[m
[31m-                "교육 시스템을 혁신하여 AI와 협업하는 역량을 키우면 노동시장은 더 풍요로워질 것입니다.",[m
[31m-                SpeechStance.PRO, null);[m
[31m-        seedOffTopicReports(s3, allUsers);[m
[31m-        Speech s4 = saveSpeechIfMissing(roomAiJobs, u4,[m
[31m-                "AI 도입으로 인한 생산성 향상의 이익이 소수 기업과 자본가에게만 집중된다는 점이 문제입니다. " +[m
[31m-                "새로운 일자리가 생겨도 기술 격차로 인해 기존 노동자들이 적응하지 못하면 불평등은 심화됩니다.",[m
[31m-                SpeechStance.CON, null);[m
[31m-        seedOffTopicReports(s4, allUsers);[m
[31m-[m
[31m-        seedChatMessages(roomAiJobs, List.of([m
[31m-                msg(u1, "안녕하세요, 오늘 토론 주제가 정말 시의적절하네요."),[m
[31m-                msg(u2, "AI 관련 뉴스가 매일 쏟아지고 있죠. 제 직업도 위협받는 느낌입니다."),[m
[31m-                msg(u3, "저는 오히려 AI 덕분에 업무 효율이 높아진 케이스라서 긍정적으로 봐요."),[m
[31m-                msg(u4, "효율과 고용은 별개 문제죠. 개인이 효율적이어도 전체 고용이 줄 수 있잖아요."),[m
[31m-                msg(u5, "발언권 신청하기 전에 여기서 먼저 입장 정리해야겠어요."),[m
[31m-                msg(u6, "찬반이 팽팽한 주제네요. 데이터로 이야기해야 할 것 같습니다."),[m
[31m-                msg(u7, "저도 의견 있지만 오늘은 청강하며 배워갈게요."),[m
[31m-                msg(u1, "u2님 말씀대로 속도의 문제가 핵심인 것 같아요."),[m
[31m-                msg(u2, "맞아요. 적응 기간이 너무 짧다는 게 이번 AI 혁명의 특수성이라고 봅니다."),[m
[31m-                msg(u3, "교육 시스템 개혁이 병행되면 충분히 적응 가능하다고 생각해요.")[m
[31m-        ));[m
[31m-[m
[31m-        seedSpeakingQueue(roomAiJobs, u5, List.of(u6), now);[m
[31m-        seedReport(roomAiJobs, s2, u4, SpeechReportReason.MISINFORMATION, "인용된 통계 출처가 불명확합니다.");[m
[31m-        completeSpeechesForRoom(roomAiJobs, List.of(u1, u2, u3, u4), now.minusMinutes(5));[m
[31m-[m
[31m-        seedReactions(s1, List.of(u2, u3, u4, u6, u7));[m
[31m-        seedReactions(s2, List.of(u1, u5, u7));[m
[31m-        seedReactions(s3, List.of(u2, u4, u5));[m
[31m-        seedReactions(s4, List.of(u1, u6));[m
[31m-[m
[31m-        // ── 진행중: 탄소세 (곧 종료, 10분 남음) ──[m
[31m-        Room roomCarbonTax = findOrCreateRoom([m
[31m-                findOrCreateTopic(TOPIC_CARBON_TAX,[m
[31m-                        "탄소세가 기후변화 대응과 경제성장에 미치는 영향을 분석합니다.",[m
[31m-                        "환경·에너지", "https://example.com/carbon-tax"),[m
[31m-                now.minusMinutes(50), now.plusMinutes(10));[m
[31m-[m
[31m-        join(roomCarbonTax, u5); join(roomCarbonTax, u6);[m
[31m-        join(roomCarbonTax, u8); join(roomCarbonTax, u9);[m
[31m-        leave(roomCarbonTax, u10);[m
[31m-[m
[31m-        Speech s5 = saveSpeechIfMissing(roomCarbonTax, u5,[m
[31m-                "[대박 찬스] 지금 바로 확인! " +[m
[31m-                "[수익 보장] 하루 딱 10분 투자로 월 500만 원 버는 비밀, 선착순 30명 마감 임박!",[m
[31m-                SpeechStance.PRO, "https://example.com/sweden-carbon-tax");[m
[31m-        seedOffTopicReports(s5, allUsers);[m
[31m-        Speech s6 = saveSpeechIfMissing(roomCarbonTax, u6,[m
[31m-                "탄소세는 저소득층과 중소기업에게 역진적으로 작용합니다. " +[m
[31m-                "에너지 비용이 높아지면 생산비 상승으로 서민 물가가 오르고, 경쟁력 약화로 일자리가 해외로 이전될 수 있습니다.",[m
[31m-                SpeechStance.CON, null);[m
[31m-        seedOffTopicReports(s6, allUsers);[m
[31m-        Speech s7 = saveSpeechIfMissing(roomCarbonTax, u8,[m
[31m-                "[대박 찬스] 지금 바로 확인 안 하시면 평생 후회할 역대급 정보 대공개! " +[m
[31m-                        "[수익 보장] 하루 딱 10분 투자로 월 500만 원 버는 비밀, 선착순 30명 마감 임박!",[m
[31m-                SpeechStance.PRO, "https://example.com/carbon-dividend");[m
[31m-        seedOffTopicReports(s7, allUsers);[m
[31m-[m
[31m-        seedChatMessages(roomCarbonTax, List.of([m
[31m-                msg(u5, "종료 10분 남았네요. 핵심 논거를 정리해봅시다."),[m
[31m-                msg(u6, "탄소세가 경쟁력 문제로 이어진다는 점은 실증 데이터가 있습니다."),[m
[31m-                msg(u8, "배당 모델 사례가 설득력 있더라고요."),[m
[31m-                msg(u9, "결론이 나지 않아도 각자 입장이 선명해진 것만으로 의미 있었습니다."),[m
[31m-                msg(u10, "저는 중간 입장인데 오늘 논의로 생각이 정리됐어요. 좋은 토론이었습니다."),[m
[31m-                msg(u5, "마무리 발언 준비해야겠네요.")[m
[31m-        ));[m
[31m-[m
[31m-        completeSpeechesForRoom(roomCarbonTax, List.of(u5, u6, u8), now.minusMinutes(2));[m
[31m-[m
[31m-        seedSpeakingQueue(roomCarbonTax, u9, List.of(), now);[m
[31m-        seedReactions(s5, List.of(u6, u8, u9, u10));[m
[31m-        seedReactions(s6, List.of(u5, u9));[m
[31m-        seedReactions(s7, List.of(u5, u6, u9, u10));[m
[31m-[m
[31m-        // ── 진행중: 가상화폐 (이제 막 시작, 55분 남음) ──[m
[31m-        Room roomCrypto = findOrCreateRoom([m
[31m-                findOrCreateTopic(TOPIC_CRYPTO,[m
[31m-                        "비트코인·스테이블코인의 법정화폐 인정 여부와 금융 안정성을 토론합니다.",[m
[31m-                        "경제·금융", "https://example.com/crypto-legal"),[m
[31m-                now.minusMinutes(5), now.plusMinutes(55));[m
[31m-[m
[31m-        join(roomCrypto, u9); join(roomCrypto, u10);[m
[31m-        join(roomCrypto, u11); join(roomCrypto, u12);[m
[31m-[m
[31m-        seedChatMessages(roomCrypto, List.of([m
[31m-                msg(u9, "이제 막 시작됐네요! 다들 어떤 입장인가요?"),[m
[31m-                msg(u10, "저는 반대 입장입니다. 변동성이 너무 심해요."),[m
[31m-                msg(u11, "엘살바도르 사례가 좋은 실험이 됐죠."),[m
[31m-                msg(u12, "법정화폐로 인정하면 통화정책 수단이 사라진다는 우려가 크긴 하죠.")[m
[31m-        ));[m
[31m-[m
[31m-        seedSpeakingQueueOnly(roomCrypto, List.of(u10, u11), now);[m
[31m-[m
[31m-        // ── 진행중: 기본소득제 (빈 방, 참여자 없음) ──[m
[31m-        findOrCreateRoom([m
[31m-                findOrCreateTopic(TOPIC_UBI,[m
[31m-                        "기술 실업 시대의 소득 안전망으로서 기본소득제의 가능성을 논의합니다.",[m
[31m-                        "경제·금융", "https://example.com/ubi"),[m
[31m-                now.minusMinutes(10), now.plusMinutes(50));[m
[31m-[m
[31m-        // ── 진행중: 개인형 이동장치 (테스트용 빈 방, 참여자 없음) ──[m
[31m-        Room roomCleanStageTest = findOrCreateRoom([m
[31m-                findOrCreateTopic(TOPIC_CLEAN_STAGE_TEST,[m
[31m-                        "전동킥보드 등 개인형 이동장치의 안전 규제 강화 여부를 토론합니다.",[m
[31m-                        "안전", "https://example.com/personal-mobility-safety"),[m
[31m-                now.minusMinutes(1), now.plusDays(7));[m
[31m-[m
[31m-        // ── 종료: 사형제 (1시간 전 종료, 요약 완료) ──[m
[31m-        Room roomDeathPenalty = findOrCreateRoom([m
[31m-                findOrCreateTopic(TOPIC_DEATH_PENALTY,[m
[31m-                        "사형제의 억제력, 오판 위험, 인권 침해 여부를 중심으로 토론합니다.",[m
[31m-                        "법·제도", "https://example.com/death-penalty"),[m
[31m-                now.minusHours(2), now.minusHours(1));[m
[31m-        roomDeathPenalty.close(now.minusHours(1));[m
[31m-[m
[31m-        leave(roomDeathPenalty, u1); leave(roomDeathPenalty, u2);[m
[31m-        leave(roomDeathPenalty, u3); leave(roomDeathPenalty, u4);[m
[31m-        leave(roomDeathPenalty, u11); leave(roomDeathPenalty, u12);[m
[31m-[m
[31m-        Speech sd1 = saveSpeechIfMissing(roomDeathPenalty, u1,[m
[31m-                "사형제는 가장 무고한 피해자조차 보호하지 못합니다. " +[m
[31m-                "미국에서만 1973년 이후 185명의 사형수가 무죄로 밝혀졌습니다. " +[m
[31m-                "국가가 오판으로 무고한 사람의 생명을 빼앗는 것은 어떤 이유로도 정당화될 수 없습니다.",[m
[31m-                SpeechStance.PRO, "https://example.com/death-penalty-innocence");[m
[31m-        seedOffTopicReports(sd1, allUsers);[m
[31m-        Speech sd2 = saveSpeechIfMissing(roomDeathPenalty, u2,[m
[31m-                "흉악범죄 피해자 유족의 응보 감정과 사회 안전을 위해 사형제는 필요합니다. " +[m
[31m-                "재범이 불가능하다는 점에서 무기징역보다 확실한 사회 보호 수단이 됩니다.",[m
[31m-                SpeechStance.CON, null);[m
[31m-        seedOffTopicReports(sd2, allUsers);[m
[31m-        Speech sd3 = saveSpeechIfMissing(roomDeathPenalty, u3,[m
[31m-                "사형제의 범죄 억제 효과는 실증적으로 입증되지 않았습니다. " +[m
[31m-                "사형제 폐지 국가와 유지 국가의 살인율을 비교해도 유의미한 차이가 없습니다. " +[m
[31m-                "비용도 무기징역보다 재판 절차 등을 고려하면 오히려 더 많이 듭니다.",[m
[31m-                SpeechStance.PRO, "https://example.com/dp-deterrence-study");[m
[31m-        seedOffTopicReports(sd3, allUsers);[m
[31m-        Speech sd4 = saveSpeechIfMissing(roomDeathPenalty, u4,[m
[31m-                "피해자 가족에게 법적 종결감(closure)을 줄 수 있다는 측면이 간과되고 있습니다. " +[m
[31m-                "무기징역수의 재심·가석방 요청이 반복될 때마다 피해자 가족이 받는 2차 피해를 고려해야 합니다.",[m
[31m-                SpeechStance.CON, null);[m
[31m-        seedOffTopicReports(sd4, allUsers);[m
[31m-        Speech sd5 = saveSpeechIfMissing(roomDeathPenalty, u11,[m
[31m-                "사법 시스템의 구조적 불평등 문제도 짚어야 합니다. " +[m
[31m-                "변호인 선임 능력, 인종·계층에 따라 사형 적용이 불균형하게 이루어진다는 연구가 많습니다.",[m
[31m-                SpeechStance.PRO, null);[m
[31m-        seedOffTopicReports(sd5, allUsers);[m
[31m-        Speech sd6 = saveSpeechIfMissing(roomDeathPenalty, u12,[m
[31m-                "국민 다수가 사형제 유지를 지지하는 민주주의 국가에서 폐지는 민의를 거스르는 것입니다. " +[m
[31m-                "인권 논거보다 현실 민심을 반영하는 제도를 유지해야 합니다.",[m
[31m-                SpeechStance.CON, null);[m
[31m-        seedOffTopicReports(sd6, allUsers);[m
[31m-[m
[31m-        completeSpeechesForRoom(roomDeathPenalty, List.of(u1, u2, u3, u4, u11, u12),[m
[31m-                now.minusHours(1));[m
[31m-[m
[31m-        seedChatMessages(roomDeathPenalty, List.of([m
[31m-                msg(u1, "오늘 논의가 정말 깊이 있었습니다. 감사합니다."),[m
[31m-                msg(u2, "찬반 양쪽 모두 진지하게 임해주셔서 좋았어요."),[m
[31m-                msg(u3, "통계 자료를 준비해온 분들 덕분에 팩트 기반 토론이 됐네요."),[m
[31m-                msg(u4, "피해자 관점이 중요하게 다뤄져서 다행이었습니다."),[m
[31m-                msg(u11, "구조적 불평등 문제는 다음번에 더 심층적으로 다루면 좋겠어요."),[m
[31m-                msg(u12, "양측 다 설득력 있는 논거였습니다. 생각할 거리가 많네요."),[m
[31m-                msg(u1, "토론 종료 전 마지막 채팅 남깁니다. 모두 수고하셨습니다!")[m
[31m-        ));[m
[31m-[m
[31m-        seedReactions(sd1, List.of(u2, u3, u4, u11, u12));[m
[31m-        seedReactions(sd2, List.of(u1, u3, u11));[m
[31m-        seedReactions(sd3, List.of(u1, u2, u4, u12));[m
[31m-        seedReactions(sd4, List.of(u3, u11));[m
[31m-        seedReactions(sd5, List.of(u2, u4, u12));[m
[31m-        seedReactions(sd6, List.of(u1, u3));[m
[31m-        seedReport(roomDeathPenalty, sd4, u11, SpeechReportReason.OFF_TOPIC,[m
[31m-                "발언 내용이 주제에서 벗어나 개인적인 경험담 위주로 구성됐습니다.");[m
[31m-        seedReport(roomDeathPenalty, sd6, u3, SpeechReportReason.MISINFORMATION,[m
[31m-                "민심 지지율 수치의 출처가 불명확합니다.");[m
[31m-[m
[31m-        seedStageSummary(roomDeathPenalty, now.minusHours(1),[m
[31m-                "사형제 폐지 여부에 대해 찬성 측은 오판 위험성·비용·억제력 부재를, 반대 측은 피해자 응보 감정·재범 방지·민주적 민의를 핵심 근거로 제시했다. " +[m
[31m-                "양측 모두 실증 데이터를 활용하여 논쟁을 전개하였으며, 사법 시스템의 구조적 불평등 문제가 새로운 쟁점으로 부각되었다.",[m
[31m-                List.of("오판에 의한 무고한 생명 박탈 위험", "범죄 억제력 실증 근거 부족", "피해자 가족의 응보 감정과 법적 종결감",[m
[31m-                        "사법 시스템의 계층·인종별 불균형 적용", "무기징역 대비 비용 효율성 논쟁"), 6,[m
[31m-                now.minusMinutes(58));[m
[31m-[m
[31m-        // ── 종료: 안락사 (3시간 전 종료) ──[m
[31m-        Room roomEuthanasia = findOrCreateRoom([m
[31m-                findOrCreateTopic(TOPIC_EUTHANASIA,[m
[31m-                        "존엄사·적극적 안락사 합법화를 둘러싼 의료 윤리와 법적 쟁점을 토론합니다.",[m
[31m-                        "사회·복지", "https://example.com/euthanasia"),[m
[31m-                now.minusHours(5), now.minusHours(3));[m
[31m-        roomEuthanasia.close(now.minusHours(3));[m
[31m-[m
[31m-        leave(roomEuthanasia, u7); leave(roomEuthanasia, u8);[m
[31m-        leave(roomEuthanasia, u9); leave(roomEuthanasia, u13);[m
[31m-        leave(roomEuthanasia, u14);[m
[31m-[m
[31m-        Speech se1 = saveSpeechIfMissing(roomEuthanasia, u7,[m
[31m-                "응 그냥 죽여 " +[m
[31m-                "내 맘대로 못 죽게하냐?",[m
[31m-                SpeechStance.PRO, "https://example.com/euthanasia-netherlands");[m
[31m-        seedOffTopicReports(se1, allUsers);[m
[31m-        Speech se2 = saveSpeechIfMissing(roomEuthanasia, u8,[m
[31m-                "안락사 합법화는 사회적 취약계층에게 '죽어야 한다는 압박'으로 작용할 수 있습니다. " +[m
[31m-                "경제적 이유로 치료를 포기하고 안락사를 선택하는 사례가 생기면 사회가 삶을 포기하도록 유도하는 셈입니다.",[m
[31m-                SpeechStance.CON, null);[m
[31m-        seedOffTopicReports(se2, allUsers);[m
[31m-        Speech se3 = saveSpeechIfMissing(roomEuthanasia, u13,[m
[31m-                "완화의료(호스피스)가 충분히 발전하면 안락사를 선택할 이유가 줄어듭니다. " +[m
[31m-                "적극적 안락사보다 고통 없이 자연스러운 죽음을 돕는 완화의료 투자를 먼저 확대해야 합니다.",[m
[31m-                SpeechStance.CON, "https://example.com/palliative-care");[m
[31m-        seedOffTopicReports(se3, allUsers);[m
[31m-[m
[31m-        completeSpeechesForRoom(roomEuthanasia, List.of(u7, u8, u13), now.minusHours(3));[m
[31m-[m
[31m-        seedChatMessages(roomEuthanasia, List.of([m
[31m-                msg(u7, "생명 윤리 토론은 항상 무겁지만 꼭 필요한 논의입니다."),[m
[31m-                msg(u8, "의료진의 입장도 다양해서 더 복잡한 문제인 것 같아요."),[m
[31m-                msg(u9, "네덜란드 사례 자료가 인상적이었습니다."),[m
[31m-                msg(u13, "완화의료 확대라는 대안이 가장 현실적으로 느껴졌어요."),[m
[31m-                msg(u14, "오늘 토론으로 제 생각이 많이 바뀌었습니다. 감사합니다.")[m
[31m-        ));[m
[31m-[m
[31m-        seedReactions(se1, List.of(u8, u9, u13, u14));[m
[31m-        seedReactions(se2, List.of(u7, u9, u14));[m
[31m-        seedReactions(se3, List.of(u7, u8, u9));[m
[31m-[m
[31m-        // ── 종료: 최저임금 (어제 종료, 요약 완료) ──[m
[31m-        Room roomMinWage = findOrCreateRoom([m
[31m-                findOrCreateTopic(TOPIC_MIN_WAGE,[m
[31m-                        "최저임금 대폭 인상이 고용·물가·소득 분배에 미치는 효과를 분석합니다.",[m
[31m-                        "경제·금융", "https://example.com/min-wage"),[m
[31m-                now.minusHours(26), now.minusHours(24));[m
[31m-        roomMinWage.close(now.minusHours(24));[m
[31m-[m
[31m-        leave(roomMinWage, u11); leave(roomMinWage, u12);[m
[31m-        leave(roomMinWage, u13); leave(roomMinWage, u14);[m
[31m-        leave(roomMinWage, u1);[m
[31m-[m
[31m-        Speech sm1 = saveSpeechIfMissing(roomMinWage, u11,[m
[31m-                "최저임금 인상은 저임금 노동자의 구매력을 높여 내수 소비를 자극합니다. " +[m
[31m-                "시애틀 최저임금 인상 연구에서 저소득층 생활 수준이 개선됐다는 결과가 나왔습니다.",[m
[31m-                SpeechStance.PRO, "https://example.com/seattle-min-wage-study");[m
[31m-        seedOffTopicReports(sm1, allUsers);[m
[31m-        Speech sm2 = saveSpeechIfMissing(roomMinWage, u12,[m
[31m-                "급격한 최저임금 인상은 소규모 자영업자와 중소기업의 인건비 부담을 가중시킵니다. " +[m
[31m-                "특히 고용 유발 효과가 낮은 업종에서는 무인화·자동화 도입이 가속화되어 오히려 고용이 줄 수 있습니다.",[m
[31m-                SpeechStance.CON, null);[m
[31m-        seedOffTopicReports(sm2, allUsers);[m
[31m-        Speech sm3 = saveSpeechIfMissing(roomMinWage, u13,[m
[31m-                "업종별·지역별 차등 적용이 대안이 될 수 있습니다. " +[m
[31m-                "도시와 농촌, 대기업 하청과 소규모 자영업을 동일 기준으로 묶으면 부작용이 커집니다.",[m
[31m-                SpeechStance.CON, null);[m
[31m-        seedOffTopicReports(sm3, allUsers);[m
[31m-        Speech sm4 = saveSpeechIfMissing(roomMinWage, u14,[m
[31m-                "최저임금 인상과 함께 사회보험 사각지대 해소, 플랫폼 노동자 보호 등을 병행하면 " +[m
[31m-                "인상 효과가 더 넓게 확산됩니다. 단순 인상보다 종합적 노동 정책이 필요합니다.",[m
[31m-                SpeechStance.PRO, null);[m
[31m-        seedOffTopicReports(sm4, allUsers);[m
[31m-        Speech sm5 = saveSpeechIfMissing(roomMinWage, u1,[m
[31m-                "카드 수수료·임대료 등 고정 비용 부담이 큰 소상공인에게 최저임금 인상은 폐업 위협으로 다가옵니다. " +[m
[31m-                "인상 전에 상가 임대료 안정화, 카드 수수료 인하 등 선제 조치가 필요합니다.",[m
[31m-                SpeechStance.CON, "https://example.com/sme-min-wage-burden");[m
[31m-        seedOffTopicReports(sm5, allUsers);[m
[31m-[m
[31m-        completeSpeechesForRoom(roomMinWage, List.of(u11, u12, u13, u14, u1), now.minusHours(24));[m
[31m-[m
[31m-        seedChatMessages(roomMinWage, List.of([m
[31m-                msg(u11, "어제 토론이었는데 기록을 보니 알차네요."),[m
[31m-                msg(u12, "소상공인 입장이 충분히 다뤄져서 균형 잡힌 토론이었어요."),[m
[31m-                msg(u13, "차등 적용 아이디어에 많은 분들이 공감해주셨습니다."),[m
[31m-                msg(u14, "종합 노동 정책 관점으로 봐야 한다는 게 제 핵심 주장이었는데 잘 전달됐으면 합니다."),[m
[31m-                msg(u1, "소상공인으로서 현실적인 어려움을 말씀드렸는데 들어주셔서 감사합니다.")[m
[31m-        ));[m
[31m-[m
[31m-        seedReactions(sm1, List.of(u12, u13, u14, u1));[m
[31m-        seedReactions(sm2, List.of(u11, u13, u14));[m
[31m-        seedReactions(sm3, List.of(u11, u12, u1));[m
[31m-        seedReactions(sm4, List.of(u12, u13));[m
[31m-        seedReactions(sm5, List.of(u11, u14));[m
[31m-        seedReport(roomMinWage, sm2, u11, SpeechReportReason.MISINFORMATION,[m
[31m-                "무인화 가속 주장의 근거 데이터가 특정 업종에만 해당하는 편향된 자료입니다.");[m
[31m-[m
[31m-        seedStageSummary(roomMinWage, now.minusHours(24),[m
[31m-                "최저임금 대폭 인상 찬성 측은 저임금 노동자 구매력 향상과 내수 진작 효과를, " +[m
[31m-                "반대 측은 소상공인·중소기업 인건비 부담 증가와 자동화 촉진에 따른 고용 감소를 핵심 논거로 제시하였다. " +[m
[31m-                "업종·지역별 차등 적용 방안이 절충안으로 부상하였으며, " +[m
[31m-                "최저임금 단독 인상보다 임대료·수수료 부담 완화 등 보완 정책 병행의 필요성에 대한 공감대가 형성되었다.",[m
[31m-                List.of("저임금 노동자 구매력 향상 및 내수 소비 자극", "소상공인·중소기업 인건비 부담 및 폐업 위험",[m
[31m-                        "무인화·자동화 가속에 따른 역설적 고용 감소", "업종·지역별 차등 최저임금 적용 대안",[m
[31m-                        "임대료·카드 수수료 등 고정비 완화 정책 병행 필요"), 5,[m
[31m-                now.minusHours(23));[m
[31m-[m
[31m-        log.info("""[m
[31m-                ====================================================[m
[31m-                로컬 더미 데이터 초기화 완료[m
[31m-                ====================================================[m
[31m-                [계정][m
[31m-                  어드민  : {} / {}[m
[31m-                  일반유저: u1@sisibibi.test ~ u14@sisibibi.test / {}[m
[31m-                [진행중인 토론방][m
[31m-                  활발    : roomId={} ({})[m
[31m-                  곧 종료 : roomId={} ({})[m
[31m-                  초기    : roomId={} ({})[m
[31m-                  빈 방   : {}[m
[31m-                  테스트  : roomId={} ({})[m
[31m-                [종료된 토론방][m
[31m-                  1시간 전: roomId={} ({})[m
[31m-                  3시간 전: roomId={} ({})[m
[31m-                  어제    : roomId={} ({})[m
[31m-                ====================================================[m
[31m-                """,[m
[31m-                ADMIN_EMAIL, PASSWORD, PASSWORD,[m
[31m-                roomAiJobs.getId(), TOPIC_AI_JOBS,[m
[31m-                roomCarbonTax.getId(), TOPIC_CARBON_TAX,[m
[31m-                roomCrypto.getId(), TOPIC_CRYPTO,[m
[31m-                TOPIC_UBI,[m
[31m-                roomCleanStageTest.getId(), TOPIC_CLEAN_STAGE_TEST,[m
[31m-                roomDeathPenalty.getId(), TOPIC_DEATH_PENALTY,[m
[31m-                roomEuthanasia.getId(), TOPIC_EUTHANASIA,[m
[31m-                roomMinWage.getId(), TOPIC_MIN_WAGE[m
[31m-        );[m
[31m-    }[m
[31m-[m
[31m-    // ── 유저 ──[m
[31m-[m
[31m-    private User findOrCreateUser(String email, String nickname) {[m
[31m-        return userRepository.findByEmail(email)[m
[31m-                .orElseGet(() -> userRepository.save(User.signup([m
[31m-                        email, passwordEncoder.encode(PASSWORD), nickname)));[m
[31m-    }[m
[31m-[m
[31m-    private User findOrCreateAdmin() {[m
[31m-        return userRepository.findByEmail(ADMIN_EMAIL)[m
[31m-                .orElseGet(() -> userRepository.save(User.admin([m
[31m-                        ADMIN_EMAIL, passwordEncoder.encode(PASSWORD), "관리자")));[m
[31m-    }[m
[31m-[m
[31m-    // ── 토픽·방 ──[m
[31m-[m
[31m-    private Topic findOrCreateTopic(String title, String description, String category, String sourceUrl) {[m
[31m-        return topicRepository.findByTitle(title)[m
[31m-                .orElseGet(() -> topicRepository.save(Topic.approved(title, description, category, sourceUrl)));[m
[31m-    }[m
[31m-[m
[31m-    private Room findOrCreateRoom(Topic topic, LocalDateTime startedAt, LocalDateTime endedAt) {[m
[31m-        return roomRepository.findByTopicId(topic.getId())[m
[31m-                .map(room -> {[m
[31m-                    ensureQueueSequence(room.getId(), startedAt);[m
[31m-                    return room;[m
[31m-                })[m
[31m-                .orElseGet(() -> {[m
[31m-                    Room room = roomRepository.save([m
[31m-                            Room.open(topic.getId(), topic.getTitle(), startedAt, endedAt, 100));[m
[31m-                    ensureQueueSequence(room.getId(), startedAt);[m
[31m-                    return room;[m
[31m-                });[m
[31m-    }[m
[31m-[m
[31m-    private void ensureQueueSequence(Long roomId, LocalDateTime now) {[m
[31m-        if (!roomQueueSequenceRepository.existsById(roomId)) {[m
[31m-            roomQueueSequenceRepository.save(RoomQueueSequence.create(roomId, now));[m
[31m-        }[m
[31m-    }[m
[31m-[m
[31m-    // ── 참여자 ──[m
[31m-[m
[31m-    private void join(Room room, User user) {[m
[31m-        RoomParticipant p = roomParticipantRepository[m
[31m-                .findByRoomIdAndUserId(room.getId(), user.getId())[m
[31m-                .orElseGet(() -> RoomParticipant.join(room.getId(), user.getId()));[m
[31m-        p.rejoin();[m
[31m-        roomParticipantRepository.save(p);[m
[31m-    }[m
[31m-[m
[31m-    private void leave(Room room, User user) {[m
[31m-        RoomParticipant p = roomParticipantRepository[m
[31m-                .findByRoomIdAndUserId(room.getId(), user.getId())[m
[31m-                .orElseGet(() -> RoomParticipant.join(room.getId(), user.getId()));[m
[31m-        p.leave();[m
[31m-        roomParticipantRepository.save(p);[m
[31m-    }[m
[31m-[m
[31m-    // ── 발언 ──[m
[31m-[m
[31m-    private Speech saveSpeechIfMissing(Room room, User user, String content, SpeechStance stance, String linkUrl) {[m
[31m-        if (speechRepository.existsByRoomIdAndUserIdAndDeletedFalse(room.getId(), user.getId())) {[m
[31m-            return speechRepository.findByRoomIdBeforeCursor(room.getId(), null, PageRequest.of(0, 50))[m
[31m-                    .stream().filter(s -> s.getUserId().equals(user.getId())).findFirst()[m
[31m-                    .orElseThrow();[m
[31m-        }[m
[31m-        Speech speech = Speech.createMainOpinion(room.getId(), user.getId(), content, stance);[m
[31m-        if (linkUrl != null) {[m
[31m-            speech.updateLink(linkUrl);[m
[31m-        }[m
[31m-        return speechRepository.save(speech);[m
[31m-    }[m
[31m-[m
[31m-    private void completeSpeechesForRoom(Room room, List<User> users, LocalDateTime endedAt) {[m
[31m-        for (User user : users) {[m
[31m-            speechRepository.completeSpeakingSpeeches([m
[31m-                    room.getId(), user.getId(),[m
[31m-                    SpeechStatus.SPEAKING, SpeechStatus.COMPLETED,[m
[31m-                    endedAt);[m
[31m-        }[m
[31m-    }[m
[31m-[m
[31m-    // ── 채팅 ──[m
[31m-[m
[31m-    private record MsgDraft(User user, String content) {}[m
[31m-[m
[31m-    private MsgDraft msg(User user, String content) {[m
[31m-        return new MsgDraft(user, content);[m
[31m-    }[m
[31m-[m
[31m-    private void seedChatMessages(Room room, List<MsgDraft> drafts) {[m
[31m-        boolean hasMessage = !chatMessageRepository[m
[31m-                .findVisibleByRoomIdBeforeCursor(room.getId(), null, PageRequest.of(0, 1))[m
[31m-                .isEmpty();[m
[31m-        if (hasMessage) return;[m
[31m-[m
[31m-        for (MsgDraft draft : drafts) {[m
[31m-            chatMessageRepository.save(ChatMessage.create([m
[31m-                    room.getId(), draft.user().getId(),[m
[31m-                    draft.user().getNickname(), draft.content()));[m
[31m-        }[m
[31m-    }[m
[31m-[m
[31m-    // ── 발언 큐 (현재 발언자 + 대기자) ──[m
[31m-[m
[31m-    private void seedSpeakingQueue(Room room, User currentSpeaker, List<User> waitingUsers, LocalDateTime now) {[m
[31m-        if (speakingQueueRepository.existsByRoomIdAndStatus(room.getId(), SpeakingQueueStatus.ASSIGNED)) {[m
[31m-            return;[m
[31m-        }[m
[31m-        SpeakingQueue assigned = SpeakingQueue.waiting(room.getId(), currentSpeaker.getId(), 1,[m
[31m-                SpeechStance.PRO, now.minusMinutes(3));[m
[31m-        assigned.assign(now.minusMinutes(1), now.plusMinutes(4));[m
[31m-        speakingQueueRepository.save(assigned);[m
[31m-        syncAssignedToRedis(room, currentSpeaker);[m
[31m-[m
[31m-        int order = 2;[m
[31m-        for (User waiting : waitingUsers) {[m
[31m-            saveWaitingIfMissing(room, waiting, order++, SpeechStance.CON, now.minusMinutes(2));[m
[31m-        }[m
[31m-    }[m
[31m-[m
[31m-    private void seedSpeakingQueueOnly(Room room, List<User> waitingUsers, LocalDateTime now) {[m
[31m-        int order = 1;[m
[31m-        for (User user : waitingUsers) {[m
[31m-            saveWaitingIfMissing(room, user, order++, SpeechStance.PRO, now.minusMinutes(order));[m
[31m-        }[m
[31m-    }[m
[31m-[m
[31m-    private void saveWaitingIfMissing(Room room, User user, int queueOrder, SpeechStance stance, LocalDateTime requestedAt) {[m
[31m-        boolean exists = speakingQueueRepository.existsByRoomIdAndUserIdAndStatusIn([m
[31m-                room.getId(), user.getId(),[m
[31m-                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED));[m
[31m-        if (exists) return;[m
[31m-[m
[31m-        speakingQueueRepository.save(SpeakingQueue.waiting([m
[31m-                room.getId(), user.getId(), queueOrder, stance, requestedAt));[m
[31m-        syncWaitingToRedis(room, user, queueOrder);[m
[31m-    }[m
[31m-[m
[31m-    private void syncAssignedToRedis(Room room, User user) {[m
[31m-        try {[m
[31m-            redisSpeakingQueueRepository.assign(room.getId(), user.getId());[m
[31m-        } catch (RuntimeException e) {[m
[31m-            log.warn("Redis current speaker sync failed. roomId={}, userId={}", room.getId(), user.getId(), e);[m
[31m-        }[m
[31m-    }[m
[31m-[m
[31m-    private void syncWaitingToRedis(Room room, User user, int queueOrder) {[m
[31m-        try {[m
[31m-            redisSpeakingQueueRepository.upsert(room.getId(), user.getId(), queueOrder);[m
[31m-        } catch (RuntimeException e) {[m
[31m-            log.warn("Redis waiting queue sync failed. roomId={}, userId={}, order={}", room.getId(), user.getId(), queueOrder, e);[m
[31m-        }[m
[31m-    }[m
[31m-[m
[31m-    // ── 반응 ──[m
[31m-[m
[31m-    private void seedReactions(Speech speech, List<User> reactors) {[m
[31m-        if (speech == null) return;[m
[31m-        for (User user : reactors) {[m
[31m-            if (!speechReactionRepository.existsBySpeechIdAndUserId(speech.getId(), user.getId())) {[m
[31m-                speechReactionRepository.save(SpeechReaction.create(speech.getId(), user.getId()));[m
[31m-            }[m
[31m-        }[m
[31m-    }[m
[31m-[m
[31m-    // ── 신고 ──[m
[31m-[m
[31m-    private void seedOffTopicReports(Speech speech, List<User> allUsers) {[m
[31m-        allUsers.stream()[m
[31m-                .filter(u -> !u.getId().equals(speech.getUserId()))[m
[31m-                .limit(4)[m
[31m-                .forEach(reporter -> {[m
[31m-                    if (!speechReportRepository.existsBySpeechIdAndReporterUserId(speech.getId(), reporter.getId())) {[m
[31m-                        speechReportRepository.save(SpeechReport.create([m
[31m-                                speech.getId(), speech.getUserId(), reporter.getId(),[m
[31m-                                speech.getContent(), SpeechReportReason.OFF_TOPIC, "논점 이탈로 판단되어 신고합니다."));[m
[31m-                    }[m
[31m-                });[m
[31m-    }[m
[31m-[m
[31m-    private void seedReport(Room room, Speech speech, User reporter, SpeechReportReason reason, String description) {[m
[31m-        if (speech == null) return;[m
[31m-        if (speechReportRepository.existsBySpeechIdAndReporterUserId(speech.getId(), reporter.getId())) return;[m
[31m-        speechReportRepository.save(SpeechReport.create([m
[31m-                speech.getId(), speech.getUserId(), reporter.getId(),[m
[31m-                speech.getContent(), reason, description));[m
[31m-    }[m
[31m-[m
[31m-    // ── 스테이지 요약 ──[m
[31m-[m
[31m-    private void seedStageSummary(Room room, LocalDateTime triggeredAt, String summary,[m
[31m-            List<String> keyPoints, int speakerCount, LocalDateTime completedAt) {[m
[31m-        if (stageSummaryRepository.findByRoomId(room.getId()).isPresent()) return;[m
[31m-        StageSummary stageSummary = StageSummary.pending(room.getId(), triggeredAt, speakerCount);[m
[31m-        stageSummaryRepository.save(stageSummary);[m
[31m-        stageSummary.complete(summary, keyPoints, speakerCount, completedAt);[m
[31m-        stageSummaryRepository.save(stageSummary);[m
[31m-    }[m
[31m-}[m
[32m+[m[32m//package com.sisibibi.api.global.init;[m
[32m+[m[32m//[m
[32m+[m[32m//import com.sisibibi.api.domain.chat.entity.ChatMessage;[m
[32m+[m[32m//import com.sisibibi.api.domain.chat.repository.ChatMessageRepository;[m
[32m+[m[32m//import com.sisibibi.api.domain.room.entity.Room;[m
[32m+[m[32m//import com.sisibibi.api.domain.room.repository.RoomRepository;[m
[32m+[m[32m//import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipant;[m
[32m+[m[32m//import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;[m
[32m+[m[32m//import com.sisibibi.api.domain.speech.entity.RoomQueueSequence;[m
[32m+[m[32m//import com.sisibibi.api.domain.speech.entity.SpeakingQueue;[m
[32m+[m[32m//import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;[m
[32m+[m[32m//import com.sisibibi.api.domain.speech.entity.Speech;[m
[32m+[m[32m//import com.sisibibi.api.domain.speech.enti