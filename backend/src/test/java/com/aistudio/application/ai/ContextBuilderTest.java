package com.aistudio.application.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.aistudio.application.knowledge.KnowledgeRetrievalService;
import com.aistudio.domain.common.Priority;
import com.aistudio.domain.requirement.RequirementStatus;
import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import com.aistudio.infrastructure.persistence.entity.RequirementEntity;
import com.aistudio.infrastructure.persistence.repository.ContextAssetRepository;
import com.aistudio.infrastructure.persistence.repository.DocumentRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectRepository;
import com.aistudio.infrastructure.persistence.repository.RequirementRepository;
import com.aistudio.infrastructure.persistence.repository.TaskRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContextBuilderTest {

    @Mock ProjectRepository projectRepository;
    @Mock RequirementRepository requirementRepository;
    @Mock TaskRepository taskRepository;
    @Mock DocumentRepository documentRepository;
    @Mock ContextAssetRepository contextAssetRepository;
    @Mock KnowledgeRetrievalService knowledgeRetrievalService;

    ContextBuilder contextBuilder;
    UUID projectId;

    @BeforeEach
    void setUp() {
        contextBuilder = new ContextBuilder(
                projectRepository,
                requirementRepository,
                taskRepository,
                documentRepository,
                contextAssetRepository,
                knowledgeRetrievalService,
                2,
                10,
                400
        );
        projectId = UUID.randomUUID();
    }

    @Test
    void buildIncludesProjectHeaderAndTruncatesRequirements() {
        ProjectEntity project = new ProjectEntity();
        project.setId(projectId);
        project.setName("Portal");
        project.setProjectKey("PORT");
        project.setDescription("Client portal");

        List<RequirementEntity> requirements = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            RequirementEntity req = new RequirementEntity();
            req.setTitle("Requirement " + i);
            req.setDescription("Detail " + i);
            req.setPriority(Priority.MUST);
            req.setStatus(RequirementStatus.DRAFT);
            requirements.add(req);
        }

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(contextAssetRepository.findByProjectIdOrderByAssetTypeAsc(projectId)).thenReturn(List.of());
        when(requirementRepository.findByProjectIdOrderBySortOrderAscCreatedAtAsc(projectId))
                .thenReturn(requirements);
        when(taskRepository.findByProjectIdOrderBySortOrderAscCreatedAtAsc(projectId)).thenReturn(List.of());
        when(documentRepository.findByProjectIdOrderByUpdatedAtDesc(projectId)).thenReturn(List.of());
        when(knowledgeRetrievalService.formatForPrompt(eq(projectId), any())).thenReturn("");

        String context = contextBuilder.buildForProject(projectId, "login");

        assertThat(context).contains("# Project");
        assertThat(context).contains("Name: Portal");
        assertThat(context).contains("Requirement 1");
        assertThat(context).contains("Requirement 2");
        assertThat(context).contains("additional requirements truncated");
        assertThat(context).doesNotContain("Requirement 5");
    }
}
