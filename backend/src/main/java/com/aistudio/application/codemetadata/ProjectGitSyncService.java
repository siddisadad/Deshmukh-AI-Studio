package com.aistudio.application.codemetadata;

import com.aistudio.api.codemetadata.dto.ProjectGitLinkResponse;
import com.aistudio.api.codemetadata.dto.UpsertProjectGitLinkRequest;
import com.aistudio.application.codemetadata.GitFileEntry;
import com.aistudio.application.job.BackgroundJobService;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.job.JobType;
import com.aistudio.infrastructure.config.GitProperties;
import com.aistudio.infrastructure.persistence.entity.ProjectGitLinkEntity;
import com.aistudio.infrastructure.persistence.repository.ProjectGitLinkRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectGitSyncService {

    private static final Set<String> ALLOWED_PROVIDERS = Set.of("github", "gitlab", "bitbucket", "mock");

    private final ProjectGitLinkRepository gitLinkRepository;
    private final ProjectAuthorizationService authorizationService;
    private final ProjectCodeMetadataService codeMetadataService;
    private final GitMetadataRegistry gitMetadataRegistry;
    private final BackgroundJobService backgroundJobService;
    private final String publicApiBaseUrl;

    public ProjectGitSyncService(
            ProjectGitLinkRepository gitLinkRepository,
            ProjectAuthorizationService authorizationService,
            ProjectCodeMetadataService codeMetadataService,
            GitMetadataRegistry gitMetadataRegistry,
            BackgroundJobService backgroundJobService,
            GitProperties gitProperties
    ) {
        this.gitLinkRepository = gitLinkRepository;
        this.authorizationService = authorizationService;
        this.codeMetadataService = codeMetadataService;
        this.gitMetadataRegistry = gitMetadataRegistry;
        this.backgroundJobService = backgroundJobService;
        this.publicApiBaseUrl = gitProperties.publicApiBaseUrl() == null || gitProperties.publicApiBaseUrl().isBlank()
                ? "http://localhost:8080"
                : gitProperties.publicApiBaseUrl().trim().replaceAll("/+$", "");
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
        if (request.regenerateWebhookSecret() != null && request.regenerateWebhookSecret()) {
            entity.setWebhookSecret(generateSecret());
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
        ProjectGitLinkEntity link = gitLinkRepository.findByProjectId(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Git link not configured"));
        if (!link.isEnabled()) {
            throw new DomainException("VALIDATION_ERROR", "Git link is disabled");
        }
        return syncLink(link);
    }

    @Transactional
    public void handleGithubWebhook(UUID projectId, String signatureHeader, String payload) {
        ProjectGitLinkEntity link = requireWebhookLink(projectId, "github");
        verifyHmacSha256Signature(link.getWebhookSecret(), signatureHeader, payload, "sha256=");
        enqueueWebhookSync(projectId);
    }

    @Transactional
    public void handleGitlabWebhook(UUID projectId, String tokenHeader, String payload) {
        ProjectGitLinkEntity link = requireWebhookLink(projectId, "gitlab");
        verifySharedToken(link.getWebhookSecret(), tokenHeader);
        enqueueWebhookSync(projectId);
    }

    @Transactional
    public void handleBitbucketWebhook(UUID projectId, String signatureHeader, String payload) {
        ProjectGitLinkEntity link = requireWebhookLink(projectId, "bitbucket");
        verifyHmacSha256Signature(link.getWebhookSecret(), signatureHeader, payload, "sha256=");
        enqueueWebhookSync(projectId);
    }

    private void enqueueWebhookSync(UUID projectId) {
        backgroundJobService.enqueueInternal(projectId, JobType.CODE_METADATA_SYNC, Map.of("source", "webhook"));
    }

    private int syncLink(ProjectGitLinkEntity link) {
        try {
            List<GitFileEntry> files = gitMetadataRegistry.require(link.getProvider())
                    .fetchRepositoryFiles(link.getRepository(), link.getBranch());
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

    private ProjectGitLinkResponse toResponse(ProjectGitLinkEntity entity) {
        return new ProjectGitLinkResponse(
                entity.getId(),
                entity.getProjectId(),
                entity.getProvider(),
                entity.getRepository(),
                entity.getBranch(),
                entity.isEnabled(),
                webhookUrl(entity.getProvider(), entity.getProjectId()),
                entity.getWebhookSecret(),
                entity.getLastSyncedAt(),
                entity.getLastSyncStatus(),
                entity.getLastSyncError(),
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
                webhookUrl("github", projectId),
                null,
                null,
                "never",
                null,
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
