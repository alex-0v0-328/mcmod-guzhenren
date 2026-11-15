"""Every exported model against its Blockbench source and the authority's own JSON exports."""
from __future__ import annotations

import contextlib
import io
import json
import struct
import unittest
import zlib
from pathlib import Path

import bbmodel_to_geckolib as exporter


OUTPUT_ROOT = Path(__file__).resolve().parents[1]
MIRRORED_RIGHT_LEGS = ["ear_right", "front_leg_right", "front_leg_right_shin", "front_leg_right_paw",
                       "back_leg_right", "back_leg_right_shin", "back_leg_right_paw"]


def load_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def normalize_animation(animation: dict) -> dict:
    """Keep only the fields the exporter emits; Blockbench writes a lone keyframe as a bare {"vector": ...},
    which the exporter keys by time."""
    bones = {bone: {channel: {"0.0": frames} if "vector" in frames else frames for channel, frames in channels.items()}
             for bone, channels in animation.get("bones", {}).items()}
    return {"bones": bones, **{key: animation[key] for key in ("loop", "animation_length") if key in animation}}


def rgba_pixels(path: Path) -> list[tuple[int, int, int, int]]:
    """Decodes an 8-bit RGBA, non-interlaced PNG without third-party imaging libraries."""
    data = path.read_bytes()
    width, height, depth, color_type, _, _, interlace = struct.unpack(">IIBBBBB", data[16:29])
    assert depth == 8 and color_type == 6 and interlace == 0, f"{path.name} is not 8-bit RGBA"
    position, compressed = 8, b""
    while position < len(data):
        length = struct.unpack(">I", data[position:position + 4])[0]
        if data[position + 4:position + 8] == b"IDAT":
            compressed += data[position + 8:position + 8 + length]
        position += 12 + length
    raw = zlib.decompress(compressed)
    stride = width * 4
    previous = bytearray(stride)
    pixels: list[tuple[int, int, int, int]] = []
    offset = 0
    for _ in range(height):
        kind = raw[offset]
        line = bytearray(raw[offset + 1:offset + 1 + stride])
        offset += 1 + stride
        for index in range(stride):
            left = line[index - 4] if index >= 4 else 0
            up = previous[index]
            corner = previous[index - 4] if index >= 4 else 0
            if kind == 1:
                line[index] = (line[index] + left) & 0xFF
            elif kind == 2:
                line[index] = (line[index] + up) & 0xFF
            elif kind == 3:
                line[index] = (line[index] + (left + up) // 2) & 0xFF
            elif kind == 4:
                guess = left + up - corner
                pick = min((abs(guess - left), 0, left), (abs(guess - up), 1, up), (abs(guess - corner), 2, corner))
                line[index] = (line[index] + pick[2]) & 0xFF
        pixels.extend(tuple(line[index:index + 4]) for index in range(0, stride, 4))
        previous = line
    return pixels


class ExportedModelTests(unittest.TestCase):
    """The checks every model passes: validate_artifacts, the witnesses, the UV grid and an untouched source."""

    outputs: dict[str, tuple[dict, dict, Path]] = {}

    @classmethod
    def setUpClass(cls):
        for name, spec in exporter.MODELS.items():
            before = exporter.source_hashes(spec, spec.source_model)
            summary = exporter.validate_artifacts(spec, spec.source_model, OUTPUT_ROOT, before)
            assert summary == {"bones": spec.bones, "cubes": spec.cubes, "animations": spec.animations,
                               "keyframes": spec.keyframes, "textures": len(spec.texture_map)}, (name, summary)
            assert exporter.source_hashes(spec, spec.source_model) == before, f"{name}: source changed"
            geo_path, animation_path, texture_root = exporter.output_paths(spec, OUTPUT_ROOT)
            cls.outputs[name] = (load_json(geo_path)["minecraft:geometry"][0],
                                 load_json(animation_path)["animations"], texture_root)

    def bones(self, name):
        return {bone["name"]: bone for bone in self.outputs[name][0]["bones"]}

    def animations(self, name):
        return self.outputs[name][1]

    def test_every_model_matches_the_authoritys_own_exports(self):
        for name, spec in exporter.MODELS.items():
            with self.subTest(name):
                geometry, animations, _ = self.outputs[name]
                witness = load_json(spec.witness_animations)["animations"]
                self.assertEqual({key: normalize_animation(value) for key, value in witness.items()},
                                 {key: normalize_animation(value) for key, value in animations.items()})
                if spec.witness_geometry:
                    self.assertEqual(load_json(spec.witness_geometry)["minecraft:geometry"][0], geometry)

    def test_every_uv_sits_on_the_pixel_grid_inside_the_texture(self):
        for name, spec in exporter.MODELS.items():
            geometry, _, texture_root = self.outputs[name]
            self.assertEqual(geometry["description"]["texture_width"], spec.texture_size, name)
            for target in spec.texture_map.values():
                self.assertEqual(exporter.png_dimensions(texture_root / target), (spec.texture_size,) * 2, target)
            for bone in geometry["bones"]:
                for cube in bone.get("cubes", []):
                    uv = cube["uv"]
                    faces = [uv] if isinstance(uv, list) else [
                        [face["uv"][0], face["uv"][0] + face["uv_size"][0],
                         face["uv"][1], face["uv"][1] + face["uv_size"][1]] for face in uv.values()]
                    for corners in faces:
                        self.assertTrue(all(float(value).is_integer() and 0 <= value <= spec.texture_size
                                            for value in corners), f"{name}.{bone['name']}: {corners}")

    def test_bear_and_tiger_mirror_only_their_right_legs(self):
        for name in ("bear", "tiger"):
            bones = self.bones(name)
            self.assertEqual(list(bones)[:2], ["root", "body"])
            self.assertEqual(bones["body"]["parent"], "root")
            for right in MIRRORED_RIGHT_LEGS:
                left = right.replace("right", "left")
                self.assertEqual(len(bones[right]["cubes"]), 1)
                self.assertIs(bones[right]["cubes"][0].get("mirror"), True, f"{name}.{right}")
                self.assertNotIn("mirror", bones[left]["cubes"][0], f"{name}.{left}")
        self.assertEqual(set(self.animations("bear")), {f"animation.{name}" for name in (
            "idle", "walk", "run", "attack_swipe", "attack_rear", "roar", "sit", "lie_down", "lie", "sleep",
            "get_up", "roll", "back_scratch", "hurt_left", "hurt_right", "death", "charge", "appear")})
        self.assertEqual(set(self.animations("tiger")), {f"animation.{name}" for name in (
            "idle", "walk", "run", "attack_pounce", "attack_swipe", "roar", "lie_down", "lie", "sleep", "get_up",
            "sit", "hurt_left", "hurt_right", "death", "charge", "appear")})

    def test_boar_gu_converts_pivots_and_rotations(self):
        bones = self.bones("boar_gu")
        self.assertEqual(list(bones), ["root", "body", "head", "antenna_left", "antenna_right", "shell_left",
                                       "shell_right"])
        self.assertEqual(bones["antenna_left"]["pivot"], [0.5, 1, -1.25])
        self.assertEqual((bones["shell_left"]["parent"], bones["antenna_left"]["parent"]), ("body", "head"))
        self.assertFalse(any("rotation" in bone for bone in bones.values()))
        animations = self.animations("boar_gu")
        self.assertEqual(set(animations), {"animation.idle", "animation.lift", "animation.fly", "animation.land"})
        self.assertIs(animations["animation.fly"]["loop"], True)
        self.assertNotIn("loop", animations["animation.land"])
        self.assertEqual(animations["animation.fly"]["bones"]["shell_left"]["rotation"]["0.0"]["vector"], [45, 25, 0])

    def test_rhinoceros_beetle_keeps_rest_rotations_and_loop_modes(self):
        bones = self.bones("rhinoceros_beetle")
        self.assertEqual(bones["elytron_left"]["rotation"], [25, 9, 5])
        self.assertEqual(bones["elytron_right"]["rotation"], [25, -9, -5])
        self.assertTrue(all(isinstance(cube["uv"], dict) for bone in bones.values() for cube in bone.get("cubes", [])))
        animations = self.animations("rhinoceros_beetle")
        self.assertIs(animations["animation.hover"]["loop"], True)
        self.assertNotIn("loop", animations["animation.hurt"])
        self.assertEqual(animations["animation.death"]["loop"], "hold_on_last_frame")
        self.assertEqual(animations["animation.death"]["animation_length"], 1.2)

    def test_human_soul_glows_at_exactly_its_four_eye_pixels(self):
        geometry, animations, texture_root = self.outputs["human_soul"]
        cubes = [cube for bone in geometry["bones"] for cube in bone.get("cubes", [])]
        self.assertEqual(sum(isinstance(cube["uv"], list) for cube in cubes), 14, "inner box-UV cubes")
        self.assertEqual(sum(isinstance(cube["uv"], dict) for cube in cubes), 14, "outer per-face-UV cubes")
        self.assertEqual(animations["animation.disappear"]["loop"], "hold_on_last_frame")
        for name in ("animation.beckon", "animation.cross_arms_nod", "animation.hands_hips_shake"):
            self.assertNotIn("root", animations[name]["bones"], f"{name} keys the root the movement controller owns")
        blue = rgba_pixels(texture_root / "human_soul_blue.png")
        glow = rgba_pixels(texture_root / "human_soul_glowmask.png")
        eyes = [11 * 128 + 74, 11 * 128 + 77, 12 * 128 + 74, 12 * 128 + 77]
        self.assertEqual([index for index, pixel in enumerate(glow) if pixel != (0, 0, 0, 0)], eyes)
        self.assertTrue(all(glow[index] == blue[index] == (255, 255, 255, 255) for index in eyes))

    def test_tortoise_phantom_is_translucent_but_its_four_eye_pixels(self):
        bones = self.bones("tortoise")
        self.assertEqual(list(bones), ["root", "body", "neck", "head", "front_leg_left", "back_leg_left",
                                       "front_leg_right", "back_leg_right"])
        self.assertEqual((bones["neck"]["parent"], bones["head"]["parent"]), ("body", "neck"))
        for right in ("front_leg_right", "back_leg_right"):
            left = right.replace("right", "left")
            self.assertIs(bones[right]["cubes"][0].get("mirror"), True, right)
            self.assertNotIn("mirror", bones[left]["cubes"][0], left)
        animations = self.animations("tortoise")
        self.assertEqual(list(animations), [f"animation.{name}" for name in (
            "idle", "walk", "sleep", "death", "hurt", "eat", "charge")])
        self.assertIs(animations["animation.charge"]["loop"], True)
        self.assertEqual(animations["animation.death"]["loop"], "hold_on_last_frame")
        self.assertEqual(animations["animation.death"]["animation_length"], 1.6)
        texture_root = self.outputs["tortoise"][2]
        phantom = rgba_pixels(texture_root / "tortoise_phantom.png")
        self.assertEqual(phantom, rgba_pixels(texture_root / "tortoise_phantom_glowmask.png"))
        eyes = [21 * 128 + 75, 21 * 128 + 76, 21 * 128 + 86, 21 * 128 + 87]
        self.assertEqual([index for index, pixel in enumerate(phantom) if pixel[3] == 255], eyes)
        self.assertTrue(all(phantom[index] == (255, 255, 255, 255) for index in eyes))
        self.assertEqual({pixel[3] for pixel in phantom}, {0, 104, 255})

    def test_buffalo_keeps_its_jointed_legs_the_charger_clip_timing_and_a_translucent_phantom(self):
        bones = self.bones("buffalo")
        self.assertEqual(list(bones), ["root", "body", "neck", "head", "horn_left", "horn_right", "ear_left",
                                       "ear_right", "front_leg_left", "front_leg_left_shin", "front_leg_right",
                                       "front_leg_right_shin", "back_leg_left", "back_leg_left_shin", "back_leg_right",
                                       "back_leg_right_shin", "tail", "tail_tuft"])
        for shin in ("front_leg_left_shin", "front_leg_right_shin", "back_leg_left_shin", "back_leg_right_shin"):
            self.assertEqual(bones[shin]["parent"], shin.removesuffix("_shin"), shin)
        self.assertEqual((bones["horn_left"]["parent"], bones["tail_tuft"]["parent"]), ("head", "tail"))
        for right in ("horn_right", "ear_right", "front_leg_right", "back_leg_right"):
            self.assertIs(bones[right]["cubes"][0].get("mirror"), True, right)
        animations = self.animations("buffalo")
        self.assertEqual(list(animations), [f"animation.{name}" for name in (
            "idle", "walk", "run", "graze", "alert", "attack_charge", "attack_toss", "hurt", "death", "lie_down",
            "lie", "get_up", "charge", "appear")])
        self.assertEqual(animations["animation.attack_charge"]["animation_length"], 1.2)
        self.assertEqual(animations["animation.attack_toss"]["animation_length"], 0.8)
        self.assertEqual(animations["animation.death"]["loop"], "hold_on_last_frame")
        self.assertEqual(animations["animation.death"]["animation_length"], 1.6)
        self.assertIs(animations["animation.charge"]["loop"], True)
        self.assertEqual(animations["animation.appear"]["animation_length"], 1.0)
        self.assertNotIn("loop", animations["animation.appear"])
        texture_root = self.outputs["buffalo"][2]
        for name in ("yellow_ox", "water_buffalo", "azure_ox"):
            self.assertEqual({pixel[3] for pixel in rgba_pixels(texture_root / f"{name}.png")}, {0, 255}, name)
        phantom = rgba_pixels(texture_root / "buffalo_phantom.png")
        self.assertEqual(phantom, rgba_pixels(texture_root / "buffalo_phantom_glowmask.png"))
        eyes = [41 * 128 + 8, 41 * 128 + 9, 41 * 128 + 17, 41 * 128 + 18]
        self.assertEqual([index for index, pixel in enumerate(phantom) if pixel[3] == 255], eyes)
        self.assertTrue(all(phantom[index] == (255, 255, 255, 255) for index in eyes))
        self.assertEqual({pixel[3] for pixel in phantom}, {0, 104, 255})

    def test_elephant_keeps_its_jointed_trunk_and_legs_and_the_charger_clip_timing(self):
        bones = self.bones("elephant")
        self.assertEqual(list(bones), ["root", "body", "head", "trunk", "trunk_tip", "ear_left", "ear_right",
                                       "tusk_left", "tusk_right", "front_leg_left", "front_leg_left_shin",
                                       "front_leg_right", "front_leg_right_shin", "back_leg_left",
                                       "back_leg_left_shin", "back_leg_right", "back_leg_right_shin", "tail",
                                       "tail_tip"])
        self.assertEqual(sum(len(bone.get("cubes", [])) for bone in bones.values()), 21)
        for shin in ("front_leg_left_shin", "front_leg_right_shin", "back_leg_left_shin", "back_leg_right_shin"):
            self.assertEqual(bones[shin]["parent"], shin.removesuffix("_shin"), shin)
        self.assertEqual((bones["trunk"]["parent"], bones["trunk_tip"]["parent"], bones["tail_tip"]["parent"]),
                         ("head", "trunk", "tail"))
        for right in ("ear_right", "tusk_right", "front_leg_right", "back_leg_right"):
            self.assertIs(bones[right]["cubes"][0].get("mirror"), True, right)
        animations = self.animations("elephant")
        self.assertEqual(list(animations), [f"animation.{name}" for name in (
            "idle", "walk", "run", "graze", "alert", "attack_charge", "attack_toss", "attack_stomp", "attack_trunk",
            "hurt", "death", "lie_down", "lie", "get_up", "charge")])
        self.assertEqual(animations["animation.attack_charge"]["animation_length"], 1.2)
        self.assertEqual(animations["animation.attack_toss"]["animation_length"], 0.8)
        self.assertEqual(animations["animation.attack_stomp"]["animation_length"], 1.2)
        self.assertEqual(animations["animation.attack_trunk"]["animation_length"], 1.0)
        self.assertEqual(animations["animation.death"]["loop"], "hold_on_last_frame")
        self.assertEqual(animations["animation.death"]["animation_length"], 1.6)
        texture_root = self.outputs["elephant"][2]
        for name in ("elephant", "white_elephant"):
            self.assertEqual({pixel[3] for pixel in rgba_pixels(texture_root / f"{name}.png")}, {0, 255}, name)


class CommandLineTests(unittest.TestCase):

    def test_models_are_named_or_all(self):
        for argv in (["validate"], ["validate", "bear", "--all"], ["validate", "wolf"],
                     ["export", "bear", "tiger", "--source", "x.bbmodel"]):
            with self.subTest(argv), self.assertRaises(SystemExit), contextlib.redirect_stderr(io.StringIO()):
                exporter.main(argv)


if __name__ == "__main__":
    unittest.main()
