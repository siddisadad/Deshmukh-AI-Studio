package com.aistudio.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "project_code_files",
        uniqueConstraints = @UniqueConstraint(name = "uq_project_code_files_path", columnNames = {"project_id", "path"})
)
@Getter
@Setter
public class ProjectCodeFileEntity {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(nullable = false, length = 40)
    private String language = "";

    @Column(nullable = false, columnDefinition = "text")
    private String snippet = "";

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (language == null) {
            language = "";
        }
        if (snippet == null) {
            snippet = "";
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
