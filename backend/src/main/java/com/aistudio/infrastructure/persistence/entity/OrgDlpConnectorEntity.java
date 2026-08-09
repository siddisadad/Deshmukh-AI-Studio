package com.aistudio.infrastructure.persistence.entity;

import com.aistudio.domain.export.OrgDlpConnectorType;
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
@Table(name = "organization_dlp_connectors")
@Getter
@Setter
public class OrgDlpConnectorEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 64)
    private String slug;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "connector_type", nullable = false, length = 16)
    private OrgDlpConnectorType connectorType;

    @Column(name = "webhook_url", nullable = false, length = 512)
    private String webhookUrl;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "block_on_match", nullable = false)
    private boolean blockOnMatch = true;

    @Column(name = "custom_patterns_json")
    private String customPatternsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
