package com.aistudio.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "billing_reconciliation_runs")
@Getter
@Setter
public class BillingReconciliationRunEntity {

    @Id
    private UUID id;

    @Column(name = "processed_orgs", nullable = false)
    private int processedOrgs;

    @Column(name = "matched_orgs", nullable = false)
    private int matchedOrgs;

    @Column(name = "mismatched_orgs", nullable = false)
    private int mismatchedOrgs;

    @Column(name = "total_internal_cents", nullable = false)
    private long totalInternalCents;

    @Column(name = "total_stripe_cents", nullable = false)
    private long totalStripeCents;

    @Column(name = "tolerance_cents", nullable = false)
    private long toleranceCents;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
