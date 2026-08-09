package com.aistudio.api.export;

import com.aistudio.api.export.dto.SiemExportRunResponse;
import com.aistudio.application.export.SiemExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Exports")
public class ExportSiemController {

    private final SiemExportService siemExportService;

    public ExportSiemController(SiemExportService siemExportService) {
        this.siemExportService = siemExportService;
    }

    @PostMapping("/api/v1/exports/siem/run")
    @Operation(summary = "Export pending DLP events to SIEM connectors (EXPORT_SIEM_SYNC_TOKEN)")
    public SiemExportRunResponse runSiemExport() {
        SiemExportService.SiemExportRunResult result = siemExportService.exportPendingEvents();
        return new SiemExportRunResponse(
                result.processed(),
                result.exported(),
                result.failed(),
                result.messages()
        );
    }
}
