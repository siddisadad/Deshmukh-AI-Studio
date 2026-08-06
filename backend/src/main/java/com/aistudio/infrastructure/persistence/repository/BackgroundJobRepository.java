package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.domain.job.JobStatus;
import com.aistudio.infrastructure.persistence.entity.BackgroundJobEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BackgroundJobRepository extends JpaRepository<BackgroundJobEntity, UUID> {
    List<BackgroundJobEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);

    @Query("""
            select j from BackgroundJobEntity j
            where j.status = com.aistudio.domain.job.JobStatus.PENDING
            order by j.createdAt asc
            """)
    List<BackgroundJobEntity> findPending(Pageable pageable);

    long countByProjectIdAndStatus(UUID projectId, JobStatus status);
}
