package com.aistudio.application.ai;

import com.aistudio.infrastructure.persistence.entity.ContextAssetEntity;
import com.aistudio.infrastructure.persistence.entity.DocumentEntity;
import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import com.aistudio.infrastructure.persistence.entity.RequirementEntity;
import com.aistudio.infrastructure.persistence.entity.TaskEntity;
import com.aistudio.infrastructure.persistence.repository.ContextAssetRepository;
import com.aistudio.infrastructure.persistence.repository.DocumentRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectRepository;
import com.aistudio.infrastructure.persistence.repository.RequirementRepository;
import com.aistudio.infrastructure.persistence.repository.TaskRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ContextBuilder {

    private final ProjectRepository projectRepository;
    private final RequirementRepository requirementRepository;
    private final TaskRepository taskRepository;
    private final DocumentRepository documentRepository;
    private final ContextAssetRepository contextAssetRepository;
    private final int maxRequirements;
    private final int maxTasks;
    private final int maxChars;

    public ContextBuilder(
            ProjectRepository projectRepository,
            RequirementRepository requirementRepository,
            TaskRepository taskRepository,
            DocumentRepository documentRepository,
            ContextAssetRepository contextAssetRepository,
            @Value("${aistudio.ai.context.max-requirements:50}") int maxRequirements,
            @Value("${aistudio.ai.context.max-tasks:100}") int maxTasks,
            @Value("${aistudio.ai.context.max-chars:48000}") int maxChars
    ) {
        this.projectRepository = projectRepository;
        this.requirementRepository = requirementRepository;
        this.taskRepository = taskRepository;
        this.documentRepository = documentRepository;
        this.contextAssetRepository = contextAssetRepository;
        this.maxRequirements = maxRequirements;
        this.maxTasks = maxTasks;
        this.maxChars = maxChars;
    }

    public String buildForProject(UUID projectId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        StringBuilder sb = new StringBuilder();
        sb.append("# Project\n")
                .append("Name: ").append(project.getName()).append('\n')
                .append("Key: ").append(project.getProjectKey()).append('\n')
                .append("Description: ").append(nullToEmpty(project.getDescription())).append("\n\n");

        List<ContextAssetEntity> assets = contextAssetRepository.findByProjectIdOrderByAssetTypeAsc(projectId);
        if (!assets.isEmpty()) {
            sb.append("# Context assets\n");
            for (ContextAssetEntity asset : assets) {
                sb.append("## [").append(asset.getAssetType()).append("] ").append(asset.getTitle()).append('\n');
                String body = nullToEmpty(asset.getContent());
                if (!body.isBlank()) {
                    sb.append(body.length() > 2000 ? body.substring(0, 2000) + "…" : body).append('\n');
                }
                sb.append('\n');
                if (overBudget(sb)) {
                    return truncate(sb);
                }
            }
        }

        List<RequirementEntity> requirements = requirementRepository
                .findByProjectIdOrderBySortOrderAscCreatedAtAsc(projectId);
        sb.append("# Requirements\n");
        int count = 0;
        for (RequirementEntity req : requirements) {
            if (count >= maxRequirements) {
                sb.append("…[additional requirements truncated]\n");
                break;
            }
            sb.append("- [").append(req.getPriority()).append("/").append(req.getStatus()).append("] ")
                    .append(req.getTitle()).append('\n')
                    .append("  ").append(nullToEmpty(req.getDescription()).replace("\n", " ")).append('\n');
            count++;
            if (overBudget(sb)) {
                return truncate(sb);
            }
        }

        List<TaskEntity> tasks = taskRepository.findByProjectIdOrderBySortOrderAscCreatedAtAsc(projectId);
        sb.append("\n# Tasks\n");
        int taskCount = 0;
        for (TaskEntity task : tasks) {
            if (taskCount >= maxTasks) {
                sb.append("…[additional tasks truncated]\n");
                break;
            }
            sb.append("- [").append(task.getStatus()).append("/").append(task.getPriority()).append("] ")
                    .append(task.getTitle()).append('\n');
            taskCount++;
            if (overBudget(sb)) {
                return truncate(sb);
            }
        }

        List<DocumentEntity> documents = documentRepository.findByProjectIdOrderByUpdatedAtDesc(projectId);
        sb.append("\n# Documents\n");
        int docCount = 0;
        for (DocumentEntity doc : documents) {
            if (docCount >= 20) {
                sb.append("…[additional documents truncated]\n");
                break;
            }
            sb.append("- [").append(doc.getDocType()).append("] ").append(doc.getTitle()).append('\n');
            String body = nullToEmpty(doc.getContentMd()).replace("\n", " ");
            if (!body.isBlank()) {
                sb.append("  ").append(body.length() > 240 ? body.substring(0, 240) + "…" : body).append('\n');
            }
            docCount++;
            if (overBudget(sb)) {
                return truncate(sb);
            }
        }
        return sb.toString();
    }

    private boolean overBudget(StringBuilder sb) {
        return sb.length() > maxChars;
    }

    private String truncate(StringBuilder sb) {
        sb.setLength(maxChars);
        sb.append("\n…[truncated]");
        return sb.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
