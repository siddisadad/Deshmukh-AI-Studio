package com.aistudio.application.job;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BackgroundJobClaimer {

    private final JdbcTemplate jdbcTemplate;

    public BackgroundJobClaimer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UUID> claimNext(int limit, String workerId) {
        return jdbcTemplate.query(
                """
                UPDATE background_jobs
                SET status = 'RUNNING',
                    attempts = attempts + 1,
                    started_at = NOW(),
                    updated_at = NOW(),
                    locked_by = ?,
                    locked_at = NOW(),
                    error_message = NULL
                WHERE id IN (
                    SELECT id FROM background_jobs
                    WHERE status = 'PENDING'
                    ORDER BY created_at
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                )
                RETURNING id
                """,
                (rs, rowNum) -> UUID.fromString(rs.getString("id")),
                workerId,
                limit
        );
    }

    public boolean tryClaim(UUID jobId, String workerId) {
        List<UUID> claimed = jdbcTemplate.query(
                """
                UPDATE background_jobs
                SET status = 'RUNNING',
                    attempts = attempts + 1,
                    started_at = NOW(),
                    updated_at = NOW(),
                    locked_by = ?,
                    locked_at = NOW(),
                    error_message = NULL
                WHERE id = ?
                  AND status = 'PENDING'
                RETURNING id
                """,
                (rs, rowNum) -> UUID.fromString(rs.getString("id")),
                workerId,
                jobId
        );
        return !claimed.isEmpty();
    }

    public int reclaimStaleLocks(int staleSeconds, int maxAttempts) {
        int reclaimed = jdbcTemplate.update(
                """
                UPDATE background_jobs
                SET status = 'PENDING',
                    locked_by = NULL,
                    locked_at = NULL,
                    updated_at = NOW(),
                    error_message = 'Reclaimed stale worker lock'
                WHERE status = 'RUNNING'
                  AND locked_at IS NOT NULL
                  AND locked_at < NOW() - (? * INTERVAL '1 second')
                  AND attempts < ?
                """,
                staleSeconds,
                maxAttempts
        );
        int failed = jdbcTemplate.update(
                """
                UPDATE background_jobs
                SET status = 'FAILED',
                    finished_at = NOW(),
                    updated_at = NOW(),
                    error_message = 'Exceeded max attempts after stale lock'
                WHERE status = 'RUNNING'
                  AND locked_at IS NOT NULL
                  AND locked_at < NOW() - (? * INTERVAL '1 second')
                  AND attempts >= ?
                """,
                staleSeconds,
                maxAttempts
        );
        return reclaimed + failed;
    }
}
