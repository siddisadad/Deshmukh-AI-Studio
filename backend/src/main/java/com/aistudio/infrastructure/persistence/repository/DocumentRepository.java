package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.DocumentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {
    List<DocumentEntity> findByProjectIdOrderByUpdatedAtDesc(UUID projectId);
}
