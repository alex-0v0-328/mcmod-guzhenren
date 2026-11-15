import json
import tempfile
import unittest
from pathlib import Path

import deploy_harness
import project_runtime as env


def write(root, name, text):
    folder = root / name
    folder.mkdir(parents=True, exist_ok=True)
    (folder / 'SKILL.md').write_bytes(text.encode('utf-8'))


class DeployHarnessTests(unittest.TestCase):

    def test_constants(self):
        assert deploy_harness.SOURCE_CLAUDE_DIR == env.REPOSITORY_ROOT / '.claude'
        assert deploy_harness.USER_CLAUDE_DIR == Path.home() / '.claude'
        assert (deploy_harness.SOURCE_CLAUDE_DIR / deploy_harness.HOOK).is_file()

    def test_deploy_mirrors_the_skills_and_copies_the_hook(self):
        env.TEMP_ROOT.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=env.TEMP_ROOT) as tmp:
            root = Path(tmp)
            source, target = root / 'repo' / '.claude', root / 'home' / '.claude'
            write(source / 'skills', 'one', 'rules\n')
            (source / 'skills' / 'one' / 'LICENSE').write_bytes(b'MIT\n')
            (source / 'hooks').mkdir(parents=True)
            (source / 'hooks' / 'session_start.py').write_bytes(b'# hook\n')
            write(target / 'skills', 'one', 'stale\n')
            write(target / 'skills' / 'one' / 'gone', 'stale', 'removed upstream\n')
            write(target / 'skills', 'other', 'not ours\n')
            assert deploy_harness.deploy(source, target, ['one']) == target
            assert (target / 'skills' / 'one' / 'SKILL.md').read_bytes() == b'rules\n'
            assert (target / 'skills' / 'one' / 'LICENSE').read_bytes() == b'MIT\n'
            assert not (target / 'skills' / 'one' / 'gone').exists(), 'a file the source lacks must not survive'
            assert (target / 'skills' / 'other' / 'SKILL.md').read_bytes() == b'not ours\n', 'only named skills move'
            assert (target / 'hooks' / 'session_start.py').read_bytes() == b'# hook\n'
            assert deploy_harness.registered(target) == deploy_harness.hook_commands(target, ['one'])
            with self.assertRaises(SystemExit):
                deploy_harness.deploy(source, source, ['one'])

    def test_register_replaces_only_the_hook_commands(self):
        env.TEMP_ROOT.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=env.TEMP_ROOT) as tmp:
            target = Path(tmp)
            other = {'type': 'command', 'command': 'notify.ps1'}
            (target / 'settings.json').write_text(json.dumps({
                'autoUpdatesChannel': 'latest',
                'hooks': {'SessionStart': [
                    {'hooks': [{'type': 'command', 'command': 'python -B C:/old/hooks/session_start.py'}, other]},
                    {'matcher': 'compact', 'hooks': [{'type': 'command', 'command': 'python -B x/session_start.py'}]},
                ], 'Stop': [{'hooks': [other]}]},
            }), encoding='utf-8')
            for _ in range(2):
                deploy_harness.register(target, ['one', 'two'])
                settings = json.loads((target / 'settings.json').read_text(encoding='utf-8'))
                assert settings['autoUpdatesChannel'] == 'latest' and settings['hooks']['Stop'] == [{'hooks': [other]}]
                assert settings['hooks']['SessionStart'][0] == {'hooks': [other]}, 'other SessionStart hooks stay'
                assert len(settings['hooks']['SessionStart']) == 2, 'a group left empty goes, and reruns add none'
                assert deploy_harness.registered(target) == [
                    f'python -B {(target / "hooks" / "session_start.py").as_posix()} {name}' for name in ('one', 'two')]
            fresh = target / 'fresh'
            fresh.mkdir()
            deploy_harness.register(fresh, ['one'])
            assert deploy_harness.registered(fresh) == deploy_harness.hook_commands(fresh, ['one'])

    def test_live_deployed_copies_match_the_repository(self):
        deployed, source = deploy_harness.USER_CLAUDE_DIR, deploy_harness.SOURCE_CLAUDE_DIR
        for name in deploy_harness.session_start.ALWAYS_ON:
            assert (deployed / 'skills' / name / 'SKILL.md').is_file(), f'{name} is not deployed; run deploy_harness.py'
        assert deploy_harness.session_start.drift(source / 'skills', deploy_harness.session_start.ALWAYS_ON,
                                                  deployed / 'skills') == [], \
            'deployed skills differ from the repository; run deploy_harness.py'
        assert (deployed / deploy_harness.HOOK).read_bytes() == (source / deploy_harness.HOOK).read_bytes(), \
            'deployed hook is stale; run deploy_harness.py'
        assert deploy_harness.registered() == deploy_harness.hook_commands(), \
            'SessionStart is not one command per always-on skill; run deploy_harness.py'


if __name__ == '__main__':
    unittest.main()
