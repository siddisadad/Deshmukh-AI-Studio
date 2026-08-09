package com.aistudio.infrastructure.metrics;

public final class OrgSloContext {

    private static final ThreadLocal<String> ORGANIZATION_ID = new ThreadLocal<>();

    private OrgSloContext() {
    }

    public static void setOrganizationId(String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            ORGANIZATION_ID.remove();
        } else {
            ORGANIZATION_ID.set(organizationId);
        }
    }

    public static String organizationId() {
        return ORGANIZATION_ID.get();
    }

    public static void clear() {
        ORGANIZATION_ID.remove();
    }
}
