"""Unit checks for code_metrics.py on tiny synthetic class files, sources and Git histories."""
import json
import os
import struct
import subprocess
import tempfile
import time
import unittest
from pathlib import Path
from unittest import mock

import code_metrics as cm

ROOT = 'net.alex.mod'
PREFIX = 'net/alex/mod/'


def class_bytes(name, access=0x21, strings=()):
    """A minimal class file: a long constant (two slots) and an int first, so the reader must skip them right."""
    entries = [bytes([1]) + struct.pack('>H', len(name)) + name.encode(),
               bytes([7]) + struct.pack('>H', 1),
               bytes([5]) + struct.pack('>q', 7), bytes([3]) + struct.pack('>i', 1)]
    for text in strings:
        entries.append(bytes([1]) + struct.pack('>H', len(text)) + text.encode())
    count = 1 + 1 + 1 + 2 + 1 + len(strings)
    return (b'\xca\xfe\xba\xbe' + struct.pack('>HHH', 0, 65, count) + b''.join(entries)
            + struct.pack('>HHH', access, 2, 0))


def write_class(classes_dir, name, access=0x21, strings=()):
    path = Path(classes_dir) / (name.replace('.', '/') + '.class')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(class_bytes(name.replace('.', '/'), access, strings))


def refs_to(*names):
    return [f'(L{PREFIX}{name.replace(".", "/")};)V' for name in names]


class ClassReaderTests(unittest.TestCase):

    def test_reads_name_access_and_every_mod_class_spelled_in_the_constants(self):
        data = class_bytes(PREFIX + 'a/Main', 0x421, [
            f'(L{PREFIX}b/B;I)L{PREFIX}c/C;', f'Ljava/util/List<L{PREFIX}d/D;>;', f'[L{PREFIX}e/E;',
            'Ljava/lang/String;', f'L{PREFIX}f/Outer$Inner;'])
        name, access, refs = cm.read_class(data, PREFIX)
        self.assertEqual(f'{ROOT}.a.Main', name)
        self.assertEqual(0x421, access)
        self.assertEqual({f'{ROOT}.a.Main', f'{ROOT}.b.B', f'{ROOT}.c.C', f'{ROOT}.d.D', f'{ROOT}.e.E',
                          f'{ROOT}.f.Outer$Inner'}, refs)

    def test_rejects_other_files(self):
        with self.assertRaises(ValueError):
            cm.read_class(b'not a class', PREFIX)

    def test_named_types_skip_anonymous_local_and_synthetic_classes(self):
        self.assertTrue(cm.is_named_type(f'{ROOT}.a.Foo$Bar', 0x21))
        self.assertFalse(cm.is_named_type(f'{ROOT}.a.Foo$1', 0x21))
        self.assertFalse(cm.is_named_type(f'{ROOT}.a.Foo$1Local', 0x21))
        self.assertFalse(cm.is_named_type(f'{ROOT}.a.Foo$Bar', 0x1021))
        self.assertFalse(cm.is_named_type(f'{ROOT}.a.package-info', 0x1200))


def sample_classes(tmp):
    """a.Impl -> b.Api (interface); a.Impl$1 -> b.Api; b.Api -> a.Util is not there; a.Util inside a."""
    write_class(tmp, f'{ROOT}.a.Impl', strings=refs_to('b.Api', 'a.Util'))
    write_class(tmp, f'{ROOT}.a.Impl$1', strings=refs_to('b.Api'))
    write_class(tmp, f'{ROOT}.a.Util')
    write_class(tmp, f'{ROOT}.b.Api', access=0x601)
    write_class(tmp, f'{ROOT}.b.Abstract', access=0x421, strings=refs_to('c.sub.Deep'))
    write_class(tmp, f'{ROOT}.c.sub.Deep', strings=refs_to('a.Impl'))
    write_class(tmp, f'{ROOT}.Main', strings=refs_to('a.Impl'))
    return cm.load_classes(tmp, ROOT)


class GraphTests(unittest.TestCase):

    def test_nodes_follow_the_depth_and_the_root_package_is_its_own_node(self):
        self.assertEqual('a', cm.node_of(f'{ROOT}.a.Impl', ROOT, 1))
        self.assertEqual('c.sub', cm.node_of(f'{ROOT}.c.sub.deep.Deep', ROOT, 2))
        self.assertEqual('c', cm.node_of(f'{ROOT}.c.sub.deep.Deep', ROOT, 1))
        self.assertEqual('a', cm.node_of(f'{ROOT}.a.Impl', ROOT, 2))
        self.assertEqual('(root)', cm.node_of(f'{ROOT}.Main', ROOT, 2))

    def test_metrics_count_distinct_other_packages_and_ignore_inner_edges(self):
        with tempfile.TemporaryDirectory() as scratch:
            classes = sample_classes(scratch)
        nodes, edges = cm.build_graph(classes, ROOT, 1)
        self.assertNotIn(('a', 'a'), edges)
        self.assertEqual({('(root)', 'a'), ('a', 'b'), ('b', 'c'), ('c', 'a')}, set(edges))
        self.assertEqual(1, len(edges[('a', 'b')]))
        rows = {row['package']: row for row in cm.package_metrics(classes, nodes, edges)}
        a = rows['a']
        # Impl and Util count, the anonymous Impl$1 does not; a depends on b only, a is used by (root) and c.
        self.assertEqual((2, 0, 1, 2), (a['classes'], a['abstract'], a['ce'], a['ca']))
        self.assertEqual((0.333, 0.0, 0.667), (a['instability'], a['abstractness'], a['distance']))
        b = rows['b']
        self.assertEqual((2, 2), (b['classes'], b['abstract']))
        self.assertEqual((1, 1, 0.5, 1.0, 0.5), (b['ce'], b['ca'], b['instability'], b['abstractness'],
                                                 b['distance']))
        self.assertEqual(['a'], rows['b']['depended_by'])

    def test_a_cycle_closes_through_the_cheapest_edges_and_hubs_can_be_left_out(self):
        with tempfile.TemporaryDirectory() as scratch:
            write_class(scratch, f'{ROOT}.x.X1', strings=refs_to('y.Y1', 'y.Y2'))
            write_class(scratch, f'{ROOT}.x.X2', strings=refs_to('y.Y1'))
            write_class(scratch, f'{ROOT}.y.Y1', strings=refs_to('z.Z1'))
            write_class(scratch, f'{ROOT}.y.Y2', strings=refs_to('z.Z1'))
            write_class(scratch, f'{ROOT}.z.Z1', strings=refs_to('x.X1', 'registry.Hub'))
            write_class(scratch, f'{ROOT}.registry.Hub', strings=refs_to('w.W'))
            write_class(scratch, f'{ROOT}.w.W', strings=refs_to('z.Z1'))
            write_class(scratch, f'{ROOT}.Boot', strings=refs_to('w.W'))
            classes = cm.load_classes(scratch, ROOT)
        report = cm.analyze_graph(classes, ROOT, ('registry',))
        full = report['cycles']['depth1']['full']
        self.assertEqual([['registry', 'w', 'x', 'y', 'z']], [c['members'] for c in full])
        clean = report['cycles']['depth1']['without_hubs']
        self.assertEqual([['x', 'y', 'z']], [c['members'] for c in clean])
        closing = clean[0]['closing_edges']
        self.assertEqual([('z', 'x', 1)], [(e['from'], e['to'], e['weight']) for e in closing])
        self.assertEqual(['z.Z1 -> x.X1'], closing[0]['samples'])
        self.assertEqual(3, clean[0]['edge_count'])

    def test_hubs_are_the_root_package_and_the_named_packages_only(self):
        self.assertTrue(cm.is_hub(f'{ROOT}.Main$1', ROOT, ('registry',)))
        self.assertTrue(cm.is_hub(f'{ROOT}.registry.item.ModItems', ROOT, ('registry',)))
        self.assertFalse(cm.is_hub(f'{ROOT}.registryx.Thing', ROOT, ('registry',)))
        self.assertFalse(cm.is_hub(f'{ROOT}.a.Thing', ROOT, ()))


class SourceScanTests(unittest.TestCase):

    def scan(self, text):
        return cm.scan_source(cm.strip_source(text))

    def test_strip_keeps_lines_and_blanks_comments_strings_and_text_blocks(self):
        text = 'a // {\n/* } \n } */ b "{}" \'{\' """\n{\n""" c\n'
        stripped = cm.strip_source(text)
        self.assertEqual(len(text), len(stripped))
        self.assertEqual(text.count('\n'), stripped.count('\n'))
        self.assertNotIn('{', stripped)
        self.assertNotIn('}', stripped)
        self.assertIn('a', stripped)
        self.assertIn('c', stripped)

    def test_method_lengths_include_annotations_and_skip_lambdas_and_blocks(self):
        body = '\n'.join('        x++;' for _ in range(40))
        text = f'''class A {{
    @Override
    public void long1(int a,
                      int b) {{
{body}
    }}

    void withLambda() {{
        run(() -> {{
            if (a) {{ b(); }} else {{ c(); }}
        }});
        for (int i = 0; i < 3; i++) {{ d(); }}
        String s = "}}";
    }}

    int viaSwitch(int k) {{
        return switch (k) {{
            case 1 -> {{ yield 2; }}
            default -> 3;
        }};
    }}

    A() {{ }}
}}
'''
        methods, _ = self.scan(text)
        by_name = {name: (line, length) for line, name, length in methods}
        self.assertEqual({'long1', 'withLambda', 'viaSwitch', 'A'}, set(by_name))
        self.assertEqual((2, 44), by_name['long1'])
        self.assertEqual(7, by_name['withLambda'][1])
        self.assertEqual(6, by_name['viaSwitch'][1])
        self.assertEqual(1, by_name['A'][1])

    def test_anonymous_class_methods_and_enum_constant_bodies(self):
        text = '''enum E {
    A(1) {
        void f() { go(); }
    },
    B;

    E() { }
    E(int x) { }
    void g() { }
}
class C {
    Runnable r = new Runnable() {
        public void run() { }
    };
}
'''
        methods, _ = self.scan(text)
        self.assertEqual(['f', 'E', 'E', 'g', 'run'], [name for _, name, _ in methods])

    def test_static_fields_mutable_final_collections_and_ignored_cases(self):
        text = '''class A {
    static int counter;
    private static volatile Player last = null;
    static final int LIMIT = 3;
    static final java.util.Map<String, int[]> CACHE = new java.util.HashMap<>();
    static int[] table = { 1, 2 };
    static int after;
    static final Comparator<X> BY = (a, b) -> { return 0; };
    static void helper() { }
    static native int ping();
    int instance;
    static class Nested { static int deep; }
    @Foo(a = 1) static String tagged;
    interface Konst { static int K = 1; }
    static { setup(); }
}
'''
        _, statics = self.scan(text)
        found = [(line, name, kind) for line, name, kind in statics]
        self.assertEqual([(2, 'counter', 'mutable'), (3, 'last', 'mutable'), (5, 'CACHE', 'collection'),
                          (6, 'table', 'mutable'), (7, 'after', 'mutable'), (12, 'deep', 'mutable'),
                          (13, 'tagged', 'mutable')], found)

    def test_size_histogram_and_lists(self):
        with tempfile.TemporaryDirectory() as scratch:
            root = Path(scratch)
            source = root / 'src/main/java/p'
            source.mkdir(parents=True)
            (source / 'Small.java').write_text('class Small {\n}\n', encoding='utf-8')
            (source / 'Big.java').write_text('class Big {\n' + '    int x;\n' * 350 + '}\n', encoding='utf-8')
            result, lines = cm.analyze_sources(root / 'src/main/java', root)
        self.assertEqual({'src/main/java/p/Small.java': 2, 'src/main/java/p/Big.java': 352}, lines)
        self.assertEqual(1, result['histogram']['1-50'])
        self.assertEqual(1, result['histogram']['301-500'])
        self.assertEqual([('src/main/java/p/Big.java', 352)],
                         [(f['file'], f['lines']) for f in result['files_over_limit']])


class HistoryTests(unittest.TestCase):

    LOG = '\n'.join([
        '\x01c3', 'M\tsrc/b.java', 'R100\tsrc/old/a.java\tsrc/a.java',
        '\x01c2', 'M\tsrc/old/a.java', 'M\tsrc/b.java', 'D\tsrc/gone.java',
        '\x01c1', 'A\tsrc/old/a.java', 'A\tsrc/b.java', 'A\tsrc/gone.java', ''])

    def test_renames_are_followed_back_and_a_pure_move_is_not_a_touch(self):
        commits = cm.touched_files(cm.parse_log(self.LOG), {'src/a.java', 'src/b.java'})
        self.assertEqual([('c3', {'src/b.java'}), ('c2', {'src/a.java', 'src/b.java'}),
                          ('c1', {'src/a.java', 'src/b.java'})], commits)
        rows = cm.hotspots(commits, {'src/a.java': 10, 'src/b.java': 100})
        self.assertEqual([('src/b.java', 3, 300), ('src/a.java', 2, 20)],
                         [(r['file'], r['commits'], r['score']) for r in rows])

    def test_coupling_needs_five_co_changes_confidence_and_a_small_commit(self):
        together = [(f'c{i}', {'x', 'y'}) for i in range(5)]
        loose = [(f'l{i}', {'x', 'z'}) for i in range(5)] + [(f'm{i}', {'z'}) for i in range(10)]
        bulk = [('big', {f'f{i}' for i in range(60)} | {'x', 'y'})]
        rows, used = cm.change_coupling(together + loose + bulk, 40)
        self.assertEqual(20, used)
        self.assertEqual([('x', 'y', 5, 0.5, 1.0)],
                         [(r['a'], r['b'], r['together'], r['confidence_a_to_b'], r['confidence_b_to_a'])
                          for r in rows])
        # x-z: 5 co-changes, but x is 10 commits (0.5) and z 15 (0.33): below 0.6, so left out.
        self.assertNotIn(('x', 'z'), [(r['a'], r['b']) for r in rows])


class TestCountTests(unittest.TestCase):

    def test_counts_annotations_not_comments_or_longer_names(self):
        with tempfile.TemporaryDirectory() as scratch:
            folder = Path(scratch) / 'game'
            folder.mkdir()
            (folder / 'A.java').write_text(
                '// @Test\n@GameTestHolder\nclass A {\n@GameTest(x)\nvoid a() {}\n@GameTest\nvoid b() {}\n'
                '@GameTestGenerator\nvoid c() {}\n}\n', encoding='utf-8')
            (folder / 'B.java').write_text('class B {\n@Test void a() {}\n@ArchTest static X x;\n}\n',
                                           encoding='utf-8')
            (folder / 'C.java').write_text('class C {}\n', encoding='utf-8')
            result = cm.analyze_tests(Path(scratch))
        self.assertEqual((1, 2, 1, 1), (result['test'], result['game_test'], result['arch_test'],
                                        result['game_test_generator']))
        self.assertEqual((3, 2), (result['source_files'], result['source_files_with_tests']))


class FreshnessTests(unittest.TestCase):

    def test_reports_missing_and_stale_classes(self):
        with tempfile.TemporaryDirectory() as scratch:
            sources, classes = Path(scratch) / 's', Path(scratch) / 'c'
            for name in ('Fresh', 'Stale', 'Missing'):
                (sources / 'p').mkdir(parents=True, exist_ok=True)
                (sources / 'p' / f'{name}.java').write_text('class X {}', encoding='utf-8')
            (classes / 'p').mkdir(parents=True)
            for name in ('Fresh', 'Stale'):
                (classes / 'p' / f'{name}.class').write_bytes(b'x')
            old = time.time() - 100
            os.utime(classes / 'p' / 'Stale.class', (old, old))
            warnings = cm.freshness(sources, classes)
        self.assertEqual(2, len(warnings))
        self.assertTrue(any('no class file' in w and 'p/Missing.java' in w for w in warnings))
        self.assertTrue(any('newer than' in w and 'p/Stale.java' in w for w in warnings))
        self.assertEqual(1, len(cm.freshness(sources, Path(scratch) / 'none')))


class EndToEndTests(unittest.TestCase):

    def test_a_tiny_project_produces_both_reports_in_stable_order(self):
        with tempfile.TemporaryDirectory() as scratch:
            root = Path(scratch) / 'tiny'
            package = root / 'src/main/java/net/alex/mod/a'
            package.mkdir(parents=True)
            (package / 'Impl.java').write_text('package net.alex.mod.a;\nclass Impl {\n    static int n;\n}\n',
                                               encoding='utf-8')
            (root / 'src/test/pure').mkdir(parents=True)
            (root / 'src/test/pure/T.java').write_text('class T {\n@Test void t() {}\n}\n', encoding='utf-8')
            sample_classes(root / 'build/classes/java/main')
            git = ['git', '-c', 'user.name=t', '-c', 'user.email=t@example.com']
            subprocess.run(['git', 'init', '-q'], cwd=root, check=True)
            subprocess.run(['git', 'add', '.'], cwd=root, check=True)
            subprocess.run([*git, 'commit', '-q', '-m', 'init'], cwd=root, check=True)
            os.utime(package / 'Impl.java', (0, 0))
            with mock.patch.dict(cm.MODS, {'tiny': {'root_package': ROOT, 'hub_packages': ('registry',)}}):
                report = cm.analyze_project('tiny', root)
            json_path, markdown_path = cm.write_reports(report, Path(scratch) / 'out', False)
            data = json.loads(json_path.read_text(encoding='utf-8'))
            markdown = markdown_path.read_text(encoding='utf-8')
            self.assertEqual(json.dumps(data, indent=2, ensure_ascii=False) + '\n',
                             json_path.read_text(encoding='utf-8'))
        self.assertEqual(['meta', 'packages', 'cycles', 'edges', 'size', 'git', 'tests'], list(data))
        self.assertEqual(7, data['meta']['class_files'])
        self.assertEqual([('src/main/java/net/alex/mod/a/Impl.java', 3, 'n')],
                         [(s['file'], s['line'], s['field']) for s in data['size']['mutable_statics']])
        self.assertEqual(1, data['git']['hotspots'][0]['commits'])
        self.assertEqual(1, data['tests']['test'])
        self.assertEqual([], data['meta']['warnings'])
        self.assertIn('# tiny metrics', markdown)
        self.assertIn('## Packages at 2 level', markdown)


if __name__ == '__main__':
    unittest.main()
