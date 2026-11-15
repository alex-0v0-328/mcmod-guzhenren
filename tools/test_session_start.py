import json
import sys
import tempfile
import unittest
from pathlib import Path

import project_runtime as env

# The hook lives in .claude/hooks beside the skills it injects, not in tools/.
sys.path.insert(0, str(env.REPOSITORY_ROOT / '.claude' / 'hooks'))
import session_start


def write(root, name, text):
    folder = root / name
    folder.mkdir(parents=True, exist_ok=True)
    (folder / 'SKILL.md').write_bytes(text.encode('utf-8'))


class SessionStartTests(unittest.TestCase):

    def test_constants_and_text_helpers(self):
        assert session_start.SKILLS == env.REPOSITORY_ROOT / '.claude' / 'skills'
        assert session_start.ALWAYS_ON == ['alex-constitution', 'i-have-adhd']

        wrapped = json.loads(session_start.codex('rules：已启用\n'))
        assert wrapped == {'hookSpecificOutput': {'hookEventName': 'SessionStart', 'additionalContext': 'rules：已启用\n'}}

        assert session_start.body('---\nname: x\ndescription: y\n---\n\n# Body\ntext\n') == '# Body\ntext'
        assert session_start.body('---\r\nname: x\r\n---\r\nBody\r\n') == 'Body'
        assert session_start.body('No frontmatter\n---\nrule\n') == 'No frontmatter\n---\nrule'
        assert session_start.body('---\nunterminated\n') == '---\nunterminated'

    def test_render_and_drift(self):
        env.TEMP_ROOT.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=env.TEMP_ROOT) as tmp:
            root = Path(tmp)
            write(root, 'one', '---\nname: one\n---\nFirst rules：已启用\n')
            output = session_start.render(root, ['one', 'two'])
            assert 'Always-on skill "one"' in output and 'First rules：已启用' in output, output
            assert 'name: one' not in output
            assert 'skill "two" could not be loaded' in output and 'tell Alex' in output, output
            assert output.index('"one"') < output.index('"two"')

            ours, project = root / 'ours', root / 'project'
            write(ours, 'one', 'same\n')
            write(project / '.claude' / 'skills', 'one', 'same\n')
            assert session_start.drift(ours, ['one', 'two'], project / '.claude' / 'skills') == []
            assert session_start.drift(ours, ['one'], ours) == []
            assert session_start.notice(ours, ['one'], str(project)) == ''
            write(project / '.claude' / 'skills', 'one', 'edited\n')
            assert session_start.drift(ours, ['one', 'two'], project / '.claude' / 'skills') == ['one']
            warning = session_start.notice(ours, ['one'], str(project))
            assert 'project copy of one differs' in warning and 'deploy_harness.py' in warning, warning

    def test_live_skills_fit_the_hook_output_cap(self):
        live = session_start.render()
        for name in session_start.ALWAYS_ON:
            assert f'Always-on skill "{name}"' in live, f'live skill {name} is missing'
        assert len(live) < 10_000, f'hook output is {len(live)} characters, over the 10,000 Claude Code injects in full'


if __name__ == '__main__':
    unittest.main()
