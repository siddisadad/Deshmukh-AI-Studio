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
                .flatMap(this::resolveChainFromSubscription);
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

    public Optional<String> deployRegionOverride(UUID organizationId) {
        if (organizationId == null) {
            return Optional.empty();
        }
        return subscriptionRepository.findByOrganizationId(organizationId)
                .map(OrganizationSubscriptionEntity::getAiDeployRegion)
                .filter(region -> region != null && !region.isBlank());
    }

    public Optional<String> canaryProviderChainRaw(UUID organizationId) {
        if (organizationId == null) {
            return Optional.empty();
        }
        return subscriptionRepository.findByOrganizationId(organizationId)
                .map(OrganizationSubscriptionEntity::getAiCanaryProviderChain)
                .filter(chain -> chain != null && !chain.isBlank());
    }

    public Optional<Integer> canaryPercent(UUID organizationId) {
        if (organizationId == null) {
            return Optional.empty();
        }
        return subscriptionRepository.findByOrganizationId(organizationId)
                .map(OrganizationSubscriptionEntity::getAiCanaryPercent)
                .filter(percent -> percent != null && percent > 0);
    }

    private Optional<List<String>> resolveChainFromSubscription(OrganizationSubscriptionEntity sub) {
        if (shouldUseCanary(sub)) {
            return Optional.of(parseChain(sub.getAiCanaryProviderChain()));
        }
        String stable = sub.getAiProviderChain();
        if (stable != null && !stable.isBlank()) {
            return Optional.of(parseChain(stable));
        }
        return Optional.empty();
    }

    private boolean shouldUseCanary(OrganizationSubscriptionEntity sub) {
        String canaryChain = sub.getAiCanaryProviderChain();
        Integer percent = sub.getAiCanaryPercent();
        if (canaryChain == null || canaryChain.isBlank() || percent == null || percent <= 0) {
            return false;
        }
        if (percent >= 100) {
            return true;
        }
        long bucket = routingBucket(sub.getOrganizationId());
        return bucket < percent;
    }

    private static long routingBucket(UUID organizationId) {
        UUID conversationId = OrgAiRoutingContext.conversationId();
        long hash = 17L;
        hash = 31L * hash + organizationId.getMostSignificantBits();
        hash = 31L * hash + organizationId.getLeastSignificantBits();
        if (conversationId != null) {
            hash = 31L * hash + conversationId.getMostSignificantBits();
            hash = 31L * hash + conversationId.getLeastSignificantBits();
        }
        return Math.abs(hash % 100);
    }

    private static List<String> parseChain(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toList();
    }
}
