"""Isolated credential and branch guards; all external commands are stubbed."""
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

class MakeGuardsTest(unittest.TestCase):

    def test_guard_scenarios(self):
        repo = Path(__file__).resolve().parents[1]
        records = []
        with tempfile.TemporaryDirectory(prefix='sonar-make-mock-') as td:
            root = Path(td)
            bin = root / 'bin'
            bin.mkdir()
            (root / 'Makefile').write_text((repo / 'Makefile').read_text())

            def exe(path, body):
                path.write_text('#!' + sys.executable + '\n' + body)
                path.chmod(448)
            exe(bin / 'git', "import os,sys\nassert 'SONAR_TOKEN' not in os.environ\nassert sys.argv[1:]==['symbolic-ref','--quiet','--short','HEAD']\nbranch=os.environ.get('MOCK_BRANCH','develop')\nif branch=='DETACHED':sys.exit(1)\nprint(branch)\n")
            exe(bin / 'uname', "import os\nprint(os.environ.get('MOCK_OS','Linux'))\n")
            exe(bin / 'security', "import os,sys\nfrom pathlib import Path\nassert 'SONAR_TOKEN' not in os.environ\nassert sys.argv[1:]==['find-generic-password','-a','mock-user','-s','agentic-order-service-sonar','-w']\nPath(os.environ['MOCK_DIR'],'security-called').touch()\nmode=os.environ.get('MOCK_KEYCHAIN','denied')\nif mode=='denied':sys.exit(1)\nif mode=='ok':print('dummy-keychain')\n")
            exe(root / 'mvnw', "import os,sys\nfrom pathlib import Path\nassert os.environ['SONAR_TOKEN']==os.environ['MOCK_TOKEN']\nassert sys.argv[1:]==['-B','-ntp','clean','verify','org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar','-Dsonar.projectKey=uvormbrock_agentic-order-service','-Dsonar.branch.name='+os.environ.get('MOCK_BRANCH','develop'),'-Dsonar.qualitygate.wait=true','-Dsonar.qualitygate.timeout=300']\nPath(os.environ['MOCK_DIR'],'maven-called').touch()\nsys.exit(int(os.environ.get('MOCK_MAVEN_EXIT','0')))\n")
            exe(bin / 'python3', "import os,sys\nfrom pathlib import Path\nassert sys.argv[1]=='tools/sonar_findings.py'\naction=sys.argv[2]\nPath(os.environ['MOCK_DIR'],action+'-called').touch()\nassert not any('dummy-' in arg for arg in sys.argv)\nif action=='prepare':\n assert 'SONAR_TOKEN' not in os.environ\n if os.environ.get('MOCK_PREPARE_EXIT'):sys.exit(3)\n print('/tmp/sonar-mocked-run')\nelse:\n assert action in {'collect','findings'}\n assert os.environ['SONAR_TOKEN']==os.environ['MOCK_TOKEN']\n if action=='collect':assert sys.argv[-2:]==['--maven-exit',os.environ.get('MOCK_MAVEN_EXIT','0')]\n else:assert sys.argv[3:]==['--branch',os.environ.get('MOCK_BRANCH','develop')]\n sys.exit(int(os.environ.get('MOCK_COLLECT_EXIT','0')))\n")
            env = {'PATH': str(bin) + ':/usr/bin:/bin', 'USER': 'mock-user', 'HOME': str(root), 'MOCK_DIR': str(root)}
            dry = subprocess.run(['/usr/bin/make', '-n', 'sonar-local'], cwd=root, env=env, capture_output=True, text=True, check=True)
            subprocess.run(['/bin/sh', '-n'], input=dry.stdout, text=True, check=True)

            def case(name, extra, exit, expected=(), direct=False):
                for f in root.glob('*-called'):
                    f.unlink()
                cmd = ['/bin/sh', '-c', dry.stdout] if direct else ['/usr/bin/make', '--no-print-directory', 'sonar-local']
                result = subprocess.run(cmd, cwd=root, env=env | extra, capture_output=True, text=True)
                assert result.returncode == exit, (name, result.returncode, result.stderr)
                got = {f.name.removesuffix('-called') for f in root.glob('*-called')}
                assert got == set(expected), (name, got)
                assert 'dummy-env' not in result.stdout + result.stderr and 'dummy-keychain' not in result.stdout + result.stderr, name
                records.append({'name': name, 'exit': exit, 'passed': True})
            token = {'SONAR_TOKEN': 'dummy-env', 'MOCK_TOKEN': 'dummy-env'}
            case('missing credential', {}, 2)
            case('denied Keychain', {'MOCK_OS': 'Darwin'}, 2, ['security'])
            case('empty Keychain', {'MOCK_OS': 'Darwin', 'MOCK_KEYCHAIN': 'empty'}, 2, ['security'])
            case('environment precedence', token | {'MOCK_OS': 'Darwin'}, 0, ['prepare', 'maven', 'collect'])
            case('Keychain fallback', {'MOCK_OS': 'Darwin', 'MOCK_KEYCHAIN': 'ok', 'MOCK_TOKEN': 'dummy-keychain'}, 0, ['security', 'prepare', 'maven', 'collect'])
            case('main branch', token | {'MOCK_BRANCH': 'main'}, 0, ['prepare', 'maven', 'collect'])
            case('detached', {'MOCK_BRANCH': 'DETACHED'}, 2)
            case('unsupported', {'MOCK_BRANCH': 'feature/test'}, 2)
            for key, value in [('GITHUB_EVENT_NAME', 'pull_request'), ('CHANGE_ID', '12'), ('CI_MERGE_REQUEST_IID', '12'), ('SONAR_PULL_REQUEST_KEY', '12')]:
                case('PR ' + key, {key: value}, 2)
            case('prepare failure stops Maven', token | {'MOCK_PREPARE_EXIT': '3'}, 2, ['prepare'])
            case('retrieval incomplete propagates', token | {'MOCK_COLLECT_EXIT': '3'}, 3, ['prepare', 'maven', 'collect'], True)
            case('Maven failure has priority', token | {'MOCK_MAVEN_EXIT': '37', 'MOCK_COLLECT_EXIT': '3'}, 37, ['prepare', 'maven', 'collect'], True)
            case('failed gate still collects', token | {'MOCK_MAVEN_EXIT': '1', 'MOCK_COLLECT_EXIT': '1'}, 1, ['prepare', 'maven', 'collect'], True)
            case('Make reports failure', token | {'MOCK_COLLECT_EXIT': '3'}, 2, ['prepare', 'maven', 'collect'])
            for target in ['sonar-findings']:
                dry = subprocess.run(['/usr/bin/make', '-n', target], cwd=root, env=env, capture_output=True, text=True, check=True)
                subprocess.run(['/bin/sh', '-n'], input=dry.stdout, text=True, check=True)
                case('findings missing credential', {}, 2, direct=True)
                case('findings denied Keychain', {'MOCK_OS': 'Darwin'}, 2, ['security'], True)
                case('findings empty Keychain', {'MOCK_OS': 'Darwin', 'MOCK_KEYCHAIN': 'empty'}, 2, ['security'], True)
                case('findings environment precedence', token | {'MOCK_OS': 'Darwin'}, 0, ['findings'], True)
                case('findings Keychain fallback', {'MOCK_OS': 'Darwin', 'MOCK_KEYCHAIN': 'ok', 'MOCK_TOKEN': 'dummy-keychain'}, 0, ['security', 'findings'], True)
                case('findings main', token | {'MOCK_BRANCH': 'main'}, 0, ['findings'], True)
                case('findings detached', {'MOCK_BRANCH': 'DETACHED'}, 2, direct=True)
                case('findings unsupported', {'MOCK_BRANCH': 'feature/test'}, 2, direct=True)
                for key, value in [('GITHUB_EVENT_NAME', 'pull_request'), ('CHANGE_ID', '12'), ('CI_MERGE_REQUEST_IID', '12'), ('SONAR_PULL_REQUEST_KEY', '12')]:
                    case('findings PR ' + key, {key: value}, 2, direct=True)
                case('findings incomplete', token | {'MOCK_COLLECT_EXIT': '3'}, 3, ['findings'], True)
                case('findings failure', token | {'MOCK_COLLECT_EXIT': '1'}, 1, ['findings'], True)
            self.assertEqual(len(records), 31)
