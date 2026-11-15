"""One-command commit and push: verify the exact tree CI will see, commit each group, push, wait for CI.

    python -B tools/commit_push.py PLAN.json             preflight, CI mirror, commit, contributor gate, push, wait for CI
    python -B tools/commit_push.py PLAN.json --dry-run   preflight and CI mirror only
    python -B tools/commit_push.py --push-only           contributor gate, push, wait for CI (after fixing a failed gate)
    python -B tools/commit_push.py PLAN.json --project guworld    the same for the sibling Gu World repository
    python -B tools/commit_push.py --ci [SHA]           wait for the GitHub Actions run of SHA (default HEAD)

PLAN.json lists the commits in order, with their Conventional Commits titles and an optional body
(which may end in a BREAKING CHANGE footer):

    {"commits": [{"title": "fix(aperture): ...", "body": "...", "files": ["src/main/java/...", "src/generated/.../folder/"]}]}

A path ending in / takes everything under it, deletions included. The CI mirror exports exactly what
the commits will contain into <Temp>/<project>/commit_push/ and adds the jars CI downloads. It first runs what
GitHub runs (build, runData drift), so a forgotten file or an undeclared run/mods jar fails here instead
of on GitHub; then it runs the local stages (JUnit with the wiki gate, GameTests) against that same
tree. Before pushing, the contributor gate reads every commit in
origin/<branch>..HEAD and refuses the push when an author or committer differs from the git config
identity or looks like an AI, or when a message carries a Co-Authored-By or generated-with line: GitHub
would list that AI as a contributor. The commits stay local; fix them, then run --push-only.
Everything goes to <Temp>/<project>/logs/commit_push-<pid>.log, one per run. Exit 0 = shipped and CI green
(with --dry-run: ready to ship).

Each project's mirror runs what its own GitHub workflow runs plus its local stages
(project_runtime.PROJECTS). Each project ships its main checkout to origin/main. A
repository with no commit yet, or a remote without main yet, ships too: the scratch index then
starts from the empty tree, the gate reads every local commit no origin branch holds, and the first
push sets the upstream.
"""
import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import time
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


class Stop(Exception):
    """A failed step: the message is the reason shown to the caller."""


def normalize(path):
    path = path.strip()
    while path.startswith('./'):
        path = path[2:]
    return path


def validate_plan(plan):
    """Problems with the plan's shape, titles and paths; empty when the plan is usable."""
    commits = plan.get('commits') if isinstance(plan, dict) else None
    if not isinstance(commits, list) or not commits:
        return ['plan needs a non-empty "commits" list']
    errors = []
    seen = {}
    for number, commit in enumerate(commits, 1):
        title = commit.get('title') if isinstance(commit, dict) else None
        body = commit.get('body', '') if isinstance(commit, dict) else ''
        files = commit.get('files') if isinstance(commit, dict) else None
        if not isinstance(title, str) or '\n' in title or not TITLE.match(title):
            errors.append(f'commit {number}: title must look like "<type>[(scope)][!]: <description>" '
                          f'(types {", ".join(TYPES)}), got {title!r}')
        elif not isinstance(body, str):
            errors.append(f'commit {number}: "body" must be a string')
        elif FOOTER.search(title + '\n' + body):
            errors.append(f'commit {number}: no attribution footers in titles or bodies')
        if not isinstance(files, list) or not files or not all(isinstance(f, str) and f.strip() for f in files):
            errors.append(f'commit {number}: "files" must be a non-empty list of paths')
            continue
        for raw in files:
            path = normalize(raw)
            if path.startswith('/') or '..' in path.split('/'):
                errors.append(f'commit {number}: {raw!r} must be relative to the repository root')
            elif path in seen:
                errors.append(f'commit {number}: {path} is already in commit {seen[path]}')
            else:
                # A folder in one group would swallow a file listed under it in another group.
                overlap = next((other for other in seen if seen[other] != number
                                and (covers(other, path) or covers(path, other))), None)
                if overlap:
                    errors.append(f'commit {number}: {path} overlaps {overlap} in commit {seen[overlap]}')
                seen[path] = number
    return errors


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


def fetch_runs(owner, repo, per_page=5, get=api_get):
    # Anonymous calls share 60 per hour, which one busy session exhausts; the token Alex keeps for
    # the github MCP lifts that to 5000. A rejected token falls back to anonymous.
    api = f'https://api.github.com/repos/{owner}/{repo}/actions/runs?per_page={per_page}'
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


def ci_run(root, sha, timeout, echo=print, fetch=fetch_runs):
    """(conclusion or None, run URL) of the Actions run of `sha` in the repository at `root`."""
    owner, repo = parse_remote(env.git('remote', 'get-url', 'origin', cwd=root)[1])
    conclusion = poll(owner, repo, sha, timeout=timeout, interval=30, fetch=fetch, echo=echo)
    try:
        runs = [run for run in fetch(owner, repo, per_page=10) if (run.get('head_sha') or '').startswith(sha)]
    except RuntimeError:
        runs = []
    return conclusion, runs[0].get('html_url', '') if runs else ''


def project_flags(project):
    """The command-line flags that select `project`, for a hint the caller can paste."""
    return f' --project {project.name}' if project.name != 'guzhenren' else ''


class Shipper:
    def __init__(self, plan, log, dry_run, poll, poll_timeout, echo=print, push_only=False, project=None):
        self.commits = [{'title': c['title'], 'body': c.get('body', '').strip(),
                         'files': [normalize(f) for f in c['files']]}
                        for c in plan['commits']]
        self.files = [path for commit in self.commits for path in commit['files']]
        self.log = log
        self.dry_run = dry_run
        self.push_only = push_only
        self.poll = poll
        self.poll_timeout = poll_timeout
        self.echo = echo
        self.project = project or env.PROJECTS['guzhenren']
        self.root = self.project.root
        self.work = self.project.temp / 'commit_push'
        self.lines = []
        self.changed = []
        self.branch = BRANCH
        self.remote_branch = True

    def project_flags(self):
        return project_flags(self.project)

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

    def git(self, *arguments, index=None, check=True):
        git_env = dict(os.environ, GIT_INDEX_FILE=str(index)) if index else None
        code, stdout, stderr = env.git(*arguments, cwd=self.root, env=git_env, check=False)
        self.log.write(f'$ git {" ".join(arguments)}' + (f'   [index {index.name}]' if index else '')
                       + f' -> {code}')
        for text in (stdout, stderr):
            for line in text.splitlines()[:200]:
                self.log.write(f'  {line}')
        if check and code != 0:
            last = (stderr or stdout).strip().splitlines()
            raise Stop(f'git {arguments[0]} failed (exit {code}): {last[-1] if last else "no output"}')
        return code, stdout

    def require_branch(self):
        # symbolic-ref, not rev-parse: it also names the branch of a repository with no commit yet.
        branch = self.git('symbolic-ref', '--short', 'HEAD', check=False)[1].strip()
        if branch != self.branch:
            raise Stop(f'on branch {branch or "(detached HEAD)"}, not {self.branch}')

    def has_head(self):
        return self.git('rev-parse', '--verify', '--quiet', 'HEAD', check=False)[0] == 0

    def sync_remote(self):
        """Fetch origin/<branch> and return (behind, ahead); a remote without the branch is behind by nothing."""
        branch = self.branch
        self.remote_branch = bool(self.git('ls-remote', '--heads', 'origin', branch)[1].strip())
        if not self.remote_branch:
            ahead = len(self.unpushed_shas()) if self.has_head() else 0
            return 0, ahead
        self.git('fetch', '--quiet', 'origin', branch)
        if not self.has_head():
            raise Stop(f'origin/{branch} exists but this repository has no commit yet; clone or pull first')
        behind = int(self.git('rev-list', '--count', f'HEAD..origin/{branch}')[1].strip())
        ahead = int(self.git('rev-list', '--count', f'origin/{branch}..HEAD')[1].strip())
        return behind, ahead

    def unpushed_range(self):
        """git log arguments for the commits this push adds: past origin/<branch>, or, for a branch the
        remote lacks, every commit no origin branch already holds."""
        return [f'origin/{self.branch}..HEAD'] if self.remote_branch else ['HEAD', '--not', '--remotes=origin']

    def unpushed_shas(self):
        return [line for line in self.git('rev-list', *self.unpushed_range())[1].split() if line]

    def preflight(self):
        self.log.section('preflight')
        self.require_branch()
        if self.git('diff', '--cached', '--quiet', check=False)[0] != 0:
            raise Stop('the index already has staged changes; unstage them first (git restore --staged .)')
        changed = parse_porcelain(self.git('status', '--porcelain=v1', '-z', '--untracked-files=all')[1])
        self.changed = changed
        idle, leftovers = coverage(self.files, changed)
        if idle:
            raise Stop('plan paths without any change: ' + ', '.join(idle))
        missing = self.missing_jars()
        if missing:
            raise Stop(f'the build uses {self.project.ci_jar_dir} jars CI never downloads: ' + ', '.join(missing))
        behind, ahead = self.sync_remote()
        if behind:
            raise Stop(f'origin/{self.branch} is {behind} commit(s) ahead; pull first')
        notes = [f'stays uncommitted: {path}' for path in leftovers]
        if not self.remote_branch:
            notes.insert(0, f'origin has no {self.branch} yet: this push creates it')
        if ahead:
            notes.insert(0, f'{ahead} earlier local commit(s) will be pushed too')
        self.report('preflight', 'PASS', f'{len(self.commits)} commit(s), {len(changed) - len(leftovers)} '
                    f'changed path(s) planned, {len(leftovers)} left out', notes)

    def missing_jars(self):
        if self.project.ci_jar_dir is None:
            return []
        workflow = (self.root / WORKFLOW).read_text(encoding='utf-8')
        return missing_ci_jars((self.root / 'build.gradle').read_text(encoding='utf-8'), workflow)

    def expand(self, entries):
        """The changed paths a set of plan entries takes, as git status listed them."""
        return [path for path in self.changed if any(covers(entry, path) for entry in entries)]

    def stage(self, paths, index=None):
        # Exact paths from git status, through a pathspec file so a long list cannot overflow the
        # shell command line. --force: a tracked file deleted under a now-ignored folder is still
        # refused as "ignored" otherwise. It cannot drag in ignored
        # files, because git status never lists an ignored untracked path.
        pathspec = self.work / 'pathspec'
        pathspec.write_bytes(b'\0'.join((':(literal)' + path).encode('utf-8') for path in paths))
        self.git('add', '-A', '--force', f'--pathspec-from-file={pathspec}', '--pathspec-file-nul', index=index)

    def stage_tree(self, name):
        """Stage every plan path into a scratch index on top of HEAD; returns (index, tree id)."""
        index = self.work / name
        self.git('read-tree', *(['HEAD'] if self.has_head() else ['--empty']), index=index)
        self.stage(self.expand(self.files), index=index)
        return index, self.git('write-tree', index=index)[1].strip()

    def mirror(self):
        self.log.section('ci mirror')
        env.remove_tree(self.work)
        self.work.mkdir(parents=True)
        index, tree = self.stage_tree('index')
        base = 'HEAD' if self.has_head() else EMPTY_TREE
        code, output = self.git('diff', '--cached', '--check', base, index=index, check=False)
        if code != 0:
            raise Stop('whitespace / line-ending errors: ' + '; '.join(output.splitlines()[:5]))
        export = self.work / self.root.name
        self.git('checkout-index', '-a', '-f', f'--prefix={export.as_posix()}/', index=index)
        workflow = (self.root / WORKFLOW).read_text(encoding='utf-8')
        # A workflow that downloads into the partner's checkout needs no copy: the mirror runs with the partner.
        jar_dir = self.project.ci_jar_dir
        for jar in ci_jars(workflow) if jar_dir else []:
            source = self.root / jar_dir / jar
            if not source.exists():
                raise Stop(f'CI downloads {jar}, but {jar_dir} has no local copy to mirror it')
            (export / jar_dir).mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, export / jar_dir / jar)
        # Pass one is exactly what GitHub runs on this tree. Pass two adds the stages CI cannot run
        # on the same tree: the wiki gate (the wiki is private) and the GameTests, which need the
        # partner mod. The partner's jar is built once for both.
        notes, failed, partner_state = [], [], {}
        for label, stages in (('ci', self.project.ci_stages), ('tests', self.project.local_stages)):
            if not stages:
                continue
            wiki = None
            if label == 'tests':
                if not (export / 'src' / 'test').is_dir():
                    notes.append('tests: skipped, the planned tree has no src/test')
                    break
                wiki = env.WIKI_ROOT if self.project.wiki_gate and env.wiki_available() else None
            results = check.run_stages(export, list(stages), self.log, wiki=wiki, echo=lambda *a, **k: None,
                                       project=self.project, partner_state=partner_state)
            for name, result in results.items():
                notes.append(f'{label + ":":<7}{check.summary_line(name, result)}')
                if result.status != 'PASS':
                    failed.append(f'{label} {name}')
                    notes.extend(f'           {note}' for note in result.notes)
            if failed:
                break
        if failed:
            self.report('mirror', 'FAIL', f'failed on the exported tree: {", ".join(failed)} '
                        f'(export kept at {export})', notes)
            raise Stop('CI mirror failed')
        env.remove_tree(export)
        self.report('mirror', 'PASS', f'CI chain and local tests green on the exact tree {tree[:10]}', notes)
        return tree

    def commit(self, tree):
        self.log.section('commit')
        if self.stage_tree('recheck')[1] != tree:
            raise Stop('a planned file changed after verification (live edit?); re-run commit_push')
        # Settle every group before the first commit, so a bad plan never leaves half of it committed.
        groups = [(commit['title'], commit['body'], self.expand(commit['files'])) for commit in self.commits]
        empty = [title for title, _, paths in groups if not paths]
        if empty:
            raise Stop('groups without changes: ' + '; '.join(empty))
        shas = []
        try:
            for title, body, paths in groups:
                self.stage(paths)
                staged = [p for p in self.git('diff', '--cached', '--name-only', '-z')[1].split('\0') if p]
                strays = sorted(set(staged) - set(paths))
                if strays:
                    raise Stop(f'unexpected staged paths for "{title}": {", ".join(strays)}')
                self.git('commit', '--quiet', '-m', title, *(('-m', body) if body else ()))
                sha = self.git('rev-parse', 'HEAD')[1].strip()
                shas.append(sha)
                self.report('commit', 'PASS', f'{sha[:7]} {title}', [f'{len(staged)} path(s)'])
            if self.git('rev-parse', 'HEAD^{tree}')[1].strip() != tree:
                raise Stop('the committed tree differs from the verified tree; not pushing')
        except Stop as reason:
            # Unstage whatever the failed group left in the index; the working tree stays as is.
            self.git('reset', '--quiet', check=False)
            if shas:
                made = ', '.join(sha[:7] for sha in shas)
                raise Stop(f'{reason}. Already committed locally, NOT pushed: {made}; drop those groups '
                           f'from the plan before re-running, or push them with commit_push.py --push-only') from None
            raise
        return shas

    def push_preflight(self):
        self.log.section('preflight')
        self.require_branch()
        behind, ahead = self.sync_remote()
        if behind:
            raise Stop(f'origin/{self.branch} is {behind} commit(s) ahead; pull first')
        if not ahead:
            raise Stop('no local commits to push')
        self.report('preflight', 'PASS', f'{ahead} local commit(s) to push')
        return self.git('rev-parse', 'HEAD')[1].strip()

    def verify_contributors(self):
        """Refuse the push unless every unpushed commit carries the git config identity alone."""
        self.log.section('contributors')
        identity = tuple(self.git('config', key, check=False)[1].strip() for key in ('user.name', 'user.email'))
        if not all(identity):
            raise Stop('git config user.name / user.email is not set; nothing can be checked')
        records = parse_log(self.git('log', f'--format={LOG_FORMAT}', *self.unpushed_range())[1])
        problems = contributor_problems(records, identity)
        if problems:
            self.report('contrib', 'FAIL', 'an AI would show up as a contributor', problems)
            raise Stop('contributor gate failed; the commits stay local, NOT pushed. Fix them, then run '
                       'python -B tools/commit_push.py --push-only')
        self.report('contrib', 'PASS', f'{len(records)} unpushed commit(s) carry {identity[0]} <{identity[1]}> '
                    f'alone, no co-author or generated-with lines')

    def push(self, head):
        self.log.section('push')
        self.git('push', *([] if self.remote_branch else ['--set-upstream']), 'origin', self.branch)
        self.report('push', 'PASS', f'origin/{self.branch} -> {head[:7]}' + ('' if self.remote_branch else ' (created)'))

    def wait_for_ci(self, head):
        self.log.section('ci')
        conclusion, url = ci_run(self.root, head, self.poll_timeout, echo=lambda line: self.log.write(f'[poll] {line}'))
        if conclusion == 'success':
            self.report('ci', 'PASS', f'GitHub Actions green on {head[:7]}', [url] if url else [])
            return True
        state = 'unknown (timed out or API unreachable)' if conclusion is None else conclusion
        self.report('ci', 'FAIL', f'GitHub Actions {state} on {head[:7]}',
                    ([url] if url else []) + [f're-check: python -B tools/commit_push.py --ci {head[:7]}'
                                              + self.project_flags()])
        return False

    def run(self):
        try:
            if self.push_only:
                head = self.push_preflight()
                self.verify_contributors()
                self.push(head)
                return not self.poll or self.wait_for_ci(head)
            self.preflight()
            tree = self.mirror()
            if self.dry_run:
                self.report('dry-run', 'PASS', 'ready to ship; nothing was staged, committed or pushed')
            else:
                shas = self.commit(tree)
                self.verify_contributors()
                self.push(shas[-1])
                if self.poll and not self.wait_for_ci(shas[-1]):
                    return False
        except Stop as reason:
            # The scratch index and a failed export stay for inspection; the next run wipes them.
            self.report('stopped', 'FAIL', str(reason))
            return False
        env.remove_tree(self.work)
        return True


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('plan', nargs='?', help='JSON plan: {"commits": [{"title": ..., "files": [...]}]}')
    parser.add_argument('--dry-run', action='store_true', help='preflight and CI mirror only')
    parser.add_argument('--push-only', action='store_true',
                        help='no plan: run the contributor gate on the unpushed commits, push, wait for CI')
    parser.add_argument('--ci', nargs='?', const='HEAD', metavar='SHA',
                        help='no plan: wait for the GitHub Actions run of SHA (default HEAD) and report it')
    parser.add_argument('--no-poll', action='store_true', help='push without waiting for GitHub Actions')
    parser.add_argument('--poll-timeout', type=int, default=900, help='seconds to wait for CI (default 900)')
    parser.add_argument('--project', choices=sorted(env.PROJECTS), default='guzhenren',
                        help='which repository to ship (default guzhenren)')
    args = parser.parse_args(argv)
    if (args.push_only or args.ci) and (args.plan or args.dry_run):
        parser.error('--push-only and --ci take no plan and no --dry-run')
    if args.push_only and args.ci:
        parser.error('--push-only already waits for CI')
    if not (args.push_only or args.ci) and not args.plan:
        parser.error('a plan is required unless --push-only or --ci')
    env.require_windows()
    project = env.project(args.project)
    if args.ci:
        sha = env.git('rev-parse', args.ci, cwd=project.root)[1].strip()
        conclusion, url = ci_run(project.root, sha, args.poll_timeout, echo=lambda line: print(line, flush=True))
        print(f'RESULT: {conclusion or "unknown (timed out or API unreachable)"}' + (f'   {url}' if url else ''))
        return 0 if conclusion == 'success' else 1
    if args.push_only:
        plan = {'commits': []}
    else:
        plan = json.loads(Path(args.plan).read_text(encoding='utf-8'))
        errors = validate_plan(plan)
        if errors:
            for error in errors:
                print(error)
            return 2
    with env.Lock(wait=env.LOCK_WAIT):
        log = env.Log('commit_push', log_dir=project.temp / 'logs', per_run=True)
        try:
            mode = ' (dry run)' if args.dry_run else ' (push only)' if args.push_only else ''
            print(f'{project.name} commit_push{mode}   log: {log.path}', flush=True)
            ok = Shipper(plan, log, args.dry_run, not args.no_poll, args.poll_timeout,
                         push_only=args.push_only, project=project).run()
            verdict = 'PASS' if ok else 'FAIL'
            log.section('summary')
            log.write(f'RESULT: {verdict}')
        finally:
            log.close()
        print(f'RESULT: {verdict}   (read {log.path}, then delete it)')
        return 0 if ok else 1


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
