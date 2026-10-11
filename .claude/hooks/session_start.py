"""SessionStart hook for Claude Code: injects Alex's always-on skills into the session.

    python3 ~/.claude/hooks/session_start.py          user-level hook: every local session, from ~/.claude/skills
    python3 .claude/hooks/session_start.py            repository hook: cloud sessions, from .claude/skills
    python3 .claude/hooks/session_start.py --deploy   copies the skills and this script from the repository into
                                                      ~/.claude and into the sibling repositories

Claude Code adds the hook's stdout to the session context. Skills otherwise load on demand; this makes
alex-constitution and i-have-adhd apply from the first message. Each copy reads the skills folder beside its
own hooks folder, so the user-level hook injects the deployed copies and the repository hook the tracked ones.
The mcmod-guzhenren repository copy is the source: edit it there, then redeploy, which also writes the skills, this
script and the cloud hook settings into each sibling repository beside it (SIBLINGS), so a cloud session
opened on one of those injects the same skills. When the session's project carries its own copy of an
always-on skill that differs from the injected one, the output says so.

A missing or unreadable skill is reported in the output, never fatal: the hook must not block a session
start. Claude Code caps hook output at 10,000 characters; past that only a 2,000-character preview arrives.
"""
import os
import re
import shutil
import sys
from pathlib import Path


CLAUDE_DIR = Path(__file__).resolve().parents[1]
SKILLS = CLAUDE_DIR / 'skills'
USER_CLAUDE_DIR = Path.home() / '.claude'
ALWAYS_ON = ['alex-constitution', 'i-have-adhd']
SOURCE = 'mcmod-guzhenren'
SIBLINGS = ['mcmod-guworld', 'mcmod-camera-shift', 'mcmod-burst-flight']
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
            'tell Alex before starting work. Once the repository copy is right, redeploy it with --deploy.\n')


def deploy(source=CLAUDE_DIR, target=USER_CLAUDE_DIR, names=ALWAYS_ON):
    """Copies the always-on skills and this script from a repository .claude folder into a user-level one."""
    if source.resolve() == target.resolve():
        raise SystemExit('--deploy runs from the repository copy, not from the deployed one')
    for name in names:
        shutil.copytree(source / 'skills' / name, target / 'skills' / name, dirs_exist_ok=True)
    (target / 'hooks').mkdir(parents=True, exist_ok=True)
    shutil.copy2(source / 'hooks' / 'session_start.py', target / 'hooks' / 'session_start.py')
    return target


def sync(source=CLAUDE_DIR, siblings=SIBLINGS, names=ALWAYS_ON):
    """Copies the always-on skills, this script and the cloud hook settings into each sibling repository's
    .claude folder; a sibling not checked out beside the source is skipped. Returns the folders written."""
    written = []
    for name in siblings:
        target = source.parent.parent / name / '.claude'
        if target.parent.is_dir():
            deploy(source, target, names)
            shutil.copy2(source / 'settings.json', target / 'settings.json')
            written.append(target)
    return written


def main(argv=None):
    argv = sys.argv[1:] if argv is None else argv
    if argv == ['--deploy']:
        if CLAUDE_DIR.parent.name != SOURCE:
            raise SystemExit(f'--deploy runs from the {SOURCE} repository copy, the source of every other copy')
        print(f'deployed {", ".join(ALWAYS_ON)} and the hook into {deploy()}')
        for target in sync():
            print(f'synced them and the cloud hook settings into {target}')
        return 0
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stdout.write(render() + notice())
    except Exception:
        pass
    return 0


if __name__ == '__main__':
    sys.exit(main())
