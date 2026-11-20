import argparse
import os
import struct
import sys
import time
import unittest
import zlib
from pathlib import Path
from unittest.mock import patch

import game_window as gw
import local_checks as check
import project_runtime as env


def png_parts(data):
    """(width, height, raw filtered rows) of a PNG written by gw.png_bytes, checking every chunk's CRC."""
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    position, chunks = 8, {}
    while position < len(data):
        length, kind = struct.unpack('>I4s', data[position:position + 8])
        body = data[position + 8:position + 8 + length]
        crc = struct.unpack('>I', data[position + 8 + length:position + 12 + length])[0]
        assert crc == zlib.crc32(kind + body), f'bad CRC in {kind}'
        chunks[kind] = chunks.get(kind, b'') + body
        position += 12 + length
    width, height, depth, colour, *_ = struct.unpack('>IIBBBBB', chunks[b'IHDR'])
    assert (depth, colour) == (8, 2) and b'IEND' in chunks
    return width, height, zlib.decompress(chunks[b'IDAT'])


class PixelTests(unittest.TestCase):
    def test_png_holds_the_rows_unfiltered(self):
        rgb = bytes(range(2 * 3 * 3))
        width, height, raw = png_parts(gw.png_bytes(3, 2, rgb))
        self.assertEqual((width, height), (3, 2))
        self.assertEqual(raw, b'\x00' + rgb[:9] + b'\x00' + rgb[9:])

    def test_bgra_becomes_rgb(self):
        self.assertEqual(gw.bgra_to_rgb(bytes([1, 2, 3, 255, 4, 5, 6, 0])), bytes([3, 2, 1, 6, 5, 4]))

    def test_a_flat_frame_is_blank(self):
        self.assertTrue(gw.is_blank(bytes(30)))
        self.assertTrue(gw.is_blank(b''))
        self.assertFalse(gw.is_blank(bytes(27) + b'\x01\x00\x00'))

    def test_loading_share_counts_the_overlay_red(self):
        self.assertEqual(gw.loading_share(gw.MOJANG_RED * 4), 1.0)
        self.assertEqual(gw.loading_share(gw.MOJANG_RED * 2 + bytes(6)), 0.5)


class InputTests(unittest.TestCase):
    def test_key_names(self):
        self.assertEqual(gw.parse_key('a'), (0x41, False))
        self.assertEqual(gw.parse_key('7'), (0x37, False))
        self.assertEqual(gw.parse_key('f3'), (0x72, False))
        self.assertEqual(gw.parse_key('F24'), (0x87, False))
        self.assertEqual(gw.parse_key('Esc'), (0x1B, False))
        self.assertEqual(gw.parse_key('up'), (0x26, True))
        for unknown in ('F25', 'F0', 'é', 'hyper'):
            with self.assertRaises(ValueError, msg=unknown):
                gw.parse_key(unknown)

    def test_chord_keeps_the_press_order(self):
        self.assertEqual(gw.parse_chord('F3+B'), [(0x72, False), (0x42, False)])

    def test_key_lparam_carries_scan_code_and_release_bits(self):
        self.assertEqual(gw.key_lparam(0x3D, False, up=False), 0x003D0001)
        self.assertEqual(gw.key_lparam(0x3D, False, up=True), 0xC03D0001)
        self.assertEqual(gw.key_lparam(0x48, True, up=False), 0x01480001)


class TrackingTests(unittest.TestCase):
    def test_position_replies(self):
        self.assertEqual(gw.parse_pos('Dev has the following entity data: [1.5d, 64.0d, -3.25d]'), (1.5, 64.0, -3.25))
        self.assertEqual(gw.parse_pos('5f0c-11 has the following entity data: [-0.5d, 1.0E-4d, 2.0d]'),
                         (-0.5, 0.0001, 2.0))
        self.assertIsNone(gw.parse_pos('No entity was found'))
        self.assertIsNone(gw.parse_pos(None))

    def test_follow_command_places_feet_and_aims_above_the_target(self):
        self.assertEqual(gw.follow_command('Dev', (1.5, 64.0, -3.25), (0, 1, 4), 0.5),
                         'tp Dev 1.5 65 0.75 facing 1.5 64.5 -3.25')

    def test_size(self):
        self.assertEqual(gw.parse_size('1280x720'), (1280, 720))
        for bad in ('1280', '10x10', 'axb'):
            with self.assertRaises(argparse.ArgumentTypeError, msg=bad):
                gw.parse_size(bad)


class TitleTests(unittest.TestCase):
    def test_title_shoots_the_client_stage_window_once_loaded(self):
        calls = []

        def stage_client(ctx, on_ready=None):
            on_ready([42])
            return check.Result('PASS', 1.0, 'title screen reached')
        with patch.object(check, 'stage_client', side_effect=stage_client), \
             patch.object(gw, 'game_window', side_effect=lambda pids, timeout: calls.append(('window', pids)) or 7), \
             patch.object(gw, 'resize', side_effect=lambda hwnd, w, h: calls.append(('resize', hwnd, w, h))), \
             patch.object(gw, 'wait_loaded', side_effect=lambda hwnd: calls.append(('loaded', hwnd))), \
             patch.object(gw, 'shoot', side_effect=lambda hwnd, out: calls.append(('shoot', hwnd)) or out), \
             patch.object(gw, 'client_size', return_value=(1280, 720)):
            result, path = gw.title_shot(None, Path('title.png'), (1280, 720), echo=lambda *a, **k: None)
        self.assertEqual(result.status, 'PASS')
        self.assertEqual(path, Path('title.png'))
        self.assertEqual(calls, [('window', [42]), ('resize', 7, 1280, 720), ('loaded', 7), ('shoot', 7)])


class ClientStageHookTests(unittest.TestCase):
    """stage_client hands the settled game's JVMs to on_ready and still ends them when on_ready fails."""

    class FakeProcess:
        def __init__(self):
            self.proc = type('Child', (), {'pid': 101})()
            self.answers = [('until', 'ready'), ('timeout', None)]
            self.killed = False

        def watch(self, **kwargs):
            return self.answers.pop(0)

        def kill_tree(self):
            self.killed = True

        def __enter__(self):
            return self

        def __exit__(self, *args):
            pass

    def run_stage(self, on_ready):
        game = 'java @/checkout/build/moddev/clientRunVmArgs.txt'
        log = env.Log('client-hook', Path(self.directory))
        try:
            with patch.object(env, 'Process', return_value=self.process), \
                 patch.object(env, 'java_processes', side_effect=[{}, {102: game}, {}]), \
                 patch.object(env, 'descendant_pids', return_value=[102]), \
                 patch.object(env, 'kill_pids', side_effect=self.killed.extend):
                return check.stage_client(check.Context(Path('/checkout'), log), on_ready=on_ready)
        finally:
            log.close()

    def setUp(self):
        import tempfile
        self.directory = tempfile.mkdtemp()
        self.process = self.FakeProcess()
        self.killed = []

    def tearDown(self):
        env.remove_tree(self.directory)

    def test_on_ready_gets_the_owned_game(self):
        seen = []
        result = self.run_stage(seen.extend)
        self.assertEqual(result.status, 'PASS')
        self.assertEqual(seen, [102])
        self.assertTrue(self.process.killed)
        self.assertEqual(self.killed, [102])

    def test_a_failing_on_ready_still_ends_the_game(self):
        def broken(pids):
            raise gw.CaptureError('no window')
        with self.assertRaises(gw.CaptureError):
            self.run_stage(broken)
        self.assertTrue(self.process.killed)
        self.assertEqual(self.killed, [102])


@unittest.skipUnless(sys.platform == 'win32', 'Win32 capture')
class WindowTests(unittest.TestCase):
    """A real Tk window of this process: found by owner, captured from the compositor, resized unactivated."""

    def setUp(self):
        import tkinter
        self.root = tkinter.Tk()
        self.root.attributes('-toolwindow', True)
        self.root.attributes('-disabled', True)
        self.root.geometry('160x100+40+40')
        canvas = tkinter.Canvas(self.root, width=160, height=100, highlightthickness=0, bd=0)
        canvas.pack(fill='both', expand=True)
        canvas.create_rectangle(0, 0, 80, 100, fill='#ff0000', width=0)
        canvas.create_rectangle(80, 0, 160, 100, fill='#0000ff', width=0)
        self.root.update()
        time.sleep(0.3)
        self.root.update()

    def tearDown(self):
        self.root.destroy()

    def window(self):
        found = gw.windows_of([os.getpid()], class_prefix='TkTopLevel')
        self.assertTrue(found, 'the Tk window was not found by its owner pid')
        return found[0]

    def pixel(self, rgb, width, x, y):
        offset = (y * width + x) * 3
        return tuple(rgb[offset:offset + 3])

    def test_capture_reads_the_window_pixels(self):
        width, height, rgb = gw.grab(self.window())
        self.assertFalse(gw.is_blank(rgb))
        self.assertEqual(self.pixel(rgb, width, width // 4, height // 2), (255, 0, 0))
        self.assertEqual(self.pixel(rgb, width, width * 3 // 4, height // 2), (0, 0, 255))

    def test_resize_sets_the_client_area(self):
        hwnd = self.window()
        gw.resize(hwnd, 240, 150)
        self.root.update()
        self.assertEqual(gw.client_size(hwnd), (240, 150))

    def test_other_processes_windows_are_not_found(self):
        self.assertEqual(gw.windows_of([os.getpid()], class_prefix='GLFW'), [])


if __name__ == '__main__':
    unittest.main(verbosity=2)
