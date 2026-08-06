"""AI provider abstraction — Mock (default) or Anthropic when configured."""

from __future__ import annotations

import os
from abc import ABC, abstractmethod


class AIProvider(ABC):
    """Single-method interface so providers stay swappable."""

    @abstractmethod
    def generate(self, system_prompt: str, user_prompt: str) -> str:
        ...


class MockProvider(AIProvider):
    """Deterministic templated replies — no network, no credentials."""

    def generate(self, system_prompt: str, user_prompt: str) -> str:
        role_hint = "assistant"
        lower = system_prompt.lower()
        if "business analyst" in lower:
            role_hint = "Business Analyst"
        elif "developer" in lower and "qa" not in lower:
            role_hint = "Developer"
        elif "qa" in lower or "quality" in lower:
            role_hint = "QA Engineer"
        elif "documentation" in lower or "technical writer" in lower:
            role_hint = "Documentation Writer"

        prompt_lower = user_prompt.lower()

        if "improve" in prompt_lower and "description" in prompt_lower:
            return (
                f"[Mock {role_hint}] Improved description:\n\n"
                "This requirement has been clarified for implementability. "
                "It specifies the actor, the capability, measurable outcomes, "
                "and edge cases that must be handled. Constraints and "
                "non-goals are called out so scope stays bounded.\n\n"
                f"Original request excerpt:\n{user_prompt[:400]}"
            )

        if "user stor" in prompt_lower:
            return (
                f"[Mock {role_hint}] User stories:\n\n"
                "1. As a project member, I want to capture this capability "
                "clearly, so that the team shares a single understanding.\n"
                "2. As a developer, I want acceptance boundaries defined, "
                "so that implementation can be verified.\n"
                "3. As a QA engineer, I want concrete scenarios, so that "
                "tests map directly to expected behaviour."
            )

        if "acceptance" in prompt_lower:
            return (
                f"[Mock {role_hint}] Acceptance criteria:\n\n"
                "- Given a valid project context, when the feature is used, "
                "then the described outcome is achieved.\n"
                "- Given invalid or missing input, when the action is "
                "attempted, then a clear error is shown and no partial "
                "state is persisted.\n"
                "- Given existing related data, when the feature runs, "
                "then prior records remain consistent."
            )

        return (
            f"[Mock {role_hint}] Based on the current project context "
            f"(requirements and tasks), here is a response:\n\n"
            f"You asked: {user_prompt[:500]}\n\n"
            "I would prioritize clarifying open requirements, aligning "
            "tasks to those requirements, and verifying status on the "
            "Kanban board before expanding scope. Ask a follow-up if you "
            "want a more specific breakdown."
        )


class AnthropicProvider(AIProvider):
    """Wraps the Anthropic Messages API."""

    def __init__(self, api_key: str, model: str = "claude-sonnet-4-20250514"):
        import anthropic

        self._client = anthropic.Anthropic(api_key=api_key)
        self._model = model

    def generate(self, system_prompt: str, user_prompt: str) -> str:
        message = self._client.messages.create(
            model=self._model,
            max_tokens=2048,
            system=system_prompt,
            messages=[{"role": "user", "content": user_prompt}],
        )
        parts = []
        for block in message.content:
            text = getattr(block, "text", None)
            if text:
                parts.append(text)
        return "\n".join(parts) if parts else ""


def get_provider() -> AIProvider:
    """Use Anthropic when key + SDK are available; otherwise Mock."""
    api_key = os.environ.get("ANTHROPIC_API_KEY", "").strip()
    if not api_key:
        return MockProvider()

    try:
        import anthropic  # noqa: F401
    except ImportError:
        return MockProvider()

    return AnthropicProvider(api_key=api_key)
