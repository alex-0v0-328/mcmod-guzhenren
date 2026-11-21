"""The Soulsunder Mountain [荡魂山] shape: a reference schematic or a procedural placeholder, turned into column spans.

    python -B tools/soulsunder_shape.py placeholder [--seed N] [--style S]   a procedural body plus crystal spikes
    python -B tools/soulsunder_shape.py convert REF.schem [--seed N]         a Sponge .schem or legacy .schematic
    python -B tools/soulsunder_shape.py info [SPANS]                         volume, heights and spans of a shape file

Every command writes Gu World's data/guworld/secret_realm/soulsunder_mountain.spans unless --out names another file,
and --preview PNG draws the top view (height as grey) beside the side view (silhouette seen from the south).

placeholder --style picks the body: cone (default), needle, dome, twin or mesa; --spikes overrides its count.
convert crops the reference to its solid blocks (air, fluids, leaves, grass and snow layers do not count, --ignore adds
names or globs), scales it by nearest neighbour to 160 x 128 x 160 (each axis on its own unless --keep-aspect), clips it
to the 80-block disc around the centre, with --natural-foot tidies the reference's own foot (see natural_foot), and
then adds the seeded spikes as placeholder does. Both commands drop every run
of blocks that no face path joins to the bottom layer, so the mountain has no floating pieces.

The shipped shape is Desert Mountain by SPHYRN1DAE (planetminecraft.com/project/desert-mountain-4144359, no licence
stated; the schematic stays out of both repositories), made with convert desert-mountain-1529385474.schematic
--trim-bottom 72 --natural-foot at the default seed: the bottom 72 layers are the reference's flat ground and dunes.

The file (format 1, big-endian): magic "GZSQ", u8 version, u16 width (x), u16 depth (z), u16 height (y); then one
record per column, z-major (index z * width + x): u8 span count and that many (u8 y0, u8 y1) pairs, both inclusive,
ascending and at least one empty block apart. y counts from the mountain's base; column (80, 80) is the centre block.
Gu World's SoulsunderShape reads it once; its pure test holds the same invariants.
"""
from __future__ import annotations

import argparse
import fnmatch
import gzip
import math
import random
import struct
import sys
import zlib
from collections import deque
from dataclasses import dataclass
from pathlib import Path

import project_runtime as env


WIDTH = 160
HEIGHT = 128
CENTRE = 80
RADIUS = 80.0
MAGIC = b"GZSQ"
VERSION = 1
DEFAULT_SEED = 20261120
DEFAULT_OUT = env.GU_WORLD_ROOT / "src/main/resources/data/guworld/secret_realm/soulsunder_mountain.spans"
PEAK = 104
FOOT_OUTLIER = 4
FOOT_HOLE = 16
SUMMIT_ROOM = 16
DEFAULT_IGNORED = ("minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:structure_void",
                   "minecraft:barrier", "minecraft:light", "minecraft:water", "minecraft:lava", "minecraft:*_leaves",
                   "minecraft:short_grass", "minecraft:tall_grass", "minecraft:grass", "minecraft:fern",
                   "minecraft:large_fern", "minecraft:snow")
# Legacy numeric ids for the same kinds: air, water, lava, leaves, tall grass, dead bush, flowers, snow layer,
# leaves2, double plants, barrier, structure void.
LEGACY_IGNORED = frozenset({0, 8, 9, 10, 11, 18, 31, 32, 37, 38, 78, 161, 175, 166, 217})


class ShapeError(ValueError):
    pass


@dataclass
class Grid:
    """Solid flags, one bytearray of HEIGHT per column, index z * WIDTH + x."""
    columns: list[bytearray]

    @classmethod
    def empty(cls) -> "Grid":
        return cls([bytearray(HEIGHT) for _ in range(WIDTH * WIDTH)])

    def column(self, x: int, z: int) -> bytearray:
        return self.columns[z * WIDTH + x]

    def top(self, x: int, z: int) -> int:
        """The highest solid y of a column, -1 when it is empty."""
        column = self.column(x, z)
        index = column.rfind(1)
        return index

    def volume(self) -> int:
        return sum(column.count(1) for column in self.columns)


def in_disc(x: int, z: int) -> bool:
    return (x + 0.5 - CENTRE) ** 2 + (z + 0.5 - CENTRE) ** 2 <= RADIUS * RADIUS


# region Spans and the file


def column_spans(column: bytes) -> list[tuple[int, int]]:
    spans = []
    y = column.find(1)
    while y >= 0:
        end = column.find(0, y)
        if end < 0:
            end = len(column)
        spans.append((y, end - 1))
        y = column.find(1, end)
    return spans


def grid_spans(grid: Grid) -> list[list[tuple[int, int]]]:
    return [column_spans(column) for column in grid.columns]


def encode(spans: list[list[tuple[int, int]]]) -> bytes:
    if len(spans) != WIDTH * WIDTH:
        raise ShapeError(f"expected {WIDTH * WIDTH} columns, got {len(spans)}")
    out = bytearray(MAGIC)
    out += struct.pack(">BHHH", VERSION, WIDTH, WIDTH, HEIGHT)
    for column in spans:
        if len(column) > 255:
            raise ShapeError("a column holds more than 255 spans")
        out.append(len(column))
        for y0, y1 in column:
            out += bytes((y0, y1))
    return bytes(out)


def decode(data: bytes) -> tuple[int, int, int, list[list[tuple[int, int]]]]:
    if data[:4] != MAGIC:
        raise ShapeError("not a soulsunder shape file")
    version, width, depth, height = struct.unpack_from(">BHHH", data, 4)
    if version != VERSION:
        raise ShapeError(f"format {version}, this tool reads {VERSION}")
    offset = 11
    spans = []
    for _ in range(width * depth):
        if offset >= len(data):
            raise ShapeError("the file ends inside the column table")
        count = data[offset]
        offset += 1
        end = offset + 2 * count
        if end > len(data):
            raise ShapeError("the file ends inside a span")
        pairs = [(data[i], data[i + 1]) for i in range(offset, end, 2)]
        offset = end
        last = -2
        for y0, y1 in pairs:
            if y0 > y1 or y0 <= last + 1 or y1 >= height:
                raise ShapeError(f"bad span ({y0}, {y1}) after {last}")
            last = y1
        spans.append(pairs)
    if offset != len(data):
        raise ShapeError(f"{len(data) - offset} bytes trail the column table")
    return width, depth, height, spans


# endregion
# region Shape operations


def keep_grounded(grid: Grid) -> int:
    """Clear every span no face path joins to a span on y = 0; returns the number of blocks cleared."""
    spans = grid_spans(grid)
    reached = [[False] * len(column) for column in spans]
    queue = deque()
    for index, column in enumerate(spans):
        if column and column[0][0] == 0:
            reached[index][0] = True
            queue.append((index, 0))
    while queue:
        index, k = queue.popleft()
        y0, y1 = spans[index][k]
        x, z = index % WIDTH, index // WIDTH
        for nx, nz in ((x - 1, z), (x + 1, z), (x, z - 1), (x, z + 1)):
            if not (0 <= nx < WIDTH and 0 <= nz < WIDTH):
                continue
            neighbour = nz * WIDTH + nx
            for j, (b0, b1) in enumerate(spans[neighbour]):
                if b0 > y1:
                    break
                if b1 >= y0 and not reached[neighbour][j]:
                    reached[neighbour][j] = True
                    queue.append((neighbour, j))
    cleared = 0
    for index, column in enumerate(spans):
        for k, (y0, y1) in enumerate(column):
            if not reached[index][k]:
                grid.columns[index][y0:y1 + 1] = bytes(y1 - y0 + 1)
                cleared += y1 - y0 + 1
    return cleared


def clip_disc(grid: Grid) -> int:
    cleared = 0
    for z in range(WIDTH):
        for x in range(WIDTH):
            if not in_disc(x, z):
                column = grid.column(x, z)
                cleared += column.count(1)
                column[:] = bytes(HEIGHT)
    return cleared


class ValueNoise:
    """Seeded 2D lattice noise in [-1, 1] with smoothstep interpolation."""

    def __init__(self, rng: random.Random):
        self.values = [rng.uniform(-1.0, 1.0) for _ in range(256)]
        self.permutation = list(range(256))
        rng.shuffle(self.permutation)

    def lattice(self, x: int, z: int) -> float:
        return self.values[self.permutation[(self.permutation[x & 255] + z) & 255]]

    def sample(self, x: float, z: float) -> float:
        x0, z0 = math.floor(x), math.floor(z)
        fx, fz = x - x0, z - z0
        sx, sz = fx * fx * (3 - 2 * fx), fz * fz * (3 - 2 * fz)
        top = self.lattice(x0, z0) + (self.lattice(x0 + 1, z0) - self.lattice(x0, z0)) * sx
        bottom = self.lattice(x0, z0 + 1) + (self.lattice(x0 + 1, z0 + 1) - self.lattice(x0, z0 + 1)) * sx
        return top + (bottom - top) * sz

    def fractal(self, x: float, z: float, octaves: int = 3) -> float:
        total, amplitude, frequency, norm = 0.0, 1.0, 1.0, 0.0
        for _ in range(octaves):
            total += amplitude * self.sample(x * frequency, z * frequency)
            norm += amplitude
            amplitude *= 0.5
            frequency *= 2.0
        return total / norm


STYLES = ("cone", "needle", "dome", "twin", "mesa")


def profile(style: str, r: float, dx: float, dz: float, heading: float) -> tuple[float, float]:
    """(height before ridges and noise, flank weight for them) of a style at relative radius r."""
    if style == "cone":
        return PEAK * (1.0 - r) ** 1.35, r * (1.0 - r)
    if style == "needle":
        return 118 * (1.0 - r) ** 2.4, 0.6 * r * (1.0 - r)
    if style == "dome":
        return 76 * (1.0 - r * r) ** 0.8, 0.8 * r * (1.0 - r)
    if style == "twin":
        hx, hz = math.cos(heading), math.sin(heading)
        near = math.hypot(dx - 26 * hx, dz - 26 * hz) / 64
        far = math.hypot(dx + 30 * hx, dz + 30 * hz) / 58
        peaks = max(112 * max(0.0, 1 - near) ** 1.3, 88 * max(0.0, 1 - far) ** 1.3)
        return peaks * (1 - r ** 6), r * (1.0 - r)
    if r < 0.5:
        return 66.0, 0.25
    if r < 0.58:
        return 66 - 40 * ((r - 0.5) / 0.08) ** 0.7, 0.25
    return 26 * (1 - (r - 0.58) / 0.42) ** 1.2, r * (1.0 - r)


def main_peak(rng: random.Random, style: str = "cone") -> Grid:
    """The body under the spikes: cone (a concave peak of PEAK blocks), needle (a thin spire on a wide skirt), dome,
    twin (two summits) or mesa (a cliffed plateau on a talus), each with three seeded ridges and lattice noise."""
    grid = Grid.empty()
    noise = ValueNoise(rng)
    ridges = [(n, rng.uniform(0.5, 1.0), rng.uniform(0, 2 * math.pi)) for n in (3, 5, 7)]
    heading = rng.uniform(0, 2 * math.pi) if style == "twin" else 0.0
    for z in range(WIDTH):
        for x in range(WIDTH):
            if not in_disc(x, z):
                continue
            dx, dz = x + 0.5 - CENTRE, z + 0.5 - CENTRE
            r = math.hypot(dx, dz) / RADIUS
            theta = math.atan2(dz, dx)
            ridge = sum(amplitude * math.cos(n * theta + phase) for n, amplitude, phase in ridges) / 2.25
            body, flank = profile(style, r, dx, dz, heading)
            height = body + 22 * ridge * flank + 10 * noise.fractal(x / 24, z / 24) * flank
            top = min(HEIGHT, int(round(height)))
            if top > 0:
                grid.column(x, z)[0:top] = b"\x01" * top
    return grid


def add_spike(grid: Grid, base: tuple[float, float, float], axis: tuple[float, float, float], length: float,
              radius: float, twist: float) -> int:
    """A hexagonal crystal: a prism for the first 60% of its length, then a pyramid to the tip; returns blocks set."""
    ax, ay, az = axis
    # u lies in the horizontal plane across the axis, v completes the frame; twist turns the hexagon about the axis.
    ux, uz = -az, ax
    norm = math.hypot(ux, uz) or 1.0
    ux, uy, uz = ux / norm, 0.0, uz / norm
    vx, vy, vz = ay * uz - az * uy, az * ux - ax * uz, ax * uy - ay * ux
    c, s = math.cos(twist), math.sin(twist)
    ux, uy, uz, vx, vy, vz = (c * ux + s * vx, c * uy + s * vy, c * uz + s * vz,
                              -s * ux + c * vx, -s * uy + c * vy, -s * uz + c * vz)
    bx, by, bz = base
    tip = (bx + ax * length, by + ay * length, bz + az * length)
    low = [math.floor(min(b, t) - radius) for b, t in zip(base, tip)]
    high = [math.ceil(max(b, t) + radius) for b, t in zip(base, tip)]
    shoulder = length * 0.6
    placed = 0
    for z in range(max(0, low[2]), min(WIDTH - 1, high[2]) + 1):
        for x in range(max(0, low[0]), min(WIDTH - 1, high[0]) + 1):
            if not in_disc(x, z):
                continue
            column = grid.column(x, z)
            for y in range(max(0, low[1]), min(HEIGHT - 1, high[1]) + 1):
                px, py, pz = x + 0.5 - bx, y + 0.5 - by, z + 0.5 - bz
                t = px * ax + py * ay + pz * az
                if t < 0 or t > length:
                    continue
                qx, qy, qz = px - t * ax, py - t * ay, pz - t * az
                pu = qx * ux + qy * uy + qz * uz
                pv = qx * vx + qy * vy + qz * vz
                hexagon = max(abs(pu), abs(0.5 * pu + 0.866 * pv), abs(0.5 * pu - 0.866 * pv))
                limit = radius if t <= shoulder else radius * (length - t) / (length - shoulder)
                if hexagon <= limit and not column[y]:
                    column[y] = 1
                    placed += 1
    return placed


def add_spikes(grid: Grid, rng: random.Random, count: int) -> list[dict]:
    """count crystals: one upright on the summit reaching the top layer (none when the summit is already within
    SUMMIT_ROOM of it), the rest rooted on the surface between 12% and 58% of the radius, evenly spread round the
    centre with jitter, each leaning 8-28 degrees outward; every base sits one radius under the surface."""
    spikes = []
    summit = max(range(WIDTH * WIDTH), key=lambda index: (grid.columns[index].rfind(1), -abs(index % WIDTH - CENTRE)))
    x, z = summit % WIDTH, summit // WIDTH
    top = grid.top(x, z)
    if 0 <= top < HEIGHT - SUMMIT_ROOM:
        # The summit crystal stands near upright on the highest column and ends on the top layer.
        radius = rng.uniform(5.0, 7.5)
        tilt, heading = math.radians(rng.uniform(0, 6)), rng.uniform(0, 2 * math.pi)
        axis = (math.sin(tilt) * math.cos(heading), math.cos(tilt), math.sin(tilt) * math.sin(heading))
        base = (x + 0.5, top + 0.5 - radius, z + 0.5)
        length = (HEIGHT - 0.5 - base[1]) / axis[1]
        placed = add_spike(grid, base, axis, length, radius, rng.uniform(0, math.pi / 3))
        spikes.append({"x": x, "z": z, "surface": top, "length": round(length, 1), "radius": round(radius, 1),
                       "tilt": round(math.degrees(tilt), 1), "blocks": placed})
        count -= 1
    start = rng.uniform(0, 2 * math.pi)
    for k in range(count):
        theta = start + k * 2 * math.pi / count + rng.uniform(-0.35, 0.35)
        distance = rng.uniform(0.12, 0.58) * RADIUS
        x = min(WIDTH - 1, max(0, int(CENTRE + distance * math.cos(theta))))
        z = min(WIDTH - 1, max(0, int(CENTRE + distance * math.sin(theta))))
        top = grid.top(x, z)
        if top < 0:
            continue
        tilt = math.radians(rng.uniform(8, 28))
        heading = theta + rng.uniform(-0.4, 0.4)
        axis = (math.sin(tilt) * math.cos(heading), math.cos(tilt), math.sin(tilt) * math.sin(heading))
        radius = rng.uniform(3.5, 7.0)
        length = rng.uniform(28, 48)
        base = (x + 0.5, top + 0.5 - radius, z + 0.5)
        placed = add_spike(grid, base, axis, length, radius, rng.uniform(0, math.pi / 3))
        spikes.append({"x": x, "z": z, "surface": top, "length": round(length, 1), "radius": round(radius, 1),
                       "tilt": round(math.degrees(tilt), 1), "blocks": placed})
    return spikes


SPIKES = {"needle": 4, "dome": 8, "twin": 6, "mesa": 9}


def placeholder(seed: int, style: str = "cone", spikes: int | None = None) -> tuple[Grid, dict]:
    """A procedural mountain: the style's body plus its spikes (cone draws 4-6 from the seed)."""
    rng = random.Random(seed)
    grid = main_peak(rng, style)
    count = rng.randint(4, 6) if style == "cone" else SPIKES[style]
    spikes = add_spikes(grid, rng, count if spikes is None else spikes)
    cleared = keep_grounded(grid)
    return grid, {"spikes": spikes, "floating_cleared": cleared}


# endregion
# region Schematics


def read_nbt(data: bytes) -> dict:
    """The root compound of an NBT file, gzip or raw; values come back as ints, floats, bytes, str, lists and dicts."""
    if data[:2] == b"\x1f\x8b":
        data = gzip.decompress(data)
    position = 0

    def take(size: int) -> bytes:
        nonlocal position
        if position + size > len(data):
            raise ShapeError("the NBT data ends early")
        chunk = data[position:position + size]
        position += size
        return chunk

    def unpack(fmt: str):
        return struct.unpack(">" + fmt, take(struct.calcsize(">" + fmt)))[0]

    def string() -> str:
        return take(unpack("H")).decode("utf-8", "replace")

    def payload(kind: int):
        if kind == 1: return unpack("b")
        if kind == 2: return unpack("h")
        if kind == 3: return unpack("i")
        if kind == 4: return unpack("q")
        if kind == 5: return unpack("f")
        if kind == 6: return unpack("d")
        if kind == 7: return take(unpack("i"))
        if kind == 8: return string()
        if kind == 9:
            item, size = unpack("b"), unpack("i")
            return [payload(item) for _ in range(size)]
        if kind == 10:
            compound = {}
            while (tag := unpack("b")) != 0:
                name = string()
                compound[name] = payload(tag)
            return compound
        if kind == 11: return list(struct.unpack(f">{(size := unpack('i'))}i", take(4 * size)))
        if kind == 12: return list(struct.unpack(f">{(size := unpack('i'))}q", take(8 * size)))
        raise ShapeError(f"unknown NBT tag {kind}")

    if unpack("b") != 10:
        raise ShapeError("the NBT root is not a compound")
    string()
    return payload(10)


def varints(data: bytes, count: int) -> list[int]:
    values, value, shift = [], 0, 0
    for byte in data:
        value |= (byte & 0x7F) << shift
        if byte & 0x80:
            shift += 7
            continue
        values.append(value)
        value, shift = 0, 0
        if len(values) == count:
            break
    if len(values) != count:
        raise ShapeError(f"block data holds {len(values)} entries, expected {count}")
    return values


def block_name(state: str) -> str:
    name = state.split("[", 1)[0].strip()
    return name if ":" in name else "minecraft:" + name


@dataclass
class Schematic:
    width: int
    height: int
    length: int
    solid: bytearray  # index (y * length + z) * width + x

    def at(self, x: int, y: int, z: int) -> int:
        return self.solid[(y * self.length + z) * self.width + x]


def read_schematic(data: bytes, ignored: tuple[str, ...] = DEFAULT_IGNORED) -> Schematic:
    root = read_nbt(data)
    if isinstance(root.get("Schematic"), dict):
        root = root["Schematic"]
    width, height, length = (root[key] & 0xFFFF for key in ("Width", "Height", "Length"))
    size = width * height * length

    def skip(name: str) -> bool:
        return any(fnmatch.fnmatchcase(name, pattern) for pattern in ignored)

    if isinstance(root.get("Blocks"), dict):
        palette, raw = root["Blocks"]["Palette"], root["Blocks"]["Data"]
    elif "BlockData" in root:
        palette, raw = root["Palette"], root["BlockData"]
    elif isinstance(root.get("Blocks"), bytes):
        ids = root["Blocks"]
        add = root.get("AddBlocks", b"")
        solid = bytearray(size)
        for index in range(size):
            block = ids[index]
            if add:
                nibble = add[index >> 1]
                block |= ((nibble >> 4) if index % 2 == 0 else nibble & 0x0F) << 8
            solid[index] = block not in LEGACY_IGNORED
        return Schematic(width, height, length, solid)
    else:
        raise ShapeError("neither a Sponge .schem nor a legacy .schematic")
    hollow = {index for state, index in palette.items() if skip(block_name(state))}
    solid = bytearray(index not in hollow for index in varints(raw, size))
    return Schematic(width, height, length, solid)


def column_heights(grid: Grid) -> list[int]:
    return [column.rfind(1) + 1 for column in grid.columns]


def natural_foot(grid: Grid) -> dict:
    """Tidy the reference's own foot instead of padding it: columns more than FOOT_OUTLIER blocks off their 3x3 median
    take that median, only the largest face-joined footprint stays (stray dune fragments go), enclosed gaps of up to
    FOOT_HOLE columns are filled to their neighbours' median, and every column is made solid from y 0 to its top.
    The outline stays as irregular as the reference; no envelope or disc-wide pad, which drew concentric terraces."""
    heights = column_heights(grid)
    cleaned = list(heights)
    outliers = 0
    for z in range(WIDTH):
        for x in range(WIDTH):
            around = sorted(heights[nz * WIDTH + nx] for nz in range(max(0, z - 1), min(WIDTH, z + 2))
                            for nx in range(max(0, x - 1), min(WIDTH, x + 2)))
            median = around[len(around) // 2]
            if abs(heights[z * WIDTH + x] - median) > FOOT_OUTLIER:
                cleaned[z * WIDTH + x] = median
                outliers += 1
    filled = 0
    for region in regions(cleaned, lambda height: height == 0):
        if len(region) <= FOOT_HOLE and not any(touches_edge(index) for index in region):
            for index in region:
                x, z = index % WIDTH, index // WIDTH
                around = sorted(cleaned[nz * WIDTH + nx] for nx, nz in neighbours(x, z) if cleaned[nz * WIDTH + nx])
                cleaned[index] = around[len(around) // 2] if around else 1
            filled += len(region)
    bodies = sorted(regions(cleaned, lambda height: height > 0), key=len, reverse=True)
    dropped = sum(len(body) for body in bodies[1:])
    for body in bodies[1:]:
        for index in body:
            cleaned[index] = 0
    added = 0
    for index, height in enumerate(cleaned):
        column = grid.columns[index]
        added += height - column[:height].count(1)
        column[:] = b"\x01" * height + bytes(HEIGHT - height)
    return {"foot_outliers": outliers, "foot_holes_filled": filled, "foot_fragments_dropped": dropped,
            "foot_added": added}


def neighbours(x: int, z: int) -> list[tuple[int, int]]:
    return [(nx, nz) for nx, nz in ((x - 1, z), (x + 1, z), (x, z - 1), (x, z + 1))
            if 0 <= nx < WIDTH and 0 <= nz < WIDTH]


def touches_edge(index: int) -> bool:
    x, z = index % WIDTH, index // WIDTH
    return not in_disc(x, z) or any(not in_disc(nx, nz) for nx, nz in neighbours(x, z))


def regions(heights: list[int], member) -> list[list[int]]:
    """Face-joined regions of disc columns whose height passes member."""
    seen = [False] * len(heights)
    found = []
    for start in range(len(heights)):
        if seen[start] or not in_disc(start % WIDTH, start // WIDTH) or not member(heights[start]):
            continue
        seen[start] = True
        region, queue = [], deque([start])
        while queue:
            index = queue.popleft()
            region.append(index)
            for nx, nz in neighbours(index % WIDTH, index // WIDTH):
                other = nz * WIDTH + nx
                if not seen[other] and in_disc(nx, nz) and member(heights[other]):
                    seen[other] = True
                    queue.append(other)
        found.append(region)
    return found


def foot_reach(heights: list[int], sectors: int = 72) -> tuple[float, float]:
    """The farthest foot column's distance from the centre in each of sectors directions: (smallest, largest)."""
    reach = [0.0] * sectors
    for index, height in enumerate(heights):
        if height:
            dx, dz = index % WIDTH + 0.5 - CENTRE, index // WIDTH + 0.5 - CENTRE
            sector = int((math.atan2(dz, dx) + math.pi) / (2 * math.pi) * sectors) % sectors
            reach[sector] = max(reach[sector], math.hypot(dx, dz))
    return min(reach), max(reach)


def convert(schematic: Schematic, seed: int, spikes: int, keep_aspect: bool = False,
            trim_bottom: int = 0, tidy_foot: bool = False) -> tuple[Grid, dict]:
    xs, ys, zs = [], [], []
    for y in range(trim_bottom, schematic.height):
        for z in range(schematic.length):
            for x in range(schematic.width):
                if schematic.at(x, y, z):
                    xs.append(x)
                    ys.append(y)
                    zs.append(z)
    if not xs:
        raise ShapeError("the reference holds no solid block")
    x0, y0, z0 = min(xs), min(ys), min(zs)
    spans = (max(xs) - x0 + 1, max(ys) - y0 + 1, max(zs) - z0 + 1)
    if keep_aspect:
        scale = min(WIDTH / spans[0], HEIGHT / spans[1], WIDTH / spans[2])
        size = [max(1, round(extent * scale)) for extent in spans]
    else:
        size = [WIDTH, HEIGHT, WIDTH]
    offset_x, offset_z = (WIDTH - size[0]) // 2, (WIDTH - size[2]) // 2
    y_map = [y0 + min(spans[1] - 1, int((y + 0.5) * spans[1] / size[1])) for y in range(size[1])]
    grid = Grid.empty()
    for z in range(size[2]):
        source_z = z0 + min(spans[2] - 1, int((z + 0.5) * spans[2] / size[2]))
        for x in range(size[0]):
            source_x = x0 + min(spans[0] - 1, int((x + 0.5) * spans[0] / size[0]))
            column = grid.column(offset_x + x, offset_z + z)
            for y, source_y in enumerate(y_map):
                column[y] = schematic.at(source_x, source_y, source_z)
    clipped = clip_disc(grid)
    foot = natural_foot(grid) if tidy_foot else {}
    added = add_spikes(grid, random.Random(seed), spikes)
    cleared = keep_grounded(grid)
    return grid, {"reference": {"width": schematic.width, "height": schematic.height, "length": schematic.length,
                                "solid_box": spans}, "scaled_to": size, "disc_clipped": clipped, **foot,
                  "spikes": added, "floating_cleared": cleared}


# endregion
# region Output


def describe(spans: list[list[tuple[int, int]]]) -> dict:
    volume = sum(y1 - y0 + 1 for column in spans for y0, y1 in column)
    filled = [column for column in spans if column]
    return {"volume": volume, "columns": len(filled), "max_height": max((c[-1][1] + 1 for c in filled), default=0),
            "max_spans": max((len(c) for c in filled), default=0),
            "multi_span_columns": sum(1 for c in filled if len(c) > 1),
            "grounded_columns": sum(1 for c in filled if c[0][0] == 0),
            "outside_disc": sum(1 for i, c in enumerate(spans) if c and not in_disc(i % WIDTH, i // WIDTH)),
            "foot_reach": [round(value, 1) for value in foot_reach([c[-1][1] + 1 if c else 0 for c in spans])]}


def png(width: int, height: int, rows: list[bytes]) -> bytes:
    def chunk(kind: bytes, body: bytes) -> bytes:
        return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body))

    raw = b"".join(b"\x00" + row for row in rows)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 0, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def preview(spans: list[list[tuple[int, int]]]) -> bytes:
    """Top view (brightness = height) on the left, the silhouette seen from the south on the right, 2x scale."""
    top = [[0] * WIDTH for _ in range(WIDTH)]
    side = [[0] * WIDTH for _ in range(HEIGHT)]
    for index, column in enumerate(spans):
        x, z = index % WIDTH, index // WIDTH
        if column:
            top[z][x] = 40 + int(215 * (column[-1][1] + 1) / HEIGHT)
        for y0, y1 in column:
            for y in range(y0, y1 + 1):
                # Nearer columns (larger z) paint brighter, so the silhouette keeps some depth.
                side[HEIGHT - 1 - y][x] = max(side[HEIGHT - 1 - y][x], 60 + int(195 * z / WIDTH))
    rows = []
    for row in range(WIDTH):
        left = top[row]
        right = side[row - (WIDTH - HEIGHT)] if row >= WIDTH - HEIGHT else [0] * WIDTH
        line = bytes(v for v in left for _ in range(2)) + b"\x20\x20" + bytes(v for v in right for _ in range(2))
        rows += [line, line]
    return png(len(rows[0]), len(rows), rows)


def write(grid: Grid, out: Path, preview_path: Path | None) -> dict:
    spans = grid_spans(grid)
    data = encode(spans)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(data)
    if preview_path:
        preview_path.write_bytes(preview(spans))
    return {"file": str(out), "bytes": len(data), **describe(spans)}


def report(lines: dict, echo=print) -> None:
    for key, value in lines.items():
        if key == "spikes":
            for spike in value:
                echo(f"  spike {spike}")
        else:
            echo(f"{key}: {value}")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    commands = parser.add_subparsers(dest="command", required=True)
    for name in ("placeholder", "convert"):
        command = commands.add_parser(name)
        if name == "placeholder":
            command.add_argument("--style", choices=STYLES, default="cone")
            command.add_argument("--spikes", type=int, help="spike count, the summit crystal included")
        if name == "convert":
            command.add_argument("reference", type=Path)
            command.add_argument("--spikes", type=int, default=5)
            command.add_argument("--keep-aspect", action="store_true")
            command.add_argument("--trim-bottom", type=int, default=0, help="drop this many bottom layers first")
            command.add_argument("--natural-foot", action="store_true",
                                 help="tidy the reference's foot: no fragments, no small gaps, solid columns")
            command.add_argument("--ignore", nargs="*", default=[], help="more block names or globs that are air")
        command.add_argument("--seed", type=int, default=DEFAULT_SEED)
        command.add_argument("--out", type=Path, default=DEFAULT_OUT)
        command.add_argument("--preview", type=Path)
    info = commands.add_parser("info")
    info.add_argument("spans", type=Path, nargs="?", default=DEFAULT_OUT)
    info.add_argument("--preview", type=Path)
    arguments = parser.parse_args(argv)
    try:
        if arguments.command == "info":
            width, depth, height, spans = decode(arguments.spans.read_bytes())
            if arguments.preview:
                arguments.preview.write_bytes(preview(spans))
            report({"file": str(arguments.spans), "size": f"{width} x {height} x {depth}", **describe(spans)})
            return 0
        if arguments.command == "placeholder":
            grid, notes = placeholder(arguments.seed, arguments.style, arguments.spikes)
        else:
            ignored = DEFAULT_IGNORED + tuple(block_name(name) if "*" not in name else name
                                              for name in arguments.ignore)
            schematic = read_schematic(arguments.reference.read_bytes(), ignored)
            grid, notes = convert(schematic, arguments.seed, arguments.spikes, arguments.keep_aspect,
                                  arguments.trim_bottom, arguments.natural_foot)
        report({"seed": arguments.seed, **notes, **write(grid, arguments.out, arguments.preview)})
        return 0
    except (OSError, ShapeError, KeyError) as error:
        print(f"soulsunder_shape: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
