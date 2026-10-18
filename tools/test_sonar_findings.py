"""Credential-free tests for the local SonarQube findings workflow."""

import contextlib
import hashlib
import io
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from urllib.error import HTTPError

import sonar_findings as sonar


def issue(key='i1', **extra):
    return {'key': key, 'project': sonar.PROJECT,
            'component': sonar.PROJECT + ':src/Example.java', 'line': 10,
            'type': 'BUG', 'severity': 'MAJOR', 'rule': 'java:S1',
            'message': 'Example finding', 'issueStatus': 'OPEN',
            'impacts': [{'softwareQuality': 'RELIABILITY', 'severity': 'HIGH'}], **extra}


def page(items, p=1, total=None):
    return {'paging': {'pageIndex': p, 'pageSize': 100,
                       'total': len(items) if total is None else total}, 'issues': items}


class FixtureApi:
    def __init__(self):
        self.calls = []
        self.overrides = {}
        self.branch = 'develop'
        self.main = False
        self.gate = 'OK'
        self.fail = None
        self.latest_count = 0
        self.superseded = False

    def get(self, endpoint, **params):
        self.calls.append((endpoint, params))
        if endpoint == self.fail:
            raise sonar.Incomplete('API HTTP 403; access check required')
        if endpoint in self.overrides:
            return self.overrides[endpoint](params)
        if endpoint == 'ce/task':
            return {'task': {'id': 't1', 'type': 'REPORT', 'branch': self.branch, 'branchType': 'SHORT',
                             'componentKey': sonar.PROJECT, 'status': 'SUCCESS', 'analysisId': 'a1'}}
        if endpoint == 'project_analyses/search':
            self.latest_count += 1
            return {'analyses': [{'key': 'a2' if self.superseded and self.latest_count > 1 else 'a1'}]}
        if endpoint == 'qualitygates/project_status':
            return {'projectStatus': {'status': self.gate, 'conditions': []}}
        if endpoint == 'issues/search':
            assert params['branch'] == self.branch
            assert params['componentKeys'] == sonar.PROJECT
            assert params['issueStatuses'] == 'OPEN,CONFIRMED'
            return page([issue()])
        if endpoint == 'hotspots/search':
            assert params['branch'] == self.branch
            assert params['projectKey'] == sonar.PROJECT
            assert params['status'] == 'TO_REVIEW'
            return {'paging': {'pageIndex': 1, 'pageSize': 100, 'total': 1},
                    'hotspots': [issue('h1', status='TO_REVIEW', issueStatus='TO_REVIEW',
                                       vulnerabilityProbability='HIGH', rule=None, ruleKey='java:S2')]}
        if endpoint == 'project_branches/list':
            return {'branches': [{'name': self.branch, 'isMain': self.main}]}
        raise AssertionError(endpoint)


class FindingsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / 'checkout'
        self.root.mkdir()
        self.run = Path(self.temp.name) / 'run'
        self.run.mkdir()
        (self.root / 'target/sonar').mkdir(parents=True)
        self.original = Path.cwd()
        os.chdir(self.root)
        self.addCleanup(os.chdir, self.original)
        self.source = {'identity': 'source1', 'base': 'base1',
                       'changes': {'src/Example.java': [[10, 12]]}, 'previous_task': None}
        (self.run / 'source.json').write_text(json.dumps(self.source))
        self.taskfile = self.root / 'target/sonar/report-task.txt'
        self.taskfile.write_text('serverUrl=https://sonarcloud.io\nprojectKey=' + sonar.PROJECT
                                 + '\nceTaskId=t1\ndashboardUrl=https://sonarcloud.io/dashboard?id='
                                 + sonar.PROJECT + '&branch=develop\n')
        self.state = patch.object(sonar, 'source_state', return_value=('source1', {'branch': 'develop'}))
        self.state.start()
        self.addCleanup(self.state.stop)

    def collect(self, api=None, rc=0, baseline=None):
        return sonar.collect(api or FixtureApi(), self.run, 'develop', rc, baseline)

    def test_prepare_records_source_baseline_and_previous_task(self):
        def git_result(*args):
            if args[0] == 'rev-parse' and args[1] == '--verify':
                return b'base-commit\n'
            if args == ('rev-parse', '--show-toplevel'):
                return str(self.root).encode() + b'\n'
            raise AssertionError(args)
        previous_umask = os.umask(0o077)
        try:
            with patch.object(sonar, 'git', side_effect=git_result), \
                    patch.object(sonar, 'changed_lines', return_value={'example': [[1, 2]]}), \
                    patch.object(sonar.tempfile, 'mkdtemp', return_value=str(self.run)), \
                    patch.dict(os.environ, {'SONAR_BASE_REF': 'intake'}), \
                    contextlib.redirect_stdout(io.StringIO()):
                sonar.prepare('develop')
        finally:
            os.umask(previous_umask)
        result = json.loads((self.run / 'source.json').read_text())
        self.assertEqual(result['base'], 'base-commit')
        self.assertEqual(result['changes'], {'example': [[1, 2]]})
        self.assertEqual(result['previous_task'], hashlib.sha256(self.taskfile.read_bytes()).hexdigest())
        self.assertEqual((self.run / 'source.json').stat().st_mode & 0o777, 0o600)

    def test_source_identity_changes_with_untracked_content(self):
        sample = self.root / 'untracked.txt'
        sample.write_text('first')
        def git_result(*args):
            if args[0] == 'ls-files':
                return b'untracked.txt\0'
            if args[0] == 'rev-parse':
                return b'revision1\n'
            if args[0] == 'symbolic-ref':
                return b'develop\n'
            if args[0] == 'diff':
                return b'index-patch'
            raise AssertionError(args)
        # Call the original implementation; setUp mocks it for collect fixtures.
        with patch.object(sonar, 'git', side_effect=git_result):
            self.state.stop()
            try:
                first, _ = sonar.source_state()
                sample.write_text('second')
                second, _ = sonar.source_state()
                self.assertNotEqual(first, second)
            finally:
                self.state.start()

    def test_gate_and_review_are_separate(self):
        report = self.collect()
        self.assertEqual(report['quality_gate']['status'], 'Passed')
        self.assertEqual(report['issues']['total'], 1)
        self.assertEqual(report['issues']['findings'][0]['classification']['repository_change'], 'changed-line')
        self.assertEqual(report['issue_review'], 'Pending')
        self.assertEqual(report['hotspots']['status'], 'Pending')
        self.assertIsNone(report['hotspots']['counts'])

    def test_failed_gate_still_retrieves_issues(self):
        api = FixtureApi(); api.gate = 'ERROR'
        report = self.collect(api, 1)
        self.assertEqual(report['quality_gate']['status'], 'Failed')
        self.assertEqual(report['issues']['status'], 'Passed')
        self.assertEqual(report['maven_verification'], 'Passed')
        self.assertEqual(report['maven_invocation'], 'Failed')

    def test_main_branch_hotspots_verified(self):
        api = FixtureApi(); api.main = True
        report = self.collect(api)
        self.assertEqual(report['hotspots']['status'], 'Passed')
        self.assertEqual(report['hotspots']['counts'], {'HIGH': 1})

    def test_permission_failure_never_zero(self):
        api = FixtureApi(); api.fail = 'issues/search'
        report = self.collect(api)
        self.assertEqual(report['issues']['status'], 'Pending')
        self.assertIsNone(report['issues']['counts'])
        self.assertNotIn('total', report['issues'])
        self.assertEqual(report['quality_gate']['status'], 'Passed')

    def test_hotspot_permission_failure_separate(self):
        api = FixtureApi(); api.fail = 'hotspots/search'
        report = self.collect(api)
        self.assertEqual(report['issues']['status'], 'Passed')
        self.assertEqual(report['hotspots']['status'], 'Pending')

    def test_incomplete_pagination(self):
        api = FixtureApi()
        api.overrides['issues/search'] = lambda p: page([issue()], total=101)
        self.assertEqual(self.collect(api)['issues']['status'], 'Pending')

    def test_two_pages(self):
        api = FixtureApi()
        def pages(p):
            items = [issue(str(i)) for i in range(100)] if p['p'] == 1 else [issue('100')]
            return page(items, p['p'], 101)
        api.overrides['issues/search'] = pages
        self.assertEqual(len(sonar.paginated(api, 'issues/search', 'issues')), 101)

    def test_duplicate_changing_total_and_ceiling(self):
        for mode in ['duplicate', 'total', 'ceiling']:
            with self.subTest(mode=mode):
                api = FixtureApi()
                def pages(p):
                    total = 10001 if mode == 'ceiling' else 101 if p['p'] == 1 or mode != 'total' else 102
                    items = [issue(str(i)) for i in range(100)] if p['p'] == 1 else [issue('0')]
                    return page(items, p['p'], total)
                api.overrides['issues/search'] = pages
                with self.assertRaises(sonar.Incomplete):
                    sonar.paginated(api, 'issues/search', 'issues')

    def test_new_code_not_change_request_ownership(self):
        api = FixtureApi()
        api.overrides['issues/search'] = lambda p: page([] if p.get('sinceLeakPeriod') else [issue()])
        record = self.collect(api)['issues']['findings'][0]['classification']
        self.assertFalse(record['sonar_new_code'])
        self.assertIsNone(record['present_in_baseline'])
        self.assertEqual(record['change_request_ownership'], 'Pending independent review')

    def test_baseline_membership_and_wrong_branch(self):
        api = FixtureApi()
        baseline = self.root / 'baseline.json'
        baseline.write_text(json.dumps({'project': sonar.PROJECT, 'branch': 'develop', 'analysis_id': 'old',
                                        'issues': {'status': 'Passed', 'findings': [{'id': 'i1'}]}}))
        record = self.collect(api, baseline=str(baseline))['issues']['findings'][0]
        self.assertTrue(record['classification']['present_in_baseline'])
        value = json.loads(baseline.read_text()); value['branch'] = 'main'
        baseline.write_text(json.dumps(value))
        self.assertEqual(self.collect(baseline=str(baseline))['issues']['status'], 'Pending')

    def test_cross_project_malformed_and_new_population(self):
        for item in [issue(project='wrong'), issue(component='wrong'), issue(textRange='wrong'),
                     issue(issueStatus='FIXED'), issue(line='10')]:
            api = FixtureApi(); api.overrides['issues/search'] = lambda p: page([item])
            self.assertEqual(self.collect(api)['issues']['status'], 'Pending')
        api = FixtureApi()
        api.overrides['issues/search'] = lambda p: page([issue('new' if p.get('sinceLeakPeriod') else 'old')])
        self.assertEqual(self.collect(api)['issues']['status'], 'Pending')

    def test_stale_task_wrong_branch_and_concurrent_analysis(self):
        source = dict(self.source, previous_task=hashlib.sha256(self.taskfile.read_bytes()).hexdigest())
        (self.run / 'source.json').write_text(json.dumps(source))
        self.assertEqual(self.collect()['quality_gate']['status'], 'Pending')
        (self.run / 'source.json').write_text(json.dumps(self.source))
        api = FixtureApi(); api.superseded = True
        report = self.collect(api)
        self.assertEqual(report['issues']['status'], 'Passed')
        self.assertEqual(report['supersessionVerification'], 'Pending')
        self.assertEqual(report['issueSnapshotProvenance'], 'Pending')
        self.assertFalse(any(e == 'project_analyses/search' for e, _ in api.calls))
        self.taskfile.write_text(self.taskfile.read_text().replace('branch=develop', 'branch=main'))
        self.assertEqual(self.collect()['quality_gate']['status'], 'Pending')

    def test_missing_task_and_working_tree_change(self):
        self.taskfile.unlink()
        self.assertEqual(self.collect()['issues']['status'], 'Pending')
        with patch.object(sonar, 'source_state', return_value=('changed', {'branch': 'develop'})):
            self.assertEqual(self.collect()['quality_gate']['status'], 'Pending')

    def test_http_auth_and_failures_are_safe(self):
        api = sonar.Api('dummy-secret')
        response = io.BytesIO(b'{"value":1}')
        with patch.object(api.opener, 'open', return_value=response) as call:
            self.assertEqual(api.get('issues/search', branch='develop'), {'value': 1})
            request = call.call_args.args[0]
            self.assertEqual(request.get_header('Authorization'), 'Bearer dummy-secret')
            self.assertNotIn('dummy-secret', request.full_url)
        for code in [401, 403, 429, 500, 302]:
            error = HTTPError('https://sonarcloud.io', code, 'dummy-secret', {}, io.BytesIO(b'dummy-secret'))
            with patch.object(api.opener, 'open', side_effect=error):
                with self.assertRaises(sonar.Incomplete) as caught:
                    api.get('issues/search')
                self.assertNotIn('dummy-secret', str(caught.exception))
        for raw in [b'invalid', b'[]', b'{"errors":[]}']:
            with patch.object(api.opener, 'open', return_value=io.BytesIO(raw)):
                with self.assertRaises(sonar.Incomplete):
                    api.get('issues/search')

    def test_task_identity_fails_closed(self):
        for field, value in [('id', 'other'), ('branch', 'main'), ('componentKey', 'wrong'),
                             ('analysisId', ''), ('analysisId', None), ('status', 'FAILED'),
                             ('branchType', None), ('type', 'OTHER')]:
            with self.subTest(field=field, value=value):
                api = FixtureApi()
                task = api.get('ce/task')['task']
                task[field] = value
                api.overrides['ce/task'] = lambda params: {'task': task}
                report = self.collect(api)
                self.assertEqual(report['quality_gate']['status'], 'Pending')
                self.assertEqual(report['issues']['status'], 'Pending')
                self.assertFalse(any(e == 'issues/search' for e, _ in api.calls))

    def test_long_branch_and_analysis_specific_gate(self):
        api = FixtureApi()
        task = api.get('ce/task')['task']; task['branchType'] = 'LONG'
        api.overrides['ce/task'] = lambda params: {'task': task}
        self.assertEqual(self.collect(api)['quality_gate']['status'], 'Passed')
        self.assertIn(('qualitygates/project_status', {'analysisId': 'a1'}), api.calls)

    def test_safe_400_diagnostics(self):
        api = sonar.Api('dummy-secret'); api.stage = 'issues'
        error = HTTPError('https://sonarcloud.io', 400, 'dummy-secret', {},
                          io.BytesIO(b'{"errors":[{"msg":"dummy-secret sensitive content"}]}'))
        with patch.object(api.opener, 'open', side_effect=error):
            with self.assertRaises(sonar.Incomplete) as caught:
                api.get('issues/search', branch='develop')
        self.assertEqual(str(caught.exception), 'issues: /api/issues/search; HTTP 400; invalid-request')

    def test_exit_policy_dimensions(self):
        for main, gate, failure, expected in [(True, 'OK', None, 0),
                (False, 'OK', None, 3), (True, 'ERROR', None, 1),
                (True, 'OK', 'issues/search', 3), (True, 'OK', 'ce/task', 3),
                (True, 'OK', 'qualitygates/project_status', 3)]:
            with self.subTest(main=main, gate=gate, failure=failure):
                api = FixtureApi(); api.main = main; api.gate = gate; api.fail = failure
                args = ['sonar_findings.py', 'collect', '--branch', 'develop', '--run', str(self.run)]
                with patch('sys.argv', args), patch.dict(os.environ, {'SONAR_TOKEN': 'dummy-secret'}), \
                        patch.object(sonar, 'Api', return_value=api), contextlib.redirect_stdout(io.StringIO()):
                    self.assertEqual(sonar.main(), expected)

    def test_standalone_has_no_task_or_analysis_identity(self):
        self.taskfile.unlink()
        api = FixtureApi(); api.main = True
        report = sonar.collect(api, self.run, 'develop', None, standalone=True)
        self.assertEqual(report['issues']['status'], 'Passed')
        self.assertEqual(report['hotspots']['status'], 'Passed')
        self.assertEqual(report['quality_gate']['status'], 'Pending')
        self.assertEqual(report['maven_invocation'], 'Not Applicable')
        self.assertNotIn('analysis_id', report)
        self.assertFalse(any(e in {'ce/task', 'qualitygates/project_status'} for e, _ in api.calls))
        self.assertEqual(report['issueSnapshotProvenance'], 'Pending')

    def test_standalone_cli_exit_policy(self):
        for main, failure, expected in [(True, None, 0), (False, None, 0),
                                        (True, 'issues/search', 3), (True, 'hotspots/search', 0)]:
            with self.subTest(main=main, failure=failure):
                api = FixtureApi(); api.main = main; api.fail = failure
                args = ['sonar_findings.py', 'findings', '--branch', 'develop']
                with patch('sys.argv', args), patch.dict(os.environ, {'SONAR_TOKEN': 'dummy-secret'}), \
                        patch.object(sonar, 'prepare', return_value=self.run), \
                        patch.object(sonar, 'Api', return_value=api), contextlib.redirect_stdout(io.StringIO()):
                    self.assertEqual(sonar.main(), expected)

    def test_standalone_empty_issues_retains_pending_evidence(self):
        api = FixtureApi()
        api.overrides['issues/search'] = lambda p: page([])
        api.overrides['hotspots/search'] = lambda p: {
            'paging': {'pageIndex': 1, 'pageSize': 100, 'total': 0}, 'hotspots': []}
        args = ['sonar_findings.py', 'findings', '--branch', 'develop']
        with patch('sys.argv', args), patch.dict(os.environ, {'SONAR_TOKEN': 'dummy-secret'}), \
                patch.object(sonar, 'prepare', return_value=self.run), \
                patch.object(sonar, 'Api', return_value=api), contextlib.redirect_stdout(io.StringIO()) as out:
            self.assertEqual(sonar.main(), 0)
        report = json.loads((self.run / 'findings.json').read_text())
        self.assertEqual(report['issues']['total'], 0)
        self.assertEqual(report['issues']['findings'], [])
        self.assertIsNone(report['hotspots']['counts'])
        self.assertNotIn('total', report['hotspots'])
        for key in ('hotspots', 'quality_gate'):
            self.assertEqual(report[key]['status'], 'Pending')
        for key in ('issueSnapshotProvenance', 'supersessionVerification', 'issue_review'):
            self.assertEqual(report[key], 'Pending')
        self.assertIn('Quality Gate: Pending; issue retrieval: Passed; hotspot retrieval: Pending', out.getvalue())
        self.assertIn('independent issue review: Pending', out.getvalue())

    def test_standalone_failed_issue_status_is_nonzero(self):
        report = sonar.collect(FixtureApi(), self.run, 'develop', None, standalone=True)
        report['issues']['status'] = 'Failed'
        args = ['sonar_findings.py', 'findings', '--branch', 'develop']
        with patch('sys.argv', args), patch.dict(os.environ, {'SONAR_TOKEN': 'dummy-secret'}), \
                patch.object(sonar, 'prepare', return_value=self.run), \
                patch.object(sonar, 'collect', return_value=report), contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(sonar.main(), 3)

    def test_standalone_preserves_pending_hotspots_and_security_issues(self):
        api = FixtureApi()
        api.overrides['issues/search'] = lambda p: page([issue(type='VULNERABILITY')])
        report = sonar.collect(api, self.run, 'develop', None, standalone=True)
        self.assertEqual(report['issues']['findings'][0]['type'], 'VULNERABILITY')
        self.assertEqual(report['hotspots']['status'], 'Pending')
        self.assertEqual(len(report['hotspots']['findings']), 1)

    def test_redacts_nontrivial_token_before_json_encoding(self):
        token = 'dummy-\"secret'
        self.assertNotIn(token, str(sonar.redact({'message': token, token: [token]}, token)))

    def test_cli_redaction_and_private_evidence(self):
        api = FixtureApi(); api.main = True
        api.overrides['issues/search'] = lambda p: page([issue(message='dummy-secret')])
        args = ['sonar_findings.py', 'collect', '--branch', 'develop', '--run', str(self.run)]
        with patch('sys.argv', args), patch.dict(os.environ, {'SONAR_TOKEN': 'dummy-secret'}), \
                patch.object(sonar, 'Api', return_value=api), contextlib.redirect_stdout(io.StringIO()) as out:
            self.assertEqual(sonar.main(), 0)
        text = (self.run / 'findings.json').read_text()
        self.assertNotIn('dummy-secret', text + out.getvalue())
        self.assertEqual((self.run / 'findings.json').stat().st_mode & 0o777, 0o600)


if __name__ == '__main__':
    unittest.main()
