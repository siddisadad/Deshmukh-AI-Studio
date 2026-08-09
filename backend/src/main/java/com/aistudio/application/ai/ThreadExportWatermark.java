package com.aistudio.application.ai;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Embeds export provenance metadata into JSON payloads and Markdown footers.
 */
public final class ThreadExportWatermark {

    public record Metadata(
            UUID exportId,
            Instant exportedAt,
            UUID exportedByUserId,
            String notice
    ) {
    }

    private ThreadExportWatermark() {
    }

    public static void applyToJsonMap(Map<String, Object> map, Metadata metadata) {
        map.put("exportId", metadata.exportId());
        map.put("exportedByUserId", metadata.exportedByUserId());
        if (metadata.notice() != null && !metadata.notice().isBlank()) {
            map.put("watermarkNotice", metadata.notice());
        }
    }

    public static Map<String, Object> watermarkJsonMap(Metadata metadata) {
        Map<String, Object> watermark = new LinkedHashMap<>();
        watermark.put("exportId", metadata.exportId());
        watermark.put("exportedAt", metadata.exportedAt());
        watermark.put("exportedByUserId", metadata.exportedByUserId());
        if (metadata.notice() != null && !metadata.notice().isBlank()) {
            watermark.put("notice", metadata.notice());
        }
        return watermark;
    }

    public static String markdownFooter(Metadata metadata) {
        StringBuilder footer = new StringBuilder();
        footer.append("\n\n---\n\n");
        footer.append("*AI Studio export — ID `").append(metadata.exportId()).append("`");
        footer.append(" | exported at ").append(metadata.exportedAt());
        footer.append(" | user `").append(metadata.exportedByUserId()).append("*");
        if (metadata.notice() != null && !metadata.notice().isBlank()) {
            footer.append("\n\n*").append(metadata.notice()).append("*");
        }
        return footer.toString();
    }
}
