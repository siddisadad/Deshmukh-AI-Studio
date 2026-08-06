package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.domain.context.ContextAssetType;
import com.aistudio.infrastructure.persistence.entity.ContextAssetEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContextAssetRepository extends JpaRepository<ContextAssetEntity, UUID> {
    List<ContextAssetEntity> findByProjectIdOrderByAssetTypeAsc(UUID projectId);

    Optional<ContextAssetEntity> findByProjectIdAndAssetType(UUID projectId, ContextAssetType assetType);
}
