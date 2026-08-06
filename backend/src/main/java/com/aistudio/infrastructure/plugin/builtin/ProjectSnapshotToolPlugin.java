package com.aistudio.infrastructure.plugin.builtin;

import com.aistudio.application.ai.ContextBuilder;
import com.aistudio.application.plugin.spi.ToolPlugin;
import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import com.aistudio.infrastructure.persistence.repository.ProjectRepository;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ProjectSnapshotToolPlugin implements ToolPlugin {

    private final ProjectRepository projectRepository;
    private final ContextBuilder contextBuilder;

    public ProjectSnapshotToolPlugin(ProjectRepository projectRepository, ContextBuilder contextBuilder) {
        this.projectRepository = projectRepository;
        this.contextBuilder = contextBuilder;
    }

    @Override public String id() { return "core.tool.project_snapshot"; }
    @Override public String name() { return "Project Snapshot"; }
    @Override public String version() { return "1.0.0"; }
    @Override public String description() { return "Returns a truncated shared project context snapshot."; }
    @Override public boolean builtin() { return true; }
    @Override public String inputSchemaDescription() {
        return "Optional query string in arguments.query to bias context selection.";
    }

    @Override
    public ToolResult invoke(ToolContext context) {
        ProjectEntity project = projectRepository.findById(context.projectId()).orElse(null);
        if (project == null) {
            return ToolResult.fail("Project not found");
        }
        Object query = context.arguments() == null ? null : context.arguments().get("query");
        String focus = query == null ? project.getName() : String.valueOf(query);
        String snapshot = contextBuilder.buildForProject(context.projectId(), focus);
        if (snapshot.length() > 4000) {
            snapshot = snapshot.substring(0, 3997) + "...";
        }
        return new ToolResult(true, snapshot, Map.of(
                "projectKey", project.getProjectKey(),
                "chars", snapshot.length()
        ));
    }
}
