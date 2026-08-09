package com.aistudio.application.ops;

import com.aistudio.api.ops.dto.StagingSignoffRunResponse;
import com.aistudio.api.ops.dto.StagingSignoffSubmitResponse;
import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.ops.StagingSignoffRunType;
import com.aistudio.infrastructure.persistence.entity.StagingSignoffRunEntity;
import com.aistudio.infrastructure.persistence.repository.StagingSignoffRunRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StagingSignoffService {

    private final StagingSignoffRunRepository runRepository;
    private final ObjectMapper objectMapper;

    public StagingSignoffService(StagingSignoffRunRepository runRepository, ObjectMapper objectMapper) {
        this.runRepository = runRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<StagingSignoffRunResponse> listRecentRuns() {
        return runRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public StagingSignoffSubmitResponse submitReport(String reportJson, String s3Uri) {
        if (reportJson == null || reportJson.isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "reportJson is required");
        }
        try {
            JsonNode root = objectMapper.readTree(reportJson);
            ParsedReport parsed = parseReport(root);
            StagingSignoffRunEntity entity = new StagingSignoffRunEntity();
            entity.setRunType(parsed.runType());
            entity.setHost(parsed.host());
            entity.setEnvironmentLabel(parsed.environmentLabel());
            entity.setImageTag(parsed.imageTag());
            entity.setOverall(parsed.overall());
            entity.setPassCount(parsed.passCount());
            entity.setFailCount(parsed.failCount());
            entity.setSkipCount(parsed.skipCount());
            entity.setReportJson(reportJson.trim());
            entity.setS3Uri(blankToNull(s3Uri));
            StagingSignoffRunEntity saved = runRepository.save(entity);
            return new StagingSignoffSubmitResponse(saved.getId(), saved.getOverall(), saved.getImageTag());
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException("VALIDATION_ERROR", "Invalid sign-off report JSON: " + ex.getMessage());
        }
    }

    private ParsedReport parseReport(JsonNode root) {
        if (root.has("environments") && root.has("summary")) {
            return parseMatrixReport(root);
        }
        return parseSingleReport(root);
    }

    private ParsedReport parseSingleReport(JsonNode root) {
        JsonNode summary = root.path("summary");
        String overall = textOrDefault(summary, "overall", "fail").toLowerCase(Locale.ROOT);
        String imageTag = textOrDefault(root, "imageTag", "unknown");
        String host = textOrDefault(root, "host", "unknown");
        String environment = textOrBlank(root, "environment");
        return new ParsedReport(
                StagingSignoffRunType.SINGLE,
                host,
                environment,
                imageTag,
                overall,
                intOrZero(summary, "pass"),
                intOrZero(summary, "fail"),
                intOrZero(summary, "skip")
        );
    }

    private ParsedReport parseMatrixReport(JsonNode root) {
        JsonNode summary = root.path("summary");
        String overall = textOrDefault(summary, "overall", "fail").toLowerCase(Locale.ROOT);
        String imageTag = textOrDefault(root, "imageTag", "unknown");
        int pass = intOrZero(summary, "pass");
        int fail = intOrZero(summary, "fail");
        int environments = intOrZero(summary, "environments");
        int skip = Math.max(0, environments - pass - fail);
        return new ParsedReport(
                StagingSignoffRunType.MATRIX,
                "matrix",
                null,
                imageTag,
                overall,
                pass,
                fail,
                skip
        );
    }

    private StagingSignoffRunResponse toResponse(StagingSignoffRunEntity entity) {
        return new StagingSignoffRunResponse(
                entity.getId(),
                entity.getRunType().name(),
                entity.getHost(),
                entity.getEnvironmentLabel(),
                entity.getImageTag(),
                entity.getOverall(),
                entity.getPassCount(),
                entity.getFailCount(),
                entity.getSkipCount(),
                entity.getS3Uri(),
                entity.getCreatedAt()
        );
    }

    private static String textOrDefault(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return defaultValue;
        }
        return value.asText(defaultValue);
    }

    private static String textOrBlank(JsonNode node, String field) {
        String value = textOrDefault(node, field, "");
        return value.isBlank() ? null : value;
    }

    private static int intOrZero(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return 0;
        }
        return value.asInt(0);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record ParsedReport(
            StagingSignoffRunType runType,
            String host,
            String environmentLabel,
            String imageTag,
            String overall,
            int passCount,
            int failCount,
            int skipCount
    ) {
    }
}
