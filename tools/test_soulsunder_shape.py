"""The Soulsunder Mountain shape tool: the span file, the placeholder, grounding, schematic reading and the shipped file."""
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


if __name__ == "__main__":
    unittest.main()
