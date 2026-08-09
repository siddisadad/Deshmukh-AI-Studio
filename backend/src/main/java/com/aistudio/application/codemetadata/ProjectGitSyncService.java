package com.aistudio.application.codemetadata;

import com.aistudio.api.codemetadata.dto.ProjectGitLinkResponse;
import com.aistudio.api.codemetadata.dto.UpsertProjectGitLinkRequest;
import com.aistudio.application.codemetadata.GitFileEntry;
import com.aistudio.application.job.BackgroundJobService;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.job.JobStatus;
import com.aistudio.domain.job.JobType;
import com.aistudio.infrastructure.config.GitProperties;
import com.aistudio.infrastructure.codemetadata.GitWebhookPayloadParser;
import com.aistudio.infrastructure.persistence.entity.ProjectGitLinkEntity;
import com.aistudio.infrastructure.persistence.repository.BackgroundJobRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectGitLinkRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectGitSyncService {

    private static final Set<String> ALLOWED_PROVIDERS = Set.of("github", "gitlab", "bitbucket", "mock");

    private final ProjectGitLinkRepository gitLinkRepository;
    private final BackgroundJobRepository backgroundJobRepository;
    private final ProjectAuthorizationService authorizationService;
    private final ProjectCodeMetadataService codeMetadataService;
    private final GitMetadataRegistry gitMetadataRegistry;
    private final BackgroundJobService backgroundJobService;
    private final GitWebhookPayloadParser webhookPayloadParser;
    private final ObjectMapper objectMapper;
    private final String publicApiBaseUrl;
    private final boolean fetchFileContent;
    private final boolean webhookDeltaSync;
    private final boolean scheduledSyncEnabled;
    private final long defaultScheduledSyncIntervalMs;

    public ProjectGitSyncService(
            ProjectGitLinkRepository gitLinkRepository,
            BackgroundJobRepository backgroundJobRepository,
            ProjectAuthorizationService authorizationService,
            ProjectCodeMetadataService codeMetadataService,
            GitMetadataRegistry gitMetadataRegistry,
            BackgroundJobService backgroundJobService,
            GitWebhookPayloadParser webhookPayloadParser,
            ObjectMapper objectMapper,
            GitProperties gitProperties
    ) {
        this.gitLinkRepository = gitLinkRepository;
        this.backgroundJobRepository = backgroundJobRepository;
        this.authorizationService = authorizationService;
        this.codeMetadataService = codeMetadataService;
        this.gitMetadataRegistry = gitMetadataRegistry;
        this.backgroundJobService = backgroundJobService;
        this.webhookPayloadParser = webhookPayloadParser;
        this.objectMapper = objectMapper;
        this.publicApiBaseUrl = gitProperties.publicApiBaseUrl() == null || gitProperties.publicApiBaseUrl().isBlank()
                ? "http://localhost:8080"
                : gitProperties.publicApiBaseUrl().trim().replaceAll("/+$", "");
        this.fetchFileContent = gitProperties.fetchFileContentEnabled();
        this.webhookDeltaSync = gitProperties.webhookDeltaSyncEnabled();
        this.scheduledSyncEnabled = gitProperties.isScheduledSyncEnabled();
        this.defaultScheduledSyncIntervalMs = gitProperties.effectiveScheduledSyncIntervalMs();
    }

    @Transactional(readOnly = true)
    public ProjectGitLinkResponse getLink(UUID projectId, UUID userId) {
        authorizationService.requireProjectAccess(projectId, userId);
        return gitLinkRepository.findByProjectId(projectId)
                .map(this::toResponse)
                .orElseGet(() -> emptyResponse(projectId));
    }

    @Transactional
    public ProjectGitLinkResponse upsertLink(UUID projectId, UUID userId, UpsertProjectGitLinkRequest request) {
        authorizationService.requireProjectEdit(projectId, userId);
        String repository = normalizeRepository(request.repository());
        String provider = parseProvider(request.provider());
        ProjectGitLinkEntity entity = gitLinkRepository.findByProjectId(projectId)
                .orElseGet(() -> {
                    ProjectGitLinkEntity created = new ProjectGitLinkEntity();
                    created.setProjectId(projectId);
                    created.setWebhookSecret(generateSecret());
                    return created;
                });
        entity.setProvider(provider);
        entity.setRepository(repository);
        entity.setBranch(request.branch() == null || request.branch().isBlank() ? "main" : request.branch().trim());
        entity.setEnabled(request.enabled() == null || request.enabled());
        if (request.scheduledSyncEnabled() != null) {
            entity.setScheduledSyncEnabled(request.scheduledSyncEnabled());
        }
        if (request.regenerateWebhookSecret() != null && request.regenerateWebhookSecret()) {
            entity.setWebhookSecret(generateSecret());
        }
        if (request.clearScheduledSyncInterval() != null && request.clearScheduledSyncInterval()) {
            entity.setScheduledSyncIntervalMinutes(null);
        } else if (request.scheduledSyncIntervalMinutes() != null) {
            entity.setScheduledSyncIntervalMinutes(request.scheduledSyncIntervalMinutes());
        }
        if (request.clearPathIgnorePatterns() != null && request.clearPathIgnorePatterns()) {
            entity.setPathIgnorePatterns(new ArrayList<>());
        } else if (request.pathIgnorePatterns() != null) {
            entity.setPathIgnorePatterns(GitPathIgnoreMatcher.normalizePatterns(request.pathIgnorePatterns()));
        }
        gitLinkRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public ProjectGitLinkResponse syncNow(UUID projectId, UUID userId) {
        authorizationService.requireProjectEdit(projectId, userId);
        ProjectGitLinkEntity link = requireEnabledLink(projectId);
        syncLink(link);
        return toResponse(gitLinkRepository.findById(link.getId()).orElseThrow());
    }

    @Transactional
    public int syncProject(UUID projectId) {
        return syncProject(projectId, null);
    }

    @Transactional
    public int syncProject(UUID projectId, String jobPayloadJson) {
        ProjectGitLinkEntity link = gitLinkRepository.findByProjectId(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Git link not configured"));
        if (!link.isEnabled()) {
            throw new DomainException("VALIDATION_ERROR", "Git link is disabled");
        }
        GitWebhookDelta delta = parseJobDelta(jobPayloadJson);
        if (webhookDeltaSync && delta != null && delta.hasChanges()) {
            return syncLinkDelta(link, delta);
        }
        return syncLink(link);
    }

    @Transactional
    public int enqueueScheduledSyncsForEnabledLinks() {
        if (!scheduledSyncEnabled) {
            return 0;
        }
        int enqueued = 0;
        for (ProjectGitLinkEntity link : gitLinkRepository.findByEnabledTrue()) {
            if (!link.isScheduledSyncEnabled()) {
                continue;
            }
            if (!isDueForScheduledSync(link)) {
                continue;
            }
            if (backgroundJobRepository.countByProjectIdAndJobTypeAndStatus(
                    link.getProjectId(),
                    JobType.CODE_METADATA_SYNC,
                    JobStatus.PENDING
            ) > 0) {
                continue;
            }
            backgroundJobService.enqueueInternal(
                    link.getProjectId(),
                    JobType.CODE_METADATA_SYNC,
                    Map.of("source", "scheduled")
            );
            enqueued++;
        }
        return enqueued;
    }

    @Transactional
    public void handleGithubWebhook(UUID projectId, String signatureHeader, String payload) {
        ProjectGitLinkEntity link = requireWebhookLink(projectId, "github");
        verifyHmacSha256Signature(link.getWebhookSecret(), signatureHeader, payload, "sha256=");
        enqueueWebhookSync(projectId, webhookPayloadParser.parseGithub(payload, link.getBranch()));
    }

    @Transactional
    public void handleGitlabWebhook(UUID projectId, String tokenHeader, String payload) {
        ProjectGitLinkEntity link = requireWebhookLink(projectId, "gitlab");
        verifySharedToken(link.getWebhookSecret(), tokenHeader);
        enqueueWebhookSync(projectId, webhookPayloadParser.parseGitlab(payload, link.getBranch()));
    }

    @Transactional
    public void handleBitbucketWebhook(UUID projectId, String signatureHeader, String payload) {
        ProjectGitLinkEntity link = requireWebhookLink(projectId, "bitbucket");
        verifyHmacSha256Signature(link.getWebhookSecret(), signatureHeader, payload, "sha256=");
        enqueueWebhookSync(projectId, webhookPayloadParser.parseBitbucket(payload, link.getBranch()));
    }

    private void enqueueWebhookSync(UUID projectId, GitWebhookDelta delta) {
        ProjectGitLinkEntity link = gitLinkRepository.findByProjectId(projectId).orElse(null);
        List<String> ignorePatterns = link == null ? List.of() : ignorePatterns(link);
        Map<String, Object> jobPayload = new HashMap<>();
        jobPayload.put("source", "webhook");
        if (webhookDeltaSync && delta != null && delta.hasChanges()) {
            jobPayload.put("changedPaths", GitPathIgnoreMatcher.filterPaths(delta.changedPaths(), ignorePatterns));
            jobPayload.put("removedPaths", GitPathIgnoreMatcher.filterPaths(delta.removedPaths(), ignorePatterns));
        }
        backgroundJobService.enqueueInternal(projectId, JobType.CODE_METADATA_SYNC, jobPayload);
    }

    private int syncLinkDelta(ProjectGitLinkEntity link, GitWebhookDelta delta) {
        try {
            List<String> ignorePatterns = ignorePatterns(link);
            List<String> changedPaths = GitPathIgnoreMatcher.filterPaths(delta.changedPaths(), ignorePatterns);
            List<String> removedPaths = GitPathIgnoreMatcher.filterPaths(delta.removedPaths(), ignorePatterns);
            GitMetadataPort port = gitMetadataRegistry.require(link.getProvider());
            List<GitFileEntry> upserts = port.fetchFilesByPaths(
                    link.getRepository(),
                    link.getBranch(),
                    changedPaths
            );
            if (fetchFileContent) {
                upserts = port.hydrateFileContents(link.getRepository(), link.getBranch(), upserts);
            }
            upserts = GitPathIgnoreMatcher.filterFiles(upserts, ignorePatterns);
            int count = codeMetadataService.applyDeltaInternal(
                    link.getProjectId(),
                    upserts,
                    removedPaths
            );
            link.setLastSyncedAt(Instant.now());
            link.setLastSyncStatus("success");
            link.setLastSyncError(null);
            gitLinkRepository.save(link);
            return count;
        } catch (Exception ex) {
            link.setLastSyncStatus("failed");
            link.setLastSyncError(ex.getMessage() == null ? "sync failed" : truncate(ex.getMessage(), 2000));
            gitLinkRepository.save(link);
            if (ex instanceof DomainException domainEx) {
                throw domainEx;
            }
            throw new DomainException("GIT_ERROR", link.getLastSyncError());
        }
    }

    private GitWebhookDelta parseJobDelta(String jobPayloadJson) {
        if (jobPayloadJson == null || jobPayloadJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(jobPayloadJson);
            if (!root.has("changedPaths") && !root.has("removedPaths")) {
                return null;
            }
            List<String> changed = readPathArray(root.path("changedPaths"));
            List<String> removed = readPathArray(root.path("removedPaths"));
            return new GitWebhookDelta(changed, removed);
        } catch (Exception ex) {
            return null;
        }
    }

    private static List<String> readPathArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> paths = new ArrayList<>();
        for (JsonNode value : node) {
            String path = value.asText("").trim();
            if (!path.isBlank()) {
                paths.add(path);
            }
        }
        return paths;
    }

    private int syncLink(ProjectGitLinkEntity link) {
        try {
            GitMetadataPort port = gitMetadataRegistry.require(link.getProvider());
            List<GitFileEntry> files = port.fetchRepositoryFiles(link.getRepository(), link.getBranch());
            if (fetchFileContent) {
                files = port.hydrateFileContents(link.getRepository(), link.getBranch(), files);
            }
            files = GitPathIgnoreMatcher.filterFiles(files, ignorePatterns(link));
            int count = codeMetadataService.replaceFilesInternal(link.getProjectId(), files);
            link.setLastSyncedAt(Instant.now());
            link.setLastSyncStatus("success");
            link.setLastSyncError(null);
            gitLinkRepository.save(link);
            return count;
        } catch (Exception ex) {
            link.setLastSyncStatus("failed");
            link.setLastSyncError(ex.getMessage() == null ? "sync failed" : truncate(ex.getMessage(), 2000));
            gitLinkRepository.save(link);
            if (ex instanceof DomainException domainEx) {
                throw domainEx;
            }
            throw new DomainException("GIT_ERROR", link.getLastSyncError());
        }
    }

    private ProjectGitLinkEntity requireEnabledLink(UUID projectId) {
        ProjectGitLinkEntity link = gitLinkRepository.findByProjectId(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Git link not configured"));
        if (!link.isEnabled()) {
            throw new DomainException("VALIDATION_ERROR", "Git link is disabled");
        }
        return link;
    }

    private ProjectGitLinkEntity requireWebhookLink(UUID projectId, String expectedProvider) {
        ProjectGitLinkEntity link = gitLinkRepository.findByProjectId(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Git link not configured"));
        if (!link.getProvider().equalsIgnoreCase(expectedProvider)) {
            throw new DomainException("VALIDATION_ERROR", "Git link provider mismatch");
        }
        return link;
    }

    private boolean isDueForScheduledSync(ProjectGitLinkEntity link) {
        Instant lastSyncedAt = link.getLastSyncedAt();
        if (lastSyncedAt == null) {
            return true;
        }
        long intervalMs = effectiveScheduledSyncIntervalMs(link);
        return Instant.now().toEpochMilli() - lastSyncedAt.toEpochMilli() >= intervalMs;
    }

    private long effectiveScheduledSyncIntervalMs(ProjectGitLinkEntity link) {
        Integer minutes = link.getScheduledSyncIntervalMinutes();
        if (minutes == null || minutes <= 0) {
            return defaultScheduledSyncIntervalMs;
        }
        return minutes.longValue() * 60_000L;
    }

    private List<String> ignorePatterns(ProjectGitLinkEntity link) {
        return GitPathIgnoreMatcher.normalizePatterns(link.getPathIgnorePatterns());
    }

    private ProjectGitLinkResponse toResponse(ProjectGitLinkEntity entity) {
        return new ProjectGitLinkResponse(
                entity.getId(),
                entity.getProjectId(),
                entity.getProvider(),
                entity.getRepository(),
                entity.getBranch(),
                entity.isEnabled(),
                entity.isScheduledSyncEnabled(),
                webhookUrl(entity.getProvider(), entity.getProjectId()),
                entity.getWebhookSecret(),
                entity.getLastSyncedAt(),
                entity.getLastSyncStatus(),
                entity.getLastSyncError(),
                entity.getScheduledSyncIntervalMinutes(),
                ignorePatterns(entity),
                entity.getUpdatedAt()
        );
    }

    private ProjectGitLinkResponse emptyResponse(UUID projectId) {
        return new ProjectGitLinkResponse(
                null,
                projectId,
                "github",
                "",
                "main",
                false,
                true,
                webhookUrl("github", projectId),
                null,
                null,
                "never",
                null,
                null,
                List.of(),
                null
        );
    }

    private String webhookUrl(String provider, UUID projectId) {
        String host = provider == null || provider.isBlank() ? "github" : provider.trim().toLowerCase(Locale.ROOT);
        return publicApiBaseUrl + "/api/v1/git/webhook/" + host + "/" + projectId;
    }

    private static String parseProvider(String provider) {
        String value = provider == null || provider.isBlank() ? "github" : provider.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_PROVIDERS.contains(value)) {
            throw new DomainException("VALIDATION_ERROR", "Invalid git provider: " + provider);
        }
        return value;
    }

    private static String normalizeRepository(String repository) {
        if (repository == null || repository.isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "repository is required (owner/name)");
        }
        String trimmed = repository.trim();
        String[] parts = trimmed.split("/", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "repository must be owner/name or workspace/slug");
        }
        return parts[0] + "/" + parts[1];
    }

    private static String generateSecret() {
        byte[] bytes = new byte[24];
        new java.security.SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static void verifySharedToken(String secret, String tokenHeader) {
        if (secret == null || secret.isBlank()) {
            throw new DomainException("CONFIG_ERROR", "Webhook secret not configured");
        }
        if (tokenHeader == null || tokenHeader.isBlank() || !secret.equals(tokenHeader.trim())) {
            throw new DomainException("AUTH_ERROR", "Invalid GitLab webhook token");
        }
    }

    private static void verifyHmacSha256Signature(
            String secret,
            String signatureHeader,
            String payload,
            String prefix
    ) {
        if (secret == null || secret.isBlank()) {
            throw new DomainException("CONFIG_ERROR", "Webhook secret not configured");
        }
        if (signatureHeader == null || !signatureHeader.startsWith(prefix)) {
            throw new DomainException("AUTH_ERROR", "Missing webhook signature");
        }
        String expected = signatureHeader.substring(prefix.length());
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String actual = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            if (!MessageDigest.isEqual(
                    actual.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                    expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
            )) {
                throw new DomainException("AUTH_ERROR", "Invalid webhook signature");
            }
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException("AUTH_ERROR", "Webhook signature verification failed");
        }
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
