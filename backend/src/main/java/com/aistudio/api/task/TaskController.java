package com.aistudio.api.task;

import com.aistudio.api.task.dto.CreateLabelRequest;
import com.aistudio.api.task.dto.CreateTaskRequest;
import com.aistudio.api.task.dto.LabelResponse;
import com.aistudio.api.task.dto.ReorderTasksRequest;
import com.aistudio.api.task.dto.TaskResponse;
import com.aistudio.api.task.dto.UpdateTaskRequest;
import com.aistudio.application.task.TaskService;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/api/v1/projects/{projectId}/labels")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create label")
    public LabelResponse createLabel(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateLabelRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return taskService.createLabel(projectId, user.getId(), request);
    }

    @GetMapping("/api/v1/projects/{projectId}/labels")
    @Operation(summary = "List labels")
    public List<LabelResponse> listLabels(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return taskService.listLabels(projectId, user.getId());
    }

    @DeleteMapping("/api/v1/labels/{labelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete label")
    public void deleteLabel(@PathVariable UUID labelId, @AuthenticationPrincipal AuthenticatedUser user) {
        taskService.deleteLabel(labelId, user.getId());
    }

    @PostMapping("/api/v1/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create task")
    public TaskResponse create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return taskService.create(projectId, user.getId(), request);
    }

    @GetMapping("/api/v1/projects/{projectId}/tasks")
    @Operation(summary = "List tasks")
    public List<TaskResponse> list(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return taskService.list(projectId, user.getId(), status, priority);
    }

    @GetMapping("/api/v1/tasks/{taskId}")
    @Operation(summary = "Get task")
    public TaskResponse get(@PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        return taskService.get(taskId, user.getId());
    }

    @PatchMapping("/api/v1/tasks/{taskId}")
    @Operation(summary = "Update task")
    public TaskResponse update(
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return taskService.update(taskId, user.getId(), request);
    }

    @DeleteMapping("/api/v1/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete task")
    public void delete(@PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        taskService.delete(taskId, user.getId());
    }

    @PatchMapping("/api/v1/projects/{projectId}/tasks/reorder")
    @Operation(summary = "Reorder / move tasks on the board")
    public List<TaskResponse> reorder(
            @PathVariable UUID projectId,
            @Valid @RequestBody ReorderTasksRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return taskService.reorder(projectId, user.getId(), request);
    }
}
