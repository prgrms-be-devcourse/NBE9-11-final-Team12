-- Performance test seed data for local/test DB only.
-- 목적: 10개 토론방 × 방당 100명 동시접속 규모의 k6 테스트 데이터를 준비한다.
-- 실행 전제: MySQL local/test DB. 운영 DB에서 실행 금지.
-- 기본 ID 범위:
--   users: 100000~101199
--   topics: 900001~900010
--   rooms: 900001~900010
--   speeches: 910001~910500
-- 실행 예:
--   docker exec -i sisibibi-mysql mysql -uroot -proot sisibibi < performance/sql/seed-performance-data.sql

SET @password_hash = (SELECT password FROM users ORDER BY id LIMIT 1);
SET @now = NOW();

DROP PROCEDURE IF EXISTS seed_sisibibi_performance_data;
DELIMITER $$
CREATE PROCEDURE seed_sisibibi_performance_data()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE room_offset INT DEFAULT 0;
    DECLARE user_offset INT DEFAULT 0;
    DECLARE speech_offset INT DEFAULT 0;
    DECLARE reaction_offset INT DEFAULT 0;
    DECLARE current_room_id BIGINT;
    DECLARE current_user_id BIGINT;
    DECLARE current_speech_id BIGINT;

    WHILE i < 1200 DO
        SET current_user_id = 100000 + i;
        INSERT INTO users (id, email, password, nickname, role, status, token_version, created_at, updated_at)
        VALUES (
            current_user_id,
            CONCAT('perf-user-', current_user_id, '@sisibibi.test'),
            @password_hash,
            CONCAT('성능유저', current_user_id),
            'USER',
            'ACTIVE',
            0,
            @now,
            @now
        )
        ON DUPLICATE KEY UPDATE
            status = 'ACTIVE',
            token_version = 0,
            updated_at = VALUES(updated_at);
        SET i = i + 1;
    END WHILE;

    SET room_offset = 0;
    WHILE room_offset < 10 DO
        SET current_room_id = 900001 + room_offset;

        INSERT INTO topics (id, title, description, category, source_url, status, created_at, approved_at, approved_by)
        VALUES (
            current_room_id,
            CONCAT('[PERF] 성능 테스트 토픽 ', room_offset + 1),
            '성능 테스트 전용 토픽입니다.',
            'PERFORMANCE',
            'https://example.com/performance',
            'APPROVED',
            @now,
            @now,
            1
        )
        ON DUPLICATE KEY UPDATE
            title = VALUES(title),
            status = 'APPROVED',
            approved_at = VALUES(approved_at);

        INSERT INTO rooms (id, topic_id, title, status, started_at, ended_at, max_participants, created_at)
        VALUES (
            current_room_id,
            current_room_id,
            CONCAT('[PERF] 성능 테스트 토론방 ', room_offset + 1),
            'OPEN',
            DATE_SUB(@now, INTERVAL 5 MINUTE),
            DATE_ADD(@now, INTERVAL 2 HOUR),
            100,
            @now
        )
        ON DUPLICATE KEY UPDATE
            title = VALUES(title),
            status = 'OPEN',
            ended_at = VALUES(ended_at),
            max_participants = 100;

        INSERT INTO room_queue_sequences (room_id, next_queue_order, created_at, updated_at)
        VALUES (current_room_id, 1, @now, @now)
        ON DUPLICATE KEY UPDATE
            next_queue_order = 1,
            updated_at = VALUES(updated_at);

        SET user_offset = 0;
        WHILE user_offset < 100 DO
            SET current_user_id = 100000 + room_offset * 100 + user_offset;
            INSERT INTO room_participants (room_id, user_id, joined_at, left_at, status)
            VALUES (current_room_id, current_user_id, @now, NULL, 'JOINED')
            ON DUPLICATE KEY UPDATE
                joined_at = VALUES(joined_at),
                left_at = NULL,
                status = 'JOINED';
            SET user_offset = user_offset + 1;
        END WHILE;

        SET speech_offset = 0;
        WHILE speech_offset < 50 DO
            SET current_user_id = 100000 + room_offset * 100 + (speech_offset % 100);
            SET current_speech_id = 910001 + room_offset * 50 + speech_offset;

            INSERT INTO speeches (id, room_id, user_id, content, stance, link_url, image_url, status, started_at, ended_at, created_at, updated_at, is_deleted, delete_reason, deleted_at)
            VALUES (
                current_speech_id,
                current_room_id,
                current_user_id,
                CONCAT('[PERF] 성능 테스트 의견 ', current_room_id, '-', speech_offset + 1),
                IF(speech_offset % 2 = 0, 'PRO', 'CON'),
                NULL,
                NULL,
                'COMPLETED',
                DATE_SUB(@now, INTERVAL 3 MINUTE),
                DATE_SUB(@now, INTERVAL 1 MINUTE),
                DATE_SUB(@now, INTERVAL speech_offset SECOND),
                @now,
                FALSE,
                NULL,
                NULL
            )
            ON DUPLICATE KEY UPDATE
                room_id = VALUES(room_id),
                user_id = VALUES(user_id),
                content = VALUES(content),
                stance = VALUES(stance),
                status = 'COMPLETED',
                started_at = VALUES(started_at),
                ended_at = VALUES(ended_at),
                created_at = VALUES(created_at),
                updated_at = VALUES(updated_at),
                is_deleted = FALSE,
                delete_reason = NULL,
                deleted_at = NULL;

            SET reaction_offset = 0;
            WHILE reaction_offset < 20 DO
                SET current_user_id = 100000 + room_offset * 100 + ((speech_offset + reaction_offset + 1) % 100);
                INSERT IGNORE INTO speech_reactions (speech_id, user_id, created_at)
                VALUES (current_speech_id, current_user_id, @now);
                SET reaction_offset = reaction_offset + 1;
            END WHILE;

            SET speech_offset = speech_offset + 1;
        END WHILE;

        SET room_offset = room_offset + 1;
    END WHILE;
END$$
DELIMITER ;

CALL seed_sisibibi_performance_data();
DROP PROCEDURE IF EXISTS seed_sisibibi_performance_data;

SELECT 'performance users' AS label, COUNT(*) AS count FROM users WHERE id BETWEEN 100000 AND 101199
UNION ALL
SELECT 'performance rooms', COUNT(*) FROM rooms WHERE id BETWEEN 900001 AND 900010
UNION ALL
SELECT 'performance queue sequences', COUNT(*) FROM room_queue_sequences WHERE room_id BETWEEN 900001 AND 900010
UNION ALL
SELECT 'performance participants', COUNT(*) FROM room_participants WHERE room_id BETWEEN 900001 AND 900010
UNION ALL
SELECT 'performance speeches', COUNT(*) FROM speeches WHERE content LIKE '[PERF] 성능 테스트 의견%'
UNION ALL
SELECT 'performance reactions', COUNT(*) FROM speech_reactions WHERE speech_id IN (SELECT id FROM speeches WHERE content LIKE '[PERF] 성능 테스트 의견%');
