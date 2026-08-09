package com.aistudio.application.ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.aistudio.infrastructure.persistence.entity.StagingSignoffRunEntity;
import com.aistudio.infrastructure.persistence.repository.StagingSignoffRunRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StagingSignoffServiceTest {

    @Mock StagingSignoffRunRepository runRepository;
    @Spy ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks StagingSignoffService stagingSignoffService;

    @Test
    void parsesSingleHostReport() {
        when(runRepository.save(any(StagingSignoffRunEntity.class))).thenAnswer(invocation -> {
            StagingSignoffRunEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
        String json = """
                {
                  "timestamp": "20260809T120000Z",
                  "host": "https://staging.example.com",
                  "imageTag": "v0.2.54-beta",
                  "summary": { "pass": 10, "fail": 0, "skip": 2, "overall": "pass" }
                }
                """;
        var result = stagingSignoffService.submitReport(json, null);
        assertThat(result.overall()).isEqualTo("pass");
        assertThat(result.imageTag()).isEqualTo("v0.2.54-beta");
    }

    @Test
    void parsesMatrixReport() {
        when(runRepository.save(any(StagingSignoffRunEntity.class))).thenAnswer(invocation -> {
            StagingSignoffRunEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
        String json = """
                {
                  "timestamp": "20260809T120000Z",
                  "imageTag": "v0.2.54-beta",
                  "summary": { "environments": 2, "pass": 2, "fail": 0, "overall": "pass" },
                  "environments": []
                }
                """;
        var result = stagingSignoffService.submitReport(json, "s3://bucket/matrix");
        assertThat(result.overall()).isEqualTo("pass");
        assertThat(result.imageTag()).isEqualTo("v0.2.54-beta");
    }
}
