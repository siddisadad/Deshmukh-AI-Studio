package com.aistudio.application.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class PluginPackManifestLoader {

    private final ObjectMapper objectMapper;

    public PluginPackManifestLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<PluginPackManifest> loadAll() {
        List<PluginPackManifest> manifests = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:plugin-packs/*.json");
            for (Resource resource : resources) {
                try (InputStream in = resource.getInputStream()) {
                    manifests.add(objectMapper.readValue(in, PluginPackManifest.class));
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load plugin pack manifests", ex);
        }
        return manifests;
    }
}
