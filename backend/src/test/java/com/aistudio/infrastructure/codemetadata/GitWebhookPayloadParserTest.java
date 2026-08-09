package com.aistudio.infrastructure.codemetadata;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class GitWebhookPayloadParserTest {

    private final GitWebhookPayloadParser parser = new GitWebhookPayloadParser(new ObjectMapper());

    @Test
    void parsesGithubPushPathsForConfiguredBranch() {
        String payload = """
                {
                  "ref": "refs/heads/main",
                  "commits": [
                    {
                      "added": ["src/App.java"],
                      "modified": ["README.md"],
                      "removed": ["legacy/Old.java"]
                    }
                  ]
                }
                """;
        var delta = parser.parseGithub(payload, "main");
        assertThat(delta.changedPaths()).containsExactly("src/App.java", "README.md");
        assertThat(delta.removedPaths()).containsExactly("legacy/Old.java");
    }

    @Test
    void ignoresGithubPushOnOtherBranch() {
        String payload = """
                {
                  "ref": "refs/heads/develop",
                  "commits": [{"added": ["src/App.java"]}]
                }
                """;
        var delta = parser.parseGithub(payload, "main");
        assertThat(delta.hasChanges()).isFalse();
    }

    @Test
    void filtersNonCodePaths() {
        String payload = """
                {
                  "ref": "refs/heads/main",
                  "commits": [{"added": ["assets/logo.png", "src/App.java"]}]
                }
                """;
        var delta = parser.parseGithub(payload, "main");
        assertThat(delta.changedPaths()).containsExactly("src/App.java");
    }

    @Test
    void parsesBitbucketPushPaths() {
        String payload = """
                {
                  "push": {
                    "changes": [{
                      "new": {"name": "main", "type": "branch"},
                      "commits": [{
                        "added": [{"path": "src/Main.java"}],
                        "removed": [{"path": "old.txt"}]
                      }]
                    }]
                  }
                }
                """;
        var delta = parser.parseBitbucket(payload, "main");
        assertThat(delta.changedPaths()).containsExactly("src/Main.java");
        assertThat(delta.removedPaths()).containsExactly("old.txt");
    }
}
