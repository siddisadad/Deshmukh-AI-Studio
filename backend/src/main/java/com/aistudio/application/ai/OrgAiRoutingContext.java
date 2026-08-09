package com.aistudio.application.ai;

import java.util.UUID;

/**
 * Request-scoped organization id for org-level AI routing policy resolution.
 */
public final class OrgAiRoutingContext {

    private static final ThreadLocal<UUID> ORGANIZATION_ID = new ThreadLocal<>();

    private OrgAiRoutingContext() {
    }

    public static void setOrganizationId(UUID organizationId) {
        ORGANIZATION_ID.set(organizationId);
    }

    public static UUID organizationId() {
        return ORGANIZATION_ID.get();
    }

    public static void clear() {
        ORGANIZATION_ID.remove();
    }
}
