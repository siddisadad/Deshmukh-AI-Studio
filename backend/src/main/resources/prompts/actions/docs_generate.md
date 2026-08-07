---
version: 2
---
Generate markdown documentation for the project based on the shared context.

Document type: {{doc_type}}
Current title: {{title}}
Current content (may be empty):
{{content_md}}

## Output format (required)
Return markdown only for the document body — no wrapper code fences.
Use a `#` title matching or refining the current title, then logical `##` sections.

## Project context
{{project_context}}

## Extra instructions
{{instructions}}
