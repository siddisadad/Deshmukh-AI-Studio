# Git sync file content hydration

**Version:** v0.2.61-beta  
**Scope:** Fetch file snippets from Git hosts during sync so `CODE_FILE` RAG indexes real content, not just paths.

Complements [64-GIT-CODE-METADATA-SYNC-GUIDE.md](64-GIT-CODE-METADATA-SYNC-GUIDE.md) and [65-GITLAB-BITBUCKET-SYNC-GUIDE.md](65-GITLAB-BITBUCKET-SYNC-GUIDE.md).

---

## Overview

Git tree sync lists code-like file paths. **Content hydration** fetches raw file bodies from GitHub, GitLab, or Bitbucket and stores truncated snippets on `project_code_files` before RAG reindex.

Hydration runs when `GIT_SYNC_FETCH_CONTENT=true` (default). Files larger than `GIT_SYNC_MAX_CONTENT_FETCH_BYTES` keep path-only metadata.

---

## Configuration

| Variable | Default | Purpose |
|----------|---------|---------|
| `GIT_SYNC_FETCH_CONTENT` | `true` | Enable post-tree file content fetch |
| `GIT_SYNC_MAX_SNIPPET_BYTES` | `4000` | Max UTF-8 bytes stored per snippet |
| `GIT_SYNC_MAX_CONTENT_FETCH_BYTES` | `512000` | Skip content fetch for larger files |

---

## Provider behavior

| Provider | Content API |
|----------|-------------|
| GitHub | `GET /repos/{owner}/{repo}/contents/{path}?ref={branch}` (base64 decode) |
| GitLab | `GET /projects/{id}/repository/files/{path}/raw?ref={branch}` |
| Bitbucket | `GET /repositories/{workspace}/{slug}/src/{commit}/{path}` |
| Mock | Snippets included in tree mock (hydration no-op) |

Failed per-file fetches are skipped; sync continues with path-only entries.

---

## Staging deploy

```bash
export IMAGE_TAG=v0.2.61-beta
export GIT_SYNC_FETCH_CONTENT=true
export GIT_SYNC_MAX_SNIPPET_BYTES=4000
```

---

## Tests

- `GitSnippetUtilsTest` — truncation and size gate
- `ProjectGitLinkControllerIT` — sync stores non-empty snippets from mock provider
