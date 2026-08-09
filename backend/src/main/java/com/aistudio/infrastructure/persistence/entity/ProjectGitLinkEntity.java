package com.aistudio.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "project_git_links")
@Getter
@Setter
public class ProjectGitLinkEntity {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false, unique = true)
    private UUID projectId;

    @Column(nullable = false, length = 20)
    private String provider = "github";

    @Column(nullable = false, length = 200)
    private String repository;

    @Column(nullable = false, length = 100)
    private String branch = "main";

    @Column(name = "webhook_secret", nullable = false, length = 128)
    private String webhookSecret;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "scheduled_sync_enabled", nullable = false)
    private boolean scheduledSyncEnabled = true;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "last_sync_status", nullable = false, length = 40)
    private String lastSyncStatus = "never";

    @Column(name = "last_sync_error", columnDefinition = "text")
    private String lastSyncError;

    @Column(name = "scheduled_sync_interval_minutes")
    private Integer scheduledSyncIntervalMinutes;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (branch == null || branch.isBlank()) {
            branch = "main";
        }
        if (provider == null || provider.isBlank()) {
            provider = "github";
        }
        if (lastSyncStatus == null || lastSyncStatus.isBlank()) {
            lastSyncStatus = "never";
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
