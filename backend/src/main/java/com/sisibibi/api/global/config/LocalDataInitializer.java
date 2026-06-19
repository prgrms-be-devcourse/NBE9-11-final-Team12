package com.sisibibi.api.global.config;

import com.sisibibi.api.domain.chat.entity.ChatMessage;
import com.sisibibi.api.domain.chat.repository.ChatMessageRepository;
import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipant;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.RedisSpeakingQueueRepository;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
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
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer implements ApplicationRunner {

    private static final String TEST_USER_EMAIL = "local-user@sisibibi.test";
    private static final String AUTHOR_EMAIL = "local-author@sisibibi.test";
    private static final String ADMIN_EMAIL = "local-admin@sisibibi.test";
    private static final String TEST_PASSWORD = "test1234!";

    private static final String ACTIVE_TOPIC_TITLE = "AI 생성 뉴스, 신뢰할 수 있을까?";
    private static final String EMPTY_TOPIC_TITLE = "주 4일제, 생산성을 높일까?";
    private static final String CLOSING_TOPIC_TITLE = "청소년 스마트폰 제한, 필요한가?";
    private static final String CLOSED_TOPIC_TITLE = "배달 플랫폼 수수료 상한제, 도입해야 할까?";

    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final SpeechRepository speechRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SpeakingQueueRepository speakingQueueRepository;
    private final RedisSpeakingQueueRepository redisSpeakingQueueRepository;
    private final SpeechReportRepository speechReportRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User testUser = findOrCreateUser(TEST_USER_EMAIL, "local_user");
        User author = findOrCreateUser(AUTHOR_EMAIL, "opinion_author");
        User proSpeaker = findOrCreateUser("local-pro@sisibibi.test", "pro_speaker");
        User conSpeaker = findOrCreateUser("local-con@sisibibi.test", "con_speaker");
        User waitingOne = findOrCreateUser("local-waiting-1@sisibibi.test", "waiting_one");
        User waitingTwo = findOrCreateUser("local-waiting-2@sisibibi.test", "waiting_two");
        User reporter = findOrCreateUser("local-reporter@sisibibi.test", "reporter");
        User leftUser = findOrCreateUser("local-left@sisibibi.test", "left_user");
        User admin = findOrCreateAdmin();

        LocalDateTime now = LocalDateTime.now();

        Room activeRoom = findOrCreateRoom(
                findOrCreateTopic(
                        ACTIVE_TOPIC_TITLE,
                        "AI가 만든 뉴스의 출처 표시, 검증 책임, 플랫폼 규제를 함께 토론합니다.",
                        "AI·기술",
                        "https://example.com/ai-news"
                ),
                now.minusMinutes(20),
                now.plusMinutes(40)
        );
        Room emptyRoom = findOrCreateRoom(
                findOrCreateTopic(
                        EMPTY_TOPIC_TITLE,
                        "근무 시간 단축이 생산성, 임금, 채용에 미치는 영향을 비교합니다.",
                        "경제·금융",
                        "https://example.com/four-day-week"
                ),
                now.minusMinutes(5),
                now.plusHours(1)
        );
        Room closingRoom = findOrCreateRoom(
                findOrCreateTopic(
                        CLOSING_TOPIC_TITLE,
                        "학교와 가정에서 스마트폰 사용 제한을 어디까지 허용할지 검증합니다.",
                        "사회·복지",
                        "https://example.com/smartphone-policy"
                ),
                now.minusMinutes(55),
                now.plusMinutes(5)
        );
        Room closedRoom = findOrCreateRoom(
                findOrCreateTopic(
                        CLOSED_TOPIC_TITLE,
                        "소상공인 보호와 플랫폼 혁신 사이의 균형점을 토론합니다.",
                        "경제·금융",
                        "https://example.com/delivery-fee"
                ),
                now.minusHours(2),
                now.minusHours(1)
        );
        closedRoom.close(now.minusHours(1));

        join(activeRoom, testUser);
        join(activeRoom, author);
        join(activeRoom, proSpeaker);
        join(activeRoom, conSpeaker);
        join(activeRoom, waitingOne);
        join(activeRoom, waitingTwo);
        join(activeRoom, reporter);
        leave(activeRoom, leftUser);

        join(closingRoom, testUser);
        join(closingRoom, proSpeaker);
        join(closingRoom, reporter);

        leave(closedRoom, author);
        leave(closedRoom, conSpeaker);

        seedSpeeches(activeRoom, author, proSpeaker, conSpeaker);
        seedSpeeches(closingRoom, proSpeaker, reporter, null);
        seedSpeeches(closedRoom, author, conSpeaker, null);

        seedChatMessages(activeRoom, List.of(testUser, author, proSpeaker, conSpeaker, waitingOne));
        seedChatMessages(closingRoom, List.of(testUser, proSpeaker, reporter));
        seedChatMessages(closedRoom, List.of(author, conSpeaker));

        seedSpeakingQueue(activeRoom, proSpeaker, waitingOne, waitingTwo, now);
        seedReport(activeRoom, conSpeaker, reporter);

        log.info(
                "Local test data ready. userEmail={}, adminEmail={}, password={}, activeRoomId={}, emptyRoomId={}, closingRoomId={}, closedRoomId={}, userId={}, adminId={}",
                TEST_USER_EMAIL,
                ADMIN_EMAIL,
                TEST_PASSWORD,
                activeRoom.getId(),
                emptyRoom.getId(),
                closingRoom.getId(),
                closedRoom.getId(),
                testUser.getId(),
                admin.getId()
        );
    }

    private User findOrCreateUser(String email, String nickname) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.signup(
                        email,
                        passwordEncoder.encode(TEST_PASSWORD),
                        nickname
                )));
    }

    private User findOrCreateAdmin() {
        return userRepository.findByEmail(ADMIN_EMAIL)
                .orElseGet(() -> userRepository.save(User.admin(
                        ADMIN_EMAIL,
                        passwordEncoder.encode(TEST_PASSWORD),
                        "local_admin"
                )));
    }

    private Topic findOrCreateTopic(
            String title,
            String description,
            String category,
            String sourceUrl
    ) {
        return topicRepository.findByTitle(title)
                .orElseGet(() -> topicRepository.save(Topic.approved(
                        title,
                        description,
                        category,
                        sourceUrl
                )));
    }

    private Room findOrCreateRoom(
            Topic topic,
            LocalDateTime startedAt,
            LocalDateTime endedAt
    ) {
        return roomRepository.findByTopicId(topic.getId())
                .orElseGet(() -> roomRepository.save(Room.open(
                        topic.getId(),
                        topic.getTitle(),
                        startedAt,
                        endedAt
                )));
    }

    private void join(Room room, User user) {
        RoomParticipant participant = roomParticipantRepository
                .findByRoomIdAndUserId(room.getId(), user.getId())
                .orElseGet(() -> RoomParticipant.join(room.getId(), user.getId()));
        participant.rejoin();
        roomParticipantRepository.save(participant);
    }

    private void leave(Room room, User user) {
        RoomParticipant participant = roomParticipantRepository
                .findByRoomIdAndUserId(room.getId(), user.getId())
                .orElseGet(() -> RoomParticipant.join(room.getId(), user.getId()));
        participant.leave();
        roomParticipantRepository.save(participant);
    }

    private void seedSpeeches(
            Room room,
            User proAuthor,
            User conAuthor,
            User additionalAuthor
    ) {
        saveSpeechIfMissing(
                room,
                proAuthor,
                "찬성: 이 정책은 투명성을 높이고 이용자가 정보를 검증할 출발점을 제공합니다.",
                SpeechStance.PRO,
                "https://example.com/evidence/pro"
        );
        saveSpeechIfMissing(
                room,
                conAuthor,
                "반대: 과도한 규제는 서비스 출시 속도를 늦추고 작은 팀의 부담을 키울 수 있습니다.",
                SpeechStance.CON,
                "https://example.com/evidence/con"
        );
        if (additionalAuthor != null) {
            saveSpeechIfMissing(
                    room,
                    additionalAuthor,
                    "보완 의견: 규제보다 먼저 공개 기준과 신고 절차를 명확히 해야 합니다.",
                    SpeechStance.PRO,
                    null
            );
        }
    }

    private void saveSpeechIfMissing(
            Room room,
            User user,
            String content,
            SpeechStance stance,
            String linkUrl
    ) {
        if (speechRepository.existsByRoomIdAndUserIdAndDeletedFalse(room.getId(), user.getId())) {
            return;
        }

        Speech speech = Speech.createMainOpinion(
                room.getId(),
                user.getId(),
                content,
                stance
        );
        if (linkUrl != null) {
            speech.updateLink(linkUrl);
        }
        speechRepository.save(speech);
    }

    private void seedChatMessages(Room room, List<User> users) {
        boolean hasAnyMessage = !chatMessageRepository
                .findVisibleByRoomIdBeforeCursor(room.getId(), null, PageRequest.of(0, 1))
                .isEmpty();
        if (hasAnyMessage) {
            return;
        }

        for (int index = 0; index < users.size(); index++) {
            User user = users.get(index);
            chatMessageRepository.save(ChatMessage.create(
                    room.getId(),
                    user.getId(),
                    user.getNickname(),
                    switch (index) {
                        case 0 -> "오늘 토론 핵심은 검증 책임을 어디에 둘지인 것 같습니다.";
                        case 1 -> "출처 공개는 최소 기준으로 필요하다고 봅니다.";
                        case 2 -> "플랫폼 책임을 너무 넓히면 신규 서비스가 위축될 수 있습니다.";
                        case 3 -> "사용자 신고와 사후 검증을 함께 두면 균형이 맞을 것 같습니다.";
                        default -> "발언권 신청 전에 채팅으로 쟁점을 정리해 보겠습니다.";
                    }
            ));
        }
    }

    private void seedSpeakingQueue(
            Room room,
            User currentSpeaker,
            User waitingOne,
            User waitingTwo,
            LocalDateTime now
    ) {
        if (speakingQueueRepository.existsByRoomIdAndStatus(
                room.getId(),
                SpeakingQueueStatus.ASSIGNED
        )) {
            return;
        }

        SpeakingQueue assigned = SpeakingQueue.waiting(
                room.getId(),
                currentSpeaker.getId(),
                1,
                now.minusMinutes(3)
        );
        assigned.assign(now.minusMinutes(1), now.plusMinutes(4));
        speakingQueueRepository.save(assigned);
        assignCurrentSpeakerInRedis(room, currentSpeaker);

        saveWaitingQueue(room, waitingOne, 2, now.minusMinutes(2));
        saveWaitingQueue(room, waitingTwo, 3, now.minusMinutes(1));
    }

    private void saveWaitingQueue(
            Room room,
            User user,
            int queueOrder,
            LocalDateTime requestedAt
    ) {
        boolean exists = speakingQueueRepository.existsByRoomIdAndUserIdAndStatusIn(
                room.getId(),
                user.getId(),
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        );
        if (exists) {
            return;
        }

        speakingQueueRepository.save(SpeakingQueue.waiting(
                room.getId(),
                user.getId(),
                queueOrder,
                requestedAt
        ));
        upsertWaitingQueueInRedis(room, user, queueOrder);
    }

    private void assignCurrentSpeakerInRedis(Room room, User user) {
        try {
            redisSpeakingQueueRepository.assign(room.getId(), user.getId());
        } catch (RuntimeException exception) {
            log.warn(
                    "Local data DB seed completed, but Redis current speaker sync failed. roomId={}, userId={}",
                    room.getId(),
                    user.getId(),
                    exception
            );
        }
    }

    private void upsertWaitingQueueInRedis(Room room, User user, int queueOrder) {
        try {
            redisSpeakingQueueRepository.upsert(room.getId(), user.getId(), queueOrder);
        } catch (RuntimeException exception) {
            log.warn(
                    "Local data DB seed completed, but Redis waiting queue sync failed. roomId={}, userId={}, queueOrder={}",
                    room.getId(),
                    user.getId(),
                    queueOrder,
                    exception
            );
        }
    }

    private void seedReport(Room room, User reportedUser, User reporter) {
        List<Speech> speeches = speechRepository.findByRoomIdBeforeCursor(
                room.getId(),
                null,
                PageRequest.of(0, 20)
        );
        speeches.stream()
                .filter(speech -> speech.getUserId().equals(reportedUser.getId()))
                .findFirst()
                .ifPresent(speech -> {
                    if (speechReportRepository.existsBySpeechIdAndReporterUserId(
                            speech.getId(),
                            reporter.getId()
                    )) {
                        return;
                    }
                    speechReportRepository.save(SpeechReport.create(
                            speech.getId(),
                            reportedUser.getId(),
                            reporter.getId(),
                            speech.getContent(),
                            SpeechReportReason.OFF_TOPIC,
                            "로컬 테스트용 신고 데이터입니다."
                    ));
                });
    }
}
