package com.aistudio.infrastructure.codemetadata;

import com.aistudio.application.codemetadata.GitFileEntry;
import com.aistudio.application.codemetadata.GitMetadataPort;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.config.GitProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "aistudio.git.provider", havingValue = "github")
public class GithubGitMetadataProvider implements GitMetadataPort {

    private static final int MAX_FILES = 500;

    private final RestClient client;
    private final ObjectMapper objectMapper;

    public GithubGitMetadataProvider(GitProperties properties, ObjectMapper objectMapper) {
        String token = properties.apiToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("GITHUB_SYNC_TOKEN is required when aistudio.git.provider=github");
        }
        String baseUrl = properties.apiBaseUrl() == null || properties.apiBaseUrl().isBlank()
                ? "https://api.github.com"
                : properties.apiBaseUrl();
        this.objectMapper = objectMapper;
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    @Override
    public String providerId() {
        return "github";
    }

    @Override
    public List<GitFileEntry> fetchRepositoryFiles(String repository, String branch) {
        String[] parts = parseRepository(repository);
        String owner = parts[0];
        String repo = parts[1];
        String branchName = branch == null || branch.isBlank() ? "main" : branch.trim();
        try {
            String branchJson = client.get()
                    .uri("/repos/{owner}/{repo}/branches/{branch}", owner, repo, branchName)
                    .retrieve()
                    .body(String.class);
            JsonNode branchNode = objectMapper.readTree(branchJson);
            String sha = branchNode.path("commit").path("sha").asText(null);
            if (sha == null || sha.isBlank()) {
                throw new DomainException("GIT_ERROR", "GitHub branch commit SHA missing");
            }
            String treeJson = client.get()
                    .uri("/repos/{owner}/{repo}/git/trees/{sha}?recursive=1", owner, repo, sha)
                    .retrieve()
                    .body(String.class);
            JsonNode tree = objectMapper.readTree(treeJson).path("tree");
            List<MockGitMetadataProvider.GitTreeNode> nodes = new ArrayList<>();
            for (JsonNode node : tree) {
                nodes.add(new MockGitMetadataProvider.GitTreeNode(
                        node.path("path").asText(""),
                        node.path("type").asText(""),
                        node.path("size").asInt(0)
                ));
            }
            return MockGitMetadataProvider.filterCodePaths(nodes, MAX_FILES);
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException("GIT_ERROR", "GitHub repository sync failed: " + ex.getMessage());
        }
    }

    private static String[] parseRepository(String repository) {
        if (repository == null || repository.isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "repository is required (owner/name)");
        }
        String trimmed = repository.trim();
        String[] parts = trimmed.split("/", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "repository must be owner/name");
        }
        return parts;
    }
}
