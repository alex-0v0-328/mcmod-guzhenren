import contextlib
import io
import json
import sys
import tempfile
import tomllib
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


def codex_hook():
    config = tomllib.loads((env.REPOSITORY_ROOT / '.codex' / 'config.toml').read_text(encoding='utf-8'))
    return config['hooks']['SessionStart'][0]['hooks'][0]


class SessionStartTests(unittest.TestCase):

    def test_constants_and_text_helpers(self):
        assert session_start.SKILLS == env.REPOSITORY_ROOT / '.claude' / 'skills'
        assert session_start.ALWAYS_ON == ['alex-constitution', 'i-have-adhd']

        wrapped = json.loads(session_start.output('rules：已启用\n'))
        assert wrapped == {'hookSpecificOutput': {'hookEventName': 'SessionStart', 'additionalContext': 'rules：已启用\n'}}

        assert session_start.body('---\nname: x\ndescription: y\n---\n\n# Body\ntext\n') == '# Body\ntext'
        assert session_start.body('---\r\nname: x\r\n---\r\nBody\r\n') == 'Body'
        assert session_start.body('No frontmatter\n---\nrule\n') == 'No frontmatter\n---\nrule'
        assert session_start.body('---\nunterminated\n') == '---\nunterminated'

        assert session_start.normalize('# Title\r\n\r\nrule one  \r\n\r\n\r\n\r\n## Part\nrule two\t\n') == \
            'rule one\n\n## Part\nrule two'
        assert session_start.normalize('## Part\n# Not a title\n') == '## Part\n# Not a title', 'only a leading H1'
        assert session_start.normalize('```\ncode  \n```') == '```\ncode\n```'

        assert session_start.shown(Path.home() / '.claude' / 'skills') == '~/.claude/skills'
        outside = Path(Path.home().anchor) / 'elsewhere' / 'SKILL.md'
        assert session_start.shown(outside) == outside.as_posix()

    def test_render_drift_and_main(self):
        env.TEMP_ROOT.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=env.TEMP_ROOT) as tmp:
            root = Path(tmp)
            write(root, 'one', '---\nname: one\n---\n# One\n\nFirst rules：已启用\n')
            output = session_start.render(root, ['one', 'two'])
            assert 'Always-on skill one, from ' in output and 'it applies for the whole session' in output, output
            assert 'First rules：已启用' in output and 'name: one' not in output and '# One' not in output, output
            assert 'skill two could not be loaded' in output and 'Alex needs to hear this' in output, output
            assert output.index('skill one') < output.index('skill two')
            assert '[session-start]' not in output and 'tell Alex' not in output, 'facts, not system-style orders'

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

        for argv, names in ((['i-have-adhd'], ['i-have-adhd']), ([], session_start.ALWAYS_ON)):
            printed = io.StringIO()
            with contextlib.redirect_stdout(printed):
                assert session_start.main(argv) == 0
            context = json.loads(printed.getvalue())['hookSpecificOutput']['additionalContext']
            for name in session_start.ALWAYS_ON:
                assert (f'Always-on skill {name},' in context) == (name in names), (argv, name)

    def test_live_skills_fit_each_harness_cap(self):
        for name in session_start.ALWAYS_ON:
            alone = session_start.render(names=[name])
            assert f'Always-on skill {name},' in alone, f'live skill {name} is missing'
            assert len(alone) < session_start.CAP, \
                f'{name} injects {len(alone)} characters, over the {session_start.CAP} one Claude Code hook carries'
        hook = codex_hook()
        assert hook['command'].endswith('session_start.py"'), 'the Codex hook injects every always-on skill'
        combined = session_start.render()
        assert len(combined) < hook['additional_context_limit'], \
            f'Codex gets {len(combined)} characters, over its additional_context_limit'


if __name__ == '__main__':
    unittest.main()
