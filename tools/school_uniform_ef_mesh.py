"""The School Uniform [学堂制服] Epic Fight skinned meshes, built from the Blockbench project's OBJ export.

    python -B tools/school_uniform_ef_mesh.py UNIFORM.obj [--out DIR] [--hem-full-z PX]

Epic Fight reads assets/<ns>/animmodels/armor/<item id>.json before it falls back to converting the GeckoLib cubes,
and that conversion hangs the whole tunic on Torso and keeps the hem rigid, so the thighs pass through it. This tool
writes school_uniform_chestplate.json, school_uniform_leggings.json and school_uniform_boots.json into --out (the mod's
animmodels/armor folder by default) in the layout of Epic Fight's own animmodels/armor/chestplate.json.

The input is Blockbench's native OBJ export of Assets/blockbench-gzr/armor/geckolib-school-uniform (File > Export >
OBJ Model, or Codecs.obj.compile() over the Blockbench MCP): block units, Blockbench space, one `o` per exported cube,
named as in the project. Geometry and UV stay those of the cubes, so the meshes share the GeckoLib textures; rerun the
tool whenever a cube, its UV or the texture layout changes. Output is in Epic Fight's Blender space (Z up, +Y front,
-X left, block units).

Weights depend on the position alone, so overlapping layers bend alike and never cross:
    tunic, belt, hem top   Epic Fight's skin jacket split from Torso up to Chest over the tunic's height
    hem below the tunic    Torso fades into the thighs, fully thigh at --hem-full-z (10.0 px, Alex's pick);
                           x <= -1 px Thigh_L, x >= 1 px Thigh_R, the seam between them blended
    sleeves                as the skin's arms: Arm above 18.03 px, the 18.0 ring Elbow behind, Hand below 17.98
    trousers, puttees      as the skin's legs: Thigh above 6.03 px, the 6.0 ring Knee in front, Leg below 5.98
    shoes                  Leg

Each sleeve comes twice, as part <side>Arm for 4 px skin arms and <side>ArmSlim for 3 px ones; the mod hides the
one that does not match the wearer's skin before Epic Fight draws the mesh.
"""
from __future__ import annotations

import argparse
import json
import os
import sys

JOINTS = ['Root', 'Thigh_R', 'Leg_R', 'Knee_R', 'Thigh_L', 'Leg_L', 'Knee_L', 'Torso', 'Chest', 'Head',
          'Shoulder_R', 'Arm_R', 'Hand_R', 'Tool_R', 'Elbow_R', 'Shoulder_L', 'Arm_L', 'Hand_L', 'Tool_L', 'Elbow_L']
JOINT_INDEX = {name: i for i, name in enumerate(JOINTS)}
DEFAULT_OUT = os.path.join('src', 'main', 'resources', 'assets', 'guzhenren', 'animmodels', 'armor')

# Pixels, Epic Fight space (z up).
BODY_BOTTOM, BODY_TOP = 11.45, 24.55  # body_tunic's edges with its 0.55 inflate
JACKET_ROWS = [(0 / 6, 1.0), (1 / 6, 0.75), (2 / 6, 0.75), (3 / 6, 0.5), (4 / 6, 0.26), (5 / 6, 0.23), (6 / 6, 0.0)]
HEM_FULL_Z = 10.0
HEM_ROWS = [10.5, 9.75, 9.0, 8.0]
BAND = 1.0  # half width of the seam blend
ELBOW = (18.03, 18.0, 17.98)
KNEE = (6.03, 6.0, 5.98)

SLOTS = {
    'school_uniform_chestplate': {
        'torso': ['body_tunic', 'body_belt', 'body_hem_front', 'body_hem_back'],
        'leftArm': ['left_sleeve_front', 'left_sleeve_back', 'left_inner_sleeve', 'left_sleeve_bandage'],
        'leftArmSlim': ['left_sleeve_front_slim', 'left_sleeve_back_slim', 'left_inner_sleeve_slim',
                        'left_sleeve_bandage_slim'],
        'rightArm': ['right_sleeve_front', 'right_sleeve_back', 'right_inner_sleeve', 'right_sleeve_bandage'],
        'rightArmSlim': ['right_sleeve_front_slim', 'right_sleeve_back_slim', 'right_inner_sleeve_slim',
                         'right_sleeve_bandage_slim'],
    },
    'school_uniform_leggings': {
        'noGroups': ['left_leg_pants', 'left_leg_puttee', 'right_leg_pants', 'right_leg_puttee'],
    },
    'school_uniform_boots': {
        'noGroups': ['left_boot_shoe', 'right_boot_shoe'],
    },
}


def region_of(name: str) -> str:
    """body, or arm_/leg_/boot_ with the side letter L or R."""
    if name.startswith('body_'):
        return 'body'
    side = 'L' if name.startswith('left_') else 'R'
    if 'sleeve' in name:
        return 'arm_' + side
    if 'boot' in name:
        return 'boot_' + side
    return 'leg_' + side


def parse_obj(path: str) -> dict[str, list]:
    """Each object's faces as lists of (position, uv with v flipped down, normal)."""
    vs, vts, vns, objs, cur = [], [], [], {}, None
    with open(path, encoding='utf-8') as file:
        for line in file:
            p = line.split()
            if not p:
                continue
            if p[0] == 'o':
                cur = p[1]
                objs[cur] = []
            elif p[0] == 'v':
                vs.append(tuple(map(float, p[1:4])))
            elif p[0] == 'vt':
                vts.append((float(p[1]), 1.0 - float(p[2])))
            elif p[0] == 'vn':
                vns.append(tuple(map(float, p[1:4])))
            elif p[0] == 'f':
                poly = []
                for tok in p[1:]:
                    a, b, c = tok.split('/')
                    poly.append((vs[int(a) - 1], vts[int(b) - 1], vns[int(c) - 1]))
                objs[cur].append(poly)
    return objs


def to_ef(v: tuple) -> tuple:
    """Blockbench (x, y up, -z front) to Epic Fight Blender space (x, +y front, z up)."""
    return v[0], -v[2], v[1]


def lerp(a: tuple, b: tuple, t: float) -> tuple:
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(len(a)))


def split(poly: list, axis: int, c: float) -> tuple[list, list]:
    """A convex polygon of (position, uv) cut by the plane axis = c into (below, above); a side under 3 corners is []."""
    below, above = [], []
    n = len(poly)
    for i in range(n):
        p, q = poly[i], poly[(i + 1) % n]
        dp, dq = p[0][axis] - c, q[0][axis] - c
        (below if dp < 0 else above).append(p)
        if (dp < 0) != (dq < 0) and abs(dp - dq) > 1e-12:
            t = dp / (dp - dq)
            x = (lerp(p[0], q[0], t), lerp(p[1], q[1], t))
            below.append(x)
            above.append(x)
    return (below if len(below) >= 3 else []), (above if len(above) >= 3 else [])


def cut_all(polys: list, axis: int, cuts: list[float]) -> list:
    for c in cuts:
        out = []
        for poly in polys:
            out += [piece for piece in split(poly, axis, c) if piece]
        polys = out
    return polys


def jacket_weights(z: float) -> dict[str, float]:
    f = min(max((z - BODY_BOTTOM) / (BODY_TOP - BODY_BOTTOM), 0.0), 1.0)
    for (f0, t0), (f1, t1) in zip(JACKET_ROWS, JACKET_ROWS[1:]):
        if f <= f1:
            t = t0 + (t1 - t0) * ((f - f0) / (f1 - f0) if f1 > f0 else 0)
            return {'Torso': t, 'Chest': 1 - t}
    return {'Chest': 1.0}


def body_weights(x: float, z: float, hem_full_z: float) -> dict[str, float]:
    w = jacket_weights(z)
    if z < BODY_BOTTOM:
        thigh = min((BODY_BOTTOM - z) / (BODY_BOTTOM - hem_full_z), 1.0)
        left = min(max((BAND - x) / (2 * BAND), 0.0), 1.0)
        w = {k: v * (1 - thigh) for k, v in w.items()}
        w['Thigh_L'] = thigh * left
        w['Thigh_R'] = thigh * (1 - left)
    return w


def limb_weights(y: float, z: float, side: str, kind: str) -> dict[str, float]:
    upper, mid, lower = ('Arm_', 'Elbow_', 'Hand_') if kind == 'arm' else ('Thigh_', 'Knee_', 'Leg_')
    hi, ring, lo = ELBOW if kind == 'arm' else KNEE
    if z >= (hi + ring) / 2:
        return {upper + side: 1.0}
    if z <= (ring + lo) / 2:
        return {lower + side: 1.0}
    back = y < 0
    stretch_side = back if kind == 'arm' else not back  # an elbow stretches behind, a knee in front
    return {(mid if stretch_side else upper) + side: 1.0}


def weights_for(region: str, pos_px: tuple, hem_full_z: float) -> dict[str, float]:
    """Joint shares of a vertex in pixels: at most three joints (Epic Fight's cap), summing to 1."""
    x, y, z = pos_px
    if region == 'body':
        w = body_weights(x, z, hem_full_z)
    elif region.startswith('arm_'):
        w = limb_weights(y, z, region[-1], 'arm')
    elif region.startswith('leg_'):
        w = limb_weights(y, z, region[-1], 'leg')
    else:
        w = {'Leg_' + region[-1]: 1.0}
    w = {k: v for k, v in w.items() if v > 1e-4}
    if len(w) > 3:
        w = dict(sorted(w.items(), key=lambda kv: -kv[1])[:3])
    s = sum(w.values())
    return {k: round(v / s, 4) for k, v in w.items()}


def cuts_for(region: str, hem_full_z: float) -> dict[int, list[float]]:
    """Planes per axis (0 = x, 2 = z) that add rows and columns, so the weights bend along them."""
    if region == 'body':
        rows = [BODY_BOTTOM + f * (BODY_TOP - BODY_BOTTOM) for f, _ in JACKET_ROWS[1:-1]]
        return {2: sorted(set(rows + HEM_ROWS + [hem_full_z])), 0: [-BAND, 0.0, BAND]}
    if region.startswith('arm_'):
        return {2: list(ELBOW)}
    if region.startswith('leg_'):
        return {2: list(KNEE)}
    return {}


def area_ok(pos: list, a: int, b: int, c: int, n: tuple) -> bool:
    """False for a zero-area triangle; raises when the cut flipped the OBJ's counter-clockwise winding."""
    A, B, C = (pos[i][0] for i in (a, b, c))
    u = [B[i] - A[i] for i in range(3)]
    v = [C[i] - A[i] for i in range(3)]
    cross = (u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0])
    d = sum(cross[i] * n[i] for i in range(3))
    if d < 0:
        raise ValueError('winding flipped')
    return d > 1e-12


def build(objs: dict[str, list], groups: dict[str, list[str]], hem_full_z: float) -> tuple[list, list, list, dict]:
    """(positions with weights, uvs, normals, triangle corners per part) for one armour piece."""
    pos, uvs, nrm = [], [], []
    pkey, ukey, nkey = {}, {}, {}
    parts = {}

    def idx(table, keymap, key, val):
        if key not in keymap:
            keymap[key] = len(table)
            table.append(val)
        return keymap[key]

    for part, names in groups.items():
        tri = []
        for name in names:
            if name not in objs:
                raise ValueError(f'the OBJ has no object {name}')
            region = region_of(name)
            cuts = cuts_for(region, hem_full_z)
            for quad in objs[name]:
                n = to_ef(quad[0][2])
                poly = [(tuple(c * 16 for c in to_ef(p)), uv) for p, uv, _ in quad]
                pieces = [poly]
                for axis in (2, 0):
                    if axis in cuts:
                        pieces = cut_all(pieces, axis, cuts[axis])
                ni = idx(nrm, nkey, tuple(round(c, 6) for c in n), n)
                for piece in pieces:
                    ids = []
                    for p_px, uv in piece:
                        w = weights_for(region, p_px, hem_full_z)
                        pk = (tuple(round(c, 4) for c in p_px), tuple(sorted(w.items())))
                        pi = idx(pos, pkey, pk, (tuple(c / 16 for c in p_px), w))
                        ui = idx(uvs, ukey, tuple(round(c, 6) for c in uv), uv)
                        ids.append((pi, ui, ni))
                    for k in range(1, len(ids) - 1):  # fan of a convex piece keeps the OBJ's winding
                        a, b, c = ids[0], ids[k], ids[k + 1]
                        if len({a[0], b[0], c[0]}) == 3 and area_ok(pos, a[0], b[0], c[0], n):
                            tri += [a, b, c]
        parts[part] = tri
    return pos, uvs, nrm, parts


def to_json(pos: list, uvs: list, nrm: list, parts: dict) -> dict:
    weights, wkey, vcounts, vindices = [], {}, [], []
    for _, w in pos:
        vcounts.append(len(w))
        for j, v in sorted(w.items(), key=lambda kv: -kv[1]):
            if v not in wkey:
                wkey[v] = len(weights)
                weights.append(v)
            vindices += [JOINT_INDEX[j], wkey[v]]

    def rounded(values):
        return [round(x, 6) for x in values]

    def flat(rows):
        return [c for row in rows for c in row]

    return {'vertices': {
        'positions': {'stride': 3, 'count': len(pos), 'array': rounded(flat(p for p, _ in pos))},
        'uvs': {'stride': 2, 'count': len(uvs), 'array': rounded(flat(uvs))},
        'normals': {'stride': 3, 'count': len(nrm), 'array': rounded(flat(nrm))},
        'vcounts': {'stride': 1, 'count': len(vcounts), 'array': vcounts},
        'weights': {'stride': 1, 'count': len(weights), 'array': weights},
        'vindices': {'stride': 1, 'count': len(vindices), 'array': vindices},
        'parts': {k: {'stride': 3, 'count': len(t), 'array': flat(t)} for k, t in parts.items()},
    }}


def write_meshes(obj_path: str, out_dir: str, hem_full_z: float = HEM_FULL_Z) -> dict[str, str]:
    """Writes the three meshes; returns each item id's file."""
    objs = parse_obj(obj_path)
    os.makedirs(out_dir, exist_ok=True)
    written = {}
    for item, groups in SLOTS.items():
        pos, uvs, nrm, parts = build(objs, groups, hem_full_z)
        path = os.path.join(out_dir, item + '.json')
        with open(path, 'w', encoding='utf-8', newline='\n') as file:
            json.dump(to_json(pos, uvs, nrm, parts), file, separators=(',', ':'))
        written[item] = path
        tris = {k: len(t) // 3 for k, t in parts.items()}
        print(f'{item}: {len(pos)} vertices, triangles {tris} -> {path}')
    return written


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('obj', help="Blockbench's OBJ export of the School Uniform project")
    parser.add_argument('--out', default=DEFAULT_OUT, help='folder for the three meshes (default: %(default)s)')
    parser.add_argument('--hem-full-z', type=float, default=HEM_FULL_Z,
                        help='height in pixels at and below which the hem follows the thighs alone (default: 10.0)')
    args = parser.parse_args(argv)
    write_meshes(args.obj, args.out, args.hem_full_z)
    return 0


if __name__ == '__main__':
    sys.exit(main())
