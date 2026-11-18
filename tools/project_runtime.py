"""Windows plumbing for local_checks.py and commit_push.py: project paths, logs and owned processes.

The tools use each repository's gradlew.bat with JDK 21. Temporary files live under Workspace/Temps;
GZR_TEMP, GZR_GU_WORLD, GZR_WIKI, GZR_MODELS and GZR_JAVA_HOME override local paths.
One Gradle lock covers every live checkout, so parallel sessions never build the same folder at once: a run
queues for it up to GZR_LOCK_WAIT seconds (default an hour), in arrival order. A hand-run Gradle command takes
the same lock through

    python -B tools/project_runtime.py gradlew.bat runClient     any command, run once the lock is free

commit_push.py builds exported trees instead, each in one of GZR_VERIFY_SLOTS verification slots (default 2),
and takes no Gradle lock.

Processes are read through the Win32 API (ctypes): the toolhelp snapshot for parents, the PEB for command
lines and working directories. The tools expect 64-bit Python; the path helpers import on any system.
"""
import argparse
import hashlib
import html
import os
import queue
import re
import secrets
import shutil
import stat
import subprocess
import sys
import threading
import time
from dataclasses import dataclass
from pathlib import Path

if sys.platform == 'win32':
    import ctypes
    import msvcrt
    from ctypes import wintypes


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
WORKSPACE_ROOT = REPOSITORY_ROOT.parent.parent
GU_WORLD_ROOT = Path(os.environ.get('GZR_GU_WORLD') or REPOSITORY_ROOT.parent / 'mcmod-guworld').expanduser().resolve()
TEMP_ROOT = Path(os.environ.get('GZR_TEMP') or WORKSPACE_ROOT / 'Temps' / 'guzhenren').expanduser().resolve()
LOG_DIR = TEMP_ROOT / 'logs'
WIKI_ROOT = Path(os.environ.get('GZR_WIKI') or REPOSITORY_ROOT.parent / 'mcmod-wiki-gzr').expanduser().resolve()
MODELS_ROOT = Path(os.environ.get('GZR_MODELS') or REPOSITORY_ROOT.parent / 'assets' / 'blockbench-gzr').expanduser().resolve()
EXIT = object()
# How long local_checks.py and commit_push.py queue behind another run's Gradle lock before giving up.
LOCK_WAIT = float(os.environ.get('GZR_LOCK_WAIT') or 3600)
# How many commit_push.py runs verify their exported trees at once; each is a Gradle build plus a game JVM.
SLOTS = int(os.environ.get('GZR_VERIFY_SLOTS') or 2)
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
                         ('tools', 'build', 'gametest', 'data', 'server', 'client', 'multiplayer'),
                         ('build', 'data'), ('build', 'gametest'), True,
                         GU_WORLD_ROOT, 'Gu World', ('gametest', 'server', 'client', 'multiplayer')),
    'guworld': Project('guworld', GU_WORLD_ROOT, TEMP_ROOT.parent / 'guworld',
                       ('build', 'gametest', 'data'), ('build', 'data'), ('gametest',), False,
                       REPOSITORY_ROOT, 'Guzhenren', ('build', 'gametest', 'data'), ci_jar_dir=None),
}
# Gu World's local tests are GameTests alone, so its mirror adds only those to its CI build.
# The wiki ships through commit_push.py too, but it is no mod: no Gradle stage, no partner, no workflow.
WIKI = Project('wiki', WIKI_ROOT, TEMP_ROOT / 'wiki', (), (), (), False, ci_jar_dir=None)


def project(name):
    return PROJECTS[name]


def all_projects():
    return list(PROJECTS.values())


def ship_projects():
    """Every repository commit_push.py ships, by name: the two mods and the wiki."""
    return {**PROJECTS, WIKI.name: WIKI}


def partner_of(owner):
    """The other mod."""
    return next(other for other in all_projects() if other.name != owner.name)


def mod_roots(owner, root=None, partner_root=None):
    """(Gu World root, Guzhenren root) for a run of `owner` from `root` (its checkout or an export of it), with
    the partner at `partner_root` (default: the partner's checkout)."""
    root = Path(root or owner.root)
    partner = Path(partner_root or owner.partner)
    return (partner, root) if owner.name == 'guzhenren' else (root, partner)


def require_windows():
    if sys.platform != 'win32' or ctypes.sizeof(ctypes.c_void_p) != 8:
        raise SystemExit("These tools run on Windows with 64-bit Python; use this workstation's Python 3.")
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
        program_files = Path(environment.get('ProgramFiles') or r'C:\Program Files')
        # IDEA's downloads, then the usual installers (Temurin, Oracle, Microsoft, Zulu).
        for folder in [Path.home() / '.jdks', program_files / 'Eclipse Adoptium', program_files / 'Java',
                       program_files / 'Microsoft', program_files / 'Zulu']:
            if folder.is_dir():
                candidates.extend(sorted(folder.iterdir()))
    for candidate in candidates:
        try:
            release = (candidate / 'release').read_text(encoding='utf-8')
        except OSError:
            continue
        if (re.search(r'^JAVA_VERSION="21(?:[.+-]|"|$)', release, re.M)
                and (candidate / 'bin' / 'java.exe').is_file() and (candidate / 'bin' / 'javac.exe').is_file()):
            java_home = candidate.resolve()
            break
    else:
        raise SystemExit('JDK 21 with javac is required; set GZR_JAVA_HOME to its installation directory'
                         + (f' (configured: {configured})' if configured else ''))
    temporary = TEMP_ROOT / 'tmp'
    temporary.mkdir(parents=True, exist_ok=True)
    environment['JAVA_HOME'] = str(java_home)
    environment['PATH'] = str(java_home / 'bin') + os.pathsep + environment.get('PATH', '')
    # TMP and TEMP are what Windows programs read; TMPDIR is Python's first choice.
    for name in ('TMPDIR', 'TMP', 'TEMP'):
        environment[name] = str(temporary)
    environment['PYTHONDONTWRITEBYTECODE'] = '1'
    # A pipe gets the Windows code page (GBK here) unless asked otherwise; the logs and the parsers read UTF-8.
    environment['PYTHONIOENCODING'] = 'utf-8'
    java_options = environment.get('JAVA_TOOL_OPTIONS', '')
    environment['JAVA_TOOL_OPTIONS'] = (java_options + f' -Djava.io.tmpdir="{temporary}"'
                                        ' -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8').strip()
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


# The byte the Gradle lock holds, far past the holder's PID text: a Windows byte-range lock also bars every other
# handle from reading the bytes it covers, and waiters read the PID to say whom they wait for.
LOCK_BYTE = 0x7FFF0000


def try_lock(handle):
    """Takes the lock byte of an open lock file without waiting; False while another handle holds it."""
    handle.seek(LOCK_BYTE)
    try:
        msvcrt.locking(handle.fileno(), msvcrt.LK_NBLCK, 1)
        return True
    except OSError:
        return False
    finally:
        handle.seek(0)


def unlock(handle):
    """Releases the lock byte; closing the handle alone frees it only when Windows gets round to it."""
    handle.seek(LOCK_BYTE)
    try:
        msvcrt.locking(handle.fileno(), msvcrt.LK_UNLCK, 1)
    finally:
        handle.seek(0)


class Queue:
    """Arrival order for the waiters of a lock or a slot pool. `directory` holds one ticket file per waiter,
    named by its arrival time, and only the first `width` tickets may try the lock. A ticket whose process
    ended (its PID gone, or reused by a process born at another time) is dropped, so a killed waiter never
    blocks the line. The tickets only order the waiters; the byte lock alone excludes."""

    def __init__(self, directory, width=1):
        self.directory = Path(directory)
        self.width = width
        self.ticket = None

    def join(self):
        self.directory.mkdir(parents=True, exist_ok=True)
        pid = os.getpid()
        self.ticket = self.directory / f'{time.time_ns():020d}-{pid}-{_created(pid) or 0}-{secrets.token_hex(2)}'
        self.ticket.touch()

    def leave(self):
        if self.ticket is not None:
            self.ticket.unlink(missing_ok=True)
            self.ticket = None

    def my_turn(self):
        try:
            tickets = sorted(self.directory.iterdir())
        except FileNotFoundError:
            return True
        ahead = 0
        for path in tickets:
            if path == self.ticket:
                return True
            parts = path.name.split('-')
            if len(parts) != 4 or not (parts[1].isdigit() and parts[2].isdigit()):
                continue
            pid, born = int(parts[1]), int(parts[2])
            if not pid_alive(pid) or (born and _created(pid) not in (None, born)):
                path.unlink(missing_ok=True)
                continue
            ahead += 1
            if ahead >= self.width:
                return False
        return True


class Lock:
    """One Windows file lock for all projects. Keep the lock file in place while tools may be running.

    A held lock makes the caller wait up to `wait` seconds (0: give up at once), saying once whom it waits for.
    Windows offers no blocking wait with a deadline on a byte-range lock, so a waiter retries every `poll` seconds.
    The waiters (these tools and `project_runtime.py <command>`) take it in arrival order (Queue)."""

    busy = 'another local_checks/commit_push run (pid {holder}) is using Gradle; wait for it to finish'
    waiting = 'waiting for the Gradle lock (pid {holder}) ...'

    def __init__(self, path=None, wait=0, echo=print, poll=0.2):
        self.path = Path(path or TEMP_ROOT / 'gradle.lock')
        self.wait = wait
        self.echo = echo
        self.poll = poll
        self._handle = None

    def candidates(self):
        """The lock files any one of which will do."""
        return [self.path]

    def __enter__(self):
        paths = self.candidates()
        paths[0].parent.mkdir(parents=True, exist_ok=True)
        queue = Queue(paths[0].with_name(paths[0].name + '.queue'), len(paths))
        queue.join()
        deadline = time.monotonic() + self.wait
        told = False
        handle = None
        try:
            while handle is None:
                holder = ''
                for path in paths if queue.my_turn() else []:
                    candidate = path.open('a+b')
                    if try_lock(candidate):
                        handle, self.path = candidate, path
                        break
                    holder = holder or candidate.read().decode('utf-8', errors='replace').strip()
                    candidate.close()
                if handle is not None:
                    break
                if not holder and paths[0].exists():
                    holder = paths[0].read_bytes().decode('utf-8', errors='replace').strip()
                holder = holder or 'starting'
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    raise SystemExit(self.busy.format(holder=holder)) from None
                if not told:
                    self.echo(self.waiting.format(holder=holder), flush=True)
                    told = True
                time.sleep(min(self.poll, remaining))
        finally:
            queue.leave()
        try:
            handle.seek(0)
            handle.truncate()
            handle.write(str(os.getpid()).encode('utf-8'))
            handle.flush()
        except BaseException:
            unlock(handle)
            handle.close()
            raise
        self._handle = handle
        return self

    def __exit__(self, *exc):
        if self._handle is not None:
            try:
                self._handle.seek(0)
                self._handle.truncate()
                self._handle.flush()
            finally:
                try:
                    unlock(self._handle)
                finally:
                    self._handle.close()
                    self._handle = None


class Slot(Lock):
    """One of `count` verification slots under `directory`: commit_push.py verifies an exported tree in a slot,
    so that many runs verify at once and the rest wait in arrival order. `directory` is the slot's own folder
    once taken; the exports and game directories of a run live there, at the same paths every time."""

    busy = 'every verification slot is taken (pid {holder}); wait for a commit_push run to finish'
    waiting = 'waiting for a verification slot (pid {holder}) ...'

    def __init__(self, directory, count=None, wait=0, echo=print, poll=0.2):
        super().__init__(Path(directory) / 'slot-1.lock', wait, echo, poll)
        self.root = Path(directory)
        self.count = count or SLOTS

    def candidates(self):
        return [self.root / f'slot-{number}.lock' for number in range(1, self.count + 1)]

    @property
    def directory(self):
        return self.path.with_suffix('')


# ----- Win32 process access ---------------------------------------------------------------------------

PROCESS_TERMINATE = 0x0001
PROCESS_VM_READ = 0x0010
PROCESS_QUERY_LIMITED_INFORMATION = 0x1000
SYNCHRONIZE = 0x00100000
WAIT_TIMEOUT = 0x00000102
ERROR_ACCESS_DENIED = 5
TH32CS_SNAPPROCESS = 0x00000002
# Offsets in the 64-bit PEB and its RTL_USER_PROCESS_PARAMETERS, unchanged since 64-bit Windows XP.
PEB_PROCESS_PARAMETERS = 0x20
PARAMETERS_CURRENT_DIRECTORY = 0x38
PARAMETERS_COMMAND_LINE = 0x70
JAVA_EXES = ('java.exe', 'javaw.exe')

if sys.platform == 'win32':
    class _ProcessEntry(ctypes.Structure):
        _fields_ = [('dwSize', wintypes.DWORD), ('cntUsage', wintypes.DWORD), ('th32ProcessID', wintypes.DWORD),
                    ('th32DefaultHeapID', ctypes.c_size_t), ('th32ModuleID', wintypes.DWORD),
                    ('cntThreads', wintypes.DWORD), ('th32ParentProcessID', wintypes.DWORD),
                    ('pcPriClassBase', ctypes.c_long), ('dwFlags', wintypes.DWORD),
                    ('szExeFile', ctypes.c_wchar * 260)]

    class _UnicodeString(ctypes.Structure):
        _fields_ = [('Length', wintypes.USHORT), ('MaximumLength', wintypes.USHORT), ('Buffer', ctypes.c_void_p)]

    class _BasicInformation(ctypes.Structure):
        _fields_ = [('ExitStatus', ctypes.c_long), ('PebBaseAddress', ctypes.c_void_p),
                    ('AffinityMask', ctypes.c_size_t), ('BasePriority', ctypes.c_long),
                    ('UniqueProcessId', ctypes.c_size_t), ('InheritedFromUniqueProcessId', ctypes.c_size_t)]

    def _function(library, name, result, *arguments):
        function = getattr(library, name)
        function.restype, function.argtypes = result, arguments
        return function

    _kernel32 = ctypes.WinDLL('kernel32', use_last_error=True)
    _ntdll = ctypes.WinDLL('ntdll')
    _OpenProcess = _function(_kernel32, 'OpenProcess', wintypes.HANDLE, wintypes.DWORD, wintypes.BOOL,
                             wintypes.DWORD)
    _CloseHandle = _function(_kernel32, 'CloseHandle', wintypes.BOOL, wintypes.HANDLE)
    _WaitForSingleObject = _function(_kernel32, 'WaitForSingleObject', wintypes.DWORD, wintypes.HANDLE,
                                     wintypes.DWORD)
    _TerminateProcess = _function(_kernel32, 'TerminateProcess', wintypes.BOOL, wintypes.HANDLE, wintypes.UINT)
    _GetProcessTimes = _function(_kernel32, 'GetProcessTimes', wintypes.BOOL, wintypes.HANDLE,
                                 *[ctypes.POINTER(wintypes.FILETIME)] * 4)
    _QueryFullProcessImageNameW = _function(_kernel32, 'QueryFullProcessImageNameW', wintypes.BOOL,
                                            wintypes.HANDLE, wintypes.DWORD, wintypes.LPWSTR,
                                            ctypes.POINTER(wintypes.DWORD))
    _CreateToolhelp32Snapshot = _function(_kernel32, 'CreateToolhelp32Snapshot', wintypes.HANDLE, wintypes.DWORD,
                                          wintypes.DWORD)
    _Process32FirstW = _function(_kernel32, 'Process32FirstW', wintypes.BOOL, wintypes.HANDLE,
                                 ctypes.POINTER(_ProcessEntry))
    _Process32NextW = _function(_kernel32, 'Process32NextW', wintypes.BOOL, wintypes.HANDLE,
                                ctypes.POINTER(_ProcessEntry))
    _ReadProcessMemory = _function(_kernel32, 'ReadProcessMemory', wintypes.BOOL, wintypes.HANDLE, ctypes.c_void_p,
                                   ctypes.c_void_p, ctypes.c_size_t, ctypes.POINTER(ctypes.c_size_t))
    _NtQueryInformationProcess = _function(_ntdll, 'NtQueryInformationProcess', ctypes.c_long, wintypes.HANDLE,
                                           ctypes.c_ulong, ctypes.c_void_p, ctypes.c_ulong,
                                           ctypes.POINTER(ctypes.c_ulong))
    _INVALID_HANDLE = ctypes.c_void_p(-1).value


class _Opened:
    """A process handle closed on exit; None inside when the process is gone or not ours to open."""

    def __init__(self, pid, access):
        self.handle = _OpenProcess(access, False, pid) if pid > 0 else None
        self.error = 0 if self.handle else ctypes.get_last_error()

    def __enter__(self):
        return self.handle

    def __exit__(self, *exc):
        if self.handle:
            _CloseHandle(self.handle)


def pid_alive(pid):
    """True while the process runs; an exited one whose handle someone still holds counts as gone."""
    if pid <= 0:
        return False
    opened = _Opened(pid, SYNCHRONIZE | PROCESS_QUERY_LIMITED_INFORMATION)
    with opened as handle:
        if not handle:
            return opened.error == ERROR_ACCESS_DENIED
        return _WaitForSingleObject(handle, 0) == WAIT_TIMEOUT


def process_table():
    """{pid: (parent pid, exe file name)} of every running process."""
    snapshot = _CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0)
    if not snapshot or snapshot == _INVALID_HANDLE:
        raise OSError(ctypes.get_last_error(), 'CreateToolhelp32Snapshot failed')
    try:
        entry = _ProcessEntry(dwSize=ctypes.sizeof(_ProcessEntry))
        table = {}
        more = _Process32FirstW(snapshot, ctypes.byref(entry))
        while more:
            table[entry.th32ProcessID] = (entry.th32ParentProcessID, entry.szExeFile)
            more = _Process32NextW(snapshot, ctypes.byref(entry))
        return table
    finally:
        _CloseHandle(snapshot)


def _created(pid):
    """The process's creation time in FILETIME ticks, or None."""
    with _Opened(pid, PROCESS_QUERY_LIMITED_INFORMATION) as handle:
        if not handle:
            return None
        times = [wintypes.FILETIME() for _ in range(4)]
        if not _GetProcessTimes(handle, *(ctypes.byref(time_) for time_ in times)):
            return None
        return times[0].dwHighDateTime << 32 | times[0].dwLowDateTime


def descendant_pids(pid):
    """The live descendants of `pid`. Windows leaves a dead parent's PID in its children, and PIDs get reused, so
    a child must also be no older than the parent it names or a stranger would join the tree."""
    table = process_table()
    born = {}

    def created(owner):
        if owner not in born:
            born[owner] = _created(owner)
        return born[owner]

    found = []
    pending = [pid]
    while pending:
        parent = pending.pop()
        for child, (owner, _) in table.items():
            if owner != parent or child == parent or child == pid or child in found:
                continue
            if created(parent) is not None and created(child) is not None and created(child) < created(parent):
                continue
            found.append(child)
            pending.append(child)
    return found


def _read_memory(handle, address, size):
    buffer = ctypes.create_string_buffer(size)
    done = ctypes.c_size_t()
    if not _ReadProcessMemory(handle, address, buffer, size, ctypes.byref(done)) or done.value != size:
        raise OSError(ctypes.get_last_error(), 'ReadProcessMemory failed')
    return buffer.raw


def _read_unicode_string(handle, address):
    string = _UnicodeString.from_buffer_copy(_read_memory(handle, address, ctypes.sizeof(_UnicodeString)))
    if not string.Length or not string.Buffer:
        return ''
    return _read_memory(handle, string.Buffer, string.Length).decode('utf-16-le', errors='replace')


def process_parameters(pid):
    """(command line, working directory) of a process, read from its PEB; None when it is gone or not ours."""
    with _Opened(pid, PROCESS_QUERY_LIMITED_INFORMATION | PROCESS_VM_READ) as handle:
        if not handle:
            return None
        info = _BasicInformation()
        if (_NtQueryInformationProcess(handle, 0, ctypes.byref(info), ctypes.sizeof(info), None) != 0
                or not info.PebBaseAddress):
            return None
        try:
            parameters = int.from_bytes(_read_memory(handle, info.PebBaseAddress + PEB_PROCESS_PARAMETERS, 8),
                                        'little')
            return (_read_unicode_string(handle, parameters + PARAMETERS_COMMAND_LINE),
                    _read_unicode_string(handle, parameters + PARAMETERS_CURRENT_DIRECTORY))
        except OSError:
            return None


def process_exe(pid):
    """The full path of a process's executable, or None."""
    with _Opened(pid, PROCESS_QUERY_LIMITED_INFORMATION) as handle:
        if not handle:
            return None
        size = wintypes.DWORD(32768)
        buffer = ctypes.create_unicode_buffer(size.value)
        if not _QueryFullProcessImageNameW(handle, 0, buffer, ctypes.byref(size)):
            return None
        return Path(buffer.value)


def _terminate(pid):
    with _Opened(pid, PROCESS_TERMINATE) as handle:
        if handle:
            _TerminateProcess(handle, 1)


def same_path(first, second):
    """Whether two paths name the same place, compared the way Windows does: any case, either slash."""
    return os.path.normcase(str(Path(first).resolve())) == os.path.normcase(str(Path(second).resolve()))


class Process:
    """A child process whose merged output is copied line by line into the log. It starts its own process group,
    so a Ctrl+C in the tool's console reaches the tool, which then cleans the tree up itself."""

    def __init__(self, command, cwd, log, env=None):
        self.log = log
        self.command = command
        self.proc = subprocess.Popen(
            command, cwd=cwd, env=runtime_environment(env), stdin=subprocess.DEVNULL, stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NEW_PROCESS_GROUP)
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
        """Ends the child and everything it started; the descendants are listed first, while their parents live."""
        children = descendant_pids(self.proc.pid)
        if self.proc.poll() is None:
            self.proc.kill()
        kill_pids(children)
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


def gradle_command(root, *arguments, project=None, daemon=True, partner_root=None, game_root=None):
    """A Gradle invocation in `root` for `project` (default: the project at root), with its partner's checkout
    (or `partner_root`, an export of the partner) and its GameTest and datagen game directories under the
    project's Temp folder (or `game_root`, so two runs at once never share a world).
    A warm daemon saves each later invocation its JVM start and Gradle bootstrap (≈4 s); it serves both mods
    and stops after DAEMON_IDLE_MS idle. daemon=False starts a single-use daemon that ends with the build, for runs whose
    game JVM the caller must own and kill."""
    root = Path(root).resolve()
    owner = project or project_at(root)
    gu_world, guzhenren = mod_roots(owner, root, partner_root)
    games = Path(game_root or owner.temp)
    lifetime = ['--daemon', f'-Dorg.gradle.daemon.idletimeout={DAEMON_IDLE_MS}'] if daemon else ['--no-daemon']
    return [str(root / 'gradlew.bat'), *arguments, f'-PguWorldDir={gu_world}',
            f'-PguzhenrenDir={guzhenren}', f'-PgameTestDir={games / "gametest"}',
            f'-PdataGameDir={games / "data"}', *lifetime, '--console=plain']


def git(*arguments, cwd=REPOSITORY_ROOT, env=None, check=True):
    result = subprocess.run(['git', *arguments], cwd=cwd, env=env, capture_output=True)
    stdout = result.stdout.decode('utf-8', errors='replace')
    stderr = result.stderr.decode('utf-8', errors='replace')
    if check and result.returncode != 0:
        raise RuntimeError(f'git {" ".join(arguments)} failed ({result.returncode}): {stderr.strip()}')
    return result.returncode, stdout, stderr


def _java_parameters():
    """{pid: (command line, working directory)} of the running Java processes this user can read."""
    found = {}
    for pid, (_, exe) in process_table().items():
        if exe.casefold() not in JAVA_EXES:
            continue
        parameters = process_parameters(pid)
        if parameters and parameters[0]:
            found[pid] = parameters
    return found


def java_processes():
    """Running Java processes visible to this user, keyed by PID: the command line exactly as Windows keeps it."""
    return {pid: command_line for pid, (command_line, _) in _java_parameters().items()}


def java_process_dirs():
    """Working directory of each running Java process visible to this user, keyed by PID."""
    return {pid: Path(cwd) for pid, (_, cwd) in _java_parameters().items() if cwd}


def java_processes_in(folder):
    """PIDs of the Java processes whose working directory is `folder`."""
    return sorted(pid for pid, cwd in java_process_dirs().items() if same_path(cwd, folder))


def _path_text(text):
    """A path or command line in the form game_jvms compares: forward slashes, case folded."""
    return str(text).replace('\\', '/').casefold()


def game_jvms(processes, root=REPOSITORY_ROOT):
    """Game JVMs launched from this checkout, by Gradle or by IDEA: both pass ModDevGradle's
    build/moddev/<run>RunVmArgs.txt, which the Gradle daemon and wrapper never do. Windows paths match in either
    slash and any case."""
    marker = _path_text(Path(root) / 'build' / 'moddev') + '/'
    return sorted(pid for pid, command_line in processes.items()
                  if marker in _path_text(command_line) and 'runvmargs.txt' in _path_text(command_line))


def kill_pids(pids):
    """Ends the processes and waits up to 5 s for them to go. Windows has no SIGTERM for a console JVM (Java takes
    Ctrl+Break as a thread dump request), so this is TerminateProcess straight away."""
    remaining = {pid for pid in pids if pid > 4 and pid != os.getpid() and pid_alive(pid)}
    for pid in remaining:
        _terminate(pid)
    deadline = time.monotonic() + 5
    while remaining and time.monotonic() < deadline:
        remaining = {pid for pid in remaining if pid_alive(pid)}
        if remaining:
            time.sleep(0.05)


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


def main(argv=None):
    """Runs a hand-started command (a manual Gradle build) under the shared Gradle lock; returns its exit code."""
    parser = argparse.ArgumentParser(description='Run a command under the shared Gradle lock, queueing up to '
                                                 'GZR_LOCK_WAIT seconds like local_checks.py and commit_push.py.')
    parser.add_argument('command', nargs=argparse.REMAINDER, help='the command and its arguments, e.g. gradlew.bat build')
    args = parser.parse_args(argv)
    if not args.command:
        parser.error('a command is required')
    require_windows()
    with Lock(wait=LOCK_WAIT):
        return subprocess.call(args.command)


if __name__ == '__main__':
    sys.exit(main())
