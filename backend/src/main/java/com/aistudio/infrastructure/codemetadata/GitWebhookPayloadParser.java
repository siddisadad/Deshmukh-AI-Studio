package com.aistudio.infrastructure.codemetadata;

import com.aistudio.application.codemetadata.GitWebhookDelta;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class GitWebhookPayloadParser {

    private final ObjectMapper objectMapper;

    public GitWebhookPayloadParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public GitWebhookDelta parseGithub(String payload, String configuredBranch) {
        return parseCommitArrays(read(payload), configuredBranch);
    }

    public GitWebhookDelta parseGitlab(String payload, String configuredBranch) {
        JsonNode root = read(payload);
        if (!"push".equalsIgnoreCase(root.path("object_kind").asText(""))) {
            return emptyDelta();
        }
        return parseCommitArrays(root, configuredBranch);
    }

    public GitWebhookDelta parseBitbucket(String payload, String configuredBranch) {
        JsonNode root = read(payload);
        JsonNode changes = root.path("push").path("changes");
        if (!changes.isArray()) {
            return emptyDelta();
        }
        Set<String> changed = new LinkedHashSet<>();
        Set<String> removed = new LinkedHashSet<>();
        String branch = normalizeBranch(configuredBranch);
        for (JsonNode change : changes) {
            String newRef = change.path("new").path("name").asText("");
            if (!newRef.isBlank() && !branch.equals(normalizeBranch(newRef))) {
                continue;
            }
            JsonNode commits = change.path("commits");
            if (!commits.isArray()) {
                continue;
            }
            for (JsonNode commit : commits) {
                collectPathNodes(commit.path("added"), changed);
                collectPathNodes(commit.path("modified"), changed);
                collectPathNodes(commit.path("removed"), removed);
            }
        }
        return filterDelta(changed, removed);
    }

    private GitWebhookDelta parseCommitArrays(JsonNode root, String configuredBranch) {
        String ref = root.path("ref").asText("");
        if (!ref.isBlank() && !refMatchesBranch(ref, configuredBranch)) {
            return emptyDelta();
        }
        Set<String> changed = new LinkedHashSet<>();
        Set<String> removed = new LinkedHashSet<>();
        JsonNode commits = root.path("commits");
        if (!commits.isArray()) {
            return emptyDelta();
        }
        for (JsonNode commit : commits) {
            collectStringPaths(commit.path("added"), changed);
            collectStringPaths(commit.path("modified"), changed);
            collectStringPaths(commit.path("removed"), removed);
        }
        return filterDelta(changed, removed);
    }

    private static void collectStringPaths(JsonNode values, Set<String> target) {
        if (values == null || !values.isArray()) {
            return;
        }
        for (JsonNode value : values) {
            String path = normalizePath(value.asText(""));
            if (!path.isBlank()) {
                target.add(path);
            }
        }
    }

    private static void collectPathNodes(JsonNode values, Set<String> target) {
        if (values == null || !values.isArray()) {
            return;
        }
        for (JsonNode value : values) {
            String path = normalizePath(value.path("path").asText(value.asText("")));
            if (!path.isBlank()) {
                target.add(path);
            }
        }
    }

    private static GitWebhookDelta filterDelta(Set<String> changed, Set<String> removed) {
        List<String> filteredChanged = new ArrayList<>();
        for (String path : changed) {
            if (MockGitMetadataProvider.isCodeLikePath(path)) {
                filteredChanged.add(path);
            }
        }
        List<String> filteredRemoved = new ArrayList<>();
        for (String path : removed) {
            if (!path.isBlank()) {
                filteredRemoved.add(path);
            }
        }
        return new GitWebhookDelta(filteredChanged, filteredRemoved);
    }

    private static boolean refMatchesBranch(String ref, String configuredBranch) {
        String branch = normalizeBranch(configuredBranch);
        if (branch.isBlank()) {
            return true;
        }
        String normalizedRef = ref.trim();
        if (normalizedRef.startsWith("refs/heads/")) {
            normalizedRef = normalizedRef.substring("refs/heads/".length());
        }
        return branch.equals(normalizeBranch(normalizedRef));
    }

    private static String normalizeBranch(String branch) {
        return branch == null ? "" : branch.trim();
    }

    private static String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String trimmed = path.trim().replace('\\', '/');
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
    }

    private JsonNode read(String payload) {
        try {
            return objectMapper.readTree(payload == null ? "{}" : payload);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private static GitWebhookDelta emptyDelta() {
        return new GitWebhookDelta(List.of(), List.of());
    }
}
