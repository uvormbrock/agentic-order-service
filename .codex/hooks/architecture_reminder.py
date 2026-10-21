"""Advisory only: never approve, rewrite or block a tool call."""
import json
import sys


def evaluate(event):
    if event.get("hook_event_name") != "PreToolUse":
        return {}
    text = json.dumps(event.get("tool_input", {}), ensure_ascii=False).replace("\\", "/")
    if "src/main/java/" not in text or ".java" not in text:
        return {}
    return {"hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "additionalContext": "Before production Java edits consult AGENTS.md, docs/02-architecture/system-architecture.md, relevant accepted ADRs and .codex/skills/hexagonal-architecture/SKILL.md. ADR-0007 accepts Option B with existing JPA mappings; Increment 0 permits no production migration. Skill guidance does not override accepted decisions or approval scope."
    }}


if __name__ == "__main__":
    try:
        event = json.load(sys.stdin)
        result = evaluate(event) if isinstance(event, dict) else {}
    except (ValueError, TypeError):
        result = {"systemMessage": "Architecture reminder input unavailable; follow AGENTS.md manually."}
    print(json.dumps(result))
