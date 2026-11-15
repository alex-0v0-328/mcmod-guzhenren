"""Profile a local_checks.py smoke stage with JFR and attribute the cost to this mod.

Runs `local_checks.py <stage>` (server or multiplayer), attaches a Java Flight Recorder recording to each game
JVM the stage boots as soon as it appears (jcmd JFR.start, settings=profile, dumped on exit), then summarizes
every recording: CPU samples and allocation samples whose stack holds a `net.alex.guzhenren.` frame (self = the
top mod frame, inclusive = any mod frame), GC pauses, and the running samples of the server thread. The before
and after of an optimization pass are two runs of the same stage.

    python -B tools/profile_smoke.py multiplayer            # run, record, summarize into Temps/guzhenren/profile-<pid>/
    python -B tools/profile_smoke.py server --out DIR       # the lighter stage (entities, features, dimensions)
    python -B tools/profile_smoke.py summarize FILE.jfr     # summarize an existing recording again

jcmd and jfr come from the JDK the game JVM runs on (its java.exe), so the recording and its reader always match;
the summary needs JDK 21 or newer on that path. The stage's own result goes to <out>/local_checks.out and the
stage log stays where local_checks.py puts it. A recording covers boot too, so the first seconds are mod loading.
"""
import argparse
import os
import re
import subprocess
import sys
import time
from collections import Counter
from pathlib import Path

import project_runtime as env

MOD_PREFIX = 'net.alex.guzhenren.'
# The throwaway game dirs local_checks.py creates under the project's Temp folder, by the role each JVM plays.
GAME_DIRS = {'server': ('server-smoke', 'multiplayer-server'), 'client': ('multiplayer-client',)}
STAGES = ('server', 'multiplayer')
FRAME = re.compile(r'^\s+(\S+)\s+line: (\d+)')
WEIGHT = re.compile(r'weight = ([\d.]+) (\w+)')
CLASS = re.compile(r'objectClass = (\S+)')
DURATION = re.compile(r'duration = ([\d.]+) (\w+)')
THREAD = re.compile(r'sampledThread = "([^"]+)"')
TIME_UNITS = {'ns': 1e-6, 'us': 1e-3, 'ms': 1.0, 's': 1000.0}
BYTE_UNITS = {'bytes': 1, 'kB': 1e3, 'MB': 1e6, 'GB': 1e9}


def game_roles(temp_root):
    """The game JVMs of a stage by role: {role: (pid, jdk bin dir)}, found by their working directory."""
    found = {}
    for pid, cwd in env.java_process_dirs().items():
        for role, names in GAME_DIRS.items():
            if env.same_path(cwd.parent, temp_root) and cwd.name.casefold() in names:
                exe = env.process_exe(pid)
                if exe:
                    found[role] = (pid, exe.parent)
    return found


def start_recording(pid, jdk_bin, target):
    command = [str(jdk_bin / 'jcmd.exe'), str(pid), 'JFR.start', 'name=profile_smoke', 'settings=profile',
               'dumponexit=true', f'filename={target}']
    reply = subprocess.run(command, capture_output=True, text=True)
    return reply.returncode == 0 and 'Started recording' in reply.stdout, (reply.stdout + reply.stderr).strip()


def run_stage(stage, out_dir, project):
    """Runs the stage with a recording on every game JVM; returns {role: (jfr path, jdk bin dir)}."""
    out_dir.mkdir(parents=True, exist_ok=True)
    recordings = {}
    with (out_dir / 'local_checks.out').open('w', encoding='utf-8') as log:
        checks = subprocess.Popen([sys.executable, '-B', str(env.REPOSITORY_ROOT / 'tools' / 'local_checks.py'),
                                   stage, '--project', project.name], cwd=env.REPOSITORY_ROOT, stdout=log,
                                  stderr=subprocess.STDOUT)
        while checks.poll() is None:
            for role, (pid, jdk_bin) in game_roles(project.temp).items():
                if role in recordings:
                    continue
                target = out_dir / f'{role}.jfr'
                started, reply = start_recording(pid, jdk_bin, target)
                print(f'[{time.strftime("%H:%M:%S")}] {role} pid {pid}: {reply.splitlines()[-1] if reply else ""}',
                      flush=True)
                if started:
                    recordings[role] = (target, jdk_bin)
            time.sleep(1)
    print(f'local_checks exit {checks.returncode}', flush=True)
    return recordings


# ----- summary ---------------------------------------------------------------------------------------

def events(text):
    """The events of a `jfr print` dump: {'kind', 'frames' (method names, top first), 'lines'}."""
    current = None
    for line in text.splitlines():
        if line and not line.startswith(' ') and line.endswith('{'):
            if current:
                yield current
            current = {'kind': line.split()[0], 'frames': [], 'lines': []}
        elif current is not None:
            current['lines'].append(line)
            match = FRAME.match(line)
            if match:
                current['frames'].append(match.group(1))
    if current:
        yield current


def short(frame):
    return frame[len(MOD_PREFIX):] if frame.startswith(MOD_PREFIX) else frame


def summarize_text(text, top=20, path=''):
    """The summary of a `jfr print` dump as lines of text."""
    cpu_total = cpu_mod = server_thread = 0
    cpu_self, cpu_incl = Counter(), Counter()
    alloc_total = alloc_mod = 0.0
    alloc_site, alloc_class = Counter(), Counter()
    gc = []
    for event in events(text):
        body = '\n'.join(event['lines'])
        if event['kind'] == 'jdk.GCPhasePause':
            match = DURATION.search(body)
            if match:
                gc.append(float(match.group(1)) * TIME_UNITS.get(match.group(2), 1.0))
            continue
        mod_frames = [frame for frame in event['frames'] if frame.startswith(MOD_PREFIX)]
        if event['kind'] == 'jdk.ExecutionSample':
            cpu_total += 1
            thread = THREAD.search(body)
            if thread and thread.group(1) == 'Server thread':
                server_thread += 1
            if mod_frames:
                cpu_mod += 1
                cpu_self[short(mod_frames[0])] += 1
                for frame in dict.fromkeys(mod_frames):
                    cpu_incl[short(frame)] += 1
        elif event['kind'] == 'jdk.ObjectAllocationSample':
            weight = WEIGHT.search(body)
            size = float(weight.group(1)) * BYTE_UNITS.get(weight.group(2), 1) if weight else 0.0
            alloc_total += size
            if mod_frames:
                alloc_mod += size
                alloc_site[short(mod_frames[0])] += size
                klass = CLASS.search(body)
                alloc_class[klass.group(1) if klass else '?'] += size
    lines = [f'# {path}'.rstrip(),
             f'CPU samples: {cpu_total} total, {server_thread} on the server thread, {cpu_mod} with a mod frame '
             f'({100 * cpu_mod / max(1, cpu_total):.2f}%)',
             '', '## CPU self (top mod frame)']
    lines += [f'{count:6d}  {frame}' for frame, count in cpu_self.most_common(top)]
    lines += ['', '## CPU inclusive (any mod frame)']
    lines += [f'{count:6d}  {frame}' for frame, count in cpu_incl.most_common(top)]
    lines += ['', f'Allocation samples: {alloc_total / 1e6:.0f} MB weighted total, {alloc_mod / 1e6:.1f} MB with a '
              f'mod frame ({100 * alloc_mod / max(1.0, alloc_total):.2f}%)', '', '## Allocation by mod site (MB)']
    lines += [f'{size / 1e6:8.1f}  {frame}' for frame, size in alloc_site.most_common(top)]
    lines += ['', '## Allocation by class under mod frames (MB)']
    lines += [f'{size / 1e6:8.1f}  {klass}' for klass, size in alloc_class.most_common(top)]
    if gc:
        lines += ['', f'GC pauses: {len(gc)} phases, {sum(gc):.0f} ms total, max {max(gc):.1f} ms']
    return lines


def jfr_print(jfr_tool, path):
    return subprocess.run([str(jfr_tool), 'print', '--events',
                           'jdk.ExecutionSample,jdk.ObjectAllocationSample,jdk.GCPhasePause', str(path)],
                          capture_output=True, text=True, check=True).stdout


def summarize(path, jfr_tool, top):
    lines = summarize_text(jfr_print(jfr_tool, path), top, str(path))
    summary = Path(path).with_suffix('.summary.txt')
    summary.write_text('\n'.join(lines) + '\n', encoding='utf-8')
    return summary, lines


def default_jfr_tool():
    """The jfr tool of GZR_JAVA_HOME, else the first `jfr` on PATH."""
    home = os.environ.get('GZR_JAVA_HOME')
    if home:
        return Path(home) / 'bin' / 'jfr.exe'
    for folder in os.environ.get('PATH', '').split(os.pathsep):
        candidate = Path(folder) / 'jfr.exe'
        if candidate.is_file():
            return candidate
    raise SystemExit('no jfr tool: set GZR_JAVA_HOME to a JDK 21+ or pass --jfr')


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('target', help=f'a stage ({", ".join(STAGES)}), or "summarize"')
    parser.add_argument('recording', nargs='?', help='with summarize: the .jfr file')
    parser.add_argument('--out', help='where the recordings and summaries go (default Temps/<project>/profile-<pid>)')
    parser.add_argument('--project', choices=sorted(env.PROJECTS), default='guzhenren')
    parser.add_argument('--top', type=int, default=20, help='rows per summary table (default 20)')
    parser.add_argument('--jfr', help='with summarize: the jfr tool to read with (default GZR_JAVA_HOME or PATH)')
    args = parser.parse_args(argv)
    if args.target == 'summarize':
        if not args.recording:
            parser.error('summarize needs the .jfr file')
        summary, lines = summarize(Path(args.recording), Path(args.jfr) if args.jfr else default_jfr_tool(), args.top)
        print('\n'.join(lines))
        print(f'\nwritten: {summary}')
        return 0
    if args.target not in STAGES:
        parser.error(f'stage must be one of {", ".join(STAGES)}, or summarize')
    env.require_windows()
    project = env.project(args.project)
    out_dir = Path(args.out) if args.out else project.temp / f'profile-{os.getpid()}'
    recordings = run_stage(args.target, out_dir, project)
    if not recordings:
        print(f'no game JVM was recorded; read {out_dir / "local_checks.out"}', flush=True)
        return 1
    for role, (path, jdk_bin) in recordings.items():
        if not path.exists():
            print(f'{role}: recording missing at {path}', flush=True)
            continue
        summary, lines = summarize(path, jdk_bin / 'jfr.exe', args.top)
        print(f'\n{role}: {lines[1]}\n  {summary}', flush=True)
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
