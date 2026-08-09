package com.aistudio.application.ai;

import java.util.UUID;

/**
 * Request-scoped organization and model routing for AI calls.
 */
public final class OrgAiRoutingContext {

    private static final ThreadLocal<UUID> ORGANIZATION_ID = new ThreadLocal<>();
    private static final ThreadLocal<UUID> CONVERSATION_ID = new ThreadLocal<>();
    private static final ThreadLocal<AiModelRoute> MODEL_ROUTE = new ThreadLocal<>();
    private static final ThreadLocal<String> DEPLOY_REGION = new ThreadLocal<>();

    private OrgAiRoutingContext() {
    }

    public static void setOrganizationId(UUID organizationId) {
        ORGANIZATION_ID.set(organizationId);
    }

    public static UUID organizationId() {
        return ORGANIZATION_ID.get();
    }

    public static void setConversationId(UUID conversationId) {
        if (conversationId == null) {
            CONVERSATION_ID.remove();
        } else {
            CONVERSATION_ID.set(conversationId);
        }
    }

    public static UUID conversationId() {
        return CONVERSATION_ID.get();
    }

    public static void setModelRoute(AiModelRoute modelRoute) {
        MODEL_ROUTE.set(modelRoute);
    }

    public static AiModelRoute modelRoute() {
        return MODEL_ROUTE.get();
    }

    public static void setDeployRegion(String deployRegion) {
        if (deployRegion == null || deployRegion.isBlank()) {
            DEPLOY_REGION.remove();
        } else {
            DEPLOY_REGION.set(deployRegion.trim().toLowerCase());
        }
    }

    public static String deployRegion() {
        return DEPLOY_REGION.get();
    }

    public static void clear() {
        ORGANIZATION_ID.remove();
        CONVERSATION_ID.remove();
        MODEL_ROUTE.remove();
        DEPLOY_REGION.remove();
    }
}
