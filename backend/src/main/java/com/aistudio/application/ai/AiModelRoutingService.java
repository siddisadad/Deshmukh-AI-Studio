package com.aistudio.application.ai;

import com.aistudio.domain.ai.AssistantRole;
import com.aistudio.infrastructure.ai.AiModelRoutingRegistry;
import com.aistudio.infrastructure.persistence.entity.OrganizationSubscriptionEntity;
import com.aistudio.infrastructure.persistence.repository.OrganizationSubscriptionRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiModelRoutingService {

    private final AiModelRoutingRegistry platformRoutes;
    private final OrganizationSubscriptionRepository subscriptionRepository;

    public AiModelRoutingService(
            AiModelRoutingRegistry platformRoutes,
            OrganizationSubscriptionRepository subscriptionRepository
    ) {
        this.platformRoutes = platformRoutes;
        this.subscriptionRepository = subscriptionRepository;
    }

    public Optional<AiModelRoute> resolve(AssistantRole role, UUID organizationId) {
        if (organizationId != null) {
            Optional<AiModelRoute> orgRoute = subscriptionRepository.findByOrganizationId(organizationId)
                    .map(OrganizationSubscriptionEntity::getAiModelMap)
                    .filter(map -> map != null && !map.isBlank())
                    .map(AiModelRoutingRegistry::new)
                    .flatMap(registry -> registry.routeFor(role));
            if (orgRoute.isPresent()) {
                return orgRoute;
            }
        }
        return platformRoutes.routeFor(role);
    }
}
