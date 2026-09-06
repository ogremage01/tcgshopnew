-- point_log.actor_type 백필 (기존 executor 컬럼 기준, MariaDB/MySQL)
-- Hibernate ddl-auto: update 로 actor_type 컬럼이 생긴 뒤 1회 실행

UPDATE point_log
SET actor_type = 'SYSTEM'
WHERE actor_type IS NULL
  AND executor = 'SYSTEM';

UPDATE point_log
SET actor_type = 'ADMIN'
WHERE actor_type IS NULL;
