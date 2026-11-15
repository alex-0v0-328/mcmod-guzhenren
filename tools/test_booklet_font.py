"""The booklet font cutter: glyph collection, BDF cells, the PNG sheet and the shipped fonts' freshness."""
from __future__ import annotations

import json
import tempfile
import unittest
import zlib
from pathlib import Path

import booklet_font as fonts


# Two 12x12 glyphs in a FONT_ASCENT 10 font: a full-width bar on the cell's top ink row, and a 2x2 dot at the
# top right, the way the zh_hans vertical comma sits.
BDF = """STARTFONT 2.1
FONTBOUNDINGBOX 12 12 0 -2
FONT_ASCENT 10
FONT_DESCENT 2
CHARS 2
STARTCHAR uni4E00
ENCODING 19968
DWIDTH 12 0
BBX 11 1 0 8
BITMAP
FFE0
ENDCHAR
STARTCHAR uniFE10
ENCODING 65040
DWIDTH 12 0
BBX 2 2 8 6
BITMAP
C0
C0
ENDCHAR
ENDFONT
"""
JAVA = """/** 书册 [书册] in a comment never counts. */
enum Sample {
    ONE("一　︐", "a\\"b"), // 不算
    TWO("︐一");
}
"""


def decode_png(data: bytes) -> tuple[int, int, set[tuple[int, int]]]:
    width = int.from_bytes(data[16:20], "big")
    height = int.from_bytes(data[20:24], "big")
    idat = data.index(b"IDAT")
    length = int.from_bytes(data[idat - 4:idat], "big")
    raw = zlib.decompress(data[idat + 4:idat + 4 + length])
    stride = width * 4 + 1
    ink = {(x, y) for y in range(height) for x in range(width)
           if raw[y * stride] == 0 and raw[y * stride + 1 + x * 4 + 3] == 0xFF}
    return width, height, ink


class BookletFontTest(unittest.TestCase):

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        (self.root / "font.bdf").write_text(BDF, encoding="ascii")

    def tearDown(self):
        self.temp.cleanup()

    def test_collects_non_ascii_glyphs_of_literals_only(self):
        self.assertEqual(["一", "︐"], fonts.collect(JAVA))

    def test_bdf_cells_keep_their_offsets_from_the_ascent(self):
        ascent, glyphs = fonts.load_bdf(self.root / "font.bdf", {"一", "︐"})
        self.assertEqual(10, ascent)
        self.assertEqual({(x, 1) for x in range(11)}, glyphs["一"].pixels(ascent))
        self.assertEqual({(8, 2), (9, 2), (8, 3), (9, 3)}, glyphs["︐"].pixels(ascent))

    def test_sheet_lays_cells_sixteen_a_row_with_padded_chars(self):
        chars = ["一"] * 16 + ["︐"]
        outputs = fonts.cut(chars, self.root / "font.bdf", "hans")
        font = json.loads(outputs[fonts.RESOURCES / "assets/guzhenren/font/booklet_hans.json"])
        provider = font["providers"][0]
        self.assertEqual(("bitmap", "guzhenren:font/booklet_hans.png", 12, 7),
                         (provider["type"], provider["file"], provider["height"], provider["ascent"]))
        self.assertEqual(["一" * 16, "︐" + "\u0000" * 15], provider["chars"])
        width, height, ink = decode_png(outputs[fonts.RESOURCES / "assets/guzhenren/textures/font/booklet_hans.png"])
        self.assertEqual((192, 24), (width, height))
        self.assertIn((15 * 12 + 10, 1), ink)
        self.assertEqual({(8, 14), (9, 14), (8, 15), (9, 15)}, {(x, y) for x, y in ink if y >= 12})

    def test_a_glyph_missing_from_the_bdf_names_itself(self):
        with self.assertRaisesRegex(ValueError, "lacks 二"):
            fonts.cut(["一", "二"], self.root / "font.bdf", "hans")

    def test_license_text_drops_trailing_whitespace_only(self):
        self.assertEqual(b"a\n  b\n\nc", fonts.license_text(b"a \n  b\t\n \nc  "))

    def test_check_reports_only_outputs_that_differ(self):
        (self.root / "same.txt").write_bytes(b"a")
        (self.root / "other.txt").write_bytes(b"a")
        outputs = {Path("same.txt"): b"a", Path("other.txt"): b"b", Path("missing.txt"): b"c"}
        self.assertEqual([Path("other.txt"), Path("missing.txt")], fonts.stale(self.root, outputs))

    @unittest.skipUnless(fonts.FONT_ROOT.is_dir(), "the Fusion Pixel release is not on this machine")
    def test_shipped_fonts_match_a_fresh_cut(self):
        outputs = fonts.build(fonts.env.REPOSITORY_ROOT, fonts.FONT_ROOT)
        self.assertEqual([], fonts.stale(fonts.env.REPOSITORY_ROOT, outputs),
                         "rerun python -B tools/booklet_font.py")


if __name__ == "__main__":
    unittest.main()
