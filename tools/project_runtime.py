"""Linux plumbing for local_checks.py and commit_push.py: project paths, logs and owned processes.

The tools use bash and each repository's Gradle wrapper with JDK 21. Temporary files live under
Workspace/Temps; GZR_TEMP, GZR_GU_WORLD, GZR_WIKI, GZR_MODELS and GZR_JAVA_HOME override local paths.
One Gradle lock covers every project, so parallel sessions never build the same folder at once: a run queues
for it up to GZR_LOCK_WAIT seconds (default an hour).
"""
import fcntl
import hashlib
import html
import os
import queue
import shlex
import signal
import re
import shutil
import stat
import subprocess
import sys
import threading
import time
from dataclasses import dataclass
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
WORKSPACE_ROOT = REPOSITORY_ROOT.parent.parent
GU_WORLD_ROOT = Path(os.environ.get('GZR_GU_WORLD') or REPOSITORY_ROOT.parent / 'mcmod-guworld').expanduser().resolve()
TEMP_ROOT = Path(os.environ.get('GZR_TEMP') or WORKSPACE_ROOT / 'Temps' / 'guzhenren').expanduser().resolve()
LOG_DIR = TEMP_ROOT / 'logs'
WIKI_ROOT = Path(os.environ.get('GZR_WIKI') or WORKSPACE_ROOT / 'Notes' / 'wiki-guzhenren-mod').expanduser().resolve()
MODELS_ROOT = Path(os.environ.get('GZR_MODELS') or REPOSITORY_ROOT.parent / 'assets' / 'blockbench-gzr').expanduser().resolve()
EXIT = object()
# How long local_checks.py and commit_push.py queue behind another run's Gradle lock before giving up.
LOCK_WAIT = float(os.environ.get('GZR_LOCK_WAIT') or 3600)
DAEMON_IDLE_MS = 600_000


@dataclass(frozen=True)
class Project:
    """A repository the tools drive: where it lives, where its temporary files go, which local_checks.py
    stages it has, which of them its GitHub workflow runs (ci_stages), which local stages the commit_push
    mirror adds (local_stages), whether its build takes the wiki gate (wiki_gate), and the other mod its runs
    load (partner, named partner_name), whose jar local_checks.py builds before the first of partner_stages.
    ci_jar_dir is where the jars its workflow downloads land in this checkout (None: they land in the partner's
    checkout)."""
    name: str
    root: Path
    temp: Path
    stages: tuple
    ci_stages: tuple
    local_stages: tuple
    wiki_gate: bool
    partner: Path = None
    partner_name: str = ''
    partner_stages: tuple = ()
    ci_jar_dir: str = 'run/mods'


PROJECTS = {
    'guzhenren': Project('guzhenren', REPOSITORY_ROOT, TEMP_ROOT,
                         ('build', 'gametest', 'data', 'server', 'client', 'multiplayer'), ('build', 'data'),
                         ('build', 'gametest'), True,
                         GU_WORLD_ROOT, 'Gu World', ('gametest', 'server', 'client', 'multiplayer')),
    'guworld': Project('guworld', GU_WORLD_ROOT, TEMP_ROOT.parent / 'guworld',
                       ('build', 'gametest', 'data'), ('build', 'data'), ('gametest',), False,
                       REPOSITORY_ROOT, 'Guzhenren', ('build', 'gametest', 'data'), ci_jar_dir=None),
}
# Gu World's local tests are GameTests alone, so its mirror adds only those to its CI build.


def project(name):
    return PROJECTS[name]


def all_projects():
    return list(PROJECTS.values())


def partner_of(owner):
    """The other mod."""
    return next(other for other in all_projects() if other.name != owner.name)


def mod_roots(owner, root=None):
    """(Gu World root, Guzhenren root) for a run of `owner` from `root` (its checkout or an export of it)."""
    root = Path(root or owner.root)
    return (owner.partner, root) if owner.name == 'guzhenren' else (root, owner.partner)


def require_linux():
    if not sys.platform.startswith('linux'):
        raise SystemExit("These tools run on Linux; use this workstation's Python 3.")
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, 'reconfigure'):
            stream.reconfigure(encoding='utf-8')


def decode_line(raw):
    return raw.decode('utf-8', errors='replace')


def runtime_environment(overrides=None):
    environment = os.environ.copy()
    environment.update(overrides or {})
    configured = environment.get('GZR_JAVA_HOME') or environment.get('JAVA_HOME')
    if configured:
        candidates = [Path(configured).expanduser()]
    else:
        candidates = []
        for folder in [Path.home() / '.local/share/jdks', Path.home() / '.jdks', Path('/usr/lib/jvm')]:
            if folder.is_dir():
                candidates.extend(sorted(folder.iterdir()))
    for candidate in candidates:
        try:
            release = (candidate / 'release').read_text(encoding='utf-8')
        except OSError:
            continue
        if (re.search(r'^JAVA_VERSION="21(?:[.+-]|"|$)', release, re.M)
                and (candidate / 'bin/java').is_file() and (candidate / 'bin/javac').is_file()):
            java_home = candidate.resolve()
            break
    else:
        raise SystemExit('JDK 21 with javac is required; set GZR_JAVA_HOME to its installation directory'
                         + (f' (configured: {configured})' if configured else ''))
    temporary = TEMP_ROOT / 'tmp'
    temporary.mkdir(parents=True, exist_ok=True)
    environment['JAVA_HOME'] = str(java_home)
    environment['PATH'] = str(java_home / 'bin') + os.pathsep + environment.get('PATH', '')
    environment['TMPDIR'] = str(temporary)
    environment['PYTHONDONTWRITEBYTECODE'] = '1'
    java_options = environment.get('JAVA_TOOL_OPTIONS', '')
    environment['JAVA_TOOL_OPTIONS'] = (java_options + f' -Djava.io.tmpdir="{temporary}"').strip()
    return environment


def wiki_available(root=None):
    root = Path(root) if root is not None else WIKI_ROOT
    return root.is_dir() and any(root.iterdir())


def format_seconds(seconds):
    seconds = int(round(seconds))
    return f'{seconds // 60}m{seconds % 60:02d}s'


class Log:
    """One log file per tool, overwritten on every run, so a forgotten log never piles up.

    A per-run log (`per_run=True`) is `<name>-<pid>.log` instead: parallel sessions running the same tool never
    overwrite or delete each other's. Each per-run log prunes the ones of ended runs older than a day, so a failed
    run keeps its evidence for that long and forgotten ones still go."""

    STALE_SECONDS = 24 * 3600

    def __init__(self, name, log_dir=None, per_run=False):
        directory = Path(log_dir or LOG_DIR)
        directory.mkdir(parents=True, exist_ok=True)
        if per_run:
            self._prune_ended_runs(directory, name)
        self.path = directory / (f'{name}-{os.getpid()}.log' if per_run else f'{name}.log')
        self._handle = open(self.path, 'w', encoding='utf-8', newline='\n')

    @classmethod
    def _prune_ended_runs(cls, directory, name):
        cutoff = time.time() - cls.STALE_SECONDS
        for path in directory.glob(f'{name}-*.log'):
            pid = path.stem.removeprefix(f'{name}-')
            if not pid.isdigit() or pid_alive(int(pid)):
                continue
            try:
                if path.stat().st_mtime < cutoff:
                    path.unlink()
            except FileNotFoundError:
                pass

    def write(self, line=''):
        self._handle.write(line.rstrip('\r\n') + '\n')
        self._handle.flush()

    def section(self, title):
        self.write()
        self.write(f'===== {title} =====')

    def close(self):
        self._handle.close()


class Lock:
    """One kernel lock for all projects. Keep its inode in place while tools may be running.

    A held lock makes the caller wait up to `wait` seconds (0: give up at once), saying once whom it waits for.
    The wait is a blocking `flock(2)` under an alarm, never a poll: hand-run `flock gradle.lock <command>` waiters
    block in the kernel, and a poller that only retries every few seconds loses every release to them and starves."""

    class _Timeout(Exception):
        pass

    def __init__(self, path=None, wait=0, echo=print, poll=2.0):
        self.path = Path(path or TEMP_ROOT / 'gradle.lock')
        self.wait = wait
        self.echo = echo
        self.poll = poll
        self._handle = None

    def __enter__(self):
        self.path.parent.mkdir(parents=True, exist_ok=True)
        handle = self.path.open('a+', encoding='utf-8')
        deadline = time.monotonic() + self.wait
        told = False
        while True:
            try:
                fcntl.flock(handle, fcntl.LOCK_EX | fcntl.LOCK_NB)
                break
            except BlockingIOError:
                handle.seek(0)
                holder = handle.read().strip() or 'starting'
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    handle.close()
                    raise SystemExit(f'another local_checks/commit_push run (pid {holder}) is using Gradle; '
                                     'wait for it to finish') from None
                if not told:
                    self.echo(f'waiting for the Gradle lock (pid {holder}) ...', flush=True)
                    told = True
                if self._block(handle, remaining):
                    break
        try:
            handle.seek(0)
            handle.truncate()
            handle.write(str(os.getpid()))
            handle.flush()
        except BaseException:
            handle.close()
            raise
        self._handle = handle
        return self

    def _block(self, handle, remaining):
        """Blocks on the lock for up to `remaining` seconds; True once held. Off the main thread, where no alarm can
        interrupt the call, it sleeps one poll instead and reports False so the caller retries."""
        if threading.current_thread() is not threading.main_thread():
            time.sleep(min(self.poll, remaining))
            return False

        def expire(signum, frame):
            raise Lock._Timeout()

        previous = signal.signal(signal.SIGALRM, expire)
        signal.setitimer(signal.ITIMER_REAL, remaining)
        try:
            fcntl.flock(handle, fcntl.LOCK_EX)
            return True
        except Lock._Timeout:
            return False
        finally:
            signal.setitimer(signal.ITIMER_REAL, 0)
            signal.signal(signal.SIGALRM, previous)

    def __exit__(self, *exc):
        if self._handle is not None:
            try:
                self._handle.seek(0)
                self._handle.truncate()
                self._handle.flush()
            finally:
                self._handle.close()
                self._handle = None


def pid_alive(pid):
    if pid <= 0:
        return False
    try:
        state = Path(f'/proc/{pid}/stat').read_text().rsplit(')', 1)[1].split()[0]
        return state not in {'Z', 'X'}
    except FileNotFoundError:
        return False
    except PermissionError:
        return True


def descendant_pids(pid):
    parents = {}
    for entry in Path('/proc').iterdir():
        if not entry.name.isdigit():
            continue
        try:
            fields = (entry / 'stat').read_text().rsplit(')', 1)[1].split()
            parents[int(entry.name)] = int(fields[1])
        except (OSError, ValueError, IndexError):
            continue
    found = []
    pending = [pid]
    while pending:
        parent = pending.pop()
        children = [child for child, owner in parents.items() if owner == parent and child not in found]
        found.extend(children)
        pending.extend(children)
    return found


class Process:
    """A child process whose merged output is copied line by line into the log."""

    def __init__(self, command, cwd, log, env=None):
        self.log = log
        self.command = command
        self.proc = subprocess.Popen(
            command, cwd=cwd, env=runtime_environment(env), stdin=subprocess.DEVNULL, stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT, start_new_session=True)
        self._lines = queue.Queue()
        self._reader = threading.Thread(target=self._pump, daemon=True)
        self._reader.start()

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        if self.proc.poll() is None or self._reader.is_alive():
            self.kill_tree()
        self._reader.join(timeout=5)

    def _pump(self):
        with self.proc.stdout:
            for raw in iter(self.proc.stdout.readline, b''):
                line = decode_line(raw).rstrip('\r\n')
                self.log.write(line)
                self._lines.put(line)
        self._lines.put(EXIT)

    def watch(self, until=None, fail=None, timeout=None):
        """Read output until a line matches `until` or `fail`, the process exits, or time runs out.

        Returns (outcome, line): outcome is 'until', 'fail', 'exit' or 'timeout'.
        """
        deadline = None if timeout is None else time.monotonic() + timeout
        while True:
            remaining = None if deadline is None else deadline - time.monotonic()
            if remaining is not None and remaining <= 0:
                return 'timeout', None
            try:
                line = self._lines.get(timeout=remaining)
            except queue.Empty:
                return 'timeout', None
            if line is EXIT:
                self.proc.wait()
                return 'exit', None
            if fail and fail(line):
                return 'fail', line
            if until and until(line):
                return 'until', line

    def wait(self, timeout=None):
        outcome, _ = self.watch(timeout=timeout)
        return self.proc.returncode if outcome == 'exit' else None

    def kill_tree(self):
        children = descendant_pids(self.proc.pid)
        try:
            os.killpg(self.proc.pid, signal.SIGTERM)
        except ProcessLookupError:
            pass
        kill_pids(children)
        try:
            self.proc.wait(timeout=5)
        except subprocess.TimeoutExpired:
            try:
                os.killpg(self.proc.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            self.proc.wait(timeout=5)
        self._reader.join(timeout=5)


def project_at(root):
    """The project a folder is the checkout of; else, by its mod_id, that project; else Guzhenren's."""
    root = Path(root).resolve()
    found = next((owner for owner in all_projects() if owner.root == root), None)
    if found:
        return found
    properties = root / 'gradle.properties'
    if properties.is_file():
        mod_id = re.search(r'^mod_id=(\w+)$', properties.read_text(), re.M)
        if mod_id and mod_id.group(1) in PROJECTS:
            return PROJECTS[mod_id.group(1)]
    return PROJECTS['guzhenren']


def gradle_command(root, *arguments, project=None, daemon=True):
    """A Gradle invocation in `root` for `project` (default: the project at root), with its partner's checkout.
    A warm daemon saves each later invocation its JVM start and Gradle bootstrap (≈4 s); it serves both mods
    and stops after DAEMON_IDLE_MS idle. daemon=False starts a single-use daemon that ends with the build, for runs whose
    game JVM the caller must own and kill."""
    root = Path(root).resolve()
    owner = project or project_at(root)
    gu_world, guzhenren = mod_roots(owner, root)
    lifetime = ['--daemon', f'-Dorg.gradle.daemon.idletimeout={DAEMON_IDLE_MS}'] if daemon else ['--no-daemon']
    return ['bash', str(root / 'gradlew'), *arguments, f'-PguWorldDir={gu_world}',
            f'-PguzhenrenDir={guzhenren}', f'-PgameTestDir={owner.temp / "gametest"}',
            f'-PdataGameDir={owner.temp / "data"}', *lifetime, '--console=plain']


def git(*arguments, cwd=REPOSITORY_ROOT, env=None, check=True):
    result = subprocess.run(['git', *arguments], cwd=cwd, env=env, capture_output=True)
    stdout = result.stdout.decode('utf-8', errors='replace')
    stderr = result.stderr.decode('utf-8', errors='replace')
    if check and result.returncode != 0:
        raise RuntimeError(f'git {" ".join(arguments)} failed ({result.returncode}): {stderr.strip()}')
    return result.returncode, stdout, stderr


def java_processes(proc_root=Path('/proc')):
    """Running Java processes visible to this user, keyed by PID."""
    found = {}
    for entry in Path(proc_root).iterdir():
        if not entry.name.isdigit():
            continue
        try:
            if (entry / 'exe').readlink().name != 'java':
                continue
            arguments = (entry / 'cmdline').read_bytes().decode('utf-8', errors='replace').rstrip('\0').split('\0')
            if arguments and arguments[0]:
                found[int(entry.name)] = shlex.join(arguments)
        except OSError:
            continue
    return found


def java_processes_in(folder, proc_root=Path('/proc')):
    """PIDs of the Java processes whose working directory is `folder`."""
    folder = Path(folder).resolve()
    found = []
    for pid in java_processes(proc_root):
        try:
            if (Path(proc_root) / str(pid) / 'cwd').resolve(strict=True) == folder:
                found.append(pid)
        except OSError:
            continue
    return sorted(found)


def game_jvms(processes, root=REPOSITORY_ROOT):
    """Game JVMs launched from this checkout, by Gradle or by IDEA: both pass ModDevGradle's
    build/moddev/<run>RunVmArgs.txt, which the Gradle daemon and wrapper never do."""
    marker = str(Path(root) / 'build' / 'moddev') + '/'
    return sorted(pid for pid, command_line in processes.items()
                  if marker in command_line and 'RunVmArgs.txt' in command_line)


def kill_pids(pids):
    remaining = {pid for pid in pids if pid > 1 and pid != os.getpid() and pid_alive(pid)}
    for pid in remaining:
        try:
            os.kill(pid, signal.SIGTERM)
        except ProcessLookupError:
            pass
    deadline = time.monotonic() + 5
    while remaining and time.monotonic() < deadline:
        remaining = {pid for pid in remaining if pid_alive(pid)}
        if remaining:
            time.sleep(0.05)
    for pid in remaining:
        try:
            os.kill(pid, signal.SIGKILL)
        except ProcessLookupError:
            pass


def tree_hashes(root, skip_dir='.cache'):
    """{relative posix path: sha256} for every file under root, skipping datagen cache folders."""
    root = Path(root)
    hashes = {}
    if not root.exists():
        return hashes
    for path in sorted(root.rglob('*')):
        if path.is_file() and skip_dir not in path.relative_to(root).parts:
            hashes[path.relative_to(root).as_posix()] = hashlib.sha256(path.read_bytes()).hexdigest()
    return hashes


def diff_hashes(before, after):
    changed = sorted(p for p in before.keys() & after.keys() if before[p] != after[p])
    added = sorted(after.keys() - before.keys())
    removed = sorted(before.keys() - after.keys())
    return changed, added, removed


def remove_tree(path):
    """Delete a folder that may hold read-only files; returns False if something stayed locked."""
    def make_writable(function, target, _):
        os.chmod(target, stat.S_IWRITE)
        function(target)

    path = Path(path)
    if not path.exists():
        return True
    try:
        shutil.rmtree(path, onerror=make_writable)
    except OSError:
        return False
    return not path.exists()


JUNIT_SUITE = re.compile(r'<testsuite\b[^>]*>')
JUNIT_ATTR = re.compile(r'(\w+)="([^"]*)"')
JUNIT_CASE = re.compile(r'<testcase\b([^>]*?)(/>|>(.*?)</testcase>)', re.S)
JUNIT_FAILURE = re.compile(r'<(failure|error)\b([^>]*)', re.S)


def junit_totals(results_dir):
    """Sum a Gradle test-results folder: {'tests', 'failures', 'skipped', 'failed': [(case, message)]}."""
    totals = {'tests': 0, 'failures': 0, 'skipped': 0, 'failed': []}
    for xml in sorted(Path(results_dir).glob('TEST-*.xml')):
        text = xml.read_text(encoding='utf-8', errors='replace')
        suite = JUNIT_SUITE.search(text)
        if not suite:
            continue
        attrs = dict(JUNIT_ATTR.findall(suite.group(0)))
        totals['tests'] += int(attrs.get('tests', 0))
        totals['failures'] += int(attrs.get('failures', 0)) + int(attrs.get('errors', 0))
        totals['skipped'] += int(attrs.get('skipped', 0))
        for case_attrs, _, body in JUNIT_CASE.findall(text):
            failure = JUNIT_FAILURE.search(body or '')
            if failure:
                case = dict(JUNIT_ATTR.findall(case_attrs))
                message = html.unescape(dict(JUNIT_ATTR.findall(failure.group(2))).get('message', ''))
                name = f'{case.get("classname", "?").rsplit(".", 1)[-1]}.{case.get("name", "?")}'
                totals['failed'].append((name, message.splitlines()[0] if message else ''))
    return totals
