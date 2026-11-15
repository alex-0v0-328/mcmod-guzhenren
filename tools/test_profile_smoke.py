"""Unit checks for profile_smoke.py: the jfr dump parser, the mod attribution and the game-JVM role lookup."""
import os
import tempfile
import unittest
from pathlib import Path

import profile_smoke as profile

DUMP = '''jdk.ExecutionSample {
  startTime = 10:00:00.000
  sampledThread = "Server thread" (javaThreadId = 1)
  state = "STATE_RUNNABLE"
  stackTrace = [
    net.alex.guzhenren.gameplay.attribute.StrengthService.snapshot(Player) line: 40
    net.alex.guzhenren.lifecycle.Heartbeat.beat(ServerPlayer) line: 90
    net.alex.guzhenren.lifecycle.PlayerTickEvents.onPlayerTick(PlayerTickEvent$Post) line: 33
    net.minecraft.server.level.ServerPlayer.tick() line: 500
  ]
}

jdk.ExecutionSample {
  startTime = 10:00:00.020
  sampledThread = "ForkJoinPool.commonPool-worker-1" (javaThreadId = 2)
  state = "STATE_RUNNABLE"
  stackTrace = [
    net.minecraft.world.level.levelgen.synth.ImprovedNoise.noise(double, double, double) line: 10
    net.alex.guzhenrenworld.waterways.GroundHeights.compute(Context) line: 121
  ]
}

jdk.ObjectAllocationSample {
  startTime = 10:00:00.030
  objectClass = java.util.ArrayList (classLoader = app)
  weight = 2.5 MB
  stackTrace = [
    net.alex.guzhenren.gameplay.attribute.AttackDamageService.refresh(ServerPlayer) line: 98
    net.alex.guzhenren.lifecycle.Heartbeat.beat(ServerPlayer) line: 110
  ]
}

jdk.ObjectAllocationSample {
  startTime = 10:00:00.031
  objectClass = byte[]
  weight = 500.0 kB
  stackTrace = [
    net.minecraft.network.FriendlyByteBuf.writeBytes(byte[]) line: 1
  ]
}

jdk.GCPhasePause {
  startTime = 10:00:01.000
  duration = 12.5 ms
  name = "GC Pause (Young)"
}

jdk.GCPhasePause {
  startTime = 10:00:02.000
  duration = 1500 us
  name = "GC Pause (Young)"
}
'''


class DumpParserTests(unittest.TestCase):

    def test_events_split_on_top_level_braces_and_keep_frames_top_first(self):
        parsed = list(profile.events(DUMP))
        self.assertEqual(['jdk.ExecutionSample', 'jdk.ExecutionSample', 'jdk.ObjectAllocationSample',
                          'jdk.ObjectAllocationSample', 'jdk.GCPhasePause', 'jdk.GCPhasePause'],
                         [event['kind'] for event in parsed])
        self.assertEqual('net.alex.guzhenren.gameplay.attribute.StrengthService.snapshot(Player)',
                         parsed[0]['frames'][0])
        self.assertEqual(4, len(parsed[0]['frames']))

    def test_summary_attributes_only_this_mods_frames(self):
        lines = profile.summarize_text(DUMP, top=5, path='x.jfr')
        text = '\n'.join(lines)
        # One of the two CPU samples holds a guzhenren frame; the Gu World frame (guzhenrenworld) does not count.
        self.assertIn('CPU samples: 2 total, 1 on the server thread, 1 with a mod frame (50.00%)', text)
        self.assertIn('     1  gameplay.attribute.StrengthService.snapshot(Player)', text)
        # Inclusive counts every distinct mod frame on the stack once.
        self.assertIn('     1  lifecycle.Heartbeat.beat(ServerPlayer)', text)
        self.assertIn('     1  lifecycle.PlayerTickEvents.onPlayerTick(PlayerTickEvent$Post)', text)
        # 2.5 MB of 3.0 MB weighted allocation sits under a mod frame.
        self.assertIn('Allocation samples: 3 MB weighted total, 2.5 MB with a mod frame (83.33%)', text)
        self.assertIn('     2.5  gameplay.attribute.AttackDamageService.refresh(ServerPlayer)', text)
        self.assertIn('     2.5  java.util.ArrayList', text)
        # 12.5 ms + 1.5 ms of pauses, units normalized to ms.
        self.assertIn('GC pauses: 2 phases, 14 ms total, max 12.5 ms', text)

    def test_summary_of_an_empty_dump_is_well_formed(self):
        lines = profile.summarize_text('', top=5)
        self.assertEqual('CPU samples: 0 total, 0 on the server thread, 0 with a mod frame (0.00%)', lines[1])
        self.assertNotIn('GC pauses', '\n'.join(lines))


class GameRoleTests(unittest.TestCase):

    def test_roles_follow_the_stage_game_dirs_and_jdk_of_each_jvm(self):
        with tempfile.TemporaryDirectory() as scratch:
            root = Path(scratch)
            proc = root / 'proc'
            temp = root / 'temps' / 'guzhenren'
            jdk_bin = root / 'jdk' / 'bin'
            jdk_bin.mkdir(parents=True)
            (jdk_bin / 'java').write_text('', encoding='utf-8')
            for pid, folder in ((101, temp / 'multiplayer-server'), (102, temp / 'multiplayer-client'),
                                (103, temp / 'something-else')):
                folder.mkdir(parents=True)
                entry = proc / str(pid)
                entry.mkdir(parents=True)
                os.symlink(jdk_bin / 'java', entry / 'exe')
                os.symlink(folder, entry / 'cwd')
                (entry / 'cmdline').write_bytes(b'java\0-jar\0game.jar\0')
            # A non-Java process and a dead entry are skipped.
            other = proc / '104'
            other.mkdir()
            os.symlink(root / 'bin' / 'python3', other / 'exe')
            (proc / 'self').mkdir()

            roles = profile.game_roles(temp, proc_root=proc)

        self.assertEqual({'server': (101, jdk_bin), 'client': (102, jdk_bin)}, roles)


if __name__ == '__main__':
    unittest.main()
