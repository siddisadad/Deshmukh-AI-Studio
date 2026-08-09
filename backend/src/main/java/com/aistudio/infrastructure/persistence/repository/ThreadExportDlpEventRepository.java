package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.ThreadExportDlpEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ThreadExportDlpEventRepository extends JpaRepository<ThreadExportDlpEventEntity, UUID> {

    List<ThreadExportDlpEventEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    @Query("""
            SELECT e FROM ThreadExportDlpEventEntity e
            WHERE e.siemExportedAt IS NULL
            ORDER BY e.createdAt ASC
            """)
    List<ThreadExportDlpEventEntity> findPendingSiemExport();
}
