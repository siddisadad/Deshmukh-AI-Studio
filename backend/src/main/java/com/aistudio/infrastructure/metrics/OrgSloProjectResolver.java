package com.aistudio.infrastructure.metrics;

import com.aistudio.infrastructure.persistence.repository.ProjectRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class OrgSloProjectResolver {

    private static final int CACHE_MAX = 2000;

    private final ProjectRepository projectRepository;
    private final Map<UUID, Optional<UUID>> cache = new ConcurrentHashMap<>();

    public OrgSloProjectResolver(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Optional<UUID> resolveOrganizationId(UUID projectId) {
        if (projectId == null) {
            return Optional.empty();
        }
        return cache.compute(projectId, (id, existing) -> {
            if (existing != null) {
                return existing;
            }
            if (cache.size() > CACHE_MAX) {
                cache.clear();
            }
            return projectRepository.findById(id).map(project -> project.getOrganizationId());
        });
    }
}
