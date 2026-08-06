package com.aistudio.api.task.dto;

import java.util.UUID;

public record LabelResponse(UUID id, UUID projectId, String name, String color) {
}
