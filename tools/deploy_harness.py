"""Deploys the always-on skills and the SessionStart hook from this repository into ~/.claude and registers the hook.

    python -B tools/deploy_harness.py    copies .claude/skills/<always-on> and .claude/hooks/session_start.py,
                                         then registers one SessionStart command per skill in ~/.claude/settings.json

The repository copies are the source. The user-level SessionStart hooks run the deployed script, which injects the
deployed skills into every local Claude Code session of every project. Claude Code caps each hook's output on its
own, so every always-on skill gets its own command and the whole budget. Registering replaces only the commands
that run session_start.py and keeps every other setting and hook. Codex and opencode read the repository copies
directly (.codex/config.toml, opencode.json) and need no deploy. Sessions open only from this checkout, so no
sibling repository gets a copy. test_deploy_harness.py fails until the deployed copies and the registration match.
"""
import argparse
import json
import shutil
import sys
from pathlib import Path

import project_runtime as env

# The hook lives in .claude/hooks beside the skills it injects; its ALWAYS_ON list is the one source of the names.
sys.path.insert(0, str(env.REPOSITORY_ROOT / '.claude' / 'hooks'))
import session_start


SOURCE_CLAUDE_DIR = env.REPOSITORY_ROOT / '.claude'
USER_CLAUDE_DIR = Path.home() / '.claude'
HOOK = Path('hooks') / 'session_start.py'
SETTINGS = 'settings.json'


def mirror(origin, copy):
    """Copies folder `origin` over `copy` and removes what `origin` lacks, so a copy never keeps a deleted file."""
    shutil.copytree(origin, copy, dirs_exist_ok=True)
    for item in sorted(copy.rglob('*'), reverse=True):
        if not (origin / item.relative_to(copy)).exists():
            item.rmdir() if item.is_dir() and not item.is_symlink() else item.unlink()


def hook_commands(target=USER_CLAUDE_DIR, names=session_start.ALWAYS_ON):
    """The SessionStart command for each always-on skill, running the hook deployed in `target`."""
    return [f'python -B {(target / HOOK).as_posix()} {name}' for name in names]


def registered(target=USER_CLAUDE_DIR):
    """The SessionStart commands in `target`'s settings.json that run session_start.py, in order."""
    path = target / SETTINGS
    if not path.is_file():
        return []
    groups = json.loads(path.read_text(encoding='utf-8')).get('hooks', {}).get('SessionStart', [])
    return [hook.get('command', '') for group in groups for hook in group.get('hooks', [])
            if HOOK.name in hook.get('command', '')]


def register(target=USER_CLAUDE_DIR, names=session_start.ALWAYS_ON):
    """Points `target`'s SessionStart hooks at one command per skill, keeping every other setting and hook."""
    path = target / SETTINGS
    settings = json.loads(path.read_text(encoding='utf-8')) if path.is_file() else {}
    groups = settings.setdefault('hooks', {}).setdefault('SessionStart', [])
    for group in groups:
        group['hooks'] = [hook for hook in group.get('hooks', []) if HOOK.name not in hook.get('command', '')]
    groups[:] = [group for group in groups if group['hooks']]
    groups.append({'hooks': [{'type': 'command', 'command': command} for command in hook_commands(target, names)]})
    path.write_text(json.dumps(settings, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def deploy(source=SOURCE_CLAUDE_DIR, target=USER_CLAUDE_DIR, names=session_start.ALWAYS_ON):
    """Copies the always-on skills and the hook from a repository .claude folder into a user-level one, then
    registers the hook there."""
    if source.resolve() == target.resolve():
        raise SystemExit('deploy_harness.py copies from the repository .claude folder, not onto it')
    for name in names:
        mirror(source / 'skills' / name, target / 'skills' / name)
    (target / HOOK).parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source / HOOK, target / HOOK)
    register(target, names)
    return target


def main(argv=None):
    argparse.ArgumentParser(description=__doc__.splitlines()[0]).parse_args(argv)
    target = deploy()
    print(f'deployed {", ".join(session_start.ALWAYS_ON)} and the hook into {target}, '
          f'one SessionStart command per skill in {target / SETTINGS}')
    return 0


if __name__ == '__main__':
    sys.exit(main())
