"""Check field presence using the authoritative template; never certify evidence."""
import json
from pathlib import Path
import re
import sys

CONTRACT = Path(__file__).resolve().parents[2] / "docs/04-agentic-coding/multi-agent/handoff-contract.md"


def required_labels():
    template = CONTRACT.read_text().split("```text", 1)[1].split("```", 1)[0]
    return re.findall(r"^([^ \n][^:\n]*):", template, re.MULTILINE)


def evaluate(event):
    if event.get("hook_event_name") != "SubagentStop" or event.get("agent_type") not in {"implementer", "tester", "reviewer"}:
        return {}
    message = event.get("last_assistant_message") or ""
    labels = required_labels()
    pattern = re.compile(r"^(" + "|".join(re.escape(label) for label in labels) + r"):[ \t]*(.*)$", re.MULTILINE)
    matches = list(pattern.finditer(message))
    values = {}
    for index, match in enumerate(matches):
        end = matches[index + 1].start() if index + 1 < len(matches) else len(message)
        values[match.group(1)] = (match.group(2) + message[match.end():end]).strip()
    missing = [label for label in labels if not values.get(label)]
    if not missing:
        return {}
    reason = "Complete the handoff from docs/04-agentic-coding/multi-agent/handoff-contract.md. Missing/nonempty fields required: " + ", ".join(missing) + ". Use explicit Pending/Not Applicable reasons for unrun checks; do not invent evidence or approvals."
    if event.get("stop_hook_active"):
        return {"systemMessage": "Handoff remains incomplete; Orchestrator must review manually. " + reason}
    return {"decision": "block", "reason": reason}


if __name__ == "__main__":
    try:
        event = json.load(sys.stdin)
        result = evaluate(event) if isinstance(event, dict) else {"systemMessage": "Handoff input invalid; manual review required."}
    except (ValueError, TypeError, OSError, IndexError):
        result = {"systemMessage": "Handoff check unavailable; manual contract verification required."}
    print(json.dumps(result))
