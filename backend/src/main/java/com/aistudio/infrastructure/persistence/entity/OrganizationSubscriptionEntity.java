package com.aistudio.infrastructure.persistence.entity;

import com.aistudio.domain.billing.PlanCode;
import com.aistudio.domain.billing.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "organization_subscriptions")
@Getter
@Setter
public class OrganizationSubscriptionEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false, unique = true)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_code", nullable = false, length = 20)
    private PlanCode planCode = PlanCode.FREE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "external_customer_id", length = 120)
    private String externalCustomerId;

    @Column(name = "external_subscription_id", length = 120)
    private String externalSubscriptionId;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "stripe_base_subscription_item_id", length = 120)
    private String stripeBaseSubscriptionItemId;

    @Column(name = "stripe_seat_subscription_item_id", length = 120)
    private String stripeSeatSubscriptionItemId;

    @Column(name = "stripe_ai_overage_subscription_item_id", length = 120)
    private String stripeAiOverageSubscriptionItemId;

    @Column(name = "stripe_metered_usage_synced_at")
    private Instant stripeMeteredUsageSyncedAt;

    @Column(name = "daily_token_budget")
    private Long dailyTokenBudget;

    @Column(name = "ai_provider_chain", length = 255)
    private String aiProviderChain;

    @Column(name = "ai_model_map", length = 512)
    private String aiModelMap;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (planCode == null) {
            planCode = PlanCode.FREE;
        }
        if (status == null) {
            status = SubscriptionStatus.ACTIVE;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
