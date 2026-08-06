package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.MembershipEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<MembershipEntity, UUID> {
    List<MembershipEntity> findByUserId(UUID userId);
    Optional<MembershipEntity> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);
}
