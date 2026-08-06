package com.aistudio.application.project;

import com.aistudio.api.dashboard.dto.DashboardResponse;
import com.aistudio.domain.project.ProjectStatus;
import com.aistudio.infrastructure.persistence.entity.AuditLogEntity;
import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import com.aistudio.infrastructure.persistence.entity.ProjectMemberEntity;
import com.aistudio.infrastructure.persistence.repository.AuditLogRepository;
import com.aistudio.infrastructure.persistence.repository.MembershipRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectMemberRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectRepository;
import com.aistudio.infrastructure.persistence.repository.RequirementRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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

    public DashboardService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            MembershipRepository membershipRepository,
            AuditLogRepository auditLogRepository,
            RequirementRepository requirementRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.membershipRepository = membershipRepository;
        this.auditLogRepository = auditLogRepository;
        this.requirementRepository = requirementRepository;
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

        List<DashboardResponse.ProjectSummary> summaries = projects.stream()
                .map(p -> new DashboardResponse.ProjectSummary(
                        p.getId(),
                        p.getName(),
                        p.getProjectKey(),
                        p.getStatus().name(),
                        requirementRepository.countByProjectId(p.getId()),
                        0L,
                        0L,
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
}
