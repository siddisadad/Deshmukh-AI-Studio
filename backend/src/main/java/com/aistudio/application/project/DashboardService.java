package com.aistudio.application.project;

import com.aistudio.api.dashboard.dto.DashboardResponse;
import com.aistudio.domain.project.ProjectStatus;
import com.aistudio.domain.task.TaskStatus;
import com.aistudio.infrastructure.persistence.entity.AuditLogEntity;
import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import com.aistudio.infrastructure.persistence.entity.ProjectMemberEntity;
import com.aistudio.infrastructure.persistence.repository.AuditLogRepository;
import com.aistudio.infrastructure.persistence.repository.MembershipRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectCountProjection;
import com.aistudio.infrastructure.persistence.repository.ProjectMemberRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectStatusCountProjection;
import com.aistudio.infrastructure.persistence.repository.RequirementRepository;
import com.aistudio.infrastructure.persistence.repository.TaskRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MembershipRepository membershipRepository;
    private final AuditLogRepository auditLogRepository;
    private final RequirementRepository requirementRepository;
    private final TaskRepository taskRepository;

    public DashboardService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            MembershipRepository membershipRepository,
            AuditLogRepository auditLogRepository,
            RequirementRepository requirementRepository,
            TaskRepository taskRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.membershipRepository = membershipRepository;
        this.auditLogRepository = auditLogRepository;
        this.requirementRepository = requirementRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(UUID userId) {
        Set<UUID> projectIds = new HashSet<>();
        for (ProjectMemberEntity member : projectMemberRepository.findByUserId(userId)) {
            projectIds.add(member.getProjectId());
        }
        membershipRepository.findByUserId(userId).forEach(membership ->
                projectRepository.findByOrganizationIdAndStatusOrderByUpdatedAtDesc(
                                membership.getOrganizationId(), ProjectStatus.ACTIVE)
                        .forEach(p -> projectIds.add(p.getId())));

        List<ProjectEntity> projects = projectIds.isEmpty()
                ? List.of()
                : projectRepository.findByIdInAndStatusOrderByUpdatedAtDesc(projectIds, ProjectStatus.ACTIVE);

        Map<UUID, Long> requirementCounts = batchRequirementCounts(projectIds);
        Map<UUID, Long> openTaskCounts = new HashMap<>();
        Map<UUID, Long> doneTaskCounts = new HashMap<>();
        batchTaskCounts(projectIds, openTaskCounts, doneTaskCounts);

        List<DashboardResponse.ProjectSummary> summaries = projects.stream()
                .map(p -> new DashboardResponse.ProjectSummary(
                        p.getId(),
                        p.getName(),
                        p.getProjectKey(),
                        p.getStatus().name(),
                        requirementCounts.getOrDefault(p.getId(), 0L),
                        openTaskCounts.getOrDefault(p.getId(), 0L),
                        doneTaskCounts.getOrDefault(p.getId(), 0L),
                        p.getUpdatedAt()
                ))
                .toList();

        List<AuditLogEntity> logs = auditLogRepository.findByActorUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, 10));
        List<DashboardResponse.ActivityItem> activity = new ArrayList<>();
        for (AuditLogEntity log : logs) {
            activity.add(new DashboardResponse.ActivityItem(
                    log.getAction(),
                    log.getEntityType(),
                    log.getEntityId(),
                    log.getCreatedAt()
            ));
        }
        activity.sort(Comparator.comparing(DashboardResponse.ActivityItem::createdAt).reversed());

        return new DashboardResponse(summaries, activity);
    }

    private Map<UUID, Long> batchRequirementCounts(Set<UUID> projectIds) {
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> counts = new HashMap<>();
        for (ProjectCountProjection row : requirementRepository.countGroupedByProjectId(projectIds)) {
            counts.put(row.getProjectId(), row.getCount());
        }
        return counts;
    }

    private void batchTaskCounts(
            Set<UUID> projectIds,
            Map<UUID, Long> openTaskCounts,
            Map<UUID, Long> doneTaskCounts
    ) {
        if (projectIds.isEmpty()) {
            return;
        }
        for (ProjectStatusCountProjection row : taskRepository.countGroupedByProjectIdAndStatus(projectIds)) {
            if (row.getStatus() == TaskStatus.DONE) {
                doneTaskCounts.put(row.getProjectId(), row.getCount());
            } else {
                openTaskCounts.merge(row.getProjectId(), row.getCount(), Long::sum);
            }
        }
    }
}
