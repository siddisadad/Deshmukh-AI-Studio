package com.aistudio.application.organization;

import com.aistudio.api.codemetadata.dto.GitConnectionTestResponse;
import com.aistudio.api.organization.dto.OrgGitCredentialEventResponse;
import com.aistudio.api.organization.dto.OrgGitCredentialResponse;
import com.aistudio.api.organization.dto.UpsertOrgGitCredentialRequest;
import com.aistudio.application.codemetadata.GitConnectionProbeService;
import com.aistudio.application.codemetadata.GitCredentialResolver;
import com.aistudio.application.codemetadata.GitCredentialSource;
import com.aistudio.application.codemetadata.GitMetadataPort;
import com.aistudio.application.codemetadata.ResolvedGitCredentials;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.codemetadata.GitMetadataPortFactory;
import com.aistudio.infrastructure.persistence.entity.OrgGitCredentialEventEntity;
import com.aistudio.infrastructure.persistence.entity.OrgGitCredentialEntity;
import com.aistudio.infrastructure.persistence.repository.OrgGitCredentialEventRepository;
import com.aistudio.infrastructure.persistence.repository.OrgGitCredentialRepository;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgGitCredentialService {

    private static final Set<String> ALLOWED_PROVIDERS = Set.of("github", "gitlab", "bitbucket");

    private final OrgGitCredentialRepository credentialRepository;
    private final OrgGitCredentialEventRepository eventRepository;
    private final ProjectAuthorizationService authorizationService;
    private final GitCredentialResolver credentialResolver;
    private final GitMetadataPortFactory portFactory;
    private final GitConnectionProbeService probeService;

    public OrgGitCredentialService(
            OrgGitCredentialRepository credentialRepository,
            OrgGitCredentialEventRepository eventRepository,
            ProjectAuthorizationService authorizationService,
            GitCredentialResolver credentialResolver,
            GitMetadataPortFactory portFactory,
            GitConnectionProbeService probeService
    ) {
        this.credentialRepository = credentialRepository;
        this.eventRepository = eventRepository;
        this.authorizationService = authorizationService;
        this.credentialResolver = credentialResolver;
        this.portFactory = portFactory;
        this.probeService = probeService;
    }

    @Transactional(readOnly = true)
    public List<OrgGitCredentialResponse> list(UUID organizationId, UUID userId) {
        authorizationService.requireOrgMember(organizationId, userId);
        List<OrgGitCredentialResponse> responses = new ArrayList<>();
        for (String provider : ALLOWED_PROVIDERS) {
            responses.add(toResponse(organizationId, provider));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public List<OrgGitCredentialEventResponse> listEvents(UUID organizationId, UUID userId, int limit) {
        authorizationService.requireOrgMember(organizationId, userId);
        int safeLimit = limit <= 0 ? 50 : Math.min(limit, 200);
        return eventRepository.findByOrganizationIdOrderByCreatedAtDesc(
                organizationId,
                PageRequest.of(0, safeLimit)
        ).stream().map(this::toEventResponse).toList();
    }

    @Transactional
    public OrgGitCredentialResponse upsert(
            UUID organizationId,
            String provider,
            UUID userId,
            UpsertOrgGitCredentialRequest request
    ) {
        authorizationService.requireOrgOwner(organizationId, userId);
        String normalized = parseProvider(provider);
        OrgGitCredentialEntity existing = credentialRepository
                .findByOrganizationIdAndProvider(organizationId, normalized)
                .orElse(null);
        boolean isNew = existing == null;
        OrgGitCredentialEntity entity = existing != null ? existing : new OrgGitCredentialEntity();
        if (isNew) {
            entity.setOrganizationId(organizationId);
            entity.setProvider(normalized);
        }
        String previousToken = entity.getApiToken();
        boolean tokenProvided = request.apiToken() != null && !request.apiToken().isBlank();
        entity.setDisplayName(request.displayName().trim());
        if (tokenProvided) {
            entity.setApiToken(request.apiToken().trim());
        } else if (entity.getApiToken() == null || entity.getApiToken().isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "apiToken is required when creating a credential");
        }
        entity.setApiBaseUrl(blankToNull(request.apiBaseUrl()));
        if (request.enabled() != null) {
            entity.setEnabled(request.enabled());
        }
        credentialRepository.save(entity);
        String action = isNew ? "CREATED"
                : (tokenProvided && !request.apiToken().trim().equals(previousToken) ? "TOKEN_ROTATED" : "UPDATED");
        recordEvent(organizationId, normalized, userId, action, entity);
        return toStoredResponse(entity, GitCredentialSource.ORG);
    }

    @Transactional
    public void delete(UUID organizationId, String provider, UUID userId) {
        authorizationService.requireOrgOwner(organizationId, userId);
        String normalized = parseProvider(provider);
        credentialRepository.findByOrganizationIdAndProvider(organizationId, normalized).ifPresent(entity -> {
            recordEvent(organizationId, normalized, userId, "DELETED", entity);
            credentialRepository.delete(entity);
        });
    }

    @Transactional
    public GitConnectionTestResponse test(
            UUID organizationId,
            String provider,
            UUID userId,
            String repository,
            String branch
    ) {
        authorizationService.requireOrgOwner(organizationId, userId);
        String normalized = parseProvider(provider);
        ResolvedGitCredentials credentials = credentialResolver.resolve(organizationId, normalized);
        GitMetadataPort port = portFactory.create(normalized, credentials);
        GitConnectionTestResponse result = probeService.probe(port, repository, branch);
        credentialRepository.findByOrganizationIdAndProvider(organizationId, normalized).ifPresent(entity -> {
            entity.setLastTestedAt(Instant.now());
            entity.setLastTestStatus(result.ok() ? "success" : "failed");
            entity.setLastTestError(result.ok() ? null : truncate(result.message(), 512));
            credentialRepository.save(entity);
        });
        return result;
    }

    private OrgGitCredentialResponse toResponse(UUID organizationId, String provider) {
        return credentialRepository.findByOrganizationIdAndProvider(organizationId, provider)
                .map(entity -> toStoredResponse(entity, GitCredentialSource.ORG))
                .orElseGet(() -> platformPlaceholder(organizationId, provider));
    }

    private OrgGitCredentialResponse platformPlaceholder(UUID organizationId, String provider) {
        GitCredentialSource source = credentialResolver.describeSource(organizationId, provider);
        String defaultBaseUrl = defaultBaseUrl(provider);
        return new OrgGitCredentialResponse(
                null,
                provider,
                platformDisplayName(provider),
                source != GitCredentialSource.NONE,
                defaultBaseUrl,
                source != GitCredentialSource.NONE,
                source.name(),
                null,
                null,
                null,
                null,
                null
        );
    }

    private OrgGitCredentialResponse toStoredResponse(OrgGitCredentialEntity entity, GitCredentialSource source) {
        return new OrgGitCredentialResponse(
                entity.getId(),
                entity.getProvider(),
                entity.getDisplayName(),
                true,
                entity.getApiBaseUrl() != null && !entity.getApiBaseUrl().isBlank()
                        ? entity.getApiBaseUrl()
                        : defaultBaseUrl(entity.getProvider()),
                entity.isEnabled(),
                source.name(),
                entity.getLastTestedAt(),
                entity.getLastTestStatus(),
                entity.getLastTestError(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static String parseProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "provider is required");
        }
        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_PROVIDERS.contains(normalized)) {
            throw new DomainException("VALIDATION_ERROR", "provider must be github, gitlab, or bitbucket");
        }
        return normalized;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String defaultBaseUrl(String provider) {
        switch (provider) {
            case "github":
                return "https://api.github.com";
            case "gitlab":
                return "https://gitlab.com/api/v4";
            case "bitbucket":
                return "https://api.bitbucket.org/2.0";
            default:
                return null;
        }
    }

    private static String platformDisplayName(String provider) {
        switch (provider) {
            case "github":
                return "GitHub (platform)";
            case "gitlab":
                return "GitLab (platform)";
            case "bitbucket":
                return "Bitbucket (platform)";
            default:
                return provider;
        }
    }

    private void recordEvent(
            UUID organizationId,
            String provider,
            UUID actorUserId,
            String action,
            OrgGitCredentialEntity entity
    ) {
        OrgGitCredentialEventEntity event = new OrgGitCredentialEventEntity();
        event.setOrganizationId(organizationId);
        event.setProvider(provider);
        event.setAction(action);
        event.setActorUserId(actorUserId);
        event.setDisplayName(entity.getDisplayName());
        event.setApiBaseUrl(entity.getApiBaseUrl());
        eventRepository.save(event);
    }

    private OrgGitCredentialEventResponse toEventResponse(OrgGitCredentialEventEntity event) {
        return new OrgGitCredentialEventResponse(
                event.getId(),
                event.getProvider(),
                event.getAction(),
                event.getActorUserId(),
                event.getDisplayName(),
                event.getApiBaseUrl(),
                event.getCreatedAt()
        );
    }
}
