package com.aistudio.infrastructure.codemetadata;

import com.aistudio.application.codemetadata.GitFileEntry;
import com.aistudio.application.codemetadata.GitMetadataPort;
import com.aistudio.application.codemetadata.ResolvedGitCredentials;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.config.GitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class GitMetadataPortFactory {

    private final ObjectMapper objectMapper;
    private final GitProperties gitProperties;

    public GitMetadataPortFactory(ObjectMapper objectMapper, GitProperties gitProperties) {
        this.objectMapper = objectMapper;
        this.gitProperties = gitProperties;
    }

    public GitMetadataPort create(String provider, ResolvedGitCredentials credentials) {
        if (isMockMode(gitProperties)) {
            return new MockGitMetadataProvider();
        }
        String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        int maxSnippetBytes = gitProperties.effectiveMaxSnippetBytes();
        int maxContentFetchBytes = gitProperties.effectiveMaxContentFetchBytes();
        switch (normalized) {
            case "github":
                return new GithubGitMetadataProvider(
                        credentials.apiToken(),
                        credentials.effectiveBaseUrl("https://api.github.com"),
                        objectMapper,
                        maxSnippetBytes,
                        maxContentFetchBytes
                );
            case "gitlab":
                return new GitlabGitMetadataProvider(
                        credentials.apiToken(),
                        credentials.effectiveBaseUrl("https://gitlab.com/api/v4"),
                        objectMapper,
                        maxSnippetBytes,
                        maxContentFetchBytes
                );
            case "bitbucket":
                return new BitbucketGitMetadataProvider(
                        credentials.apiToken(),
                        credentials.effectiveBaseUrl("https://api.bitbucket.org/2.0"),
                        objectMapper,
                        maxSnippetBytes,
                        maxContentFetchBytes
                );
            case "mock":
                return new MockGitMetadataProvider();
            default:
                throw new DomainException(
                        "CONFIG_ERROR",
                        "Git host connector not configured for provider: " + provider
                );
        }
    }

    private static boolean isMockMode(GitProperties properties) {
        String provider = properties.provider();
        return provider == null || provider.isBlank() || "mock".equalsIgnoreCase(provider.trim());
    }
}
