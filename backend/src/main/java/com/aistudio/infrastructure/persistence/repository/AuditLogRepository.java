package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.AuditLogEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
    List<AuditLogEntity> findByActorUserIdOrderByCreatedAtDesc(UUID actorUserId, Pageable pageable);
}
