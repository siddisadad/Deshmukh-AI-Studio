# Streaming token UX guide

**Version:** v0.2.18-beta  
**Scope:** SSE chat streaming polish — live char count, RAF delta batching, usage metadata on `done`.

---

## SSE event flow

| Event | Payload | Purpose |
|-------|---------|---------|
| `user` | `ChatMessage` | Persisted user message |
| `delta` | `{ "text": "…" }` | Streaming token/chunk |
| `done` | assistant + provider + model + **usage** | Final message and metering |
| `error` | `{ "message": "…" }` | Stream failure |

### `done` usage object

```json
{
  "assistantMessage": { "id": "…", "sender": "ASSISTANT", "content": "…", "createdAt": "…" },
  "provider": "mock",
  "model": "mock-1",
  "usage": {
    "inputTokens": 42,
    "outputTokens": 128,
    "streamChars": 512,
    "deltaCount": 18
  }
}
```

- **inputTokens / outputTokens** — from the AI provider when available (mock uses chars÷4 estimate; OpenAI/Anthropic native streams return API usage on `done`).
- **streamChars** — total characters sent in `delta` events for this request.
- **deltaCount** — number of `delta` events emitted.

---

## Frontend behavior

1. **RAF batching** — `createDeltaBatcher` coalesces rapid `delta` events to one `requestAnimationFrame` flush for smoother rendering.
2. **Live char count** — chat UI shows `{streamingContent.length} chars` while streaming.
3. **Post-stream metering** — footer shows last `inputTokens` / `outputTokens` when the `done` event includes usage.

Reconnect and recovery polling (`stream-recovery`) do not include usage metadata.

---

## Staging smoke test

```bash
export IMAGE_TAG=v0.2.18-beta
./scripts/staging-ghcr-deploy.sh
```

1. Open project → AI Chat → send a message.
2. Confirm tokens appear incrementally with a pulsing cursor and rising char count.
3. After completion, footer should show provider/model and token hints (mock: estimated tokens).
4. Briefly disconnect network mid-stream — reconnect or recovery should still complete the message.

---

## Operator notes

- Usage in SSE is for **UX and debugging**; billing metering remains on `AiUsage` daily counts ([22-BILLING-USAGE-INVOICES-GUIDE.md](22-BILLING-USAGE-INVOICES-GUIDE.md)).
- Tune mock chunking via `AiProviderPort.chunkText` (backend default: ~24 chars, 12ms delay).

---

**Previous:** [22-BILLING-USAGE-INVOICES-GUIDE.md](22-BILLING-USAGE-INVOICES-GUIDE.md) · **Next:** [09-DEVELOPMENT-ROADMAP.md](09-DEVELOPMENT-ROADMAP.md)
