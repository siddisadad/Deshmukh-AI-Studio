# AI Architecture
## AI Studio for Software Engineering — MVP

| Field | Value |
|---|---|
| **Pattern** | Provider port + Prompt Manager + Context Builder + Conversation Manager |
| **Assistants** | BA, Developer, QA, Documentation Writer |
| **RAG** | pgvector knowledge chunks + mock/OpenAI embeddings |

---

## 1. Goals

1. Every assistant shares the **same project context**.
2. Providers are swappable (`mock`, `openai`, `anthropic`) without changing controllers.
3. Prompts are versioned templates, not hardcoded in controllers.
4. Conversation memory is explicit and budgeted.
5. Humans remain editors of all AI outputs.

---

## 2. Component Diagram

```
┌──────────────────┐
│  Ai Controllers  │
└────────┬─────────┘
         ▼
┌──────────────────────────────┐
│   AiOrchestrationService     │
│  - authorize project access  │
│  - select assistant + action │
│  - persist results/messages  │
└──────┬───────────┬───────────┘
       │           │
       ▼           ▼
┌─────────────┐  ┌──────────────────┐
│ PromptMgr   │  │ ContextBuilder   │
│ templates   │  │ project snapshot │
└──────┬──────┘  └────────┬─────────┘
       │                  │
       ▼                  ▼
┌─────────────────────────────────────┐
│        ConversationManager          │
│   recent N messages for assistant   │
└──────────────────┬──────────────────┘
                   ▼
┌─────────────────────────────────────┐
│           AiProviderPort            │
│  generate(system, messages, opts)   │
└───────┬─────────────┬───────────────┘
        ▼             ▼
   MockProvider   OpenAI / Anthropic
```

---

## 3. Core Ports

### 3.1 `AiProviderPort`
```java
public interface AiProviderPort {
    AiGenerationResult generate(AiGenerationRequest request);
    String providerId();
}

public record AiGenerationRequest(
    String systemPrompt,
    List<AiMessage> messages,      // role + content
    Double temperature,
    Integer maxOutputTokens,
    Map<String, String> metadata
) {}

public record AiGenerationResult(
    String text,
    String model,
    Integer inputTokens,           // nullable for mock
    Integer outputTokens,
    Map<String, Object> rawMeta
) {}
```

### 3.2 `PromptTemplatePort`
```java
String renderSystemPrompt(AssistantRole role);
String renderActionPrompt(AssistantRole role, String actionKey, Map<String, Object> vars);
```

### 3.3 Context builder output
```java
public record ProjectContextSnapshot(
    UUID projectId,
    String projectName,
    String projectDescription,
    List<RequirementSummary> requirements,
    List<TaskSummary> tasks,
    List<DocumentSummary> documents,
    List<ContextAssetSummary> assets,
    String renderedText,           // final prompt block
    int approxChars
) {}
```

---

## 4. Assistant Definitions

Each assistant has:

| Field | Description |
|---|---|
| `role` | Enum key |
| `name` | Display name |
| `systemPrompt` | Role, tone, quality bar |
| `capabilities` | Allowed action keys |
| `limitations` | Explicit non-goals (shown in UI) |
| `tools` | MVP: empty list (hook for Phase 2) |
| `contextPolicy` | Which context sections to include |

### 4.1 Business Analyst
- **Capabilities:** `improve_requirement`, `generate_user_stories`, `generate_acceptance_criteria`, `chat`
- **Limitations:** Does not invent business rules without labeling assumptions; does not write production code.
- **System prompt (summary):** Clarify requirements, remove ambiguity, structure stories/AC in Given/When/Then or checklist form; ask clarifying questions when critical info missing.

### 4.2 Developer
- **Capabilities:** `explain_implementation`, `api_suggestions`, `db_suggestions`, `code_examples`, `code_review`, `chat`
- **Limitations:** Does not deploy; treats pasted code as untrusted; prefers idiomatic stack defaults (Spring/React) unless project context says otherwise.
- **System prompt (summary):** Propose concrete technical designs grounded in project requirements/tasks/API/DB assets; show trade-offs briefly.

### 4.3 QA Engineer
- **Capabilities:** `generate_test_cases`, `api_test_scenarios`, `bug_report`, `regression_checklist`, `chat`
- **Limitations:** Does not execute tests in MVP; marks severity suggestions as recommendations.
- **System prompt (summary):** Derive test coverage from requirements and AC; include negative paths and edge cases.

### 4.4 Documentation Writer
- **Capabilities:** `generate_readme`, `api_documentation`, `release_notes`, `technical_documentation`, `chat`
- **Limitations:** Does not invent product claims not present in context.
- **System prompt (summary):** Write clear markdown docs for engineers; structure with headings; keep release notes user-facing.

---

## 5. Prompt Manager

### Storage
MVP: classpath resources

```
resources/prompts/
  assistants/
    business_analyst.system.md
    developer.system.md
    qa_engineer.system.md
    documentation_writer.system.md
  actions/
    ba_improve.md
    ba_user_stories.md
    ba_acceptance_criteria.md
    dev_api_suggestions.md
    ...
```

### Rendering
- Mustache/Handlebars-like or simple `{{var}}` replacement.
- Always inject: `project_context`, `user_instructions`, optional `requirement_block`.
- Include output format instructions (markdown sections) for parseability.

### Versioning
- Filename or front-matter `version: 1`.
- Log `promptVersion` in message `metadata` JSONB.

---

## 6. Context Builder

### Inputs (shared)
1. Project name + description + key  
2. Requirements (title, status, priority, description, improved, stories, AC)  
3. Tasks (title, status, priority, labels, linked requirement)  
4. Documents (title, type, truncated body)  
5. Context assets (DB design, API spec, source metadata)  
6. Optional: conversation summary (Phase 2)

### Budgeting algorithm (MVP)
1. Reserve chars for system prompt + user message + memory.  
2. Fill high-priority sections first: project → requirements → tasks → assets → documents.  
3. Truncate oldest/lowest-priority items with `…[truncated]`.  
4. Caps from config: `max-requirements`, `max-tasks`, `max-chars`.

### Consistency rule
**All assistants call the same `ContextBuilder.build(projectId, policy)`.**  
Policies may omit document bodies for BA actions if unused, but default policy is shared full snapshot within budget.

---

## 7. Conversation Manager

1. Ensure conversation row exists for `(projectId, assistantRole)`.  
2. Append USER message.  
3. Load last `N` messages (`aistudio.ai.context.max-messages`).  
4. Call provider with system + memory + new user content (+ context block in system or first developer message).  
5. Append ASSISTANT message with metadata (`provider`, `model`, `promptVersion`).  
6. Touch `conversations.updated_at`.

**Memory scope:** per project + assistant (shared among members). Phase 2: private threads.

---

## 8. Orchestration Flows

### Requirement AI action
```
authorize → load requirement → build context → render BA action prompt
→ provider.generate → save field on requirement → optional system chat note → return DTO
```

### Free-form chat
```
authorize → build context → load memory → render system prompt
→ provider.generate → persist both messages → return pair
```

### Document generate
```
authorize → build context → render docs action → provider.generate
→ update document.content_md → return document
```

---

## 9. Provider Implementations

| Provider | When | Notes |
|---|---|---|
| `MockAiProvider` | Default, CI, demos | Deterministic templates including role + truncated context hash |
| `OpenAiProvider` | `AI_PROVIDER=openai` | Chat Completions or Responses API |
| `AnthropicProvider` | `AI_PROVIDER=anthropic` | Messages API |

Selection via Spring `@ConditionalOnProperty` or factory bean `getProvider()`.

### Failure handling
- Timeouts (e.g. 60s).  
- Map 429 from provider → `429` or `503` with retry guidance.  
- Never echo API keys.  
- Record failed attempts in logs + optional audit.

---

## 10. RAG Readiness (Phase 2 hooks)

Define ports now even if unimplemented:

```java
public interface ProjectKnowledgePort {
    List<KnowledgeChunk> retrieve(UUID projectId, String query, int topK);
}
```

MVP `NoopProjectKnowledgePort` returns empty. Context builder can append chunks when present.

Future: embed documents/code metadata into pgvector or external store; index on document update.

---

## 11. Safety & Product Guardrails

1. System prompts instruct: label assumptions; prefer questions over fabrication for critical gaps.  
2. UI badges: “AI-generated — review before use”.  
3. All generated fields editable.  
4. Rate limit AI endpoints.  
5. Strip secrets patterns from context if detected (basic regex filter).  
6. Max input size on chat content (e.g. 20k chars).

---

## 12. Observability

Log (no raw PII beyond userId):  
`projectId`, `assistantRole`, `action`, `provider`, `model`, `latencyMs`, `approxContextChars`, `success`.

Optional: store token usage on message metadata for cost dashboards later.

---

## 13. Extending with a New Assistant

1. Add enum value + DB check constraint migration.  
2. Add system prompt file + capabilities list.  
3. Register in `AssistantRegistry`.  
4. Expose in `GET /assistants`.  
5. Add UI entry in `shared/constants/assistants.ts`.

No controller rewrite required if chat is generic by `assistantRole`.

---

## 14. Document Control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-06 | Provider/prompt/context/conversation design |

**Previous:** `06-FRONTEND-STRUCTURE.md` · **Next:** `08-UI-WIREFRAMES.md`
