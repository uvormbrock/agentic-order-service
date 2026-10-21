"""Focused smoke checks; no real Git staging, commits, hook activation or builds."""
import importlib.util
import io
import json
import os
from pathlib import Path
import runpy
import subprocess
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]


def load(name):
    spec = importlib.util.spec_from_file_location(name, ROOT / ".codex/hooks" / (name + ".py"))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


reminder = load("architecture_reminder")
handoff = load("handoff_check")
precommit = runpy.run_path(str(ROOT / ".githooks/pre-commit"), run_name="hook_test")


class NativeHookSmoke(unittest.TestCase):
    def test_java_patch_reminder_is_advisory_and_short(self):
        result = reminder.evaluate({"hook_event_name": "PreToolUse", "tool_input": {"command": "*** Update File: src/main/java/example/A.java"}})
        output = result["hookSpecificOutput"]
        self.assertEqual("PreToolUse", output["hookEventName"])
        self.assertIn("AGENTS.md", output["additionalContext"])
        self.assertIn("system-architecture.md", output["additionalContext"])
        self.assertIn("hexagonal-architecture/SKILL.md", output["additionalContext"])
        self.assertLess(len(output["additionalContext"]), 700)
        self.assertNotIn("permissionDecision", output)

    def test_shell_path_gets_reminder(self):
        self.assertTrue(reminder.evaluate({"hook_event_name": "PreToolUse", "tool_input": {"cmd": "python edit src/main/java/example/A.java"}}))

    def test_nonproduction_tools_do_not_get_context(self):
        for name in ["docs/notes.md", "src/test/java/example/ATest.java"]:
            self.assertEqual({}, reminder.evaluate({"hook_event_name": "PreToolUse", "tool_input": {"file_path": name}}))

    def test_wrong_event_ignored(self):
        self.assertEqual({}, reminder.evaluate({"hook_event_name": "Stop", "tool_input": "src/main/java/A.java"}))
        self.assertEqual({}, handoff.evaluate({"hook_event_name": "Stop", "agent_type": "reviewer"}))

    def complete(self):
        return "\n".join(label + ": explicit smoke evidence; no edits; checks not applicable; approval Pending" for label in handoff.required_labels())

    def test_all_specialists_can_return_complete_handoff(self):
        for role in ["implementer", "tester", "reviewer"]:
            self.assertEqual({}, handoff.evaluate({"hook_event_name": "SubagentStop", "agent_type": role, "last_assistant_message": self.complete()}))

    def test_multiline_values_preserve_existing_handoff_format(self):
        text = "\n\n".join(label + ":\n- Explicit inspected paths or Pending evidence reason" for label in handoff.required_labels())
        self.assertEqual({}, handoff.evaluate({"hook_event_name": "SubagentStop", "agent_type": "reviewer", "last_assistant_message": text}))

    def test_missing_information_requests_correction(self):
        result = handoff.evaluate({"hook_event_name": "SubagentStop", "agent_type": "reviewer", "last_assistant_message": "Done"})
        self.assertEqual("block", result["decision"])
        self.assertIn("handoff-contract.md", result["reason"])

    def test_empty_field_does_not_consume_next_line(self):
        label = handoff.required_labels()[0]
        text = self.complete().replace(label + ": explicit smoke evidence; no edits; checks not applicable; approval Pending", label + ":")
        self.assertEqual("block", handoff.evaluate({"hook_event_name": "SubagentStop", "agent_type": "tester", "last_assistant_message": text})["decision"])

    def test_second_stop_warns_without_loop(self):
        result = handoff.evaluate({"hook_event_name": "SubagentStop", "agent_type": "tester", "stop_hook_active": True})
        self.assertNotIn("decision", result)
        self.assertIn("manual", result["systemMessage"])

    def test_unrelated_agent_ignored(self):
        self.assertEqual({}, handoff.evaluate({"hook_event_name": "SubagentStop", "agent_type": "explorer"}))

    def test_scripts_emit_valid_json_for_bad_input(self):
        for script in ["architecture_reminder", "handoff_check"]:
            result = subprocess.run(["python3", str(ROOT / ".codex/hooks" / (script + ".py"))], input="invalid", text=True, capture_output=True, check=True)
            self.assertIn("systemMessage", json.loads(result.stdout))

    def test_configured_native_commands_from_nested_cwd(self):
        config = json.loads((ROOT / ".codex/hooks.json").read_text())
        payloads = {"PreToolUse": {"tool_input": {"command": "src/main/java/A.java"}},
                    "SubagentStop": {"agent_type": "reviewer", "last_assistant_message": "incomplete"}}
        for event, payload in payloads.items():
            command = config["hooks"][event][0]["hooks"][0]["command"]
            payload["hook_event_name"] = event
            result = subprocess.run(command, shell=True, cwd=ROOT / "src", input=json.dumps(payload), text=True, capture_output=True, check=True)
            output = json.loads(result.stdout)
            self.assertTrue(output)
        # Direct command invocation proves command resolution, not lifecycle delivery/trust.


class GitHookSmoke(unittest.TestCase):
    def run_hook(self, outputs, exit_code=0, java="/jdk21"):
        namespace = precommit["main"].__globals__
        with patch.dict(namespace, {"git": unittest.mock.Mock(side_effect=outputs), "java_home": unittest.mock.Mock(return_value=java)}), \
                patch("os.chdir"), patch("subprocess.run", return_value=subprocess.CompletedProcess([], exit_code)) as run, \
                patch("sys.stdout", new_callable=io.StringIO):
            value = precommit["main"]()
            calls = run.call_args_list
        return value, calls

    def test_docs_only_skips_maven(self):
        code, calls = self.run_hook([str(ROOT).encode(), b"docs/a.md\0"])
        self.assertEqual(0, code)
        self.assertEqual([], calls)

    def test_java_runs_one_focused_lifecycle(self):
        code, calls = self.run_hook([str(ROOT).encode(), b"src/main/java/A.java\0", b"", b""])
        self.assertEqual(0, code)
        self.assertEqual(1, len(calls))
        self.assertEqual(['./mvnw', '-B', '-ntp', '-Dtest=ArchitectureComplianceTest', 'clean', 'test'], calls[0].args[0])
        self.assertEqual('/jdk21', calls[0].kwargs['env']['JAVA_HOME'])

    def test_build_failure_propagates(self):
        code, _ = self.run_hook([str(ROOT).encode(), b"src/test/java/ATest.java\0", b"", b""], exit_code=7)
        self.assertEqual(7, code)

    def test_deleted_java_still_runs_architecture(self):
        code, calls = self.run_hook([str(ROOT).encode(), b"src/main/java/Deleted.java\0", b"", b""])
        self.assertEqual(0, code)
        self.assertEqual(1, len(calls))

    def test_rename_away_from_java_requests_deleted_source_paths(self):
        namespace = precommit["main"].__globals__
        fake_git = unittest.mock.Mock(side_effect=[str(ROOT).encode(), b"src/main/java/A.java\0src/main/java/A.txt\0", b"", b""])
        with patch.dict(namespace, {"git": fake_git, "java_home": unittest.mock.Mock(return_value="/jdk21")}), \
                patch("os.chdir"), patch("subprocess.run", return_value=subprocess.CompletedProcess([], 0)) as run, \
                patch("sys.stdout", new_callable=io.StringIO):
            self.assertEqual(0, precommit["main"]())
            fake_git.assert_any_call('diff', '--cached', '--no-renames', '--name-only', '-z')
            self.assertEqual(1, run.call_count)

    def test_partial_staging_refused(self):
        with self.assertRaisesRegex(RuntimeError, 'Unstaged/untracked'):
            self.run_hook([str(ROOT).encode(), b"src/main/java/A.java\0", b"src/main/java/A.java\0"])

    def test_untracked_build_input_refused(self):
        with self.assertRaisesRegex(RuntimeError, 'Unstaged/untracked'):
            self.run_hook([str(ROOT).encode(), b"src/main/java/A.java\0", b"", b"src/main/java/B.java\0"])

    def test_java_21_checked(self):
        with patch.dict(os.environ, {'JAVA_HOME': '/jdk21'}), patch('subprocess.run', return_value=subprocess.CompletedProcess([], 0, stdout='openjdk version "21.0.6"')):
            self.assertEqual('/jdk21', precommit['java_home']())

    def test_wrong_java_refused(self):
        with patch.dict(os.environ, {'JAVA_HOME': '/jdk17'}), patch('subprocess.run', return_value=subprocess.CompletedProcess([], 0, stdout='openjdk version "17.0.1"')):
            with self.assertRaisesRegex(RuntimeError, 'Java 21'):
                precommit['java_home']()


if __name__ == '__main__':
    unittest.main()
