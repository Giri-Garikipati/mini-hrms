-- V3: "version" columns for optimistic locking.
-- Every update increases version by 1. If two people edit the same row at the same
-- time, the second save sees the version has changed and is rejected (HTTP 409)
-- instead of silently overwriting the first person's change.
ALTER TABLE employees      ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE leave_requests ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
