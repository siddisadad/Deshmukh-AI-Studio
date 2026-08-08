# Provider-native streaming APIs

**Version:** v0.2.24-beta  
**Scope:** OpenAI and Anthropic chat use provider SSE streams (not simulated chunking) with usage on `done`.

Complements client SSE UX ([23-STREAMING-TOKEN-UX-GUIDE.md](23-STREAMING-TOKEN-UX-GUIDE.md).

---

## Provider behavior

| Provider | Stream API | Usage in stream |
|----------|------------|-----------------|
| **openai** | `POST /v1/chat/completions` `stream=true` + `stream_options.include_usage` | Final chunk `usage.prompt_tokens` / `completion_tokens` |
| **anthropic** | `POST /v1/messages` `stream=true` | `message_start` input tokens; `message_delta` output tokens |
| **mock** (default) | Simulated chunks via `chunkText` after `generate()` | Char÷4 estimates |

Chat always calls `AiProviderPort.stream()` from `ConversationService` — no separate env flag.

---

## Environment

```bash
AI_PROVIDER=openai   # or anthropic
OPENAI_API_KEY=sk-...
# OPENAI_MODEL=gpt-4o-mini
ANTHROPIC_API_KEY=sk-ant-...
```

Staging/CI default: `AI_PROVIDER=mock` (no external API).

---

## SSE to browser

Unchanged app contract:

1. `POST /conversations/{id}/messages/stream`
2. Events: `user` → `delta` → `done` (with `usage.inputTokens` / `outputTokens` when provider returns them)

Provider-native streams emit real token deltas (not fixed 24-char mock chunks).

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.24-beta
export AI_PROVIDER=openai
export OPENAI_API_KEY=sk-...
./scripts/staging-ghcr-deploy.sh
```

Send a chat message; footer should show API-reported token counts on `done`, not mock estimates.

---

## Related

| Doc | Topic |
|-----|-------|
| [23-STREAMING-TOKEN-UX-GUIDE.md](23-STREAMING-TOKEN-UX-GUIDE.md) | Client SSE + RAF batching |
| [07-AI-ARCHITECTURE.md](07-AI-ARCHITECTURE.md) | Provider ports |
