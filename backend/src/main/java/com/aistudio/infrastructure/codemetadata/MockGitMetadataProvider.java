package com.aistudio.infrastructure.codemetadata;

import com.aistudio.application.codemetadata.GitFileEntry;
import com.aistudio.application.codemetadata.GitMetadataPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "aistudio.git.provider", havingValue = "mock", matchIfMissing = true)
public class MockGitMetadataProvider implements GitMetadataPort {

    @Override
    public List<GitFileEntry> fetchRepositoryFiles(String repository, String branch) {
        String repo = repository == null ? "unknown/unknown" : repository.trim();
        String safeBranch = branch == null || branch.isBlank() ? "main" : branch.trim();
        return List.of(
                new GitFileEntry(
                        "src/main/java/" + repo.replace('/', '_') + "/App.java",
                        "java",
                        "public class App { // branch " + safeBranch + " }",
                        1800
                ),
                new GitFileEntry(
                        "src/main/java/" + repo.replace('/', '_') + "/AuthService.java",
                        "java",
                        "class AuthService { void login() { ... } }",
                        2400
                ),
                new GitFileEntry(
                        "README.md",
                        "markdown",
                        "# " + repo + " on " + safeBranch,
                        512
                )
        );
    }

    static String languageFromPath(String path) {
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot == path.length() - 1) {
            return "";
        }
        String ext = path.substring(dot + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "java" -> "java";
            case "kt", "kts" -> "kotlin";
            case "ts", "tsx" -> "typescript";
            case "js", "jsx" -> "javascript";
            case "py" -> "python";
            case "go" -> "go";
            case "rs" -> "rust";
            case "md" -> "markdown";
            case "yml", "yaml" -> "yaml";
            case "json" -> "json";
            case "sql" -> "sql";
            default -> ext;
        };
    }

    static List<GitFileEntry> filterCodePaths(List<GitTreeNode> nodes, int maxFiles) {
        List<GitFileEntry> out = new ArrayList<>();
        for (GitTreeNode node : nodes) {
            if (!"blob".equalsIgnoreCase(node.type())) {
                continue;
            }
            String path = node.path();
            if (path == null || path.isBlank() || !isCodeLikePath(path)) {
                continue;
            }
            out.add(new GitFileEntry(
                    path,
                    languageFromPath(path),
                    "",
                    node.size() <= 0 ? 0 : node.size()
            ));
            if (out.size() >= maxFiles) {
                break;
            }
        }
        return out;
    }

    private static boolean isCodeLikePath(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".java")
                || lower.endsWith(".kt")
                || lower.endsWith(".kts")
                || lower.endsWith(".ts")
                || lower.endsWith(".tsx")
                || lower.endsWith(".js")
                || lower.endsWith(".jsx")
                || lower.endsWith(".py")
                || lower.endsWith(".go")
                || lower.endsWith(".rs")
                || lower.endsWith(".md")
                || lower.endsWith(".yml")
                || lower.endsWith(".yaml")
                || lower.endsWith(".json")
                || lower.endsWith(".sql")
                || lower.endsWith(".xml")
                || lower.endsWith(".html")
                || lower.endsWith(".css")
                || lower.endsWith(".scss");
    }

    public record GitTreeNode(String path, String type, int size) {
    }
}
