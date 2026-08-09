package com.aistudio.application.codemetadata;

import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.config.GitProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GitMetadataRegistry {

    private final Map<String, GitMetadataPort> ports;

    public GitMetadataRegistry(List<GitMetadataPort> portList, GitProperties properties) {
        Map<String, GitMetadataPort> map = new HashMap<>();
        for (GitMetadataPort port : portList) {
            map.put(port.providerId().toLowerCase(Locale.ROOT), port);
        }
        if (isMockMode(properties)) {
            GitMetadataPort mock = map.get("mock");
            if (mock != null) {
                for (String host : List.of("github", "gitlab", "bitbucket")) {
                    map.putIfAbsent(host, mock);
                }
            }
        }
        this.ports = Map.copyOf(map);
    }

    public GitMetadataPort require(String provider) {
        String key = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        GitMetadataPort port = ports.get(key);
        if (port == null) {
            throw new DomainException(
                    "CONFIG_ERROR",
                    "Git host connector not configured for provider: " + provider
            );
        }
        return port;
    }

    private static boolean isMockMode(GitProperties properties) {
        String provider = properties.provider();
        return provider == null || provider.isBlank() || "mock".equalsIgnoreCase(provider.trim());
    }
}
