"""Workflow smoke tests using disposable report fixtures, never application builds."""
import contextlib
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location('verification', Path(__file__).with_name('verify.py'))
VERIFY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VERIFY)


class VerificationSmoke(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / 'repo'
        self.root.mkdir()
        self.evidence = Path(self.temp.name) / 'evidence'
        self.evidence.mkdir()
        self.calls = []
        self.failure = None
        self.omit_reports = False
        self.jdk = '21'

    def fake_invoke(self, command, log, env):
        self.calls.append(command)
        log.write_text('[INFO] --- spotless:3.10.4:check (check) @ fixture ---\n'
                       '[INFO] --- compiler:3:compile (compile) @ fixture ---\n')
        if self.failure == command[0]:
            return 7
        if command[0] == './mvnw' and not self.omit_reports:
            for name in ['surefire-reports', 'failsafe-reports']:
                directory = self.root / 'target' / name
                directory.mkdir(parents=True)
                (directory / 'TEST-Fixture.xml').write_text('<testsuite name="com.example.agenticorderservice.architecture.ArchitectureComplianceTest" tests="3" failures="0" errors="0" skipped="0"/>')
            coverage = self.root / 'target/site/jacoco'
            coverage.mkdir(parents=True)
            (coverage / 'index.html').write_text('fixture coverage report')
            (coverage / 'jacoco.xml').write_text('<report><counter type="LINE" covered="2" missed="1"/></report>')
        if command[0] == 'make':
            directory = Path(env['MUTATION_EVIDENCE_ROOT']) / 'PIT-fixture/pit-reports'
            directory.mkdir(parents=True)
            (directory / 'mutations.xml').write_text('<mutations><mutation status="SURVIVED"/></mutations>')
        return 0

    def execute(self, mode, scope='all'):
        java = subprocess.CompletedProcess([], 0, stdout='java.specification.version = ' + self.jdk, stderr='')
        with patch.object(VERIFY, 'ROOT', self.root), patch.object(VERIFY, 'invoke', side_effect=self.fake_invoke), \
                patch.dict(os.environ, {'JAVA_HOME': '/fixture-jdk'}), patch.object(VERIFY.subprocess, 'run', return_value=java), \
                patch.object(VERIFY.subprocess, 'check_output', return_value='fixture\n'), contextlib.redirect_stdout(io.StringIO()) as output:
            code = VERIFY.execute(mode, scope if mode == 'full' else None, self.evidence)
        self.output = output.getvalue()
        return code, json.loads((self.evidence / 'summary.json').read_text())

    def test_fast_never_runs_integration_mutation_or_cloud(self):
        code, summary = self.execute('fast')
        self.assertEqual(0, code)
        self.assertEqual([['./mvnw', '-B', '-ntp', '-DfailIfNoTests=true', 'clean', 'test']], self.calls)
        self.assertEqual(3, summary['surefire']['tests'])
        self.assertNotIn('failsafe', summary)

    def test_full_archives_reports_before_mutation_and_requires_review(self):
        code, summary = self.execute('full')
        self.assertEqual(0, code)
        self.assertEqual('verify', self.calls[0][-1])
        self.assertEqual(['make', 'mutation-test', 'SCOPE=all'], self.calls[1])
        self.assertTrue((self.evidence / 'jacoco/jacoco.xml').is_file())
        self.assertEqual({'SURVIVED': 1}, summary['mutationOutcomes'])
        self.assertEqual('Pending', summary['independentReview'])
        self.assertEqual('Pending', summary['humanAcceptance'])

    def test_maven_failure_prevents_pit_and_propagates(self):
        self.failure = './mvnw'
        code, summary = self.execute('full')
        self.assertEqual(7, code)
        self.assertEqual(1, len(self.calls))
        self.assertIn('blocked', summary['pit'])

    def test_zero_exit_without_test_reports_is_not_success(self):
        self.omit_reports = True
        code, summary = self.execute('full')
        self.assertEqual(3, code)
        self.assertEqual(1, len(self.calls))
        self.assertIn('Missing fresh test XML', summary['verificationError'])

    def test_mutation_failure_propagates_without_coverage_loss(self):
        self.failure = 'make'
        code, summary = self.execute('full')
        self.assertEqual(7, code)
        self.assertTrue((self.evidence / 'jacoco/jacoco.xml').is_file())
        self.assertIn('Failed', summary['pit'])

    def test_wrong_jdk_prevents_build(self):
        self.jdk = '17'
        code, summary = self.execute('full')
        self.assertEqual(3, code)
        self.assertEqual([], self.calls)
        self.assertIn('Java 21', summary['verificationError'])

    def test_competing_wrapper_fails_without_running_build(self):
        with patch.object(VERIFY, 'ROOT', self.root), VERIFY.build_lock():
            with self.assertRaisesRegex(RuntimeError, 'Another verify'):
                with VERIFY.build_lock():
                    self.fail('Competing lock acquired')

    def test_fast_console_shows_actual_results_and_duration(self):
        code, summary = self.execute('fast')
        self.assertEqual(0, code)
        self.assertIn('Verification PASS (fast, execution checks)', self.output)
        self.assertIn('3 executed, 3 passed, 0 failed, 0 errors, 0 skipped', self.output)
        self.assertIn('Architecture: PASS', self.output)
        self.assertIn('Spotless: PASS', self.output)
        self.assertRegex(self.output, r'checks\) \| \d+\.\ds')
        self.assertIn(str(self.evidence / 'summary.json'), self.output)
        self.assertNotIn('PostgreSQL integration', self.output)
        self.assertEqual({'mode', 'scope', 'startedUtc', 'maven', 'pit', 'independentReview',
                          'humanAcceptance', 'sonarCloud', 'mavenCommand', 'mavenExit',
                          'surefire', 'exitCode'}, set(summary))

    def test_full_console_separates_execution_from_mutation_review(self):
        self.execute('full')
        self.assertIn('Verification PASS (full, execution checks)', self.output)
        self.assertIn('PostgreSQL integration: PASS', self.output)
        self.assertIn('line 66.67% (2/3)', self.output)
        self.assertIn('1 survived', self.output)
        self.assertIn('Mutation-quality disposition: PENDING', self.output)
        self.assertIn(str(self.evidence / 'jacoco/index.html'), self.output)

    def test_missing_evidence_never_prints_pass(self):
        self.omit_reports = True
        code, _ = self.execute('fast')
        self.assertEqual(3, code)
        self.assertIn('Verification FAIL', self.output)
        self.assertIn('Fast tests: NOT VERIFIED', self.output)
        self.assertIn('Architecture: NOT VERIFIED', self.output)
        self.assertIn('Missing fresh test XML', self.output)

    def test_jdk_failure_has_no_stale_test_or_format_pass(self):
        self.jdk = '17'
        self.execute('fast')
        self.assertIn('Verification FAIL', self.output)
        self.assertIn('Java 21', self.output)
        self.assertNotIn(': PASS', self.output)

    def test_failed_maven_prints_named_failures_and_preserves_exit_json(self):
        self.failure = './mvnw'
        original = self.fake_invoke
        def failure_reports(command, log, env):
            code = original(command, log, env)
            directory = self.root / 'target/surefire-reports'
            directory.mkdir(parents=True)
            (directory / 'TEST-Failed.xml').write_text(
                '<testsuite name="ArchitectureComplianceTest" tests="4" failures="1" errors="1" skipped="1">'
                '<testcase classname="BoundaryTest" name="domainIsIsolated"><failure message="forbidden API dependency"/></testcase>'
                '<testcase classname="BoundaryTest" name="loadsClasses"><error type="ClassNotFoundException"/></testcase>'
                '</testsuite>')
            return code
        self.fake_invoke = failure_reports
        code, summary = self.execute('fast')
        self.assertEqual(7, code)
        self.assertIn('Verification FAIL', self.output)
        self.assertIn('3 executed, 1 passed, 1 failed, 1 errors, 1 skipped', self.output)
        self.assertIn('BoundaryTest#domainIsIsolated: forbidden API dependency', self.output)
        self.assertIn('BoundaryTest#loadsClasses: ClassNotFoundException', self.output)
        self.assertEqual(7, summary['exitCode'])
        self.assertNotIn('surefire', summary)  # Existing failure-path JSON contract unchanged.
        self.assertFalse((self.evidence / 'surefire-reports').exists())

    def test_zero_exit_with_skips_is_not_console_pass_and_json_unchanged(self):
        original = self.fake_invoke
        def skipped(command, log, env):
            code = original(command, log, env)
            for path in (self.root / 'target').glob('*-reports/TEST-*.xml'):
                path.write_text(path.read_text().replace('skipped="0"', 'skipped="1"'))
            return code
        self.fake_invoke = skipped
        code, summary = self.execute('fast')
        self.assertEqual(0, code)  # Preserve existing skip/exit policy.
        self.assertEqual(1, summary['surefire']['skipped'])
        self.assertIn('Verification FAIL', self.output)
        self.assertNotIn('Architecture: PASS', self.output)

    def test_absent_architecture_report_is_not_invented(self):
        original = self.fake_invoke
        def non_architecture(command, log, env):
            code = original(command, log, env)
            for path in (self.root / 'target').glob('*-reports/TEST-*.xml'):
                path.write_text(path.read_text().replace('ArchitectureComplianceTest', 'OtherTest'))
            return code
        self.fake_invoke = non_architecture
        self.execute('fast')
        self.assertIn('Verification FAIL', self.output)
        self.assertIn('Architecture: NOT VERIFIED', self.output)

    def test_missing_spotless_evidence_does_not_invent_pass(self):
        original = self.fake_invoke
        def no_spotless(command, log, env):
            code = original(command, log, env)
            log.write_text('No formatter evidence\n')
            return code
        self.fake_invoke = no_spotless
        self.execute('fast')
        self.assertIn('Verification FAIL', self.output)
        self.assertIn('Spotless: NOT VERIFIED', self.output)

    def test_spotless_failure_names_the_check_and_shows_bounded_error(self):
        self.failure = './mvnw'
        original = self.fake_invoke
        def bad_format(command, log, env):
            code = original(command, log, env)
            log.write_text('[ERROR] Failed to execute goal spotless:check: formatting violations\n')
            return code
        self.fake_invoke = bad_format
        self.execute('fast')
        self.assertIn('Spotless: FAIL', self.output)
        self.assertIn('Error: [ERROR] Failed to execute goal spotless:check', self.output)

    def test_pit_failure_is_visible_with_original_nonzero_exit(self):
        self.failure = 'make'
        code, _ = self.execute('full')
        self.assertEqual(7, code)
        self.assertIn('Verification FAIL', self.output)
        self.assertIn('PIT command: FAIL (exit 7)', self.output)
        self.assertIn('Mutation-quality disposition: PENDING', self.output)

    def test_old_reports_are_not_presented_as_current_success(self):
        self.omit_reports = True
        directory = self.root / 'target/surefire-reports'
        directory.mkdir(parents=True)
        path = directory / 'TEST-Old.xml'
        path.write_text('<testsuite name="ArchitectureComplianceTest" tests="3" failures="0" errors="0" skipped="0"/>')
        os.utime(path, (1, 1))
        self.execute('fast')
        self.assertIn('Fast tests: NOT VERIFIED', self.output)
        self.assertIn('Verification FAIL', self.output)

    def test_console_rendering_leaves_evidence_bytes_unchanged(self):
        self.execute('full')
        before = {str(p): p.read_bytes() for p in self.evidence.rglob('*') if p.is_file()}
        summary = json.loads((self.evidence / 'summary.json').read_text())
        with contextlib.redirect_stdout(io.StringIO()):
            VERIFY.console_summary(summary, self.evidence, 1.5, 0)
        after = {str(p): p.read_bytes() for p in self.evidence.rglob('*') if p.is_file()}
        self.assertEqual(before, after)

    def test_presentation_error_does_not_rewrite_machine_report_or_exit(self):
        with patch.object(VERIFY, 'console_summary', side_effect=ValueError('presentation failure')):
            code, summary = self.execute('fast')
        self.assertEqual(0, code)
        self.assertNotIn('verificationError', summary)
        self.assertIn('console evidence unavailable', self.output)

    def test_full_cli_defaults_to_all_and_preserves_explicit_scopes(self):
        for scope in [None, *sorted(VERIFY.SCOPES)]:
            with self.subTest(scope=scope), tempfile.TemporaryDirectory() as directory:
                arguments = ['verify.py', 'full'] + (['--scope', scope] if scope else [])
                with patch('sys.argv', arguments), patch.dict(os.environ, {'VERIFICATION_EVIDENCE_ROOT': directory}), \
                        patch.object(VERIFY, 'build_lock', return_value=contextlib.nullcontext()), \
                        patch.object(VERIFY, 'execute', return_value=7) as execute:
                    self.assertEqual(7, VERIFY.main())
                    self.assertEqual(('full', scope or 'all'), execute.call_args.args[:2])

    def test_invalid_explicit_scope_is_rejected_before_execution(self):
        with patch('sys.argv', ['verify.py', 'full', '--scope', 'unknown']), \
                patch.object(VERIFY, 'execute') as execute, contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit) as caught:
                VERIFY.main()
        self.assertEqual(2, caught.exception.code)
        execute.assert_not_called()

    def test_make_full_defaults_to_all_and_preserves_explicit_scopes(self):
        env = os.environ.copy()
        for name in ('SCOPE', 'MAKEFLAGS', 'MFLAGS', 'MAKEOVERRIDES'):
            env.pop(name, None)
        for scope in [None, *sorted(VERIFY.SCOPES)]:
            with self.subTest(scope=scope):
                command = ['make', '--dry-run', 'verify-full'] + (['SCOPE=' + scope] if scope else [])
                result = subprocess.run(command, cwd=VERIFY.ROOT, env=env, text=True,
                                        capture_output=True, check=True)
                self.assertIn('python3 tools/verify.py full --scope "' + (scope or 'all') + '"', result.stdout)
                self.assertNotIn('exit 2', result.stdout)

    def test_selected_scope_reaches_pit_and_console(self):
        for scope in sorted(VERIFY.SCOPES):
            with self.subTest(scope=scope):
                self.root = Path(self.temp.name) / scope / 'repo'
                self.root.mkdir(parents=True)
                self.evidence = self.root.parent / 'evidence'
                self.evidence.mkdir()
                self.calls = []
                code, summary = self.execute('full', scope)
                self.assertEqual(0, code)
                self.assertEqual(['make', 'mutation-test', 'SCOPE=' + scope], self.calls[1])
                self.assertIn('Scope: ' + scope, self.output)
                self.assertEqual(scope, summary['scope'])


if __name__ == '__main__':
    unittest.main()
