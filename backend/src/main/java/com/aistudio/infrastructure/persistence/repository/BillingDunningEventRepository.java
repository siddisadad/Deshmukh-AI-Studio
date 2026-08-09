package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.BillingDunningEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingDunningEventRepository extends JpaRepository<BillingDunningEventEntity, UUID> {
    List<BillingDunningEventEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
