package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.BillingReconciliationRunEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingReconciliationRunRepository extends JpaRepository<BillingReconciliationRunEntity, UUID> {
    Optional<BillingReconciliationRunEntity> findFirstByOrderByCreatedAtDesc();
}
