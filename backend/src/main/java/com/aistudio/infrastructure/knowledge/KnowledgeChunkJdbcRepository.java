package com.aistudio.infrastructure.knowledge;

import com.aistudio.application.knowledge.KnowledgeChunkHit;
import com.aistudio.domain.knowledge.KnowledgeSourceType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class KnowledgeChunkJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public KnowledgeChunkJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void deleteByProjectId(UUID projectId) {
        jdbcTemplate.update("DELETE FROM knowledge_chunks WHERE project_id = ?", projectId);
    }

    public void deleteBySource(UUID projectId, KnowledgeSourceType sourceType, UUID sourceId) {
        jdbcTemplate.update(
                "DELETE FROM knowledge_chunks WHERE project_id = ? AND source_type = ? AND source_id = ?",
                projectId,
                sourceType.name(),
                sourceId
        );
    }

    public int countByProjectId(UUID projectId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_chunks WHERE project_id = ?",
                Integer.class,
                projectId
        );
        return count == null ? 0 : count;
    }

    public void insertChunks(
            UUID projectId,
            KnowledgeSourceType sourceType,
            UUID sourceId,
            List<String> titles,
            List<String> contents,
            List<float[]> embeddings
    ) {
        if (contents.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO knowledge_chunks
                            (id, project_id, source_type, source_id, chunk_index, title, content, embedding, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS vector), ?, ?)
                        """,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setObject(1, UUID.randomUUID());
                        ps.setObject(2, projectId);
                        ps.setString(3, sourceType.name());
                        ps.setObject(4, sourceId);
                        ps.setInt(5, i);
                        ps.setString(6, titles.get(i));
                        ps.setString(7, contents.get(i));
                        ps.setString(8, toVectorLiteral(embeddings.get(i)));
                        ps.setTimestamp(9, Timestamp.from(now));
                        ps.setTimestamp(10, Timestamp.from(now));
                    }

                    @Override
                    public int getBatchSize() {
                        return contents.size();
                    }
                }
        );
    }

    public List<KnowledgeChunkHit> search(UUID projectId, float[] query, int topK) {
        String vector = toVectorLiteral(query);
        return jdbcTemplate.query(
                """
                        SELECT id, source_type, source_id, title, content,
                               (1 - (embedding <=> CAST(? AS vector))) AS score
                        FROM knowledge_chunks
                        WHERE project_id = ?
                        ORDER BY embedding <=> CAST(? AS vector)
                        LIMIT ?
                        """,
                (rs, rowNum) -> new KnowledgeChunkHit(
                        rs.getObject("id", UUID.class),
                        KnowledgeSourceType.valueOf(rs.getString("source_type")),
                        rs.getObject("source_id", UUID.class),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getDouble("score")
                ),
                vector,
                projectId,
                vector,
                topK
        );
    }

    static String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder(embedding.length * 8);
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
