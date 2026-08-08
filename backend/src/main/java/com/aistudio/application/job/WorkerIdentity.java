package com.aistudio.application.job;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WorkerIdentity {

    private final String configuredWorkerId;
    private String workerId;

    public WorkerIdentity(@Value("${aistudio.jobs.worker-id:}") String configuredWorkerId) {
        this.configuredWorkerId = configuredWorkerId == null ? "" : configuredWorkerId.trim();
    }

    @PostConstruct
    void init() {
        if (configuredWorkerId.isBlank()) {
            workerId = "worker-" + java.util.UUID.randomUUID();
        } else {
            workerId = configuredWorkerId;
        }
    }

    public String id() {
        return workerId;
    }
}
