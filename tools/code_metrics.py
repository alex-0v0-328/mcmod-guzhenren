"""Structural baseline of both mods from their compiled classes, sources and Git history.

Per project (--project guzhenren|guworld, default both) it measures:
  1. the package dependency graph at depth 1 and depth 2 under the mod root: Ce, Ca, instability I, abstractness A
     and distance from the main sequence D per package (edges inside one package ignored);
  2. dependency cycles (Tarjan SCCs): full, and with the registration hubs left out the way ArchitectureTest does
     (root package classes plus `registry..`), each with the edges that close it;
  3. size: lines per file, files over 300 lines, methods over 40 lines, mutable (non-final) static fields;
  4. Git hotspots (commits in the last 180 days x current lines) and change coupling between src/main files;
  5. test annotations and test source size.

    python3 -B tools/code_metrics.py --out DIR                       # both mods
    python3 -B tools/code_metrics.py --out DIR --project guworld --full

Writes <out>/<project>-metrics.json (full data, stable key order) and <project>-metrics.md (<= 5 rows per list
unless --full). It never builds: the graph is read from build/classes/java/main by a pure-Python class-file reader
and a source file whose class is missing or older than the source is reported under `warnings`.

Approximations: a dependency is any Utf8 constant of a class that spells a class of the same mod in internal form
(super types, member and call references, descriptors, generic signatures, annotations), attributed to the class's
package; a package's Ce/Ca count distinct other packages. Abstractness counts named types (anonymous, local and
synthetic classes excluded): interfaces and abstract classes over all. Method and static-field detection scans
comment- and string-free source with brace matching, not a Java parser: a method's length runs from the first token
of its header (annotations included) to its closing brace; lambdas and anonymous-class bodies are not methods.
"""
import argparse
import bisect
import json
import re
import struct
import sys
from collections import defaultdict
from datetime import datetime, timedelta, timezone
from itertools import combinations
from pathlib import Path

import project_runtime as env

MODS = {
    'guzhenren': {'root_package': 'net.alex.guzhenren', 'hub_packages': ('registry',)},
    'guworld': {'root_package': 'net.alex.guzhenrenworld', 'hub_packages': ()},
}
ROOT_NODE = '(root)'
LONG_FILE_LINES = 300
LONG_METHOD_LINES = 40
HISTOGRAM = ((50, '1-50'), (100, '51-100'), (200, '101-200'), (300, '201-300'), (500, '301-500'),
             (1000, '501-1000'), (None, '1001+'))
OUTLIER_MIN_CLASSES = 5
OUTLIER_D = 0.7
HOTSPOT_ROWS = 20
COUPLING_MIN_COMMITS = 5
COUPLING_MIN_CONFIDENCE = 0.6
DEFAULT_DAYS = 180
DEFAULT_MAX_COMMIT_FILES = 40
MD_ROWS = 5

ACC_INTERFACE = 0x0200
ACC_ABSTRACT = 0x0400
ACC_SYNTHETIC = 0x1000


#region Class files [类文件]
def read_class(data, prefix):
    """(internal name, access flags, referenced mod classes) of a class file; `prefix` is the mod root in
    internal form with a trailing slash. The references are every constant-pool Utf8 that spells a mod class."""
    if data[:4] != b'\xca\xfe\xba\xbe':
        raise ValueError('not a class file')
    count, = struct.unpack_from('>H', data, 8)
    position = 10
    utf8 = {}
    class_names = {}
    index = 1
    while index < count:
        tag = data[position]
        position += 1
        if tag == 1:
            length, = struct.unpack_from('>H', data, position)
            utf8[index] = data[position + 2:position + 2 + length].decode('utf-8', errors='replace')
            position += 2 + length
        elif tag == 7:
            class_names[index], = struct.unpack_from('>H', data, position)
            position += 2
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            position += 4
        elif tag in (5, 6):
            position += 8
            index += 1
        elif tag in (8, 16, 19, 20):
            position += 2
        elif tag == 15:
            position += 3
        else:
            raise ValueError(f'unknown constant tag {tag}')
        index += 1
    access, this_class = struct.unpack_from('>HH', data, position)
    name = utf8[class_names[this_class]]
    pattern = re.compile(re.escape(prefix) + r'[\w$/]*')
    refs = set()
    for text in utf8.values():
        for found in pattern.findall(text):
            refs.add(found.replace('/', '.'))
    return name.replace('/', '.'), access, refs


def outermost(class_name):
    return class_name.split('$', 1)[0]


def is_named_type(class_name, access):
    """Counts toward abstractness: not anonymous, local or synthetic, not package-info or module-info."""
    simple = class_name.rsplit('.', 1)[-1]
    if access & ACC_SYNTHETIC or simple in ('package-info', 'module-info'):
        return False
    return not re.search(r'\$\d', simple)


def load_classes(classes_dir, root_package):
    """{class name: {'access': int, 'refs': set}} for every class file under classes_dir."""
    prefix = root_package.replace('.', '/') + '/'
    found = {}
    for path in sorted(Path(classes_dir).rglob('*.class')):
        name, access, refs = read_class(path.read_bytes(), prefix)
        refs.discard(name)
        found[name] = {'access': access, 'refs': refs}
    return found
#endregion


#region Package graph [包依赖图]
def node_of(class_name, root_package, depth):
    package = class_name.rsplit('.', 1)[0]
    relative = package[len(root_package) + 1:] if package != root_package else ''
    return '.'.join(relative.split('.')[:depth]) if relative else ROOT_NODE


def is_hub(class_name, root_package, hub_packages):
    package = outermost(class_name).rsplit('.', 1)[0]
    if package == root_package:
        return True
    return any(package == f'{root_package}.{hub}' or package.startswith(f'{root_package}.{hub}.')
               for hub in hub_packages)


def build_graph(classes, root_package, depth, hub_packages=None):
    """(nodes, edges): nodes {node: [class names]}, edges {(src node, dst node): {(src class, dst class)}} with
    classes shortened to their outermost class relative to the root. With hub_packages (a tuple) the registration
    hubs are left out of both ends of every edge, as ArchitectureTest ignores them."""
    skip = hub_packages is not None
    nodes = defaultdict(list)
    edges = defaultdict(set)
    for name, info in classes.items():
        if skip and is_hub(name, root_package, hub_packages):
            continue
        source = node_of(name, root_package, depth)
        nodes[source].append(name)
        for target in info['refs']:
            if target not in classes or (skip and is_hub(target, root_package, hub_packages)):
                continue
            destination = node_of(target, root_package, depth)
            if destination != source:
                edges[(source, destination)].add((short(outermost(name), root_package),
                                                  short(outermost(target), root_package)))
    return nodes, edges


def short(class_name, root_package):
    return class_name[len(root_package) + 1:] if class_name.startswith(root_package + '.') else class_name


def package_metrics(classes, nodes, edges):
    outgoing = defaultdict(set)
    incoming = defaultdict(set)
    for source, destination in edges:
        outgoing[source].add(destination)
        incoming[destination].add(source)
    rows = []
    for node in sorted(nodes):
        named = [n for n in nodes[node] if is_named_type(n, classes[n]['access'])]
        abstract = [n for n in named if classes[n]['access'] & (ACC_INTERFACE | ACC_ABSTRACT)]
        ce, ca = len(outgoing[node]), len(incoming[node])
        instability = ce / (ca + ce) if ca + ce else 0.0
        abstractness = len(abstract) / len(named) if named else 0.0
        rows.append({'package': node, 'classes': len(named), 'abstract': len(abstract), 'ce': ce, 'ca': ca,
                     'instability': round(instability, 3), 'abstractness': round(abstractness, 3),
                     'distance': round(abs(abstractness + instability - 1), 3),
                     'depends_on': sorted(outgoing[node]), 'depended_by': sorted(incoming[node])})
    return rows


def strongly_connected(nodes, edges):
    """Tarjan: the components with more than one node, members sorted, largest first."""
    successors = defaultdict(list)
    for source, destination in sorted(edges):
        successors[source].append(destination)
    index, low, on_stack, stack, found = {}, {}, set(), [], []

    def visit(vertex):
        index[vertex] = low[vertex] = len(index)
        stack.append(vertex)
        on_stack.add(vertex)
        for other in successors[vertex]:
            if other not in index:
                visit(other)
                low[vertex] = min(low[vertex], low[other])
            elif other in on_stack:
                low[vertex] = min(low[vertex], index[other])
        if low[vertex] == index[vertex]:
            members = []
            while True:
                member = stack.pop()
                on_stack.discard(member)
                members.append(member)
                if member == vertex:
                    break
            if len(members) > 1:
                found.append(sorted(members))

    for vertex in sorted(nodes):
        if vertex not in index:
            visit(vertex)
    return sorted(found, key=lambda members: (-len(members), members))


def closing_edges(members, edges):
    """The edges that run against a good order of the component (Eades-Lin-Smyth ordering, weighted by the
    number of class pairs): cutting them leaves no cycle. Cheapest first."""
    inside = {pair: len(pairs) for pair, pairs in edges.items() if pair[0] in members and pair[1] in members}
    remaining = set(members)
    left, right = [], []

    def weight(vertex, outgoing):
        return sum(w for (a, b), w in inside.items()
                   if (a if outgoing else b) == vertex and (b if outgoing else a) in remaining)

    while remaining:
        moved = True
        while moved and remaining:
            moved = False
            for vertex in sorted(remaining):
                if weight(vertex, True) == 0:
                    right.insert(0, vertex)
                elif weight(vertex, False) == 0:
                    left.append(vertex)
                else:
                    continue
                remaining.discard(vertex)
                moved = True
                break
        if remaining:
            best = max(sorted(remaining), key=lambda v: weight(v, True) - weight(v, False))
            left.append(best)
            remaining.discard(best)
    position = {vertex: i for i, vertex in enumerate(left + right)}
    back = [(pair, w) for pair, w in inside.items() if position[pair[0]] > position[pair[1]]]
    return [{'from': pair[0], 'to': pair[1], 'weight': w,
             'samples': [f'{a} -> {b}' for a, b in sorted(edges[pair])[:3]]}
            for pair, w in sorted(back, key=lambda item: (item[1], item[0]))]


def cycle_report(nodes, edges):
    report = []
    for members in strongly_connected(nodes, edges):
        report.append({'members': members,
                       'edge_count': sum(1 for a, b in edges if a in members and b in members),
                       'closing_edges': closing_edges(set(members), edges)})
    return report


def analyze_graph(classes, root_package, hub_packages):
    result = {'packages': {}, 'cycles': {}, 'edges': {}}
    for depth in (1, 2):
        nodes, edges = build_graph(classes, root_package, depth)
        no_hub_nodes, no_hub_edges = build_graph(classes, root_package, depth, hub_packages)
        key = f'depth{depth}'
        result['packages'][key] = package_metrics(classes, nodes, edges)
        result['edges'][key] = [[a, b, len(pairs)] for (a, b), pairs in sorted(edges.items())]
        result['cycles'][key] = {'full': cycle_report(nodes, edges),
                                 'without_hubs': cycle_report(no_hub_nodes, no_hub_edges)}
    return result
#endregion


#region Source scan [源码扫描]
def blank(text):
    return re.sub(r'[^\n]', ' ', text)


def strip_source(text):
    """The source with comments and string, char and text-block contents blanked; length and lines are kept."""
    out = []
    i, n = 0, len(text)
    while i < n:
        if text.startswith('//', i):
            j = text.find('\n', i)
            j = n if j < 0 else j
            out.append(' ' * (j - i))
        elif text.startswith('/*', i):
            j = text.find('*/', i + 2)
            j = n if j < 0 else j + 2
            out.append(blank(text[i:j]))
        elif text.startswith('"""', i):
            j = i + 3
            while j < n and not text.startswith('"""', j):
                j += 2 if text[j] == '\\' else 1
            j = min(n, j + 3)
            out.append(blank(text[i:j]))
        elif text[i] in '"\'':
            quote = text[i]
            j = i + 1
            while j < n and text[j] != quote and text[j] != '\n':
                j += 2 if text[j] == '\\' else 1
            j = min(n, j + 1)
            out.append(quote + ' ' * max(0, j - i - 2) + quote if j - i >= 2 else ' ' * (j - i))
        else:
            j = i + 1
            out.append(text[i])
        i = j
    return ''.join(out)


ANNOTATION = re.compile(r'@(?!interface\b)\w+(?:\.\w+)*(?:\s*\([^()]*\))?')
TYPE_DECLARATION = re.compile(r'@interface\s+(\w+)|\b(class|interface|enum|record)\s+(\w+)')
CONTROL = re.compile(r'^\s*(?:else\b\s*)?(?:if|for|while|switch|catch|synchronized|try|else|do|finally)\b')
STATEMENT_WORD = re.compile(r'\b(?:return|throw|yield|switch|case|assert)\b')
ASSIGNMENT = re.compile(r'(?<![=!<>])=(?![=>])')
COLLECTION_INITIALIZER = re.compile(r'new\s+[\w.]*(?:Map|Set|List|Deque|Queue)\s*[<(]')


class Frame:

    def __init__(self, kind, header, name='', sub=''):
        self.kind = kind
        self.header = header
        self.name = name
        self.sub = sub
        self.paren = 0
        self.in_constants = sub == 'enum'
        self.method_start = 0
        self.method_name = ''


def classify_header(header_text, parent):
    """'inline', 'method' or 'block'/'class' with the declared name and sub kind, from the text before a '{'."""
    text = ANNOTATION.sub(' ', header_text)
    if parent.in_constants:
        return 'class', '', ''
    if '->' in text:
        return 'inline', '', ''
    stripped = text.strip()
    if CONTROL.match(text) or not stripped or stripped == 'static':
        return 'block', '', ''
    declaration = TYPE_DECLARATION.search(text)
    if declaration:
        return 'class', declaration.group(1) or declaration.group(3), declaration.group(2) or 'interface'
    if ASSIGNMENT.search(text):
        return 'inline', '', ''
    if re.search(r'\bnew\b', text) and stripped.endswith(')'):
        return 'class', '', 'anonymous'
    if STATEMENT_WORD.search(text):
        return 'inline', '', ''
    if stripped.endswith(')') or re.search(r'\)\s*throws\s+[\w.,\s<>]+$', stripped):
        return 'method', '', ''
    if parent.kind == 'class' and parent.name and re.search(rf'\b{re.escape(parent.name)}$', stripped):
        return 'method', '', ''
    return 'block', '', ''


def scan_source(code):
    """(methods, statics): methods [(first line, name, lines)] for every method body, statics
    [(line, name, kind)] with kind 'mutable' for non-final static fields and 'collection' for static final ones
    initialized with a collection."""
    starts = [0] + [m.end() for m in re.finditer(r'\n', code)]

    def line_of(index):
        return bisect.bisect_right(starts, index)

    frames = [Frame('file', 0)]
    methods, statics = [], []
    for match in re.finditer(r'[(){};]', code):
        char, i = match.group(), match.start()
        frame = frames[-1]
        if char == '(':
            frame.paren += 1
        elif char == ')':
            frame.paren = max(0, frame.paren - 1)
        elif char == ';' and frame.paren == 0:
            if frame.kind == 'class':
                if frame.in_constants:
                    frame.in_constants = False
                elif frame.sub != 'interface':
                    record_static(code, frame.header, i, line_of, statics)
            frame.header = i + 1
        elif char == '{':
            if frame.paren > 0:
                frames.append(Frame('inline', i + 1))
                continue
            kind, name, sub = classify_header(code[frame.header:i], frame)
            child = Frame(kind, i + 1, name, sub)
            if kind == 'method':
                lead = code[frame.header:i]
                child.method_start = frame.header + len(lead) - len(lead.lstrip())
                child.method_name = method_name(lead)
            frames.append(child)
        elif char == '}' and len(frames) > 1:
            closed = frames.pop()
            if closed.kind == 'method':
                length = line_of(i) - line_of(closed.method_start) + 1
                methods.append((line_of(closed.method_start), closed.method_name, length))
            if closed.kind != 'inline':
                frames[-1].header = i + 1
    return methods, statics


def method_name(header_text):
    found = re.search(r'(\w+)\s*\(', ANNOTATION.sub(' ', header_text))
    return found.group(1) if found else '?'


def record_static(code, header_start, end, line_of, statics):
    text = code[header_start:end]
    plain = ANNOTATION.sub(lambda m: ' ' * len(m.group()), text)
    assignment = ASSIGNMENT.search(plain)
    declaration = plain[:assignment.start()] if assignment else plain
    if '(' in declaration or '{' in declaration:
        return
    words = re.findall(r'\w+', declaration)
    if 'static' not in words:
        return
    name = words[-1] if words else '?'
    line = line_of(header_start + re.search(r'\bstatic\b', plain).start())
    if 'final' not in words:
        statics.append((line, name, 'mutable'))
    elif assignment and COLLECTION_INITIALIZER.search(plain[assignment.end():]):
        statics.append((line, name, 'collection'))


def count_lines(text):
    return text.count('\n') + (0 if not text or text.endswith('\n') else 1)


def analyze_sources(source_root, project_root):
    """Size, long methods and static fields over every .java file under source_root (paths relative to
    project_root)."""
    files, long_methods, statics = [], [], []
    method_total = 0
    for path in sorted(Path(source_root).rglob('*.java')):
        text = path.read_text(encoding='utf-8', errors='replace')
        relative = path.relative_to(project_root).as_posix()
        files.append({'file': relative, 'lines': count_lines(text)})
        methods, found = scan_source(strip_source(text))
        method_total += len(methods)
        long_methods += [{'file': relative, 'line': line, 'method': name, 'lines': length}
                         for line, name, length in methods if length > LONG_METHOD_LINES]
        statics += [{'file': relative, 'line': line, 'field': name, 'kind': kind} for line, name, kind in found]
    histogram = {label: 0 for _, label in HISTOGRAM}
    for item in files:
        for limit, label in HISTOGRAM:
            if limit is None or item['lines'] <= limit:
                histogram[label] += 1
                break
    long_methods.sort(key=lambda m: (-m['lines'], m['file'], m['line']))
    return {
        'files': len(files), 'lines': sum(item['lines'] for item in files), 'histogram': histogram,
        'files_over_limit': sorted((item for item in files if item['lines'] > LONG_FILE_LINES),
                                   key=lambda item: (-item['lines'], item['file'])),
        'methods': method_total, 'methods_over_limit': long_methods,
        'mutable_statics': [s for s in statics if s['kind'] == 'mutable'],
        'static_final_collections': [s for s in statics if s['kind'] == 'collection'],
    }, {item['file']: item['lines'] for item in files}
#endregion


#region Git history [提交历史]
def parse_log(text):
    """[(sha, [(status, paths)])] newest first, from `git log --name-status --format=%x01%H`."""
    commits = []
    for line in text.splitlines():
        if line.startswith('\x01'):
            commits.append((line[1:].strip(), []))
        elif line.strip() and commits:
            fields = line.split('\t')
            commits[-1][1].append((fields[0], fields[1:]))
    return commits


def touched_files(commits, current):
    """[(sha, set of current files touched)] newest first; a rename is followed back to the current name, and a
    pure rename (100% similar) is a move, not a touch."""
    alias = {}
    result = []
    for sha, changes in commits:
        touched = set()
        for status, paths in changes:
            if status[0] in 'RC' and len(paths) == 2:
                new_name = alias.get(paths[1], paths[1])
                if status[0] == 'R':
                    alias[paths[0]] = new_name
                if status != 'R100':
                    touched.add(new_name)
            elif paths:
                touched.add(alias.get(paths[0], paths[0]))
        result.append((sha, touched & current))
    return result


def hotspots(commits, lines):
    counts = defaultdict(int)
    for _, touched in commits:
        for name in touched:
            counts[name] += 1
    rows = [{'file': name, 'commits': count, 'lines': lines[name], 'score': count * lines[name]}
            for name, count in counts.items()]
    return sorted(rows, key=lambda row: (-row['score'], row['file']))


def change_coupling(commits, max_files):
    """Pairs co-changed in at least COUPLING_MIN_COMMITS commits whose stronger direction has confidence
    >= COUPLING_MIN_CONFIDENCE (co-changes / commits of the file); commits touching more than max_files files
    (bulk renames, formatting) are left out of every count."""
    singles = defaultdict(int)
    pairs = defaultdict(int)
    used = 0
    for _, touched in commits:
        if not touched or len(touched) > max_files:
            continue
        used += 1
        for name in touched:
            singles[name] += 1
        for pair in combinations(sorted(touched), 2):
            pairs[pair] += 1
    rows = []
    for (a, b), together in pairs.items():
        if together < COUPLING_MIN_COMMITS:
            continue
        a_to_b, b_to_a = together / singles[a], together / singles[b]
        if max(a_to_b, b_to_a) >= COUPLING_MIN_CONFIDENCE:
            rows.append({'a': a, 'b': b, 'together': together, 'commits_a': singles[a], 'commits_b': singles[b],
                         'confidence_a_to_b': round(a_to_b, 3), 'confidence_b_to_a': round(b_to_a, 3)})
    rows.sort(key=lambda row: (-row['together'], -max(row['confidence_a_to_b'], row['confidence_b_to_a']),
                               row['a'], row['b']))
    return rows, used


def git_history(project_root, days, max_files, lines):
    since = (datetime.now(timezone.utc) - timedelta(days=days)).strftime('%Y-%m-%d')
    _, output, _ = env.git('log', f'--since={since}', '--no-merges', '-M', '--name-status', '--format=%x01%H',
                           '--', 'src/main/java', cwd=project_root)
    commits = touched_files(parse_log(output), set(lines))
    coupling, used = change_coupling(commits, max_files)
    return {'since': since, 'commits': len(commits), 'commits_in_coupling': used, 'max_commit_files': max_files,
            'hotspots': hotspots(commits, lines)[:HOTSPOT_ROWS], 'coupling': coupling}
#endregion


#region Tests [测试]
def analyze_tests(test_root):
    patterns = {'test': r'@Test\b', 'parameterized_test': r'@ParameterizedTest\b', 'game_test': r'@GameTest\b',
                'game_test_generator': r'@GameTestGenerator\b', 'arch_test': r'@ArchTest\b'}
    totals = {key: 0 for key in patterns}
    files = lines = with_tests = 0
    for path in sorted(Path(test_root).rglob('*.java')):
        text = path.read_text(encoding='utf-8', errors='replace')
        code = strip_source(text)
        files += 1
        lines += count_lines(text)
        found = {key: len(re.findall(pattern, code)) for key, pattern in patterns.items()}
        with_tests += any(found.values())
        for key, count in found.items():
            totals[key] += count
    return {**totals, 'source_files': files, 'source_files_with_tests': with_tests, 'source_lines': lines}
#endregion


#region Freshness [类文件新旧]
def freshness(source_root, classes_dir):
    """Warnings when the compiled classes do not match the sources they are read for."""
    warnings = []
    if not Path(classes_dir).is_dir() or not any(Path(classes_dir).rglob('*.class')):
        return [f'no compiled classes under {classes_dir}']
    missing, stale = [], []
    for path in Path(source_root).rglob('*.java'):
        if path.name in ('package-info.java', 'module-info.java'):
            continue
        compiled = Path(classes_dir) / path.relative_to(source_root).with_suffix('.class')
        if not compiled.exists():
            missing.append(path.relative_to(source_root).as_posix())
        elif path.stat().st_mtime > compiled.stat().st_mtime:
            stale.append(path.relative_to(source_root).as_posix())
    if missing:
        warnings.append(f'{len(missing)} source files have no class file (e.g. {sorted(missing)[0]})')
    if stale:
        warnings.append(f'{len(stale)} source files are newer than their class file (e.g. {sorted(stale)[0]})')
    return warnings
#endregion


#region Report [报告]
def analyze_project(name, root, days=DEFAULT_DAYS, max_commit_files=DEFAULT_MAX_COMMIT_FILES):
    config = MODS[name]
    root = Path(root)
    main_sources = root / 'src/main/java'
    classes_dir = root / 'build/classes/java/main'
    root_package = config['root_package']
    warnings = freshness(main_sources / root_package.replace('.', '/'), classes_dir / root_package.replace('.', '/'))
    classes = load_classes(classes_dir, root_package)
    graph = analyze_graph(classes, root_package, config['hub_packages'])
    size, lines = analyze_sources(main_sources, root)
    _, head, _ = env.git('rev-parse', 'HEAD', cwd=root)
    return {
        'meta': {'project': name, 'root_package': root_package, 'head': head.strip(),
                 'hub_packages': ['(root package classes)'] + [f'{h}..' for h in config['hub_packages']],
                 'class_files': len(classes), 'source_files': size['files'],
                 'thresholds': {'file_lines': LONG_FILE_LINES, 'method_lines': LONG_METHOD_LINES,
                                'outlier_min_classes': OUTLIER_MIN_CLASSES, 'outlier_distance': OUTLIER_D},
                 'warnings': warnings},
        'packages': graph['packages'], 'cycles': graph['cycles'], 'edges': graph['edges'],
        'size': size, 'git': git_history(root, days, max_commit_files, lines),
        'tests': analyze_tests(root / 'src/test'),
    }


def table(headers, rows):
    if not rows:
        return ['(none)']
    lines = ['| ' + ' | '.join(headers) + ' |', '|' + '|'.join('---' for _ in headers) + '|']
    lines += ['| ' + ' | '.join(str(cell) for cell in row) + ' |' for row in rows]
    return lines


def limited(rows, full):
    return rows if full else rows[:MD_ROWS]


def outliers(rows):
    picked = [row for row in rows if row['classes'] >= OUTLIER_MIN_CLASSES]
    return sorted(picked, key=lambda row: (-row['distance'], row['package']))


def render_markdown(report, full):
    meta, size, git, tests = report['meta'], report['size'], report['git'], report['tests']
    out = [f'# {meta["project"]} metrics', '',
           f'Head `{meta["head"][:8]}`, {meta["class_files"]} class files, {meta["source_files"]} source files, '
           f'hubs left out of the "without hubs" cycles: {", ".join(meta["hub_packages"])}']
    for warning in meta['warnings']:
        out.append(f'- WARNING: {warning}')
    for depth in ('depth1', 'depth2'):
        rows = report['packages'][depth]
        out += ['', f'## Packages at {depth[-1]} level ({len(rows)})', '']
        shown = rows if full else outliers(rows)[:MD_ROWS]
        out.append(f'D outliers (at least {OUTLIER_MIN_CLASSES} classes, worst first)' if not full else 'All packages')
        out += ['']
        out += table(['package', 'classes', 'abstract', 'Ce', 'Ca', 'I', 'A', 'D'],
                     [(r['package'], r['classes'], r['abstract'], r['ce'], r['ca'], r['instability'],
                       r['abstractness'], r['distance']) for r in shown])
    out += ['', '## Cycles', '']
    for depth in ('depth1', 'depth2'):
        for variant in ('full', 'without_hubs'):
            components = report['cycles'][depth][variant]
            out.append(f'### {depth[-1]} level, {variant.replace("_", " ")}: {len(components)} cycle group(s)')
            for component in limited(components, full):
                out += ['', f'- members ({len(component["members"])}): {", ".join(component["members"])}',
                        f'  closing edges ({len(component["closing_edges"])} of {component["edge_count"]}):']
                out += [f'  - {e["from"]} -> {e["to"]} ({e["weight"]} class pairs, e.g. {e["samples"][0]})'
                        for e in limited(component['closing_edges'], full)]
            out.append('')
    out += ['## Size', '',
            f'{size["files"]} files, {size["lines"]} lines; histogram: '
            + ', '.join(f'{label} {count}' for label, count in size['histogram'].items()), '',
            f'Files over {LONG_FILE_LINES} lines: {len(size["files_over_limit"])}', '']
    out += table(['file', 'lines'], [(f['file'], f['lines']) for f in limited(size['files_over_limit'], full)])
    out += ['', f'Methods over {LONG_METHOD_LINES} lines (approximate): {len(size["methods_over_limit"])} of '
            f'{size["methods"]}', '']
    out += table(['file:line', 'method', 'lines'], [(f'{m["file"]}:{m["line"]}', m['method'], m['lines'])
                                                    for m in limited(size['methods_over_limit'], full)])
    out += ['', f'Mutable statics (non-final): {len(size["mutable_statics"])}', '']
    out += table(['file:line', 'field'], [(f'{s["file"]}:{s["line"]}', s['field'])
                                          for s in limited(size['mutable_statics'], full)])
    out += ['', f'Static final collections (heuristic): {len(size["static_final_collections"])}', '']
    out += table(['file:line', 'field'], [(f'{s["file"]}:{s["line"]}', s['field'])
                                          for s in limited(size['static_final_collections'], full)])
    out += ['', f'## Git, since {git["since"]} ({git["commits"]} commits touching src/main/java)', '']
    out += table(['file', 'commits', 'lines', 'commits x lines'],
                 [(h['file'], h['commits'], h['lines'], h['score']) for h in limited(git['hotspots'], full)])
    out += ['', f'Change coupling: {len(git["coupling"])} pairs (>= {COUPLING_MIN_COMMITS} co-changes, confidence >= '
            f'{COUPLING_MIN_CONFIDENCE}; {git["commits_in_coupling"]} commits of at most {git["max_commit_files"]} '
            'files)', '']
    out += table(['a', 'b', 'together', 'a->b', 'b->a'],
                 [(c['a'], c['b'], c['together'], c['confidence_a_to_b'], c['confidence_b_to_a'])
                  for c in limited(git['coupling'], full)])
    out += ['', '## Tests', '',
            f'@Test {tests["test"]}, @ParameterizedTest {tests["parameterized_test"]}, @GameTest '
            f'{tests["game_test"]}, @GameTestGenerator {tests["game_test_generator"]}, @ArchTest '
            f'{tests["arch_test"]}; {tests["source_files"]} test source files '
            f'({tests["source_files_with_tests"]} with tests), {tests["source_lines"]} lines', '']
    return '\n'.join(out)


def write_reports(report, out_dir, full):
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    name = report['meta']['project']
    json_path = out_dir / f'{name}-metrics.json'
    markdown_path = out_dir / f'{name}-metrics.md'
    json_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    markdown_path.write_text(render_markdown(report, full), encoding='utf-8')
    return json_path, markdown_path


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('--out', required=True, help='directory for <project>-metrics.json and .md')
    parser.add_argument('--project', choices=sorted(MODS), help='one mod (default: both)')
    parser.add_argument('--full', action='store_true', help='no row limit in the markdown')
    parser.add_argument('--days', type=int, default=DEFAULT_DAYS, help='Git window (default 180)')
    parser.add_argument('--max-commit-files', type=int, default=DEFAULT_MAX_COMMIT_FILES,
                        help='commits touching more files are ignored for change coupling (default 40)')
    args = parser.parse_args(argv)
    env.require_linux()
    for name in [args.project] if args.project else sorted(MODS):
        report = analyze_project(name, env.project(name).root, args.days, args.max_commit_files)
        json_path, markdown_path = write_reports(report, args.out, args.full)
        for warning in report['meta']['warnings']:
            print(f'{name}: WARNING {warning}')
        print(f'{name}: {json_path}\n{name}: {markdown_path}')
    return 0
#endregion


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
