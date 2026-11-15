"""Static audit of both mods' JSON data: inventory, duplicates, region copies, references, reachability, lang.

Reads src/generated/resources and src/main/resources of Guzhenren and Gu World (project_runtime.PROJECTS) and writes
<out>/<project>-datapack.json (everything, uncut) and <out>/<project>-datapack.md (lists cut to 20 unless --full).

    python -B tools/datapack_audit.py --out DIR                    both projects
    python -B tools/datapack_audit.py --out DIR --project guworld  one project (the other is still indexed to resolve cross-mod ids)
    python -B tools/datapack_audit.py --out DIR --full             uncut lists in the markdown too
    python -B tools/datapack_audit.py --out DIR --regions a,b      override the auto-detected region names

Region names are the folders under data/<ns>/worldgen/{density_function,noise}/terrain/. A file is per-region when a
path segment or the file stem equals one of them; strict normalization rewrites terrain/<region>/ inside string
values, loose normalization rewrites every whole-word occurrence of a region name (and its underscore-free form).

Approximations: only JSON is read (other files are counted); numbers are compared as written (1 and 1.0 differ);
the kind of a reference is inferred from the referencing field, and a mod id in a field with no known kind is
reported as unclassified, never as dangling; ids built in Java are invisible, so reachability only softens that by
checking quoted literals in src/main/java.
"""
import argparse
import hashlib
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

import project_runtime as env


MOD_NAMESPACES = ('guzhenren', 'guworld')
CHECKED_NAMESPACES = MOD_NAMESPACES + ('minecraft',)
REFERENCE = re.compile(r'#?[a-z0-9_.-]+:[a-z0-9_./-]+')
RESOURCE_ROOTS = ('src/generated/resources', 'src/main/resources')
SHOWN = 20
TERRAIN_KINDS = ('worldgen/density_function', 'worldgen/noise')
ROOT_KINDS = ('dimension', 'dimension_type', 'worldgen/noise_settings')
DYNAMIC_TAG_REGISTRIES = {'damage_type', 'enchantment', 'dimension_type', 'instrument', 'painting_variant',
                          'jukebox_song', 'trim_pattern', 'trim_material', 'banner_pattern', 'wolf_variant'}
REGISTRY_TYPE_KEYS = {'type', 'function', 'condition', 'trigger'}
PLACED_FEATURE_KEYS = {'feature', 'features', 'default', 'feature_true', 'feature_false', 'vegetation_feature'}
SHIFT_TYPES = {'minecraft:shift_a', 'minecraft:shift_b', 'minecraft:shift'}
SKIP = object()


class Entry:
    """One file under a resource root: its path relative to that root and, for JSON, the parsed content."""
    __slots__ = ('project', 'root', 'rel', 'size', 'data', 'canonical', 'error', 'area', 'ns', 'kind', 'rid')

    def __init__(self, project, root, rel, size):
        self.project, self.root, self.rel, self.size = project, root, rel, size
        self.data = self.canonical = self.error = None
        self.area, self.ns, self.kind, self.rid = classify_path(rel.split('/'))

    @property
    def label(self):
        return f'{self.area}/{self.ns}/{self.kind}' if self.ns else self.area

    @property
    def node(self):
        return self.kind, self.rid


def classify_path(parts):
    """(area, namespace, kind, 'namespace:id') for a path relative to a resource root; kind is the registry folder."""
    area = parts[0] if parts[0] in ('data', 'assets') and len(parts) > 2 else 'root'
    if area == 'root':
        return area, '', '', ''
    ns, rest = parts[1], parts[2:]
    if area == 'data':
        if rest[0] in ('worldgen', 'neoforge') and len(rest) > 2:
            kind, tail = '/'.join(rest[:2]), rest[2:]
        elif rest[0] == 'tags' and len(rest) > 2:
            if rest[1] == 'worldgen' and len(rest) > 3:
                kind, tail = f'tags/worldgen/{rest[2]}', rest[3:]
            else:
                kind, tail = f'tags/{rest[1]}', rest[2:]
        else:
            kind, tail = rest[0], rest[1:]
    else:
        kind, tail = rest[0], rest[1:]
    return area, ns, kind, f'{ns}:{"/".join(tail)}'.removesuffix('.json')


def canonical_text(data):
    return json.dumps(data, sort_keys=True, separators=(',', ':'), ensure_ascii=False)


def scan_project(name, root, parse):
    entries, skipped = [], Counter()
    for resource_root in RESOURCE_ROOTS:
        base = root / resource_root
        if not base.is_dir():
            continue
        for path in sorted(base.rglob('*')):
            if not path.is_file() or '.cache' in path.relative_to(base).parts:
                continue
            rel = path.relative_to(base).as_posix()
            if not rel.endswith('.json'):
                skipped[resource_root] += 1
                entries.append(Entry(name, resource_root, rel, path.stat().st_size))
                continue
            entry = Entry(name, resource_root, rel, path.stat().st_size)
            if parse:
                try:
                    entry.data = json.loads(path.read_text(encoding='utf-8'))
                    entry.canonical = canonical_text(entry.data)
                except (ValueError, OSError) as error:
                    entry.error = str(error)
            entries.append(entry)
    return entries, skipped


def json_entries(entries):
    return [e for e in entries if e.rel.endswith('.json') and e.data is not None]


def human(size):
    return f'{size / 1024:.0f} KiB' if size < 1 << 20 else f'{size / (1 << 20):.2f} MiB'


# region Inventory
def inventory(entries, skipped):
    by_kind, by_ns, by_root = defaultdict(lambda: [0, 0]), defaultdict(lambda: [0, 0]), defaultdict(lambda: [0, 0])
    canonical = 0
    for e in entries:
        if not e.rel.endswith('.json'):
            continue
        for table, key in ((by_kind, e.label), (by_ns, f'{e.area}/{e.ns}' if e.ns else e.area), (by_root, e.root)):
            table[key][0] += 1
            table[key][1] += e.size
        canonical += len(e.canonical.encode()) if e.canonical else 0
    count = lambda table: {k: {'files': v[0], 'bytes': v[1]} for k, v in sorted(table.items())}
    return {'files': sum(v[0] for v in by_root.values()), 'bytes': sum(v[1] for v in by_root.values()),
            'canonical_bytes': canonical, 'non_json_skipped': dict(skipped), 'by_root': count(by_root),
            'by_namespace': count(by_ns), 'by_kind': count(by_kind)}
# endregion


# region Duplicates
def exact_duplicates(entries):
    groups = defaultdict(list)
    for e in json_entries(entries):
        groups[hashlib.sha1(e.canonical.encode()).hexdigest()].append(e)
    result, by_kind = [], defaultdict(lambda: {'groups': 0, 'redundant_files': 0, 'redundant_bytes': 0})
    for digest, group in groups.items():
        if len(group) < 2:
            continue
        redundant = sum(e.size for e in group[1:])
        kinds = sorted({e.label for e in group})
        result.append({'hash': digest[:12], 'count': len(group), 'bytes_each': group[0].size,
                       'redundant_bytes': redundant, 'kinds': kinds, 'files': [e.rel for e in group]})
        row = by_kind[kinds[0] if len(kinds) == 1 else 'mixed']
        row['groups'] += 1
        row['redundant_files'] += len(group) - 1
        row['redundant_bytes'] += redundant
    result.sort(key=lambda g: (-g['redundant_bytes'], g['files'][0]))
    return {'groups': len(result), 'redundant_files': sum(g['count'] - 1 for g in result),
            'redundant_bytes': sum(g['redundant_bytes'] for g in result),
            'by_kind': dict(sorted(by_kind.items())), 'list': result}
# endregion


# region Region copies
def detect_regions(entries):
    regions = set()
    for e in entries:
        parts = e.rel.split('/')
        if e.area == 'data' and e.kind in TERRAIN_KINDS and len(parts) > 6 and parts[4] == 'terrain':
            regions.add(parts[5])
    return sorted(regions)


class RegionNames:
    def __init__(self, regions):
        self.canonical = {r: r for r in regions} | {r.replace('_', ''): r for r in regions}
        alternatives = '|'.join(sorted(map(re.escape, self.canonical), key=len, reverse=True))
        self.strict = re.compile(rf'(?<=terrain/)(?:{alternatives})(?=/)')
        self.loose = re.compile(rf'(?<![a-z0-9_])(?:{alternatives})(?![a-z0-9_])')

    def normalize(self, node, pattern):
        if isinstance(node, str):
            return pattern.sub('<R>', node)
        if isinstance(node, list):
            return [self.normalize(v, pattern) for v in node]
        if isinstance(node, dict):
            return {k: self.normalize(v, pattern) for k, v in node.items()}
        return node

    def path_region(self, entry):
        """(region, normalized rid) when a path segment or the file stem is a region name, else None."""
        parts = entry.rid.split(':', 1)[1].split('/')
        for i, part in enumerate(parts):
            if part in self.canonical:
                return self.canonical[part], f'{entry.ns}:' + '/'.join(parts[:i] + ['<R>'] + parts[i + 1:])
        return None


def leaf_diffs(a, b, path='', limit=4):
    if len(path) > 200:
        return []
    if isinstance(a, dict) and isinstance(b, dict):
        out = []
        for key in sorted(set(a) | set(b)):
            if key not in a or key not in b:
                out.append((f'{path}/{key}', a.get(key, '<absent>'), b.get(key, '<absent>')))
            else:
                out += leaf_diffs(a[key], b[key], f'{path}/{key}', limit)
            if len(out) >= limit:
                break
        return out[:limit]
    if isinstance(a, list) and isinstance(b, list) and len(a) == len(b):
        out = []
        for i, (x, y) in enumerate(zip(a, b)):
            out += leaf_diffs(x, y, f'{path}[{i}]', limit)
            if len(out) >= limit:
                break
        return out[:limit]
    return [] if a == b else [(path or '/', a, b)]


def short(value):
    text = json.dumps(value, ensure_ascii=False)
    return text if len(text) <= 80 else text[:77] + '...'


def region_copies(entries, regions):
    if not regions:
        return None
    names = RegionNames(regions)
    families = defaultdict(lambda: defaultdict(list))
    for e in json_entries(entries):
        hit = names.path_region(e) if e.area == 'data' else None
        if hit:
            families[(e.ns, e.kind, hit[1])][hit[0]].append(e)
    patterns = Counter()
    for e in entries:
        if e.area == 'data' and names.path_region(e):
            patterns[f'{e.label}/' + names.path_region(e)[1].split(':', 1)[1].split('<R>')[0] + '<R>'] += 1
    table = defaultdict(lambda: {'files': 0, 'bytes': 0, 'families': 0, 'strict_bodies': 0, 'strict_saved_bytes': 0,
                                 'loose_bodies': 0, 'loose_saved_bytes': 0, 'differing_families': 0})
    differing, incomplete = [], []
    for (ns, kind, nid), by_region in sorted(families.items()):
        row = table[f'{ns}/{kind}']
        members = [e for group in by_region.values() for e in group]
        row['files'] += len(members)
        row['bytes'] += sum(e.size for e in members)
        row['families'] += 1
        if len(by_region) != len(regions):
            incomplete.append({'family': f'{kind}/{nid}', 'regions': sorted(by_region)})
        bodies = {}
        for mode, pattern in (('strict', names.strict), ('loose', names.loose)):
            groups = defaultdict(list)
            for e in members:
                normalized = names.normalize(e.data, pattern)
                groups[canonical_text(normalized)].append((e, normalized))
            row[f'{mode}_bodies'] += len(groups)
            row[f'{mode}_saved_bytes'] += sum(g[0][0].size * (len(g) - 1) for g in groups.values())
            bodies[mode] = list(groups.values())
        if len(bodies['strict']) > 1:
            row['differing_families'] += 1
            base = bodies['strict'][0]
            differing.append({
                'family': f'{kind}/{nid}', 'strict_bodies': len(bodies['strict']),
                'loose_identical': len(bodies['loose']) == 1,
                'groups': [sorted(names.canonical[names.path_region(e)[0]] for e, _ in g) for g in bodies['strict']],
                'diffs': [{'against': [names.path_region(e)[0] for e, _ in g], 'leaves': [
                    {'at': p, 'a': short(x), 'b': short(y)} for p, x, y in leaf_diffs(base[0][1], g[0][1])]}
                    for g in bodies['strict'][1:]]})
    other = Counter()
    for e in json_entries(entries):
        if e.area == 'data' and not names.path_region(e):
            hits = names.loose.findall(canonical_text(e.data))
            if hits:
                other[f'{e.label}: {Counter(hits).most_common(1)[0][0]}'] += 1
    totals = {key: sum(r[key] for r in table.values()) for key in
              ('files', 'bytes', 'families', 'strict_bodies', 'strict_saved_bytes', 'loose_bodies',
               'loose_saved_bytes', 'differing_families')}
    terrain = {key: sum(r[key] for k, r in table.items() if k.split('/', 1)[1] in TERRAIN_KINDS) for key in totals}
    return {'regions': regions, 'terrain_totals': terrain, 'path_patterns': dict(sorted(patterns.items())), 'totals': totals,
            'by_kind': dict(sorted(table.items())), 'differing': differing, 'incomplete_families': incomplete,
            'non_region_files_mentioning_a_region': dict(sorted(other.items()))}
# endregion


# region References
def nearest_key(keys):
    return next((k for k in reversed(keys) if k != '[]'), '')


def tag_registry(entry):
    return entry.kind.removeprefix('tags/') if entry.kind.startswith('tags/') else None


def classify_reference(entry, keys, raw, holder):
    """What a mod/minecraft id in entry at keys points to: ('data', kind), ('tag', registry|None), ('model',),
    ('texture',), None (unclassified) or SKIP (a registry type name, not a data reference)."""
    last = nearest_key(keys)
    if entry.area == 'assets':
        if entry.kind in ('models', 'blockstates'):
            if last in ('parent', 'model'):
                return ('model',)
            if 'textures' in keys:
                return ('texture', '')
        if entry.kind == 'particles' and 'textures' in keys:
            return ('texture', 'particle/')
        if entry.kind == 'font' and last == 'file':
            return ('asset', 'textures/')
        if last == 'texture' and raw.endswith('.png'):
            return ('asset', '')
        return None
    kind = entry.kind
    if last in REGISTRY_TYPE_KEYS and not (kind == 'dimension' and keys == ('type',)):
        return SKIP
    tag = raw.startswith('#')
    if tag:
        if kind.startswith('tags/'):
            return ('tag', tag_registry(entry))
        return ('tag', 'worldgen/biome') if last == 'biomes' else ('tag', None)
    if kind == 'dimension':
        if keys == ('type',):
            return ('data', 'dimension_type')
        if last == 'settings':
            return ('data', 'worldgen/noise_settings')
    if last in ('biome', 'biome_is') or (last == 'biomes' and kind == 'neoforge/biome_modifier'):
        return ('data', 'worldgen/biome')
    if last in ('noise', 'meander') or (last == 'argument' and holder.get('type') in SHIFT_TYPES):
        return ('data', 'worldgen/noise')
    if last == 'feature' and 'placement' in holder:
        return ('data', 'worldgen/configured_feature')
    if kind == 'worldgen/density_function' or (kind == 'worldgen/noise_settings' and keys[:1] == ('noise_router',)):
        return ('data', 'worldgen/density_function')
    if kind == 'worldgen/biome' and 'features' in keys:
        return ('data', 'worldgen/placed_feature')
    if kind == 'neoforge/biome_modifier' and last == 'features':
        return ('data', 'worldgen/placed_feature')
    if kind == 'worldgen/configured_feature' and last in PLACED_FEATURE_KEYS:
        return ('data', 'worldgen/placed_feature')
    if last in ('structure', 'structures', 'structure_set'):
        return ('data', 'worldgen/structure')
    if last == 'loot_table':
        return ('data', 'loot_table')
    if last in ('recipe', 'recipes'):
        return ('data', 'recipe')
    if kind == 'advancement' and last == 'parent':
        return ('data', 'advancement')
    if kind.startswith('tags/') and last in ('values', 'id'):
        registry = tag_registry(entry)
        if registry.startswith('worldgen/') or registry in DYNAMIC_TAG_REGISTRIES:
            return ('data', registry)
    return None


def walk_strings(node, keys=(), holder=None):
    """(key path, string, nearest enclosing object) for every string; list items add '[]' to the path."""
    if isinstance(node, str):
        yield keys, node, holder or {}
    elif isinstance(node, list):
        for v in node:
            yield from walk_strings(v, keys + ('[]',), holder)
    elif isinstance(node, dict):
        for k, v in node.items():
            yield from walk_strings(v, keys + (k,), node)


class Index:
    """Which resources exist: data ids by kind, tag ids by registry, asset paths, across every indexed project."""

    def __init__(self, entries):
        self.data = defaultdict(set)
        self.assets = set()
        self.any_kind = defaultdict(set)
        for e in entries:
            if e.area == 'data' and e.rel.endswith('.json'):
                self.data[e.kind].add(e.rid)
                self.any_kind[e.rid].add(e.kind)
            elif e.area == 'assets':
                self.assets.add(e.rel)

    def exists(self, target, ns, path):
        rid = f'{ns}:{path}'
        if target[0] == 'data':
            return rid in self.data[target[1]]
        if target[0] == 'tag':
            return any(rid in self.data[k] for k in self.data if k.startswith('tags/')
                       and (target[1] is None or k == f'tags/{target[1]}'))
        if target[0] == 'model':
            return f'assets/{ns}/models/{path}.json' in self.assets
        if target[0] == 'asset':
            return f'assets/{ns}/{target[1]}{path}' in self.assets
        return f'assets/{ns}/textures/{target[1]}{path}.png' in self.assets


def target_node(target, rid, index):
    if target[0] == 'data':
        return target[1], rid
    if target[0] == 'tag':
        kinds = [f'tags/{target[1]}'] if target[1] else [k for k in index.data if k.startswith('tags/')]
        return next(((k, rid) for k in kinds if rid in index.data[k]), None)
    return None


def references(entries, index, lang_keys):
    totals, tags, skipped_types = Counter(), Counter(), 0
    dangling, unclassified, edges = [], Counter(), []
    unresolved = Counter()
    unresolved_samples = defaultdict(list)
    for e in json_entries(entries):
        if e.kind == 'lang':
            continue
        for keys, raw, holder in walk_strings(e.data):
            if not REFERENCE.fullmatch(raw):
                continue
            ns, path = raw.lstrip('#').split(':', 1)
            if ns not in CHECKED_NAMESPACES:
                continue
            totals[ns] += 1
            target = classify_reference(e, keys, raw, holder)
            if target is SKIP:
                skipped_types += 1
                continue
            if raw.startswith('#'):
                tags[ns] += 1
            context = f'{e.label}: {nearest_key(keys) or "<root>"}'
            if target is None:
                if ns in MOD_NAMESPACES:
                    unclassified[context] += 1
                    if f'{ns}:{path}' not in index.any_kind:
                        guesses = (f'{p}.{ns}.{path.replace("/", ".")}' for p in ('item', 'block', 'entity', 'effect'))
                        if not any(g in lang_keys for g in guesses):
                            unresolved[context] += 1
                            if len(unresolved_samples[context]) < 5:
                                unresolved_samples[context].append(f'{e.rel} -> {raw}')
                continue
            if ns not in MOD_NAMESPACES:
                continue
            if index.exists(target, ns, path):
                node = target_node(target, f'{ns}:{path}', index)
                if node:
                    edges.append((e.node, node, e.project))
            else:
                dangling.append({'file': e.rel, 'field': '/'.join(keys), 'ref': raw,
                                 'expected': target[1] if len(target) > 1 and target[1] else target[0]})
    return {'total_by_namespace': dict(totals), 'registry_type_strings_skipped': skipped_types,
            'tag_references': dict(tags), 'dangling_count': len(dangling), 'dangling': dangling, 'unclassified_mod_references': dict(unclassified.most_common()),
            'unclassified_not_a_data_file_nor_in_lang': dict(unresolved.most_common()),
            'unclassified_samples': dict(unresolved_samples)}, edges
# endregion


# region Reachability
def reachability(entries, edges, ns, java_text):
    mine = [e for e in entries if e.area == 'data' and e.rel.endswith('.json')]
    nodes = {e.node: e for e in mine}
    forward, indegree = defaultdict(set), Counter()
    for source, target, _ in edges:
        if source != target:
            forward[source].add(target)
            indegree[target] += 1
    for e in mine:
        if e.kind.startswith('tags/'):
            registry = e.kind.removeprefix('tags/')
            for keys, raw, _ in walk_strings(e.data):
                if nearest_key(keys) not in ('values', 'id'):
                    continue
                member = raw.lstrip('#')
                kind = e.kind if raw.startswith('#') else registry
                if (kind, member) in nodes:
                    forward[e.node].add((kind, member))
                    indegree[(kind, member)] += 1
    roots = [n for n, e in nodes.items() if e.kind in ROOT_KINDS or e.kind.startswith('neoforge/')]
    seen, stack = set(roots), list(roots)
    while stack:
        for nxt in forward[stack.pop()]:
            if nxt not in seen:
                seen.add(nxt)
                stack.append(nxt)
    dimension_biomes = {t for n, targets in forward.items() if n[0] == 'dimension' for t in targets
                        if t[0] == 'worldgen/biome'}
    table, listing = {}, {}
    for kind in sorted({e.kind for e in mine if e.kind.startswith('worldgen/') and e.ns == ns}):
        members = sorted(n for n, e in nodes.items() if e.kind == kind and e.ns == ns)
        unreachable = [n for n in members if n not in seen]
        orphan = [n for n in members if indegree[n] == 0 and nodes[n].kind not in ROOT_KINDS]
        in_java = [n for n in unreachable if java_mentions(n[1], java_text)]
        table[kind] = {'total': len(members), 'reachable': len(members) - len(unreachable),
                       'unreachable': len(unreachable), 'never_referenced': len(orphan),
                       'unreachable_but_named_in_java': len(in_java),
                       'unreachable_bytes': sum(nodes[n].size for n in unreachable)}
        listing[kind] = {'unreachable': [{'id': n[1], 'bytes': nodes[n].size, 'named_in_java': n in in_java}
                                         for n in unreachable],
                         'never_referenced': [n[1] for n in orphan]}
    biomes = sorted(n[1] for n, e in nodes.items() if e.kind == 'worldgen/biome' and e.ns == ns)
    return {'roots': len(roots), 'by_kind': table, 'lists': listing,
            'biomes_not_in_a_dimension_biome_source': [b for b in biomes if ('worldgen/biome', b) not in dimension_biomes]}


def java_mentions(rid, java_text):
    path = rid.split(':', 1)[1]
    return bool(java_text) and (f'"{path}"' in java_text or f':{path}"' in java_text)
# endregion


# region Lang
def lang_gaps(entries):
    files = defaultdict(lambda: defaultdict(set))
    for e in json_entries(entries):
        if e.area == 'assets' and e.kind == 'lang' and isinstance(e.data, dict):
            files[e.ns][e.rel.rsplit('/', 1)[1].removesuffix('.json')] |= set(e.data)
    result = {}
    for ns, languages in sorted(files.items()):
        reference = languages.get('en_us', set())
        result[ns] = {'keys': {lang: len(keys) for lang, keys in sorted(languages.items())}}
        for lang, keys in sorted(languages.items()):
            if lang == 'en_us':
                continue
            result[ns][f'missing_in_{lang}'] = sorted(reference - keys)
            result[ns][f'only_in_{lang}'] = sorted(keys - reference)
    return result
# endregion


def audit_project(name, entries, skipped, index, lang_keys, regions, root):
    ns = name
    java = root / 'src' / 'main' / 'java'
    java_text = '\n'.join(p.read_text(encoding='utf-8', errors='replace') for p in java.rglob('*.java')) \
        if java.is_dir() else ''
    refs, edges = references(entries, index, lang_keys)
    duplicated_paths = [rel for rel, count in Counter(e.rel for e in entries).items() if count > 1]
    return {'project': name, 'inventory': inventory(entries, skipped), 'exact_duplicates': exact_duplicates(entries),
            'region_copies': region_copies(entries, regions), 'references': refs,
            'reachability': reachability(entries, edges, ns, java_text), 'lang': lang_gaps(entries),
            'warnings': {'parse_errors': [{'file': e.rel, 'error': e.error} for e in entries if e.error],
                         'same_path_in_both_resource_roots': duplicated_paths}}


# region Markdown
def table(rows, header):
    lines = ['| ' + ' | '.join(header) + ' |', '|' + '|'.join('---' for _ in header) + '|']
    return lines + ['| ' + ' | '.join(str(c) for c in row) + ' |' for row in rows]


def cut(items, full):
    return list(items) if full else list(items)[:SHOWN]


def render(report, full):
    out = [f'# {report["project"]} datapack audit', '']
    inv = report['inventory']
    out += ['## Inventory', '', f'{inv["files"]} JSON files, {human(inv["bytes"])} on disk, '
            f'{human(inv["canonical_bytes"])} compact; non-JSON files skipped: {inv["non_json_skipped"]}', '']
    out += table([(k, v['files'], human(v['bytes'])) for k, v in inv['by_namespace'].items()], ['namespace', 'files', 'bytes'])
    out += [''] + table([(k, v['files'], human(v['bytes'])) for k, v in inv['by_kind'].items()], ['kind', 'files', 'bytes'])
    dup = report['exact_duplicates']
    out += ['', '## Exact duplicates', '', f'{dup["groups"]} groups, {dup["redundant_files"]} redundant files, '
            f'{human(dup["redundant_bytes"])} redundant', '']
    out += table([(k, v['groups'], v['redundant_files'], human(v['redundant_bytes'])) for k, v in dup['by_kind'].items()],
                 ['kind', 'groups', 'redundant files', 'bytes'])
    out += [''] + table([(g['count'], human(g['redundant_bytes']), ', '.join(g['kinds']), g['files'][0] + (' ...' if g['count'] > 1 else ''))
                         for g in cut(dup['list'], full)], ['copies', 'redundant', 'kind', 'first file'])
    region = report['region_copies']
    if region:
        t = region['totals']
        k = region['terrain_totals']
        out += ['', '## Region copies', '', f'regions: {", ".join(region["regions"])}', '',
                f'terrain only (density_function + noise): {k["files"]} files ({human(k["bytes"])}) -> {k["strict_bodies"]} strict bodies, '
                f'{human(k["strict_saved_bytes"])} saved; {k["differing_families"]} families differ', '',
                f'{t["files"]} per-region files ({human(t["bytes"])}) in {t["families"]} families. '
                f'Strict (terrain/R/): {t["strict_bodies"]} distinct bodies, {human(t["strict_saved_bytes"])} saved if shared. '
                f'Loose (any whole-word region name): {t["loose_bodies"]} bodies, {human(t["loose_saved_bytes"])} saved. '
                f'{t["differing_families"]} families differ between regions (strict).', '']
        out += table([(k, v['files'], v['families'], v['strict_bodies'], human(v['strict_saved_bytes']), v['loose_bodies'],
                       human(v['loose_saved_bytes']), v['differing_families']) for k, v in region['by_kind'].items()],
                     ['kind', 'files', 'families', 'strict bodies', 'strict saved', 'loose bodies', 'loose saved', 'differing'])
        out += ['', 'Path patterns: ' + '; '.join(f'`{k}` x{v}' for k, v in region['path_patterns'].items())]
        if region['incomplete_families']:
            out += ['', f'Families missing a region: {len(region["incomplete_families"])}']
        if region['non_region_files_mentioning_a_region']:
            out += ['', 'Other files mentioning a region name: ' + '; '.join(
                f'{k} x{v}' for k, v in region['non_region_files_mentioning_a_region'].items())]
        out += ['', f'### Per-region families that differ ({len(region["differing"])})', '']
        for d in cut(region['differing'], full):
            out.append(f'- `{d["family"]}`: {d["strict_bodies"]} bodies {d["groups"]}'
                       + (' (loose-identical)' if d['loose_identical'] else ''))
            for diff in d['diffs'][:2]:
                for leaf in diff['leaves'][:3]:
                    out.append(f'  - {leaf["at"]}: {d["groups"][0]} {leaf["a"]} | {diff["against"]} {leaf["b"]}')
    refs = report['references']
    out += ['', '## References', '', f'strings by namespace: {refs["total_by_namespace"]} (+{refs["registry_type_strings_skipped"]} registry type names skipped); tag references: {refs["tag_references"]}',
            f'dangling: {refs["dangling_count"]}', '']
    out += table([(d['file'], d['field'], d['ref'], d['expected']) for d in cut(refs['dangling'], full)],
                 ['file', 'field', 'reference', 'expected kind'])
    if refs['unclassified_mod_references']:
        out += ['', 'Unclassified mod ids (field has no known kind): ' + '; '.join(
            f'{k} x{v}' for k, v in cut(refs['unclassified_mod_references'].items(), full))]
    if refs['unclassified_not_a_data_file_nor_in_lang']:
        out += ['', 'Of those, neither a data file nor an item/block/entity/effect lang key: ' + '; '.join(
            f'{k} x{v}' for k, v in cut(refs['unclassified_not_a_data_file_nor_in_lang'].items(), full))]
    reach = report['reachability']
    out += ['', '## Reachability', '', f'{reach["roots"]} roots (dimension, dimension_type, noise_settings, neoforge/*); '
            'worldgen kinds of this project only', '']
    out += table([(k, v['total'], v['reachable'], v['unreachable'], v['never_referenced'], v['unreachable_but_named_in_java'],
                   human(v['unreachable_bytes'])) for k, v in reach['by_kind'].items()],
                 ['kind', 'total', 'reachable', 'unreachable', 'never referenced', 'unreachable but named in Java', 'unreachable bytes'])
    for kind, lists in reach['lists'].items():
        if lists['unreachable']:
            out += ['', f'Unreachable {kind} ({len(lists["unreachable"])}): ' + ', '.join(
                f'`{x["id"]}`' + ('*' if x['named_in_java'] else '') for x in cut(lists['unreachable'], full)) + '  (* named in Java)']
    if reach['biomes_not_in_a_dimension_biome_source']:
        out += ['', 'Biomes in no dimension biome source: ' + ', '.join(
            f'`{b}`' for b in cut(reach['biomes_not_in_a_dimension_biome_source'], full))]
    out += ['', '## Lang', '']
    for ns, languages in report['lang'].items():
        out.append(f'- {ns}: keys {languages["keys"]}')
        for key, value in languages.items():
            if key != 'keys' and value:
                out.append(f'  - {key}: {len(value)} ({", ".join(cut(value, full)[:5])}{" ..." if len(value) > 5 else ""})')
    warnings = report['warnings']
    if warnings['parse_errors'] or warnings['same_path_in_both_resource_roots']:
        out += ['', '## Warnings', '', json.dumps(warnings, ensure_ascii=False, indent=1)]
    return '\n'.join(out) + '\n'
# endregion


def lang_key_set(entries):
    keys = set()
    for e in json_entries(entries):
        if e.area == 'assets' and e.kind == 'lang' and e.rel.endswith('/en_us.json') and isinstance(e.data, dict):
            keys |= set(e.data)
    return keys


def run(roots, selected, out_dir, full=False, regions_override=None):
    """roots: {project name: repository root}; selected: names to audit; every root is indexed for cross-mod ids."""
    scanned = {name: scan_project(name, root, parse=name in selected) for name, root in roots.items()}
    index = Index([e for entries, _ in scanned.values() for e in entries])
    out_dir.mkdir(parents=True, exist_ok=True)
    reports = {}
    for name in selected:
        entries, skipped = scanned[name]
        regions = regions_override if regions_override is not None else detect_regions(entries)
        report = audit_project(name, entries, skipped, index, lang_key_set(entries), regions, roots[name])
        (out_dir / f'{name}-datapack.json').write_text(json.dumps(report, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
        (out_dir / f'{name}-datapack.md').write_text(render(report, full), encoding='utf-8')
        reports[name] = report
    return reports


def summary_line(report):
    inv, dup, refs = report['inventory'], report['exact_duplicates'], report['references']
    unreachable = sum(v['unreachable'] for v in report['reachability']['by_kind'].values())
    region = report['region_copies']
    collapse = (f', region copies {region["totals"]["files"]}->{region["totals"]["strict_bodies"]} bodies '
                f'(-{human(region["totals"]["strict_saved_bytes"])})') if region else ''
    return (f'{report["project"]}: {inv["files"]} JSON ({human(inv["bytes"])}), {dup["redundant_files"]} redundant exact copies '
            f'({human(dup["redundant_bytes"])}){collapse}, {refs["dangling_count"]} dangling, {unreachable} unreachable worldgen')


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('--out', required=True, type=Path, help='directory for <project>-datapack.json and .md')
    parser.add_argument('--project', choices=['both'] + sorted(env.PROJECTS), default='both')
    parser.add_argument('--full', action='store_true', help='do not cut lists in the markdown')
    parser.add_argument('--regions', help='comma-separated region names instead of auto-detection')
    args = parser.parse_args(argv)
    roots = {p.name: p.root for p in env.all_projects()}
    selected = sorted(roots) if args.project == 'both' else [args.project]
    regions = [r for r in args.regions.split(',') if r] if args.regions is not None else None
    for report in run(roots, selected, args.out.expanduser().resolve(), args.full, regions).values():
        print(summary_line(report))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
