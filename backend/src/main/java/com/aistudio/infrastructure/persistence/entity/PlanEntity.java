package com.aistudio.infrastructure.persistence.entity;

import com.aistudio.domain.billing.PlanCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "plans")
@Getter
@Setter
public class PlanEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PlanCode code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "price_cents_monthly", nullable = false)
    private int priceCentsMonthly;

    @Column(name = "max_projects", nullable = false)
    private int maxProjects;

    @Column(name = "max_ai_actions_per_day", nullable = false)
    private int maxAiActionsPerDay;

    @Column(name = "max_seats", nullable = false)
    private int maxSeats = 3;

    @Column(name = "price_cents_per_seat_monthly", nullable = false)
    private int priceCentsPerSeatMonthly = 0;

    @Column(name = "price_cents_per_ai_action_overage", nullable = false)
    private int priceCentsPerAiActionOverage = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String features = "[]";

    @Column(name = "stripe_price_id", length = 120)
    private String stripePriceId;
}
