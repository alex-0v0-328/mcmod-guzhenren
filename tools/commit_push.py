"""One-command commit and push for the two mods and the wiki: verify the exact trees GitHub will get, land
every commit at once, push, wait for CI.

    python -B tools/commit_push.py PLAN.json             preflight, verify, commit, contributor gate, push, wait for CI
    python -B tools/commit_push.py PLAN.json --dry-run   preflight and verify only
    python -B tools/commit_push.py --push-only           the same for commits already made by hand: verify, gate, push
    python -B tools/commit_push.py PLAN.json --project guworld    commits that name no project go to Gu World
    python -B tools/commit_push.py PLAN.json --full      verify with every stage, whatever the change touches
    python -B tools/commit_push.py --ci [SHA]            wait for the GitHub Actions run of SHA (default HEAD)

PLAN.json lists the commits in order, with their Conventional Commits titles and an optional body
(which may end in a BREAKING CHANGE footer). One plan may ship all three repositories:

    {"commits": [{"project": "guworld", "title": "fix(aperture): ...", "body": "...",
                  "files": ["src/main/java/...", "src/generated/.../folder/"],
                  "patches": ["shared-lang.patch"],
                  "contents": {"src/main/java/.../Shared.java": "Shared.java.staged"}}]}

"project" is guzhenren, guworld or wiki (default: --project). A commit takes whole files ("files": a path
ending in / takes everything under it, deletions included), part of a file another session also edits
("patches": `git diff` output applied to the index alone; "contents": the file exactly as it should be
committed, read from another file), or any mix. Patch and content sources are relative to the plan's folder.
The repositories ship in dependency order, Gu World, Guzhenren, wiki, unless the plan carries
"order": ["guzhenren", "guworld"].

Preflight derives, without touching the checkout or its index, the tree each repository's commits add up
to. Verify then runs what that change calls for (needs) on an export of exactly that tree, in one of the
verification slots (project_runtime.Slot): several runs verify at once, each in its own folder, and none
takes the Gradle lock of the live checkouts. A mod change runs what GitHub runs (build, runData drift), so a
forgotten file or an undeclared run/mods jar fails here instead of on GitHub, then the local stages (JUnit
with the wiki gate, GameTests), against the partner mod exported from its own committed or planned tree. A
change to the tools or the agent harness runs the tool tests; README and editor files run nothing; the wiki
runs its link check. A tree that already passed is not verified twice (dry run then ship, a retry after a
failed push or gate).

Landing is the only serialized part and takes seconds: under one lock, every tree is derived again and must
still be the verified one (else the run verifies again, as when another session landed first), the commits
are built beside the branch with `git commit-tree`, the contributor gate reads them, and only then does
each branch move, in one step, and get pushed. Nothing is ever half committed and the index is never staged
into. The gate reads every commit in origin/<branch>..<new tip> and refuses when an author or committer
differs from the git config identity or looks like an AI, or when a message carries a Co-Authored-By or
generated-with line: GitHub would list that AI as a contributor. The wait for GitHub Actions holds no lock.
Everything goes to <Temp>/<project>/logs/commit_push-<pid>.log, one per run. Exit 0 = shipped and CI green
(with --dry-run: ready to ship).

A repository with no commit yet, or a remote without main yet, ships too: the scratch index then starts
from the empty tree, the gate reads every local commit no origin branch holds, and the first push sets the
upstream.
"""
import argparse
import hashlib
import json
import os
import re
import secrets
import shutil
import subprocess
import sys
import time
from dataclasses import dataclass
from pathlib import Path

import local_checks as check
import project_runtime as env


TYPES = ('feat', 'fix', 'build', 'chore', 'ci', 'docs', 'style', 'refactor', 'perf', 'test')
TITLE = re.compile(rf'^({"|".join(TYPES)})(\([a-z0-9][a-z0-9._/-]*\))?!?: \S.*$')
FOOTER = re.compile(r'co-authored-by|generated with', re.I)
AI_IDENTITY = re.compile(r'claude|anthropic|codex|openai|opencode|deepseek', re.I)
LOG_FORMAT = '%H%x1f%an%x1f%ae%x1f%cn%x1f%ce%x1f%B%x1e'
CI_JAR = re.compile(r'^\s*\w+_file=(\S+\.jar)\s*$', re.M)
RUN_MODS_JAR = re.compile(r"files\('run/mods/([^']+\.jar)'\)")
BRANCH = 'main'
WORKFLOW = Path('.github') / 'workflows' / 'build.yml'
EMPTY_TREE = '4b825dc642cb6eb9a060e54bf8d69288fbee4904'
ZERO_SHA = '0' * 40
# Gu World first, so Guzhenren's runs and links already find it; the wiki documents both.
SHIP_ORDER = ('guworld', 'guzhenren', 'wiki')
# Paths no check reads: a change to them alone ships unverified.
DOCS = re.compile(r'^(README[^/]*|LICENSE[^/]*|\.gitignore|\.gitattributes|\.editorconfig|\.idea/.*)$')
# The tools and the agent harness: the tool tests cover them, Gradle never sees them.
HARNESS = re.compile(r'^(tools/.*|\.claude/.*|\.codex/.*|AGENTS\.md|CLAUDE\.md|opencode\.json)$')
# How often a run verifies again because main or a planned file moved before it could land.
ROUNDS = 3
# A folder entry that takes more changed paths than this is spelled out: it may hold another session's work.
WIDE_ENTRY = 50
STALE_SECONDS = 24 * 3600
# Part of every verification record's key: raise it when what a stage checks changes.
CACHE_VERSION = 'v1'


class Stop(Exception):
    """A failed step: the message is the reason shown to the caller."""


class LandLock(env.Lock):
    busy = 'another commit_push run (pid {holder}) is landing its commits; wait for it to finish'
    waiting = 'waiting for another run (pid {holder}) to land its commits ...'


class PartnerLock(env.Lock):
    busy = 'another commit_push run (pid {holder}) is building the partner jar; wait for it to finish'
    waiting = 'waiting for another run (pid {holder}) to build the partner jar ...'


@dataclass
class Target:
    """What a repository's planned commits add up to: the commit they start from (None in an empty
    repository), the tree after each commit, the paths each commit touches, and the scratch index
    holding the last tree."""
    head: str
    trees: list
    groups: list
    tree: str
    index: Path


def normalize(path):
    path = path.strip()
    while path.startswith('./'):
        path = path[2:]
    return path


def validate_plan(plan, projects=None, default='guzhenren'):
    """Problems with the plan's shape, titles and paths; empty when the plan is usable."""
    projects = projects or tuple(env.ship_projects())
    commits = plan.get('commits') if isinstance(plan, dict) else None
    if not isinstance(commits, list) or not commits:
        return ['plan needs a non-empty "commits" list']
    errors = []
    order = plan.get('order')
    if order is not None and (not isinstance(order, list) or len(set(order)) != len(order)
                              or not all(name in projects for name in order)):
        errors.append(f'"order" must list each of {", ".join(projects)} at most once')
    seen = {}
    for number, commit in enumerate(commits, 1):
        entry = commit if isinstance(commit, dict) else {}
        title, body, files = entry.get('title'), entry.get('body', ''), entry.get('files', [])
        patches, contents, project = entry.get('patches', []), entry.get('contents', {}), entry.get('project', default)
        if not isinstance(title, str) or '\n' in title or not TITLE.match(title):
            errors.append(f'commit {number}: title must look like "<type>[(scope)][!]: <description>" '
                          f'(types {", ".join(TYPES)}), got {title!r}')
        elif not isinstance(body, str):
            errors.append(f'commit {number}: "body" must be a string')
        elif FOOTER.search(title + '\n' + body):
            errors.append(f'commit {number}: no attribution footers in titles or bodies')
        if project not in projects:
            errors.append(f'commit {number}: "project" must be one of {", ".join(projects)}, got {project!r}')
        if not isinstance(patches, list) or not all(isinstance(p, str) and p.strip() for p in patches):
            errors.append(f'commit {number}: "patches" must be a list of patch files')
            patches = []
        if not isinstance(contents, dict) or not all(isinstance(k, str) and k.strip() and isinstance(v, str)
                                                     and v.strip() for k, v in contents.items()):
            errors.append(f'commit {number}: "contents" must map a repository path to the file holding its content')
            contents = {}
        if not isinstance(files, list) or not all(isinstance(f, str) and f.strip() for f in files) or not (
                files or patches or contents):
            errors.append(f'commit {number}: "files" must be a non-empty list of paths '
                          '(or give "patches" or "contents")')
            continue
        for raw in list(files) + list(contents):
            path = normalize(raw)
            if path.startswith('/') or '..' in path.split('/'):
                errors.append(f'commit {number}: {raw!r} must be relative to the repository root')
            elif (project, path) in seen:
                errors.append(f'commit {number}: {path} is already in commit {seen[project, path]}')
            else:
                # A folder in one group would swallow a file listed under it in another group.
                overlap = next((other for owner, other in seen if owner == project
                                and seen[owner, other] != number
                                and (covers(other, path) or covers(path, other))), None)
                if overlap:
                    errors.append(f'commit {number}: {path} overlaps {overlap} in commit {seen[project, overlap]}')
                seen[project, path] = number
    return errors


def needs(project, paths, full=False):
    """What a change to `paths` is verified with, in running order: 'links' (the wiki's link check), 'tools'
    (the tool tests), 'gradle' (the CI chain and the local stages). Empty when no check reads those paths.
    Any path this does not know for certain takes the whole Gradle chain."""
    if project.name == env.WIKI.name:
        return ['links'] if paths or full else []
    has_tools = 'tools' in project.stages
    kinds = {'tools', 'gradle'} if full else set()
    for path in paths:
        if DOCS.match(path):
            continue
        kinds.add('tools' if has_tools and HARNESS.match(path) else 'gradle')
    return [kind for kind in ('tools', 'gradle') if kind in kinds and (kind != 'tools' or has_tools)]


def parse_log(output):
    """Commit records from `git log --format=LOG_FORMAT`: (sha, author name, author email,
    committer name, committer email, message)."""
    records = []
    for chunk in output.split('\x1e'):
        fields = chunk.strip('\r\n').split('\x1f')
        if len(fields) == 6:
            records.append(tuple(fields))
    return records


def contributor_problems(records, identity):
    """Why GitHub would list anyone but `identity` on these commits; empty when each is his alone."""
    problems = []
    if any(AI_IDENTITY.search(part) for part in identity):
        problems.append(f'the git config identity {identity[0]} <{identity[1]}> looks like an AI')
    for sha, author, author_email, committer, committer_email, message in records:
        for role, name, email in (('author', author, author_email), ('committer', committer, committer_email)):
            if (name, email) != identity:
                problems.append(f'{sha[:7]}: {role} is {name} <{email}>, expected {identity[0]} <{identity[1]}>')
        for line in message.splitlines():
            if FOOTER.search(line):
                problems.append(f'{sha[:7]}: message carries "{line.strip()}"')
    return problems


def ci_jars(workflow_text):
    return sorted(set(CI_JAR.findall(workflow_text)))


def missing_ci_jars(build_text, workflow_text):
    """Jars the build compiles against that the CI workflow never downloads: the run/mods files the
    build script names."""
    return sorted(set(RUN_MODS_JAR.findall(build_text)) - set(ci_jars(workflow_text)))


def parse_porcelain(output):
    """Paths from `git status --porcelain=v1 -z`; a rename contributes both of its paths."""
    entries = output.split('\0')
    paths = []
    index = 0
    while index < len(entries):
        entry = entries[index]
        index += 1
        if len(entry) < 4:
            continue
        paths.append(entry[3:])
        if entry[0] in 'RC':
            paths.append(entries[index])
            index += 1
    return paths


def covers(entry, path):
    entry = entry.rstrip('/')
    return path == entry or path.startswith(entry + '/')


def coverage(entries, changed):
    """(plan entries that match no change, changed paths no plan entry takes)."""
    idle = [entry for entry in entries if not any(covers(entry, path) for path in changed)]
    leftovers = [path for path in changed if not any(covers(entry, path) for entry in entries)]
    return idle, leftovers


def wide_entries(entries, changed, limit=WIDE_ENTRY):
    """A note for each plan entry that takes more than `limit` changed paths, with its largest folders: a
    folder entry ships everything under it, another session's unfinished work included."""
    notes = []
    for entry in entries:
        taken = [path for path in changed if covers(entry, path)]
        if len(taken) <= limit:
            continue
        depth = len(entry.rstrip('/').split('/'))
        folders = {}
        for path in taken:
            folder = '/'.join(path.split('/')[:depth + 1])
            folders[folder] = folders.get(folder, 0) + 1
        largest = sorted(folders.items(), key=lambda item: (-item[1], item[0]))[:4]
        notes.append(f'{entry} takes {len(taken)} changed paths (' + ', '.join(
            f'{folder} {count}' for folder, count in largest) + '): all of them ship, whoever edited them')
    return notes


# ----- GitHub Actions over REST (curl -L, no gh CLI) ---------------------------------------------------

def parse_remote(url):
    """Extract (owner, repo) from a GitHub remote URL, https or ssh form."""
    url = url.strip()
    if url.endswith('.git'):
        url = url[:-4]
    if 'github.com/' in url:
        path = url.split('github.com/', 1)[1]
    elif url.startswith('git@github.com:'):
        path = url.split(':', 1)[1]
    else:
        raise ValueError(f'not a GitHub remote: {url}')
    parts = path.strip('/').split('/')
    if len(parts) != 2 or not all(parts):
        raise ValueError(f'cannot parse owner/repo from: {url}')
    return parts[0], parts[1]


def api_get(url, token=''):
    """(body, HTTP status) of a GitHub REST GET; a token goes through stdin, never the command line."""
    command = ['curl', '-sL', '-w', '\n%{http_code}', url]
    if token:
        command[1:1] = ['-H', '@-']
    # GitHub answers in UTF-8: pin it rather than trust the locale with non-ASCII commit text.
    result = subprocess.run(command, input=f'Authorization: Bearer {token}\n' if token else None,
                            capture_output=True, text=True, encoding='utf-8')
    if result.returncode != 0:
        raise RuntimeError(f'curl failed ({result.returncode}): {result.stderr.strip()}')
    body, _, status = result.stdout.rpartition('\n')
    return body, int(status) if status.isdigit() else 0


def fetch_runs(owner, repo, per_page=5, get=api_get, sha=''):
    # Anonymous calls share 60 per hour, which one busy session exhausts; the token Alex keeps for
    # the github MCP lifts that to 5000. A rejected token falls back to anonymous. A full SHA asks for
    # that commit's runs alone, however many other pushes followed it.
    api = f'https://api.github.com/repos/{owner}/{repo}/actions/runs?per_page={per_page}'
    if len(sha) == 40:
        api += f'&head_sha={sha}'
    token = os.environ.get('GITHUB_PERSONAL_ACCESS_TOKEN', '').strip().strip('"')
    body, status = get(api, token)
    if status == 401 and token:
        body, status = get(api, '')
    if status != 200:
        try:
            message = json.loads(body).get('message', '')
        except ValueError:
            message = body[:120]
        raise RuntimeError(f'GitHub API {status}: {message}')
    return json.loads(body).get('workflow_runs', [])


def describe(run):
    title = run.get('display_title') or run.get('name') or '?'
    branch = run.get('head_branch') or '?'
    sha = (run.get('head_sha') or '?')[:7]
    return f'{title} [{branch} {sha}] status={run.get("status")} conclusion={run.get("conclusion")}'


def poll(owner, repo, sha, timeout=600, interval=15, fetch=fetch_runs, sleep=time.sleep, now=time.monotonic,
         echo=print):
    """Wait for the latest Actions run of commit `sha` (a prefix will do).

    Returns the conclusion string, or None on timeout. A failed fetch (network, rate limit) is
    retried until the deadline instead of aborting the wait.
    """
    deadline = now() + timeout
    while True:
        try:
            runs = [run for run in fetch(owner, repo) if (run.get('head_sha') or '').startswith(sha)]
        except RuntimeError as error:
            echo(f'fetch failed, retrying: {error}')
        else:
            if runs:
                echo(describe(runs[0]))
                if runs[0].get('status') == 'completed':
                    return runs[0].get('conclusion')
            else:
                echo(f'no workflow run for {sha[:7]} yet')
        if now() >= deadline:
            return None
        sleep(interval)


def ci_run(root, sha, timeout, echo=print, fetch=None):
    """(conclusion or None, run URL) of the Actions run of `sha` in the repository at `root`."""
    owner, repo = parse_remote(env.git('remote', 'get-url', 'origin', cwd=root)[1])
    fetch = fetch or (lambda owner, repo, **options: fetch_runs(owner, repo, sha=sha, **options))
    conclusion = poll(owner, repo, sha, timeout=timeout, interval=30, fetch=fetch, echo=echo)
    try:
        runs = [run for run in fetch(owner, repo) if (run.get('head_sha') or '').startswith(sha)]
    except RuntimeError:
        runs = []
    return conclusion, runs[0].get('html_url', '') if runs else ''


def project_flags(project):
    """The command-line flags that select `project`, for a hint the caller can paste."""
    return f' --project {project.name}' if project.name != 'guzhenren' else ''


class Repo:
    """One repository's part of a run: its planned commits, the tree they add up to, the git plumbing."""

    def __init__(self, run, project, commits):
        self.run = run
        self.project = project
        self.name = project.name
        self.root = project.root
        self.commits = commits
        self.files = [path for commit in commits for path in commit['files']]
        self.branch = BRANCH
        self.remote_branch = True
        self.changed = []
        self.target = None
        self.kinds = []
        self.seal = None
        self.verified = None
        self.tip = None
        self.pushed = False

    def git(self, *arguments, index=None, check=True):
        return self.run.git(self.root, *arguments, index=index, check=check)

    def scratch(self, name):
        return self.run.work / f'{self.name}.{name}'

    def require_branch(self):
        # symbolic-ref, not rev-parse: it also names the branch of a repository with no commit yet.
        branch = self.git('symbolic-ref', '--short', 'HEAD', check=False)[1].strip()
        if branch != self.branch:
            raise Stop(f'{self.name}: on branch {branch or "(detached HEAD)"}, not {self.branch}')

    def head(self):
        code, sha = self.git('rev-parse', '--verify', '--quiet', 'HEAD', check=False)
        return sha.strip() if code == 0 else None

    def sync_remote(self):
        """Fetch origin/<branch> and return (behind, ahead); a remote without the branch is behind by nothing."""
        branch = self.branch
        self.remote_branch = bool(self.git('ls-remote', '--heads', 'origin', branch)[1].strip())
        if not self.remote_branch:
            ahead = len(self.unpushed_shas()) if self.head() else 0
            return 0, ahead
        self.git('fetch', '--quiet', 'origin', branch)
        if not self.head():
            raise Stop(f'{self.name}: origin/{branch} exists but this repository has no commit yet; '
                       'clone or pull first')
        behind = int(self.git('rev-list', '--count', f'HEAD..origin/{branch}')[1].strip())
        ahead = int(self.git('rev-list', '--count', f'origin/{branch}..HEAD')[1].strip())
        return behind, ahead

    def unpushed_range(self, tip='HEAD'):
        """git log arguments for the commits a push of `tip` adds: past origin/<branch>, or, for a branch the
        remote lacks, every commit no origin branch already holds."""
        return [f'origin/{self.branch}..{tip}'] if self.remote_branch else [tip, '--not', '--remotes=origin']

    def unpushed_shas(self):
        return [line for line in self.git('rev-list', *self.unpushed_range())[1].split() if line]

    def pushed_tree(self):
        """The tree GitHub last got, which earlier pushes verified: what this push is compared with."""
        if not self.remote_branch:
            return EMPTY_TREE
        return self.git('rev-parse', f'origin/{self.branch}^{{tree}}')[1].strip()

    def names(self, before, after):
        """The paths that differ between two trees."""
        return [path for path in self.git('diff-tree', '-r', '--name-only', '-z', before, after)[1].split('\0')
                if path]

    def show(self, tree, path):
        """A file of a tree as text; empty when the tree has no such file."""
        code, text = self.git('show', f'{tree}:{path}', check=False)
        return text if code == 0 else ''

    def expand(self, entries):
        """The changed paths a set of plan entries takes, as git status listed them."""
        return [path for path in self.changed if any(covers(entry, path) for entry in entries)]

    def stage(self, paths, index):
        # Exact paths from git status, through a pathspec file so a long list cannot overflow the
        # shell command line. --force: a tracked file deleted under a now-ignored folder is still
        # refused as "ignored" otherwise. It cannot drag in ignored
        # files, because git status never lists an ignored untracked path.
        pathspec = self.scratch('pathspec')
        pathspec.write_bytes(b'\0'.join((':(literal)' + path).encode('utf-8') for path in paths))
        self.git('add', '-A', '--force', f'--pathspec-from-file={pathspec}', '--pathspec-file-nul', index=index)

    def stage_content(self, path, source, index):
        """Put `source`'s bytes in the index as `path`, through the path's own clean filters, keeping its mode."""
        blob = self.git('hash-object', '-w', f'--path={path}', str(source))[1].strip()
        entry = self.git('ls-files', '-s', '--', ':(literal)' + path, index=index)[1].strip()
        mode = entry.split(' ', 1)[0] if entry else '100644'
        self.git('update-index', '--add', '--cacheinfo', f'{mode},{blob},{path}', index=index)

    def derive(self):
        """Build, in a scratch index on top of HEAD, the tree after each planned commit. The checkout and its
        own index are only read."""
        head = self.head()
        self.changed = parse_porcelain(self.git('status', '--porcelain=v1', '-z', '--untracked-files=all')[1])
        idle, _ = coverage(self.files, self.changed)
        if idle:
            raise Stop(f'{self.name}: plan paths without any change: ' + ', '.join(idle))
        index = self.scratch('index')
        index.unlink(missing_ok=True)
        self.git('read-tree', *([head] if head else ['--empty']), index=index)
        previous = self.git('write-tree', index=index)[1].strip()
        trees, groups = [], []
        for commit in self.commits:
            paths = self.expand(commit['files'])
            if paths:
                self.stage(paths, index)
            for path, source in commit['contents'].items():
                self.stage_content(path, source, index)
            for patch in commit['patches']:
                self.git('apply', '--cached', '--whitespace=nowarn', str(patch), index=index)
            tree = self.git('write-tree', index=index)[1].strip()
            touched = self.names(previous, tree)
            if not touched:
                raise Stop(f'{self.name}: "{commit["title"]}" changes nothing')
            trees.append(tree)
            groups.append(touched)
            previous = tree
        return Target(head, trees, groups, previous, index)

    def missing_jars(self, tree=None):
        if self.project.ci_jar_dir is None:
            return []
        if tree:
            return missing_ci_jars(self.show(tree, 'build.gradle'), self.show(tree, WORKFLOW.as_posix()))
        workflow = (self.root / WORKFLOW).read_text(encoding='utf-8')
        return missing_ci_jars((self.root / 'build.gradle').read_text(encoding='utf-8'), workflow)

    def prepare(self):
        self.require_branch()
        behind, ahead = self.sync_remote()
        if behind:
            raise Stop(f'{self.name}: origin/{self.branch} is {behind} commit(s) ahead; pull first')
        if not self.commits and not ahead:
            raise Stop(f'{self.name}: no local commits to push')
        for commit in self.commits:
            for source in list(commit['patches']) + list(commit['contents'].values()):
                if not Path(source).is_file():
                    raise Stop(f'{self.name}: "{commit["title"]}" names {source}, which is not a file')
        self.target = self.derive()
        missing = self.missing_jars(self.target.tree)
        if missing:
            raise Stop(f'{self.name}: the build uses {self.project.ci_jar_dir} jars CI never downloads: '
                       + ', '.join(missing))
        if self.name != env.WIKI.name and self.commits:
            # Markdown ends a line in two blanks on purpose; the mods' sources never do.
            code, output = self.git('diff', '--cached', '--check', self.target.head or EMPTY_TREE,
                                    index=self.target.index, check=False)
            if code != 0:
                raise Stop(f'{self.name}: whitespace / line-ending errors: ' + '; '.join(output.splitlines()[:5]))
        planned = {path for group in self.target.groups for path in group}
        leftovers = [path for path in coverage(self.files, self.changed)[1] if path not in planned]
        notes = wide_entries(self.files, self.changed) + [f'stays uncommitted: {path}' for path in leftovers]
        if not self.remote_branch:
            notes.insert(0, f'origin has no {self.branch} yet: this push creates it')
        if ahead:
            notes.insert(0, f'{ahead} earlier local commit(s) are verified and pushed too')
        self.run.report('preflight', 'PASS', f'{self.name}: {len(self.commits)} commit(s), {len(planned)} '
                        f'changed path(s) planned, {len(leftovers)} left out', notes)

    def export(self, tree, destination):
        """Write `tree` out as files under `destination`."""
        index = self.scratch('export')
        index.unlink(missing_ok=True)
        self.git('read-tree', tree, index=index)
        self.git('checkout-index', '-a', '-f', f'--prefix={Path(destination).as_posix()}/', index=index)

    def copy_ci_jars(self, tree, export):
        """Give an export the jars its workflow downloads, from this checkout's copy of them."""
        jar_dir = self.project.ci_jar_dir
        for jar in ci_jars(self.show(tree, WORKFLOW.as_posix())) if jar_dir else []:
            source = self.root / jar_dir / jar
            if not source.exists():
                raise Stop(f'{self.name}: CI downloads {jar}, but {jar_dir} has no local copy to mirror it')
            (export / jar_dir).mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, export / jar_dir / jar)

    def build_commits(self):
        """Create the planned commits beside the branch, each on the one before; returns the last (HEAD itself
        when the plan is empty). The branch does not move."""
        signed = self.git('config', '--type=bool', 'commit.gpgsign', check=False)[1].strip() == 'true'
        parent, shas = self.target.head, []
        for commit, tree in zip(self.commits, self.target.trees):
            message = ['-m', commit['title']] + (['-m', commit['body']] if commit['body'] else [])
            parent = self.git('commit-tree', *(['-S'] if signed else []), tree,
                              *(['-p', parent] if parent else []), *message)[1].strip()
            shas.append(parent)
        self.shas = shas
        self.tip = parent
        return parent

    def verify_contributors(self):
        """Refuse the push unless every commit it adds carries the git config identity alone."""
        identity = tuple(self.git('config', key, check=False)[1].strip() for key in ('user.name', 'user.email'))
        if not all(identity):
            raise Stop(f'{self.name}: git config user.name / user.email is not set; nothing can be checked')
        records = parse_log(self.git('log', f'--format={LOG_FORMAT}', *self.unpushed_range(self.tip))[1])
        problems = contributor_problems(records, identity)
        if problems:
            self.run.report('contrib', 'FAIL', f'{self.name}: an AI would show up as a contributor', problems)
            raise Stop(f'{self.name}: contributor gate failed; nothing was committed or pushed. Fix the plan, or '
                       'the hand-made commits it names, then re-run (python -B tools/commit_push.py --push-only'
                       f'{project_flags(self.project)} for commits already made)')
        self.run.report('contrib', 'PASS', f'{self.name}: {len(records)} unpushed commit(s) carry {identity[0]} '
                        f'<{identity[1]}> alone, no co-author or generated-with lines')

    def move_branch(self):
        """Point the branch at the new commits in one step, refusing if it moved since they were derived, then
        bring the index entries of the committed paths up to the new HEAD: the work tree already holds them."""
        if self.tip == self.target.head:
            return
        code, _ = self.git('update-ref', '-m', f'commit_push: {len(self.shas)} commit(s)', 'HEAD', self.tip,
                           self.target.head or ZERO_SHA, check=False)
        if code != 0:
            raise Stop(f'{self.name}: HEAD moved while its commits were landing (a hand-made commit?); nothing '
                       f'of {self.name} was committed, re-run commit_push')
        base = self.git('rev-parse', f'{self.target.head}^{{tree}}')[1].strip() if self.target.head else EMPTY_TREE
        pathspec = self.scratch('pathspec')
        pathspec.write_bytes(b'\0'.join((':(literal)' + path).encode('utf-8')
                                        for path in self.names(base, self.target.tree)))
        for attempt in range(10):
            # IDEA's background git holds index.lock for moments at a time.
            if self.git('reset', '--quiet', f'--pathspec-from-file={pathspec}', '--pathspec-file-nul',
                        check=False)[0] == 0:
                break
            time.sleep(0.5)
        else:
            self.run.report('commit', 'NOTE', f'{self.name}: the index could not be refreshed (index.lock held); '
                            'run `git reset --quiet` once it is free')
        for sha, commit, paths in zip(self.shas, self.commits, self.target.groups):
            self.run.report('commit', 'PASS', f'{self.name}: {sha[:7]} {commit["title"]}', [f'{len(paths)} path(s)'])

    def push(self):
        code, output = self.git('push', *([] if self.remote_branch else ['--set-upstream']), 'origin',
                                self.branch, check=False)
        if code != 0:
            raise Stop(f'{self.name}: git push failed; the commits are local, NOT pushed. Fix the cause, then run '
                       f'python -B tools/commit_push.py --push-only{project_flags(self.project)}')
        self.pushed = True
        self.run.report('push', 'PASS', f'{self.name}: origin/{self.branch} -> {self.tip[:7]}'
                        + ('' if self.remote_branch else ' (created)'))


class Shipper:
    """One run: the repositories a plan touches, in ship order, taken together through preflight, verify,
    landing and the wait for CI."""

    def __init__(self, plan, log, dry_run, poll, poll_timeout, echo=print, push_only=False, project=None,
                 full=False, ship_root=None, plan_dir=None, projects=None):
        default = project or env.PROJECTS['guzhenren']
        known = {**(projects or env.ship_projects()), default.name: default}
        plan_dir = Path(plan_dir or '.')
        groups = {default.name: []} if push_only else {}
        for commit in plan['commits']:
            groups.setdefault(commit.get('project', default.name), []).append({
                'title': commit['title'], 'body': commit.get('body', '').strip(),
                'files': [normalize(path) for path in commit.get('files', [])],
                'patches': [plan_dir / patch for patch in commit.get('patches', [])],
                'contents': {normalize(path): plan_dir / source
                             for path, source in commit.get('contents', {}).items()}})
        order = list(plan.get('order') or []) + list(SHIP_ORDER)
        names = sorted(groups, key=lambda name: order.index(name) if name in order else len(order))
        self.log = log
        self.dry_run = dry_run
        self.push_only = push_only
        self.poll = poll
        self.poll_timeout = poll_timeout
        self.echo = echo
        self.full = full
        self.project = default
        shared = any(default is owner for owner in env.ship_projects().values())
        self.ship_root = Path(ship_root or (env.TEMP_ROOT if shared else default.temp) / 'ship')
        self.work = self.ship_root / f'run-{os.getpid()}'
        self.lines = []
        self.repos = [Repo(self, known[name], groups[name]) for name in names]
        self._wiki_state = None

    # The first repository answers for a one-repository run.
    @property
    def root(self):
        return self.repos[0].root

    @property
    def branch(self):
        return self.repos[0].branch

    @property
    def remote_branch(self):
        return self.repos[0].remote_branch

    def unpushed_range(self, tip='HEAD'):
        return self.repos[0].unpushed_range(tip)

    def missing_jars(self):
        return self.repos[0].missing_jars()

    def report(self, step, status, detail, notes=()):
        line = f'{step:<9} {status:<5} {detail}'
        self.lines.append(line)
        self.lines.extend(f'           {note}' for note in notes)
        self.log.write(line)
        for note in notes:
            self.log.write(f'           {note}')
        self.echo(line, flush=True)
        for note in notes:
            self.echo(f'           {note}', flush=True)

    def git(self, root, *arguments, index=None, check=True):
        git_env = dict(os.environ, GIT_INDEX_FILE=str(index)) if index else None
        code, stdout, stderr = env.git(*arguments, cwd=root, env=git_env, check=False)
        self.log.write(f'$ git {" ".join(arguments)}   [{Path(root).name}' + (f', index {index.name}' if index else '')
                       + f'] -> {code}')
        for text in (stdout, stderr):
            # -z output separates its records with NUL bytes, which would make the log a binary file.
            for line in text.replace('\0', '\n').splitlines()[:200]:
                self.log.write(f'  {line}')
        if check and code != 0:
            last = (stderr or stdout).strip().splitlines()
            raise Stop(f'{Path(root).name}: git {arguments[0]} failed (exit {code}): '
                       f'{last[-1] if last else "no output"}')
        return code, stdout

    def prune(self):
        """Drop what ended runs left under the ship folder once it is a day old: their scratch folders, the
        exports a failed verification kept, verification records, partner exports."""
        cutoff = time.time() - STALE_SECONDS

        def stale(path):
            try:
                return path.stat().st_mtime < cutoff
            except OSError:
                return False

        for path in list(self.ship_root.glob('run-*')) + list(self.ship_root.glob('failed-*')):
            pid = path.name.rsplit('-', 1)[-1]
            if pid.isdigit() and not env.pid_alive(int(pid)) and stale(path):
                env.remove_tree(path)
        for path in (self.ship_root / 'verified').glob('*.json'):
            if stale(path):
                path.unlink(missing_ok=True)
        for marker in (self.ship_root / 'partner').glob('*/built'):
            if stale(marker):
                env.remove_tree(marker.parent)
        for lock in (self.ship_root / 'partner').glob('*.lock'):
            # A lock whose export is gone guards nothing; one a run still holds refuses to go.
            if not lock.with_suffix('').exists() and stale(lock):
                try:
                    lock.unlink()
                    env.remove_tree(lock.with_name(lock.name + '.queue'))
                except OSError:
                    pass

    # ----- verify --------------------------------------------------------------------------------

    def partner(self, repo):
        """The other mod, when the stages of `repo` load it."""
        project = repo.project
        loads = set(project.ci_stages + project.local_stages) & set(project.partner_stages)
        return env.partner_of(project) if project.partner and loads else None

    def partner_tree(self, repo):
        """The partner's tree `repo` is verified against: the one this run plans for it, else its HEAD."""
        partner = self.partner(repo)
        planned = next((other for other in self.repos if other.project is partner), None)
        if planned:
            return planned.target.tree
        code, tree = self.git(partner.root, 'rev-parse', '--verify', '--quiet', 'HEAD^{tree}', check=False)
        if code != 0:
            raise Stop(f'{repo.name}: {project_name(partner)} at {partner.root} has no commit to build')
        return tree.strip()

    def wiki_state(self):
        """A tree id for the wiki as it stands, uncommitted edits included: the wiki gate reads the live pages."""
        if self._wiki_state is None:
            index = self.work / 'wiki-state.index'
            index.unlink(missing_ok=True)
            steps = (('read-tree', 'HEAD'), ('add', '-A'), ('write-tree',))
            results = [self.git(env.WIKI_ROOT, *step, index=index, check=False) for step in steps]
            ok = all(code == 0 for code, _ in results)
            # A wiki git cannot read is never the same twice, so nothing verified with it is reused.
            self._wiki_state = results[-1][1].strip() if ok else secrets.token_hex(8)
        return self._wiki_state

    def seal(self, repo):
        """What a verification of `repo` stands for: its tree and, when its stages load the partner, the
        partner's tree. Landing demands the same seal again."""
        return (repo.target.tree, self.partner_tree(repo) if 'gradle' in repo.kinds and self.partner(repo) else '')

    def record(self, repo):
        """Where a passed verification of `repo` is remembered: one file per seal, stage set and wiki state."""
        parts = [CACHE_VERSION, repo.name, *self.seal(repo), *repo.kinds]
        if 'gradle' in repo.kinds and repo.project.wiki_gate and env.wiki_available():
            parts.append(self.wiki_state())
        key = hashlib.sha256('\n'.join(parts).encode('utf-8')).hexdigest()[:32]
        return self.ship_root / 'verified' / f'{key}.json'

    def verify(self):
        self.log.section('verify')
        pending = []
        for repo in self.repos:
            repo.kinds = needs(repo.project, repo.names(repo.pushed_tree(), repo.target.tree), self.full)
            repo.seal = self.seal(repo)
            tree = repo.target.tree[:10]
            if not repo.kinds:
                repo.verified = repo.seal
                self.report('verify', 'PASS', f'{repo.name}: no check reads what this change touches; '
                            f'tree {tree} ships as it is')
                continue
            record = self.record(repo)
            if record.is_file() and record.stat().st_mtime > time.time() - STALE_SECONDS:
                repo.verified = repo.seal
                earlier = json.loads(record.read_text(encoding='utf-8'))
                self.report('verify', 'PASS', f'{repo.name}: the exact tree {tree} already passed '
                            f'{", ".join(repo.kinds)} at {earlier.get("at", "?")}', earlier.get('notes', []))
                continue
            pending.append(repo)
        if pending:
            with env.Slot(self.ship_root, wait=env.LOCK_WAIT,
                          echo=lambda line, **options: self.echo(line, **options)) as slot:
                for repo in pending:
                    self.verify_in(slot.directory, repo)

    def verify_in(self, slot, repo):
        project, tree = repo.project, repo.target.tree
        export = slot / project.root.name
        games = slot / project.name
        if not env.remove_tree(export):
            raise Stop(f'{repo.name}: cannot clear {export}; a process still holds a file there')
        repo.export(tree, export)
        notes, failed = [], []

        def judge(label, results):
            for name, result in results.items():
                notes.append(f'{label + ":":<7}{check.summary_line(name, result)}')
                if result.status != 'PASS':
                    failed.append(f'{label} {name}')
                    notes.extend(f'           {note}' for note in result.notes)

        def stages(label, names, **options):
            judge(label, check.run_stages(export, list(names), self.log, echo=lambda *a, **k: None, project=project,
                                          game_root=games, **options))

        if 'links' in repo.kinds:
            judge('wiki', {'links': self.links(export)})
        if 'tools' in repo.kinds and not failed:
            stages('tools', ['tools'])
        if 'gradle' in repo.kinds and not failed:
            repo.copy_ci_jars(tree, export)
            partner = {'partner_root': self.partner_export(repo), 'partner_state': {'problem': ''}} \
                if self.partner(repo) else {}
            # Pass one is exactly what GitHub runs on this tree. Pass two adds the stages CI cannot run
            # on the same tree: the wiki gate (the wiki is private) and the GameTests, which need the
            # partner mod.
            if project.ci_stages:
                stages('ci', project.ci_stages, **partner)
            if project.local_stages and not failed:
                if (export / 'src' / 'test').is_dir():
                    wiki = env.WIKI_ROOT if project.wiki_gate and env.wiki_available() else None
                    stages('tests', project.local_stages, wiki=wiki, **partner)
                else:
                    notes.append('tests: skipped, the planned tree has no src/test')
        if failed:
            kept = self.ship_root / f'failed-{os.getpid()}' / export.name
            env.remove_tree(kept)
            kept.parent.mkdir(parents=True, exist_ok=True)
            try:
                export.rename(kept)
            except OSError:
                kept = export
            self.report('verify', 'FAIL', f'{repo.name}: failed on the exported tree: {", ".join(failed)} '
                        f'(export kept at {kept})', notes)
            raise Stop(f'{repo.name}: verification failed')
        env.remove_tree(export)
        record = self.record(repo)
        record.parent.mkdir(parents=True, exist_ok=True)
        record.write_text(json.dumps({'project': repo.name, 'tree': tree, 'kinds': repo.kinds, 'notes': notes,
                                      'at': time.strftime('%Y-%m-%d %H:%M:%S')}), encoding='utf-8')
        repo.verified = repo.seal
        self.report('verify', 'PASS', f'{repo.name}: {", ".join(repo.kinds)} green on the exact tree {tree[:10]}',
                    notes)

    def links(self, export):
        """The wiki's link check, run on the exported pages instead of the live ones."""
        started = time.monotonic()
        command = [sys.executable, '-B', str(env.REPOSITORY_ROOT / 'tools' / 'doc_conventions.py'), 'links']
        self.log.write(f'$ {" ".join(command)}   (wiki at {export})')
        problems = []
        with env.Process(command, env.REPOSITORY_ROOT, self.log, env={'GZR_WIKI': str(export)}) as process:
            deadline = started + 600
            while True:
                outcome, line = process.watch(until=lambda _: True, timeout=max(0, deadline - time.monotonic()))
                if outcome != 'until':
                    break
                problems.append(line)
            code = process.proc.returncode if outcome == 'exit' else None
            if code is None:
                process.kill_tree()
        seconds = time.monotonic() - started
        if code == 0:
            return check.Result('PASS', seconds, 'every wiki link resolves')
        return check.Result('FAIL', seconds, 'timed out' if code is None else f'{len(problems)} broken link(s)',
                            problems[:15])

    def partner_export(self, repo):
        """The partner mod, exported from its own tree with its jar built, kept under ship/partner by tree id:
        every run that loads the same partner tree shares one build, and the partner's checkout is never
        built in or read from, so another session's unfinished work there cannot reach this verification."""
        partner, tree = self.partner(repo), self.partner_tree(repo)
        home = self.ship_root / 'partner' / f'{partner.name}-{tree[:16]}'
        export = home / partner.root.name
        with PartnerLock(home.with_name(home.name + '.lock'), wait=env.LOCK_WAIT,
                         echo=lambda line, **options: self.echo(line, **options)):
            marker = home / 'built'
            if marker.is_file():
                os.utime(marker)
                return export
            self.log.section(f'partner {partner.name} {tree[:10]}')
            if not env.remove_tree(home):
                raise Stop(f'{repo.name}: cannot clear {home}; a process still holds a file there')
            source = Repo(self, partner, [])
            index = self.work / f'{partner.name}.partner'
            index.unlink(missing_ok=True)
            self.git(partner.root, 'read-tree', tree, index=index)
            self.git(partner.root, 'checkout-index', '-a', '-f', f'--prefix={export.as_posix()}/', index=index)
            source.copy_ci_jars(tree, export)
            context = check.Context(export, self.log, project=partner, game_root=home / 'games')
            code, _, collector = check.gradle(context, 'assemble')
            if code != 0:
                raise Stop(f'{repo.name}: {project_name(partner)} build failed (exit {code}): '
                           + '; '.join(check.went_wrong(collector, 2)))
            marker.write_text(tree, encoding='utf-8')
        return export

    # ----- land ----------------------------------------------------------------------------------

    def land(self):
        """Commit and push every repository, or nothing: False when a tree is no longer the verified one."""
        self.log.section('land')
        with LandLock(self.ship_root / 'land.lock', wait=env.LOCK_WAIT,
                      echo=lambda line, **options: self.echo(line, **options)):
            for repo in self.repos:
                repo.target = repo.derive()
                if self.seal(repo) != repo.verified:
                    self.report('land', 'RETRY', f'{repo.name}: main, the partner or a planned file moved since '
                                'verification; verifying the new tree')
                    return False
            for repo in self.repos:
                repo.build_commits()
                self.log.section(f'contributors {repo.name}')
                repo.verify_contributors()
            for repo in self.repos:
                self.log.section(f'commit and push {repo.name}')
                try:
                    repo.move_branch()
                    repo.push()
                except Stop as reason:
                    shipped = [other.name for other in self.repos if other.pushed]
                    raise Stop(f'{reason}' + (f'. Already pushed: {", ".join(shipped)}; drop their commits from '
                                              'the plan before re-running' if shipped else '')) from None
        return True

    def wait_for_ci(self):
        ok = True
        for repo in self.repos:
            if not (repo.root / WORKFLOW).is_file():
                continue
            self.log.section(f'ci {repo.name}')
            head = repo.tip
            conclusion, url = ci_run(repo.root, head, self.poll_timeout,
                                     echo=lambda line: self.log.write(f'[poll] {line}'))
            if conclusion == 'success':
                self.report('ci', 'PASS', f'{repo.name}: GitHub Actions green on {head[:7]}', [url] if url else [])
                continue
            ok = False
            state = 'unknown (timed out or API unreachable)' if conclusion is None else conclusion
            self.report('ci', 'FAIL', f'{repo.name}: GitHub Actions {state} on {head[:7]}',
                        ([url] if url else []) + [f're-check: python -B tools/commit_push.py --ci {head[:7]}'
                                                  + project_flags(repo.project)])
        return ok

    def run(self):
        try:
            self.work.mkdir(parents=True, exist_ok=True)
            self.prune()
            for attempt in range(ROUNDS):
                self.log.section('preflight')
                for repo in self.repos:
                    repo.prepare()
                self.verify()
                if self.dry_run:
                    self.report('dry-run', 'PASS', 'ready to ship; nothing was committed or pushed')
                    return True
                if self.land():
                    break
            else:
                raise Stop(f'the planned trees changed {ROUNDS} times between verification and landing; '
                           're-run once the other sessions and the live edits settle')
            return not self.poll or self.wait_for_ci()
        except Stop as reason:
            self.report('stopped', 'FAIL', str(reason))
            return False
        finally:
            env.remove_tree(self.work)


def project_name(project):
    return {'guzhenren': 'Guzhenren', 'guworld': 'Gu World'}.get(project.name, project.name)


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('plan', nargs='?', help='JSON plan: {"commits": [{"title": ..., "files": [...]}]}')
    parser.add_argument('--dry-run', action='store_true', help='preflight and verify only')
    parser.add_argument('--push-only', action='store_true',
                        help='no plan: verify the commits already made, run the contributor gate, push, wait for CI')
    parser.add_argument('--ci', nargs='?', const='HEAD', metavar='SHA',
                        help='no plan: wait for the GitHub Actions run of SHA (default HEAD) and report it')
    parser.add_argument('--full', action='store_true', help='verify with every stage, whatever the change touches')
    parser.add_argument('--no-poll', action='store_true', help='push without waiting for GitHub Actions')
    parser.add_argument('--poll-timeout', type=int, default=900, help='seconds to wait for CI (default 900)')
    parser.add_argument('--project', choices=sorted(env.ship_projects()), default='guzhenren',
                        help='the repository of commits that name none, of --push-only and of --ci '
                             '(default guzhenren)')
    args = parser.parse_args(argv)
    if (args.push_only or args.ci) and (args.plan or args.dry_run):
        parser.error('--push-only and --ci take no plan and no --dry-run')
    if args.push_only and args.ci:
        parser.error('--push-only already waits for CI')
    if not (args.push_only or args.ci) and not args.plan:
        parser.error('a plan is required unless --push-only or --ci')
    env.require_windows()
    project = env.ship_projects()[args.project]
    if args.ci:
        if not (project.root / WORKFLOW).is_file():
            print(f'RESULT: {project.name} has no GitHub Actions workflow')
            return 1
        sha = env.git('rev-parse', args.ci, cwd=project.root)[1].strip()
        conclusion, url = ci_run(project.root, sha, args.poll_timeout, echo=lambda line: print(line, flush=True))
        print(f'RESULT: {conclusion or "unknown (timed out or API unreachable)"}' + (f'   {url}' if url else ''))
        return 0 if conclusion == 'success' else 1
    if args.push_only:
        plan = {'commits': []}
    else:
        plan = json.loads(Path(args.plan).read_text(encoding='utf-8'))
        errors = validate_plan(plan, default=args.project)
        if errors:
            for error in errors:
                print(error)
            return 2
    home = project if project.name in env.PROJECTS else env.PROJECTS['guzhenren']
    log = env.Log('commit_push', log_dir=home.temp / 'logs', per_run=True)
    try:
        shipper = Shipper(plan, log, args.dry_run, not args.no_poll, args.poll_timeout, push_only=args.push_only,
                          project=project, full=args.full, plan_dir=Path(args.plan).parent if args.plan else None)
        mode = ' (dry run)' if args.dry_run else ' (push only)' if args.push_only else ''
        print(f'commit_push: {", ".join(repo.name for repo in shipper.repos)}{mode}   log: {log.path}', flush=True)
        ok = shipper.run()
        verdict = 'PASS' if ok else 'FAIL'
        log.section('summary')
        log.write(f'RESULT: {verdict}')
    finally:
        log.close()
    print(f'RESULT: {verdict}   (read {log.path}, then delete it)')
    return 0 if ok else 1


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
