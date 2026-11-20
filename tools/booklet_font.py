"""Bitmap fonts for the booklets [书册], cut from the Fusion Pixel 12px monospaced BDF fonts.

    python -B tools/booklet_font.py            cut every glyph the booklets use into the fonts, copy the licenses
    python -B tools/booklet_font.py --check    fail when the shipped fonts or licenses differ from a fresh cut

The fonts take every non-ASCII, non-space code point from the string literals of Booklet.java and Chronicle.java,
plus every Han glyph of the chronicle texts under assets/guzhenren/booklets/ (their horizontal punctuation never
shows: Chronicle turns it into vertical forms named in its literals). Each glyph is cut from the zh_hans and the
zh_hant 12px BDF into a 12x12 cell, one glyph pixel to one GUI pixel, white ink on transparency, giving
assets/guzhenren/font/booklet_<script>.json over textures/font/booklet_<script>.png; both sheets take the vertical
stops and commas in CENTERED from zh_hant, centred in the cell as Taiwanese vertical books set them (zh_hans hangs
them top right). Every sheet has 16 cells a row and ascent 7, so a glyph's cell top sits on the y it is drawn at; each
font holds both scripts' glyphs, and the screen picks hant under zh_tw and zh_hk, hans everywhere else. The SIL OFL
texts of the release and the fonts it merges go to licenses/fusion-pixel-font/. BookletFontTest fails when a booklet
uses a glyph the shipped fonts lack; this script is the fix.
"""
from __future__ import annotations

import argparse
import json
import re
import struct
import sys
import zlib
from dataclasses import dataclass
from pathlib import Path

import project_runtime as env


FONT_ROOT = env.REPOSITORY_ROOT.parent / "assets" / "fonts" / "fusion-pixel-font-2026.10.25"
BOOKLET_SOURCE = Path("src/main/java/net/alex/guzhenren/item/Booklet.java")
CHRONICLE_SOURCE = Path("src/main/java/net/alex/guzhenren/item/Chronicle.java")
RESOURCES = Path("src/main/resources")
CHRONICLE_TEXTS = RESOURCES / "assets/guzhenren/booklets"
SCRIPTS = ("hans", "hant")
LICENSES = ("OFL.txt", "LICENSES/ark-pixel/OFL.txt", "LICENSES/cubic-11/OFL.txt", "LICENSES/galmuri/LICENSE.txt")
CELL = 12
CENTERED = "︐︑︒︓︔︕︖"
PER_ROW = 16
ASCENT = 7
LITERAL = re.compile(r'"((?:[^"\\\n]|\\.)*)"')
BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.DOTALL)
LINE_COMMENT = re.compile(r"//[^\n]*")


@dataclass(frozen=True)
class Glyph:
    width: int
    height: int
    x_offset: int
    y_offset: int
    rows: tuple[int, ...]

    def pixels(self, ascent: int) -> set[tuple[int, int]]:
        top = ascent - (self.y_offset + self.height)
        hex_width = (self.width + 7) // 8 * 8
        return {(self.x_offset + x, top + y) for y, row in enumerate(self.rows) for x in range(self.width)
                if row >> (hex_width - 1 - x) & 1}


def collect(java: str) -> list[str]:
    code = LINE_COMMENT.sub("", BLOCK_COMMENT.sub("", java))
    glyphs = {char for literal in LITERAL.findall(code) for char in literal if ord(char) > 0x7F and not char.isspace()}
    return sorted(glyphs)


def is_han(char: str) -> bool:
    return "\u3400" <= char <= "\u9fff" or "\uf900" <= char <= "\ufaff" or char >= "\U00020000"


def collect_texts(root: Path) -> list[str]:
    return sorted({char for path in sorted(root.rglob("*.txt")) for char in path.read_text(encoding="utf-8")
                   if is_han(char)})


def load_bdf(path: Path, wanted: set[str]) -> tuple[int, dict[str, Glyph]]:
    ascent = None
    glyphs: dict[str, Glyph] = {}
    with path.open("r", encoding="ascii") as handle:
        lines = iter(handle)
        for line in lines:
            if line.startswith("FONT_ASCENT "):
                ascent = int(line.split()[1])
            elif line.startswith("STARTCHAR"):
                encoding = bbx = None
                for line in lines:
                    if line.startswith("ENCODING "):
                        encoding = int(line.split()[1])
                    elif line.startswith("BBX "):
                        bbx = tuple(int(value) for value in line.split()[1:])
                    elif line.startswith("BITMAP"):
                        break
                rows = tuple(int(row, 16) for row in iter(lambda: next(lines).strip(), "ENDCHAR"))
                char = chr(encoding)
                if char in wanted:
                    glyphs[char] = Glyph(*bbx, rows)
    if ascent is None:
        raise ValueError(f"{path} has no FONT_ASCENT")
    return ascent, glyphs


def encode_png(width: int, height: int, ink: set[tuple[int, int]]) -> bytes:
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for x in range(width):
            raw.extend(b"\xff\xff\xff\xff" if (x, y) in ink else b"\x00\x00\x00\x00")

    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
            + chunk(b"IEND", b""))


def cut(chars: list[str], bdf: Path, name: str, lender: tuple[Path, str] | None = None) -> dict[Path, bytes]:
    lent = set(lender[1]) & set(chars) if lender else set()
    ascent, glyphs = load_bdf(bdf, set(chars) - lent)
    if lender:
        lent_ascent, borrowed = load_bdf(lender[0], lent)
        if lent_ascent != ascent:
            raise ValueError(f"{lender[0].name} and {bdf.name} differ in ascent")
        glyphs.update(borrowed)
    missing = [char for char in chars if char not in glyphs]
    if missing:
        raise ValueError(f"{bdf.name} lacks {''.join(missing)}")
    rows = [chars[start:start + PER_ROW] for start in range(0, len(chars), PER_ROW)]
    ink = set()
    for row, line in enumerate(rows):
        for column, char in enumerate(line):
            for x, y in glyphs[char].pixels(ascent):
                if not (0 <= x < CELL and 0 <= y < CELL):
                    raise ValueError(f"{char} spills out of its {CELL}x{CELL} cell in {bdf.name}")
                ink.add((column * CELL + x, row * CELL + y))
    provider = {"type": "bitmap", "file": f"guzhenren:font/{name}.png", "height": CELL, "ascent": ASCENT,
                "chars": ["".join(line).ljust(PER_ROW, "\u0000") for line in rows]}
    font = json.dumps({"providers": [provider]}, ensure_ascii=False, indent=2) + "\n"
    return {RESOURCES / "assets/guzhenren/font" / f"{name}.json": font.encode("utf-8"),
            RESOURCES / "assets/guzhenren/textures/font" / f"{name}.png":
                encode_png(PER_ROW * CELL, len(rows) * CELL, ink)}


def build(repository: Path, source: Path) -> dict[Path, bytes]:
    chars = sorted({*collect((repository / BOOKLET_SOURCE).read_text(encoding="utf-8")),
                    *collect((repository / CHRONICLE_SOURCE).read_text(encoding="utf-8")),
                    *collect_texts(repository / CHRONICLE_TEXTS)})
    outputs: dict[Path, bytes] = {}
    for script in SCRIPTS:
        outputs.update(cut(chars, source / f"fusion-pixel-12px-monospaced-zh_{script}.bdf", f"booklet_{script}",
                           lender=(source / "fusion-pixel-12px-monospaced-zh_hant.bdf", CENTERED)))
    for license_file in LICENSES:
        outputs[RESOURCES / "licenses/fusion-pixel-font" / license_file] = license_text(
            (source / license_file).read_bytes())
    return outputs


def license_text(data: bytes) -> bytes:
    # Git's whitespace check, which commit_push.py runs, refuses trailing blanks; the words stay verbatim.
    return b"\n".join(line.rstrip(b" \t") for line in data.split(b"\n"))


def stale(repository: Path, outputs: dict[Path, bytes]) -> list[Path]:
    return [path for path, data in outputs.items()
            if not (repository / path).is_file() or (repository / path).read_bytes() != data]


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check", action="store_true", help="report stale outputs instead of writing them")
    parser.add_argument("--source", type=Path, default=FONT_ROOT, help="the Fusion Pixel 12px release folder")
    parser.add_argument("--repository", type=Path, default=env.REPOSITORY_ROOT, help=argparse.SUPPRESS)
    args = parser.parse_args(argv)
    outputs = build(args.repository, args.source)
    changed = stale(args.repository, outputs)
    if args.check:
        for path in changed:
            print(f"stale: {path}")
        return 1 if changed else 0
    for path in changed:
        target = args.repository / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(outputs[path])
        print(f"wrote {path}")
    print(f"{len(outputs)} files, {len(changed)} rewritten")
    return 0


if __name__ == "__main__":
    sys.exit(main())
