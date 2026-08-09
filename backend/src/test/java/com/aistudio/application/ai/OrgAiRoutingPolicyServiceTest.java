package com.aistudio.application.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aistudio.infrastructure.persistence.entity.OrganizationSubscriptionEntity;
import com.aistudio.infrastructure.persistence.repository.OrganizationSubscriptionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrgAiRoutingPolicyServiceTest {

    @Mock OrganizationSubscriptionRepository subscriptionRepository;

    OrgAiRoutingPolicyService service;
    UUID orgId;

    @BeforeEach
    void setUp() {
        service = new OrgAiRoutingPolicyService(subscriptionRepository);
        orgId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        OrgAiRoutingContext.clear();
    }

    @Test
    void resolveChainUsesCanaryWhenPercentIsFull() {
        OrganizationSubscriptionEntity sub = subscription("mock", "mock,openai", 100);
        when(subscriptionRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(sub));

        assertThat(service.resolveChain(orgId)).contains(List.of("mock", "openai"));
    }

    @Test
    void resolveChainUsesStableWhenCanaryInactive() {
        OrganizationSubscriptionEntity sub = subscription("mock", "mock,openai", 0);
        when(subscriptionRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(sub));

        assertThat(service.resolveChain(orgId)).contains(List.of("mock"));
    }

    @Test
    void resolveChainSplitsTrafficByConversation() {
        OrganizationSubscriptionEntity sub = subscription("stable", "canary", 50);
        when(subscriptionRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(sub));

        UUID canaryConversation = null;
        UUID stableConversation = null;
        for (int i = 0; i < 200; i++) {
            UUID conversationId = UUID.randomUUID();
            OrgAiRoutingContext.setConversationId(conversationId);
            String first = service.resolveChain(orgId).orElseThrow().getFirst();
            if ("canary".equals(first)) {
                canaryConversation = conversationId;
            } else {
                stableConversation = conversationId;
            }
            if (canaryConversation != null && stableConversation != null) {
                break;
            }
        }

        assertThat(canaryConversation).isNotNull();
        assertThat(stableConversation).isNotNull();
    }

    private OrganizationSubscriptionEntity subscription(String stable, String canary, int percent) {
        OrganizationSubscriptionEntity sub = new OrganizationSubscriptionEntity();
        sub.setOrganizationId(orgId);
        sub.setAiProviderChain(stable);
        sub.setAiCanaryProviderChain(canary);
        sub.setAiCanaryPercent(percent);
        return sub;
    }
}
