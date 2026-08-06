package com.aistudio.infrastructure.billing;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AiUsageJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public AiUsageJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int getCount(UUID organizationId, LocalDate date) {
        List<Integer> rows = jdbcTemplate.query(
                "SELECT action_count FROM ai_usage_daily WHERE organization_id = ? AND usage_date = ?",
                (rs, rowNum) -> rs.getInt(1),
                organizationId,
                java.sql.Date.valueOf(date)
        );
        return rows.isEmpty() ? 0 : rows.getFirst();
    }

    public void increment(UUID organizationId, LocalDate date) {
        jdbcTemplate.update(
                """
                        INSERT INTO ai_usage_daily (organization_id, usage_date, action_count)
                        VALUES (?, ?, 1)
                        ON CONFLICT (organization_id, usage_date)
                        DO UPDATE SET action_count = ai_usage_daily.action_count + 1
                        """,
                organizationId,
                java.sql.Date.valueOf(date)
        );
    }
}
