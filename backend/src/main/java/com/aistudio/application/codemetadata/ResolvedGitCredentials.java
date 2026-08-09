package com.aistudio.application.codemetadata;

public record ResolvedGitCredentials(
        String apiToken,
        String apiBaseUrl,
        GitCredentialSource source
) {

    public String effectiveBaseUrl(String platformDefault) {
        if (apiBaseUrl != null && !apiBaseUrl.isBlank()) {
            return apiBaseUrl.trim().replaceAll("/+$", "");
        }
        return platformDefault;
    }
}
