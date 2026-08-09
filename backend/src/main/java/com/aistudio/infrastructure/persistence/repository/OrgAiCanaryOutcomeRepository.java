package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.OrgAiCanaryOutcomeEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgAiCanaryOutcomeRepository extends JpaRepository<OrgAiCanaryOutcomeEntity, UUID> {
}
