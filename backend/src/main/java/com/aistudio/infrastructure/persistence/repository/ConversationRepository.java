package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.domain.ai.AssistantRole;
import com.aistudio.infrastructure.persistence.entity.ConversationEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {
    List<ConversationEntity> findByProjectIdAndAssistantRoleOrderByUpdatedAtDesc(
            UUID projectId,
            AssistantRole assistantRole
    );

    List<ConversationEntity> findByProjectIdOrderByUpdatedAtDesc(UUID projectId);

    @Query(
            value = """
                    SELECT DISTINCT c.* FROM conversations c
                    WHERE c.project_id = :projectId
                    AND (
                      LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%'))
                      OR EXISTS (
                        SELECT 1 FROM messages m
                        WHERE m.conversation_id = c.id
                        AND LOWER(m.content) LIKE LOWER(CONCAT('%', :q, '%'))
                      )
                    )
                    ORDER BY c.updated_at DESC
                    """,
            nativeQuery = true
    )
    List<ConversationEntity> searchByProjectId(@Param("projectId") UUID projectId, @Param("q") String q);

    @Query(
            value = """
                    SELECT DISTINCT c.* FROM conversations c
                    WHERE c.project_id = :projectId
                    AND c.assistant_role = :assistantRole
                    AND (
                      LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%'))
                      OR EXISTS (
                        SELECT 1 FROM messages m
                        WHERE m.conversation_id = c.id
                        AND LOWER(m.content) LIKE LOWER(CONCAT('%', :q, '%'))
                      )
                    )
                    ORDER BY c.updated_at DESC
                    """,
            nativeQuery = true
    )
    List<ConversationEntity> searchByProjectIdAndAssistantRole(
            @Param("projectId") UUID projectId,
            @Param("assistantRole") String assistantRole,
            @Param("q") String q
    );

    Optional<ConversationEntity> findByShareTokenHash(String shareTokenHash);
}
