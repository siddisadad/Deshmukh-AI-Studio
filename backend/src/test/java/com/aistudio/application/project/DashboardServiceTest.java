package com.aistudio.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aistudio.domain.project.ProjectStatus;
import com.aistudio.domain.task.TaskStatus;
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
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock ProjectRepository projectRepository;
    @Mock ProjectMemberRepository projectMemberRepository;
    @Mock MembershipRepository membershipRepository;
    @Mock AuditLogRepository auditLogRepository;
    @Mock RequirementRepository requirementRepository;
    @Mock TaskRepository taskRepository;

    @InjectMocks DashboardService dashboardService;

    @Test
    void dashboardUsesBatchAggregatesInsteadOfPerProjectCounts() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        ProjectEntity project = new ProjectEntity();
        project.setId(projectId);
        project.setName("Portal");
        project.setProjectKey("CP");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setUpdatedAt(Instant.parse("2026-01-02T00:00:00Z"));

        ProjectMemberEntity membership = new ProjectMemberEntity();
        membership.setProjectId(projectId);
        membership.setUserId(userId);

        when(projectMemberRepository.findByUserId(userId)).thenReturn(List.of(membership));
        when(membershipRepository.findByUserId(userId)).thenReturn(List.of());
        when(projectRepository.findByIdInAndStatusOrderByUpdatedAtDesc(Set.of(projectId), ProjectStatus.ACTIVE))
                .thenReturn(List.of(project));

        when(requirementRepository.countGroupedByProjectId(Set.of(projectId)))
                .thenReturn(List.of(projectCount(projectId, 3)));
        when(taskRepository.countGroupedByProjectIdAndStatus(Set.of(projectId)))
                .thenReturn(List.of(
                        statusCount(projectId, TaskStatus.TODO, 2),
                        statusCount(projectId, TaskStatus.DONE, 1)));
        when(auditLogRepository.findByActorUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(List.of());

        var response = dashboardService.dashboard(userId);

        assertThat(response.projects()).hasSize(1);
        assertThat(response.projects().get(0).requirementCount()).isEqualTo(3);
        assertThat(response.projects().get(0).openTaskCount()).isEqualTo(2);
        assertThat(response.projects().get(0).doneTaskCount()).isEqualTo(1);

        verify(requirementRepository).countGroupedByProjectId(Set.of(projectId));
        verify(taskRepository).countGroupedByProjectIdAndStatus(Set.of(projectId));
        verify(requirementRepository, org.mockito.Mockito.never()).countByProjectId(any());
        verify(taskRepository, org.mockito.Mockito.never()).countByProjectIdAndStatus(any(), any());
        verify(taskRepository, org.mockito.Mockito.never()).countByProjectIdAndStatusNot(any(), any());
    }

    private static ProjectCountProjection projectCount(UUID projectId, long count) {
        return new ProjectCountProjection() {
            @Override
            public UUID getProjectId() {
                return projectId;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }

    private static ProjectStatusCountProjection statusCount(UUID projectId, TaskStatus status, long count) {
        return new ProjectStatusCountProjection() {
            @Override
            public UUID getProjectId() {
                return projectId;
            }

            @Override
            public TaskStatus getStatus() {
                return status;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }
}
