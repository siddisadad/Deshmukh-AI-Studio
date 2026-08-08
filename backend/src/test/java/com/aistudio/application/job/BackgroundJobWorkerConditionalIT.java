package com.aistudio.application.job;

import static org.junit.jupiter.api.Assertions.assertNull;

import com.aistudio.support.IntegrationTestProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class BackgroundJobWorkerConditionalIT {

    @Autowired(required = false)
    private BackgroundJobWorker worker;

    @DynamicPropertySource
    static void workerDisabled(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
        registry.add("aistudio.jobs.worker-enabled", () -> "false");
    }

    @Test
    void workerBeanNotRegisteredWhenDisabled() {
        assertNull(worker);
    }
}
