package com.aistudio.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "org_ai_canary_outcomes")
@Getter
@Setter
public class OrgAiCanaryOutcomeEntity {

    @Id
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "canary_success_count", nullable = false)
    private long canarySuccessCount = 0L;

    @Column(name = "canary_failure_count", nullable = false)
    private long canaryFailureCount = 0L;

    @Column(name = "stable_success_count", nullable = false)
    private long stableSuccessCount = 0L;

    @Column(name = "stable_failure_count", nullable = false)
    private long stableFailureCount = 0L;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
