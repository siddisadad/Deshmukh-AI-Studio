package com.aistudio.api.ops;

import com.aistudio.api.ops.dto.ReleaseGateStatusResponse;
import com.aistudio.api.ops.dto.StagingSignoffRunResponse;
import com.aistudio.api.ops.dto.StagingSignoffSubmitRequest;
import com.aistudio.api.ops.dto.StagingSignoffSubmitResponse;
import com.aistudio.application.ops.ReleaseGateService;
import com.aistudio.application.ops.StagingSignoffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Ops")
public class OpsController {

    private final StagingSignoffService stagingSignoffService;
    private final ReleaseGateService releaseGateService;

    public OpsController(StagingSignoffService stagingSignoffService, ReleaseGateService releaseGateService) {
        this.stagingSignoffService = stagingSignoffService;
        this.releaseGateService = releaseGateService;
    }

    @PostMapping("/api/v1/ops/staging-signoff/submit")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit staging sign-off report (BILLING_USAGE_SYNC_TOKEN)")
    public StagingSignoffSubmitResponse submitSignoff(@Valid @RequestBody StagingSignoffSubmitRequest request) {
        return stagingSignoffService.submitReport(request.reportJson(), request.s3Uri());
    }

    @GetMapping("/api/v1/ops/staging-signoff/runs")
    @Operation(summary = "List recent staging sign-off runs (BILLING_USAGE_SYNC_TOKEN)")
    public List<StagingSignoffRunResponse> listSignoffRuns() {
        return stagingSignoffService.listRecentRuns();
    }

    @GetMapping("/api/v1/ops/release-gate")
    @Operation(summary = "Evaluate release gate for IMAGE_TAG (BILLING_USAGE_SYNC_TOKEN)")
    public ReleaseGateStatusResponse releaseGate(@RequestParam(name = "imageTag") String imageTag) {
        return releaseGateService.evaluate(imageTag);
    }
}
