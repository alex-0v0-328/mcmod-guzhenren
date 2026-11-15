import json
import tempfile
import unittest
from contextlib import redirect_stderr
from io import StringIO
from pathlib import Path

import datapack_audit as audit


def write(root, rel, content):
    path = root / 'src' / 'generated' / 'resources' / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    if not isinstance(content, str):
        content = json.dumps(content, indent=2)
    path.write_text(content, encoding='utf-8')


def build_fixture(root):
    dimension = {'type': 'guworld:alpha', 'generator': {'type': 'minecraft:noise', 'settings': 'guworld:alpha',
                 'biome_source': {'type': 'minecraft:multi_noise', 'biomes': [{'biome': 'guworld:plain', 'parameters': {}}]}}}
    write(root, 'data/guworld/dimension/alpha.json', dimension)
    write(root, 'data/guworld/dimension_type/alpha.json', {'ambient_light': 0.0})
    for region, scale in (('alpha', 0.5), ('beta', 0.5), ('gamma', 0.7)):
        write(root, f'data/guworld/worldgen/noise/terrain/{region}/n.json', {'firstOctave': -3, 'amplitudes': [1.0]})
        write(root, f'data/guworld/worldgen/density_function/terrain/{region}/final.json', {
            'type': 'minecraft:add', 'argument1': f'guworld:terrain/{region}/scale', 'argument2': {
                'type': 'minecraft:shift_a', 'argument': f'guworld:terrain/{region}/n'}})
        write(root, f'data/guworld/worldgen/density_function/terrain/{region}/scale.json', scale)
    write(root, 'data/guworld/worldgen/density_function/terrain/alpha/orphan.json', 1.0)
    write(root, 'data/guworld/worldgen/noise_settings/alpha.json', {
        'noise_router': {'final_density': 'guworld:terrain/alpha/final', 'depth': {'type': 'minecraft:noise', 'noise': 'guworld:terrain/alpha/n'}},
        'surface_rule': {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:biome', 'biome_is': ['guworld:plain']}}})
    write(root, 'data/guworld/worldgen/biome/plain.json', {'features': [[], ['guworld:tree']], 'carvers': {}})
    write(root, 'data/guworld/worldgen/placed_feature/tree.json', {'feature': 'guworld:tree', 'placement': []})
    write(root, 'data/guworld/worldgen/placed_feature/unused.json', {'feature': 'guworld:lost', 'placement': []})
    write(root, 'data/guworld/worldgen/configured_feature/tree.json', {
        'type': 'minecraft:vegetation_patch', 'config': {'vegetation_feature': {'feature': 'guworld:grass', 'placement': []}}})
    write(root, 'data/guworld/worldgen/configured_feature/grass.json', {'type': 'minecraft:no_op', 'config': {}})
    write(root, 'data/guworld/worldgen/configured_feature/unused.json', {
        'type': 'minecraft:random_patch', 'config': {'feature': 'guworld:missing_placed', 'x': 'guworld:other'}})
    write(root, 'data/guworld/tags/block/a.json', {'values': ['minecraft:stone']})
    write(root, 'data/guworld/tags/block/b.json', {'values': ['minecraft:stone']})
    write(root, 'assets/guworld/lang/en_us.json', {'a': 'A', 'b': 'B'})
    write(root, 'assets/guworld/lang/zh_cn.json', {'a': 'x', 'c': 'C'})
    write(root, 'assets/guworld/models/item/thing.json', {
        'parent': 'minecraft:item/generated', 'textures': {'layer0': 'guworld:item/thing'}})
    write(root, 'assets/guworld/particles/ring.json', {'textures': ['guworld:ring']})
    write(root, 'assets/guworld/textures/particle/ring.png', 'png')
    write(root, 'broken.json', '{')


class DatapackAuditTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.directory = tempfile.TemporaryDirectory()
        base = Path(cls.directory.name)
        cls.root, cls.out = base / 'guworld', base / 'out'
        build_fixture(cls.root)
        cls.report = audit.run({'guworld': cls.root}, ['guworld'], cls.out)['guworld']

    @classmethod
    def tearDownClass(cls):
        cls.directory.cleanup()

    def test_inventory_counts_json_per_kind(self):
        inventory = self.report['inventory']
        self.assertEqual(26, inventory['files'])
        self.assertEqual(3, inventory['by_kind']['data/guworld/worldgen/noise']['files'])
        self.assertEqual(2, inventory['by_kind']['data/guworld/tags/block']['files'])
        self.assertEqual(1, inventory['non_json_skipped']['src/generated/resources'])

    def test_exact_duplicates_group_identical_canonical_json(self):
        listed = {tuple(g['files']) for g in self.report['exact_duplicates']['list']}
        self.assertIn(('data/guworld/tags/block/a.json', 'data/guworld/tags/block/b.json'), listed)
        noises = [g for g in self.report['exact_duplicates']['list'] if g['count'] == 3 and 'noise' in g['files'][0]]
        self.assertEqual(1, len(noises))

    def test_region_copies_collapse_modulo_region_and_flag_real_differences(self):
        copies = self.report['region_copies']
        self.assertEqual(['alpha', 'beta', 'gamma'], copies['regions'])
        noise = copies['by_kind']['guworld/worldgen/noise']
        self.assertEqual((3, 1), (noise['files'], noise['strict_bodies']))
        functions = copies['by_kind']['guworld/worldgen/density_function']
        self.assertEqual(3 * 2 + 1, functions['files'])
        self.assertEqual(3 + 1, functions['strict_bodies'])
        self.assertEqual(1, functions['differing_families'])
        differing = [d for d in copies['differing'] if d['family'].endswith('scale')]
        self.assertEqual([['alpha', 'beta'], ['gamma']], differing[0]['groups'])
        self.assertEqual('0.5', differing[0]['diffs'][0]['leaves'][0]['a'])

    def test_references_use_the_field_to_pick_the_kind(self):
        dangling = {(d['file'].rsplit('/', 1)[1], d['ref']): d['expected'] for d in self.report['references']['dangling']}
        self.assertEqual('worldgen/placed_feature', dangling[('unused.json', 'guworld:missing_placed')])
        self.assertEqual('worldgen/configured_feature', dangling[('unused.json', 'guworld:lost')])
        self.assertNotIn(('final.json', 'guworld:terrain/alpha/n'), dangling)
        self.assertNotIn(('tree.json', 'guworld:grass'), dangling)
        self.assertIn('data/guworld/worldgen/configured_feature: x', self.report['references']['unclassified_mod_references'])

    def test_asset_references_resolve_models_and_particle_textures(self):
        files = {d['file'] for d in self.report['references']['dangling']}
        self.assertEqual({'assets/guworld/models/item/thing.json'}, {f for f in files if f.startswith('assets/')})

    def test_reachability_walks_from_dimension_and_noise_settings(self):
        kinds = self.report['reachability']['by_kind']
        lists = self.report['reachability']['lists']
        orphans = {x['id'] for x in lists['worldgen/density_function']['unreachable']}
        self.assertEqual({'guworld:terrain/beta/final', 'guworld:terrain/beta/scale', 'guworld:terrain/gamma/final',
                          'guworld:terrain/gamma/scale', 'guworld:terrain/alpha/orphan'}, orphans)
        self.assertEqual(3, kinds['worldgen/density_function']['never_referenced'])
        self.assertEqual(['guworld:unused'], [x['id'] for x in lists['worldgen/placed_feature']['unreachable']])
        self.assertEqual([], lists['worldgen/biome']['unreachable'])

    def test_lang_gaps_are_reported_both_ways(self):
        lang = self.report['lang']['guworld']
        self.assertEqual(['b'], lang['missing_in_zh_cn'])
        self.assertEqual(['c'], lang['only_in_zh_cn'])

    def test_parse_errors_are_warnings_and_files_are_written(self):
        self.assertEqual(['broken.json'], [w['file'] for w in self.report['warnings']['parse_errors']])
        self.assertTrue((self.out / 'guworld-datapack.json').is_file())
        self.assertIn('## Region copies', (self.out / 'guworld-datapack.md').read_text(encoding='utf-8'))

    def test_main_requires_out_and_prints_one_summary_line(self):
        with self.assertRaises(SystemExit), redirect_stderr(StringIO()):
            audit.main([])
        self.assertEqual('guworld', audit.classify_path('data/guworld/worldgen/noise/a.json'.split('/'))[1])
        self.assertEqual(('data', 'guworld', 'tags/worldgen/biome', 'guworld:a/b'),
                         audit.classify_path('data/guworld/tags/worldgen/biome/a/b.json'.split('/')))
        self.assertIn('guworld:', audit.summary_line(self.report))


if __name__ == '__main__':
    unittest.main()
