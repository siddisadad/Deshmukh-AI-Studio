package com.aistudio.application.ai;

import com.aistudio.infrastructure.persistence.entity.OrgAiCanaryOutcomeEntity;
import com.aistudio.infrastructure.persistence.repository.OrgAiCanaryOutcomeRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgAiCanaryOutcomeService {

    private final OrgAiCanaryOutcomeRepository repository;

    public OrgAiCanaryOutcomeService(OrgAiCanaryOutcomeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(UUID organizationId, boolean canaryRoute, boolean success) {
        if (organizationId == null) {
            return;
        }
        OrgAiCanaryOutcomeEntity row = repository.findById(organizationId)
                .orElseGet(() -> {
                    OrgAiCanaryOutcomeEntity entity = new OrgAiCanaryOutcomeEntity();
                    entity.setOrganizationId(organizationId);
                    return entity;
                });
        if (canaryRoute) {
            if (success) {
                row.setCanarySuccessCount(row.getCanarySuccessCount() + 1);
            } else {
                row.setCanaryFailureCount(row.getCanaryFailureCount() + 1);
            }
        } else if (success) {
            row.setStableSuccessCount(row.getStableSuccessCount() + 1);
        } else {
            row.setStableFailureCount(row.getStableFailureCount() + 1);
        }
        repository.save(row);
    }

    @Transactional(readOnly = true)
    public OrgAiCanaryOutcomeEntity metrics(UUID organizationId) {
        return repository.findById(organizationId).orElseGet(() -> {
            OrgAiCanaryOutcomeEntity empty = new OrgAiCanaryOutcomeEntity();
            empty.setOrganizationId(organizationId);
            return empty;
        });
    }

    @Transactional
    public void reset(UUID organizationId) {
        repository.findById(organizationId).ifPresent(row -> {
            row.setCanarySuccessCount(0L);
            row.setCanaryFailureCount(0L);
            row.setStableSuccessCount(0L);
            row.setStableFailureCount(0L);
            repository.save(row);
        });
    }
}
