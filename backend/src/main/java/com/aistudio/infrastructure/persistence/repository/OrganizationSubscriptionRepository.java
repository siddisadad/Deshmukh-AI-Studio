package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.domain.billing.PlanCode;
import com.aistudio.infrastructure.persistence.entity.OrganizationSubscriptionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationSubscriptionRepository extends JpaRepository<OrganizationSubscriptionEntity, UUID> {
    Optional<OrganizationSubscriptionEntity> findByOrganizationId(UUID organizationId);

    Optional<OrganizationSubscriptionEntity> findByExternalCustomerId(String externalCustomerId);

    Optional<OrganizationSubscriptionEntity> findByExternalSubscriptionId(String externalSubscriptionId);

    List<OrganizationSubscriptionEntity> findByExternalSubscriptionIdIsNotNullAndPlanCodeIn(List<PlanCode> planCodes);
}
