package com.aistudio.application.ops;

import com.aistudio.api.ops.dto.ReleaseGateStatusResponse;
import com.aistudio.infrastructure.persistence.entity.StagingSignoffRunEntity;
import com.aistudio.infrastructure.persistence.repository.StagingSignoffRunRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReleaseGateService {

    private final StagingSignoffRunRepository runRepository;
    private final int maxAgeHours;

    public ReleaseGateService(
            StagingSignoffRunRepository runRepository,
            @Value("${aistudio.ops.release-gate-max-age-hours:48}") int maxAgeHours
    ) {
        this.runRepository = runRepository;
        this.maxAgeHours = maxAgeHours;
    }

    @Transactional(readOnly = true)
    public ReleaseGateStatusResponse evaluate(String imageTag) {
        String normalizedTag = imageTag == null || imageTag.isBlank() ? "unknown" : imageTag.trim();
        Instant since = Instant.now().minus(maxAgeHours, ChronoUnit.HOURS);
        return runRepository
                .findFirstByImageTagAndOverallAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        normalizedTag, "pass", since)
                .map(run -> new ReleaseGateStatusResponse(
                        true,
                        normalizedTag,
                        "Latest passing sign-off within " + maxAgeHours + "h",
                        run.getId(),
                        run.getCreatedAt(),
                        maxAgeHours))
                .orElseGet(() -> buildDenied(
                        normalizedTag,
                        "No passing sign-off for IMAGE_TAG=" + normalizedTag + " within " + maxAgeHours + "h"));
    }

    private ReleaseGateStatusResponse buildDenied(String imageTag, String reason) {
        return new ReleaseGateStatusResponse(false, imageTag, reason, null, null, maxAgeHours);
    }
}
