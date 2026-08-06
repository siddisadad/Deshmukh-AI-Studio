package com.aistudio.api.billing.dto;

import java.util.List;

public record PlanResponse(
        String code,
        String name,
        int priceCentsMonthly,
        int maxProjects,
        int maxAiActionsPerDay,
        List<String> features
) {
}
