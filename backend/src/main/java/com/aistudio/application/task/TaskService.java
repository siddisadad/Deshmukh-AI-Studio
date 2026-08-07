package com.aistudio.application.task;

import com.aistudio.api.task.dto.CreateLabelRequest;
import com.aistudio.api.task.dto.CreateTaskRequest;
import com.aistudio.api.task.dto.LabelResponse;
import com.aistudio.api.task.dto.ReorderTasksRequest;
import com.aistudio.api.task.dto.TaskResponse;
import com.aistudio.api.task.dto.UpdateTaskRequest;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.common.Priority;
import com.aistudio.domain.task.TaskStatus;
import com.aistudio.infrastructure.persistence.entity.LabelEntity;
import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import com.aistudio.infrastructure.persistence.entity.TaskEntity;
import com.aistudio.infrastructure.persistence.repository.LabelRepository;
import com.aistudio.infrastructure.persistence.repository.MembershipRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectRepository;
import com.aistudio.infrastructure.persistence.repository.RequirementRepository;
import com.aistudio.infrastructure.persistence.repository.TaskRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final LabelRepository labelRepository;
    private final RequirementRepository requirementRepository;
    private final ProjectRepository projectRepository;
    private final MembershipRepository membershipRepository;
    private final ProjectAuthorizationService authorizationService;

    public TaskService(
            TaskRepository taskRepository,
            LabelRepository labelRepository,
            RequirementRepository requirementRepository,
            ProjectRepository projectRepository,
            MembershipRepository membershipRepository,
            ProjectAuthorizationService authorizationService
    ) {
        this.taskRepository = taskRepository;
        this.labelRepository = labelRepository;
        this.requirementRepository = requirementRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public LabelResponse createLabel(UUID projectId, UUID userId, CreateLabelRequest request) {
        authorizationService.requireProjectEdit(projectId, userId);
        if (labelRepository.existsByProjectIdAndNameIgnoreCase(projectId, request.name().trim())) {
            throw new DomainException("LABEL_TAKEN", "Label name already exists in this project");
        }
        LabelEntity label = new LabelEntity();
        label.setProjectId(projectId);
        label.setName(request.name().trim());
        label.setColor(request.color() == null ? "#6B7280" : request.color());
        labelRepository.save(label);
        return toLabelResponse(label);
    }

    @Transactional(readOnly = true)
    public List<LabelResponse> listLabels(UUID projectId, UUID userId) {
        authorizationService.requireProjectAccess(projectId, userId);
        return labelRepository.findByProjectIdOrderByNameAsc(projectId).stream()
                .map(TaskService::toLabelResponse)
                .toList();
    }

    @Transactional
    public void deleteLabel(UUID labelId, UUID userId) {
        LabelEntity label = labelRepository.findById(labelId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Label not found"));
        authorizationService.requireProjectEdit(label.getProjectId(), userId);
        labelRepository.delete(label);
    }

    @Transactional
    public TaskResponse create(UUID projectId, UUID userId, CreateTaskRequest request) {
        authorizationService.requireProjectEdit(projectId, userId);
        validateRequirement(projectId, request.requirementId());
        validateAssignee(projectId, request.assigneeId());

        TaskEntity task = new TaskEntity();
        task.setProjectId(projectId);
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setPriority(request.priority() == null ? Priority.MEDIUM : request.priority());
        task.setStatus(request.status() == null ? TaskStatus.TODO : request.status());
        task.setRequirementId(request.requirementId());
        task.setAssigneeId(request.assigneeId());
        task.setSortOrder((int) taskRepository.countByProjectIdAndStatus(projectId, task.getStatus()));
        task.setCreatedBy(userId);
        task.setLabels(resolveLabels(projectId, request.labelIds()));
        taskRepository.save(task);
        return toTaskResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(UUID projectId, UUID userId, String status, String priority) {
        authorizationService.requireProjectAccess(projectId, userId);
        return taskRepository.findByProjectIdOrderBySortOrderAscCreatedAtAsc(projectId).stream()
                .filter(t -> status == null || status.isBlank() || t.getStatus().name().equalsIgnoreCase(status))
                .filter(t -> priority == null || priority.isBlank() || t.getPriority().name().equalsIgnoreCase(priority))
                .map(TaskService::toTaskResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(UUID taskId, UUID userId) {
        TaskEntity task = taskRepository.findWithLabelsById(taskId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Task not found"));
        authorizationService.requireProjectAccess(task.getProjectId(), userId);
        return toTaskResponse(task);
    }

    @Transactional
    public TaskResponse update(UUID taskId, UUID userId, UpdateTaskRequest request) {
        TaskEntity task = taskRepository.findWithLabelsById(taskId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Task not found"));
        authorizationService.requireProjectEdit(task.getProjectId(), userId);

        if (request.title() != null && !request.title().isBlank()) {
            task.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (Boolean.TRUE.equals(request.clearRequirementId())) {
            task.setRequirementId(null);
        } else if (request.requirementId() != null) {
            validateRequirement(task.getProjectId(), request.requirementId());
            task.setRequirementId(request.requirementId());
        }
        if (Boolean.TRUE.equals(request.clearAssigneeId())) {
            task.setAssigneeId(null);
        } else if (request.assigneeId() != null) {
            validateAssignee(task.getProjectId(), request.assigneeId());
            task.setAssigneeId(request.assigneeId());
        }
        if (request.sortOrder() != null) {
            task.setSortOrder(request.sortOrder());
        }
        if (request.labelIds() != null) {
            task.setLabels(resolveLabels(task.getProjectId(), request.labelIds()));
        }
        taskRepository.save(task);
        return toTaskResponse(task);
    }

    @Transactional
    public void delete(UUID taskId, UUID userId) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Task not found"));
        authorizationService.requireProjectEdit(task.getProjectId(), userId);
        taskRepository.delete(task);
    }

    @Transactional
    public List<TaskResponse> reorder(UUID projectId, UUID userId, ReorderTasksRequest request) {
        authorizationService.requireProjectEdit(projectId, userId);
        for (ReorderTasksRequest.TaskOrderUpdate update : request.updates()) {
            TaskEntity task = taskRepository.findById(update.taskId())
                    .orElseThrow(() -> new DomainException("NOT_FOUND", "Task not found"));
            if (!task.getProjectId().equals(projectId)) {
                throw new DomainException("FORBIDDEN", "Task does not belong to this project");
            }
            task.setStatus(parseStatus(update.status()));
            task.setSortOrder(update.sortOrder());
            taskRepository.save(task);
        }
        return list(projectId, userId, null, null);
    }

    private void validateRequirement(UUID projectId, UUID requirementId) {
        if (requirementId == null) {
            return;
        }
        requirementRepository.findByIdAndProjectId(requirementId, projectId)
                .orElseThrow(() -> new DomainException("VALIDATION_ERROR", "Requirement not found in this project"));
    }

    private void validateAssignee(UUID projectId, UUID assigneeId) {
        if (assigneeId == null) {
            return;
        }
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Project not found"));
        membershipRepository.findByOrganizationIdAndUserId(project.getOrganizationId(), assigneeId)
                .orElseThrow(() -> new DomainException("VALIDATION_ERROR", "Assignee must be an organization member"));
    }

    private Set<LabelEntity> resolveLabels(UUID projectId, List<UUID> labelIds) {
        if (labelIds == null || labelIds.isEmpty()) {
            return new HashSet<>();
        }
        List<LabelEntity> labels = labelRepository.findByProjectIdAndIdIn(projectId, labelIds);
        if (labels.size() != labelIds.stream().distinct().count()) {
            throw new DomainException("VALIDATION_ERROR", "One or more labels are invalid for this project");
        }
        return new HashSet<>(labels);
    }

    private static TaskStatus parseStatus(String status) {
        try {
            return TaskStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new DomainException("VALIDATION_ERROR", "Invalid task status");
        }
    }

    private static LabelResponse toLabelResponse(LabelEntity label) {
        return new LabelResponse(label.getId(), label.getProjectId(), label.getName(), label.getColor());
    }

    private static TaskResponse toTaskResponse(TaskEntity task) {
        List<LabelResponse> labels = task.getLabels() == null
                ? List.of()
                : task.getLabels().stream().map(TaskService::toLabelResponse).toList();
        return new TaskResponse(
                task.getId(),
                task.getProjectId(),
                task.getRequirementId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getPriority().name(),
                task.getAssigneeId(),
                task.getSortOrder(),
                labels,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
