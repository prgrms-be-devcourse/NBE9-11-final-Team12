-- Cleanup performance test data created by seed-performance-data.sql.
-- 실행 예:
--   docker exec -i sisibibi-mysql mysql -uroot -proot sisibibi < performance/sql/cleanup-performance-data.sql

DELETE FROM speech_reactions
WHERE speech_id IN (SELECT id FROM speeches WHERE content LIKE '[PERF] 성능 테스트 의견%');

DELETE FROM speech_reports
WHERE speech_id IN (SELECT id FROM speeches WHERE content LIKE '[PERF] 성능 테스트 의견%');

DELETE FROM chat_reports
WHERE message_id IN (SELECT id FROM chat_messages WHERE room_id BETWEEN 900001 AND 900010);

DELETE FROM chat_messages
WHERE room_id BETWEEN 900001 AND 900010
   OR user_id BETWEEN 100000 AND 101199;

DELETE FROM speaking_queue
WHERE room_id BETWEEN 900001 AND 900010
   OR user_id BETWEEN 100000 AND 101199;

DELETE FROM speeches
WHERE content LIKE '[PERF] 성능 테스트 의견%';

DELETE FROM room_participants
WHERE room_id BETWEEN 900001 AND 900010
   OR user_id BETWEEN 100000 AND 101199;

DELETE FROM room_queue_sequences
WHERE room_id BETWEEN 900001 AND 900010;

DELETE FROM rooms
WHERE id BETWEEN 900001 AND 900010
   OR topic_id BETWEEN 900001 AND 900010;

DELETE FROM topics
WHERE id BETWEEN 900001 AND 900010;

DELETE FROM users
WHERE id BETWEEN 100000 AND 101199;

SELECT 'performance users' AS label, COUNT(*) AS count FROM users WHERE id BETWEEN 100000 AND 101199
UNION ALL
SELECT 'performance rooms', COUNT(*) FROM rooms WHERE id BETWEEN 900001 AND 900010
UNION ALL
SELECT 'performance queue sequences', COUNT(*) FROM room_queue_sequences WHERE room_id BETWEEN 900001 AND 900010
UNION ALL
SELECT 'performance speeches', COUNT(*) FROM speeches WHERE content LIKE '[PERF] 성능 테스트 의견%'
UNION ALL
SELECT 'performance chat messages', COUNT(*) FROM chat_messages WHERE room_id BETWEEN 900001 AND 900010;
