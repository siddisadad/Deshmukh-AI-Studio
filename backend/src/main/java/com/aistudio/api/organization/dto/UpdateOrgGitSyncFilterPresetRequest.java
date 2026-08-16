package com.aistudio.api.organization.dto;

import java.util.Map;

public record UpdateOrgGitSyncFilterPresetRequest(String label, Map<String, String> filters) {}
