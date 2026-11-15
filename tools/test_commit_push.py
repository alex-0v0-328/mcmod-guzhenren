import os
import tempfile
import unittest
import unittest.mock
from dataclasses import replace
from pathlib import Path

import project_runtime as env
import commit_push


def plan(*commits):
    return {'commits': [{'title': title, 'files': files} for title, files in commits]}


def first_push_round_trip():
    """A repository with no commit yet and a remote with no main: commit_push commits, gates and pushes the
    first commit, and its gate still refuses a later co-authored commit. Local bare remote, no
    Gradle, no network."""
    env.TEMP_ROOT.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=env.TEMP_ROOT) as scratch:
        scratch = Path(scratch)
        remote, work = scratch / 'remote.git', scratch / 'work'
        env.git('init', '--quiet', '--bare', '--initial-branch=main', str(remote), cwd=scratch)
        env.git('init', '--quiet', '--initial-branch=main', str(work), cwd=scratch)
        for key, value in (('user.name', 'Alex Test'), ('user.email', 'alex@example.com'),
                           ('core.autocrlf', 'false')):
            env.git('config', key, value, cwd=work)
        env.git('remote', 'add', 'origin', remote.as_posix(), cwd=work)
        (work / '.github' / 'workflows').mkdir(parents=True)
        # Raw LF bytes keep git's --check clean whatever core.autocrlf says.
        (work / '.github' / 'workflows' / 'build.yml').write_bytes(b'run: ./gradlew build\n')
        (work / 'build.gradle').write_bytes(b'plugins {}\n')
        (work / 'a.txt').write_bytes(b'one\n')
        (work / 'CLAUDE.md').write_bytes(b'# CLAUDE.md\n\n## Shared\n\nrule\n\n## Claude Code\n\nclaude\n')
        (work / 'AGENTS.md').write_bytes(b'# AGENTS.md\n\n## Shared\n\nrule\n\n## Codex and opencode\n\ncodex\n')
        project = env.Project('scratch', work, scratch / 'temp', (), (), (), False, False)
        quiet = lambda *args, **kwargs: None

        log = env.Log('commit_push', log_dir=scratch / 'logs')
        try:
            first = ['.github/', 'build.gradle', 'a.txt', 'CLAUDE.md', 'AGENTS.md']
            shipper = commit_push.Shipper(plan(('chore: first commit', first)), log,
                                          dry_run=False, poll=False, poll_timeout=0, echo=quiet, project=project)
            assert shipper.run(), '\n'.join(shipper.lines)
        finally:
            log.close()
        assert shipper.remote_branch is False, 'the first push must have seen a remote without main'
        assert shipper.unpushed_range() == ['HEAD', '--not', '--remotes=origin']
        assert env.git('rev-list', '--count', 'main', cwd=remote)[1].strip() == '1'
        assert env.git('rev-parse', '--abbrev-ref', 'main@{upstream}', cwd=work)[1].strip() == 'origin/main'

        (work / 'a.txt').write_bytes(b'two\n')
        env.git('commit', '--quiet', '-am', 'chore: second', '-m',
                'Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>', cwd=work)
        log = env.Log('commit_push', log_dir=scratch / 'logs')
        try:
            gated = commit_push.Shipper(plan(), log, dry_run=False, poll=False, poll_timeout=0, echo=quiet,
                                        push_only=True, project=project)
            assert not gated.run(), 'the gate must refuse a co-authored commit'
        finally:
            log.close()
        assert any('message carries' in line for line in gated.lines), gated.lines
        assert env.git('rev-list', '--count', 'main', cwd=remote)[1].strip() == '1', 'nothing may be pushed'
        assert gated.remote_branch is True and gated.unpushed_range() == ['origin/main..HEAD']


def ship_scratch(plan_commits, work, project, **options):
    quiet = lambda *args, **kwargs: None
    options = {'dry_run': False, 'poll': False, 'poll_timeout': 0, **options}
    log = env.Log('commit_push', log_dir=work.parent / 'logs')
    try:
        shipper = commit_push.Shipper(plan(*plan_commits), log, echo=quiet, project=project, **options)
        shipper.ok = shipper.run()
    finally:
        log.close()
    return shipper


def wrong_branch_and_jar_mirror():
    """A shipper refuses a checkout off main, and a jar the workflow downloads into this checkout
    (ci_jar_dir set) must exist locally to be mirrored. Local bare remote, no Gradle."""
    with tempfile.TemporaryDirectory(dir=env.TEMP_ROOT) as scratch:
        scratch = Path(scratch)
        remote, work = scratch / 'remote.git', scratch / 'work'
        env.git('init', '--quiet', '--bare', '--initial-branch=main', str(remote), cwd=scratch)
        env.git('init', '--quiet', '--initial-branch=main', str(work), cwd=scratch)
        for key, value in (('user.name', 'Alex Test'), ('user.email', 'alex@example.com'),
                           ('core.autocrlf', 'false')):
            env.git('config', key, value, cwd=work)
        env.git('remote', 'add', 'origin', remote.as_posix(), cwd=work)
        (work / '.github' / 'workflows').mkdir(parents=True)
        (work / '.github' / 'workflows' / 'build.yml').write_bytes(b'run: ./gradlew build\n  epic_fight_file=epic.jar\n')
        (work / 'build.gradle').write_bytes(b'plugins {}\n')
        (work / 'CLAUDE.md').write_bytes(b'# CLAUDE.md\n\n## Shared\n\nrule\n\n## Claude Code\n\nclaude\n')
        (work / 'AGENTS.md').write_bytes(b'# AGENTS.md\n\n## Shared\n\nrule\n\n## Codex and opencode\n\ncodex\n')
        env.git('add', '-A', cwd=work)
        env.git('commit', '--quiet', '-m', 'chore: main base', cwd=work)
        env.git('push', '--quiet', 'origin', 'main', cwd=work)
        project = env.Project('scratch', work, scratch / 'temp', (), (), (), False, False)

        env.git('checkout', '--quiet', '-b', 'topic', cwd=work)
        (work / 'b.txt').write_bytes(b'two\n')
        wrong = ship_scratch([('chore: wrong branch', ['b.txt'])], work, project, dry_run=True)
        assert not wrong.ok and any('on branch topic, not main' in line for line in wrong.lines), wrong.lines
        wrong = ship_scratch([], work, project, push_only=True)
        assert not wrong.ok and any('not main' in line for line in wrong.lines), wrong.lines

        env.git('checkout', '--quiet', 'main', cwd=work)
        mirror = ship_scratch([('chore: jar mirror', ['b.txt'])], work, project, dry_run=True)
        assert not mirror.ok and any('run/mods has no local copy' in line for line in mirror.lines), mirror.lines


def missing_jars_read_build_gradle():
    """The CI jar parity reads the run/mods files build.gradle names, and nothing where the workflow
    downloads into the partner's checkout."""
    with tempfile.TemporaryDirectory(dir=env.TEMP_ROOT) as scratch:
        root = Path(scratch)
        (root / '.github' / 'workflows').mkdir(parents=True)
        (root / '.github' / 'workflows' / 'build.yml').write_text(
            'run: |\n  epic_fight_file=epic-fight.jar\n  geckolib_file=geckolib.jar\n', encoding='utf-8')
        (root / 'build.gradle').write_text("localRuntime files('run/mods/other.jar')\n", encoding='utf-8')
        base = env.Project('scratch', root, root / 'temp', (), (), (), False, False)
        shipper = lambda project: commit_push.Shipper(plan(('chore: x', ['a'])), None, False, False, 0,
                                                      project=project)
        assert shipper(base).missing_jars() == ['other.jar']
        assert shipper(replace(base, ci_jar_dir=None)).missing_jars() == [], 'the partner owns those jars'


class PlanTests(unittest.TestCase):

    def test_plan_validation(self):
        validate_plans()

    def test_ci_jar_parity(self):
        ci_jar_parity()
        missing_jars_read_build_gradle()

    def test_porcelain_and_coverage(self):
        porcelain_and_coverage()

    def test_contributor_gate(self):
        contributor_gate()

    def test_project_table(self):
        project_table()


class RoundTripTests(unittest.TestCase):
    """Local bare remotes, no Gradle, no network."""

    def test_first_push_from_an_empty_repository(self):
        first_push_round_trip()

    def test_branch_the_remote_lacks(self):
        wrong_branch_and_jar_mirror()


def validate_plans():
    good = plan(('chore: add bears and tigers', ['src/main/java/A.java', 'src/generated/data/']),
                ('test(gametest)!: isolate the server smoke directory', ['build.gradle']))
    assert commit_push.validate_plan(good) == []
    with_body = {'commits': [{'title': 'fix: x', 'body': 'Why.\n\nBREAKING CHANGE: y', 'files': ['a']}]}
    assert commit_push.validate_plan(with_body) == []

    assert commit_push.validate_plan({}) == ['plan needs a non-empty "commits" list']
    assert commit_push.validate_plan({'commits': []}) == ['plan needs a non-empty "commits" list']
    assert 'title must look like' in commit_push.validate_plan(plan(('UPDATE no brackets', ['a'])))[0]
    assert 'title must look like' in commit_push.validate_plan(plan(('[UPDATE] old prefix', ['a'])))[0]
    assert 'title must look like' in commit_push.validate_plan(plan(('feature: unknown type', ['a'])))[0]
    assert 'title must look like' in commit_push.validate_plan(plan(('fix:no space', ['a'])))[0]
    assert 'title must look like' in commit_push.validate_plan(plan(('chore: two\nlines', ['a'])))[0]
    assert 'attribution' in commit_push.validate_plan(plan(('chore: x Co-Authored-By: Claude', ['a'])))[0]
    ai_body = {'commits': [{'title': 'fix: x', 'body': 'Co-Authored-By: Claude <noreply@anthropic.com>', 'files': ['a']}]}
    assert 'attribution' in commit_push.validate_plan(ai_body)[0]
    assert 'non-empty list' in commit_push.validate_plan(plan(('chore: empty', [])))[0]
    assert 'relative' in commit_push.validate_plan(plan(('chore: abs', ['/alex/a.java'])))[0]
    assert 'relative' in commit_push.validate_plan(plan(('chore: up', ['../project-wiki/a.md'])))[0]
    duplicate = commit_push.validate_plan(plan(('chore: one', ['a.java']), ('chore: two', ['./a.java'])))
    assert duplicate == ['commit 2: a.java is already in commit 1']
    overlap = commit_push.validate_plan(plan(('chore: folder', ['src/test/']),
                                             ('chore: file', ['src/test/pure/A.java'])))
    assert overlap == ['commit 2: src/test/pure/A.java overlaps src/test/ in commit 1'], overlap
    reverse = commit_push.validate_plan(plan(('chore: file', ['src/a/B.java']), ('chore: folder', ['src/a'])))
    assert reverse == ['commit 2: src/a overlaps src/a/B.java in commit 1'], reverse
    assert commit_push.validate_plan(plan(('chore: same group', ['src/a/', 'src/a/B.java']))) == []
    assert commit_push.validate_plan(plan(('chore: one', ['src/a']), ('chore: two', ['src/ab.java']))) == []

    assert commit_push.normalize('./src/main/A.java ') == 'src/main/A.java'


def ci_jar_parity():
    workflow = 'run: |\n  epic_fight_file=epic-fight.jar\n  geckolib_file=geckolib.jar\n'
    build = "compileOnly files('run/mods/epic-fight.jar')\ncompileOnly files('run/mods/geckolib.jar')\n"
    assert commit_push.ci_jars(workflow) == ['epic-fight.jar', 'geckolib.jar']
    assert commit_push.missing_ci_jars(build, workflow) == []
    assert commit_push.missing_ci_jars(build + "localRuntime files('run/mods/new.jar')", workflow) == ['new.jar']
    live_build = (env.REPOSITORY_ROOT / 'build.gradle').read_text(encoding='utf-8')
    live_workflow = (env.REPOSITORY_ROOT / commit_push.WORKFLOW).read_text(encoding='utf-8')
    assert commit_push.RUN_MODS_JAR.findall(live_build), 'build.gradle run/mods references must stay parseable'
    assert commit_push.missing_ci_jars(live_build, live_workflow) == [], 'live build.gradle needs a jar CI lacks'


def porcelain_and_coverage():
    porcelain = ' M src/a.java\0?? src/new/b.json\0R  src/renamed.java\0src/original.java\0 D gone.txt\0'
    assert commit_push.parse_porcelain(porcelain) == ['src/a.java', 'src/new/b.json', 'src/renamed.java',
                                               'src/original.java', 'gone.txt']

    assert commit_push.covers('src/new/', 'src/new/b.json')
    assert commit_push.covers('src/new', 'src/new/b.json')
    assert commit_push.covers('src/a.java', 'src/a.java')
    assert not commit_push.covers('src/a', 'src/ab.java')
    idle, leftovers = commit_push.coverage(['src/a.java', 'src/new/', 'nothing/'],
                                    ['src/a.java', 'src/new/b.json', 'README.md'])
    assert idle == ['nothing/'] and leftovers == ['README.md']


def contributor_gate():
    alex = ('Alex', 'alex@example.com')
    log = ('a' * 40 + '\x1fAlex\x1falex@example.com\x1fAlex\x1falex@example.com\x1fchore: one\n\n\x1e\n'
           + 'b' * 40 + '\x1fAlex\x1falex@example.com\x1fAlex\x1falex@example.com\x1fchore: two\n\n'
           'Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\n\x1e\n')
    records = commit_push.parse_log(log)
    assert [r[0][:1] for r in records] == ['a', 'b'] and records[0][5].startswith('chore: one'), records
    assert commit_push.parse_log('') == []
    assert commit_push.contributor_problems(records[:1], alex) == []
    trailer = commit_push.contributor_problems(records, alex)
    assert trailer == ['bbbbbbb: message carries "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"'], trailer
    generated = ('c' * 40, 'Alex', 'alex@example.com', 'Alex', 'alex@example.com',
                 'chore: x\n\n🤖 Generated with [Claude Code](https://claude.com/claude-code)')
    assert 'message carries' in commit_push.contributor_problems([generated], alex)[0]
    claude = ('d' * 40, 'Claude', 'noreply@anthropic.com', 'Alex', 'alex@example.com', 'chore: x')
    assert commit_push.contributor_problems([claude], alex) == [
        'ddddddd: author is Claude <noreply@anthropic.com>, expected Alex <alex@example.com>']
    stranger = ('e' * 40, 'Alex', 'alex@example.com', 'Bot', 'bot@example.com', 'chore: x')
    assert commit_push.contributor_problems([stranger], alex) == [
        'eeeeeee: committer is Bot <bot@example.com>, expected Alex <alex@example.com>']
    for worker in (('Codex', 'codex@openai.com'), ('opencode', 'bot@opencode.ai')):
        assert commit_push.contributor_problems([], worker), f'{worker} must look like an AI'
    ai_config = commit_push.contributor_problems([], ('Claude', 'noreply@anthropic.com'))
    assert ai_config == ['the git config identity Claude <noreply@anthropic.com> looks like an AI'], ai_config


def project_table():
    gu_world = env.PROJECTS['guworld']
    assert gu_world.ci_stages == ('build', 'data') and gu_world.local_stages == ('gametest',)
    assert not gu_world.wiki_gate
    guzhenren = env.PROJECTS['guzhenren']
    assert guzhenren.ci_stages == ('build', 'data') and guzhenren.wiki_gate
    assert (guzhenren.ci_jar_dir, gu_world.ci_jar_dir) == ('run/mods', None)
    for project in (env.PROJECTS['guzhenren'], env.PROJECTS['guworld']):
        shipper = commit_push.Shipper(plan(('chore: x', ['a'])), None, False, False, 0, project=project)
        assert shipper.branch == 'main' and shipper.root == project.root
        assert shipper.work == project.temp / 'commit_push'


def run(status, conclusion, sha='1234567890abcdef'):
    return {'status': status, 'conclusion': conclusion, 'display_title': 'build', 'head_branch': 'main',
            'head_sha': sha}


def scripted(states):
    queue = list(states)
    return lambda owner, repo: [queue.pop(0)] if len(queue) > 1 else [queue[0]]


class GitHubActionsTests(unittest.TestCase):

    def poll(self, fetch, timeout=60, sha='1234567'):
        return commit_push.poll('o', 'r', sha, timeout=timeout, interval=0, fetch=fetch, sleep=lambda _: None,
                                echo=lambda _: None)

    def test_parse_remote(self):
        expected = ('alex-0v0-328', 'mcmod-guzhenren')
        for url in ('https://github.com/alex-0v0-328/mcmod-guzhenren.git',
                    'git@github.com:alex-0v0-328/mcmod-guzhenren.git', 'https://github.com/alex-0v0-328/mcmod-guzhenren'):
            self.assertEqual(commit_push.parse_remote(url), expected)
        for bad in ('https://gitlab.com/a/b.git', 'https://github.com/only-owner', ''):
            with self.assertRaises(ValueError):
                commit_push.parse_remote(bad)

    def test_poll_waits_for_the_run_of_its_commit(self):
        self.assertEqual(self.poll(scripted([run('queued', None), run('in_progress', None),
                                             run('completed', 'success')])), 'success')
        self.assertEqual(self.poll(scripted([run('completed', 'failure')])), 'failure')
        self.assertIsNone(self.poll(scripted([run('in_progress', None)]), timeout=0))
        self.assertIsNone(self.poll(lambda owner, repo: [], timeout=0))
        other = run('completed', 'failure', sha='fedcba0987654321')
        self.assertEqual(self.poll(lambda owner, repo: [other, run('completed', 'success')]), 'success')
        self.assertIsNone(self.poll(lambda owner, repo: [other], timeout=0))
        attempts = []

        def flaky(owner, repo):
            attempts.append(1)
            if len(attempts) == 1:
                raise RuntimeError('GitHub API 403: API rate limit exceeded')
            return [run('completed', 'success')]
        self.assertEqual(self.poll(flaky), 'success')
        self.assertEqual(len(attempts), 2, 'a failed fetch is retried')

    def test_a_rejected_token_falls_back_to_anonymous(self):
        calls = []

        def reject_token(url, token):
            calls.append(bool(token))
            return ('{"workflow_runs": [{"head_sha": "abc"}]}', 200) if not token else ('{"message": "Bad"}', 401)
        with unittest.mock.patch.dict(os.environ, {'GITHUB_PERSONAL_ACCESS_TOKEN': '"not-a-real-token"'}):
            self.assertEqual(commit_push.fetch_runs('o', 'r', get=reject_token), [{'head_sha': 'abc'}])
            self.assertEqual(calls, [True, False])
            with self.assertRaisesRegex(RuntimeError, 'rate limit'):
                commit_push.fetch_runs('o', 'r', get=lambda url, token: ('{"message": "API rate limit exceeded"}', 403))


if __name__ == '__main__':
    unittest.main()
