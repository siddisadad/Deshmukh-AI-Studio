package com.aistudio.application.audit;

import com.aistudio.infrastructure.persistence.entity.AuditLogEntity;
import com.aistudio.infrastructure.persistence.repository.AuditLogRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(UUID actorUserId, String action, String entityType, UUID entityId, String detailsJson, String ip) {
        AuditLogEntity log = new AuditLogEntity();
        log.setActorUserId(actorUserId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(detailsJson == null ? "{}" : detailsJson);
        log.setIpAddress(ip);
        auditLogRepository.save(log);
    }
}
