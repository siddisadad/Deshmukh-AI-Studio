package com.aistudio.application.ai;

import com.aistudio.infrastructure.persistence.entity.OrganizationSubscriptionEntity;
import com.aistudio.infrastructure.persistence.repository.OrganizationSubscriptionRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrgAiRoutingPolicyService {

    private final OrganizationSubscriptionRepository subscriptionRepository;

    public OrgAiRoutingPolicyService(OrganizationSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public Optional<List<String>> resolveChain(UUID organizationId) {
        if (organizationId == null) {
            return Optional.empty();
        }
        return subscriptionRepository.findByOrganizationId(organizationId)
                .map(OrganizationSubscriptionEntity::getAiProviderChain)
                .filter(chain -> chain != null && !chain.isBlank())
                .map(OrgAiRoutingPolicyService::parseChain);
    }

    public Optional<Long> dailyTokenBudgetOverride(UUID organizationId) {
        if (organizationId == null) {
            return Optional.empty();
        }
        return subscriptionRepository.findByOrganizationId(organizationId)
                .map(OrganizationSubscriptionEntity::getDailyTokenBudget)
                .filter(budget -> budget != null && budget > 0);
    }

    public Optional<String> providerChainRaw(UUID organizationId) {
        if (organizationId == null) {
            return Optional.empty();
        }
        return subscriptionRepository.findByOrganizationId(organizationId)
                .map(OrganizationSubscriptionEntity::getAiProviderChain)
                .filter(chain -> chain != null && !chain.isBlank());
    }

    private static List<String> parseChain(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toList();
    }
}
