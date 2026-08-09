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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "org_ai_policy_simulations")
@Getter
@Setter
public class OrgAiPolicySimulationEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "simulated_by_user_id", nullable = false)
    private UUID simulatedByUserId;

    @Column(name = "provider_chain", length = 255)
    private String providerChain;

    @Column(name = "daily_token_budget")
    private Long dailyTokenBudget;

    @Column(name = "model_map", length = 512)
    private String modelMap;

    @Column(name = "deploy_region", length = 64)
    private String deployRegion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_providers", nullable = false, columnDefinition = "jsonb")
    private String missingProviders = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "current_effective_chain", nullable = false, columnDefinition = "jsonb")
    private String currentEffectiveChain = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "simulated_effective_chain", nullable = false, columnDefinition = "jsonb")
    private String simulatedEffectiveChain = "[]";

    @Column(name = "gate_passed", nullable = false)
    private boolean gatePassed;

    @Column(name = "applied_change_id")
    private UUID appliedChangeId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (missingProviders == null) {
            missingProviders = "[]";
        }
        if (currentEffectiveChain == null) {
            currentEffectiveChain = "[]";
        }
        if (simulatedEffectiveChain == null) {
            simulatedEffectiveChain = "[]";
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
