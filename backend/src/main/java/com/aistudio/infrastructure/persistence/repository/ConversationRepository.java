package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.domain.ai.AssistantRole;
import com.aistudio.infrastructure.persistence.entity.ConversationEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {
    List<ConversationEntity> findByProjectIdAndAssistantRoleOrderByUpdatedAtDesc(
            UUID projectId,
            AssistantRole assistantRole
    );

    List<ConversationEntity> findByProjectIdOrderByUpdatedAtDesc(UUID projectId);
}
