"""Run Make recipes only against local executable stubs; never invoke Maven or APIs."""

import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

MAKEFILE = Path(__file__).resolve().parents[1] / 'Makefile'


class MakefileTest(unittest.TestCase):
    def test_mocked_targets(self):
        for target, maven_rc, findings_rc, expected in [
                ('sonar-findings', 0, 0, 0), ('sonar-findings', 0, 3, 2),
                ('sonar-local', 0, 0, 0), ('sonar-local', 0, 3, 2),
                ('sonar-local', 1, 0, 2)]:
            with self.subTest(target=target, maven_rc=maven_rc, findings_rc=findings_rc):
                with tempfile.TemporaryDirectory() as directory:
                    root = Path(directory)
                    shutil.copy(MAKEFILE, root / 'Makefile')
                    scripts = {
                        'git': '#!/bin/sh\nprintf "develop\\n"\n',
                        'python3': '#!/bin/sh\nprintf "%s\\n" "$*" >> "$CALLS"\n'
                                   'if [ "$2" = prepare ]; then printf "/tmp/mock-sonar-run\\n"; exit 0; fi\n'
                                   'exit "$FINDINGS_RC"\n',
                        'mvnw': '#!/bin/sh\nprintf "maven %s\\n" "$*" >> "$CALLS"\nexit "$MAVEN_RC"\n',
                    }
                    for name, content in scripts.items():
                        executable = root / name
                        executable.write_text(content)
                        executable.chmod(0o700)
                    log = root / 'calls'
                    env = dict(os.environ, PATH=str(root) + os.pathsep + os.environ['PATH'],
                               SONAR_TOKEN='offline-placeholder', CALLS=str(log),
                               MAVEN_RC=str(maven_rc), FINDINGS_RC=str(findings_rc))
                    for key in ('GITHUB_EVENT_NAME', 'CHANGE_ID', 'CI_MERGE_REQUEST_IID', 'SONAR_PULL_REQUEST_KEY'):
                        env.pop(key, None)
                    result = subprocess.run(['make', '--no-print-directory', target], cwd=root,
                                            env=env, capture_output=True, text=True)
                    self.assertEqual(result.returncode, expected, result.stderr)
                    calls = log.read_text().splitlines()
                    if target == 'sonar-findings':
                        self.assertEqual(calls, ['tools/sonar_findings.py findings --branch develop'])
                    else:
                        self.assertEqual(sum(line.startswith('maven ') for line in calls), 1)
                        self.assertIn('clean verify', calls[1])
                        self.assertIn('--maven-exit ' + str(maven_rc), calls[2])
                        self.assertEqual(len(calls), 3)


if __name__ == '__main__':
    unittest.main()
