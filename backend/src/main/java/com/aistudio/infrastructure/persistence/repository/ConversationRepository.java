package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.domain.ai.AssistantRole;
import com.aistudio.infrastructure.persistence.entity.ConversationEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {
    Optional<ConversationEntity> findByProjectIdAndAssistantRole(UUID projectId, AssistantRole assistantRole);
}
