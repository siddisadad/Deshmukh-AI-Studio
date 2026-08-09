package com.aistudio.application.codemetadata;

import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.config.GitProperties;
import com.aistudio.infrastructure.persistence.entity.OrgGitCredentialEntity;
import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import com.aistudio.infrastructure.persistence.repository.OrgGitCredentialRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GitCredentialResolver {

    private final OrgGitCredentialRepository credentialRepository;
    private final ProjectRepository projectRepository;
    private final GitProperties gitProperties;

    public GitCredentialResolver(
            OrgGitCredentialRepository credentialRepository,
            ProjectRepository projectRepository,
            GitProperties gitProperties
    ) {
        this.credentialRepository = credentialRepository;
        this.projectRepository = projectRepository;
        this.gitProperties = gitProperties;
    }

    public ResolvedGitCredentials resolve(UUID organizationId, String provider) {
        String normalized = normalizeProvider(provider);
        OrgGitCredentialEntity orgCredential = credentialRepository
                .findByOrganizationIdAndProvider(organizationId, normalized)
                .orElse(null);
        if (orgCredential != null && orgCredential.isEnabled()) {
            return new ResolvedGitCredentials(
                    orgCredential.getApiToken(),
                    orgCredential.getApiBaseUrl(),
                    GitCredentialSource.ORG
            );
        }
        ResolvedGitCredentials platform = platformCredential(normalized);
        if (platform != null) {
            return platform;
        }
        throw new DomainException(
                "CONFIG_ERROR",
                "No git credential configured for provider " + normalized
                        + ". Add an organization credential or set platform env token."
        );
    }

    public ResolvedGitCredentials resolveForProject(UUID projectId, String provider) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Project not found"));
        return resolve(project.getOrganizationId(), provider);
    }

    public UUID organizationIdForProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Project not found"))
                .getOrganizationId();
    }

    public GitCredentialSource describeSource(UUID organizationId, String provider) {
        String normalized = normalizeProvider(provider);
        OrgGitCredentialEntity orgCredential = credentialRepository
                .findByOrganizationIdAndProvider(organizationId, normalized)
                .orElse(null);
        if (orgCredential != null && orgCredential.isEnabled()) {
            return GitCredentialSource.ORG;
        }
        return platformCredential(normalized) != null ? GitCredentialSource.PLATFORM : GitCredentialSource.NONE;
    }

    private ResolvedGitCredentials platformCredential(String provider) {
        switch (provider) {
            case "github":
                String githubToken = gitProperties.apiToken();
                if (githubToken != null && !githubToken.isBlank()) {
                    return new ResolvedGitCredentials(
                            githubToken,
                            gitProperties.apiBaseUrl(),
                            GitCredentialSource.PLATFORM
                    );
                }
                return null;
            case "gitlab":
                String gitlabToken = gitProperties.gitlabApiToken();
                if (gitlabToken != null && !gitlabToken.isBlank()) {
                    return new ResolvedGitCredentials(
                            gitlabToken,
                            gitProperties.gitlabApiBaseUrl(),
                            GitCredentialSource.PLATFORM
                    );
                }
                return null;
            case "bitbucket":
                String bitbucketToken = gitProperties.bitbucketApiToken();
                if (bitbucketToken != null && !bitbucketToken.isBlank()) {
                    return new ResolvedGitCredentials(
                            bitbucketToken,
                            gitProperties.bitbucketApiBaseUrl(),
                            GitCredentialSource.PLATFORM
                    );
                }
                return null;
            case "mock":
                return new ResolvedGitCredentials("mock-token", null, GitCredentialSource.PLATFORM);
            default:
                return null;
        }
    }

    private static String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "provider is required");
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }
}
