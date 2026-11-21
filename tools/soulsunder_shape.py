"""The Soulsunder Mountain [荡魂山] shape: a reference schematic or a procedural placeholder, turned into column spans.

    python -B tools/soulsunder_shape.py placeholder [--seed N] [--style S]   a procedural body plus crystal spikes
    python -B tools/soulsunder_shape.py convert REF.schem [--seed N]         a Sponge .schem or legacy .schematic
    python -B tools/soulsunder_shape.py info [SPANS]                         volume, heights and spans of a shape file
    python -B tools/soulsunder_shape.py palace [SPANS]                       cut the trail and the palace hall

Every command writes Gu World's data/guworld/secret_realm/soulsunder_mountain.spans unless --out names another file,
and --preview PNG draws the top view (height as grey) beside the side view (silhouette seen from the south).

placeholder --style picks the body: cone (default), needle, dome, twin or mesa; --spikes overrides its count.
convert crops the reference to its solid blocks (air, fluids, leaves, grass and snow layers do not count, --ignore adds
names or globs), scales it by nearest neighbour to 160 x 128 x 160 (each axis on its own unless --keep-aspect), clips it
to the 80-block disc around the centre, with --natural-foot tidies the reference's own foot (see natural_foot), and
then adds the seeded spikes as placeholder does. Both commands drop every run
of blocks that no face path joins to the bottom layer, so the mountain has no floating pieces.

convert can reshape the tidied column heights before the spikes; these need --natural-foot and run in this order:
    --peak N           scales every height so the highest column is N (a column that had blocks keeps at least one)
    --lift L           raises the flanks: h + L * 4 * (h / H) * (1 - h / H), rounded, with H the highest column, so the
                       foot and the summit stay and mid-height rises by L; L under H / 4 keeps every slope rising
    --blunt KNEE CAP   eases every height h above KNEE towards CAP:
                       KNEE + (CAP - KNEE) * (1 - e^(-(h - KNEE) / (CAP - KNEE))), rounded
--spike-radius LO HI and --spike-length LO HI set the flank crystals' ranges (3.5-7 and 28-48 by default), and
--no-summit-spike leaves the summit crystal out: no draw for it, and every --spikes is a flank crystal.

The shipped shape is Desert Mountain by SPHYRN1DAE (planetminecraft.com/project/desert-mountain-4144359, no licence
stated; the schematic stays out of both repositories), made at the default seed with
    convert desert-mountain-1529385474.schematic --trim-bottom 72 --natural-foot --lift 16 --blunt 104 124
            --no-summit-spike --spikes 5 --spike-radius 2.0 3.0 --spike-length 12 20
The bottom 72 layers are the reference's flat ground and dunes. Alex found the stretched reference too sharp and its
crystals too big and asked for a fuller mountain that keeps most of its height: hence the lifted flanks, the rounded
summit and five thin flank crystals.

palace reads a shape file (the shipped one by default) and writes it back with the Soulsunder Palace [荡魂行宫] and
the mountain trail cut in; it changes only fixed cells, so running it again gives the same bytes. In this order:
    trail    a path TRAIL_WIDTH * 2 blocks wide along the TRAIL waypoints, from the north-west foot round to the
             palace mouth on the south flank and on up to the summit; every trail column becomes solid up to the
             trail height and open above, and the height rises 0.45 blocks per block of waypoint line, so vanilla
             walking climbs it (the converted mountain's own steps are 2-12 blocks, too high to jump)
    hall     an eroded cave round the PALACE_X x PALACE_Z footprint, floor at PALACE_FLOOR: a rounded room whose
             ceiling domes up to PALACE_DOME blocks, its walls pushed in and out by up to ERODE_AMPLITUDE blocks
             of domain-warped noise, STALACTITES drips and STALAGMITES stumps; under at least ROOF blocks
    tunnel   a winding tube from the hall's south wall to TUNNEL_END, its middle swinging up to TUNNEL_MEANDER
             blocks; a 3 x 3 walking core always stays open, and the mouth row is the plain TUNNEL_X x TUNNEL_HEIGHT
Only the air joined to the anchor stays, and stone left floating inside the cave becomes air. Alex chose the heavy
erosion of three previews and a bare cave: no walls, floor or furnishing of other blocks. The trail waypoints were
fitted once to the shipped mountain's contours (from the mouth at height 40 up and down, then thinned to one point
per 5 blocks). The command fails rather than leave fewer than ROOF solid blocks over the hall, or leave the summit or
the anchor out of a walk that starts on the open ground at the shape's edge (GROUND, one block over the base), climbs
STEP_UP and drops STEP_DOWN.

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
SPIKE_RADIUS = (3.5, 7.0)
SPIKE_LENGTH = (28.0, 48.0)
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


def add_spikes(grid: Grid, rng: random.Random, count: int, radius_range: tuple[float, float] = SPIKE_RADIUS,
               length_range: tuple[float, float] = SPIKE_LENGTH, summit_spike: bool = True) -> list[dict]:
    """count crystals: one upright on the summit reaching the top layer (none when the summit is already within
    SUMMIT_ROOM of it or summit_spike is off), the rest rooted on the surface between 12% and 58% of the radius,
    evenly spread round the centre with jitter, each leaning 8-28 degrees outward, their radius and length drawn from
    radius_range and length_range; every base sits one radius under the surface."""
    spikes = []
    summit = max(range(WIDTH * WIDTH), key=lambda index: (grid.columns[index].rfind(1), -abs(index % WIDTH - CENTRE)))
    x, z = summit % WIDTH, summit // WIDTH
    top = grid.top(x, z)
    if summit_spike and 0 <= top < HEIGHT - SUMMIT_ROOM:
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
        radius = rng.uniform(*radius_range)
        length = rng.uniform(*length_range)
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


def peak_heights(heights: list[int], peak: int) -> list[int]:
    """Every height scaled so the highest is peak; a column that had blocks keeps at least one."""
    top = max(heights)
    if top == 0:
        return list(heights)
    return [max(1, round(height * peak / top)) if height else 0 for height in heights]


def lift_heights(heights: list[int], lift: float) -> list[int]:
    """Every height h raised by lift * 4 * (h / H) * (1 - h / H), rounded, H the highest: nothing at the foot or the
    summit, lift at mid-height. Below H / 4 the curve keeps rising, so no slope turns over and a step between two
    columns grows by at most 1 + 4 * lift / H times."""
    top = max(heights)
    if not 0 <= lift < top / 4:
        raise ShapeError(f"--lift needs 0 <= L < {top / 4} (a quarter of the highest column), got {lift}")
    return [round(height + lift * 4 * (height / top) * (1 - height / top)) if height else 0 for height in heights]


def blunt_heights(heights: list[int], knee: int, cap: int) -> list[int]:
    """Heights above knee eased towards cap, rounded; knee and below stay."""
    room = cap - knee
    return [round(knee + room * (1 - math.exp(-(height - knee) / room))) if height > knee else height
            for height in heights]


def check_reshape(tidy_foot: bool, peak: int | None, lift: float | None, blunt: tuple[int, int] | None,
                  spike_radius: tuple[float, float], spike_length: tuple[float, float]) -> None:
    if (peak is not None or lift is not None or blunt is not None) and not tidy_foot:
        raise ShapeError("--peak, --lift and --blunt reshape a height map and need --natural-foot")
    if peak is not None and not 1 <= peak <= HEIGHT:
        raise ShapeError(f"--peak must lie in 1..{HEIGHT}, got {peak}")
    if lift is not None and lift < 0:
        raise ShapeError(f"--lift must not be negative, got {lift}")
    if blunt is not None and not 0 <= blunt[0] < blunt[1] <= HEIGHT:
        raise ShapeError(f"--blunt needs 0 <= KNEE < CAP <= {HEIGHT}, got {blunt[0]} {blunt[1]}")
    for name, (low, high) in (("--spike-radius", spike_radius), ("--spike-length", spike_length)):
        if not 0 < low <= high:
            raise ShapeError(f"{name} needs 0 < LO <= HI, got {low} {high}")


def reshape(grid: Grid, peak: int | None = None, lift: float | None = None,
            blunt: tuple[int, int] | None = None) -> dict:
    """Peak, then lift, then blunt the column heights of a height map (every column solid from y 0 to its top)."""
    heights = column_heights(grid)
    before = (max(heights), sum(1 for height in heights if height))
    if peak is not None:
        heights = peak_heights(heights, peak)
    raised = 0
    if lift is not None:
        lifted = lift_heights(heights, lift)
        raised = sum(1 for old, new in zip(heights, lifted) if new > old)
        heights = lifted
    if blunt is not None:
        heights = blunt_heights(heights, *blunt)
    for column, height in zip(grid.columns, heights):
        column[:] = b"\x01" * height + bytes(HEIGHT - height)
    return {"max_height_before": before[0], "max_height_after": max(heights), "columns_before": before[1],
            "columns_after": sum(1 for height in heights if height), "lifted_columns": raised}


def convert(schematic: Schematic, seed: int, spikes: int, keep_aspect: bool = False,
            trim_bottom: int = 0, tidy_foot: bool = False, peak: int | None = None, lift: float | None = None,
            blunt: tuple[int, int] | None = None, spike_radius: tuple[float, float] = SPIKE_RADIUS,
            spike_length: tuple[float, float] = SPIKE_LENGTH, summit_spike: bool = True) -> tuple[Grid, dict]:
    check_reshape(tidy_foot, peak, lift, blunt, spike_radius, spike_length)
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
    shaped = {}
    if peak is not None or lift is not None or blunt is not None:
        shaped = {"reshape": reshape(grid, peak, lift, blunt)}
    added = add_spikes(grid, random.Random(seed), spikes, tuple(spike_radius), tuple(spike_length), summit_spike)
    cleared = keep_grounded(grid)
    return grid, {"reference": {"width": schematic.width, "height": schematic.height, "length": schematic.length,
                                "solid_box": spans}, "scaled_to": size, "disc_clipped": clipped, **foot, **shaped,
                  "spikes": added, "floating_cleared": cleared}


# endregion
# region Palace and trail


PALACE_FLOOR = 40
PALACE_X = (-9, 5)
PALACE_Z = (-13, 7)
PALACE_HEIGHT = 8
PALACE_DOME = 5
TUNNEL_X = (-3, -1)
TUNNEL_HEIGHT = 4
TUNNEL_END = 24
TUNNEL_MEANDER = 2.07
TUNNEL_ROUGHNESS = 1.97
ERODE_AMPLITUDE = 4.2
ERODE_SCALE = 5.0
ERODE_WARP = 4.5
ERODE_SEED = 13
STALACTITES = 18
STALAGMITES = 5
ROOF = 3
ANCHOR = (-2, PALACE_FLOOR, -3)
SUMMIT = (4, 118, -2)
TRAIL_WIDTH = 1.5
TRAIL_SAMPLE = 0.1
STEP_UP = 1
STEP_DOWN = 3
# The base layer replaces the grass, so the ground round the mountain stands one block over it.
GROUND = 1
# (dx, dz, height) from the centre column; height is the standing y above the mountain's base.
TRAIL = ((-72, -30, 1.0), (-56, -23, 1.0), (-54, -18, 2.8), (-50, -14, 5.2), (-46, -11, 7.7), (-43, -7, 10.1),
         (-45, -2, 12.4), (-47, 3, 14.8), (-45, 8, 17.1), (-41, 11, 19.3), (-37, 15, 21.6), (-34, 18, 23.9),
         (-29, 20, 26.2), (-24, 19, 28.5), (-19, 19, 30.9), (-14, 21, 33.1), (-10, 23, 35.4), (-5, 25, 37.7),
         (0, 24, 40.0), (4, 21, 42.3), (8, 19, 44.6), (13, 20, 46.8), (18, 20, 49.1), (21, 16, 51.4), (22, 11, 53.7),
         (23, 6, 56.0), (23, 1, 58.2), (27, -3, 60.5), (24, -8, 62.9), (20, -10, 65.2), (15, -12, 67.4),
         (10, -13, 69.7), (7, -16, 72.0), (1, -16, 74.2), (-3, -15, 76.5), (-7, -11, 78.8), (-11, -9, 81.0),
         (-13, -4, 83.3), (-12, 1, 85.6), (-9, 5, 87.8), (-4, 7, 90.1), (1, 8, 92.3), (6, 9, 94.6), (10, 8, 96.9),
         (15, 6, 99.1), (15, 1, 101.4), (14, -4, 103.7), (10, -6, 106.0), (5, -8, 108.2), (0, -9, 110.5),
         (-4, -6, 112.7), (-5, -2, 115.0), (-3, 2, 117.0), (4, -2, 118.0))


def grid_from_spans(spans: list[list[tuple[int, int]]]) -> Grid:
    grid = Grid.empty()
    for index, column in enumerate(spans):
        for y0, y1 in column:
            grid.columns[index][y0:y1 + 1] = b"\x01" * (y1 - y0 + 1)
    return grid


def trail_cells(waypoints=TRAIL) -> dict[int, int]:
    """Column index -> standing height for every column within TRAIL_WIDTH of the waypoint line."""
    best: dict[int, tuple[float, float]] = {}
    for (x0, z0, h0), (x1, z1, h1) in zip(waypoints, waypoints[1:]):
        length = math.hypot(x1 - x0, z1 - z0)
        steps = max(1, math.ceil(length / TRAIL_SAMPLE))
        for i in range(steps + 1):
            t = i / steps
            px, pz, ph = x0 + (x1 - x0) * t, z0 + (z1 - z0) * t, h0 + (h1 - h0) * t
            for dz in range(math.floor(pz - TRAIL_WIDTH), math.ceil(pz + TRAIL_WIDTH) + 1):
                for dx in range(math.floor(px - TRAIL_WIDTH), math.ceil(px + TRAIL_WIDTH) + 1):
                    distance = math.hypot(dx - px, dz - pz)
                    if distance > TRAIL_WIDTH:
                        continue
                    index = (CENTRE + dz) * WIDTH + CENTRE + dx
                    if index not in best or distance < best[index][0]:
                        best[index] = (distance, ph)
    return {index: math.floor(height + 0.5) for index, (_, height) in best.items()}


def cut_trail(grid: Grid, cells: dict[int, int]) -> tuple[int, int]:
    """Every trail column solid below its height and open above; returns (blocks cut, blocks filled)."""
    cut = filled = 0
    for index, height in cells.items():
        column = grid.columns[index]
        filled += column[:height].count(0)
        cut += column[height:].count(1)
        column[:height] = b"\x01" * height
        column[height:] = bytes(HEIGHT - height)
    return cut, filled


def lattice3(x: int, y: int, z: int, seed: int) -> float:
    """A hashed lattice value in [-1, 1]."""
    n = (x * 374761393 + y * 668265263 + z * 2147483647 + seed * 144665) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 32767.5 - 1.0


def noise3(x: float, y: float, z: float, seed: int) -> float:
    x0, y0, z0 = math.floor(x), math.floor(y), math.floor(z)
    sx, sy, sz = ((t - f) * (t - f) * (3 - 2 * (t - f)) for t, f in ((x, x0), (y, y0), (z, z0)))

    def lerp(a: float, b: float, t: float) -> float:
        return a + (b - a) * t

    c = [[[lattice3(x0 + i, y0 + j, z0 + k, seed) for k in (0, 1)] for j in (0, 1)] for i in (0, 1)]
    return lerp(lerp(lerp(c[0][0][0], c[0][0][1], sz), lerp(c[0][1][0], c[0][1][1], sz), sy),
                lerp(lerp(c[1][0][0], c[1][0][1], sz), lerp(c[1][1][0], c[1][1][1], sz), sy), sx)


def fractal3(x: float, y: float, z: float, seed: int, octaves: int) -> float:
    total, amplitude, frequency, norm = 0.0, 1.0, 1.0, 0.0
    for octave in range(octaves):
        total += amplitude * noise3(x * frequency, y * frequency, z * frequency, seed + octave * 31)
        norm += amplitude
        amplitude *= 0.5
        frequency *= 2.0
    return total / norm


def erosion(x: float, y: float, z: float) -> float:
    """Domain-warped fractal noise: the warp turns a wobble into scalloped, water-worn walls."""
    warp = ERODE_WARP * fractal3(x / 7, y / 7, z / 7, ERODE_SEED + 1, 2)
    shift = ERODE_WARP * fractal3(x / 7, y / 7, z / 7, ERODE_SEED + 2, 2)
    return fractal3((x + warp) / ERODE_SCALE, y / ERODE_SCALE, (z + shift) / ERODE_SCALE, ERODE_SEED, 3)


def palace_air() -> set[tuple[int, int, int]]:
    """The eroded hall and tunnel as (dx, y, dz) air cells, fixed by the constants alone."""
    centre_x, centre_z = (PALACE_X[0] + PALACE_X[1]) / 2, (PALACE_Z[0] + PALACE_Z[1]) / 2
    half_x, half_z = (PALACE_X[1] - PALACE_X[0] + 1) / 2, (PALACE_Z[1] - PALACE_Z[0] + 1) / 2
    pad = math.ceil(ERODE_AMPLITUDE) + 2
    air = set()
    for dz in range(PALACE_Z[0] - pad, PALACE_Z[1] + pad + 1):
        for dx in range(PALACE_X[0] - pad, PALACE_X[1] + pad + 1):
            r = (abs((dx - centre_x) / half_x) ** 3 + abs((dz - centre_z) / half_z) ** 3) ** (1 / 3)
            ceiling = PALACE_FLOOR + PALACE_HEIGHT - 1 + PALACE_DOME * max(0.0, 1 - r * r)
            floor = PALACE_FLOOR + (1 if fractal3(dx / 3.5, 0, dz / 3.5, ERODE_SEED + 99, 2) > 0.5 else 0)
            wall = (r - 1) * min(half_x, half_z)
            for y in range(floor, PALACE_FLOOR + PALACE_HEIGHT + PALACE_DOME + pad + 1):
                n = ERODE_AMPLITUDE * erosion(dx, y, dz)
                if wall + n < 0 and y <= ceiling + 0.9 * n:
                    air.add((dx, y, dz))
    rng = random.Random(ERODE_SEED)
    columns: dict[tuple[int, int], list[int]] = {}
    for dx, y, dz in air:
        columns.setdefault((dx, dz), []).append(y)
    spots = sorted(columns)
    anchor_x, anchor_y, anchor_z = ANCHOR
    for _ in range(STALACTITES):
        dx, dz = spots[rng.randrange(len(spots))]
        top = max(columns[(dx, dz)])
        for y in range(top - rng.randint(1, 4) + 1, top + 1):
            if y > PALACE_FLOOR + 3:
                air.discard((dx, y, dz))
    for _ in range(STALAGMITES):
        dx, dz = spots[rng.randrange(len(spots))]
        if TUNNEL_X[0] <= dx <= TUNNEL_X[1] or abs(dx - anchor_x) + abs(dz - anchor_z) < 3:
            continue
        for y in range(PALACE_FLOOR, PALACE_FLOOR + rng.randint(1, 3)):
            air.discard((dx, y, dz))
    for y in range(anchor_y, anchor_y + 3):
        air.add((anchor_x, y, anchor_z))
    middle = (TUNNEL_X[0] + TUNNEL_X[1]) / 2
    first, last = PALACE_Z[1] + 1, TUNNEL_END - 1
    for dz in range(first, last + 1):
        envelope = math.sin(math.pi * (dz - first) / (last - first))
        shift = round(envelope * TUNNEL_MEANDER * fractal3(dz / 6, 0, 0, ERODE_SEED + 5, 2) * 2)
        for dx in range(TUNNEL_X[0] - 4, TUNNEL_X[1] + 5):
            for y in range(PALACE_FLOOR, PALACE_FLOOR + TUNNEL_HEIGHT + 3):
                across = (dx - middle - shift) / 1.7
                up = (y - PALACE_FLOOR - 1.5) / 2.5
                core = abs(dx - middle - shift) <= 1 and y < PALACE_FLOOR + 3
                if core or across * across + up * up + TUNNEL_ROUGHNESS * envelope * erosion(dx, y, dz) < 1.0:
                    air.add((dx, y, dz))
    for dx in range(TUNNEL_X[0], TUNNEL_X[1] + 1):
        for y in range(PALACE_FLOOR, PALACE_FLOOR + TUNNEL_HEIGHT):
            air.add((dx, y, last))
    return tidy_cave(air)


def tidy_cave(air: set[tuple[int, int, int]]) -> set[tuple[int, int, int]]:
    """Only the air joined to the anchor stays; stone floating inside the cave becomes air."""
    joined = {ANCHOR}
    queue = deque(joined)
    steps = ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))
    while queue:
        x, y, z = queue.popleft()
        for sx, sy, sz in steps:
            cell = (x + sx, y + sy, z + sz)
            if cell in air and cell not in joined:
                joined.add(cell)
                queue.append(cell)
    low = [min(cell[i] for cell in joined) - 1 for i in range(3)]
    high = [max(cell[i] for cell in joined) + 1 for i in range(3)]
    box = [(x, y, z) for x in range(low[0], high[0] + 1) for y in range(low[1], high[1] + 1)
           for z in range(low[2], high[2] + 1)]
    held = {cell for cell in box if cell not in joined and any(cell[i] in (low[i], high[i]) for i in range(3))}
    queue = deque(held)
    while queue:
        x, y, z = queue.popleft()
        for sx, sy, sz in steps:
            cell = (x + sx, y + sy, z + sz)
            if all(low[i] <= cell[i] <= high[i] for i in range(3)) and cell not in joined and cell not in held:
                held.add(cell)
                queue.append(cell)
    return joined | {cell for cell in box if cell not in held}


def cut_palace(grid: Grid) -> dict:
    """The eroded hall and its tunnel; returns where they ended up."""
    air = palace_air()
    reach = {(dx, dz) for dx, _, dz in air}
    for dx in range(PALACE_X[0] - 1, PALACE_X[1] + 2):
        for dz in range(PALACE_Z[0] - 1, TUNNEL_END + 1):
            reach.add((dx, dz))
    for dx, dz in reach:
        column = grid.column(CENTRE + dx, CENTRE + dz)
        top = column.rfind(1)
        column[:top + 1] = b"\x01" * (top + 1)
    for dx, y, dz in air:
        grid.column(CENTRE + dx, CENTRE + dz)[y] = 0
    ceilings: dict[tuple[int, int], int] = {}
    for dx, y, dz in air:
        if dz <= PALACE_Z[1] + 2:
            ceilings[(dx, dz)] = max(y, ceilings.get((dx, dz), y))
    roof = HEIGHT
    for (dx, dz), y in ceilings.items():
        above = grid.column(CENTRE + dx, CENTRE + dz)[y + 1:]
        roof = min(roof, len(above) if above.find(0) < 0 else above.find(0))
    if roof < ROOF:
        raise ShapeError(f"the hall roof is {roof} solid blocks thick, under {ROOF}")
    xs, ys, zs = ([cell[i] for cell in air] for i in range(3))
    return {"hall": f"x {min(xs)}..{max(xs)}, y {min(ys)}..{max(ys)}, z {min(zs)}..{max(zs)}, {len(air)} air",
            "roof": roof, "tunnel_mouth": TUNNEL_END, "anchor": ANCHOR}


def standing(column: bytes) -> list[int]:
    """Heights a player can stand at in a column: on top of each span with two free blocks above, or on the ground."""
    spots = [GROUND] if not any(column[:GROUND + 2]) else []
    for y0, y1 in column_spans(column):
        y = y1 + 1
        if (y + 1 < HEIGHT and not column[y] and not column[y + 1] or y + 1 >= HEIGHT) and y not in spots:
            spots.append(y)
    return spots


def walk(grid: Grid) -> set[tuple[int, int, int]]:
    """Every (x, z, y) reachable on foot from the open ground at the shape's edge: up STEP_UP, down STEP_DOWN.

    Stepping down walks off at the old height first, so the target column must be open from its spot to two blocks
    over the old height."""
    spots = [standing(column) for column in grid.columns]
    seen = set()
    queue = deque()
    for index, column in enumerate(grid.columns):
        x, z = index % WIDTH, index // WIDTH
        if (x in (0, WIDTH - 1) or z in (0, WIDTH - 1)) and not column.count(1):
            seen.add((x, z, GROUND))
            queue.append((x, z, GROUND))
    while queue:
        x, z, y = queue.popleft()
        origin = grid.column(x, z)
        for nx, nz in ((x - 1, z), (x + 1, z), (x, z - 1), (x, z + 1)):
            if not (0 <= nx < WIDTH and 0 <= nz < WIDTH):
                continue
            for ny in spots[nz * WIDTH + nx]:
                if not -STEP_DOWN <= ny - y <= STEP_UP or (nx, nz, ny) in seen:
                    continue
                if ny > y and y + 2 < HEIGHT and origin[y + 2]:
                    continue
                if ny <= y and any(grid.column(nx, nz)[ny:min(y + 2, HEIGHT)]):
                    continue
                seen.add((nx, nz, ny))
                queue.append((nx, nz, ny))
    return seen


def palace(grid: Grid) -> dict:
    cells = trail_cells()
    cut, filled = cut_trail(grid, cells)
    notes = cut_palace(grid)
    dropped = keep_grounded(grid)
    reached = walk(grid)
    summit_x, summit_y, summit_z = SUMMIT
    if (CENTRE + summit_x, CENTRE + summit_z, summit_y) not in reached:
        raise ShapeError(f"no walk reaches the summit {SUMMIT}")
    anchor_x, anchor_y, anchor_z = ANCHOR
    if (CENTRE + anchor_x, CENTRE + anchor_z, anchor_y) not in reached:
        raise ShapeError(f"no walk reaches the palace anchor {ANCHOR}")
    if max(column.rfind(1) for column in grid.columns) + 1 != summit_y:
        raise ShapeError(f"the summit is no longer {summit_y} high")
    return {"trail_columns": len(cells), "trail_cut": cut, "trail_filled": filled, **notes,
            "summit": SUMMIT, "dropped": dropped}


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
            command.add_argument("--peak", type=int, metavar="N",
                                 help="scale the heights so the highest column is N (needs --natural-foot)")
            command.add_argument("--lift", type=float, metavar="L",
                                 help="raise the flanks by up to L blocks at mid-height (needs --natural-foot)")
            command.add_argument("--blunt", type=int, nargs=2, metavar=("KNEE", "CAP"),
                                 help="ease the heights above KNEE towards CAP (needs --natural-foot)")
            command.add_argument("--spike-radius", type=float, nargs=2, metavar=("LO", "HI"), default=SPIKE_RADIUS,
                                 help="flank crystal radius range")
            command.add_argument("--spike-length", type=float, nargs=2, metavar=("LO", "HI"), default=SPIKE_LENGTH,
                                 help="flank crystal length range")
            command.add_argument("--no-summit-spike", action="store_true",
                                 help="no summit crystal; every --spikes is a flank one")
        command.add_argument("--seed", type=int, default=DEFAULT_SEED)
        command.add_argument("--out", type=Path, default=DEFAULT_OUT)
        command.add_argument("--preview", type=Path)
    info = commands.add_parser("info")
    info.add_argument("spans", type=Path, nargs="?", default=DEFAULT_OUT)
    info.add_argument("--preview", type=Path)
    carve = commands.add_parser("palace")
    carve.add_argument("spans", type=Path, nargs="?", default=DEFAULT_OUT)
    carve.add_argument("--out", type=Path, help="the file to write (default: back into SPANS)")
    carve.add_argument("--preview", type=Path)
    arguments = parser.parse_args(argv)
    try:
        if arguments.command == "info":
            width, depth, height, spans = decode(arguments.spans.read_bytes())
            if arguments.preview:
                arguments.preview.write_bytes(preview(spans))
            report({"file": str(arguments.spans), "size": f"{width} x {height} x {depth}", **describe(spans)})
            return 0
        if arguments.command == "palace":
            grid = grid_from_spans(decode(arguments.spans.read_bytes())[3])
            notes = palace(grid)
            report({**notes, **write(grid, arguments.out or arguments.spans, arguments.preview)})
            return 0
        if arguments.command == "placeholder":
            grid, notes = placeholder(arguments.seed, arguments.style, arguments.spikes)
        else:
            ignored = DEFAULT_IGNORED + tuple(block_name(name) if "*" not in name else name
                                              for name in arguments.ignore)
            schematic = read_schematic(arguments.reference.read_bytes(), ignored)
            grid, notes = convert(schematic, arguments.seed, arguments.spikes, arguments.keep_aspect,
                                  arguments.trim_bottom, arguments.natural_foot, arguments.peak, arguments.lift,
                                  arguments.blunt and tuple(arguments.blunt), tuple(arguments.spike_radius),
                                  tuple(arguments.spike_length), not arguments.no_summit_spike)
        report({"seed": arguments.seed, **notes, **write(grid, arguments.out, arguments.preview)})
        return 0
    except (OSError, ShapeError, KeyError) as error:
        print(f"soulsunder_shape: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
