# RAG large corpus and embedding operations

**Version:** v0.2.57-beta  
**Scope:** HNSW vector index, configurable chunking, corpus limits, batched embedding calls, and Prometheus metrics.

---

## Overview

Knowledge RAG indexes requirements, documents, context assets, and tasks into `knowledge_chunks` with pgvector embeddings. This release adds production-oriented tuning for larger corpora and real embedding provider batching.

---

## Database

V33 migration adds an HNSW index for cosine similarity search:

```sql
CREATE INDEX idx_knowledge_chunks_embedding_hnsw
    ON knowledge_chunks USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
```

Apply via Flyway on deploy (standard API startup).

---

## Configuration

| Variable | Default | Purpose |
|----------|---------|---------|
| `EMBEDDING_PROVIDER` | `mock` | `mock` (CI/dev) or `openai` |
| `EMBEDDING_MODEL` | `text-embedding-3-small` | OpenAI embedding model |
| `EMBEDDING_BATCH_SIZE` | `64` | Texts per embedding API batch |
| `RAG_ENABLED` | `true` | Enable RAG indexing and retrieval |
| `RAG_TOP_K` | `8` | Default hits for chat prompt injection |
| `RAG_MAX_CHARS` | `6000` | Max chars injected into prompts |
| `RAG_MAX_CHUNKS_PER_PROJECT` | `10000` | Corpus cap per project |
| `RAG_CHUNK_SIZE` | `900` | Characters per chunk |
| `RAG_CHUNK_OVERLAP` | `120` | Overlap between chunks |
| `RAG_SEARCH_MAX_K` | `32` | Max hits for `/knowledge/search` |

OpenAI embeddings require `OPENAI_API_KEY` when `EMBEDDING_PROVIDER=openai`.

---

## API

Project-scoped (JWT):

| Method | Path | Notes |
|--------|------|-------|
| `GET` | `/api/v1/projects/{id}/knowledge` | Status including `maxChunksPerProject`, `corpusLimitReached` |
| `POST` | `/api/v1/projects/{id}/knowledge/reindex` | Sync rebuild; returns `corpusLimitReached` when truncated |
| `POST` | `/api/v1/projects/{id}/knowledge/reindex/async` | Background job (`KNOWLEDGE_REINDEX`) |
| `GET` | `/api/v1/projects/{id}/knowledge/search?q=` | Semantic search (`limit` up to `RAG_SEARCH_MAX_K`) |

Example status:

```json
{
  "enabled": true,
  "embeddingProvider": "mock",
  "indexedChunks": 42,
  "maxChunksPerProject": 10000,
  "corpusLimitReached": false
}
```

---

## Metrics

Prometheus counters:

| Metric | Description |
|--------|-------------|
| `aistudio.knowledge.embeddings.texts` | Chunk texts embedded |
| `aistudio.knowledge.embeddings.batches` | Embedding batch operations |

---

## UI

Project settings → **Knowledge index (RAG)** shows indexed / max chunks and a warning when the corpus limit is reached.

---

## Staging deploy

```bash
export IMAGE_TAG=v0.2.57-beta
# see docs/14-STAGING-DOGFOOD-GUIDE.md
```

---

## Related

| Doc | Topic |
|-----|-------|
| [18-JOB-WORKER-AUTOSCALING-GUIDE.md](18-JOB-WORKER-AUTOSCALING-GUIDE.md) | Background reindex workers |
