package com.aistudio.application.ai;

import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import com.aistudio.infrastructure.persistence.entity.RequirementEntity;
import com.aistudio.infrastructure.persistence.entity.TaskEntity;
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
    private final int maxRequirements;
    private final int maxTasks;
    private final int maxChars;

    public ContextBuilder(
            ProjectRepository projectRepository,
            RequirementRepository requirementRepository,
            TaskRepository taskRepository,
            @Value("${aistudio.ai.context.max-requirements:50}") int maxRequirements,
            @Value("${aistudio.ai.context.max-tasks:100}") int maxTasks,
            @Value("${aistudio.ai.context.max-chars:48000}") int maxChars
    ) {
        this.projectRepository = projectRepository;
        this.requirementRepository = requirementRepository;
        this.taskRepository = taskRepository;
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
            if (sb.length() > maxChars) {
                sb.setLength(maxChars);
                sb.append("\n…[truncated]");
                return sb.toString();
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
            if (sb.length() > maxChars) {
                sb.setLength(maxChars);
                sb.append("\n…[truncated]");
                return sb.toString();
            }
        }
        return sb.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
