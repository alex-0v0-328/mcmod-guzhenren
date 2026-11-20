"""Screenshots of the game window alone and key input aimed at that window alone: evidence for in-game checks.

    python -B tools/game_window.py shot [--pid PID] [--out PNG]      a client of this checkout that already runs
    python -B tools/game_window.py keys [--pid PID] F3 F3+B ...      press keys (chords with +) in that window
    python -B tools/game_window.py title [--out PNG]                 boot the client stage, shoot the title screen
    python -B tools/game_window.py scene --setup CMD ... [--keys KEY ...] [--target SELECTOR --frames N]
                                                                     a Temp server and a client that joins it; one shot,
                                                                     or a burst of --frames every --interval seconds

Capture is PrintWindow with PW_RENDERFULLCONTENT on the window handle: the window's own pixels from the compositor,
whatever covers it, never the screen. A minimised window has no pixels, and a frame that comes back one flat colour
is retried, then refused. Input is PostMessage to that handle (WM_KEYDOWN/UP carrying the scan code GLFW maps keys
from, WM_CHAR); there is no SendInput or SetForegroundWindow, so focus and every
other window stay as they are. GLFW reads modifier state from the real keyboard, so a posted Shift reaches
key-state checks (glfwGetKey) but not the callback's modifier bits.

The client stays unfocused: the scene writes pauseOnLostFocus:false (local_checks.client_options), and a client
of Alex's own needs the same or the pause menu covers the world. Rendered size is the window's client area
(854×480 by default; overrideWidth in options.txt does not resize it), so --size resizes the window, unactivated.

scene and title run Gradle and hold the shared lock. scene is the multiplayer stage without its checks: the classpath
mods (--mod copies extra jars into both throwaway dirs), --option lines appended to options.txt (lang:en_us,
guiScale:2), --setup commands over RCON with {player} replaced, --keys, then the shots. With --target, each frame
reads the target's position (`data get entity <target> Pos`), teleports the camera player to --offset from it facing
--look above its feet (`tp` places the feet; the eye is 1.62 higher), waits --settle and shoots: about 0.6 s a frame.
Without --target a burst shoots in place, e.g. F5 front views of the player's own animation; space such bursts with
an explicit --interval and clear the ground first (`fill … air`). Shots go to Temps/<project>/shots/.
"""
import argparse
import math
import re
import shutil
import struct
import sys
import time
import zlib
from pathlib import Path

import local_checks as check
import project_runtime as env

if sys.platform == 'win32':
    import ctypes
    from ctypes import wintypes


SHOTS = 'shots'
WINDOW_WAIT = 30
WORLD_SETTLE = 8
BLANK_RETRIES = 5
# The run/ pack reloads resources a second time ~8 s after the first; the overlay then fades out over 1 s.
LOADING_WAIT = 90
LOADING_FADE = 1.5
MOJANG_RED = bytes((0xEF, 0x32, 0x3D))
POSITION = re.compile(r'\[(-?\d+(?:\.\d+)?(?:E-?\d+)?)d, (-?\d+(?:\.\d+)?(?:E-?\d+)?)d, '
                      r'(-?\d+(?:\.\d+)?(?:E-?\d+)?)d\]', re.I)


class CaptureError(Exception):
    pass


# ----- pure helpers: PNG, pixels, key and mouse messages, tracking --------------------------------------------

def png_bytes(width, height, rgb):
    """An 8-bit RGB PNG of `rgb` (rows top to bottom, 3 bytes a pixel), with no filtering."""
    stride = width * 3
    raw = b''.join(b'\x00' + rgb[row * stride:(row + 1) * stride] for row in range(height))

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 6)) + chunk(b'IEND', b''))


def write_png(path, width, height, rgb):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png_bytes(width, height, rgb))
    return path


def bgra_to_rgb(bgra):
    """32-bit BGRA pixels (GetDIBits) to packed RGB."""
    rgb = bytearray(len(bgra) // 4 * 3)
    rgb[0::3], rgb[1::3], rgb[2::3] = bgra[2::4], bgra[1::4], bgra[0::4]
    return bytes(rgb)


def is_blank(rgb):
    """True when every pixel has the first pixel's colour: what a window without content captures as."""
    return len(rgb) < 3 or rgb == rgb[:3] * (len(rgb) // 3)


def loading_share(rgb):
    """The share of pixels in the Mojang loading overlay's red: near 1 while resources (re)load."""
    return rgb.count(MOJANG_RED) * 3 / max(len(rgb), 1)


VK = {'ESCAPE': 0x1B, 'ESC': 0x1B, 'ENTER': 0x0D, 'RETURN': 0x0D, 'SPACE': 0x20, 'TAB': 0x09, 'BACKSPACE': 0x08,
      'SHIFT': 0x10, 'CONTROL': 0x11, 'CTRL': 0x11, 'ALT': 0x12, 'MINUS': 0xBD, 'EQUALS': 0xBB, 'COMMA': 0xBC,
      'PERIOD': 0xBE, 'SLASH': 0xBF, 'GRAVE': 0xC0, 'SEMICOLON': 0xBA, 'QUOTE': 0xDE, 'LBRACKET': 0xDB,
      'RBRACKET': 0xDD, 'BACKSLASH': 0xDC}
# Keys whose scan code needs the extended bit: without it GLFW reads the numpad key with the same code.
EXTENDED = {'LEFT': 0x25, 'UP': 0x26, 'RIGHT': 0x27, 'DOWN': 0x28, 'INSERT': 0x2D, 'DELETE': 0x2E, 'HOME': 0x24,
            'END': 0x23, 'PAGEUP': 0x21, 'PAGEDOWN': 0x22}
WM_KEYDOWN, WM_KEYUP, WM_CHAR = 0x0100, 0x0101, 0x0102


def parse_key(name):
    """(virtual key, extended) for a key name: a letter or digit, F1–F24, or a name in VK or EXTENDED."""
    key = name.strip().upper()
    if len(key) == 1 and (key.isascii() and key.isalnum()):
        return ord(key), False
    if re.fullmatch(r'F([1-9]|1\d|2[0-4])', key):
        return 0x6F + int(key[1:]), False
    if key in VK:
        return VK[key], False
    if key in EXTENDED:
        return EXTENDED[key], True
    raise ValueError(f'unknown key {name!r}; use a letter, digit, F1-F24 or one of '
                     f'{", ".join(sorted(VK.keys() | EXTENDED.keys()))}')


def parse_chord(text):
    """A key or a chord such as F3+B: the keys pressed in order and released in reverse."""
    return [parse_key(part) for part in text.split('+')]


def key_lparam(scan, extended, up):
    """WM_KEYDOWN/UP lParam: repeat count 1, the scan code GLFW maps the key from, and for a release the
    previous-state and transition bits."""
    return 1 | (scan & 0xFF) << 16 | (1 << 24 if extended else 0) | ((0b11 << 30) if up else 0)


def parse_pos(reply):
    """(x, y, z) from a `data get entity <target> Pos` reply, or None when no entity matched."""
    match = POSITION.search(reply or '')
    return tuple(float(value) for value in match.groups()) if match else None


def follow_command(camera, target, offset, look):
    """The `tp` that puts the camera's feet at `offset` from `target` (x, y, z), facing `look` blocks above
    the target's feet. Vanilla aims `facing` from the camera's eyes."""
    feet = [round(axis + delta, 3) for axis, delta in zip(target, offset)]
    aim = [round(target[0], 3), round(target[1] + look, 3), round(target[2], 3)]
    return f'tp {camera} {feet[0]:g} {feet[1]:g} {feet[2]:g} facing {aim[0]:g} {aim[1]:g} {aim[2]:g}'


def parse_size(text):
    match = re.fullmatch(r'(\d+)[x×](\d+)', text.strip().lower())
    if not match or min(int(match.group(1)), int(match.group(2))) < 64:
        raise argparse.ArgumentTypeError(f'size {text!r}: use WIDTHxHEIGHT, each at least 64, e.g. 1280x720')
    return int(match.group(1)), int(match.group(2))


def stamp():
    return time.strftime('%Y%m%d-%H%M%S')


# ----- Win32: find, capture, resize, post input ----------------------------------------------------------------

if sys.platform == 'win32':
    class _BitmapInfo(ctypes.Structure):
        _fields_ = [('biSize', wintypes.DWORD), ('biWidth', wintypes.LONG), ('biHeight', wintypes.LONG),
                    ('biPlanes', wintypes.WORD), ('biBitCount', wintypes.WORD), ('biCompression', wintypes.DWORD),
                    ('biSizeImage', wintypes.DWORD), ('biXPelsPerMeter', wintypes.LONG),
                    ('biYPelsPerMeter', wintypes.LONG), ('biClrUsed', wintypes.DWORD),
                    ('biClrImportant', wintypes.DWORD), ('bmiColors', wintypes.DWORD * 4)]

    def _function(library, name, result, *arguments):
        function = getattr(library, name)
        function.restype, function.argtypes = result, arguments
        return function

    _user32 = ctypes.WinDLL('user32', use_last_error=True)
    _gdi32 = ctypes.WinDLL('gdi32', use_last_error=True)
    _ENUM = ctypes.WINFUNCTYPE(wintypes.BOOL, wintypes.HWND, wintypes.LPARAM)
    _EnumWindows = _function(_user32, 'EnumWindows', wintypes.BOOL, _ENUM, wintypes.LPARAM)
    _GetWindowThreadProcessId = _function(_user32, 'GetWindowThreadProcessId', wintypes.DWORD, wintypes.HWND,
                                          ctypes.POINTER(wintypes.DWORD))
    _GetClassNameW = _function(_user32, 'GetClassNameW', ctypes.c_int, wintypes.HWND, wintypes.LPWSTR, ctypes.c_int)
    _IsWindowVisible = _function(_user32, 'IsWindowVisible', wintypes.BOOL, wintypes.HWND)
    _IsIconic = _function(_user32, 'IsIconic', wintypes.BOOL, wintypes.HWND)
    _IsWindow = _function(_user32, 'IsWindow', wintypes.BOOL, wintypes.HWND)
    _GetClientRect = _function(_user32, 'GetClientRect', wintypes.BOOL, wintypes.HWND, ctypes.POINTER(wintypes.RECT))
    _GetDC = _function(_user32, 'GetDC', wintypes.HDC, wintypes.HWND)
    _ReleaseDC = _function(_user32, 'ReleaseDC', ctypes.c_int, wintypes.HWND, wintypes.HDC)
    _PrintWindow = _function(_user32, 'PrintWindow', wintypes.BOOL, wintypes.HWND, wintypes.HDC, wintypes.UINT)
    _PostMessageW = _function(_user32, 'PostMessageW', wintypes.BOOL, wintypes.HWND, wintypes.UINT, wintypes.WPARAM,
                              wintypes.LPARAM)
    _MapVirtualKeyW = _function(_user32, 'MapVirtualKeyW', wintypes.UINT, wintypes.UINT, wintypes.UINT)
    _GetWindowLongW = _function(_user32, 'GetWindowLongW', wintypes.LONG, wintypes.HWND, ctypes.c_int)
    _GetDpiForWindow = _function(_user32, 'GetDpiForWindow', wintypes.UINT, wintypes.HWND)
    _AdjustWindowRectExForDpi = _function(_user32, 'AdjustWindowRectExForDpi', wintypes.BOOL,
                                          ctypes.POINTER(wintypes.RECT), wintypes.DWORD, wintypes.BOOL,
                                          wintypes.DWORD, wintypes.UINT)
    _SetWindowPos = _function(_user32, 'SetWindowPos', wintypes.BOOL, wintypes.HWND, wintypes.HWND, ctypes.c_int,
                              ctypes.c_int, ctypes.c_int, ctypes.c_int, wintypes.UINT)
    _SetProcessDpiAwarenessContext = _function(_user32, 'SetProcessDpiAwarenessContext', wintypes.BOOL,
                                               wintypes.HANDLE)
    _CreateCompatibleDC = _function(_gdi32, 'CreateCompatibleDC', wintypes.HDC, wintypes.HDC)
    _CreateCompatibleBitmap = _function(_gdi32, 'CreateCompatibleBitmap', wintypes.HBITMAP, wintypes.HDC,
                                        ctypes.c_int, ctypes.c_int)
    _SelectObject = _function(_gdi32, 'SelectObject', wintypes.HGDIOBJ, wintypes.HDC, wintypes.HGDIOBJ)
    _GetDIBits = _function(_gdi32, 'GetDIBits', ctypes.c_int, wintypes.HDC, wintypes.HBITMAP, wintypes.UINT,
                           wintypes.UINT, ctypes.c_void_p, ctypes.POINTER(_BitmapInfo), wintypes.UINT)
    _DeleteObject = _function(_gdi32, 'DeleteObject', wintypes.BOOL, wintypes.HGDIOBJ)
    _DeleteDC = _function(_gdi32, 'DeleteDC', wintypes.BOOL, wintypes.HDC)
    # Per-monitor v2: sizes and client coordinates in physical pixels, the ones GLFW and the game use.
    _SetProcessDpiAwarenessContext(wintypes.HANDLE(-4))

PW_CLIENTONLY = 0x1
PW_RENDERFULLCONTENT = 0x2
SWP_NOMOVE, SWP_NOZORDER, SWP_NOACTIVATE, SWP_NOOWNERZORDER = 0x2, 0x4, 0x10, 0x200
GWL_STYLE, GWL_EXSTYLE = -16, -20


def windows_of(pids, class_prefix='GLFW'):
    """Visible top-level windows owned by `pids` whose class starts with `class_prefix`, largest first."""
    wanted = set(pids)
    found = []

    @_ENUM
    def visit(hwnd, _):
        owner = wintypes.DWORD()
        _GetWindowThreadProcessId(hwnd, ctypes.byref(owner))
        if owner.value in wanted and _IsWindowVisible(hwnd):
            name = ctypes.create_unicode_buffer(256)
            _GetClassNameW(hwnd, name, 256)
            if name.value.startswith(class_prefix):
                found.append(hwnd)
        return True
    _EnumWindows(visit, 0)
    return sorted(found, key=lambda hwnd: -(client_size(hwnd)[0] * client_size(hwnd)[1]))


def game_window(pids=None, root=env.REPOSITORY_ROOT, timeout=0):
    """The game window of `pids`, or of the game JVMs from this checkout (IDEA's or Gradle's), waiting up to
    `timeout` seconds for it to open."""
    deadline = time.monotonic() + timeout
    while True:
        owners = pids if pids else env.game_jvms(env.java_processes(), root)
        found = windows_of(owners)
        if found:
            return found[0]
        if time.monotonic() >= deadline:
            raise CaptureError(f'no game window for {"pids " + str(list(owners)) if owners else "this checkout"}'
                               ' (is a client running?)')
        time.sleep(0.5)


def client_size(hwnd):
    rect = wintypes.RECT()
    _GetClientRect(hwnd, ctypes.byref(rect))
    return rect.right - rect.left, rect.bottom - rect.top


def grab(hwnd):
    """(width, height, rgb) of the window's client area, rendered by the compositor into our own bitmap."""
    if not _IsWindow(hwnd):
        raise CaptureError('the game window is gone')
    if _IsIconic(hwnd):
        raise CaptureError('the game window is minimised; restore it (no focus needed) and retry')
    width, height = client_size(hwnd)
    if width <= 0 or height <= 0:
        raise CaptureError('the game window has no client area')
    screen = _GetDC(None)
    memory = _CreateCompatibleDC(screen)
    bitmap = _CreateCompatibleBitmap(screen, width, height)
    previous = _SelectObject(memory, bitmap)
    try:
        if not _PrintWindow(hwnd, memory, PW_CLIENTONLY | PW_RENDERFULLCONTENT):
            raise CaptureError(f'PrintWindow failed (error {ctypes.get_last_error()})')
        info = _BitmapInfo(biSize=40, biWidth=width, biHeight=-height, biPlanes=1, biBitCount=32, biCompression=0)
        pixels = ctypes.create_string_buffer(width * height * 4)
        if _GetDIBits(memory, bitmap, 0, height, pixels, ctypes.byref(info), 0) != height:
            raise CaptureError(f'GetDIBits failed (error {ctypes.get_last_error()})')
        return width, height, bgra_to_rgb(pixels.raw)
    finally:
        _SelectObject(memory, previous)
        _DeleteObject(bitmap)
        _DeleteDC(memory)
        _ReleaseDC(None, screen)


def shoot(hwnd, path, retries=BLANK_RETRIES):
    """Saves the window's client area as a PNG; a flat frame (not drawn yet) is retried every 0.2 s."""
    for attempt in range(retries + 1):
        width, height, rgb = grab(hwnd)
        if not is_blank(rgb):
            return write_png(path, width, height, rgb)
        if attempt < retries:
            time.sleep(0.2)
    raise CaptureError(f'the window gave {retries + 1} blank {width}x{height} frames')


def wait_loaded(hwnd, timeout=LOADING_WAIT):
    """Waits until two frames LOADING_FADE apart show no loading overlay; the resource reloads of a big pack come
    in more than one wave."""
    deadline = time.monotonic() + timeout
    clear = 0
    while clear < 2:
        if time.monotonic() > deadline:
            raise CaptureError(f'the loading overlay was still up after {timeout} s')
        clear = clear + 1 if loading_share(grab(hwnd)[2]) < 0.05 else 0
        time.sleep(LOADING_FADE if clear else 0.5)


def resize(hwnd, width, height):
    """Sets the client area to width×height without activating or moving the window."""
    rect = wintypes.RECT(0, 0, width, height)
    style = _GetWindowLongW(hwnd, GWL_STYLE) & 0xFFFFFFFF
    extended = _GetWindowLongW(hwnd, GWL_EXSTYLE) & 0xFFFFFFFF
    _AdjustWindowRectExForDpi(ctypes.byref(rect), style, False, extended, _GetDpiForWindow(hwnd))
    if not _SetWindowPos(hwnd, None, 0, 0, rect.right - rect.left, rect.bottom - rect.top,
                         SWP_NOMOVE | SWP_NOZORDER | SWP_NOACTIVATE | SWP_NOOWNERZORDER):
        raise CaptureError(f'SetWindowPos failed (error {ctypes.get_last_error()})')


def post(hwnd, message, wparam, lparam):
    if not _PostMessageW(hwnd, message, wparam, lparam):
        raise CaptureError(f'PostMessage failed (error {ctypes.get_last_error()})')


def press(hwnd, chord, hold=0.1):
    """Presses a key or chord (parse_chord's text) in the window: downs in order, holds, ups in reverse."""
    keys = [(vk, _MapVirtualKeyW(vk, 0), extended) for vk, extended in parse_chord(chord)]
    for vk, scan, extended in keys:
        post(hwnd, WM_KEYDOWN, vk, key_lparam(scan, extended, up=False))
    time.sleep(hold)
    for vk, scan, extended in reversed(keys):
        post(hwnd, WM_KEYUP, vk, key_lparam(scan, extended, up=True))


def type_text(hwnd, text):
    """Types text as WM_CHAR, one UTF-16 unit at a time (GLFW pairs surrogates itself)."""
    units = text.encode('utf-16-le')
    for index in range(0, len(units), 2):
        post(hwnd, WM_CHAR, int.from_bytes(units[index:index + 2], 'little'), 1)


# ----- title: the client stage, shot once the title screen has settled ---------------------------------------

def title_shot(ctx, out, size=None, echo=print):
    shots = {}

    def on_ready(pids):
        hwnd = game_window(pids, timeout=WINDOW_WAIT)
        if size:
            resize(hwnd, *size)
        wait_loaded(hwnd)
        shots['path'] = shoot(hwnd, out)
        echo(f'title screen: {shots["path"]}  ({"x".join(map(str, client_size(hwnd)))})', flush=True)
    result = check.stage_client(ctx, on_ready=on_ready)
    return result, shots.get('path')


# ----- scene: a Temp server and a client that joins it ------------------------------------------------------

class Scene:
    """A smoke server and a client that quick-plays into it, both in fresh Temp dirs with the classpath mods: the
    multiplayer stage without its checks, held open for commands and shots. Run it under the Gradle lock; leaving
    stops both and restores IDEA's Client arguments, which the joining client's -P properties rewrote."""

    def __init__(self, ctx, options=(), mods=(), size=None, echo=print):
        self.ctx, self.options, self.mods, self.size, self.echo = ctx, list(options), list(mods), size, echo
        self.server = self.client = self.rcon = self.window = self.player = None
        self.game_dir = self.client_dir = None
        self.booted = False

    def __enter__(self):
        try:
            self._start()
        except BaseException:
            self.__exit__(None, None, None)
            raise
        return self

    def _start(self):
        ctx = self.ctx
        running = env.game_jvms(env.java_processes(), ctx.root)
        if running:
            raise CaptureError(f'a game from this checkout is already running (pid {running}); close it first')
        command, self.game_dir, port, self.rcon_port, self.password = check.server_command(ctx, 'capture-server')
        self.client_dir = check.fresh_game_dir(ctx, 'capture-client')
        options = check.client_options() + ''.join(f'{line}\n' for line in self.options)
        (self.client_dir / 'options.txt').write_text(options, encoding='utf-8')
        for jar in self.mods:
            for folder in (self.game_dir, self.client_dir):
                (folder / 'mods').mkdir(exist_ok=True)
                shutil.copy2(jar, folder / 'mods')
        self.server = env.Process(command, ctx.root, ctx.log)
        outcome, line = self.server.watch(until=check.SERVER_DONE.search, fail=check.CRASH.search,
                                          timeout=check.BOOT_TIMEOUT)
        if outcome != 'until':
            raise CaptureError(f'server did not boot ({outcome}): {line or ""}')
        self.booted = True
        client = env.gradle_command(ctx.root, 'runClient', f'-PclientGameDir={self.client_dir}',
                                    f'-PquickPlayServer=127.0.0.1:{port}', project=ctx.project, daemon=False)
        ctx.log.write(f'$ {" ".join(client)}')
        self.client = env.Process(client, ctx.root, ctx.log)
        outcome, side, line = check.watch_pair(self.server, self.client, until=check.JOINED.search,
                                               timeout=check.BOOT_TIMEOUT)
        if outcome != 'until':
            raise CaptureError(f'client did not join ({side} {outcome}): {line or ""}')
        self.player = check.JOINED.search(line).group(1)
        self.rcon = check.Rcon(self.rcon_port, self.password)
        self.window = game_window(self._client_jvms(), timeout=WINDOW_WAIT)
        if self.size:
            resize(self.window, *self.size)
        self.pulse(WORLD_SETTLE)
        self.echo(f'{self.player} joined; window {"x".join(map(str, client_size(self.window)))}', flush=True)

    def _client_jvms(self):
        owned = set(env.descendant_pids(self.client.proc.pid))
        return [pid for pid in env.game_jvms(env.java_processes(), self.ctx.root) if pid in owned]

    def pulse(self, seconds):
        """Waits while both games keep running; a crash, an exit or a dropped player raises."""
        deadline = time.monotonic() + seconds
        while True:
            remaining = deadline - time.monotonic()
            if remaining <= 0:
                return
            outcome, line = self.server.watch(
                fail=lambda text: check.CRASH.search(text) or check.LOST.search(text), timeout=min(0.05, remaining))
            if outcome not in ('timeout',):
                raise CaptureError(f'server {outcome}: {line or ""}')
            outcome, line = self.client.watch(fail=check.CRASH.search, timeout=0.01)
            if outcome != 'timeout':
                raise CaptureError(f'client {outcome}: {line or ""}')

    def command(self, text):
        text = text.replace('{player}', self.player)
        reply = self.rcon.command(text).strip()
        self.ctx.log.write(f'[rcon] {text} -> {reply}')
        return reply

    def burst(self, out_dir, frames, interval, target=None, offset=(0, 1, 4), look=0.5, settle=0.25):
        """Shoots `frames` frames `interval` seconds apart into out_dir (frame-NN.png, frames.tsv); with a target,
        each frame first moves the camera player to follow it."""
        out_dir = Path(out_dir)
        out_dir.mkdir(parents=True, exist_ok=True)
        rows = ['frame\tseconds\tx\ty\tz']
        started = time.monotonic()
        for frame in range(frames):
            self.pulse(max(0.0, started + frame * interval - time.monotonic()))
            where = ('', '', '')
            if target:
                where = parse_pos(self.command(f'data get entity {target} Pos'))
                if where is None:
                    raise CaptureError(f'no entity matches {target}')
                self.command(follow_command(self.player, where, offset, look))
                self.pulse(settle)
            shoot(self.window, out_dir / f'frame-{frame:02d}.png')
            rows.append('\t'.join([str(frame), f'{time.monotonic() - started:.2f}', *(f'{v:g}' if v != '' else ''
                                                                                      for v in where)]))
        (out_dir / 'frames.tsv').write_text('\n'.join(rows) + '\n', encoding='utf-8')
        return out_dir

    def __exit__(self, *exc):
        if self.rcon:
            self.rcon.close()
        if self.client:
            spawned = self._client_jvms()
            self.client.kill_tree()
            env.kill_pids(spawned)
        if self.server:
            if self.booted:
                check.stop_server(self.server, self.game_dir, self.rcon_port, self.password)
            self.server.__exit__(None, None, None)
        if self.client:
            stale = check.restore_client_args(self.ctx)
            if stale:
                self.echo('; '.join(stale), flush=True)
        if self.game_dir:
            env.kill_pids(env.java_processes_in(self.game_dir))
            env.remove_tree(self.game_dir)
        if self.client_dir:
            env.remove_tree(self.client_dir)


# ----- command line ---------------------------------------------------------------------------------------------

def parse_offset(values):
    return tuple(float(value) for value in values)


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    commands = parser.add_subparsers(dest='action', required=True)
    shot = commands.add_parser('shot', help='screenshot a running client of this checkout')
    shot.add_argument('--pid', type=int, help='the game JVM (default: the one running from this checkout)')
    shot.add_argument('--out', type=Path, help='PNG path (default Temps/<project>/shots/shot-<time>.png)')
    keys = commands.add_parser('keys', help='press keys or chords (F3+B) in a running client')
    keys.add_argument('keys', nargs='*', help='key names: letters, digits, F1-F24, ESC, ENTER, UP, ...')
    keys.add_argument('--pid', type=int)
    keys.add_argument('--text', help='then type this text (e.g. a chat line after T)')
    title = commands.add_parser('title', help='boot the client stage and shoot the title screen')
    title.add_argument('--out', type=Path)
    title.add_argument('--size', type=parse_size, help='client area, e.g. 1280x720')
    scene = commands.add_parser('scene', help='Temp server + joined client: setup over RCON, then shots')
    scene.add_argument('--setup', action='append', default=[], help='RCON command, {player} = the client')
    scene.add_argument('--keys', nargs='+', default=[], help='keys pressed after the setup, e.g. F1 F5')
    scene.add_argument('--option', action='append', default=[], help='options.txt line, e.g. lang:en_us')
    scene.add_argument('--mod', action='append', default=[], type=Path, help='extra mod jar for both sides')
    scene.add_argument('--size', type=parse_size)
    scene.add_argument('--wait', type=float, default=1.0, help='seconds between the keys and the first shot')
    scene.add_argument('--frames', type=int, default=1)
    scene.add_argument('--interval', type=float, default=0.6)
    scene.add_argument('--target', help='entity selector the camera follows, e.g. @e[type=guzhenren:x,limit=1]')
    scene.add_argument('--offset', nargs=3, default=(0, 1, 4), metavar=('DX', 'DY', 'DZ'),
                       help='camera feet from the target (default 0 1 4)')
    scene.add_argument('--look', type=float, default=0.5, help='aim this high above the target feet')
    scene.add_argument('--out', type=Path, help='folder (default Temps/<project>/shots/scene-<time>/)')
    args = parser.parse_args(argv)
    env.require_windows()
    project = env.PROJECTS['guzhenren']
    shots = project.temp / SHOTS
    try:
        if args.action == 'shot':
            path = shoot(game_window([args.pid] if args.pid else None), args.out or shots / f'shot-{stamp()}.png')
            print(path)
            return 0
        if args.action == 'keys':
            hwnd = game_window([args.pid] if args.pid else None)
            for chord in args.keys:
                press(hwnd, chord)
                time.sleep(0.1)
            if args.text:
                type_text(hwnd, args.text)
            return 0
        with env.Lock(wait=env.LOCK_WAIT):
            log = env.Log('game_window', log_dir=project.temp / 'logs', per_run=True)
            print(f'log: {log.path}', flush=True)
            ctx = check.Context(project.root, log, project=project)
            try:
                if args.action == 'title':
                    result, path = title_shot(ctx, args.out or shots / f'title-{stamp()}.png', args.size)
                    print(check.summary_line('client', result))
                    return 0 if result.status == 'PASS' and path else 1
                with Scene(ctx, args.option, args.mod, args.size) as scene_:
                    for text in args.setup:
                        print(f'> {text}\n  {scene_.command(text)}', flush=True)
                    for chord in args.keys:
                        press(scene_.window, chord)
                        scene_.pulse(0.2)
                    scene_.pulse(args.wait)
                    folder = scene_.burst(args.out or shots / f'scene-{stamp()}', args.frames, args.interval,
                                          args.target, parse_offset(args.offset), args.look)
                    print(folder)
                return 0
            finally:
                log.close()
    except (CaptureError, ValueError) as error:
        print(f'game_window: {error}', file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
