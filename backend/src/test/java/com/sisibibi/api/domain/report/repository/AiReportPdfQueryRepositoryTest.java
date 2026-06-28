package com.sisibibi.api.domain.report.repository;

import com.sisibibi.api.domain.report.service.AiReportPdfModel;
import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speechreaction.entity.SpeechReaction;
import com.sisibibi.api.domain.speechreaction.repository.SpeechReactionRepository;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaAuditingConfig.class, AiReportPdfQueryRepository.class})
class AiReportPdfQueryRepositoryTest {

    @Autowired
    private AiReportPdfQueryRepository queryRepository;

    @Autowired
    private SpeechRepository speechRepository;

    @Autowired
    private SpeechReactionRepository speechReactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void collectStats_excludesDeletedIncompleteAndBlankSpeeches() {
        User user1 = userRepository.save(User.signup("u1@test.com", "pw", "nick1"));
        User user2 = userRepository.save(User.signup("u2@test.com", "pw", "nick2"));
        User user3 = userRepository.save(User.signup("u3@test.com", "pw", "nick3"));
        User user4 = userRepository.save(User.signup("u4@test.com", "pw", "nick4"));
        User user5 = userRepository.save(User.signup("u5@test.com", "pw", "nick5"));

        Long roomId = 1L;
        Speech pro1 = completedSpeech(roomId, user1.getId(), "pro opinion 1", SpeechStance.PRO);
        Speech pro2 = completedSpeech(roomId, user2.getId(), "pro opinion 2", SpeechStance.PRO);
        Speech pro3 = completedSpeech(roomId, user3.getId(), "pro opinion 3", SpeechStance.PRO);
        Speech con1 = completedSpeech(roomId, user4.getId(), "con opinion 1", SpeechStance.CON);
        Speech con2 = completedSpeech(roomId, user5.getId(), "con opinion 2", SpeechStance.CON);
        Speech incomplete = Speech.createMainOpinion(roomId, user1.getId(), "incomplete", SpeechStance.PRO);
        Speech deleted = completedSpeech(roomId, user2.getId(), "deleted speech", SpeechStance.PRO);
        deleted.softDelete(LocalDateTime.now());
        speechRepository.saveAllAndFlush(List.of(pro1, pro2, pro3, con1, con2, incomplete, deleted));

        speechReactionRepository.saveAllAndFlush(List.of(
                SpeechReaction.create(pro1.getId(), user2.getId()),
                SpeechReaction.create(pro1.getId(), user3.getId()),
                SpeechReaction.create(pro2.getId(), user1.getId()),
                SpeechReaction.create(pro2.getId(), user4.getId()),
                SpeechReaction.create(con1.getId(), user1.getId()),
                SpeechReaction.create(con1.getId(), user2.getId()),
                SpeechReaction.create(con2.getId(), user1.getId()),
                SpeechReaction.create(con2.getId(), user3.getId())
        ));

        AiReportPdfModel.Stats stats = queryRepository.collectStats(roomId);

        assertThat(stats.proCount()).isEqualTo(3);
        assertThat(stats.conCount()).isEqualTo(2);
        assertThat(stats.opinionCount()).isEqualTo(5);
        assertThat(stats.reactionCount()).isEqualTo(8);
    }

    @Test
    void findTopOpinions_returnsMaxThreePerStanceByReactionCountThenOldestSpeech() {
        User user1 = userRepository.save(User.signup("t1@test.com", "pw", "tnick1"));
        User user2 = userRepository.save(User.signup("t2@test.com", "pw", "tnick2"));
        User user3 = userRepository.save(User.signup("t3@test.com", "pw", "tnick3"));
        User user4 = userRepository.save(User.signup("t4@test.com", "pw", "tnick4"));

        Long roomId = 2L;
        Speech pro1 = completedSpeech(roomId, user1.getId(), "pro 1", SpeechStance.PRO);
        Speech pro2 = completedSpeech(roomId, user2.getId(), "pro 2", SpeechStance.PRO);
        Speech pro3 = completedSpeech(roomId, user3.getId(), "pro 3", SpeechStance.PRO);
        Speech pro4 = completedSpeech(roomId, user4.getId(), "pro 4", SpeechStance.PRO);
        speechRepository.saveAllAndFlush(List.of(pro1, pro2, pro3, pro4));

        speechReactionRepository.saveAllAndFlush(List.of(
                SpeechReaction.create(pro1.getId(), user2.getId()),
                SpeechReaction.create(pro1.getId(), user3.getId()),
                SpeechReaction.create(pro1.getId(), user4.getId()),
                SpeechReaction.create(pro1.getId(), user1.getId()),
                SpeechReaction.create(pro2.getId(), user1.getId()),
                SpeechReaction.create(pro2.getId(), user3.getId()),
                SpeechReaction.create(pro3.getId(), user1.getId())
        ));

        List<AiReportPdfModel.TopOpinion> proOpinions = queryRepository.findTopOpinions(roomId, SpeechStance.PRO, 3);

        assertThat(proOpinions).hasSize(3);
        assertThat(proOpinions).extracting(AiReportPdfModel.TopOpinion::reactionCount)
                .containsExactly(4L, 2L, 1L);
    }

    private Speech completedSpeech(Long roomId, Long userId, String content, SpeechStance stance) {
        Speech speech = Speech.createMainOpinion(roomId, userId, content, stance);
        ReflectionTestUtils.setField(speech, "status", SpeechStatus.COMPLETED);
        return speech;
    }
}
