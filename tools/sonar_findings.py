"""Read-only SonarQube Cloud findings; standard library only, private external evidence."""

import argparse
from collections import Counter
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
from urllib.error import HTTPError, URLError
from urllib.parse import parse_qs, urlencode, urlparse
from urllib.request import HTTPRedirectHandler, Request, build_opener

PROJECT = 'uvormbrock_agentic-order-service'
SERVER = 'https://sonarcloud.io'
PAGE_SIZE = 100


class Incomplete(Exception):
    """Safe reason only; never include raw server responses or exceptions."""


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class Api:
    def __init__(self, token):
        self.stage = 'request'
        self.token = token
        self.opener = build_opener(NoRedirect())

    def get(self, endpoint, **params):
        request = Request(SERVER + '/api/' + endpoint + '?' + urlencode(params),
                          headers={'Authorization': 'Bearer ' + self.token,
                                   'Accept': 'application/json'})
        try:
            with self.opener.open(request, timeout=30) as response:
                raw = response.read(5_000_001)
            if len(raw) > 5_000_000:
                raise Incomplete('Response size limit exceeded')
            data = json.loads(raw)
            if not isinstance(data, dict) or 'errors' in data:
                raise Incomplete('Invalid API response')
            return data
        except HTTPError as error:
            # Fixed categories avoid persisting untrusted bodies, queries or credentials.
            category = {400: 'invalid-request', 401: 'authentication', 403: 'permission',
                        404: 'not-found', 429: 'rate-limit'}.get(error.code, 'service-or-http-error')
            safe_endpoint = endpoint if endpoint in {
                'ce/task', 'qualitygates/project_status', 'issues/search',
                'hotspots/search', 'project_branches/list'} else 'unknown-endpoint'
            raise Incomplete(f'{self.stage}: /api/{safe_endpoint}; HTTP {error.code}; {category}') from None
        except (URLError, TimeoutError, OSError, ValueError):
            raise Incomplete('API network/JSON failure') from None


def paginated(api, endpoint, collection, **params):
    found = []
    keys = set()
    total = None
    page = 1
    while True:
        data = api.get(endpoint, p=page, ps=PAGE_SIZE, **params)
        paging = data.get('paging', {})
        if not isinstance(paging, dict):
            raise Incomplete('Invalid paging metadata')
        current_total = paging.get('total')
        items = data.get(collection)
        if (type(current_total) is not int or current_total < 0 or current_total >= 10_000
                or paging.get('pageIndex') != page or paging.get('pageSize') != PAGE_SIZE
                or not isinstance(items, list)):
            raise Incomplete('Invalid pagination or search ceiling exceeded')
        if total is not None and total != current_total:
            raise Incomplete('Findings changed during pagination')
        total = current_total
        expected = min(PAGE_SIZE, total - len(found))
        if len(items) != expected:
            raise Incomplete('Incomplete search page')
        for item in items:
            if not isinstance(item, dict) or not isinstance(item.get('key'), str) or not item['key']:
                raise Incomplete('Finding identifier missing')
            if item['key'] in keys:
                raise Incomplete('Duplicate finding across search pages')
            keys.add(item['key'])
            found.append(item)
        if len(found) == total:
            return found
        page += 1


def git(*args):
    env = {k: v for k, v in os.environ.items() if k != 'SONAR_TOKEN'}
    result = subprocess.run(['git', *args], env=env, capture_output=True, check=False)
    if result.returncode:
        raise Incomplete('Git source-state verification failed')
    return result.stdout


def source_state():
    files = git('ls-files', '-co', '--exclude-standard', '-z').split(b'\0')
    identities = {}
    for name in sorted(set(files) - {b''}):
        path = Path(os.fsdecode(name))
        if path.is_symlink():
            content = os.readlink(path).encode()
        elif path.is_file():
            content = path.read_bytes()
        else:
            content = b'<deleted>'
        identities[os.fsdecode(name)] = hashlib.sha256(content).hexdigest()
    payload = {'revision': git('rev-parse', 'HEAD').decode().strip(),
               'branch': git('symbolic-ref', '--short', 'HEAD').decode().strip(),
               'index': hashlib.sha256(git('diff', '--cached', '--binary')).hexdigest(),
               'files': identities}
    return hashlib.sha256(json.dumps(payload, sort_keys=True).encode()).hexdigest(), payload


def changed_lines(base):
    # Per-file patches avoid interpreting quoted file headers as repository paths.
    names = git('diff', '--name-only', '-z', base, '--').split(b'\0')
    changes = {}
    for name in names:
        if not name:
            continue
        filename = os.fsdecode(name)
        patch = git('diff', '--no-ext-diff', '--unified=0', base, '--', filename).decode(errors='replace')
        changes[filename] = [[int(start), int(start) + int(count or '1') - 1]
                             for start, count in re.findall(r'^@@ .* \+(\d+)(?:,(\d+))? @@', patch, re.M)]
    for name in git('ls-files', '--others', '--exclude-standard', '-z').split(b'\0'):
        if name:
            changes[os.fsdecode(name)] = [[1, 2**63 - 1]]
    return changes


def write_private(path, value):
    # The run directory is mkdtemp-created, outside the checkout, mode 0700.
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, 'w') as stream:
        os.fchmod(stream.fileno(), 0o600)
        json.dump(value, stream, indent=2)
        stream.write('\n')


def prepare(branch):
    os.umask(0o077)
    identity, state = source_state()
    if state['branch'] != branch:
        raise Incomplete('Branch changed before verification')
    base = os.environ.get('SONAR_BASE_REF', 'HEAD')
    base = git('rev-parse', '--verify', '--end-of-options', base + '^{commit}').decode().strip()
    run = Path(tempfile.mkdtemp(prefix='sonar-local-'))
    # A caller-supplied TMPDIR must not cause report persistence inside the repository.
    repository = Path(git('rev-parse', '--show-toplevel').decode().strip()).resolve()
    if run.resolve().is_relative_to(repository):
        run.rmdir()
        raise Incomplete('Evidence directory must be outside the checkout')
    write_private(run / 'source.json', {'identity': identity, 'state': state,
                                      'base': base, 'changes': changed_lines(base),
                                      'previous_task': hashlib.sha256(Path('target/sonar/report-task.txt').read_bytes()).hexdigest()
                                      if Path('target/sonar/report-task.txt').is_file() else None})
    print(run)
    return run


def scope_candidate(item, source, new_keys, baseline):
    path = item['component'].removeprefix(PROJECT + ':')
    line = item.get('line', item.get('textRange', {}).get('startLine'))
    ranges = source['changes'].get(path)
    overlap = ranges is not None and type(line) is int and any(a <= line <= b for a, b in ranges)
    return {'sonar_new_code': item['key'] in new_keys if new_keys is not None else None,
            'repository_change': 'changed-line' if overlap else 'changed-file' if ranges is not None else 'unchanged-file',
            'present_in_baseline': item['key'] in baseline if baseline is not None else None,
            'change_request_ownership': 'Pending independent review'}


def normalize(item, source, new_keys, baseline, hotspot=False):
    if item.get('project') != PROJECT:
        raise Incomplete('Finding belongs to a different or missing project')
    component = item.get('component')
    if not isinstance(component, str) or not (component == PROJECT or component.startswith(PROJECT + ':')):
        raise Incomplete('Finding component identity invalid')
    if not isinstance(item.get('textRange', {}), dict):
        raise Incomplete('Invalid finding text range')
    for key in ('type', 'severity', 'message', 'status', 'issueStatus', 'ruleKey', 'resolution'):
        if item.get(key) is not None and not isinstance(item[key], str):
            raise Incomplete('Invalid finding field')
    rule = item.get('rule', item.get('ruleKey'))
    if isinstance(rule, dict):
        rule = rule.get('key')
    if rule is not None and not isinstance(rule, str):
        raise Incomplete('Invalid rule field')
    status = item.get('issueStatus', item.get('status'))
    if (hotspot and status != 'TO_REVIEW') or (not hotspot and status not in {'OPEN', 'CONFIRMED', 'REOPENED'}):
        raise Incomplete('Search returned an unexpected finding status')
    line = item.get('line', item.get('textRange', {}).get('startLine'))
    if line is not None and (type(line) is not int or line < 1):
        raise Incomplete('Invalid finding line')
    # Preserve both legacy issue type/severity and modern software-quality impacts.
    impacts = item.get('impacts', [])
    if not isinstance(impacts, list) or any(not isinstance(v, dict) for v in impacts):
        raise Incomplete('Invalid software quality impacts')
    return {'id': item['key'], 'type': 'SECURITY_HOTSPOT' if hotspot else item.get('type'),
            'severity': item.get('severity'),
            'impacts': [{'quality': v.get('softwareQuality'), 'severity': v.get('severity')} for v in impacts],
            'review_priority': item.get('vulnerabilityProbability') if hotspot else None,
            'rule': rule,
            'file': component.removeprefix(PROJECT + ':'),
            'line': item.get('line', item.get('textRange', {}).get('startLine')),
            'message': item.get('message'), 'status': item.get('issueStatus', item.get('status')),
            'resolution': item.get('resolution'),
            'classification': scope_candidate(item, source, new_keys, baseline)}


def baseline_keys(path, branch, analysis):
    if not path:
        return None
    try:
        report = json.loads(Path(path).read_text())
        if (report['project'] != PROJECT or report['branch'] != branch
                or report['issues']['status'] != 'Passed' or not report.get('analysis_id')
                or report['analysis_id'] == analysis):
            raise ValueError()
        return {i['id'] for i in report['issues']['findings']}
    except (OSError, ValueError, KeyError, TypeError):
        raise Incomplete('Baseline report is unavailable, incomplete or for another project/branch') from None


def collect(api, run, branch, maven_exit, baseline=None, standalone=False):
    source = json.loads((run / 'source.json').read_text())
    report = {'project': PROJECT, 'branch': branch, 'date': datetime.now(timezone.utc).isoformat(),
              'source_identity': source['identity'], 'base': source['base'],
              'baseline_report': str(baseline) if baseline else None,
              'operation': 'findings-only' if standalone else 'analysis-and-findings',
              'maven_exit': maven_exit,
              'maven_invocation': 'Passed' if maven_exit == 0 else 'Failed', 'maven_verification': 'Pending',
              'scanner_processing': 'Pending', 'quality_gate': {'status': 'Pending'},
              'issues': {'status': 'Pending', 'counts': None},
              'hotspots': {'status': 'Pending', 'counts': None},
              'issue_review': 'Pending',
              'issueSnapshotProvenance': 'Pending', 'supersessionVerification': 'Pending',
              'issue_scope': 'current-branch',
              'limitations': ['Issue search is current branch state, not an immutable analysis snapshot; '
                              'supersession is unverified.']}
    try:
        identity, state = source_state()
        if identity != source['identity'] or state['branch'] != branch:
            raise Incomplete('Working tree changed during verification/analysis')
        analysis = None
        if standalone:
            report['maven_exit'] = None
            report['maven_invocation'] = 'Not Applicable'
            report['maven_verification'] = 'Not Applicable'
            report['scanner_processing'] = 'Not Applicable'
            report['quality_gate']['reason'] = 'Not checked by standalone findings retrieval'
        else:
            task_file = Path('target/sonar/report-task.txt').read_bytes()
            if hashlib.sha256(task_file).hexdigest() == source.get('previous_task'):
                raise Incomplete('Scanner task report is stale from before this run')
            values = dict(line.split('=', 1) for line in task_file.decode().splitlines() if '=' in line)
            dashboard = urlparse(values.get('dashboardUrl', ''))
            query = parse_qs(dashboard.query)
            if (values.get('serverUrl', '').rstrip('/') != SERVER or values.get('projectKey') != PROJECT
                    or dashboard.scheme != 'https' or dashboard.netloc != 'sonarcloud.io'
                    or query.get('id') != [PROJECT] or query.get('branch') != [branch]
                    or not values.get('ceTaskId')):
                raise Incomplete('Scanner task project/branch/server provenance unavailable')
            report['dashboard_url'] = SERVER + '/dashboard?' + urlencode({'id': PROJECT, 'branch': branch})
            api.stage = 'scanner-task'
            task = api.get('ce/task', id=values['ceTaskId']).get('task', {})
            if (task.get('id') != values['ceTaskId'] or task.get('componentKey') != PROJECT
                    or task.get('branch') != branch or task.get('branchType') not in {'SHORT', 'LONG'}
                    or task.get('type') != 'REPORT' or task.get('status') != 'SUCCESS'
                    or not isinstance(task.get('analysisId'), str) or not task['analysisId'].strip()):
                raise Incomplete('Scanner task not successfully processed for intended project')
            report['scanner_processing'] = 'Passed'
            # Compute-engine processing implies the preceding verify phase completed in this target.
            report['maven_verification'] = 'Passed'
            analysis = task['analysisId']
            report['analysis_id'] = analysis
            try:
                api.stage = 'quality-gate'
                status = api.get('qualitygates/project_status', analysisId=analysis).get('projectStatus', {})
                gate = status.get('status')
                if gate not in {'OK', 'ERROR', 'WARN', 'NONE'}:
                    raise Incomplete('Quality Gate response incomplete')
                report['quality_gate'] = {'status': 'Passed' if gate == 'OK' else 'Failed' if gate == 'ERROR' else 'Pending',
                                          'remote_status': gate,
                                          'conditions': [{k: c.get(k) for k in ('metricKey', 'status', 'actualValue', 'errorThreshold')}
                                                         for c in status.get('conditions', [])]}
            except (Incomplete, ValueError, KeyError, TypeError, AttributeError) as error:
                report['quality_gate']['reason'] = str(error) if isinstance(error, Incomplete) else 'Malformed Quality Gate response'
        try:
            api.stage = 'issues'
            raw = paginated(api, 'issues/search', 'issues', componentKeys=PROJECT, branch=branch,
                            issueStatuses='OPEN,CONFIRMED')
            new = paginated(api, 'issues/search', 'issues', componentKeys=PROJECT, branch=branch,
                            issueStatuses='OPEN,CONFIRMED', sinceLeakPeriod='true')
            new_keys = {i['key'] for i in new}
            if not new_keys.issubset({i['key'] for i in raw}):
                raise Incomplete('New-code issue population changed during retrieval')
            known = baseline_keys(baseline, branch, analysis)
            findings = [normalize(i, source, new_keys, known) for i in raw]
            counts = Counter((i['type'] or 'UNKNOWN', i['severity'] or 'UNKNOWN') for i in findings)
            impacts = Counter((v['quality'] or 'UNKNOWN', v['severity'] or 'UNKNOWN')
                              for i in findings for v in i['impacts'])
            report['issues'] = {'status': 'Passed', 'total': len(findings),
                                'counts': [{'type': t, 'severity': s, 'count': n} for (t, s), n in sorted(counts.items())],
                                'impact_counts': [{'quality': q, 'severity': s, 'count': n}
                                                  for (q, s), n in sorted(impacts.items())],
                                'findings': findings}
        except (Incomplete, ValueError, KeyError, TypeError, AttributeError) as error:
            report['issues']['reason'] = str(error) if isinstance(error, Incomplete) else 'Malformed issue response'
        try:
            api.stage = 'hotspots'
            # Kept separate from issues: this legacy endpoint is deprecated, not silently absent.
            raw = paginated(api, 'hotspots/search', 'hotspots', projectKey=PROJECT, branch=branch, status='TO_REVIEW')
            findings = [normalize(i, source, None, None, hotspot=True) for i in raw]
            branches = api.get('project_branches/list', project=PROJECT).get('branches')
            if not isinstance(branches, list):
                raise Incomplete('Hotspot branch provenance unavailable')
            is_main = any(b.get('name') == branch and b.get('isMain') is True for b in branches)
            report['hotspots']['findings'] = findings
            # Cloud public metadata omits the hotspot branch parameter. Never trust an
            # empty/default-branch result as proof about an explicitly analyzed non-main branch.
            if not is_main:
                raise Incomplete('Deprecated hotspot API does not document branch scoping; non-main results require human verification')
            report['hotspots'] = {'status': 'Passed', 'total': len(findings),
                                  'counts': dict(Counter(i['review_priority'] or 'UNKNOWN' for i in findings)),
                                  'findings': findings}
        except (Incomplete, ValueError, KeyError, TypeError, AttributeError) as error:
            report['hotspots']['reason'] = str(error) if isinstance(error, Incomplete) else 'Malformed hotspot response'
        if source_state()[0] != source['identity']:
            raise Incomplete('Source/remote analysis changed during retrieval')
    except (Incomplete, OSError, ValueError, KeyError, TypeError, AttributeError) as error:
        reason = str(error) if isinstance(error, Incomplete) else 'Missing or malformed local/API provenance data'
        report['limitations'].append(reason)
        for key in ('quality_gate', 'issues', 'hotspots'):
            report[key] = {'status': 'Pending', 'counts': None, 'reason': reason}
    return report


def redact(value, token):
    if isinstance(value, str):
        return value.replace(token, '[REDACTED]')
    if isinstance(value, list):
        return [redact(v, token) for v in value]
    if isinstance(value, dict):
        return {redact(k, token): redact(v, token) for k, v in value.items()}
    return value


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['prepare', 'collect', 'findings'])
    parser.add_argument('--branch', required=True, choices=['main', 'develop'])
    parser.add_argument('--run', type=Path)
    parser.add_argument('--maven-exit', type=int, default=0)
    args = parser.parse_args()
    try:
        if args.action == 'prepare':
            prepare(args.branch)
            return 0
        token = os.environ.pop('SONAR_TOKEN', '')
        standalone = args.action == 'findings'
        if standalone:
            args.run = prepare(args.branch)
        if (not token or args.run is None
                or args.run.resolve().is_relative_to(Path.cwd().resolve())):
            raise Incomplete('Credential or run evidence unavailable')
        report = collect(Api(token), args.run, args.branch, args.maven_exit,
                         os.environ.get('SONAR_BASELINE_REPORT'), standalone=standalone)
        # Defense in depth: unexpected server echoes must not persist the credential.
        write_private(args.run / 'findings.json', redact(report, token))
        print('SonarQube findings evidence: ' + str(args.run / 'findings.json'))
        print('Quality Gate: ' + report['quality_gate']['status'] + '; issue retrieval: '
              + report['issues']['status'] + '; hotspot retrieval: ' + report['hotspots']['status']
              + '; independent issue review: Pending')
        if report['quality_gate']['status'] == 'Failed':
            return 1
        required = ('issues',) if standalone else ('quality_gate', 'issues', 'hotspots')
        return 0 if all(report[k]['status'] == 'Passed' for k in required) else 3
    except (Incomplete, OSError, ValueError, KeyError, TypeError, AttributeError):
        print('SonarQube findings retrieval Pending: credential, provenance or report data unavailable.')
        return 3


if __name__ == '__main__':
    raise SystemExit(main())
