"""Windows integration checks for paths, process ownership and exporter output safety."""
import json
import os
import signal
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from pathlib import Path
from unittest.mock import patch

import bbmodel_to_geckolib as exporter
import local_checks as check
import project_runtime as env


SCRATCH = env.TEMP_ROOT / 'test_project_runtime'
IDLE_JAVA = 'public class Idle { public static void main(String[] a) throws Exception { Thread.sleep(60_000); } }\n'


def tearDownModule():
    # Each test cleans its own folder; the shared parent goes once it is empty.
    try:
        SCRATCH.rmdir()
    except OSError:
        pass


def end(process):
    if process.poll() is None:
        process.kill()
    process.wait()


class WindowsToolTests(unittest.TestCase):

    def setUp(self):
        SCRATCH.mkdir(parents=True, exist_ok=True)
        self.scratch = tempfile.TemporaryDirectory(dir=SCRATCH)
        self.root = Path(self.scratch.name)
        # Registered first, so it runs last: Windows cannot delete a held lock file or a running process's folder.
        self.addCleanup(self.scratch.cleanup)

    def hold(self, path, text=''):
        """Holds the Gradle lock byte of `path` the way another run would; returns its release."""
        handle = path.open('a+b')
        if text:
            handle.write(text.encode('utf-8'))
            handle.flush()
        self.assertTrue(env.try_lock(handle), f'{path} was already locked')

        def release():
            if not handle.closed:
                env.unlock(handle)
                handle.close()
        self.addCleanup(release)
        return release

    def start(self, command, cwd):
        process = subprocess.Popen(command, cwd=cwd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        self.addCleanup(end, process)
        return process

    def test_default_paths_keep_checks_in_workspace_temps(self):
        self.assertEqual(env.TEMP_ROOT, Path.home() / 'Workspace' / 'Temps' / 'guzhenren')
        self.assertEqual(env.GU_WORLD_ROOT, Path.home() / 'Workspace' / 'Projects' / 'mcmod-guworld')

    def test_projects_are_the_two_main_checkouts(self):
        main = env.project('guzhenren')
        self.assertIs(main, env.PROJECTS['guzhenren'])
        self.assertEqual((main.root, main.temp, main.ci_jar_dir), (env.REPOSITORY_ROOT, env.TEMP_ROOT, 'run/mods'))
        guworld = env.project('guworld')
        self.assertEqual((guworld.root, guworld.temp, guworld.ci_jar_dir),
                         (env.GU_WORLD_ROOT, env.TEMP_ROOT.parent / 'guworld', None))
        self.assertEqual(len(env.all_projects()), 2)
        with self.assertRaises(KeyError):
            env.project('nope')

    def test_partner_is_the_other_mod(self):
        for owner in env.all_projects():
            other = env.partner_of(owner)
            self.assertNotEqual(other.name, owner.name)
            self.assertEqual(other.root, owner.partner, owner.root)
            self.assertEqual(env.partner_of(other), owner)
        self.assertEqual(env.partner_of(env.PROJECTS['guworld']), env.PROJECTS['guzhenren'])

    def test_gradle_command_hands_each_project_its_partner(self):
        roots = {'guzhenren': env.REPOSITORY_ROOT, 'guworld': env.GU_WORLD_ROOT}
        for name, root in roots.items():
            owner = env.project(name)
            command = env.gradle_command(owner.root, 'build')
            self.assertEqual(command[:2], [str(root / 'gradlew.bat'), 'build'], name)
            self.assertIn(f'-PguWorldDir={roots["guworld"]}', command, name)
            self.assertIn(f'-PguzhenrenDir={roots["guzhenren"]}', command, name)
            self.assertIn(f'-PgameTestDir={owner.temp / "gametest"}', command)
            self.assertIn(f'-PdataGameDir={owner.temp / "data"}', command)
            self.assertEqual(command[-3:], ['--daemon', f'-Dorg.gradle.daemon.idletimeout={env.DAEMON_IDLE_MS}',
                                            '--console=plain'])
            self.assertEqual(env.gradle_command(owner.root, 'runClient', daemon=False)[-2:],
                             ['--no-daemon', '--console=plain'], 'a run whose game JVM the caller owns')
        self.assertEqual(env.mod_roots(env.project('guworld')), (roots['guworld'], roots['guzhenren']))

    def test_gradle_command_for_an_export_root_keeps_the_partner_of_the_given_project(self):
        export = self.root / 'export'
        export.mkdir()
        guzhenren, guworld = env.project('guzhenren'), env.project('guworld')
        command = env.gradle_command(export, 'build', project=guzhenren)
        self.assertIn(f'-PguzhenrenDir={export}', command)
        self.assertIn(f'-PguWorldDir={guworld.root}', command)
        self.assertIn(f'-PgameTestDir={guzhenren.temp / "gametest"}', command)
        command = env.gradle_command(export, 'build', project=guworld)
        self.assertIn(f'-PguWorldDir={export}', command)
        self.assertIn(f'-PguzhenrenDir={guzhenren.root}', command)

    def test_project_at_finds_checkouts_exports_and_falls_back_to_guzhenren(self):
        for owner in env.all_projects():
            self.assertIs(env.project_at(owner.root), owner)
        export = self.root / 'export'
        export.mkdir()
        self.assertIs(env.project_at(export), env.PROJECTS['guzhenren'])
        (export / 'gradle.properties').write_text('mod_id=guworld\n')
        self.assertIs(env.project_at(export), env.PROJECTS['guworld'])
        (export / 'gradle.properties').write_text('mod_id=other\n')
        self.assertIs(env.project_at(export), env.PROJECTS['guzhenren'])

    def test_game_jvms_match_the_moddev_launch_file(self):
        repo = env.REPOSITORY_ROOT
        moddev = f'java -Dfml.modFolders=guzhenren%%{repo}/build/classes/java/main @{repo}/build/moddev/clientRunVmArgs.txt'
        processes = {1: moddev, 3: f'java -jar {repo}/gradle/wrapper/gradle-wrapper.jar runClient',
                     4: 'java @/other/build/moddev/clientRunVmArgs.txt',
                     5: f'"C:\\jdk\\bin\\java.exe" "@{repo}\\build\\moddev\\serverRunVmArgs.txt"'.upper()}
        self.assertEqual(env.game_jvms(processes, repo), [1, 5])
        self.assertEqual(env.game_jvms(processes, '/other'), [4])

    def test_java_inventory_reads_command_line_working_directory_and_exe(self):
        game, other, spaced = self.root / 'server-smoke', self.root / 'run', self.root / 'project with spaces'
        for folder in (game, other, spaced):
            folder.mkdir()
        source = self.root / 'Idle.java'
        source.write_text(IDLE_JAVA, encoding='utf-8')
        java = Path(env.runtime_environment()['JAVA_HOME']) / 'bin' / 'java.exe'
        launch = f'@{spaced / "build" / "moddev" / "clientRunVmArgs.txt"}'
        jvm = self.start([str(java), str(source), launch], game)
        bystander = self.start([sys.executable, '-c', 'import time; time.sleep(60)'], game)

        found = env.java_processes()
        self.assertIn(jvm.pid, found)
        self.assertIn('Idle.java', found[jvm.pid])
        self.assertNotIn(bystander.pid, found, 'a non-Java process counted as a JVM')
        self.assertEqual(env.game_jvms({jvm.pid: found[jvm.pid]}, spaced), [jvm.pid])
        self.assertEqual(env.java_processes_in(game), [jvm.pid])
        self.assertNotIn(jvm.pid, env.java_processes_in(other))
        self.assertTrue(env.same_path(env.java_process_dirs()[jvm.pid], game))
        self.assertTrue(env.same_path(env.process_exe(jvm.pid), java))

    def test_gradle_wrapper_gets_every_argument_intact_in_spaced_directory(self):
        root = self.root / 'project with spaces'
        root.mkdir()
        (root / 'gradlew.bat').write_text(
            f'@"{sys.executable}" -c "import sys; print(chr(10).join(sys.argv[1:]))" %*\r\n', encoding='utf-8')
        result = subprocess.run(env.gradle_command(root, 'build'), cwd=root, capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        lines = result.stdout.splitlines()
        self.assertIn('build', lines)
        self.assertIn(f'-PguWorldDir={env.GU_WORLD_ROOT}', lines)
        self.assertIn(f'-PguzhenrenDir={root}', lines)
        self.assertIn('--daemon', lines)

    def test_per_run_log_carries_its_pid_and_prunes_only_old_logs_of_ended_runs(self):
        ended = subprocess.Popen([sys.executable, '-c', 'pass'])
        ended.wait()
        old_ended = self.root / f'tool-{ended.pid}.log'
        fresh_ended = self.root / f'tool-{ended.pid + 1}.log'
        old_running = self.root / f'tool-{os.getppid()}.log'
        other_tool = self.root / f'other-{ended.pid}.log'
        for path in (old_ended, fresh_ended, old_running, other_tool):
            path.write_text('kept evidence', encoding='utf-8')
        two_days_ago = time.time() - 2 * 24 * 3600
        for path in (old_ended, old_running, other_tool):
            os.utime(path, (two_days_ago, two_days_ago))
        with patch.object(env, 'pid_alive', side_effect=lambda pid: pid == os.getppid()):
            log = env.Log('tool', self.root, per_run=True)
        log.close()
        self.assertEqual(log.path, self.root / f'tool-{os.getpid()}.log')
        self.assertFalse(old_ended.exists())
        self.assertTrue(fresh_ended.exists())
        self.assertTrue(old_running.exists())
        self.assertTrue(other_tool.exists())

    def test_process_supplies_jdk21_and_utf8_and_temporary_directory(self):
        log = env.Log('process', self.root)
        names = ['JAVA_HOME', 'TMPDIR', 'TMP', 'TEMP']
        script = f'import os,json; print(json.dumps({{k:os.environ.get(k) for k in {names}}})); print("中文")'
        process = env.Process([sys.executable, '-c', script], self.root, log)
        try:
            self.assertEqual(process.wait(timeout=10), 0)
        finally:
            end(process.proc)
            log.close()
        lines = log.path.read_text(encoding='utf-8').splitlines()
        values = json.loads(lines[0])
        java_home = Path(values['JAVA_HOME'])
        self.assertRegex((java_home / 'release').read_text(encoding='utf-8'), r'JAVA_VERSION="21[."]')
        self.assertTrue((java_home / 'bin' / 'javac.exe').is_file())
        for name in ('TMPDIR', 'TMP', 'TEMP'):
            self.assertEqual(values[name], str(env.TEMP_ROOT / 'tmp'), name)
        self.assertEqual(lines[1], '中文')

    def test_pid_probe_recognizes_live_and_ended_processes(self):
        process = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(30)'])
        try:
            self.assertTrue(env.pid_alive(process.pid))
            self.assertTrue(env.pid_alive(os.getpid()))
        finally:
            process.kill()
            process.wait()
        # Popen still holds the ended process's handle, so its PID cannot have been reused yet.
        self.assertFalse(env.pid_alive(process.pid))
        self.assertFalse(env.pid_alive(-1))

    def test_descendants_skip_a_child_older_than_the_pid_it_names(self):
        # 13 names PID 10 as its parent but predates the current 10: its real parent ended and 10 was reused.
        table = {10: (1, 'cmd.exe'), 11: (10, 'java.exe'), 12: (11, 'java.exe'), 13: (10, 'old.exe'),
                 14: (99, 'other.exe')}
        born = {10: 100, 11: 110, 12: 120, 13: 50, 14: 10}
        with patch.object(env, 'process_table', return_value=table), \
                patch.object(env, '_created', side_effect=born.get):
            self.assertEqual(sorted(env.descendant_pids(10)), [11, 12])

    def test_kill_tree_stops_its_child_and_preserves_unrelated_process(self):
        unrelated = self.start([sys.executable, '-c', 'import time; time.sleep(60)'], self.root)
        log = env.Log('tree', self.root)
        script = ('import subprocess,sys,time; '
                  'child=subprocess.Popen([sys.executable,"-c","import time; time.sleep(60)"]); '
                  'print(child.pid,flush=True); time.sleep(60)')
        process = env.Process([sys.executable, '-c', script], self.root, log)
        child = None
        try:
            outcome, line = process.watch(until=lambda _: True, timeout=10)
            self.assertEqual(outcome, 'until')
            child = int(line)
            self.assertIn(child, env.descendant_pids(process.proc.pid))
            process.kill_tree()
            self.assertIsNotNone(process.proc.poll())
            self.assertFalse(env.pid_alive(child), 'a game descendant survived cleanup')
            self.assertIsNone(unrelated.poll(), 'cleanup killed an unrelated process')
        finally:
            for pid in [child, process.proc.pid]:
                if pid:
                    try:
                        os.kill(pid, signal.SIGTERM)
                    except OSError:
                        pass
            process.proc.wait()
            log.close()

    def test_explicit_wrong_jdk_is_rejected_before_launch(self):
        self.assertTrue(hasattr(env, 'runtime_environment'), 'the launch environment must validate the JDK')
        wrong = self.root / 'jdk25'
        (wrong / 'bin').mkdir(parents=True)
        (wrong / 'bin' / 'java.exe').touch()
        (wrong / 'bin' / 'javac.exe').touch()
        (wrong / 'release').write_text('JAVA_VERSION="25.0.4"\n')
        with self.assertRaises(SystemExit):
            env.runtime_environment({'GZR_JAVA_HOME': str(wrong)})

    def test_failed_export_preserves_existing_file_without_repo_temporary_file(self):
        target = self.root / 'output/result.json'
        target.parent.mkdir()
        target.write_text('original')
        with patch.object(Path, 'replace', side_effect=OSError('rename failed')):
            with self.assertRaises(OSError):
                exporter.write_json(target, {'answer': 42})
        self.assertEqual(target.read_text(), 'original')
        self.assertEqual(list(target.parent.glob('*.tmp')), [])

    def test_exporters_honor_model_authority_override(self):
        models = self.root / 'model authority'
        script = ('import bbmodel_to_geckolib, json; '
                  'print(json.dumps([str(m.source_model) for m in bbmodel_to_geckolib.MODELS.values()]))')
        environment = os.environ.copy()
        environment['GZR_MODELS'] = str(models)
        result = subprocess.run([sys.executable, '-B', '-c', script], cwd=env.REPOSITORY_ROOT / 'tools',
                                env=environment, capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        for path in json.loads(result.stdout):
            self.assertTrue(Path(path).is_relative_to(models), path)

    def test_stale_pid_metadata_does_not_prevent_lock_acquisition(self):
        path = self.root / 'gradle.lock'
        path.write_text(str(os.getpid()))
        try:
            with env.Lock(path):
                with self.assertRaises(SystemExit):
                    with env.Lock(path):
                        self.fail('a second owner acquired the same lock')
        except SystemExit as error:
            self.fail(f'stale PID metadata blocked an unowned lock: {error}')
        with env.Lock(path):
            pass

    def test_lock_waiter_takes_the_lock_soon_after_release(self):
        path = self.root / 'queue.lock'
        release = self.hold(path)
        holder_release = threading.Timer(0.3, release)
        holder_release.start()
        started = time.monotonic()
        try:
            with env.Lock(path, wait=10, echo=lambda *a, **k: None, poll=0.05):
                waited = time.monotonic() - started
        finally:
            holder_release.join()
        self.assertGreaterEqual(waited, 0.25, 'took a lock that was still held')
        self.assertLess(waited, 2.0, f'the tool waited {waited:.1f}s after the release')

    def test_lock_wait_still_gives_up_at_the_deadline(self):
        path = self.root / 'deadline.lock'
        self.hold(path, '4242')
        started = time.monotonic()
        with self.assertRaises(SystemExit) as caught:
            with env.Lock(path, wait=0.5, echo=lambda *a, **k: None):
                self.fail('acquired a held lock')
        self.assertIn('4242', str(caught.exception))
        self.assertLess(time.monotonic() - started, 3.0)

    def test_lock_rejection_preserves_existing_check_and_ship_logs(self):
        for name, arguments in [('local_checks', ['build']),
                                ('commit_push', [str(self.root / 'plan.json'), '--dry-run'])]:
            project_temp = self.root / name
            (project_temp / 'logs').mkdir(parents=True)
            logfile = project_temp / 'logs' / f'{name}.log'
            logfile.write_text('active run evidence\n')
            (self.root / 'plan.json').write_text(json.dumps({'commits': [{'title': 'chore: test', 'files': ['a']}] }))
            self.hold(project_temp / 'gradle.lock', str(os.getpid()))
            environment = dict(os.environ, GZR_TEMP=str(project_temp), GZR_LOCK_WAIT='0')
            result = subprocess.run([sys.executable, '-B', str(env.REPOSITORY_ROOT / 'tools' / f'{name}.py'),
                                     *arguments], env=environment, capture_output=True, text=True)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn('another local_checks/commit_push run', result.stderr)
            self.assertEqual(logfile.read_text(), 'active run evidence\n', name)

    def test_hand_run_command_holds_the_shared_lock_and_returns_its_exit_code(self):
        temp = self.root / 'manual'
        temp.mkdir()
        lockfile = temp / 'gradle.lock'
        environment = dict(os.environ, GZR_TEMP=str(temp), GZR_LOCK_WAIT='0')
        # The command reads the lock file while its runner holds the lock: the holder's PID stays readable.
        probe = [sys.executable, '-c', 'import pathlib,sys; print(pathlib.Path(sys.argv[1]).read_text()); sys.exit(3)',
                 str(lockfile)]
        runner = [sys.executable, '-B', str(env.REPOSITORY_ROOT / 'tools' / 'project_runtime.py'), *probe]
        result = subprocess.run(runner, env=environment, capture_output=True, text=True)
        self.assertEqual(result.returncode, 3, result.stderr)
        self.assertTrue(result.stdout.strip().isdigit(), result.stdout)
        self.assertEqual(lockfile.read_text(), '', 'the runner left its PID behind')
        self.hold(lockfile, '4242')
        result = subprocess.run(runner, env=environment, capture_output=True, text=True)
        self.assertNotEqual(result.returncode, 3, 'the command ran without the lock')
        self.assertIn('4242', result.stderr)

    def test_interruption_cleans_owned_process_before_releasing_lock(self):
        self.assertTrue(hasattr(env.Process, '__exit__'), 'owned processes need exception-safe cleanup')
        log = env.Log('interrupt', self.root)
        process = env.Process([sys.executable, '-c', 'import time; time.sleep(60)'], self.root, log)
        try:
            with self.assertRaises(KeyboardInterrupt):
                with env.Lock(self.root / 'interrupt.lock'):
                    with process:
                        raise KeyboardInterrupt()
            self.assertIsNotNone(process.proc.poll())
            with env.Lock(self.root / 'interrupt.lock'):
                self.assertFalse(env.pid_alive(process.proc.pid))
        finally:
            end(process.proc)
            log.close()

    def test_gradle_timeout_is_total_even_when_output_keeps_arriving(self):
        wrapper = self.root / 'gradlew.bat'
        wrapper.write_text(f'@"{sys.executable}" -c "import time; '
                           '[(print(\'tick\', flush=True), time.sleep(0.01)) for _ in range(300)]"\r\n',
                           encoding='utf-8')
        log = env.Log('timeout', self.root)
        try:
            code, seconds, _ = check.gradle(check.Context(self.root, log), 'build', timeout=0.1)
        finally:
            log.close()
        self.assertIsNone(code, 'streaming output reset the overall timeout')
        self.assertLess(seconds, 0.8)

    def test_client_cleanup_excludes_user_started_game(self):
        class FakeProcess:
            proc = type('Child', (), {'pid': 101})()
            def watch(self, **kwargs): return 'fail', 'startup failed'
            def kill_tree(self): pass
            def __enter__(self): return self
            def __exit__(self, *args): pass
        command = 'java @/checkout/build/moddev/clientRunVmArgs.txt'
        calls = []
        log = env.Log('client', self.root)
        try:
            with patch.object(env, 'Process', return_value=FakeProcess()), \
                 patch.object(env, 'java_processes', side_effect=[{}, {102: command, 103: command}, {}]), \
                 patch.object(env, 'descendant_pids', return_value=[102]), \
                 patch.object(env, 'kill_pids', side_effect=lambda pids: calls.extend(pids)):
                check.stage_client(check.Context(Path('/checkout'), log))
        finally:
            log.close()
        self.assertEqual(calls, [102], 'a user game outside the owned process tree was selected for termination')

    def test_client_skips_when_a_game_from_the_checkout_runs(self):
        log = env.Log('client', self.root)
        game = 'java @/checkout/build/moddev/clientRunVmArgs.txt'
        try:
            with patch.object(env, 'java_processes', return_value={7: game}):
                result = check.stage_client(check.Context(Path('/checkout'), log))
        finally:
            log.close()
        self.assertEqual(result.status, 'SKIP')
        self.assertIn('[7]', result.detail)

    def test_failed_temp_write_cleans_file_and_preserves_output(self):
        import tempfile as temporary_module
        original = temporary_module.NamedTemporaryFile
        created = []
        class BrokenWriter:
            def __init__(self, **kwargs):
                self.handle = original(**kwargs); self.name = self.handle.name; created.append(Path(self.name))
            def __enter__(self): return self
            def write(self, data): raise OSError('disk full')
            def __exit__(self, *args): self.handle.close()
        target = self.root / 'result.json'; target.write_text('original')
        with patch.object(temporary_module, 'NamedTemporaryFile', side_effect=BrokenWriter):
            with self.assertRaises(OSError):
                exporter.write_json(target, {'answer': 42})
        self.assertEqual(target.read_text(), 'original')
        self.assertTrue(created)
        try:
            self.assertFalse(any(path.exists() for path in created), 'failed writes left temporary files behind')
        finally:
            for path in created: path.unlink(missing_ok=True)


    def test_empty_wiki_does_not_enable_missing_document_gate(self):
        self.assertTrue(hasattr(env, 'wiki_available'), 'wiki availability must distinguish an empty destination')
        wiki = self.root / 'wiki'; wiki.mkdir()
        self.assertFalse(env.wiki_available(wiki))
        (wiki / 'README.md').write_text('Project notes')
        self.assertTrue(env.wiki_available(wiki))


if __name__ == '__main__':
    unittest.main(verbosity=2)
