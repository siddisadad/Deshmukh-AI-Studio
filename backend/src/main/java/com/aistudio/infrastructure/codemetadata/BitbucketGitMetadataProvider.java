package com.aistudio.infrastructure.codemetadata;

import com.aistudio.application.codemetadata.GitFileEntry;
import com.aistudio.application.codemetadata.GitMetadataPort;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.config.GitProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnExpression("!'${aistudio.git.bitbucket-api-token:}'.isBlank()")
public class BitbucketGitMetadataProvider implements GitMetadataPort {

    private static final int MAX_FILES = 500;

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final int maxSnippetBytes;
    private final int maxContentFetchBytes;

    public BitbucketGitMetadataProvider(GitProperties properties, ObjectMapper objectMapper) {
        String token = properties.bitbucketApiToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("BITBUCKET_SYNC_TOKEN is required for Bitbucket connector");
        }
        String baseUrl = properties.bitbucketApiBaseUrl() == null || properties.bitbucketApiBaseUrl().isBlank()
                ? "https://api.bitbucket.org/2.0"
                : properties.bitbucketApiBaseUrl().trim().replaceAll("/+$", "");
        this.objectMapper = objectMapper;
        this.maxSnippetBytes = properties.effectiveMaxSnippetBytes();
        this.maxContentFetchBytes = properties.effectiveMaxContentFetchBytes();
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    BitbucketGitMetadataProvider(
            String token,
            String baseUrl,
            ObjectMapper objectMapper,
            int maxSnippetBytes,
            int maxContentFetchBytes
    ) {
        this.objectMapper = objectMapper;
        this.maxSnippetBytes = maxSnippetBytes;
        this.maxContentFetchBytes = maxContentFetchBytes;
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    @Override
    public String providerId() {
        return "bitbucket";
    }

    @Override
    public void probeCredential() {
        try {
            client.get().uri("/user").retrieve().toBodilessEntity();
        } catch (Exception ex) {
            throw new DomainException("GIT_ERROR", "Bitbucket credential check failed: " + ex.getMessage());
        }
    }

    @Override
    public void probeRepository(String repository, String branch) {
        String[] parts = parseRepository(repository);
        resolveCommit(parts[0], parts[1], branch == null || branch.isBlank() ? "main" : branch.trim());
    }

    @Override
    public List<GitFileEntry> fetchRepositoryFiles(String repository, String branch) {
        String[] parts = parseRepository(repository);
        String workspace = parts[0];
        String repoSlug = parts[1];
        String branchName = branch == null || branch.isBlank() ? "main" : branch.trim();
        try {
            String commit = resolveCommit(workspace, repoSlug, branchName);
            String srcJson = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repositories/{workspace}/{repo}/src/{commit}/")
                            .queryParam("max_depth", 50)
                            .queryParam("pagelen", 100)
                            .build(workspace, repoSlug, commit))
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(srcJson);
            List<MockGitMetadataProvider.GitTreeNode> nodes = new ArrayList<>();
            collectBitbucketNodes(root.path("values"), nodes);
            return MockGitMetadataProvider.filterCodePaths(nodes, MAX_FILES);
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException("GIT_ERROR", "Bitbucket repository sync failed: " + ex.getMessage());
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
        String[] parts = parseRepository(repository);
        String workspace = parts[0];
        String repoSlug = parts[1];
        String branchName = branch == null || branch.isBlank() ? "main" : branch.trim();
        String commit = resolveCommit(workspace, repoSlug, branchName);
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
                String raw = client.get()
                        .uri("/repositories/{workspace}/{repo}/src/{commit}/{path}", workspace, repoSlug, commit, file.path())
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

    private String resolveCommit(String workspace, String repoSlug, String branchName) {
        try {
            String branchJson = client.get()
                    .uri("/repositories/{workspace}/{repo}/refs/branches/{branch}", workspace, repoSlug, branchName)
                    .retrieve()
                    .body(String.class);
            JsonNode branchNode = objectMapper.readTree(branchJson);
            String commit = branchNode.path("target").path("hash").asText(null);
            if (commit == null || commit.isBlank()) {
                throw new DomainException("GIT_ERROR", "Bitbucket branch commit hash missing");
            }
            return commit;
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException("GIT_ERROR", "Bitbucket branch lookup failed: " + ex.getMessage());
        }
    }

    private void collectBitbucketNodes(JsonNode values, List<MockGitMetadataProvider.GitTreeNode> nodes) {
        if (values == null || !values.isArray()) {
            return;
        }
        for (JsonNode node : values) {
            String type = node.path("type").asText("");
            if ("commit_file".equals(type)) {
                nodes.add(new MockGitMetadataProvider.GitTreeNode(
                        node.path("path").asText(""),
                        "blob",
                        node.path("size").asInt(0)
                ));
            }
        }
    }

    private static String[] parseRepository(String repository) {
        if (repository == null || repository.isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "repository is required (workspace/slug)");
        }
        String trimmed = repository.trim();
        String[] parts = trimmed.split("/", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "repository must be workspace/slug");
        }
        return parts;
    }
}
