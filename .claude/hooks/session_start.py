"""SessionStart hook for Claude Code and Codex: injects Alex's always-on skills into the session.

    python -B C:/Users/<user>/.claude/hooks/session_start.py <skill>   Claude Code, user level: one command per skill
    python -B .claude/hooks/session_start.py                           Codex project hook: every always-on skill

Both harnesses read the JSON `hookSpecificOutput.additionalContext` this prints (Codex drops plain text that opens
with '['). Claude Code caps each hook's additionalContext at 10,000 characters and measures every hook on its own;
past the cap only a 2,000-character preview arrives. One command per skill gives each skill that whole budget, so
tools/deploy_harness.py registers one per ALWAYS_ON name in ~/.claude/settings.json. Codex takes all of them in one
call, under the additional_context_limit in .codex/config.toml.

The text is stated as facts, not orders: the hooks docs warn that system-style imperatives can trip prompt-injection
defenses. A skill's leading `# Title` is dropped (the header names it) and whitespace is normalized; the rules
themselves pass through untouched.

On Windows `python3` is the Microsoft Store stub, so hooks call `python`; the absolute forward-slash path works
in whichever shell runs the hook. Each copy reads the skills folder beside its own hooks folder, so the user-level
hook injects the deployed copies and the Codex hook the repository ones. The mcmod-guzhenren repository copy is the
source: edit it there, then redeploy with tools/deploy_harness.py. When the session's project carries its own copy
of an injected skill that differs from it, the output says so.

A missing or unreadable skill is reported in the output, never fatal: the hook must not block a session start.
"""
import json
import os
import re
import sys
from pathlib import Path


CLAUDE_DIR = Path(__file__).resolve().parents[1]
SKILLS = CLAUDE_DIR / 'skills'
ALWAYS_ON = ['alex-constitution', 'i-have-adhd']
# Claude Code's cap on one hook's additionalContext; each registered command gets its own.
CAP = 10_000
FRONTMATTER = re.compile(r'\A---[^\S\r\n]*\r?\n.*?\r?\n---[^\S\r\n]*(?:\r?\n|\Z)', re.S)
TITLE = re.compile(r'\A# [^\n]*\n+')


def body(text):
    """The skill text without its leading YAML frontmatter."""
    return FRONTMATTER.sub('', text, count=1).strip()


def normalize(text):
    """The skill body as injected: LF line ends, no trailing spaces, no leading `# Title`, single blank lines."""
    text = '\n'.join(line.rstrip() for line in text.replace('\r\n', '\n').split('\n'))
    return re.sub(r'\n{3,}', '\n\n', TITLE.sub('', text.strip(), count=1)).strip()


def shown(path):
    """A path as the output names it: forward slashes, the home folder as ~."""
    try:
        return '~/' + Path(path).relative_to(Path.home()).as_posix()
    except ValueError:
        return Path(path).as_posix()


def block(name, skills=SKILLS):
    path = skills / name / 'SKILL.md'
    try:
        text = normalize(body(path.read_text(encoding='utf-8')))
    except (OSError, UnicodeDecodeError) as error:
        return (f'Always-on skill {name} could not be loaded from {shown(path)} ({type(error).__name__}); '
                'Alex needs to hear this before work starts.')
    return f'Always-on skill {name}, from {shown(path)}; it applies for the whole session.\n\n{text}'


def render(skills=SKILLS, names=ALWAYS_ON):
    return '\n\n'.join(block(name, skills) for name in names) + '\n'


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
    """A drift note when the session's project holds a different copy of an injected skill, else ''."""
    project = project or os.environ.get('CLAUDE_PROJECT_DIR')
    changed = drift(skills, names, Path(project) / '.claude' / 'skills') if project else []
    if not changed:
        return ''
    return (f'\nThe project copy of {", ".join(changed)} differs from the injected one in {shown(skills)}; '
            'Alex needs to hear this before work starts. Once the repository copy is right, '
            'python -B tools/deploy_harness.py redeploys it.\n')


def output(text):
    """The hook output in the JSON shape both harnesses read a SessionStart context from."""
    return json.dumps({'hookSpecificOutput': {'hookEventName': 'SessionStart', 'additionalContext': text}},
                      ensure_ascii=False) + '\n'


def main(argv=None):
    names = (sys.argv[1:] if argv is None else argv) or ALWAYS_ON
    try:
        if hasattr(sys.stdout, 'reconfigure'):
            sys.stdout.reconfigure(encoding='utf-8')
        sys.stdout.write(output(render(names=names) + notice(names=names)))
    except Exception:
        pass
    return 0


if __name__ == '__main__':
    sys.exit(main())
