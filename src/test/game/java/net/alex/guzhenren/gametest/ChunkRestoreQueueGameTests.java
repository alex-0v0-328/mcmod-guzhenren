package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.rewind.ChunkRestoreQueue;
import net.alex.guzhenren.gameplay.rewind.ChunkRewindService;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 山如故[Mountain As Before Gu]、江如故[River As Before Gu]的分摊还原：逐区段写回与一次性还原结果一致（含荡魂山保留
 * 位），每 tick 不超区段预算、160 tick 内自中心区块一圈圈还原完，多次催动先进先出。用私有 {@link ChunkRestoreQueue} 在
 * 测试内手动推进，不受并行测试排进全服队列的批次干扰；远区块槽位 6–9（{@link ChunkRewindGameTests#farChunk}）。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChunkRestoreQueueGameTests {

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState GLASS = Blocks.GLASS.defaultBlockState();
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final int PILLAR = 40;
    private static final int TICK_LIMIT = 4 * ChunkRestoreQueue.RESTORE_TICKS;

    private ChunkRestoreQueueGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void queuedRestoreMatchesTheOneShotRestore(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LevelChunk chunk = ChunkRewindGameTests.farChunk(helper, 6);
        ProtoChunk untouched = ChunkRewindGameTests.copyOf(level, chunk);
        BlockPos pillar = ChunkRewindGameTests.surface(chunk, 3, 3);
        BlockPos mountain = ChunkRewindGameTests.surface(chunk, 9, 9);
        BlockPos deep = chunk.getPos().getBlockAt(12, level.getMinBuildHeight() + 10, 12);
        BlockPos sky = pillar.above(PILLAR + 8);
        try {
            for (int dy = 0; dy < PILLAR; dy++) level.setBlock(pillar.above(dy), STONE, Block.UPDATE_CLIENTS);
            level.setBlock(mountain, STONE, Block.UPDATE_CLIENTS);
            level.setBlock(mountain.above(), ModBlocks.SOULSUNDER_STONE.get().defaultBlockState(),
                    Block.UPDATE_CLIENTS);
            ProtoChunk built = ChunkRewindGameTests.copyOf(level, chunk);
            ProtoChunk origin = ChunkRewindGameTests.copyOf(level, chunk);
            for (int dy = 0; dy < PILLAR; dy++) origin.setBlockState(pillar.above(dy), AIR, false);
            origin.setBlockState(mountain, AIR, false);
            origin.setBlockState(mountain.above(), AIR, false);
            origin.setBlockState(deep, GLASS, false);
            origin.setBlockState(sky, GLASS, false);
            ChunkRewindService.restore(level, chunk, origin);
            ProtoChunk once = ChunkRewindGameTests.copyOf(level, chunk);
            ChunkRewindService.restore(level, chunk, built);
            assertSameBlocks(built, chunk, "the one-shot restore undone");
            ChunkRestoreQueue queue = new ChunkRestoreQueue();
            queue.add(level, chunk.getPos(), List.of(chunk.getPos()), Map.of(chunk.getPos(), origin));

            int ticks = 0;
            while (queue.getQueued() > 0 && ticks < TICK_LIMIT) {
                assertEquals(1, queue.tick(level.getServer()), "a lone chunk restores one section a tick");
                ticks++;
            }

            assertEquals(level.getSectionsCount(), ticks, "a lone chunk takes a tick per section");
            assertSameBlocks(once, chunk, "the queued restore");
            assertEquals(ModBlocks.SOULSUNDER_STONE.get(), level.getBlockState(mountain.above()).getBlock());
            assertEquals(Blocks.STONE, level.getBlockState(mountain).getBlock());
            assertTrue(level.getBlockState(pillar.above(PILLAR - 1)).isAir(), "the top of the pillar goes");
        } finally {
            level.setBlock(mountain.above(), AIR, ChunkRewindService.RESTORE_FLAGS);
            ChunkRewindService.restore(level, chunk, untouched);
            ChunkRewindGameTests.release(helper, chunk.getPos());
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void eachTickStaysWithinTheBudgetRingByRing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LevelChunk center = ChunkRewindGameTests.farChunk(helper, 7);
        int radius = ChunkRewindService.RESTORE_RADIUS;
        int low = level.getMinBuildHeight() + 1;
        List<ChunkPos> targets = new ArrayList<>();
        Map<ChunkPos, ChunkAccess> origins = new HashMap<>();
        Map<ChunkPos, BlockState> before = new HashMap<>();
        for (int x = center.getPos().x - radius; x <= center.getPos().x + radius; x++) {
            for (int z = center.getPos().z - radius; z <= center.getPos().z + radius; z++) {
                LevelChunk chunk = level.getChunk(x, z);
                targets.add(chunk.getPos());
                origins.put(chunk.getPos(), marked(level, chunk, low));
                before.put(chunk.getPos(), level.getBlockState(marker(chunk.getPos(), low)));
            }
        }
        int sections = targets.size() * level.getSectionsCount();
        int budget = ChunkRestoreQueue.getSectionsPerTick(sections);
        ChunkRestoreQueue queue = new ChunkRestoreQueue();
        queue.add(level, center.getPos(), targets, origins);
        Map<ChunkPos, Integer> reached = new HashMap<>();
        try {
            int ticks = 0;
            while (queue.getQueued() > 0 && ticks < TICK_LIMIT) {
                int restored = queue.tick(level.getServer());
                ticks++;
                assertTrue(restored <= budget, "tick " + ticks + " restored " + restored + " of " + budget);
                for (ChunkPos pos : targets) {
                    if (!reached.containsKey(pos) && level.getBlockState(marker(pos, low)).is(Blocks.GLASS)) {
                        reached.put(pos, ticks);
                    }
                }
            }

            assertEquals(Math.ceilDiv(sections, ChunkRestoreQueue.RESTORE_TICKS), budget, "sections over 160 ticks");
            assertEquals(Math.ceilDiv(sections, budget), ticks, "every tick but the last uses the whole budget");
            assertTrue(ticks <= ChunkRestoreQueue.RESTORE_TICKS, "the restore ends within eight seconds");
            assertEquals(targets.size(), reached.size(), "every chunk is restored");
            assertEquals(1, (int) reached.get(center.getPos()), "the holder's chunk goes first");
            for (ChunkPos near : targets) {
                for (ChunkPos far : targets) {
                    if (center.getPos().getChessboardDistance(near) >= center.getPos().getChessboardDistance(far)) {
                        continue;
                    }
                    assertTrue(reached.get(near) < reached.get(far), () -> near + " goes before the outer " + far);
                }
            }
        } finally {
            for (ChunkPos pos : targets) {
                level.setBlock(marker(pos, low), before.get(pos), ChunkRewindService.RESTORE_FLAGS);
            }
            ChunkRewindGameTests.release(helper, center.getPos());
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void castsRestoreOneAfterAnotherFirstInFirstOut(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LevelChunk first = ChunkRewindGameTests.farChunk(helper, 8);
        LevelChunk second = ChunkRewindGameTests.farChunk(helper, 9);
        int low = level.getMinBuildHeight() + 1;
        int high = level.getMaxBuildHeight() - 1;
        List<BlockPos> marks = List.of(marker(first.getPos(), low), marker(first.getPos(), high),
                marker(second.getPos(), low), marker(second.getPos(), high));
        List<BlockState> before = marks.stream().map(level::getBlockState).toList();
        ChunkRestoreQueue queue = new ChunkRestoreQueue();
        queue.add(level, first.getPos(), List.of(first.getPos()),
                Map.of(first.getPos(), marked(level, first, low, high)));
        queue.add(level, second.getPos(), List.of(second.getPos()),
                Map.of(second.getPos(), marked(level, second, low, high)));
        int[] reached = new int[marks.size()];
        try {
            assertEquals(2, queue.getQueued(), "both casts wait in the queue");
            int ticks = 0;
            while (queue.getQueued() > 0 && ticks < TICK_LIMIT) {
                queue.tick(level.getServer());
                ticks++;
                for (int i = 0; i < marks.size(); i++) {
                    if (reached[i] == 0 && level.getBlockState(marks.get(i)).is(Blocks.GLASS)) reached[i] = ticks;
                }
            }

            int sections = level.getSectionsCount();
            assertEquals(1, reached[0], "the first cast starts at once from its lowest section");
            assertEquals(sections, reached[1], "the first cast ends on its top section");
            assertEquals(sections + 1, reached[2], "the second cast starts only after the first ends");
            assertEquals(2L * sections, reached[3], "the second cast ends after its own sections");
            queue.add(level, first.getPos(), List.of(first.getPos()), Map.of(first.getPos(), marked(level, first)));
            queue.clear();
            assertEquals(0, queue.getQueued(), "a server stop drops the waiting casts");
        } finally {
            for (int i = 0; i < marks.size(); i++) {
                level.setBlock(marks.get(i), before.get(i), ChunkRewindService.RESTORE_FLAGS);
            }
            ChunkRewindGameTests.release(helper, first.getPos());
            ChunkRewindGameTests.release(helper, second.getPos());
        }
        helper.succeed();
    }

    //region chunk copies [区块副本]
    private static ProtoChunk marked(ServerLevel level, LevelChunk chunk, int... heights) {
        ProtoChunk origin = ChunkRewindGameTests.copyOf(level, chunk);
        for (int y : heights) origin.setBlockState(marker(chunk.getPos(), y), GLASS, false);
        return origin;
    }

    private static BlockPos marker(ChunkPos pos, int y) { return pos.getBlockAt(8, y, 8); }

    private static void assertSameBlocks(ChunkAccess expected, LevelChunk actual, String what) {
        for (int index = 0; index < actual.getSectionsCount(); index++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState want = expected.getSection(index).getBlockState(x, y, z);
                        BlockState got = actual.getSection(index).getBlockState(x, y, z);
                        if (want == got) continue;

                        BlockPos pos = actual.getPos().getBlockAt(x,
                                actual.getMinBuildHeight() + (index << 4) + y, z);
                        throw new GameTestAssertException(what + " at " + pos.toShortString() + ": expected " + want
                                + " but got " + got);
                    }
                }
            }
        }
    }
    //endregion
}
