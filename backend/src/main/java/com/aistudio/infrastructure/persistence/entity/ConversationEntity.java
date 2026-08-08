package com.aistudio.infrastructure.persistence.entity;

import com.aistudio.domain.ai.AssistantRole;
import com.aistudio.domain.ai.ConversationVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "conversations")
@Getter
@Setter
public class ConversationEntity {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "assistant_role", nullable = false, length = 40)
    private AssistantRole assistantRole;

    @Column(length = 200)
    private String title;

    @Column(name = "created_by")
    private UUID createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationVisibility visibility = ConversationVisibility.PROJECT;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "share_enabled", nullable = false)
    private boolean shareEnabled = false;

    @Column(name = "share_token_hash", length = 128)
    private String shareTokenHash;

    @Column(name = "share_expires_at")
    private Instant shareExpiresAt;

    @Column(name = "share_created_at")
    private Instant shareCreatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
