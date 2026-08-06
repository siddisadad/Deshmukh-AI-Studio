package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.PlanEntity;
import com.aistudio.domain.billing.PlanCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<PlanEntity, PlanCode> {
}
