package com.sisibibi.api.global.config;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.repository.TopicRepository;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
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
    private static final String TOPIC_TITLE = "AI 생성 뉴스, 신뢰할 수 있을까?";

    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final RoomRepository roomRepository;
    private final SpeechRepository speechRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User testUser = findOrCreateUser(TEST_USER_EMAIL, "테스트유저");
        User author = findOrCreateUser(AUTHOR_EMAIL, "의견작성자");
        findOrCreateAdmin();
        Topic topic = topicRepository.findByTitle(TOPIC_TITLE)
                .orElseGet(() -> topicRepository.save(Topic.approved(
                        TOPIC_TITLE,
                        "AI가 만든 뉴스의 신뢰성과 검증 책임에 대해 토론합니다.",
                        "AI·기술",
                        "https://example.com/ai-news"
                )));
        Room room = roomRepository.findByTopicId(topic.getId())
                .orElseGet(() -> roomRepository.save(Room.open(topic.getId(), topic.getTitle())));

        if (!speechRepository.existsByRoomIdAndUserIdAndDeletedFalse(room.getId(), author.getId())) {
            speechRepository.save(Speech.createMainOpinion(
                    room.getId(),
                    author.getId(),
                    "AI 생성 뉴스에는 출처 표시와 사람의 최종 검증 절차가 필요합니다.",
                    SpeechStance.PRO
            ));
        }

        log.info(
                "Local test data ready. loginEmail={}, roomId={}, testUserId={}",
                TEST_USER_EMAIL,
                room.getId(),
                testUser.getId()
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
                        "로컬관리자"
                )));
    }
}
