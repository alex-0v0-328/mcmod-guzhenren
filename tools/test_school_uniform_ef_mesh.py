"""The School Uniform Epic Fight mesh tool: OBJ cuts, joint weights per region, and the three written meshes."""
from __future__ import annotations

import json
import os
import tempfile
import unittest

import school_uniform_ef_mesh as mesh

# Blockbench pixels (x, y up, -z front) of each exported cube: left limbs on -x, as the project has them.
CUBES = {
    'body_tunic': (-4.55, 4.55, 11.45, 24.55, -2.55, 2.55),
    'body_belt': (-4.65, 4.65, 13.6, 15.9, -2.65, 2.65),
    'body_hem_front': (-4.6, 4.6, 7.4, 12.6, -3.1, 0.1),
    'body_hem_back': (-4.61, 4.61, 7.39, 12.61, -0.11, 3.11),
    'left_sleeve_front': (-9.6, -4.4, 16.4, 24.6, -2.6, 1.6),
    'left_sleeve_back': (-9.7, -4.3, 15.3, 24.7, -1.2, 3.2),
    'left_inner_sleeve': (-8.35, -3.65, 16.65, 24.35, -2.35, 2.35),
    'left_sleeve_bandage': (-8.3, -3.7, 12.7, 17.3, -2.3, 2.3),
    'left_sleeve_front_slim': (-8.6, -4.4, 16.4, 24.6, -2.6, 1.6),
    'left_sleeve_back_slim': (-8.7, -4.3, 15.3, 24.7, -1.2, 3.2),
    'left_inner_sleeve_slim': (-7.35, -3.65, 16.65, 24.35, -2.35, 2.35),
    'left_sleeve_bandage_slim': (-7.3, -3.7, 12.7, 17.3, -2.3, 2.3),
    'right_sleeve_front': (4.4, 9.6, 16.4, 24.6, -2.6, 1.6),
    'right_sleeve_back': (4.3, 9.7, 15.3, 24.7, -1.2, 3.2),
    'right_inner_sleeve': (3.65, 8.35, 16.65, 24.35, -2.35, 2.35),
    'right_sleeve_bandage': (3.7, 8.3, 12.7, 17.3, -2.3, 2.3),
    'right_sleeve_front_slim': (4.4, 8.6, 16.4, 24.6, -2.6, 1.6),
    'right_sleeve_back_slim': (4.3, 8.7, 15.3, 24.7, -1.2, 3.2),
    'right_inner_sleeve_slim': (3.65, 7.35, 16.65, 24.35, -2.35, 2.35),
    'right_sleeve_bandage_slim': (3.7, 7.3, 12.7, 17.3, -2.3, 2.3),
    'left_leg_pants': (-4.4, 0.4, 5.6, 12.4, -2.4, 2.4),
    'left_leg_puttee': (-4.3, 0.3, 0.7, 6.3, -2.3, 2.3),
    'right_leg_pants': (-0.4, 4.4, 5.6, 12.4, -2.4, 2.4),
    'right_leg_puttee': (-0.3, 4.3, 0.7, 6.3, -2.3, 2.3),
    'left_boot_shoe': (-4.35, 0.35, -0.35, 2.35, -2.35, 2.35),
    'right_boot_shoe': (-0.35, 4.35, -0.35, 2.35, -2.35, 2.35),
}


def write_obj(path: str, cubes: dict[str, tuple]) -> None:
    """Each cube as six outward counter-clockwise quads in block units, the way Blockbench exports them."""
    lines, nv, nt, nn = [], 0, 0, 0
    for name, (x0, x1, y0, y1, z0, z1) in cubes.items():
        faces = [((0, 0, -1), [(x1, y0, z0), (x0, y0, z0), (x0, y1, z0), (x1, y1, z0)]),
                 ((0, 0, 1), [(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]),
                 ((1, 0, 0), [(x1, y0, z1), (x1, y0, z0), (x1, y1, z0), (x1, y1, z1)]),
                 ((-1, 0, 0), [(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)]),
                 ((0, 1, 0), [(x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0)]),
                 ((0, -1, 0), [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)])]
        lines.append(f'o {name}')
        for k, (normal, quad) in enumerate(faces):
            lines.append('vn %g %g %g' % normal)
            nn += 1
            corners = []
            for j, p in enumerate(quad):
                lines.append('v %.6f %.6f %.6f' % tuple(c / 16 for c in p))
                lines.append('vt %.6f %.6f' % ((k * 4 + j) / 32, j / 4))
                nv += 1
                nt += 1
                corners.append(f'{nv}/{nt}/{nn}')
            lines.append('f ' + ' '.join(corners))
    with open(path, 'w', encoding='utf-8') as file:
        file.write('\n'.join(lines) + '\n')


def vertices(piece: dict) -> list[tuple[tuple, dict[str, float]]]:
    """(position in pixels, joint shares) per vertex of a written mesh."""
    v = piece['vertices']
    p, counts, idx, w = (v[k]['array'] for k in ('positions', 'vcounts', 'vindices', 'weights'))
    out, cursor = [], 0
    for n, count in enumerate(counts):
        shares = {}
        for _ in range(count):
            shares[mesh.JOINTS[idx[cursor]]] = w[idx[cursor + 1]]
            cursor += 2
        out.append((tuple(c * 16 for c in p[3 * n:3 * n + 3]), shares))
    return out


class WrittenMeshes(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.dir = tempfile.TemporaryDirectory()
        obj = os.path.join(cls.dir.name, 'uniform.obj')
        write_obj(obj, CUBES)
        cls.files = mesh.write_meshes(obj, os.path.join(cls.dir.name, 'out'))
        cls.pieces = {item: json.load(open(path, encoding='utf-8')) for item, path in cls.files.items()}

    @classmethod
    def tearDownClass(cls):
        cls.dir.cleanup()

    def test_each_item_gets_its_parts_with_triangles_inside_the_tables(self):
        self.assertEqual(set(mesh.SLOTS), set(self.pieces))
        for item, piece in self.pieces.items():
            v = piece['vertices']
            self.assertEqual(set(mesh.SLOTS[item]), set(v['parts']), item)
            for part in v['parts'].values():
                corners = part['array']
                self.assertTrue(corners and len(corners) % 9 == 0, item)
                for i in range(0, len(corners), 3):
                    self.assertLess(corners[i], v['positions']['count'], item)
                    self.assertLess(corners[i + 1], v['uvs']['count'], item)
                    self.assertLess(corners[i + 2], v['normals']['count'], item)

    def test_every_vertex_shares_one_over_at_most_three_joints(self):
        for item, piece in self.pieces.items():
            for position, shares in vertices(piece):
                self.assertLessEqual(len(shares), 3, (item, position))
                self.assertAlmostEqual(1.0, sum(shares.values()), delta=1e-3, msg=(item, position))

    def test_cloth_a_pixel_off_the_middle_follows_only_its_side(self):
        for item, piece in self.pieces.items():
            for (x, _, _), shares in vertices(piece):
                wrong = '_R' if x < -1 else '_L' if x > 1 else None
                if wrong:
                    self.assertFalse([j for j in shares if j.endswith(wrong)], (item, x, shares))

    def test_the_hem_below_10_px_follows_the_thighs_alone(self):
        hem = [(p, s) for p, s in vertices(self.pieces['school_uniform_chestplate']) if p[2] < 10.0 - 1e-6]
        self.assertTrue(hem)
        for (x, _, _), shares in hem:
            if x <= -1:
                self.assertEqual({'Thigh_L': 1.0}, shares)
            elif x >= 1:
                self.assertEqual({'Thigh_R': 1.0}, shares)
            else:
                self.assertEqual({'Thigh_L', 'Thigh_R'}, set(shares))

    def test_a_missing_cube_stops_the_tool(self):
        obj = os.path.join(self.dir.name, 'partial.obj')
        write_obj(obj, {k: v for k, v in CUBES.items() if k != 'left_boot_shoe'})
        with self.assertRaisesRegex(ValueError, 'left_boot_shoe'):
            mesh.write_meshes(obj, os.path.join(self.dir.name, 'partial'))


class Weights(unittest.TestCase):

    def test_the_tunic_runs_from_torso_at_its_hem_to_chest_at_its_collar(self):
        self.assertEqual({'Torso': 1.0}, mesh.weights_for('body', (0, 0, mesh.BODY_BOTTOM), mesh.HEM_FULL_Z))
        self.assertEqual({'Chest': 1.0}, mesh.weights_for('body', (0, 0, mesh.BODY_TOP), mesh.HEM_FULL_Z))
        middle = mesh.weights_for('body', (0, 0, 18.0), mesh.HEM_FULL_Z)
        self.assertEqual({'Torso', 'Chest'}, set(middle))

    def test_hem_full_z_sets_where_the_torso_lets_go(self):
        self.assertEqual({'Thigh_L': 1.0}, mesh.weights_for('body', (-3, 0, 9.5), 10.0))
        self.assertIn('Torso', mesh.weights_for('body', (-3, 0, 9.5), 9.0))

    def test_the_seam_blends_both_thighs(self):
        self.assertEqual({'Thigh_L': 0.5, 'Thigh_R': 0.5}, mesh.weights_for('body', (0, 0, 8.0), 10.0))

    def test_sleeves_bend_at_the_elbow_ring_behind_the_arm(self):
        self.assertEqual({'Arm_L': 1.0}, mesh.weights_for('arm_L', (-7, 0, 20.0), 10.0))
        self.assertEqual({'Hand_L': 1.0}, mesh.weights_for('arm_L', (-7, 0, 15.0), 10.0))
        self.assertEqual({'Elbow_R': 1.0}, mesh.weights_for('arm_R', (7, -1, 18.0), 10.0))
        self.assertEqual({'Arm_R': 1.0}, mesh.weights_for('arm_R', (7, 1, 18.0), 10.0))

    def test_trousers_bend_at_the_knee_ring_in_front_of_the_leg(self):
        self.assertEqual({'Thigh_L': 1.0}, mesh.weights_for('leg_L', (-2, 0, 9.0), 10.0))
        self.assertEqual({'Leg_L': 1.0}, mesh.weights_for('leg_L', (-2, 0, 3.0), 10.0))
        self.assertEqual({'Knee_R': 1.0}, mesh.weights_for('leg_R', (2, 1, 6.0), 10.0))
        self.assertEqual({'Thigh_R': 1.0}, mesh.weights_for('leg_R', (2, -1, 6.0), 10.0))

    def test_shoes_follow_the_shin(self):
        self.assertEqual({'Leg_R': 1.0}, mesh.weights_for('boot_R', (2, 0, 1.0), 10.0))


class Geometry(unittest.TestCase):

    def test_blockbench_space_turns_into_z_up_with_the_front_on_plus_y(self):
        self.assertEqual((1, 3, 2), mesh.to_ef((1, 2, -3)))

    def test_a_cut_splits_a_square_into_halves_with_the_uv_carried_along(self):
        square = [((0, 0, 0), (0, 0)), ((2, 0, 0), (1, 0)), ((2, 0, 2), (1, 1)), ((0, 0, 2), (0, 1))]
        below, above = mesh.split(square, 2, 1.0)
        self.assertEqual(4, len(below))
        self.assertEqual(4, len(above))
        self.assertIn(((2, 0, 1.0), (1, 0.5)), below)
        self.assertEqual(([], square), mesh.split(square, 2, -1.0))

    def test_a_flipped_triangle_is_refused(self):
        pos = [((0, 0, 0), {}), ((1, 0, 0), {}), ((0, 1, 0), {})]
        self.assertTrue(mesh.area_ok(pos, 0, 1, 2, (0, 0, 1)))
        with self.assertRaises(ValueError):
            mesh.area_ok(pos, 0, 2, 1, (0, 0, 1))


if __name__ == '__main__':
    unittest.main()
