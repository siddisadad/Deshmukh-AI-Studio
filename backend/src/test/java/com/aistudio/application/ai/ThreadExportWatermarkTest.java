package com.aistudio.application.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ThreadExportWatermarkTest {

    @Test
    void appliesJsonWatermarkFields() {
        var metadata = new ThreadExportWatermark.Metadata(
                java.util.UUID.fromString("00000000-0000-4000-8000-000000000001"),
                java.time.Instant.parse("2026-08-09T12:00:00Z"),
                java.util.UUID.fromString("00000000-0000-4000-8000-000000000002"),
                "Confidential");
        var map = new java.util.LinkedHashMap<String, Object>();
        ThreadExportWatermark.applyToJsonMap(map, metadata);
        assertThat(map.get("exportId")).isEqualTo(metadata.exportId());
        assertThat(map.get("exportedByUserId")).isEqualTo(metadata.exportedByUserId());
        assertThat(map.get("watermarkNotice")).isEqualTo("Confidential");
    }

    @Test
    void markdownFooterIncludesExportId() {
        var metadata = new ThreadExportWatermark.Metadata(
                java.util.UUID.fromString("00000000-0000-4000-8000-000000000003"),
                java.time.Instant.parse("2026-08-09T12:00:00Z"),
                java.util.UUID.fromString("00000000-0000-4000-8000-000000000004"),
                "Notice");
        String footer = ThreadExportWatermark.markdownFooter(metadata);
        assertThat(footer).contains("00000000-0000-4000-8000-000000000003");
        assertThat(footer).contains("Notice");
    }
}
