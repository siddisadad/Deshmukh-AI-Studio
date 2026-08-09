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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnExpression("!'${aistudio.git.gitlab-api-token:}'.isBlank()")
public class GitlabGitMetadataProvider implements GitMetadataPort {

    private static final int MAX_FILES = 500;

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final int maxSnippetBytes;
    private final int maxContentFetchBytes;

    public GitlabGitMetadataProvider(GitProperties properties, ObjectMapper objectMapper) {
        String token = properties.gitlabApiToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("GITLAB_SYNC_TOKEN is required for GitLab connector");
        }
        String baseUrl = properties.gitlabApiBaseUrl() == null || properties.gitlabApiBaseUrl().isBlank()
                ? "https://gitlab.com/api/v4"
                : properties.gitlabApiBaseUrl().trim().replaceAll("/+$", "");
        this.objectMapper = objectMapper;
        this.maxSnippetBytes = properties.effectiveMaxSnippetBytes();
        this.maxContentFetchBytes = properties.effectiveMaxContentFetchBytes();
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
                final int pageNum = page;
                String treeJson = client.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/projects/{project}/repository/tree")
                                .queryParam("recursive", true)
                                .queryParam("per_page", 100)
                                .queryParam("page", pageNum)
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

    @Override
    public List<GitFileEntry> fetchFilesByPaths(String repository, String branch, List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return List.of();
        }
        List<GitFileEntry> stubs = new ArrayList<>();
        for (String path : paths) {
            if (!MockGitMetadataProvider.isCodeLikePath(path)) {
                continue;
            }
            stubs.add(new GitFileEntry(
                    path,
                    MockGitMetadataProvider.languageFromPath(path),
                    "",
                    0
            ));
        }
        return hydrateFileContents(repository, branch, stubs);
    }

    @Override
    public List<GitFileEntry> hydrateFileContents(String repository, String branch, List<GitFileEntry> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        String projectPath = encodeProjectPath(repository);
        String branchName = branch == null || branch.isBlank() ? "main" : branch.trim();
        List<GitFileEntry> hydrated = new ArrayList<>();
        for (GitFileEntry file : files) {
            if (!GitSnippetUtils.shouldFetchContent(file.sizeBytes(), maxContentFetchBytes)) {
                hydrated.add(file);
                continue;
            }
            if (file.snippet() != null && !file.snippet().isBlank()) {
                hydrated.add(file);
                continue;
            }
            try {
                String encodedPath = URLEncoder.encode(file.path(), StandardCharsets.UTF_8).replace("+", "%20");
                String raw = client.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/projects/{project}/repository/files/{filePath}/raw")
                                .queryParam("ref", branchName)
                                .build(projectPath, encodedPath))
                        .retrieve()
                        .body(String.class);
                hydrated.add(new GitFileEntry(
                        file.path(),
                        file.language(),
                        GitSnippetUtils.truncateToUtf8Bytes(raw == null ? "" : raw, maxSnippetBytes),
                        file.sizeBytes()
                ));
            } catch (Exception ex) {
                hydrated.add(file);
            }
        }
        return hydrated;
    }

    private static String encodeProjectPath(String repository) {
        if (repository == null || repository.isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "repository is required (namespace/project)");
        }
        return URLEncoder.encode(repository.trim(), StandardCharsets.UTF_8).replace("+", "%20");
    }
}
