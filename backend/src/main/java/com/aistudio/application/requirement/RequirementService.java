package com.aistudio.application.requirement;

import com.aistudio.api.requirement.dto.CreateRequirementRequest;
import com.aistudio.api.requirement.dto.RequirementResponse;
import com.aistudio.api.requirement.dto.UpdateRequirementRequest;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.common.Priority;
import com.aistudio.domain.requirement.RequirementStatus;
import com.aistudio.infrastructure.persistence.entity.RequirementEntity;
import com.aistudio.infrastructure.persistence.repository.RequirementRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final ProjectAuthorizationService authorizationService;

    public RequirementService(
            RequirementRepository requirementRepository,
            ProjectAuthorizationService authorizationService
    ) {
        this.requirementRepository = requirementRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public RequirementResponse create(UUID projectId, UUID userId, CreateRequirementRequest request) {
        authorizationService.requireProjectEdit(projectId, userId);
        RequirementEntity entity = new RequirementEntity();
        entity.setProjectId(projectId);
        entity.setTitle(request.title().trim());
        entity.setDescription(request.description() == null ? "" : request.description());
        entity.setPriority(request.priority() == null ? Priority.MEDIUM : request.priority());
        entity.setStatus(request.status() == null ? RequirementStatus.DRAFT : request.status());
        entity.setSortOrder((int) requirementRepository.countByProjectId(projectId));
        entity.setCreatedBy(userId);
        requirementRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<RequirementResponse> list(UUID projectId, UUID userId) {
        authorizationService.requireProjectAccess(projectId, userId);
        return requirementRepository.findByProjectIdOrderBySortOrderAscCreatedAtAsc(projectId).stream()
                .map(RequirementService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RequirementResponse get(UUID requirementId, UUID userId) {
        RequirementEntity entity = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Requirement not found"));
        authorizationService.requireProjectAccess(entity.getProjectId(), userId);
        return toResponse(entity);
    }

    @Transactional
    public RequirementResponse update(UUID requirementId, UUID userId, UpdateRequirementRequest request) {
        RequirementEntity entity = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Requirement not found"));
        authorizationService.requireProjectEdit(entity.getProjectId(), userId);

        if (request.title() != null && !request.title().isBlank()) {
            entity.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        if (request.improvedDescription() != null) {
            entity.setImprovedDescription(request.improvedDescription());
        }
        if (request.userStories() != null) {
            entity.setUserStories(request.userStories());
        }
        if (request.acceptanceCriteria() != null) {
            entity.setAcceptanceCriteria(request.acceptanceCriteria());
        }
        if (request.priority() != null) {
            entity.setPriority(request.priority());
        }
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        requirementRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public void delete(UUID requirementId, UUID userId) {
        RequirementEntity entity = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Requirement not found"));
        authorizationService.requireProjectEdit(entity.getProjectId(), userId);
        requirementRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public RequirementEntity requireEditable(UUID requirementId, UUID userId) {
        RequirementEntity entity = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Requirement not found"));
        authorizationService.requireProjectEdit(entity.getProjectId(), userId);
        return entity;
    }

    public static RequirementResponse toResponse(RequirementEntity entity) {
        return new RequirementResponse(
                entity.getId(),
                entity.getProjectId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getImprovedDescription(),
                entity.getUserStories(),
                entity.getAcceptanceCriteria(),
                entity.getStatus().name(),
                entity.getPriority().name(),
                entity.getSortOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
