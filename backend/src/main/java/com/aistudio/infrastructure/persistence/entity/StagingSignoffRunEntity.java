package com.aistudio.infrastructure.persistence.entity;

import com.aistudio.domain.ops.StagingSignoffRunType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "staging_signoff_runs")
@Getter
@Setter
public class StagingSignoffRunEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "run_type", nullable = false, length = 16)
    private StagingSignoffRunType runType;

    @Column(nullable = false, length = 512)
    private String host;

    @Column(name = "environment_label", length = 64)
    private String environmentLabel;

    @Column(name = "image_tag", nullable = false, length = 64)
    private String imageTag;

    @Column(nullable = false, length = 8)
    private String overall;

    @Column(name = "pass_count", nullable = false)
    private int passCount;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Column(name = "skip_count", nullable = false)
    private int skipCount;

    @Column(name = "report_json", nullable = false, columnDefinition = "TEXT")
    private String reportJson;

    @Column(name = "s3_uri", length = 512)
    private String s3Uri;

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
