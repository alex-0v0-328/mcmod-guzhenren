"""SessionStart hook for Claude Code and Codex: injects Alex's always-on skills into the session.

    python -B C:/Users/<user>/.claude/hooks/session_start.py   user-level hook: every local session, from ~/.claude/skills
    python -B .claude/hooks/session_start.py --codex           Codex project hook (.codex/config.toml), from .claude/skills

On Windows `python3` is the Microsoft Store stub, so hooks call `python`; the absolute forward-slash path works
in whichever shell runs the hook.

Claude Code adds the hook's stdout to the session context; Codex takes it only as the JSON `additionalContext` of
--codex and drops plain text that opens with '['. Skills otherwise load on demand; this makes
alex-constitution and i-have-adhd apply from the first message. Each copy reads the skills folder beside its
own hooks folder, so the user-level hook injects the deployed copies and the Codex hook the repository ones.
The mcmod-guzhenren repository copy is the source: edit it there, then redeploy with tools/deploy_harness.py,
which copies the skills and this script into ~/.claude. The repository tracks all of it; the deployed copies
are per machine, so a new machine runs the deploy once.
When the session's project carries its own copy of an always-on skill that differs from the injected one, the
output says so.

A missing or unreadable skill is reported in the output, never fatal: the hook must not block a session
start. Claude Code caps hook output at 10,000 characters; past that only a 2,000-character preview arrives.
"""
import json
import os
import re
import sys
from pathlib import Path


CLAUDE_DIR = Path(__file__).resolve().parents[1]
SKILLS = CLAUDE_DIR / 'skills'
ALWAYS_ON = ['alex-constitution', 'i-have-adhd']
FRONTMATTER = re.compile(r'\A---[^\S\r\n]*\r?\n.*?\r?\n---[^\S\r\n]*(?:\r?\n|\Z)', re.S)


def body(text):
    """The skill text without its leading YAML frontmatter."""
    return FRONTMATTER.sub('', text, count=1).strip()


def render(skills=SKILLS, names=ALWAYS_ON):
    parts = []
    for name in names:
        path = skills / name / 'SKILL.md'
        try:
            text = body(path.read_text(encoding='utf-8'))
        except (OSError, UnicodeDecodeError) as error:
            parts.append(f'[session-start] The always-on skill "{name}" could not be loaded from {path} '
                         f'({type(error).__name__}); tell Alex before starting work.')
            continue
        parts.append(f'[session-start] Always-on skill "{name}" ({path}), in force for the whole session:'
                     f'\n\n{text}')
    return '\n\n'.join(parts) + '\n'


def drift(skills, names, other):
    """The names whose SKILL.md in `other` differs from the one in `skills`; a name `other` lacks is skipped."""
    changed = []
    for name in names:
        ours, theirs = skills / name / 'SKILL.md', other / name / 'SKILL.md'
        try:
            if theirs.is_file() and theirs.resolve() != ours.resolve() and theirs.read_bytes() != ours.read_bytes():
                changed.append(name)
        except OSError:
            changed.append(name)
    return changed


def notice(skills=SKILLS, names=ALWAYS_ON, project=None):
    """A drift warning when the session's project holds a different copy of an injected skill, else ''."""
    project = project or os.environ.get('CLAUDE_PROJECT_DIR')
    changed = drift(skills, names, Path(project) / '.claude' / 'skills') if project else []
    if not changed:
        return ''
    return (f'\n[session-start] The project copy of {", ".join(changed)} differs from the injected one in {skills}; '
            'tell Alex before starting work. Once the repository copy is right, redeploy it with '
            'python -B tools/deploy_harness.py.\n')


def codex(text):
    """The hook output in the JSON shape Codex reads a SessionStart context from."""
    return json.dumps({'hookSpecificOutput': {'hookEventName': 'SessionStart', 'additionalContext': text}},
                      ensure_ascii=False) + '\n'


def main(argv=None):
    argv = sys.argv[1:] if argv is None else argv
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        text = render() + notice()
        sys.stdout.write(codex(text) if argv == ['--codex'] else text)
    except Exception:
        pass
    return 0


if __name__ == '__main__':
    sys.exit(main())
