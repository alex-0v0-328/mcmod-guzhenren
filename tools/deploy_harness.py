"""Deploys the always-on skills and the SessionStart hook from this repository into ~/.claude.

    python3 -B tools/deploy_harness.py    copies .claude/skills/<always-on> and .claude/hooks/session_start.py

The repository copies are the source. The user-level SessionStart hook (~/.claude/settings.json) runs the deployed
script, which injects the deployed skills into every local Claude Code session of every project; Codex and opencode
read the repository copies directly (.codex/config.toml, opencode.json) and need no deploy. Sessions open only from
this checkout, so no sibling repository gets a copy. test_deploy_harness.py fails until the deployed copies match.
"""
import argparse
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


def mirror(origin, copy):
    """Copies folder `origin` over `copy` and removes what `origin` lacks, so a copy never keeps a deleted file."""
    shutil.copytree(origin, copy, dirs_exist_ok=True)
    for item in sorted(copy.rglob('*'), reverse=True):
        if not (origin / item.relative_to(copy)).exists():
            item.rmdir() if item.is_dir() and not item.is_symlink() else item.unlink()


def deploy(source=SOURCE_CLAUDE_DIR, target=USER_CLAUDE_DIR, names=session_start.ALWAYS_ON):
    """Copies the always-on skills and the hook from a repository .claude folder into a user-level one."""
    if source.resolve() == target.resolve():
        raise SystemExit('deploy_harness.py copies from the repository .claude folder, not onto it')
    for name in names:
        mirror(source / 'skills' / name, target / 'skills' / name)
    (target / HOOK).parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source / HOOK, target / HOOK)
    return target


def main(argv=None):
    argparse.ArgumentParser(description=__doc__.splitlines()[0]).parse_args(argv)
    print(f'deployed {", ".join(session_start.ALWAYS_ON)} and the hook into {deploy()}')
    return 0


if __name__ == '__main__':
    sys.exit(main())
