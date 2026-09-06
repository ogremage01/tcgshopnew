-- Allows sync jobs to store longer failure/detail messages.
-- Run once on databases that are not managed by Hibernate ddl-auto:update.
ALTER TABLE sync_log
MODIFY message TEXT;
