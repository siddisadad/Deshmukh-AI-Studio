package com.aistudio.infrastructure.persistence.entity;

import com.aistudio.domain.ai.OrgAiPolicyChangeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "org_ai_policy_changes")
@Getter
@Setter
public class OrgAiPolicyChangeEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrgAiPolicyChangeStatus status;

    @Column(name = "proposed_by_user_id", nullable = false)
    private UUID proposedByUserId;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;

    @Column(name = "provider_chain", length = 255)
    private String providerChain;

    @Column(name = "daily_token_budget")
    private Long dailyTokenBudget;

    @Column(name = "model_map", length = 512)
    private String modelMap;

    @Column(name = "deploy_region", length = 64)
    private String deployRegion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_policy", nullable = false, columnDefinition = "jsonb")
    private String previousPolicy = "{}";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (previousPolicy == null) {
            previousPolicy = "{}";
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
