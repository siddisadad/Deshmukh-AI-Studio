package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.PlanEntity;
import com.aistudio.domain.billing.PlanCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<PlanEntity, PlanCode> {
    Optional<PlanEntity> findByStripePriceId(String stripePriceId);
}
