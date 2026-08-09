package com.aistudio.application.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "aistudio.exports.siem-export-enabled", havingValue = "true")
public class SiemExportScheduler {

    private static final Logger log = LoggerFactory.getLogger(SiemExportScheduler.class);

    private final SiemExportService siemExportService;

    public SiemExportScheduler(SiemExportService siemExportService) {
        this.siemExportService = siemExportService;
    }

    @Scheduled(fixedDelayString = "${aistudio.exports.siem-export-interval-ms:300000}")
    public void exportPendingDlpEvents() {
        SiemExportService.SiemExportRunResult result = siemExportService.exportPendingEvents();
        if (result.exported() > 0) {
            log.info(
                    "SIEM export run: processed={} exported={} failed={}",
                    result.processed(),
                    result.exported(),
                    result.failed());
        }
    }
}
