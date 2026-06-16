package com.sisibibi.api.domain.speech.loadtest;

import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import com.sisibibi.api.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Profile("load-test")
@RequiredArgsConstructor
public class LoadTestStageExpirationService {

    private final JdbcTemplate jdbcTemplate;
    private final SpeakingQueueRepository speakingQueueRepository;
    private final SpeakingQueueService speakingQueueService;

    @Value("${app.speaking.expiration.time-limit-seconds:300}")
    private long speakingTimeLimitSeconds;

    @Transactional
    public LoadTestExpirationPrepareRes prepareExpirationCandidates(
            int roomCount,
            int waitingPerRoom,
            Long roomIdStart,
            Long userIdStart
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime requestedAt = now.minusMinutes(10);
        LocalDateTime expiredAssignedAt = now.minusSeconds(speakingTimeLimitSeconds + 60);
        int usersPerRoom = waitingPerRoom + 1;

        jdbcTemplate.update(
                "delete from speaking_queue where room_id >= ? and room_id < ?",
                roomIdStart,
                roomIdStart + roomCount
        );

        int preparedCurrentSpeakers = 0;
        int preparedWaitingSpeakers = 0;

        for (int roomIndex = 0; roomIndex < roomCount; roomIndex++) {
            long roomId = roomIdStart + roomIndex;
            long firstUserId = userIdStart + ((long) roomIndex * usersPerRoom);

            upsertRoom(roomId);
            upsertUsers(firstUserId, usersPerRoom);

            insertSpeakingQueue(
                    roomId,
                    firstUserId,
                    1,
                    SpeakingQueueStatus.ASSIGNED,
                    requestedAt,
                    expiredAssignedAt
            );
            preparedCurrentSpeakers++;

            for (int waitingIndex = 0; waitingIndex < waitingPerRoom; waitingIndex++) {
                insertSpeakingQueue(
                        roomId,
                        firstUserId + waitingIndex + 1,
                        waitingIndex + 2,
                        SpeakingQueueStatus.WAITING,
                        requestedAt,
                        null
                );
                preparedWaitingSpeakers++;
            }
        }

        return new LoadTestExpirationPrepareRes(
                roomIdStart,
                userIdStart,
                roomCount,
                waitingPerRoom,
                preparedCurrentSpeakers,
                preparedWaitingSpeakers
        );
    }

    public LoadTestExpirationRunRes runExpiration() {
        long startedAt = System.nanoTime();
        LocalDateTime now = LocalDateTime.now();
        Duration speakingTimeLimit = Duration.ofSeconds(speakingTimeLimitSeconds);
        LocalDateTime expiresBefore = now.minus(speakingTimeLimit);

        List<Long> roomIds = speakingQueueRepository
                .findDistinctRoomIdsByStatusAndAssignedAtLessThanEqual(
                        SpeakingQueueStatus.ASSIGNED,
                        expiresBefore
                );

        int expiredCount = 0;
        int failureCount = 0;

        for (Long roomId : roomIds) {
            try {
                speakingQueueService.expireCurrentSpeakerIfTimedOut(
                        roomId,
                        now,
                        speakingTimeLimit
                );
                expiredCount++;
            } catch (CustomException e) {
                failureCount++;
            }
        }

        long elapsedMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
        long avgPerRoomMs = roomIds.isEmpty() ? 0 : elapsedMs / roomIds.size();

        return new LoadTestExpirationRunRes(
                roomIds.size(),
                expiredCount,
                failureCount,
                elapsedMs,
                avgPerRoomMs
        );
    }

    @Transactional
    public LoadTestExpirationRacePrepareRes prepareExpirationRace(
            Long roomId,
            Long currentSpeakerUserId,
            Long nextSpeakerUserId
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime requestedAt = now.minusMinutes(10);
        LocalDateTime expiredAssignedAt = now.minusSeconds(speakingTimeLimitSeconds + 60);

        jdbcTemplate.update("delete from speaking_queue where room_id = ?", roomId);

        upsertRoom(roomId);
        upsertUsers(currentSpeakerUserId, 1);
        upsertUsers(nextSpeakerUserId, 1);

        insertSpeakingQueue(
                roomId,
                currentSpeakerUserId,
                1,
                SpeakingQueueStatus.ASSIGNED,
                requestedAt,
                expiredAssignedAt
        );
        insertSpeakingQueue(
                roomId,
                nextSpeakerUserId,
                2,
                SpeakingQueueStatus.WAITING,
                requestedAt,
                null
        );

        return new LoadTestExpirationRacePrepareRes(
                roomId,
                currentSpeakerUserId,
                nextSpeakerUserId,
                1,
                1
        );
    }

    @Transactional(readOnly = true)
    public LoadTestExpirationRaceVerifyRes verifyExpirationRace(Long roomId) {
        int completedCount = countQueueByRoomAndStatus(roomId, SpeakingQueueStatus.COMPLETED);
        int expiredCount = countQueueByRoomAndStatus(roomId, SpeakingQueueStatus.EXPIRED);
        int assignedCount = countQueueByRoomAndStatus(roomId, SpeakingQueueStatus.ASSIGNED);
        int waitingCount = countQueueByRoomAndStatus(roomId, SpeakingQueueStatus.WAITING);
        int terminalCount = completedCount + expiredCount;
        boolean valid = terminalCount == 1 && assignedCount == 1 && waitingCount == 0;

        return new LoadTestExpirationRaceVerifyRes(
                roomId,
                terminalCount,
                completedCount,
                expiredCount,
                assignedCount,
                waitingCount,
                valid
        );
    }

    private void upsertRoom(Long roomId) {
        jdbcTemplate.update(
                """
                        insert into rooms (id, status)
                        values (?, 'OPEN')
                        on duplicate key update status = 'OPEN'
                        """,
                roomId
        );
    }

    private void upsertUsers(Long userIdStart, int userCount) {
        for (long userId = userIdStart; userId < userIdStart + userCount; userId++) {
            jdbcTemplate.update(
                    """
                            insert into users (id, status)
                            values (?, 'ACTIVE')
                            on duplicate key update status = 'ACTIVE'
                            """,
                    userId
            );
        }
    }

    private void insertSpeakingQueue(
            Long roomId,
            Long userId,
            int queueOrder,
            SpeakingQueueStatus status,
            LocalDateTime requestedAt,
            LocalDateTime assignedAt
    ) {
        jdbcTemplate.update(
                """
                        insert into speaking_queue (
                            room_id,
                            user_id,
                            queue_order,
                            status,
                            requested_at,
                            assigned_at
                        )
                        values (?, ?, ?, ?, ?, ?)
                        """,
                roomId,
                userId,
                queueOrder,
                status.name(),
                requestedAt,
                assignedAt
        );
    }

    private int countQueueByRoomAndStatus(Long roomId, SpeakingQueueStatus status) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from speaking_queue
                        where room_id = ?
                          and status = ?
                        """,
                Integer.class,
                roomId,
                status.name()
        );

        return count == null ? 0 : count;
    }
}
