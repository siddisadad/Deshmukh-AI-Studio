package com.aistudio.infrastructure.codemetadata;

import com.aistudio.application.codemetadata.GitFileEntry;
import com.aistudio.application.codemetadata.GitMetadataPort;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.config.GitProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "aistudio.git.gitlab-api-token")
public class GitlabGitMetadataProvider implements GitMetadataPort {

    private static final int MAX_FILES = 500;

    private final RestClient client;
    private final ObjectMapper objectMapper;

    public GitlabGitMetadataProvider(GitProperties properties, ObjectMapper objectMapper) {
        String token = properties.gitlabApiToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("GITLAB_SYNC_TOKEN is required for GitLab connector");
        }
        String baseUrl = properties.gitlabApiBaseUrl() == null || properties.gitlabApiBaseUrl().isBlank()
                ? "https://gitlab.com/api/v4"
                : properties.gitlabApiBaseUrl().trim().replaceAll("/+$", "");
        this.objectMapper = objectMapper;
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("PRIVATE-TOKEN", token)
                .build();
    }

    @Override
    public String providerId() {
        return "gitlab";
    }

    @Override
    public List<GitFileEntry> fetchRepositoryFiles(String repository, String branch) {
        String projectPath = encodeProjectPath(repository);
        String branchName = branch == null || branch.isBlank() ? "main" : branch.trim();
        try {
            List<MockGitMetadataProvider.GitTreeNode> nodes = new ArrayList<>();
            int page = 1;
            while (nodes.size() < MAX_FILES) {
                String treeJson = client.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/projects/{project}/repository/tree")
                                .queryParam("recursive", true)
                                .queryParam("per_page", 100)
                                .queryParam("page", page)
                                .queryParam("ref", branchName)
                                .build(projectPath))
                        .retrieve()
                        .body(String.class);
                JsonNode root = objectMapper.readTree(treeJson);
                if (!root.isArray() || root.isEmpty()) {
                    break;
                }
                for (JsonNode node : root) {
                    String type = node.path("type").asText("");
                    if (!"blob".equalsIgnoreCase(type)) {
                        continue;
                    }
                    nodes.add(new MockGitMetadataProvider.GitTreeNode(
                            node.path("path").asText(""),
                            "blob",
                            0
                    ));
                }
                if (root.size() < 100) {
                    break;
                }
                page++;
            }
            return MockGitMetadataProvider.filterCodePaths(nodes, MAX_FILES);
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException("GIT_ERROR", "GitLab repository sync failed: " + ex.getMessage());
        }
    }

    private static String encodeProjectPath(String repository) {
        if (repository == null || repository.isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "repository is required (namespace/project)");
        }
        return URLEncoder.encode(repository.trim(), StandardCharsets.UTF_8).replace("+", "%20");
    }
}
