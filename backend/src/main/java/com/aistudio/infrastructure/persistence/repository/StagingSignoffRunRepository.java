package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.StagingSignoffRunEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StagingSignoffRunRepository extends JpaRepository<StagingSignoffRunEntity, UUID> {

    List<StagingSignoffRunEntity> findTop20ByOrderByCreatedAtDesc();

    Optional<StagingSignoffRunEntity> findFirstByImageTagAndOverallAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            String imageTag,
            String overall,
            Instant since
    );
}
