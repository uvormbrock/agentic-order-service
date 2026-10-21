#!/usr/bin/env python3
"""Local Maven/PIT entry point; no formatting, Git writes, Cloud calls or approval."""
import argparse
from collections import Counter
from contextlib import contextmanager
from datetime import datetime, timezone
import fcntl
import hashlib
import json
import os
import re
from pathlib import Path
import shutil
import subprocess
import tempfile
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SCOPES = {'order', 'product', 'customer', 'shared', 'all'}


@contextmanager
def build_lock():
    key = hashlib.sha256(str(ROOT).encode()).hexdigest()[:20]
    with open(Path(tempfile.gettempdir()) / ('agentic-verify-' + key + '.lock'), 'a') as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError as failure:
            raise RuntimeError('Another verify-fast/full owns this checkout; serialize all builds.') from failure
        yield


def suites(directory):
    paths = sorted(directory.glob('TEST-*.xml'))
    if not paths:
        raise RuntimeError('Missing fresh test XML: ' + str(directory))
    counts = Counter()
    for path in paths:
        suite = ET.parse(path).getroot()
        if suite.tag != 'testsuite':
            raise RuntimeError('Unexpected test XML root: ' + str(path))
        values = {key: int(suite.attrib[key]) for key in ('tests', 'failures', 'errors', 'skipped')}
        if any(value < 0 for value in values.values()) or values['skipped'] > values['tests']:
            raise RuntimeError('Invalid test counts: ' + str(path))
        counts.update(values)
    if counts['tests'] <= counts['skipped'] or counts['failures'] or counts['errors']:
        raise RuntimeError('No successful test population: ' + str(directory))
    return dict(counts)


def invoke(command, log, env):
    print('Running: ' + ' '.join(command), flush=True)
    with open(log, 'w') as stream:
        return subprocess.run(command, cwd=ROOT, env=env, stdout=stream, stderr=subprocess.STDOUT,
                              check=False).returncode


def console_summary(summary, evidence, duration, reports_after_ns):
    """Read-only presentation; never change saved evidence, gate policy or exit codes."""
    def test_group(name):
        archived = evidence / name
        directory = archived if archived.is_dir() else ROOT / 'target' / name
        paths = [p for p in directory.glob('TEST-*.xml')
                 if p.stat().st_mtime_ns >= reports_after_ns] if 'mavenExit' in summary else []
        totals = Counter()
        architecture = Counter()
        failures = []
        try:
            for path in sorted(paths):
                suite = ET.parse(path).getroot()
                values = {key: int(suite.attrib[key]) for key in ('tests', 'failures', 'errors', 'skipped')}
                if suite.tag != 'testsuite' or any(v < 0 for v in values.values()) or sum(
                        values[key] for key in ('failures', 'errors', 'skipped')) > values['tests']:
                    raise ValueError('Invalid test counters')
                totals.update(values)
                if suite.get('name', '').rsplit('.', 1)[-1] == 'ArchitectureComplianceTest':
                    architecture.update(values)
                for case in suite.findall('testcase'):
                    for node in list(case.findall('failure')) + list(case.findall('error')):
                        message = node.get('message', '').splitlines()
                        detail = message[0] if message and message[0].strip() else node.get('type', node.tag)
                        detail = re.sub(r'[\x00-\x1f\x7f]', ' ', detail)[:160]
                        failures.append(case.get('classname', suite.get('name', '?')) + '#'
                                        + case.get('name', '?') + ': ' + detail)
        except (OSError, ET.ParseError, KeyError, ValueError):
            return None, None, ['Invalid test report; inspect ' + str(directory)], directory
        return (dict(totals) if paths else None, dict(architecture) if architecture else None,
                failures, directory)

    def passed(counts):
        return bool(counts and counts['tests'] > 0
                    and not any(counts[key] for key in ('failures', 'errors', 'skipped')))

    def counts_line(label, counts):
        if counts is None:
            return label + ': NOT VERIFIED (no fresh usable reports)'
        executed = counts['tests'] - counts['skipped']
        successful = executed - counts['failures'] - counts['errors']
        state = 'PASS' if passed(counts) else 'FAIL'
        if state == 'PASS' and summary.get('mavenExit') != 0:
            state = 'NOT VERIFIED'
        return (f"{label}: {state} | "
                f"{executed} executed, {successful} passed, {counts['failures']} failed, "
                f"{counts['errors']} errors, {counts['skipped']} skipped")

    fast, architecture, failures, fast_dir = test_group('surefire-reports')
    integration = None
    integration_dir = None
    if summary['mode'] == 'full':
        integration, _, extra, integration_dir = test_group('failsafe-reports')
        failures += extra
    log = evidence / 'maven.log'
    text = log.read_text(errors='replace') if log.is_file() else ''
    check = re.search(r'--- spotless:[^\s]+:check[^\n]*', text)
    spotless = 'NOT VERIFIED'
    if re.search(r'\[ERROR\] Failed to execute goal[^\n]*spotless', text, re.IGNORECASE):
        spotless = 'FAIL'
    elif check:
        remaining = text[check.end():]
        next_goal = re.search(r'\[INFO\] --- ', remaining)
        block = remaining[:next_goal.start()] if next_goal else remaining
        if '[ERROR]' in block:
            spotless = 'FAIL'
        elif next_goal or '[INFO] BUILD SUCCESS' in block:
            spotless = 'PASS'
    full = summary['mode'] == 'full'
    outcomes = summary.get('mutationOutcomes', {})
    okay = (summary['exitCode'] == 0 and summary.get('mavenExit') == 0 and passed(fast)
            and passed(architecture) and spotless == 'PASS')
    if full:
        okay = bool(okay and passed(integration) and summary.get('jacoco')
                    and summary.get('pitExit') == 0 and sum(outcomes.values()) > 0)
    print(f"\nVerification {'PASS' if okay else 'FAIL'} ({summary['mode']}, execution checks) | {duration:.1f}s")
    if full:
        print('Scope: ' + str(summary['scope']))
    print(counts_line('Fast tests', fast))
    print(counts_line('Architecture', architecture))
    print('Spotless: ' + spotless)
    if full:
        print(counts_line('PostgreSQL integration', integration))
        coverage = []
        for kind in ('LINE', 'BRANCH', 'INSTRUCTION'):
            if kind in summary.get('jacoco', {}):
                item = summary['jacoco'][kind]
                total = item['covered'] + item['missed']
                value = f"{100 * item['covered'] / total:.2f}%" if total else 'n/a'
                coverage.append(f"{kind.lower()} {value} ({item['covered']}/{total})")
        print('JaCoCo: ' + (', '.join(coverage) if coverage else 'NOT VERIFIED'))
        if outcomes:
            print('PIT: ' + ', '.join(f'{count} {status.lower()}' for status, count in sorted(outcomes.items())))
        else:
            print('PIT: ' + summary.get('pit', 'Pending'))
        print('Mutation-quality disposition: PENDING (execution success is not acceptance)')
    if summary.get('verificationError'):
        print('Check error: ' + summary['verificationError'])
    for line in failures[:5]:
        print('Failed test: ' + line)
    if len(failures) > 5:
        print(f'... {len(failures) - 5} further test failures; see reports')
    if summary.get('mavenExit', 0):
        print('Maven: FAIL (exit ' + str(summary['mavenExit']) + ')')
    if summary.get('pitExit', 0):
        print('PIT command: FAIL (exit ' + str(summary['pitExit']) + ')')
    if not okay:
        # Bounded Maven/Make diagnostics, no full stack trace or log dump.
        error_lines = []
        for name in ('maven.log', 'mutation.log'):
            path = evidence / name
            if path.is_file():
                for line in path.read_text(errors='replace').splitlines():
                    if line.startswith('[ERROR] ') or line.startswith('make: ***'):
                        error_lines.append(re.sub(r'[\x00-\x1f\x7f]', ' ', line)[:180])
        for line in error_lines[:3]:
            print('Error: ' + line)
    if not okay and summary['exitCode'] == 0:
        print('Process exit 0 retained; console checks above are incomplete or need skip disposition.')
    print('Evidence: ' + str(evidence))
    reports = [str(evidence / 'summary.json')]
    for path in (fast_dir, integration_dir, evidence / 'jacoco/index.html',
                 Path(summary['pitReport']) if summary.get('pitReport') else None,
                 evidence / 'maven.log', evidence / 'mutation.log'):
        if path is not None and path.exists():
            reports.append(str(path))
    print('Reports: ' + '\n         '.join(reports), flush=True)


def execute(mode, scope, evidence):
    started = time.monotonic()
    reports_after_ns = time.time_ns()
    summary = {'mode': mode, 'scope': scope, 'startedUtc': datetime.now(timezone.utc).isoformat(),
               'maven': 'Pending', 'pit': 'Pending' if mode == 'full' else 'Not Applicable',
               'independentReview': 'Pending', 'humanAcceptance': 'Pending',
               'sonarCloud': 'Human-controlled; not invoked'}
    def save(code):
        summary['exitCode'] = code
        (evidence / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
        try:
            console_summary(summary, evidence, time.monotonic() - started, reports_after_ns)
        except (OSError, ValueError, TypeError, KeyError, ET.ParseError):
            # Presentation must not rewrite machine reports or replace the original exit.
            print('Verification FAIL: console evidence unavailable; inspect ' + str(evidence), flush=True)
        return code
    try:
        env = os.environ.copy()
        java = env.get('JAVA_HOME')
        if not java:
            raise RuntimeError('Set JAVA_HOME to Java 21.')
        checked = subprocess.run([str(Path(java) / 'bin/java'), '-XshowSettings:properties', '-version'],
                                 text=True, capture_output=True, check=True)
        settings = checked.stdout + checked.stderr
        if not any(line.strip() == 'java.specification.version = 21' for line in settings.splitlines()):
            raise RuntimeError('JAVA_HOME must select Java 21.')
        inputs = [ROOT / 'pom.xml', ROOT / 'Makefile', ROOT / 'tools/verify.py']
        inputs += [p for p in (ROOT / 'src').rglob('*') if p.is_file()]
        (evidence / 'source-sha256.json').write_text(json.dumps(
            {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs if p.is_file()}, indent=2) + '\n')
        for command, name in [(['git', 'rev-parse', 'HEAD'], 'revision.txt'),
                              (['git', 'status', '--short'], 'status.txt')]:
            (evidence / name).write_text(subprocess.check_output(command, cwd=ROOT, text=True))
        phase = 'verify' if mode == 'full' else 'test'
        command = ['./mvnw', '-B', '-ntp', '-DfailIfNoTests=true', 'clean', phase]
        code = invoke(command, evidence / 'maven.log', env)
        summary['mavenCommand'] = command
        summary['mavenExit'] = code
        if code:
            summary['maven'] = 'Failed; inspect maven.log'
            summary['pit'] = 'Pending — blocked by Maven failure' if mode == 'full' else 'Not Applicable'
            return save(code)
        summary['surefire'] = suites(ROOT / 'target/surefire-reports')
        shutil.copytree(ROOT / 'target/surefire-reports', evidence / 'surefire-reports')
        if mode == 'full':
            summary['failsafe'] = suites(ROOT / 'target/failsafe-reports')
            shutil.copytree(ROOT / 'target/failsafe-reports', evidence / 'failsafe-reports')
            coverage = ROOT / 'target/site/jacoco'
            report = ET.parse(coverage / 'jacoco.xml').getroot()
            summary['jacoco'] = {node.attrib['type']: {'missed': int(node.attrib['missed']), 'covered': int(node.attrib['covered'])}
                                 for node in report.findall('counter')}
            if not summary['jacoco']:
                raise RuntimeError('Missing JaCoCo counters; no coverage success claim.')
            shutil.copytree(coverage, evidence / 'jacoco')
        summary['maven'] = 'Passed execution; review skips and assertions'
        if mode == 'full':
            # Reuse the existing PIT baseline/report collector, including its safety checks.
            env['MUTATION_EVIDENCE_ROOT'] = str(evidence / 'mutation')
            command = ['make', 'mutation-test', 'SCOPE=' + scope]
            code = invoke(command, evidence / 'mutation.log', env)
            summary['pitCommand'] = command
            summary['pitExit'] = code
            if code:
                summary['pit'] = 'Failed execution; quality assessment Pending — inspect mutation.log'
                return save(code)
            reports = list((evidence / 'mutation').glob('PIT-*/pit-reports/mutations.xml'))
            if len(reports) != 1:
                raise RuntimeError('Expected one fresh mutation report.')
            mutations = ET.parse(reports[0]).getroot().findall('mutation')
            if not mutations:
                raise RuntimeError('Empty mutation population.')
            summary['mutationOutcomes'] = dict(Counter(node.attrib['status'] for node in mutations))
            summary['pitReport'] = str(reports[0])
            summary['pit'] = 'Executed — outcome/survivor review required; no score threshold configured'
        return save(0)
    except (OSError, ValueError, KeyError, RuntimeError, ET.ParseError, subprocess.CalledProcessError) as failure:
        summary['verificationError'] = str(failure)
        # Incomplete evidence is not a passed pipeline, even after a zero tool exit.
        return save(3)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode', choices=['fast', 'full'])
    parser.add_argument('--scope', choices=sorted(SCOPES))
    args = parser.parse_args()
    if args.mode == 'full' and not args.scope:
        args.scope = 'all'
    base = Path(os.environ.get('VERIFICATION_EVIDENCE_ROOT', tempfile.gettempdir())).resolve()
    if base == ROOT or ROOT in base.parents:
        parser.error('Verification evidence must be outside the checkout.')
    base.mkdir(parents=True, exist_ok=True)
    try:
        with build_lock():
            evidence = Path(tempfile.mkdtemp(prefix='agentic-verify-' + args.mode + '-', dir=base))
            return execute(args.mode, args.scope, evidence)
    except (OSError, RuntimeError) as failure:
        parser.exit(2, str(failure) + '\n')


if __name__ == '__main__':
    raise SystemExit(main())
