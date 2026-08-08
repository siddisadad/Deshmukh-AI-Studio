package com.aistudio.infrastructure.billing;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public int getOverageCount(UUID organizationId, LocalDate date) {
        List<Integer> rows = jdbcTemplate.query(
                "SELECT overage_count FROM ai_usage_daily WHERE organization_id = ? AND usage_date = ?",
                (rs, rowNum) -> rs.getInt(1),
                organizationId,
                java.sql.Date.valueOf(date)
        );
        return rows.isEmpty() ? 0 : rows.getFirst();
    }

    public void incrementIncluded(UUID organizationId, LocalDate date) {
        jdbcTemplate.update(
                """
                        INSERT INTO ai_usage_daily (organization_id, usage_date, action_count, overage_count)
                        VALUES (?, ?, 1, 0)
                        ON CONFLICT (organization_id, usage_date)
                        DO UPDATE SET action_count = ai_usage_daily.action_count + 1
                        """,
                organizationId,
                java.sql.Date.valueOf(date)
        );
    }

    public void incrementOverage(UUID organizationId, LocalDate date) {
        jdbcTemplate.update(
                """
                        INSERT INTO ai_usage_daily (organization_id, usage_date, action_count, overage_count)
                        VALUES (?, ?, 0, 1)
                        ON CONFLICT (organization_id, usage_date)
                        DO UPDATE SET overage_count = ai_usage_daily.overage_count + 1
                        """,
                organizationId,
                java.sql.Date.valueOf(date)
        );
    }

    public Map<LocalDate, Integer> getCountsBetween(UUID organizationId, LocalDate from, LocalDate to) {
        Map<LocalDate, Integer> counts = new HashMap<>();
        jdbcTemplate.query(
                """
                        SELECT usage_date, action_count
                        FROM ai_usage_daily
                        WHERE organization_id = ? AND usage_date >= ? AND usage_date <= ?
                        ORDER BY usage_date
                        """,
                (rs, rowNum) -> counts.put(
                        rs.getDate("usage_date").toLocalDate(),
                        rs.getInt("action_count")
                ),
                organizationId,
                java.sql.Date.valueOf(from),
                java.sql.Date.valueOf(to)
        );
        return counts;
    }

    public Map<LocalDate, Integer> getOverageCountsBetween(UUID organizationId, LocalDate from, LocalDate to) {
        Map<LocalDate, Integer> counts = new HashMap<>();
        jdbcTemplate.query(
                """
                        SELECT usage_date, overage_count
                        FROM ai_usage_daily
                        WHERE organization_id = ? AND usage_date >= ? AND usage_date <= ?
                        ORDER BY usage_date
                        """,
                (rs, rowNum) -> counts.put(
                        rs.getDate("usage_date").toLocalDate(),
                        rs.getInt("overage_count")
                ),
                organizationId,
                java.sql.Date.valueOf(from),
                java.sql.Date.valueOf(to)
        );
        return counts;
    }

    public int sumOverageBetween(UUID organizationId, LocalDate from, LocalDate to) {
        Integer total = jdbcTemplate.queryForObject(
                """
                        SELECT COALESCE(SUM(overage_count), 0)
                        FROM ai_usage_daily
                        WHERE organization_id = ? AND usage_date >= ? AND usage_date <= ?
                        """,
                Integer.class,
                organizationId,
                java.sql.Date.valueOf(from),
                java.sql.Date.valueOf(to)
        );
        return total == null ? 0 : total;
    }

    public int sumActionsOnDate(LocalDate date) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(action_count), 0) FROM ai_usage_daily WHERE usage_date = ?",
                Integer.class,
                java.sql.Date.valueOf(date)
        );
        return total == null ? 0 : total;
    }

    public int sumOverageOnDate(LocalDate date) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(overage_count), 0) FROM ai_usage_daily WHERE usage_date = ?",
                Integer.class,
                java.sql.Date.valueOf(date)
        );
        return total == null ? 0 : total;
    }

    public int sumOverageBetweenAllOrgs(LocalDate from, LocalDate to) {
        Integer total = jdbcTemplate.queryForObject(
                """
                        SELECT COALESCE(SUM(overage_count), 0)
                        FROM ai_usage_daily
                        WHERE usage_date >= ? AND usage_date <= ?
                        """,
                Integer.class,
                java.sql.Date.valueOf(from),
                java.sql.Date.valueOf(to)
        );
        return total == null ? 0 : total;
    }

    public int sumEstimatedOverageCentsBetweenAllOrgs(LocalDate from, LocalDate to) {
        Integer total = jdbcTemplate.queryForObject(
                """
                        SELECT COALESCE(SUM(u.overage_count * p.price_cents_per_ai_action_overage), 0)
                        FROM ai_usage_daily u
                        INNER JOIN organization_subscriptions os ON os.organization_id = u.organization_id
                        INNER JOIN plans p ON p.code = os.plan_code
                        WHERE u.usage_date >= ? AND u.usage_date <= ?
                        """,
                Integer.class,
                java.sql.Date.valueOf(from),
                java.sql.Date.valueOf(to)
        );
        return total == null ? 0 : total;
    }
}
