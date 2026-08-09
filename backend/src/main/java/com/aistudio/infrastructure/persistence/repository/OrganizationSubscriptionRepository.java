package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.domain.billing.PlanCode;
import com.aistudio.infrastructure.persistence.entity.OrganizationSubscriptionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrganizationSubscriptionRepository extends JpaRepository<OrganizationSubscriptionEntity, UUID> {
    Optional<OrganizationSubscriptionEntity> findByOrganizationId(UUID organizationId);

    Optional<OrganizationSubscriptionEntity> findByExternalCustomerId(String externalCustomerId);

    Optional<OrganizationSubscriptionEntity> findByExternalSubscriptionId(String externalSubscriptionId);

    List<OrganizationSubscriptionEntity> findByExternalSubscriptionIdIsNotNullAndPlanCodeIn(List<PlanCode> planCodes);

    @Query("""
            SELECT s FROM OrganizationSubscriptionEntity s
            WHERE s.aiCanaryProviderChain IS NOT NULL
              AND s.aiCanaryPercent IS NOT NULL
              AND s.aiCanaryPercent > 0
              AND (s.aiCanaryAutoPromoteEnabled = true OR s.aiCanaryAutoAbortEnabled = true)
            """)
    List<OrganizationSubscriptionEntity> findCanaryHookCandidates();
}
