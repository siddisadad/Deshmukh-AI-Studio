package com.aistudio.api.profile.dto;

import java.util.List;
import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String displayName,
        String theme,
        List<OrgMembership> organizations
) {
    public record OrgMembership(UUID id, String name, String slug, String role) {
    }
}
