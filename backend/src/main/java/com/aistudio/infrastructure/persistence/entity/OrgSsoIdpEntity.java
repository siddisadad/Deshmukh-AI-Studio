package com.aistudio.infrastructure.persistence.entity;

import com.aistudio.domain.auth.OrgSsoIdpProtocol;
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
@Table(name = "organization_sso_idps")
@Getter
@Setter
public class OrgSsoIdpEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 64)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private OrgSsoIdpProtocol protocol;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "issuer_uri", length = 512)
    private String issuerUri;

    @Column(name = "client_id", length = 256)
    private String clientId;

    @Column(name = "client_secret", length = 512)
    private String clientSecret;

    @Column(length = 256)
    private String scopes;

    @Column(name = "metadata_url", length = 512)
    private String metadataUrl;

    @Column(name = "entity_id", length = 256)
    private String entityId;

    @Column(name = "acs_url", length = 512)
    private String acsUrl;

    @Column(name = "sp_private_key")
    private String spPrivateKey;

    @Column(name = "sp_certificate")
    private String spCertificate;

    @Column(name = "want_encrypted_assertions", nullable = false)
    private boolean wantEncryptedAssertions = false;

    @Column(name = "metadata_json")
    private String metadataJson;

    @Column(name = "metadata_fetched_at")
    private Instant metadataFetchedAt;

    @Column(name = "metadata_refresh_error", length = 512)
    private String metadataRefreshError;

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

    public String providerId() {
        return "db-" + id;
    }
}
