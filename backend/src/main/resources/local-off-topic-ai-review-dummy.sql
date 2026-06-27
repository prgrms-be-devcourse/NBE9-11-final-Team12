-- Local dummy data for checking off-topic AI review UI.
-- Target DB: MySQL configured in backend/src/main/resources/application.yaml.
--
-- Recommended local login when LocalDataInitializer is enabled:
--   admin: local-admin@sisibibi.test / test1234!
--   user : local-user@sisibibi.test / test1234!
--
-- The users inserted below are only stable display targets for report/user IDs.
--
-- This script uses high fixed IDs to avoid most local seed-data collisions.

SET @now = NOW();

INSERT INTO users (
    id,
    email,
    password,
    nickname,
    role,
    status,
    token_version,
    created_at,
    updated_at
) VALUES
    (9901, 'admin-offtopic@example.com', '$2a$10$gK7VJkJfLwQmdUmQWm3BFOxTfKf4JfLlG0Ch3z7Jc4yQ4Z2NDSSP2', '논점검토관리자', 'ADMIN', 'ACTIVE', 0, @now, @now),
    (9902, 'user-offtopic@example.com', '$2a$10$gK7VJkJfLwQmdUmQWm3BFOxTfKf4JfLlG0Ch3z7Jc4yQ4Z2NDSSP2', '토론참여자', 'USER', 'ACTIVE', 0, @now, @now),
    (9903, 'reporter1-offtopic@example.com', '$2a$10$gK7VJkJfLwQmdUmQWm3BFOxTfKf4JfLlG0Ch3z7Jc4yQ4Z2NDSSP2', '신고자1', 'USER', 'ACTIVE', 0, @now, @now),
    (9904, 'reporter2-offtopic@example.com', '$2a$10$gK7VJkJfLwQmdUmQWm3BFOxTfKf4JfLlG0Ch3z7Jc4yQ4Z2NDSSP2', '신고자2', 'USER', 'ACTIVE', 0, @now, @now),
    (9905, 'reporter3-offtopic@example.com', '$2a$10$gK7VJkJfLwQmdUmQWm3BFOxTfKf4JfLlG0Ch3z7Jc4yQ4Z2NDSSP2', '신고자3', 'USER', 'ACTIVE', 0, @now, @now)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    nickname = VALUES(nickname),
    role = VALUES(role),
    status = VALUES(status),
    updated_at = @now;

INSERT INTO topics (
    id,
    title,
    description,
    category,
    source_url,
    status,
    created_at,
    approved_at,
    approved_by
) VALUES
    (
        9901,
        'AI 논점 이탈 검토 프론트 확인용 토픽',
        '신고 목록/상세의 AI 검토 보조 결과와 삭제된 의견 표시를 확인하기 위한 로컬 더미 토픽입니다.',
        '테스트',
        'https://example.com/local-off-topic-ai-review',
        'APPROVED',
        @now - INTERVAL 2 DAY,
        @now - INTERVAL 2 DAY,
        9901
    )
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    description = VALUES(description),
    category = VALUES(category),
    source_url = VALUES(source_url),
    status = VALUES(status),
    approved_at = VALUES(approved_at),
    approved_by = VALUES(approved_by);

INSERT INTO rooms (
    id,
    topic_id,
    title,
    status,
    started_at,
    ended_at,
    max_participants,
    created_at
) VALUES
    (
        9901,
        9901,
        '논점 이탈 AI 검토 UI 확인방',
        'OPEN',
        @now - INTERVAL 1 DAY,
        @now + INTERVAL 7 DAY,
        50,
        @now - INTERVAL 1 DAY
    )
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    status = VALUES(status),
    started_at = VALUES(started_at),
    ended_at = VALUES(ended_at),
    max_participants = VALUES(max_participants);

INSERT INTO speeches (
    id,
    room_id,
    user_id,
    content,
    stance,
    link_url,
    image_url,
    status,
    started_at,
    ended_at,
    created_at,
    updated_at,
    is_deleted,
    delete_reason,
    deleted_at
) VALUES
    (
        9901,
        9901,
        9902,
        '이 토론방에서는 AI 검토 보조 결과가 관리자에게 어떻게 보이는지 확인합니다.',
        'PRO',
        NULL,
        NULL,
        'COMPLETED',
        @now - INTERVAL 5 HOUR,
        @now - INTERVAL 4 HOUR,
        @now - INTERVAL 5 HOUR,
        @now - INTERVAL 4 HOUR,
        0,
        NULL,
        NULL
    ),
    (
        9902,
        9901,
        9902,
        '논점 이탈로 삭제된 의견입니다.',
        'CON',
        NULL,
        NULL,
        'COMPLETED',
        @now - INTERVAL 4 HOUR,
        @now - INTERVAL 3 HOUR,
        @now - INTERVAL 4 HOUR,
        @now - INTERVAL 30 MINUTE,
        1,
        'OFF_TOPIC',
        @now - INTERVAL 30 MINUTE
    ),
    (
        9903,
        9901,
        9902,
        '삭제된 의견입니다.',
        'PRO',
        NULL,
        NULL,
        'COMPLETED',
        @now - INTERVAL 3 HOUR,
        @now - INTERVAL 2 HOUR,
        @now - INTERVAL 3 HOUR,
        @now - INTERVAL 20 MINUTE,
        1,
        'USER_DELETED',
        @now - INTERVAL 20 MINUTE
    ),
    (
        9904,
        9901,
        9902,
        '관리자 신고 상세에서 AI가 논점 이탈 가능성이 낮다고 판단한 케이스입니다.',
        'CON',
        NULL,
        NULL,
        'COMPLETED',
        @now - INTERVAL 2 HOUR,
        @now - INTERVAL 90 MINUTE,
        @now - INTERVAL 2 HOUR,
        @now - INTERVAL 90 MINUTE,
        0,
        NULL,
        NULL
    ),
    (
        9905,
        9901,
        9902,
        'AI 검토가 실패한 신고 케이스를 확인하기 위한 의견입니다.',
        'PRO',
        NULL,
        NULL,
        'COMPLETED',
        @now - INTERVAL 80 MINUTE,
        @now - INTERVAL 70 MINUTE,
        @now - INTERVAL 80 MINUTE,
        @now - INTERVAL 70 MINUTE,
        0,
        NULL,
        NULL
    )
ON DUPLICATE KEY UPDATE
    content = VALUES(content),
    stance = VALUES(stance),
    status = VALUES(status),
    updated_at = VALUES(updated_at),
    is_deleted = VALUES(is_deleted),
    delete_reason = VALUES(delete_reason),
    deleted_at = VALUES(deleted_at);

INSERT INTO speech_reports (
    id,
    speech_id,
    reported_user_id,
    reporter_user_id,
    content_snapshot,
    reason,
    description,
    status,
    reviewed_by,
    reviewed_at,
    resolution_note,
    severity,
    created_at,
    updated_at
) VALUES
    (
        9901,
        9902,
        9902,
        9903,
        '토론 주제와 무관한 상품 홍보와 개인적인 이야기가 반복되어 신고합니다.',
        'OFF_TOPIC',
        '토론 흐름과 관계없는 내용이어서 검토가 필요합니다.',
        'REVIEWING',
        9901,
        NULL,
        NULL,
        NULL,
        @now - INTERVAL 50 MINUTE,
        @now - INTERVAL 45 MINUTE
    ),
    (
        9902,
        9904,
        9902,
        9904,
        '관리자 신고 상세에서 AI가 논점 이탈 가능성이 낮다고 판단한 케이스입니다.',
        'OFF_TOPIC',
        '논점에서 벗어난 것 같아 신고합니다.',
        'PENDING',
        NULL,
        NULL,
        NULL,
        NULL,
        @now - INTERVAL 40 MINUTE,
        @now - INTERVAL 40 MINUTE
    ),
    (
        9903,
        9905,
        9902,
        9905,
        'AI 검토가 실패한 신고 케이스를 확인하기 위한 의견입니다.',
        'OFF_TOPIC',
        'AI 검토 실패 상태 표시 확인용입니다.',
        'PENDING',
        NULL,
        NULL,
        NULL,
        NULL,
        @now - INTERVAL 35 MINUTE,
        @now - INTERVAL 35 MINUTE
    ),
    (
        9904,
        9901,
        9902,
        9904,
        '이 토론방에서는 AI 검토 보조 결과가 관리자에게 어떻게 보이는지 확인합니다.',
        'OTHER',
        'AI 검토 없음 표시 확인용 신고입니다.',
        'PENDING',
        NULL,
        NULL,
        NULL,
        NULL,
        @now - INTERVAL 30 MINUTE,
        @now - INTERVAL 30 MINUTE
    ),
    (
        9905,
        9902,
        9902,
        9905,
        '처리 완료된 신고 상세 표시 확인용입니다.',
        'OFF_TOPIC',
        '이미 처리된 논점 이탈 신고입니다.',
        'RESOLVED',
        9901,
        @now - INTERVAL 10 MINUTE,
        '관리자가 최종 확인하여 논점 이탈로 처리했습니다.',
        'HIGH',
        @now - INTERVAL 25 MINUTE,
        @now - INTERVAL 10 MINUTE
    )
ON DUPLICATE KEY UPDATE
    content_snapshot = VALUES(content_snapshot),
    reason = VALUES(reason),
    description = VALUES(description),
    status = VALUES(status),
    reviewed_by = VALUES(reviewed_by),
    reviewed_at = VALUES(reviewed_at),
    resolution_note = VALUES(resolution_note),
    severity = VALUES(severity),
    updated_at = VALUES(updated_at);

INSERT INTO off_topic_ai_reviews (
    id,
    version,
    speech_id,
    room_id,
    content_snapshot,
    report_count,
    threshold,
    participant_count,
    status,
    is_off_topic,
    confidence,
    reason,
    error_message,
    completed_at,
    created_at,
    updated_at
) VALUES
    (
        9901,
        0,
        9902,
        9901,
        '토론 주제와 무관한 상품 홍보와 개인적인 이야기가 반복되어 신고합니다.',
        5,
        5,
        50,
        'COMPLETED',
        1,
        0.82,
        '발언의 핵심 내용이 토론 주제보다 개인 홍보와 무관한 경험담에 집중되어 있어 논점 이탈 가능성이 있습니다. 최종 판단은 관리자가 신고 맥락과 토론 흐름을 함께 확인해야 합니다.',
        NULL,
        @now - INTERVAL 48 MINUTE,
        @now - INTERVAL 50 MINUTE,
        @now - INTERVAL 48 MINUTE
    ),
    (
        9902,
        0,
        9904,
        9901,
        '관리자 신고 상세에서 AI가 논점 이탈 가능성이 낮다고 판단한 케이스입니다.',
        3,
        5,
        50,
        'COMPLETED',
        0,
        0.28,
        '발언이 다소 우회적으로 표현되었지만 토론 쟁점과 연결되는 의견으로 보입니다. 논점 이탈 가능성은 낮습니다.',
        NULL,
        @now - INTERVAL 38 MINUTE,
        @now - INTERVAL 40 MINUTE,
        @now - INTERVAL 38 MINUTE
    ),
    (
        9903,
        0,
        9905,
        9901,
        'AI 검토가 실패한 신고 케이스를 확인하기 위한 의견입니다.',
        5,
        5,
        50,
        'FAILED',
        NULL,
        NULL,
        NULL,
        '로컬 더미 데이터: AI 검토 실패 상태 확인용입니다.',
        NULL,
        @now - INTERVAL 35 MINUTE,
        @now - INTERVAL 34 MINUTE
    )
ON DUPLICATE KEY UPDATE
    version = VALUES(version),
    content_snapshot = VALUES(content_snapshot),
    report_count = VALUES(report_count),
    threshold = VALUES(threshold),
    participant_count = VALUES(participant_count),
    status = VALUES(status),
    is_off_topic = VALUES(is_off_topic),
    confidence = VALUES(confidence),
    reason = VALUES(reason),
    error_message = VALUES(error_message),
    completed_at = VALUES(completed_at),
    updated_at = VALUES(updated_at);
