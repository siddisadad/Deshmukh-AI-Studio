package com.aistudio.api.profile.dto;

import com.aistudio.domain.user.ThemePreference;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 1, max = 120) String displayName,
        ThemePreference theme
) {
}
