import json
import os
import socket
import struct
import tempfile
import threading
import unittest
from contextlib import redirect_stderr, redirect_stdout
from dataclasses import replace
from io import StringIO
from pathlib import Path
from unittest.mock import patch

import local_checks as check
import project_runtime as env


def expect_exit(function, *arguments):
    try:
        function(*arguments)
    except SystemExit:
        return
    raise AssertionError(f'expected SystemExit from {function.__name__}{arguments}')


def fake_rcon_server(replies, reject_login=False):
    """A one-connection RCON server on a free port; returns (port, thread)."""
    listener = socket.socket()
    listener.bind(('127.0.0.1', 0))
    listener.listen(1)

    def serve():
        connection, _ = listener.accept()
        with connection, listener:
            request_id, _, _ = check.read_rcon_packet(connection)
            connection.sendall(check.rcon_packet(-1 if reject_login else request_id, 2, ''))
            for reply in replies:
                request_id, _, _ = check.read_rcon_packet(connection)
                connection.sendall(check.rcon_packet(request_id, 0, reply))

    thread = threading.Thread(target=serve, daemon=True)
    thread.start()
    return listener.getsockname()[1], thread


class FakeProcess:
    """Stands in for env.Process: records the command, replays scripted watch() outcomes."""
    commands = []

    def __init__(self, command, cwd, log, outcomes=()):
        FakeProcess.commands.append(command)
        self.outcomes = list(outcomes) or [('exit', None)]
        self.proc = type('Child', (), {'pid': 1, 'returncode': 0})()

    def __enter__(self):
        return self

    def __exit__(self, *args):
        pass

    def watch(self, until=None, fail=None, timeout=None):
        return self.outcomes.pop(0) if len(self.outcomes) > 1 else self.outcomes[0]

    def wait(self, timeout=None):
        return 0

    def kill_tree(self):
        pass


def command_of(stage, ctx):
    """The Gradle command a stage launches, with the process faked out."""
    FakeProcess.commands = []
    with patch.object(env, 'Process', FakeProcess):
        result = stage(ctx)
    return FakeProcess.commands[0], result


def project_checks(root):
    guzhenren, guworld = env.PROJECTS['guzhenren'], env.PROJECTS['guworld']
    assert check.select([], False, guworld) == ['build', 'gametest', 'data']
    expect_exit(check.select, ['client'], False, guworld)
    for owner in (guzhenren, guworld):
        assert f'-PguWorldDir={guworld.root}' in env.gradle_command(owner.root, 'build', project=owner)
        assert f'-PguzhenrenDir={guzhenren.root}' in env.gradle_command(owner.root, 'build', project=owner)
    assert check.Context(root, None).project is guzhenren

    listed = StringIO()
    with redirect_stdout(listed):
        assert check.main(['--list', '--project', 'guworld']) == 0
    assert [line.split()[0] for line in listed.getvalue().splitlines()] == ['build', 'gametest', 'data']
    with redirect_stderr(StringIO()):
        expect_exit(check.main, ['--list', '--branch', 'forge-1.20.1'])

    # The client is ready once both the sound engine and the GUI atlas are up, in either order.
    ready = check.client_ready({})
    ready('Sound engine started')
    assert not ready('minecraft:textures/atlas/mob_effects.png-atlas')
    assert ready('Created: 4096x2048x0 minecraft:textures/atlas/gui.png-atlas')
    reversed_ready = check.client_ready({})
    assert not reversed_ready('Created: 4096x2048x0 minecraft:textures/atlas/gui.png-atlas')
    assert reversed_ready('[Render thread/INFO] [SoundEngine/SOUNDS]: Sound engine started')

    # runData and runServer run in the project's own temp game dirs.
    generated = root / 'generated-root'
    generated.mkdir()
    for owner in (guzhenren, guworld):
        data_log = env.Log('data', root)
        command, result = command_of(check.stage_data, check.Context(generated, data_log, project=owner))
        data_log.close()
        assert result.status == 'PASS', result
        assert command[1] == 'runData' and command[2].startswith('-P'), command
        assert f'-PdataGameDir={owner.temp / "data"}' in command
    server_project = replace(guzhenren, temp=root / 'main-temp')
    owner = server_project
    recorded = []
    replies = [('until', '[Server thread/INFO]: Done (1.000s)! For help, type "help"'), ('timeout', None),
               ('until', 'ThreadedAnvilChunkStorage: All dimensions are saved')]
    smoke_log = env.Log('server', root)

    class Replay(FakeProcess):
        def __init__(self, command, cwd, log):
            super().__init__(command, cwd, log, replies)
            recorded.append((owner.temp / 'server-smoke' / 'config' / 'fml.toml').read_text(encoding='utf-8'))

    class FakeRcon:
        connection = type('Connection', (), {'sendall': lambda self, data: None})()

        def __init__(self, port, password):
            pass

        def command(self, text):
            return 'Summoned'

        def close(self):
            pass

    def record_smoke(source, gu_world=None):
        recorded.append((source, gu_world))
        return []
    FakeProcess.commands = []
    with patch.object(env, 'Process', Replay), patch.multiple(check, Rcon=FakeRcon, smoke_commands=record_smoke):
        result = check.stage_server(check.Context(owner.root, smoke_log, project=owner))
    smoke_log.close()
    command = FakeProcess.commands[0]
    assert result.status == 'PASS', result
    assert f'-PserverGameDir={owner.temp / "server-smoke"}' in command, 'the smoke world lives in the project temp'
    assert f'-PguWorldDir={env.partner_of(owner).root}' in command, 'the sibling Gu World'
    assert recorded == ['versionCheck = false\n', (owner.root, env.partner_of(owner).root)], recorded

    # A saved server whose JVM an idle thread holds open is ended after STOP_GRACE and still passes.
    held_log = env.Log('held', root)
    waits = [None, 143]

    class HeldOpen(FakeProcess):
        def __init__(self, command, cwd, log):
            super().__init__(command, cwd, log, [('until', 'Done (1.000s)! For help'), ('timeout', None),
                                                 ('until', 'All dimensions are saved')])

        def wait(self, timeout=None):
            return waits.pop(0)
    killed = []
    with patch.object(env, 'Process', HeldOpen), patch.object(env, 'kill_pids', killed.append), \
            patch.object(env, 'java_processes_in', lambda folder: [4242]), \
            patch.multiple(check, Rcon=FakeRcon, smoke_commands=lambda *a: []):
        held = check.stage_server(check.Context(server_project.root, held_log, project=server_project))
    held_log.close()
    assert held.status == 'PASS' and 'held open by an idle thread' in held.detail, held
    assert [4242] in killed and waits == [], (killed, waits)

    # The partner is built without clean: Gradle's up-to-date check keeps an unchanged jar.
    (root / 'gradlew.bat').write_text('', encoding='utf-8')
    for owner in (guzhenren, guworld):
        calls = []

        def built(ctx, *arguments, root=None, project=None, **kwargs):
            calls.append((arguments, root, project))
            return 0, 0.0, check.Collector()
        log = env.Log('partner', root)
        with patch.object(check, 'gradle', built):
            assert check.partner_problem(check.Context(root, log, project=owner), root) is None
        log.close()
        assert calls == [(('assemble',), root, env.partner_of(owner))], calls

    # A version bump leaves the old jar beside the new one; the runs load both, so it goes.
    libs = root / 'build' / 'libs'
    libs.mkdir(parents=True)
    (root / 'gradle.properties').write_text('mod_id=guworld\nmod_version=1.2.0\n', encoding='utf-8')
    for name in ('guworld-1.1.0.jar', 'guworld-1.2.0.jar', 'guworld-1.2.0-all.jar', 'guworld-1.2.0-sources.jar',
                 'guworld-1.2.0-rc1.jar', 'other-1.0.0.jar'):
        (libs / name).write_bytes(b'')
    assert [path.name for path in check.stale_jars(root, 'guworld')] == ['guworld-1.1.0.jar', 'guworld-1.2.0-rc1.jar']
    log = env.Log('partner', root)
    with patch.object(check, 'gradle', lambda ctx, *arguments, **kwargs: (0, 0.0, check.Collector())):
        shared = {}
        assert check.partner_problem(check.Context(root, log, project=env.PROJECTS['guzhenren'],
                                                   partner_state=shared), root) is None
    log.close()
    assert sorted(path.name for path in libs.iterdir()) == ['guworld-1.2.0-all.jar', 'guworld-1.2.0-sources.jar',
                                                            'guworld-1.2.0.jar', 'other-1.0.0.jar']
    assert shared == {'problem': ''}, 'a shared state carries the outcome into the next run_stages call'
    assert check.partner_problem(check.Context(root, None, partner_state=shared), root) is None, 'built once'


class CheckTests(unittest.TestCase):

    def setUp(self):
        env.TEMP_ROOT.mkdir(parents=True, exist_ok=True)
        self.scratch = tempfile.TemporaryDirectory(dir=env.TEMP_ROOT)
        self.root = Path(self.scratch.name)

    def tearDown(self):
        self.scratch.cleanup()

    def test_stage_selection_and_the_project_table(self):
        assert check.select([], False) == list(check.STAGES)
        assert check.select([], True) == ['tools', 'build', 'gametest', 'data', 'server']
        assert check.select(['client', 'build'], False) == ['build', 'client']
        assert check.select(['multiplayer'], True) == []
        expect_exit(check.select, ['bogus'], False)
        gu_world = env.PROJECTS['guworld']
        assert check.select([], False, gu_world) == ['build', 'gametest', 'data']
        assert check.select(['data'], True, gu_world) == ['data']
        expect_exit(check.select, ['client'], False, gu_world)
        assert gu_world.root == env.GU_WORLD_ROOT and gu_world.temp == env.TEMP_ROOT.parent / 'guworld'
        assert env.GU_WORLD_ROOT.name == 'mcmod-guworld' or 'GZR_GU_WORLD' in os.environ
        assert f'-PguzhenrenDir={env.REPOSITORY_ROOT}' in env.gradle_command(gu_world.root, 'build')
        guzhenren = env.PROJECTS['guzhenren']
        assert guzhenren.partner == env.GU_WORLD_ROOT
        assert guzhenren.partner_stages == ('gametest', 'server', 'client', 'multiplayer')
        assert gu_world.partner == env.REPOSITORY_ROOT and gu_world.partner_name == 'Guzhenren'
        assert set(gu_world.partner_stages) == set(gu_world.stages), 'every Gu World stage loads Guzhenren'
        assert sorted(env.PROJECTS) == ['guworld', 'guzhenren']
        assert guzhenren.partner_name == 'Gu World'

    def test_log_patterns_and_client_readiness(self):
        assert check.SERVER_DONE.search('[23:29:23] [Server thread/INFO] [minecraft/DedicatedServer]: '
                                        'Done (4.113s)! For help, type "help"').group(1) == '4.113'
        assert check.GAMETEST_PASSED.search('GameTestServer/]: All 116 required tests passed :)').group(1) == '116'
        assert check.GAMETEST_FAILED.search('GameTestServer/]: 1 required tests failed :(').group(1) == '1'
        assert check.GAMETEST_FAILED_NAME.search(
            '[Server thread/INFO] [net.minecraft.gametest.framework.GameTestServer/]:    - hopeguseeks').group(1) \
            == 'hopeguseeks'
        report = check.GAMETEST_REPORT.search('[net.minecraft.gametest.framework.LogTestReporter/]: '
                                              'bearswipe failed at -9, -60, 13! Expected 9 got 8')
        assert report.groups() == ('bearswipe', 'Expected 9 got 8')
        assert check.CRASH.search('---- Minecraft Crash Report ----')
        assert check.CRASH.search('#@!@# Game crashed! Crash report saved to: #@!@# x')
        assert not check.CRASH.search('[Render thread/WARN] [Iris/]: Failed to resolve uniform')
        assert check.MOD_ERROR.search('[Server thread/ERROR] [guzhenren/]: boom')
        assert check.MOD_ERROR.search('[Render thread/ERROR] [guworld/]: boom')

        collector = check.Collector(check.TASK_FAILED)
        for line in ['> Task :compileJava FAILED', '* What went wrong:', "Execution failed for task ':compileJava'.",
                     '', '* Try:', '> Run with --stacktrace']:
            collector(line)
        assert check.failed_tasks(collector) == [':compileJava']
        assert check.went_wrong(collector) == ["Execution failed for task ':compileJava'."]

        ready = check.client_ready({})
        assert not ready('[Render thread/INFO] [SoundEngine/SOUNDS]: Sound engine started')
        assert ready('Created: 4096x2048x0 minecraft:textures/atlas/gui.png-atlas')
        assert not check.client_ready({})('Created: 512x512x0 minecraft:textures/atlas/mob_effects.png-atlas')

    def test_multiplayer_join_signals_and_commands(self):
        joined = check.JOINED.search('[Server thread/INFO] [minecraft/MinecraftServer]: Dev joined the game')
        assert joined.group(1) == 'Dev'
        lost = check.LOST.search('[Server thread/INFO] [minecraft/ServerGamePacketListenerImpl]: '
                                 'Dev lost connection: Disconnected')
        assert lost.groups() == ('Dev', 'Disconnected')
        assert not check.JOINED.search('[Server thread/INFO] [minecraft/MinecraftServer]: Dev left the game')
        options = check.client_options().splitlines()
        assert 'onboardAccessibility:false' in options and 'pauseOnLostFocus:false' in options
        commands = check.multiplayer_commands('Dev')
        assert [text.split()[0] for text, _ in commands] == ['gzr', 'guworld', 'guworld', 'list']
        assert commands[-1] == ('list', 'Dev'), 'the last command proves the player is still connected'

        class Scripted:
            def __init__(self, lines):
                self.lines = list(lines)

            def watch(self, until=None, fail=None, timeout=None):
                if not self.lines:
                    return 'timeout', None
                line = self.lines.pop(0)
                if line is None:
                    return 'exit', None
                if fail and fail(line):
                    return 'fail', line
                if until and until(line):
                    return 'until', line
                return 'timeout', None

        server = Scripted(['booting', ']: Dev joined the game'])
        assert check.watch_pair(server, Scripted([]), until=check.JOINED.search, timeout=5)[:2] == ('until', 'server')
        assert check.watch_pair(Scripted([]), Scripted([None]), timeout=5)[:2] == ('exit', 'client')
        dropped = check.watch_pair(Scripted([']: Dev lost connection: Timed out']), Scripted([]), timeout=5)
        assert dropped[:2] == ('fail', 'server')
        assert check.watch_pair(Scripted([]), Scripted([]), timeout=0.2) == ('timeout', None, None)

    def test_multiplayer_restores_the_ide_client_args(self):
        calls = []
        broken = check.Collector()
        broken.went_wrong = ["Execution failed for task ':prepareClientRun'."]

        def prepared(code):
            def run(ctx, *arguments, **kwargs):
                calls.append(arguments)
                return code, 0.0, broken if code else check.Collector()
            return run

        ctx = check.Context(self.root, None, project=env.PROJECTS['guzhenren'])
        with patch.object(check, 'gradle', prepared(0)):
            assert check.restore_client_args(ctx) == []
        assert calls == [('prepareClientRun',)], 'rewritten without the smoke run\'s -P properties'
        with patch.object(check, 'gradle', prepared(1)):
            stale = check.restore_client_args(ctx)
        assert 'quick-plays the stopped smoke server' in stale[0] and stale[1:] == broken.went_wrong, stale
        assert len(calls) == 2, calls

    def test_rcon_round_trip_and_server_properties(self):
        body = 'summon guzhenren:tiger ~ ~1 ~'
        packet = check.rcon_packet(7, 2, body)
        assert struct.unpack('<i', packet[:4])[0] == len(packet) - 4 and packet.endswith(b'\x00\x00')
        port, thread = fake_rcon_server(['Summoned new Tiger'])
        rcon = check.Rcon(port, 'secret')
        assert rcon.command(body) == 'Summoned new Tiger'
        rcon.close()
        thread.join(5)
        port, thread = fake_rcon_server([], reject_login=True)
        try:
            check.Rcon(port, 'wrong')
        except ConnectionError:
            pass
        else:
            raise AssertionError('a rejected RCON login must raise')
        thread.join(5)

        properties = check.server_properties(25601, 25602, 'pw')
        for key in ('online-mode=false', 'server-ip=127.0.0.1', 'server-port=25601', 'enable-rcon=true',
                    'rcon.port=25602', 'rcon.password=pw'):
            assert key in properties.splitlines(), key

    def test_smoke_commands_partner_guard_hashes_junit_log_and_lock(self):
        root = self.root
        lang = root / 'src/generated/resources/assets/guzhenren/lang/zh_cn.json'
        lang.parent.mkdir(parents=True)
        lang.write_text(json.dumps({'entity.guzhenren.tiger': '老虎', 'entity.guzhenren.bear': '熊',
                                    'entity.guzhenren.bear.hint': 'x', 'item.guzhenren.gu': '蛊'}), encoding='utf-8')
        gu_world = root / 'guworld'
        world = gu_world / 'src/generated/resources'
        features = world / 'data/guworld/worldgen/configured_feature'
        features.mkdir(parents=True)
        (features / 'spirit_spring.json').write_text('{}', encoding='utf-8')
        (world / 'assets/guworld/lang').mkdir(parents=True)
        (world / 'assets/guworld/lang/zh_cn.json').write_text(
            json.dumps({'dimension.guworld.treasure_yellow_heaven': '宝黄天'}), encoding='utf-8')
        (world / 'data/guworld/dimension').mkdir(parents=True)
        (world / 'data/guworld/dimension/treasure_yellow_heaven.json').write_text('{}', encoding='utf-8')
        own = root / 'src/generated/resources'
        assert check.mod_ids('guzhenren', own) == ['guzhenren:bear', 'guzhenren:tiger']
        assert check.mod_ids('guworld', world) == []
        assert check.mod_ids('absent', root / 'nowhere') == []
        assert check.data_ids('guworld', world, 'worldgen/configured_feature') == ['guworld:spirit_spring']
        assert check.data_ids('guzhenren', own, 'worldgen/configured_feature') == []
        assert check.generated_sources(root, gu_world) == [('guzhenren', own), ('guworld', world)]
        assert check.generated_sources(root)[1][1] == env.GU_WORLD_ROOT / 'src/generated/resources'
        commands = check.smoke_commands(root, gu_world)
        assert [text for text, _ in commands] == [
            'summon guzhenren:bear ~ ~1 ~', 'summon guzhenren:tiger ~ ~1 ~',
            'place feature guworld:spirit_spring ~ ~ ~',
            'execute in guworld:treasure_yellow_heaven run time query gametime']
        assert commands[-1][1] == 'The time is'
        missing = check.Context(root, None, project=env.PROJECTS['guzhenren'])
        assert check.partner_problem(missing, root / 'absent').startswith('Gu World not found at ')
        assert check.partner_problem(missing, gu_world) == missing.partner, 'the partner is built at most once per run'
        missing = check.Context(root, None, project=env.PROJECTS['guworld'])
        assert check.partner_problem(missing, root / 'absent').startswith('Guzhenren not found at ')
        skipped = check.stage_gametest(check.Context(root / 'absent', None, project=env.PROJECTS['guworld']))
        assert skipped.status == 'SKIP' and 'src/test/game' in skipped.detail, skipped
        assert f'-PguWorldDir={env.GU_WORLD_ROOT}' in env.gradle_command(root, 'build')

        generated = root / 'generated'
        (generated / '.cache').mkdir(parents=True)
        (generated / 'a.json').write_text('1', encoding='utf-8')
        (generated / '.cache' / 'x').write_text('ignored', encoding='utf-8')
        before = env.tree_hashes(generated)
        assert list(before) == ['a.json']
        (generated / 'a.json').write_text('2', encoding='utf-8')
        (generated / 'b.json').write_text('new', encoding='utf-8')
        assert env.diff_hashes(before, env.tree_hashes(generated)) == (['a.json'], ['b.json'], [])

        reports = root / 'build' / 'test-results'
        for task in ('pureTest', 'test', 'gone'):
            (reports / task).mkdir(parents=True)
            (reports / task / 'TEST-x.ATest.xml').write_text(
                '<testsuite name="x.ATest" tests="2" skipped="0" failures="0" errors="0"></testsuite>', encoding='utf-8')
        ran = check.Collector(check.TASK_RAN)
        for line in ('> Task :compileJava UP-TO-DATE', '> Task :pureTest', '> Task :test UP-TO-DATE',
                     '> Task :gametestClasses NO-SOURCE'):
            ran(line)
        assert {name: totals['tests'] for name, totals in check.junit_layers(root, ran).items()} == \
            {'pureTest': 2, 'test': 2}, 'a test task the build no longer runs leaves reports that do not count'
        results = root / 'results'
        results.mkdir()
        (results / 'TEST-x.FooTest.xml').write_text(
            '<?xml version="1.0"?><testsuite name="x.FooTest" tests="3" skipped="1" failures="1" errors="0">'
            '<testcase name="ok" classname="x.FooTest"/>'
            '<testcase name="bad" classname="x.FooTest"><failure message="expected &lt;9&gt; but was 8\nmore">'
            'trace</failure></testcase><testcase name="skip" classname="x.FooTest"><skipped/></testcase>'
            '</testsuite>', encoding='utf-8')
        totals = env.junit_totals(results)
        assert (totals['tests'], totals['failures'], totals['skipped']) == (3, 1, 1)
        assert totals['failed'] == [('FooTest.bad', 'expected <9> but was 8')]

        log = env.Log('probe', log_dir=root)
        log.write('first run')
        log.close()
        log = env.Log('probe', log_dir=root)
        log.write('second run')
        log.close()
        assert log.path.read_text(encoding='utf-8') == 'second run\n'

        lock_path = root / 'gradle.lock'
        lock_path.write_text('999999', encoding='utf-8')
        with env.Lock(lock_path):
            assert lock_path.read_text(encoding='utf-8') == str(os.getpid())
            lock_path_holder = env.Lock(lock_path)
            expect_exit(lock_path_holder.__enter__)
        assert lock_path.exists() and lock_path.read_text() == ''
        with env.Lock(lock_path):
            expect_exit(env.Lock(lock_path).__enter__)
        with env.Lock(lock_path):
            pass
        holder = lock_path.open('a+b')
        assert env.try_lock(holder)

        def release():
            env.unlock(holder)
            holder.close()
        threading.Timer(0.2, release).start()
        said = []
        with env.Lock(lock_path, wait=5, echo=lambda text, **_: said.append(text), poll=0.05):
            assert lock_path.read_text(encoding='utf-8') == str(os.getpid())
        assert len(said) == 1 and 'waiting for the Gradle lock' in said[0], said

    def test_tools_stage_runs_the_tests_in_parallel_processes(self):
        tools = self.root / 'tools'
        tools.mkdir()
        # An export sits elsewhere: the tests must still find this workstation's Temp, partner, wiki and models.
        (tools / 'test_scratch.py').write_text(
            'import os, unittest\n'
            'class T(unittest.TestCase):\n'
            f'    def test_pinned(self): self.assertEqual(os.environ["GZR_TEMP"], {str(env.TEMP_ROOT)!r})\n'
            '    def test_alone(self): self.assertNotIn("shared", globals()); globals()["shared"] = 1\n'
            '    def test_alone_too(self): self.assertNotIn("shared", globals()); globals()["shared"] = 1\n'
            '    @unittest.skip("skipped")\n'
            '    def test_skipped(self): pass\n', encoding='utf-8')
        log = env.Log('tools', self.root)
        ctx = check.Context(self.root, log)
        # A slow module runs one process per test, so the two globals() tests cannot see each other.
        with patch.object(check, 'SLOW_TOOL_MODULES', ('test_scratch',)):
            passed = check.stage_tools(ctx)
        assert (passed.status, passed.detail) == ('PASS', '4 tool tests passed'), passed
        assert check.tool_shards(['a.T.x', 'test_commit_push.T.y', 'a.T.z', 'test_commit_push.T.w']) == \
            [['test_commit_push.T.y'], ['test_commit_push.T.w'], ['a.T.x', 'a.T.z']]
        (tools / 'test_scratch.py').write_text(
            'import unittest\nclass T(unittest.TestCase):\n    def test_red(self): self.fail("red")\n'
            '    def test_green(self): pass\n    def test_error(self): raise OSError\n', encoding='utf-8')
        failed = check.stage_tools(ctx)
        assert (failed.status, failed.detail) == ('FAIL', '2 of 3 tool tests failed'), failed
        assert failed.notes == ['test_scratch.T.test_error', 'test_scratch.T.test_red'], failed.notes
        (tools / 'test_broken.py').write_text('import not_a_module\n', encoding='utf-8')
        broken = check.stage_tools(ctx)
        log.close()
        assert (broken.status, broken.detail) == ('FAIL', 'tool test discovery failed'), broken
        assert any('test_broken' in note for note in broken.notes), broken.notes
        assert check.select(['tools'], False) == ['tools']
        expect_exit(check.select, ['tools'], False, env.PROJECTS['guworld'])

    def test_a_verification_run_gets_its_own_partner_and_game_directories(self):
        log = env.Log('verify', self.root)
        partner, games = self.root / 'partner', self.root / 'games'
        ctx = check.Context(self.root, log, partner_root=partner, game_root=games)
        command, _ = command_of(check.stage_data, ctx)
        log.close()
        assert f'-PguWorldDir={partner}' in command and f'-PguzhenrenDir={self.root}' in command, command
        assert f'-PdataGameDir={games / "data"}' in command and f'-PgameTestDir={games / "gametest"}' in command

    def test_gametest_takes_the_wiki_gate_like_build(self):
        (self.root / 'src' / 'test' / 'game').mkdir(parents=True)
        log = env.Log('gametest', self.root)
        for wiki, expected in ((self.root / 'wiki', True), (None, False)):
            command, _ = command_of(check.stage_gametest, check.Context(self.root, log, wiki))
            assert (f'-PwikiDir={self.root / "wiki"}' in command) == expected, command
        log.close()

    def test_projects(self):
        project_checks(self.root)

    def test_game_jvms_and_small_helpers(self):
        repo = env.REPOSITORY_ROOT
        vm_args = repo / 'build' / 'moddev' / 'clientRunVmArgs.txt'
        processes = {
            1: f'java -Dfml.modFolders=guzhenren%%{repo}/build/classes/java/main @{vm_args} -Dfile.encoding=UTF-8',
            2: f'java -Xmx64m -jar {repo}/gradle/wrapper/gradle-wrapper.jar runClient',
            3: 'java -cp gradle-daemon-main.jar org.gradle.launcher.daemon.bootstrap.GradleDaemon 9.7.1',
            4: 'java @/other/project/build/moddev/clientRunVmArgs.txt',
            # How Windows launches it: backslashes, a quoted path, a drive letter in another case.
            5: f'"C:\\jdk\\bin\\java.exe" "@{str(vm_args).swapcase()}"',
        }
        assert env.game_jvms(processes, repo) == [1, 5]

        assert env.decode_line('熊'.encode('utf-8')) == '熊'
        assert env.decode_line(b'\xff') == '\ufffd'
        assert env.format_seconds(130.4) == '2m10s'


if __name__ == '__main__':
    unittest.main()
