"""The Soulsunder Mountain shape tool: span file, placeholder, grounding, schematics, reshaping, palace, shipped."""
from __future__ import annotations

import gzip
import struct
import math
import unittest

import soulsunder_shape as shape


def nbt(value, kind: int | None = None) -> tuple[int, bytes]:
    """(tag id, payload) for a test value: int is TAG_Int unless kind says otherwise, bytes TAG_Byte_Array."""
    if isinstance(value, dict):
        body = b"".join(bytes((tag,)) + struct.pack(">H", len(name)) + name.encode() + payload
                        for name, (tag, payload) in ((n, nbt(*v) if isinstance(v, tuple) else nbt(v))
                                                     for n, v in value.items()))
        return 10, body + b"\x00"
    if isinstance(value, bytes):
        return 7, struct.pack(">i", len(value)) + value
    if isinstance(value, str):
        return 8, struct.pack(">H", len(value)) + value.encode()
    if kind == 2:
        return 2, struct.pack(">h", value)
    return 3, struct.pack(">i", value)


def nbt_file(root: dict) -> bytes:
    tag, payload = nbt(root)
    return gzip.compress(bytes((tag,)) + struct.pack(">H", 0) + payload)


def varint(value: int) -> bytes:
    out = bytearray()
    while True:
        byte = value & 0x7F
        value >>= 7
        out.append(byte | (0x80 if value else 0))
        if not value:
            return bytes(out)


# A 3x5x3 reference: a 3x2x3 stone base (y 0-1) with water at the (0, 0, 0) corner, and a lone stone at (1, 4, 1)
# two empty layers above it. Scaled to 160 x 128 x 160, every source cell is about 53 x 26 x 53 blocks.
SIZE = (3, 5, 3)


def fixture_cells():
    width, height, length = SIZE
    cells = []
    for y in range(height):
        for z in range(length):
            for x in range(width):
                if y <= 1:
                    cells.append("minecraft:water" if (x, y, z) == (0, 0, 0) else "minecraft:stone")
                elif (x, y, z) == (1, 4, 1):
                    cells.append("minecraft:stone")
                else:
                    cells.append("minecraft:air")
    return cells


class SpanFileTest(unittest.TestCase):

    def test_round_trip_keeps_every_span(self):
        spans = [[] for _ in range(shape.WIDTH * shape.WIDTH)]
        spans[0] = [(0, 3), (5, 5), (10, 127)]
        spans[-1] = [(0, 0)]
        width, depth, height, decoded = shape.decode(shape.encode(spans))
        self.assertEqual((width, depth, height), (160, 160, 128))
        self.assertEqual(decoded, spans)

    def test_column_spans_split_on_air(self):
        column = bytes([1, 1, 0, 1, 0, 0, 1])
        self.assertEqual(shape.column_spans(column), [(0, 1), (3, 3), (6, 6)])

    def test_decode_rejects_broken_files(self):
        good = shape.encode([[] for _ in range(shape.WIDTH * shape.WIDTH)])
        touching = bytearray(good)
        touching[11:12] = bytes((2, 0, 3, 4, 6))  # (0, 3) then (4, 6): adjacent spans must have been one
        for broken in (b"XXXX" + good[4:], good[:-1], good + b"\x00", bytes(touching)):
            with self.assertRaises(shape.ShapeError):
                shape.decode(broken)


class PlaceholderTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.grid, cls.notes = shape.placeholder(shape.DEFAULT_SEED)
        cls.spans = shape.grid_spans(cls.grid)

    def test_same_seed_same_bytes_other_seed_other_shape(self):
        again, _ = shape.placeholder(shape.DEFAULT_SEED)
        other, _ = shape.placeholder(shape.DEFAULT_SEED + 1)
        data = shape.encode(self.spans)
        self.assertEqual(shape.encode(shape.grid_spans(again)), data)
        self.assertNotEqual(shape.encode(shape.grid_spans(other)), data)

    def test_peak_with_four_to_six_spikes_inside_the_disc(self):
        facts = shape.describe(self.spans)
        self.assertIn(len(self.notes["spikes"]), range(4, 7))
        self.assertEqual(facts["outside_disc"], 0)
        self.assertGreaterEqual(facts["max_height"], 120)
        self.assertLessEqual(facts["max_height"], shape.HEIGHT)
        self.assertEqual(facts["grounded_columns"], facts["columns"])
        centre = self.spans[shape.CENTRE * shape.WIDTH + shape.CENTRE]
        self.assertGreater(centre[-1][1], shape.PEAK - 10)

    def test_nothing_floats(self):
        self.assertEqual(shape.keep_grounded(self.grid), 0)


class StyleTest(unittest.TestCase):

    def test_every_style_is_grounded_inside_the_disc_with_its_spikes(self):
        for style in shape.STYLES[1:]:
            with self.subTest(style=style):
                grid, notes = shape.placeholder(3, style)
                facts = shape.describe(shape.grid_spans(grid))
                self.assertEqual(len(notes["spikes"]), shape.SPIKES[style])
                self.assertEqual(facts["outside_disc"], 0)
                self.assertEqual(notes["floating_cleared"], 0)
                self.assertGreater(facts["max_height"], 100)


class GroundingTest(unittest.TestCase):

    def test_overhang_stays_floater_goes(self):
        grid = shape.Grid.empty()
        grid.column(10, 10)[0:20] = b"\x01" * 20
        grid.column(11, 10)[15:18] = b"\x01" * 3  # an overhang joined by a face to the pillar
        grid.column(40, 40)[30:32] = b"\x01" * 2  # a floater
        grid.column(10, 10)[25] = 1  # floats above its own column's pillar
        self.assertEqual(shape.keep_grounded(grid), 3)
        self.assertEqual(shape.column_spans(grid.column(11, 10)), [(15, 17)])
        self.assertEqual(shape.column_spans(grid.column(10, 10)), [(0, 19)])
        self.assertFalse(any(grid.column(40, 40)))


class SchematicTest(unittest.TestCase):

    def sponge_v2(self) -> bytes:
        width, height, length = SIZE
        names = sorted(set(fixture_cells()))
        palette = {name if name != "minecraft:stone" else "stone[variant=x]": (index, 3)
                   for index, name in enumerate(names)}
        data = b"".join(varint(names.index(cell)) for cell in fixture_cells())
        return nbt_file({"Version": 2, "Width": (width, 2), "Height": (height, 2), "Length": (length, 2),
                         "Palette": palette, "BlockData": data})

    def test_sponge_v2_v3_and_legacy_read_alike(self):
        width, height, length = SIZE
        names = sorted(set(fixture_cells()))
        data = b"".join(varint(names.index(cell)) for cell in fixture_cells())
        v3 = nbt_file({"Schematic": {"Version": 3, "Width": (width, 2), "Height": (height, 2),
                                     "Length": (length, 2), "Blocks": {
                                         "Palette": {name: index for index, name in enumerate(names)}, "Data": data}}})
        legacy_ids = {"minecraft:air": 0, "minecraft:stone": 1, "minecraft:water": 9}
        legacy = nbt_file({"Width": (width, 2), "Height": (height, 2), "Length": (length, 2), "Materials": "Alpha",
                           "Blocks": bytes(legacy_ids[cell] for cell in fixture_cells())})
        expected = bytearray(cell == "minecraft:stone" for cell in fixture_cells())
        for raw in (self.sponge_v2(), v3, legacy):
            schematic = shape.read_schematic(raw)
            self.assertEqual((schematic.width, schematic.height, schematic.length), SIZE)
            self.assertEqual(schematic.solid, expected)

    def test_convert_fills_the_disc_and_drops_the_floater(self):
        schematic = shape.read_schematic(self.sponge_v2())
        grid, notes = shape.convert(schematic, seed=1, spikes=0)
        facts = shape.describe(shape.grid_spans(grid))
        self.assertEqual(notes["reference"]["solid_box"], (3, 5, 3))
        self.assertEqual(facts["outside_disc"], 0)
        self.assertEqual(facts["max_spans"], 1)
        # The base fills the lower 2/5 of the height; the water corner stays hollow; the floater is gone.
        self.assertEqual(facts["max_height"], round(2 / 5 * shape.HEIGHT))
        self.assertGreater(notes["floating_cleared"], 0)
        self.assertEqual(shape.column_spans(grid.column(30, 30)), [(26, 50)])  # stone over the water corner
        self.assertTrue(all(grid.column(shape.CENTRE, shape.CENTRE)[:51]))
        self.assertFalse(any(grid.column(shape.CENTRE, shape.CENTRE)[52:]))

    def test_spikes_rise_from_the_converted_surface(self):
        grid, notes = shape.convert(shape.read_schematic(self.sponge_v2()), seed=7, spikes=3)
        self.assertEqual(len(notes["spikes"]), 3)
        self.assertGreater(shape.describe(shape.grid_spans(grid))["max_height"], round(2 / 5 * shape.HEIGHT))
        self.assertEqual(shape.keep_grounded(grid), 0)


class NaturalFootTest(unittest.TestCase):

    def test_foot_keeps_its_outline_without_fragments_or_pinholes(self):
        grid = shape.Grid.empty()
        for z in range(50, 111):
            for x in range(50, 111):
                height = max(1, 100 - 3 * int(math.hypot(x - 80, z - 80)))
                grid.column(x, z)[0:height] = b"\x01" * height
        grid.column(80, 80)[50:60] = bytes(10)  # a hollow inside the peak
        for z in range(79, 82):
            for x in range(60, 63):
                grid.column(x, z)[:] = bytes(shape.HEIGHT)  # a 3x3 gap in the foot
        grid.column(20, 80)[0:3] = b"\x01" * 3  # a dune fragment out past the foot
        grid.column(100, 100)[0:40] = b"\x01" * 40  # a lone pillar off its 3x3 median
        notes = shape.natural_foot(grid)
        self.assertEqual(notes["foot_fragments_dropped"], 1)
        self.assertGreaterEqual(notes["foot_holes_filled"], 1)
        self.assertGreaterEqual(notes["foot_outliers"], 1)
        self.assertEqual(grid.top(20, 80), -1)
        self.assertTrue(all(grid.top(x, z) > 0 for z in range(79, 82) for x in range(60, 63)))
        self.assertLess(grid.top(100, 100), 39)
        self.assertEqual(grid.top(80, 80), 99)
        self.assertEqual(grid.top(30, 80), -1)  # outside the reference's foot stays empty: no disc-wide pad
        for column in grid.columns:
            self.assertLessEqual(len(shape.column_spans(column)), 1)
        reach = shape.foot_reach(shape.column_heights(grid))
        self.assertAlmostEqual(reach[1], 42.4, delta=1.5)


def cone_heights() -> list[int]:
    """A gentle cone, 109 blocks at the centre and about 1.7 blocks lower per block outward, gone at 64 blocks."""
    heights = []
    for z in range(shape.WIDTH):
        for x in range(shape.WIDTH):
            distance = math.hypot(x + 0.5 - shape.CENTRE, z + 0.5 - shape.CENTRE)
            heights.append(max(0, round(110 * (1 - distance / 64))))
    return heights


def height_grid(heights: list[int]) -> shape.Grid:
    grid = shape.Grid.empty()
    for column, height in zip(grid.columns, heights):
        column[0:height] = b"\x01" * height
    return grid


class ReshapeTest(unittest.TestCase):

    def assert_height_map(self, grid: shape.Grid):
        for index, column in enumerate(grid.columns):
            spans = shape.column_spans(column)
            self.assertLessEqual(len(spans), 1)
            if spans:
                self.assertEqual(spans[0][0], 0)
                self.assertTrue(shape.in_disc(index % shape.WIDTH, index // shape.WIDTH))

    def test_peak_scales_the_highest_column_to_n(self):
        before = cone_heights()
        for peak in (80, 20):
            with self.subTest(peak=peak):
                grid = height_grid(before)
                shape.reshape(grid, peak=peak)
                after = shape.column_heights(grid)
                self.assertEqual(max(after), peak)
                self.assertEqual([h > 0 for h in after], [h > 0 for h in before])  # low foot columns keep one block
                self.assert_height_map(grid)

    def test_blunt_keeps_the_knee_and_stays_under_the_cap(self):
        before = cone_heights()
        grid = height_grid(before)
        shape.reshape(grid, blunt=(70, 96))
        after = shape.column_heights(grid)
        for old, new in zip(before, after):
            if old <= 70:
                self.assertEqual(new, old)
            else:
                self.assertGreater(new, 70)
        self.assertEqual(max(after), round(70 + 26 * (1 - math.exp(-(max(before) - 70) / 26))))
        self.assertLessEqual(max(after), 96)
        self.assert_height_map(grid)

    def test_lift_raises_the_flanks_and_keeps_the_foot_and_the_summit(self):
        before = cone_heights()
        top, lift = max(before), 16
        grid = height_grid(before)
        notes = shape.reshape(grid, lift=lift)
        after = shape.column_heights(grid)
        self.assertEqual(max(after), top)
        self.assertEqual([h > 0 for h in after], [h > 0 for h in before])
        self.assertTrue(all(0 <= new - old <= lift for old, new in zip(before, after)))
        self.assertEqual({new - old for old, new in zip(before, after) if old in (top // 2, top // 2 + 1)}, {lift})
        self.assertGreater(notes["lifted_columns"], 0)
        self.assert_height_map(grid)

    def test_lift_steepens_a_step_by_at_most_one_plus_four_lift_over_the_top(self):
        before = cone_heights()
        top, lift = max(before), 16
        grid = height_grid(before)
        shape.reshape(grid, lift=lift)
        after = shape.column_heights(grid)
        factor = 1 + 4 * lift / top
        width = shape.WIDTH
        for index in range(width * width):
            x, z = index % width, index // width
            for other in ((index + 1) if x + 1 < width else None, (index + width) if z + 1 < width else None):
                if other is not None:
                    old, new = abs(before[index] - before[other]), abs(after[index] - after[other])
                    self.assertLessEqual(new, factor * old + 1)  # + 1 for the two roundings

    def test_lift_must_stay_under_a_quarter_of_the_top(self):
        before = cone_heights()
        with self.assertRaises(shape.ShapeError):
            shape.reshape(height_grid(before), lift=max(before) / 4)

    def test_reshaping_needs_the_natural_foot(self):
        schematic = shape.Schematic(1, 1, 1, bytearray([1]))
        for option in ({"peak": 100}, {"lift": 16.0}, {"blunt": (80, 112)}):
            with self.subTest(option=option):
                with self.assertRaises(shape.ShapeError):
                    shape.convert(schematic, seed=1, spikes=0, **option)


def cone_schematic() -> shape.Schematic:
    """A 41-cell cone, 40 cells high and 20 in radius; converted with keep_aspect a cell is 3.2 blocks, so the foot
    ends about 66 blocks out, clear of the disc edge."""
    size = 41
    solid = bytearray(size ** 3)
    for y in range(size):
        for z in range(size):
            for x in range(size):
                solid[(y * size + z) * size + x] = y < 40 * (1 - math.hypot(x - 20, z - 20) / 20)
    return shape.Schematic(size, size, size, solid)


class ConvertSpikeTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        schematic = cone_schematic()
        options = {"keep_aspect": True, "tidy_foot": True, "peak": 96, "spike_radius": (2.0, 3.0),
                   "spike_length": (12.0, 20.0)}
        cls.summit, cls.summit_notes = shape.convert(schematic, seed=5, spikes=5, **options)
        cls.flank, cls.flank_notes = shape.convert(schematic, seed=5, spikes=5, summit_spike=False, **options)

    def test_flank_spikes_keep_to_the_given_ranges(self):
        spikes = self.flank_notes["spikes"]
        self.assertEqual(len(spikes), 5)
        for spike in spikes:
            self.assertGreaterEqual(spike["radius"], 2.0)
            self.assertLessEqual(spike["radius"], 3.0)
            self.assertGreaterEqual(spike["length"], 12.0)
            self.assertLessEqual(spike["length"], 20.0)

    def test_no_summit_spike_means_nothing_reaches_the_top_layer(self):
        room = shape.HEIGHT - shape.SUMMIT_ROOM
        summit_top = self.summit_notes["reshape"]["max_height_after"] - 1
        self.assertEqual(len(self.summit_notes["spikes"]), 5)  # the summit crystal takes one of the five
        self.assertEqual(self.summit_notes["spikes"][0]["surface"], summit_top)
        self.assertGreater(shape.describe(shape.grid_spans(self.summit))["max_height"], room)
        self.assertTrue(all(spike["surface"] < summit_top for spike in self.flank_notes["spikes"]))
        self.assertLessEqual(shape.describe(shape.grid_spans(self.flank))["max_height"], room)

    def test_reshaped_mountains_have_nothing_floating(self):
        for grid in (self.summit, self.flank):
            facts = shape.describe(shape.grid_spans(grid))
            self.assertEqual(facts["grounded_columns"], facts["columns"])
            self.assertEqual(facts["outside_disc"], 0)
            self.assertEqual(shape.keep_grounded(grid), 0)


class ShippedFileTest(unittest.TestCase):

    def test_shipped_shape_decodes_grounded_inside_the_disc(self):
        if not shape.DEFAULT_OUT.exists():
            self.skipTest(f"{shape.DEFAULT_OUT} is not checked out")
        width, depth, height, spans = shape.decode(shape.DEFAULT_OUT.read_bytes())
        self.assertEqual((width, depth, height), (shape.WIDTH, shape.WIDTH, shape.HEIGHT))
        facts = shape.describe(spans)
        self.assertEqual(facts["outside_disc"], 0)
        grid = shape.Grid.empty()
        for index, column in enumerate(spans):
            for y0, y1 in column:
                grid.columns[index][y0:y1 + 1] = b"\x01" * (y1 - y0 + 1)
        self.assertEqual(shape.keep_grounded(grid), 0)


class PalaceTest(unittest.TestCase):

    @staticmethod
    def block(height: int) -> "shape.Grid":
        """Every column inside the disc solid up to height."""
        grid = shape.Grid.empty()
        for z in range(shape.WIDTH):
            for x in range(shape.WIDTH):
                if shape.in_disc(x, z):
                    grid.column(x, z)[:height] = b"\x01" * height
        return grid

    def test_trail_rises_at_most_one_block_between_neighbours(self):
        cells = shape.trail_cells()
        for index, height in cells.items():
            x, z = index % shape.WIDTH, index // shape.WIDTH
            for neighbour in (index + 1, index + shape.WIDTH):
                if neighbour in cells:
                    self.assertLessEqual(abs(cells[neighbour] - height), 1, f"step at ({x}, {z})")
        self.assertEqual(cells[(shape.CENTRE + shape.SUMMIT[2]) * shape.WIDTH + shape.CENTRE + shape.SUMMIT[0]],
                         shape.SUMMIT[1])

    @staticmethod
    def roofed_top() -> int:
        return max(y for _, y, z in shape.palace_air() if z <= shape.ROOFED_Z)

    def test_cave_opens_round_the_anchor_under_a_thick_roof(self):
        grid = self.block(self.roofed_top() + shape.ROOF + 1)
        notes = shape.cut_palace(grid)
        self.assertEqual(notes["roof"], shape.ROOF)
        x, y, z = shape.ANCHOR
        column = grid.column(shape.CENTRE + x, shape.CENTRE + z)
        self.assertEqual(column[y - 1], 1)
        self.assertEqual(bytes(column[y:y + 3]), bytes(3))
        self.assertEqual(shape.keep_grounded(grid), 0, "the cave leaves stone floating")

    def test_cave_is_eroded_beyond_its_footprint(self):
        air = shape.palace_air()
        xs = [x for x, _, _ in air]
        ys = [y for _, y, _ in air]
        self.assertLess(min(xs), shape.PALACE_X[0])
        self.assertGreater(max(ys), shape.PALACE_FLOOR + shape.PALACE_HEIGHT)
        self.assertEqual(min(ys), shape.PALACE_FLOOR)
        self.assertEqual(air, shape.palace_air(), "the cave is not fixed by its constants")

    def test_thin_roof_is_refused(self):
        grid = self.block(self.roofed_top() + shape.ROOF)
        with self.assertRaises(shape.ShapeError):
            shape.cut_palace(grid)

    def test_mouth_widens_and_rises_towards_the_flank(self):
        air = shape.mouth_air()

        def width(dz):
            xs = [x for x, _, z in air if z == dz]
            return max(xs) - min(xs) + 1

        def height(dz):
            return max(y for _, y, z in air if z == dz) - shape.PALACE_FLOOR + 1

        self.assertLess(width(shape.MOUTH_START), width(shape.MOUTH_END))
        self.assertLess(height(shape.MOUTH_START), height(shape.MOUTH_END))
        self.assertGreaterEqual(width(shape.MOUTH_END), 2 * shape.MOUTH_HALF_WIDTH[1] - 2)

    def test_mouth_keeps_the_trail_tread(self):
        grid = self.block(self.roofed_top() + shape.ROOF + 1)
        dx, dz = round(shape.MOUTH_X), shape.ROOFED_Z + 4
        index = (shape.CENTRE + dz) * shape.WIDTH + shape.CENTRE + dx
        tread = shape.PALACE_FLOOR + 3
        shape.cut_palace(grid, {index: tread})
        column = grid.column(shape.CENTRE + dx, shape.CENTRE + dz)
        self.assertEqual(bytes(column[shape.PALACE_FLOOR:tread]), b"" * 3, "the tread is sunk")
        self.assertEqual(column[tread], 0, "the mouth is closed over the tread")
        beside = grid.column(shape.CENTRE + dx + 1, shape.CENTRE + dz)
        self.assertEqual(beside[shape.PALACE_FLOOR], 0, "the mouth floor is not open beside the trail")

    def test_walk_climbs_one_block_steps_only(self):
        grid = shape.Grid.empty()
        for step in range(4):
            grid.column(shape.CENTRE + step, shape.CENTRE)[:step + 1] = b"\x01" * (step + 1)
        self.assertIn((shape.CENTRE + 3, shape.CENTRE, 4), shape.walk(grid))
        grid.column(shape.CENTRE + 3, shape.CENTRE)[:6] = b"\x01" * 6
        self.assertNotIn((shape.CENTRE + 3, shape.CENTRE, 6), shape.walk(grid))

    def test_shipped_shape_keeps_its_palace(self):
        if not shape.DEFAULT_OUT.exists():
            self.skipTest(f"{shape.DEFAULT_OUT} is not checked out")
        data = shape.DEFAULT_OUT.read_bytes()
        grid = shape.grid_from_spans(shape.decode(data)[3])
        shape.palace(grid)
        self.assertEqual(shape.encode(shape.grid_spans(grid)), data, "palace is not idempotent on the shipped file")


if __name__ == "__main__":
    unittest.main()
