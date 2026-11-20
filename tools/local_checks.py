"""One-command local check of the mod: tool tests, build + JUnit, GameTests, runData drift, server and client smoke.

A stage that loads the other mod builds that mod's jar first, once per run, and fails without it
(project_runtime.PROJECTS, partner): Guzhenren's game stages (gametest, server, client) load the sibling Gu
World, which supplies every dimension; every Gu World stage loads Guzhenren, its required mod.

    python -B tools/local_checks.py                   every stage (client and multiplayer open a game window, ~1 min each)
    python -B tools/local_checks.py --headless        every stage except the two that open a game window
    python -B tools/local_checks.py gametest data     only the named stages, always in the canonical order
    python -B tools/local_checks.py --list            what each stage covers
    python -B tools/local_checks.py --project guworld the sibling Gu World: build, GameTests, runData drift

The full output of every stage goes to <Temp>/<project>/logs/local_checks-<pid>.log, one per run;
the console shows one summary line per stage. Exit code 0 means every stage that ran passed.
"""
import argparse
import json
import re
import secrets
import socket
import struct
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, field
from pathlib import Path

import project_runtime as env


BOOT_TIMEOUT = 420
SERVER_SETTLE = 10
CLIENT_SETTLE = 10
MULTIPLAYER_SETTLE = 10
# A saved server whose JVM an idle non-daemon thread pool of a pack mod still holds open is ended this many
# seconds after its save.
STOP_GRACE = 5
CRASH = re.compile(r'---- Minecraft Crash Report ----|#@!@# Game crashed!|Exception in server tick loop'
                   r'|Failed to start the minecraft server|Encountered an unexpected exception'
                   r'|Exception in thread "(Render thread|Server thread|main)"')
SERVER_DONE = re.compile(r'Done \((\d+[.,]\d+)s\)! For help')
SERVER_SAVED = re.compile(r'All dimensions are saved')
JOINED = re.compile(r'\]: (\S+) joined the game')
LOST = re.compile(r'\]: (\S+) lost connection: (.*)')
GAMETEST_PASSED = re.compile(r'All (\d+) required tests passed')
GAMETEST_FAILED = re.compile(r'(\d+) required tests failed')
GAMETEST_FAILED_NAME = re.compile(r'GameTestServer/\]:\s+- (\S+)')
GAMETEST_REPORT = re.compile(r'LogTestReporter/\]: (\S+) failed at [^!]*! (.*)')
TASK_FAILED = re.compile(r'^> Task (:\S+) FAILED')
TASK_RAN = re.compile(r'^> Task :(\w+)(?: UP-TO-DATE| FROM-CACHE| FAILED)?$')
MOD_ERROR = re.compile(r'/ERROR\].*(guzhenren|guworld)', re.I)


@dataclass
class Result:
    status: str
    seconds: float = 0.0
    detail: str = ''
    notes: list = field(default_factory=list)


@dataclass
class Context:
    root: Path
    log: env.Log
    wiki: Path = None
    compile_failed: bool = False
    project: env.Project = None
    # The partner build's outcome, shared by every run_stages call that passes the same dict
    # (commit_push's two mirror passes): absent until built, then '' or the reason it failed.
    partner_state: dict = field(default_factory=dict)
    # commit_push.py verifies an export: the partner is then an export of its own tree with its jar already
    # built (partner_root), and the game directories are the run's own (game_root).
    partner_root: Path = None
    game_root: Path = None

    def __post_init__(self):
        if self.project is None:
            self.project = env.PROJECTS['guzhenren']

    @property
    def partner(self):
        return self.partner_state.get('problem')

    @partner.setter
    def partner(self, problem):
        self.partner_state['problem'] = problem


class Collector:
    """Remembers the lines a stage cares about while the whole stream goes to the log."""

    def __init__(self, *patterns):
        self.patterns = patterns
        self.hits = []
        self.tail = []
        self.went_wrong = []
        self._in_went_wrong = False

    def __call__(self, line):
        self.tail = (self.tail + [line])[-60:]
        if line.startswith('* What went wrong:'):
            self._in_went_wrong = True
        elif line.startswith('* Try:'):
            self._in_went_wrong = False
        elif self._in_went_wrong and line.strip():
            self.went_wrong.append(line.strip())
        for pattern in self.patterns:
            match = pattern.search(line)
            if match:
                self.hits.append((pattern, match))


def gradle(ctx, *arguments, patterns=(), timeout=1800, root=None, project=None):
    root = root or ctx.root
    collector = Collector(TASK_FAILED, *patterns)
    started = time.monotonic()
    ctx.log.write(f'$ gradlew.bat {" ".join(arguments)}   (in {root})')
    deadline = started + timeout
    command = env.gradle_command(root, *arguments, project=project or ctx.project,
                                 partner_root=ctx.partner_root, game_root=ctx.game_root)
    with env.Process(command, root, ctx.log) as process:
        while True:
            outcome, line = process.watch(until=lambda _: True, timeout=max(0, deadline - time.monotonic()))
            if outcome == 'until':
                collector(line)
                continue
            break
        code = process.proc.returncode if outcome == 'exit' else None
        if code is None:
            process.kill_tree()
    return code, time.monotonic() - started, collector


def matches(collector, pattern):
    return [match for hit_pattern, match in collector.hits if hit_pattern is pattern]


def failed_tasks(collector):
    return [match.group(1) for match in matches(collector, TASK_FAILED)]


def went_wrong(collector, limit=6):
    return collector.went_wrong[:limit]


# ----- tools: the unit tests of these tools ----------------------------------------------------------

TOOL_WORKERS = 8
# Modules whose every test takes seconds of Git round trips: one process per test, started first. Every other
# module runs in one process, since a Python start costs about as much as its tests.
SLOW_TOOL_MODULES = ('test_commit_push',)
LIST_TOOL_TESTS = '\n'.join((
    'import unittest',
    'def flat(suite):',
    '    for test in suite:',
    '        yield from flat(test) if isinstance(test, unittest.TestSuite) else [test]',
    "for test in flat(unittest.defaultTestLoader.discover('tools', 'test_*.py')):",
    '    print(test.id())'))


# `unittest -v` names a red test as `test_x (module.Class.test_x) ... FAIL` (or ERROR).
UNITTEST_RED = re.compile(r'^\S+ \((\S+)\)\s.*\.\.\. (?:FAIL|ERROR)$')


class Lines(list):
    """A Process log that keeps one child's output, so parallel children land in the stage log one block each."""

    def write(self, line=''):
        self.append(line.rstrip('\r\n'))


def run_lines(ctx, command, environment, deadline):
    """(output lines, exit code or None on timeout) of one child in ctx.root."""
    lines = Lines()
    with env.Process(command, ctx.root, lines, env=environment) as process:
        code = process.wait(timeout=max(0.0, deadline - time.monotonic()))
    return lines, code


def tool_shards(tests):
    """The test ids split into processes: each test of SLOW_TOOL_MODULES alone and first, every other module whole."""
    slow, modules = [], {}
    for test in tests:
        module = test.split('.', 1)[0]
        if module in SLOW_TOOL_MODULES:
            slow.append([test])
        else:
            modules.setdefault(module, []).append(test)
    return slow + list(modules.values())


def stage_tools(ctx):
    """The tool tests in ctx.root, TOOL_WORKERS processes at once (tool_shards): in one process test_commit_push's
    Git round trips alone take half a minute. An export of the repository sits elsewhere, so the paths
    project_runtime derives from its own location are pinned to this workstation's."""
    started = time.monotonic()
    deadline = started + 900
    pinned = {'GZR_TEMP': str(env.TEMP_ROOT), 'GZR_GU_WORLD': str(env.GU_WORLD_ROOT), 'GZR_WIKI': str(env.WIKI_ROOT),
              'GZR_MODELS': str(env.MODELS_ROOT)}
    ctx.log.write(f'$ unittest discover -s tools, {TOOL_WORKERS} processes at once   (in {ctx.root})')
    tests, code = run_lines(ctx, [sys.executable, '-B', '-c', LIST_TOOL_TESTS], pinned, deadline)
    # A module that fails to import is discovered as a unittest.loader._FailedTest.
    broken = [test for test in tests if test.startswith('unittest.')]
    if code != 0 or not tests or broken:
        ctx.log.write('\n'.join(tests))
        return Result('FAIL', time.monotonic() - started, 'tool test discovery failed', (broken or tests)[-8:])
    pinned['PYTHONPATH'] = str(ctx.root / 'tools')
    failed, guard = [], threading.Lock()

    def run(shard):
        began = time.monotonic()
        lines, exit_code = run_lines(ctx, [sys.executable, '-B', '-m', 'unittest', '-v', *shard], pinned, deadline)
        took = f'{time.monotonic() - began:5.1f}s'
        name = shard[0] if len(shard) == 1 else f'{shard[0].split(".", 1)[0]} ({len(shard)} tests)'
        red = {match.group(1) for line in lines if (match := UNITTEST_RED.match(line))}
        with guard:
            if exit_code == 0:
                ctx.log.write(f'ok       {took}  {name}')
                return
            failed.extend(sorted(red) if red else [name])
            ctx.log.write(f'{"FAILED" if exit_code is not None else "TIMEOUT":<8} {took}  {name}')
            for line in lines:
                ctx.log.write(f'    {line}')

    pool = ThreadPoolExecutor(TOOL_WORKERS)
    try:
        list(pool.map(run, tool_shards(tests)))
    finally:
        pool.shutdown(cancel_futures=True)
    seconds = time.monotonic() - started
    if not failed:
        return Result('PASS', seconds, f'{len(tests)} tool tests passed')
    return Result('FAIL', seconds, f'{len(failed)} of {len(tests)} tool tests failed', sorted(failed)[:15])


# ----- build: compile, jar, pureTest JUnit ------------------------------------------------------

def junit_layers(root, collector):
    """{test task: junit_totals} for the test tasks this build ran or found up to date. A task the build no
    longer has leaves its reports in build/test-results, so a folder counts only when its task appeared."""
    ran = {match.group(1) for match in matches(collector, TASK_RAN)}
    results = root / 'build' / 'test-results'
    return {folder.name: env.junit_totals(folder) for folder in sorted(results.glob('*'))
            if folder.is_dir() and folder.name in ran}


def stage_build(ctx):
    arguments = ['build']
    if ctx.wiki:
        arguments.append(f'-PwikiDir={ctx.wiki}')
    code, seconds, collector = gradle(ctx, *arguments, patterns=(TASK_RAN,))
    tasks = failed_tasks(collector)
    ctx.compile_failed = any(task.startswith(':compile') for task in tasks)
    layers = junit_layers(ctx.root, collector)
    failed = [case for totals in layers.values() for case in totals['failed']]
    counts = ' + '.join(f'{name} {totals["tests"]}' for name, totals in layers.items())
    detail = (f'JUnit {counts}, {sum(t["failures"] for t in layers.values())} failed, '
              f'{sum(t["skipped"] for t in layers.values())} skipped' if layers else 'no JUnit tasks')
    detail += f'; wiki gate {"on" if ctx.wiki else "off"}'
    notes = [f'{name}: {message}' for name, message in failed[:10]]
    if code == 0:
        return Result('PASS', seconds, detail, notes)
    if ctx.compile_failed:
        detail = 'compile failed: ' + ', '.join(tasks)
    elif not tasks and code is None:
        detail = 'timed out'
    return Result('FAIL', seconds, detail, notes or went_wrong(collector))


# ----- gametest: every @GameTest on the dedicated GameTestServer ---------------------------------

def stage_gametest(ctx):
    if not (ctx.root / 'src' / 'test' / 'game').is_dir():
        return Result('SKIP', 0, 'no local GameTests in this checkout (src/test/game)')
    # The wiki gate reaches the GameTests too (WikiItemsGameTests checks nothing without it).
    wiki = [f'-PwikiDir={ctx.wiki}'] if ctx.wiki else []
    code, seconds, collector = gradle(ctx, 'runGameTestServer', *wiki, patterns=(
        GAMETEST_PASSED, GAMETEST_FAILED, GAMETEST_FAILED_NAME, GAMETEST_REPORT, CRASH))
    passed = matches(collector, GAMETEST_PASSED)
    failed = matches(collector, GAMETEST_FAILED)
    reports = {m.group(1): m.group(2) for m in matches(collector, GAMETEST_REPORT)}
    names = [m.group(1) for m in matches(collector, GAMETEST_FAILED_NAME)]
    notes = [f'{name}: {reports.get(name, "")}'.rstrip(': ') for name in names[:15]]
    if code == 0 and passed:
        return Result('PASS', seconds, f'all {passed[-1].group(1)} required GameTests passed; '
                                       f'wiki gate {"on" if ctx.wiki else "off"}')
    if failed:
        return Result('FAIL', seconds, f'{failed[-1].group(1)} required GameTests failed', notes)
    if matches(collector, CRASH):
        return Result('FAIL', seconds, 'GameTest server crashed', went_wrong(collector) or collector.tail[-8:])
    return Result('FAIL', seconds, f'gradle exit {code}', went_wrong(collector))


# ----- data: runData must reproduce src/generated byte for byte ---------------------------------

def stage_data(ctx):
    generated = ctx.root / 'src' / 'generated'
    before = env.tree_hashes(generated)
    code, seconds, collector = gradle(ctx, 'runData')
    if code != 0:
        return Result('FAIL', seconds, f'runData failed (exit {code})', went_wrong(collector))
    changed, added, removed = env.diff_hashes(before, env.tree_hashes(generated))
    if not (changed or added or removed):
        return Result('PASS', seconds, f'src/generated unchanged ({len(before)} files)')
    notes = ([f'changed  {p}' for p in changed] + [f'added    {p}' for p in added]
             + [f'removed  {p}' for p in removed])
    return Result('FAIL', seconds,
                  f'src/generated was stale: runData rewrote {len(notes)} files; review them, then re-run',
                  notes[:20])


# ----- partner: the other mod's jar, built once before the first stage that loads it ---------------

def stale_jars(root, mod_id):
    """Jars in build/libs of an older mod_version. The runs load every `<mod_id>-*.jar` there, and a version bump
    leaves the old jar behind; the current version's jars, any letter-only classifier (-all, -sources), stay."""
    properties = root / 'gradle.properties'
    text = properties.read_text(encoding='utf-8') if properties.is_file() else ''
    version = re.search(r'^mod_version=(\S+)', text, re.M)
    if not version:
        return []
    current = re.compile(rf'{re.escape(mod_id)}-{re.escape(version.group(1))}(-[a-z]+)?\.jar')
    return sorted(path for path in (root / 'build' / 'libs').glob(f'{mod_id}-*.jar')
                  if not current.fullmatch(path.name))


def partner_problem(ctx, root=None):
    """Build the partner's jar once per run; its partner_stages load it. None when it built, else the
    reason. assemble skips the partner's own tests, which its own check runs, and Gradle's up-to-date
    check makes an unchanged jar cost seconds; a task that re-runs drops the outputs its last run left
    (a renamed resource included), and stale_jars goes after a version bump."""
    if ctx.partner is None:
        partner = env.partner_of(ctx.project)
        root = root or ctx.project.partner
        name = ctx.project.partner_name
        if not (root / 'gradlew.bat').exists():
            ctx.partner = f'{name} not found at {root}; {", ".join(ctx.project.partner_stages)} load its jar'
            return ctx.partner
        ctx.log.section(name)
        code, _, collector = gradle(ctx, 'assemble', root=root, project=partner)
        for jar in stale_jars(root, partner.name) if code == 0 else []:
            ctx.log.write(f'removed the stale jar {jar.name}')
            jar.unlink()
        ctx.partner = '' if code == 0 else f'{name} build failed (exit {code}): ' + '; '.join(
            went_wrong(collector, 2))
    return ctx.partner or None


# ----- server: dedicated server in a throwaway game dir, stopped over RCON ------------------------

def free_port():
    with socket.socket() as probe:
        probe.bind(('127.0.0.1', 0))
        return probe.getsockname()[1]


def rcon_packet(request_id, kind, body):
    payload = struct.pack('<ii', request_id, kind) + body.encode('utf-8') + b'\x00\x00'
    return struct.pack('<i', len(payload)) + payload


def read_exact(connection, size):
    data = b''
    while len(data) < size:
        chunk = connection.recv(size - len(data))
        if not chunk:
            raise ConnectionError('RCON connection closed')
        data += chunk
    return data


def read_rcon_packet(connection):
    # One packet per reply: fine for the one-line summon/place answers sent here. A reply over the
    # server's ~4 KB packet limit would arrive split and needs a sentinel-command read loop.
    length = struct.unpack('<i', read_exact(connection, 4))[0]
    data = read_exact(connection, length)
    request_id, kind = struct.unpack('<ii', data[:8])
    return request_id, kind, data[8:-2].decode('utf-8', errors='replace')


class Rcon:
    def __init__(self, port, password, host='127.0.0.1'):
        self.connection = socket.create_connection((host, port), timeout=15)
        try:
            self.connection.sendall(rcon_packet(1, 3, password))
            if read_rcon_packet(self.connection)[0] == -1:
                raise ConnectionError('RCON login rejected')
        except BaseException:
            self.connection.close()
            raise

    def command(self, text):
        self.connection.sendall(rcon_packet(2, 2, text))
        return read_rcon_packet(self.connection)[2]

    def close(self):
        self.connection.close()


def generated_sources(root, gu_world=None):
    """(namespace, generated resources folder) of this mod and of the sibling Gu World it runs with."""
    return [('guzhenren', root / 'src/generated/resources'),
            ('guworld', (gu_world or env.GU_WORLD_ROOT) / 'src/generated/resources')]


def mod_ids(namespace, resources):
    """Registry ids of a mod's entity types, read from its generated lang keys."""
    path = resources / 'assets' / namespace / 'lang' / 'zh_cn.json'
    if not path.exists():
        return []
    lang = json.loads(path.read_text(encoding='utf-8'))
    prefix = f'entity.{namespace}.'
    return sorted(f'{namespace}:' + key[len(prefix):] for key in lang
                  if key.startswith(prefix) and '.' not in key[len(prefix):])


def data_ids(namespace, resources, kind):
    """Ids of a mod's generated datapack entries of one kind, e.g. 'worldgen/configured_feature'."""
    return sorted(f'{namespace}:{path.stem}' for path in (resources / 'data' / namespace / kind).glob('*.json'))


def server_properties(port, rcon_port, password):
    return '\n'.join([
        'online-mode=false', 'server-ip=127.0.0.1', f'server-port={port}',
        'enable-rcon=true', f'rcon.port={rcon_port}', f'rcon.password={password}', 'broadcast-rcon-to-ops=false',
        'level-seed=guzhenren-smoke', 'view-distance=6', 'simulation-distance=6', 'spawn-protection=0',
        'enable-query=false', 'sync-chunk-writes=false', 'motd=GZR smoke test', ''])


def smoke_commands(root, gu_world=None):
    """Through RCON, for this mod and Gu World: summon each entity at world spawn, place each feature
    there, and query the time inside each dimension. GameTestServer never instantiates a datapack
    dimension, so this query is the one check that a mod dimension really loads as a level."""
    sources = generated_sources(root, gu_world)
    commands = []
    for namespace, resources in sources:
        commands += [(f'summon {entity} ~ ~1 ~', 'Summoned') for entity in mod_ids(namespace, resources)]
    for namespace, resources in sources:
        commands += [(f'place feature {feature} ~ ~ ~', None)
                     for feature in data_ids(namespace, resources, 'worldgen/configured_feature')]
    for namespace, resources in sources:
        commands += [(f'execute in {dimension} run time query gametime', 'The time is')
                     for dimension in data_ids(namespace, resources, 'dimension')]
    return commands


def fresh_game_dir(ctx, name):
    """An empty throwaway game dir under the project's Temp folder, with the loader's update check off: the
    loader fills in the rest of fml.toml, and its update check would hold a stopped JVM open while it waits on
    every mod's update URL."""
    game_dir = ctx.project.temp / name
    env.remove_tree(game_dir)
    (game_dir / 'config').mkdir(parents=True)
    (game_dir / 'config' / 'fml.toml').write_text('versionCheck = false\n', encoding='utf-8')
    return game_dir


def server_command(ctx, name):
    """A runServer in a fresh throwaway game dir: (command, game dir, game port, RCON port, RCON password)."""
    game_dir = fresh_game_dir(ctx, name)
    port, rcon_port, password = free_port(), free_port(), secrets.token_hex(8)
    (game_dir / 'eula.txt').write_text('eula=true\n', encoding='utf-8')
    (game_dir / 'server.properties').write_text(server_properties(port, rcon_port, password), encoding='utf-8')
    command = env.gradle_command(ctx.root, 'runServer', f'-PserverGameDir={game_dir}', project=ctx.project)
    ctx.log.write(f'$ {" ".join(command)}')
    return command, game_dir, port, rcon_port, password


def stage_server(ctx):
    started = time.monotonic()
    command, game_dir, _, rcon_port, password = server_command(ctx, 'server-smoke')
    try:
        return smoke_server(ctx, command, game_dir, rcon_port, password, started)
    finally:
        # The server JVM is the Gradle daemon's child, which Gradle stops when this client dies; one it
        # missed still runs in the throwaway game dir.
        env.kill_pids(env.java_processes_in(game_dir))


def smoke_server(ctx, command, game_dir, rcon_port, password, started):
    with env.Process(command, ctx.root, ctx.log) as process:
        crashed = CRASH.search

        outcome, line = process.watch(until=SERVER_DONE.search, fail=crashed, timeout=BOOT_TIMEOUT)
        if outcome != 'until':
            process.kill_tree()
            return Result('FAIL', time.monotonic() - started, f'server did not boot ({outcome})',
                          [line] if line else [])
        boot = SERVER_DONE.search(line).group(1)
        notes = []
        counts = {'summon': [0, 0], 'place': [0, 0], 'execute': [0, 0]}
        try:
            rcon = Rcon(rcon_port, password)
            for text, expected in smoke_commands(ctx.root, env.mod_roots(ctx.project, ctx.root)[0]):
                reply = rcon.command(text).strip()
                ctx.log.write(f'[rcon] {text} -> {reply}')
                tally = counts[text.split()[0]]
                tally[1] += 1
                if expected and expected not in reply:
                    notes.append(f'{text} -> {reply or "(no reply)"}')
                elif 'Failed' not in reply:
                    tally[0] += 1
            rcon.close()
        except OSError as error:
            process.kill_tree()
            return Result('FAIL', time.monotonic() - started, f'RCON failed after boot: {error}')

        outcome, line = process.watch(fail=crashed, timeout=SERVER_SETTLE)
        if outcome != 'timeout':
            process.kill_tree()
            return Result('FAIL', time.monotonic() - started,
                          f'server {"crashed" if outcome == "fail" else "exited"} while ticking', [line or ''])
        code, held_open = stop_server(process, game_dir, rcon_port, password)
        errors = mod_errors(ctx)
        seconds = time.monotonic() - started
        # A feature that refuses the spawn terrain is a normal outcome; only a crash would fail it.
        summary = (f'booted in {boot}s, summoned {counts["summon"][0]}/{counts["summon"][1]} mod entities, '
                   f'features placed {counts["place"][0]}/{counts["place"][1]} (terrain decides), '
                   f'dimensions loaded {counts["execute"][0]}/{counts["execute"][1]}, ticked {SERVER_SETTLE}s')
        if notes:
            return Result('FAIL', seconds, summary + f'; {len(notes)} smoke commands failed', notes)
        if code != 0 and not held_open:
            return Result('FAIL', seconds, summary + f'; did not stop cleanly (exit {code})')
        env.remove_tree(game_dir)
        tail = f'; {errors} ERROR lines mention guzhenren or guworld (see log)' if errors else ''
        return Result('PASS', seconds, summary + stopped_note(held_open) + tail)


def stop_server(process, game_dir, rcon_port, password):
    """Stop a smoke server over RCON and wait for its save: (exit code or None, whether an idle thread held
    the saved JVM open and it was ended after STOP_GRACE)."""
    try:
        rcon = Rcon(rcon_port, password)
        rcon.connection.sendall(rcon_packet(3, 2, 'stop'))
        rcon.close()
    except OSError:
        pass
    outcome, _ = process.watch(until=SERVER_SAVED.search, fail=CRASH.search, timeout=120)
    held_open = False
    if outcome == 'until':
        code = process.wait(timeout=STOP_GRACE)
        if code is None:
            held_open = True
            env.kill_pids(env.java_processes_in(game_dir))
            code = process.wait(timeout=30)
    else:
        code = process.proc.returncode if outcome == 'exit' else None
    if code is None:
        process.kill_tree()
    return code, held_open


def stopped_note(held_open):
    return (f', saved and stopped, its JVM ended after {STOP_GRACE}s held open by an idle thread'
            if held_open else ', stopped cleanly')


def mod_errors(ctx):
    return sum(1 for line in ctx.log.path.read_text(encoding='utf-8', errors='replace').splitlines()
               if MOD_ERROR.search(line))


# ----- client: boot to the title screen, then close ----------------------------------------------

def client_ready(state):
    def check(line):
        if 'Sound engine started' in line:
            state['sound'] = True
        if 'textures/atlas/gui.png-atlas' in line:
            state['atlas'] = True
        return state.get('sound') and state.get('atlas')
    return check


def stage_client(ctx, on_ready=None):
    """on_ready(game JVM pids) runs once the title screen has settled, before the client closes: game_window.py's
    title shot."""
    before = env.java_processes()
    running = env.game_jvms(before, ctx.root)
    if running:
        return Result('SKIP', 0, f'a game from this checkout is already running (pid {running}); close it first')
    started = time.monotonic()
    command = env.gradle_command(ctx.root, 'runClient', project=ctx.project, daemon=False)
    ctx.log.write(f'$ {" ".join(command)}')
    with env.Process(command, ctx.root, ctx.log) as process:
        outcome, line = process.watch(until=client_ready({}), fail=CRASH.search, timeout=BOOT_TIMEOUT)
        boot = time.monotonic() - started
        if outcome == 'until':
            outcome, line = process.watch(fail=CRASH.search, timeout=CLIENT_SETTLE)
            alive = outcome == 'timeout'
        else:
            alive = False
        owned = set(env.descendant_pids(process.proc.pid))
        spawned = [pid for pid in env.game_jvms(env.java_processes(), ctx.root) if pid in owned]
        try:
            if alive and on_ready:
                on_ready(spawned)
        finally:
            process.kill_tree()
            env.kill_pids(spawned)
        leftover = [pid for pid in env.game_jvms(env.java_processes(), ctx.root) if pid in spawned]
        seconds = time.monotonic() - started
        notes = [f'game JVM {leftover} still running after kill'] if leftover else []
        if alive:
            return Result('PASS', seconds, f'title screen reached in {boot:.0f}s, alive {CLIENT_SETTLE}s, closed',
                          notes)
        return Result('FAIL', seconds, f'client {outcome} before settling', [line or ''] + notes)



# ----- multiplayer: a client joins a smoke server, both with the same mods ------------------------------

def client_options():
    """options.txt for a first launch that goes straight into the game: no accessibility onboarding, no pause
    when the window loses focus, silent, the smallest view."""
    return '\n'.join(['onboardAccessibility:false', 'pauseOnLostFocus:false', 'skipMultiplayerWarning:true',
                      'joinedFirstServer:true', 'tutorialStep:none', 'soundCategory_master:0.0',
                      'renderDistance:2', 'simulationDistance:5', ''])


def multiplayer_commands(player):
    """Through RCON, each with the reply it must carry (None: any): data that syncs to the joined client, and a
    round trip through the Treasure Yellow Heaven, every dimension change re-sending that data and the commands."""
    return [(f'gzr awaken {player}', None),
            (f'guworld enter guworld:treasure_yellow_heaven {player}', '送入'),
            (f'guworld exit {player}', '送出'),
            ('list', player)]


def watch_pair(server, client, until=None, timeout=None):
    """Watch the server for `until` while the client's output keeps draining; either side crashing, the
    client exiting or the server dropping a player ends it. Returns (outcome, side, line)."""
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        outcome, line = server.watch(until=until, fail=lambda text: CRASH.search(text) or LOST.search(text),
                                     timeout=0.5)
        if outcome != 'timeout':
            return outcome, 'server', line
        outcome, line = client.watch(fail=CRASH.search, timeout=0.05)
        if outcome != 'timeout':
            return outcome, 'client', line
    return 'timeout', None, None


def stage_multiplayer(ctx):
    started = time.monotonic()
    running = env.game_jvms(env.java_processes(), ctx.root)
    if running:
        return Result('SKIP', 0, f'a game from this checkout is already running (pid {running}); close it first')
    command, game_dir, port, rcon_port, password = server_command(ctx, 'multiplayer-server')
    client_dir = fresh_game_dir(ctx, 'multiplayer-client')
    (client_dir / 'options.txt').write_text(client_options(), encoding='utf-8')
    try:
        with env.Process(command, ctx.root, ctx.log) as server:
            outcome, line = server.watch(until=SERVER_DONE.search, fail=CRASH.search, timeout=BOOT_TIMEOUT)
            if outcome != 'until':
                server.kill_tree()
                return Result('FAIL', time.monotonic() - started, f'server did not boot ({outcome})',
                              [line] if line else [])
            try:
                result = join_server(ctx, server, game_dir, client_dir, port, rcon_port, password, started)
            finally:
                stale = restore_client_args(ctx)
            if stale:
                result = Result('FAIL', result.seconds, f'{result.detail}; {stale[0]}', result.notes + stale[1:])
            return result
    finally:
        env.kill_pids(env.java_processes_in(game_dir))


def restore_client_args(ctx):
    """The joining client's -PclientGameDir and -PquickPlayServer land in build/moddev/clientRunProgramArgs.txt,
    the file IDEA's Client run reads, which would then quick-play the stopped smoke server. prepareClientRun
    rewrites it from build.gradle alone. Returns [] once restored, else the failure line and Gradle's reasons."""
    code, _, collector = gradle(ctx, 'prepareClientRun', timeout=600)
    if code == 0:
        return []
    return ["prepareClientRun failed: IDEA's Client still quick-plays the stopped smoke server",
            *went_wrong(collector)]


def join_server(ctx, server, game_dir, client_dir, port, rcon_port, password, started):
    command = env.gradle_command(ctx.root, 'runClient', f'-PclientGameDir={client_dir}',
                                 f'-PquickPlayServer=127.0.0.1:{port}', project=ctx.project, daemon=False)
    ctx.log.write(f'$ {" ".join(command)}')
    notes = []
    with env.Process(command, ctx.root, ctx.log) as client:
        try:
            outcome, side, line = watch_pair(server, client, until=JOINED.search, timeout=BOOT_TIMEOUT)
            if outcome != 'until':
                return Result('FAIL', time.monotonic() - started, f'client did not join ({side} {outcome})',
                              [line] if line else [])
            player = JOINED.search(line).group(1)
            joined = time.monotonic() - started
            try:
                rcon = Rcon(rcon_port, password)
                for text, expected in multiplayer_commands(player):
                    reply = rcon.command(text).strip()
                    ctx.log.write(f'[rcon] {text} -> {reply}')
                    if expected and expected not in reply:
                        notes.append(f'{text} -> {reply or "(no reply)"}')
                    outcome, side, line = watch_pair(server, client, timeout=2)
                    if outcome != 'timeout':
                        return Result('FAIL', time.monotonic() - started, f'{side} {outcome} after `{text}`',
                                      [line or ''])
                rcon.close()
            except OSError as error:
                return Result('FAIL', time.monotonic() - started, f'RCON failed after the join: {error}')
            outcome, side, line = watch_pair(server, client, timeout=MULTIPLAYER_SETTLE)
            if outcome != 'timeout':
                return Result('FAIL', time.monotonic() - started, f'{side} {outcome} while playing', [line or ''])
        finally:
            owned = set(env.descendant_pids(client.proc.pid))
            spawned = [pid for pid in env.game_jvms(env.java_processes(), ctx.root) if pid in owned]
            client.kill_tree()
            env.kill_pids(spawned)
    code, held_open = stop_server(server, game_dir, rcon_port, password)
    seconds = time.monotonic() - started
    summary = (f'{player} joined in {joined:.0f}s, synced data, crossed the Treasure Yellow Heaven and back, '
               f'played {MULTIPLAYER_SETTLE}s')
    if notes:
        return Result('FAIL', seconds, summary + f'; {len(notes)} commands failed', notes)
    if code != 0 and not held_open:
        return Result('FAIL', seconds, summary + f'; the server did not stop cleanly (exit {code})')
    env.remove_tree(game_dir)
    env.remove_tree(client_dir)
    errors = mod_errors(ctx)
    tail = f'; {errors} ERROR lines mention guzhenren or guworld (see log)' if errors else ''
    return Result('PASS', seconds, summary + stopped_note(held_open) + tail)

STAGES = {
    'tools': ('tools/test_*.py: the unit tests of these tools, one process per test, in parallel', stage_tools),
    'build': ('gradlew build: compile, jar, pureTest JUnit (wiki gate when the wiki exists)', stage_build),
    'gametest': ('gradlew runGameTestServer: every @GameTest on a dedicated server, both mods loaded (wiki gate as in build)', stage_gametest),
    'data': ('gradlew runData: providers must reproduce src/generated exactly (the CI drift check)', stage_data),
    'server': ('runServer in a throwaway Temp world, Gu World loaded: boot, summon mod entities, place mod '
               'features, load every dimension of both mods, tick, stop over RCON', stage_server),
    'client': ('runClient with the run/ pack and Gu World: boot to the title screen, stay alive, close '
               '(opens a window)', stage_client),
    'multiplayer': ('runServer plus a runClient that joins it, both in throwaway Temp dirs with the same mods: '
                    'join, sync player data, cross the Treasure Yellow Heaven and back, play, stop '
                    '(opens a window)', stage_multiplayer),
}
NEEDS_CLASSES = {'gametest', 'data', 'server', 'client', 'multiplayer'}
WINDOWED = {'client', 'multiplayer'}


def run_stages(root, names, log, wiki=None, echo=print, project=None, partner_state=None, partner_root=None,
               game_root=None):
    project = project or env.PROJECTS['guzhenren']
    ctx = Context(Path(root), log, wiki, project=project,
                  partner_state=partner_state if partner_state is not None else {},
                  partner_root=partner_root, game_root=game_root)
    results = {}
    for name in names:
        if ctx.compile_failed and name in NEEDS_CLASSES:
            results[name] = Result('SKIP', 0, 'compile failed in build')
        elif name in project.partner_stages and partner_problem(ctx):
            results[name] = Result('FAIL', 0, ctx.partner)
        else:
            log.section(name)
            echo(f'[{name}] running ...', flush=True)
            results[name] = STAGES[name][1](ctx)
        echo(summary_line(name, results[name]), flush=True)
        for note in results[name].notes:
            echo(f'           {note}', flush=True)
    return results


def summary_line(name, result):
    return f'{name:<11} {result.status:<5} {env.format_seconds(result.seconds):>6}  {result.detail}'


def select(stage_arguments, headless, project=None):
    known = [name for name in STAGES if name in (project or env.PROJECTS['guzhenren']).stages]
    unknown = [name for name in stage_arguments if name not in known]
    if unknown:
        raise SystemExit(f'unknown stage(s): {", ".join(unknown)}; known: {", ".join(known)}')
    chosen = set(stage_arguments) or set(known)
    if headless:
        chosen -= WINDOWED
    return [name for name in known if name in chosen]


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('stages', nargs='*', help=f'subset of: {", ".join(STAGES)} (default: all)')
    parser.add_argument('--project', choices=sorted(env.PROJECTS), default='guzhenren',
                        help='which repository to check (default guzhenren; guworld has build, gametest and data)')
    parser.add_argument('--headless', action='store_true', help='skip the client and multiplayer stages')
    parser.add_argument('--no-wiki', action='store_true', help='run build and gametest without the -PwikiDir gate')
    parser.add_argument('--list', action='store_true', help='describe the stages and exit')
    args = parser.parse_args(argv)
    project = env.project(args.project)
    if args.list:
        for name, (description, _) in STAGES.items():
            if name in project.stages:
                print(f'{name:<11} {description}')
        return 0
    env.require_windows()
    names = select(args.stages, args.headless, project)
    wiki = None if args.no_wiki or not project.wiki_gate or not env.wiki_available() else env.WIKI_ROOT
    with env.Lock(wait=env.LOCK_WAIT):
        log = env.Log('local_checks', log_dir=project.temp / 'logs', per_run=True)
        try:
            print(f'{project.name} check: {", ".join(names)}   log: {log.path}', flush=True)
            results = run_stages(project.root, names, log, wiki, project=project)
            log.section('summary')
            for name, result in results.items():
                log.write(summary_line(name, result))
                for note in result.notes:
                    log.write(f'           {note}')
            ok = all(result.status == 'PASS' for result in results.values())
            verdict = 'PASS' if ok else 'FAIL'
            log.write(f'RESULT: {verdict}')
        finally:
            log.close()
        print(f'RESULT: {verdict}   (read {log.path}, then delete it)')
        return 0 if ok else 1


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
