package com.aistudio.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "thread_export_dlp_events")
@Getter
@Setter
public class ThreadExportDlpEventEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "export_id", nullable = false)
    private UUID exportId;

    @Column(name = "exported_by_user_id", nullable = false)
    private UUID exportedByUserId;

    @Column(name = "match_categories", nullable = false, length = 512)
    private String matchCategories;

    @Column(nullable = false)
    private boolean blocked;

    @Column(name = "siem_exported_at")
    private Instant siemExportedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
