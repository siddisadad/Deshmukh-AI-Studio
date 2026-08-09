package com.aistudio.application.codemetadata;

import com.aistudio.api.codemetadata.dto.ProjectGitLinkResponse;
import com.aistudio.api.codemetadata.dto.UpsertProjectGitLinkRequest;
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
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectGitSyncService {

    private final ProjectGitLinkRepository gitLinkRepository;
    private final ProjectAuthorizationService authorizationService;
    private final ProjectCodeMetadataService codeMetadataService;
    private final GitMetadataPort gitMetadataPort;
    private final BackgroundJobService backgroundJobService;
    private final String publicApiBaseUrl;

    public ProjectGitSyncService(
            ProjectGitLinkRepository gitLinkRepository,
            ProjectAuthorizationService authorizationService,
            ProjectCodeMetadataService codeMetadataService,
            GitMetadataPort gitMetadataPort,
            BackgroundJobService backgroundJobService,
            GitProperties gitProperties
    ) {
        this.gitLinkRepository = gitLinkRepository;
        this.authorizationService = authorizationService;
        this.codeMetadataService = codeMetadataService;
        this.gitMetadataPort = gitMetadataPort;
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
        ProjectGitLinkEntity entity = gitLinkRepository.findByProjectId(projectId)
                .orElseGet(() -> {
                    ProjectGitLinkEntity created = new ProjectGitLinkEntity();
                    created.setProjectId(projectId);
                    created.setWebhookSecret(generateSecret());
                    return created;
                });
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
        ProjectGitLinkEntity link = gitLinkRepository.findByProjectId(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Git link not configured"));
        verifyGithubSignature(link.getWebhookSecret(), signatureHeader, payload);
        backgroundJobService.enqueueInternal(projectId, JobType.CODE_METADATA_SYNC, Map.of("source", "webhook"));
    }

    private int syncLink(ProjectGitLinkEntity link) {
        try {
            List<GitFileEntry> files = gitMetadataPort.fetchRepositoryFiles(link.getRepository(), link.getBranch());
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

    private ProjectGitLinkResponse toResponse(ProjectGitLinkEntity entity) {
        return new ProjectGitLinkResponse(
                entity.getId(),
                entity.getProjectId(),
                entity.getProvider(),
                entity.getRepository(),
                entity.getBranch(),
                entity.isEnabled(),
                webhookUrl(entity.getProjectId()),
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
                webhookUrl(projectId),
                null,
                null,
                "never",
                null,
                null
        );
    }

    private String webhookUrl(UUID projectId) {
        return publicApiBaseUrl + "/api/v1/git/webhook/github/" + projectId;
    }

    private static String normalizeRepository(String repository) {
        if (repository == null || repository.isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "repository is required (owner/name)");
        }
        String trimmed = repository.trim();
        String[] parts = trimmed.split("/", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "repository must be owner/name");
        }
        return parts[0] + "/" + parts[1];
    }

    private static String generateSecret() {
        byte[] bytes = new byte[24];
        new java.security.SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static void verifyGithubSignature(String secret, String signatureHeader, String payload) {
        if (secret == null || secret.isBlank()) {
            throw new DomainException("CONFIG_ERROR", "Webhook secret not configured");
        }
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            throw new DomainException("AUTH_ERROR", "Missing GitHub webhook signature");
        }
        String expected = signatureHeader.substring("sha256=".length());
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String actual = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            if (!MessageDigest.isEqual(
                    actual.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                    expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
            )) {
                throw new DomainException("AUTH_ERROR", "Invalid GitHub webhook signature");
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
