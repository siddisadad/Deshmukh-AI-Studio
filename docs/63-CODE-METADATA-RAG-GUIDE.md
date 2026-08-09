# Code metadata RAG ingestion

**Version:** v0.2.58-beta  
**Scope:** Structured repository file metadata manifest API, persistence, and RAG indexing as `CODE_FILE` chunks.

Complements [62-RAG-LARGE-CORPUS-EMBEDDING-GUIDE.md](62-RAG-LARGE-CORPUS-EMBEDDING-GUIDE.md).

---

## Overview

Projects can upload a **code metadata manifest** — file paths, languages, optional snippets, and sizes — without connecting a live Git host. Each entry is embedded into `knowledge_chunks` with `source_type=CODE_FILE` for semantic search and chat RAG injection.

---

## Database

V34 migration:

- `project_code_files` — per-project path manifest (`UNIQUE(project_id, path)`)
- `knowledge_chunks` constraint extended with `CODE_FILE`

---

## Configuration

| Variable | Default | Purpose |
|----------|---------|---------|
| `RAG_MAX_CODE_FILES_PER_PROJECT` | `500` | Max manifest entries per project |

Corpus chunk limits still apply via `RAG_MAX_CHUNKS_PER_PROJECT`.

---

## API

| Method | Path | Notes |
|--------|------|-------|
| `GET` | `/api/v1/projects/{id}/code-metadata` | List manifest + `fileCount` / `maxFilesPerProject` |
| `PUT` | `/api/v1/projects/{id}/code-metadata` | Replace full manifest; triggers code-file reindex |

Example body:

```json
{
  "files": [
    {
      "path": "src/auth/LoginService.java",
      "language": "java",
      "snippet": "class LoginService { ... }",
      "sizeBytes": 1200
    }
  ]
}
```

Search hits return `sourceType: "CODE_FILE"`.

---

## UI

Project settings → **Code metadata (RAG)** — paste manifest JSON, save replaces manifest and reindexes.

---

## Staging deploy

```bash
export IMAGE_TAG=v0.2.58-beta
```

---

## Related

| Doc | Topic |
|-----|-------|
| [62-RAG-LARGE-CORPUS-EMBEDDING-GUIDE.md](62-RAG-LARGE-CORPUS-EMBEDDING-GUIDE.md) | Corpus limits and embedding batching |
