package com.aistudio.application.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class PromptTemplateManager {

    private final ConcurrentHashMap<String, ParsedTemplate> cache = new ConcurrentHashMap<>();

    public String systemPrompt(String assistantKey) {
        return load("prompts/assistants/" + assistantKey + ".system.md").body();
    }

    public String systemPromptVersion(String assistantKey) {
        return load("prompts/assistants/" + assistantKey + ".system.md").version();
    }

    public String actionPrompt(String actionKey, Map<String, String> vars) {
        String template = load("prompts/actions/" + actionKey + ".md").body();
        String rendered = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return rendered;
    }

    public String actionPromptVersion(String actionKey) {
        return load("prompts/actions/" + actionKey + ".md").version();
    }

    private ParsedTemplate load(String path) {
        return cache.computeIfAbsent(path, this::readAndParse);
    }

    private ParsedTemplate readAndParse(String path) {
        return parseFrontMatter(readRaw(path));
    }

    private String readRaw(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Missing prompt template: " + path, e);
        }
    }

    static ParsedTemplate parseFrontMatter(String raw) {
        if (raw.startsWith("---")) {
            int end = raw.indexOf("---", 3);
            if (end > 0) {
                String frontMatter = raw.substring(3, end);
                String body = raw.substring(end + 3).stripLeading();
                String version = "1";
                for (String line : frontMatter.split("\n")) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("version:")) {
                        version = trimmed.substring("version:".length()).trim();
                    }
                }
                return new ParsedTemplate(version, body);
            }
        }
        return new ParsedTemplate("1", raw);
    }

    record ParsedTemplate(String version, String body) {
    }
}
