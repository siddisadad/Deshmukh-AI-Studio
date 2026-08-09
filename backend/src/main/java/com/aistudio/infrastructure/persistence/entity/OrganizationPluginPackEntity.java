package com.aistudio.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "organization_plugin_packs")
@IdClass(OrganizationPluginPackEntity.Pk.class)
@Getter
@Setter
public class OrganizationPluginPackEntity {

    @Id
    @Column(name = "organization_id")
    private UUID organizationId;

    @Id
    @Column(name = "pack_id", length = 64)
    private String packId;

    @Column(name = "installed_at", nullable = false)
    private Instant installedAt;

    @PrePersist
    void onCreate() {
        if (installedAt == null) {
            installedAt = Instant.now();
        }
    }

    public static class Pk implements Serializable {
        private UUID organizationId;
        private String packId;

        public Pk() {
        }

        public Pk(UUID organizationId, String packId) {
            this.organizationId = organizationId;
            this.packId = packId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Pk pk = (Pk) o;
            return Objects.equals(organizationId, pk.organizationId) && Objects.equals(packId, pk.packId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(organizationId, packId);
        }
    }
}
