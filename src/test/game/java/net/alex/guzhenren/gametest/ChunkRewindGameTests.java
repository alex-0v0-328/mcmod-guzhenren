package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.rewind.ChunkOriginGenerator;
import net.alex.guzhenren.gameplay.rewind.ChunkRewindService;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 山如故[Mountain As Before Gu]、江如故[River As Before Gu]背后的裸地：恢复只写异于原貌的方块并清除放置的箱子，荡魂山
 * 石、胆石连同下方连续支撑原样保留；装饰过滤留岩石重着色，去树、植物、雪、冰、湖、矿石；按种子重生成的区块复原改动地表。
 * 恢复改写整区块会抹掉相邻测试，故都在远离测试网格的区块上跑（{@link #farChunk}：测试以东 {@code slot} × 4096 格），测试期间强制加载、结束释放。
 * 原貌副本（{@link #copyOf}）逐格复制：原版 {@code PalettedContainer#copy} 与原区块共用调色板扩容回调，向副本写新方块
 * 会扩容到原区块。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChunkRewindGameTests {

    private static final int FAR_BLOCKS = 4096;

    private ChunkRewindGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void restoreSetsOnlyDifferencesAndClearsWhatIsNotLand(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LevelChunk chunk = farChunk(helper, 1);
        BlockPos chestPos = surface(chunk, 3, 3);
        BlockPos stonePos = surface(chunk, 5, 5);
        try {
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
            ChestBlockEntity chest = assertNotNull((ChestBlockEntity) level.getBlockEntity(chestPos),
                    () -> "placed chest");
            chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
            ProtoChunk origin = copyOf(level, chunk);
            origin.setBlockState(chestPos, Blocks.AIR.defaultBlockState(), false);
            origin.setBlockState(stonePos, Blocks.STONE.defaultBlockState(), false);

            int changed = ChunkRewindService.restore(level, chunk, origin);

            assertEquals(2, changed, "only the chest and the stone differ");
            assertTrue(level.getBlockState(chestPos).isAir(), "a chest that is not land goes");
            assertEquals(Blocks.STONE, level.getBlockState(stonePos).getBlock());
        } finally {
            level.setBlock(stonePos, Blocks.AIR.defaultBlockState(), ChunkRewindService.RESTORE_FLAGS);
            release(helper, chunk.getPos());
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void mortalRewindLeavesTheSoulsunderMountainAndItsSupportsStanding(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LevelChunk chunk = farChunk(helper, 5);
        BlockPos mountain = surface(chunk, 3, 3);
        BlockPos gap = surface(chunk, 9, 9);
        BlockPos loose = surface(chunk, 6, 6);
        List<BlockPos> placed = List.of(mountain, mountain.above(), mountain.above(2), mountain.above(3), gap,
                gap.above(2), loose);
        try {
            level.setBlock(mountain, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(mountain.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(mountain.above(2), ModBlocks.SOULSUNDER_STONE.get().defaultBlockState(),
                    Block.UPDATE_CLIENTS);
            level.setBlock(mountain.above(3), ModBlocks.GUTS_STONE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(gap, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(gap.above(2), ModBlocks.SOULSUNDER_STONE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(loose, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            ProtoChunk origin = copyOf(level, chunk);
            for (BlockPos pos : placed) origin.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);

            int changed = ChunkRewindService.restore(level, chunk, origin);

            assertEquals(2, changed, "only the stone under the gap and the loose stone go");
            assertEquals(ModBlocks.GUTS_STONE.get(), level.getBlockState(mountain.above(3)).getBlock());
            assertEquals(ModBlocks.SOULSUNDER_STONE.get(), level.getBlockState(mountain.above(2)).getBlock());
            assertEquals(Blocks.STONE, level.getBlockState(mountain.above()).getBlock());
            assertEquals(Blocks.DIRT, level.getBlockState(mountain).getBlock());
            assertEquals(ModBlocks.SOULSUNDER_STONE.get(), level.getBlockState(gap.above(2)).getBlock());
            assertTrue(level.getBlockState(gap).isAir(), "an air gap ends the support");
            assertTrue(level.getBlockState(loose).isAir(), "stone away from the mountain is restored");
        } finally {
            for (BlockPos pos : placed) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), ChunkRewindService.RESTORE_FLAGS);
            }
            release(helper, chunk.getPos());
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void keepLetsOnlyTerrainRecolouringThrough(GameTestHelper helper) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        assertEquals(Blocks.GRANITE, ChunkOriginGenerator.keep(stone, Blocks.GRANITE.defaultBlockState(), 60)
                .getBlock(), "a cliff's rock band stays");
        assertEquals(Blocks.STONE, ChunkOriginGenerator.keep(Blocks.DIRT.defaultBlockState(), stone, 60).getBlock(),
                "a canyon's stone column over soil stays");
        assertEquals(air, ChunkOriginGenerator.keep(air, Blocks.OAK_LOG.defaultBlockState(), 70), "no tree");
        assertEquals(air, ChunkOriginGenerator.keep(air, Blocks.SHORT_GRASS.defaultBlockState(), 70), "no plant");
        assertEquals(air, ChunkOriginGenerator.keep(air, Blocks.SNOW.defaultBlockState(), 70), "no snow layer");
        assertEquals(air, ChunkOriginGenerator.keep(air, stone, 70), "no boulder");
        assertEquals(water, ChunkOriginGenerator.keep(water, Blocks.ICE.defaultBlockState(), 62), "no ice");
        assertEquals(water, ChunkOriginGenerator.keep(water, Blocks.SEAGRASS.defaultBlockState(), 50), "no seagrass");
        assertEquals(stone, ChunkOriginGenerator.keep(stone, water, 60), "no lake");
        assertEquals(stone, ChunkOriginGenerator.keep(stone, Blocks.IRON_ORE.defaultBlockState(), 30), "no ore");
        assertEquals(deepslate, ChunkOriginGenerator.keep(deepslate,
                Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState(), -40), "no deepslate ore");
        assertEquals(deepslate, ChunkOriginGenerator.keep(Blocks.RAW_IRON_BLOCK.defaultBlockState(),
                Blocks.RAW_IRON_BLOCK.defaultBlockState(), -20), "a noise ore vein turns to the rock around it");
        assertEquals(stone, ChunkOriginGenerator.keep(Blocks.COPPER_ORE.defaultBlockState(),
                Blocks.COPPER_ORE.defaultBlockState(), 20), "a vein ore above zero turns to stone");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 600)
    public static void rewindPutsTheGeneratedSurfaceBack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LevelChunk chunk = farChunk(helper, 2);
        BlockPos top = surface(chunk, 8, 8).below();
        BlockState made = level.getBlockState(top);
        level.setBlock(top, Blocks.GLASS.defaultBlockState(), ChunkRewindService.RESTORE_FLAGS);
        level.setBlock(top.above(), Blocks.STONE.defaultBlockState(), ChunkRewindService.RESTORE_FLAGS);

        ChunkRewindService.rewindAround(level, chunk.getPos(), List.of(chunk.getPos()));

        helper.succeedWhen(() -> {
            helper.assertTrue(ChunkRewindService.getGenerating() == 0 && ChunkRewindService.getQueued() == 0,
                    "the rewind is still running");
            helper.assertTrue(level.getBlockState(top) == made, "the surface block is generated again");
            helper.assertTrue(level.getBlockState(top.above()).isAir(), "a placed block is gone");
            release(helper, chunk.getPos());
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 600)
    public static void rewindReachesTheCornerOfTheFiveByFiveSquare(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LevelChunk center = farChunk(helper, 4);
        int radius = ChunkRewindService.RESTORE_RADIUS;
        assertEquals(2, radius, "the restore covers the 5×5 chunks around the holder");
        ChunkPos cornerPos = new ChunkPos(center.getPos().x + radius, center.getPos().z + radius);
        LevelChunk corner = level.getChunk(cornerPos.x, cornerPos.z);
        BlockPos placed = surface(corner, 8, 8);
        level.setBlock(placed, Blocks.STONE.defaultBlockState(), ChunkRewindService.RESTORE_FLAGS);

        ChunkRewindService.rewindAround(level, center.getPos(), List.of(cornerPos));

        helper.succeedWhen(() -> {
            helper.assertTrue(ChunkRewindService.getGenerating() == 0 && ChunkRewindService.getQueued() == 0,
                    "the rewind is still running");
            helper.assertTrue(level.getBlockState(placed).isAir(), "the corner chunk two out is restored too");
            release(helper, center.getPos());
        });
    }

    //region far chunks [远区块]
    static LevelChunk farChunk(GameTestHelper helper, int slot) {
        ChunkPos pos = new ChunkPos(helper.absolutePos(BlockPos.ZERO).offset(FAR_BLOCKS * slot, 0, 0));
        ServerLevel level = helper.getLevel();
        forceAround(level, pos, true);
        return level.getChunk(pos.x, pos.z);
    }

    static void release(GameTestHelper helper, ChunkPos pos) {
        forceAround(helper.getLevel(), pos, false);
    }

    private static void forceAround(ServerLevel level, ChunkPos pos, boolean forced) {
        int radius = ChunkRewindService.RESTORE_RADIUS;
        for (int x = pos.x - radius; x <= pos.x + radius; x++) {
            for (int z = pos.z - radius; z <= pos.z + radius; z++) level.setChunkForced(x, z, forced);
        }
    }

    static BlockPos surface(LevelChunk chunk, int x, int z) {
        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        return chunk.getPos().getBlockAt(x, y + 1, z);
    }

    static ProtoChunk copyOf(ServerLevel level, LevelChunk chunk) {
        ProtoChunk origin = new ProtoChunk(chunk.getPos(), UpgradeData.EMPTY, level,
                level.registryAccess().registryOrThrow(Registries.BIOME), null);
        for (int index = 0; index < chunk.getSectionsCount(); index++) {
            LevelChunkSection live = chunk.getSection(index);
            origin.getSections()[index] = new LevelChunkSection(ownedCopy(live.getStates()), live.getBiomes());
        }
        return origin;
    }

    private static PalettedContainer<BlockState> ownedCopy(PalettedContainer<BlockState> states) {
        BlockState air = Blocks.AIR.defaultBlockState();
        PalettedContainer<BlockState> copy = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, air,
                PalettedContainer.Strategy.SECTION_STATES);
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState state = states.get(x, y, z);
                    if (state != air) copy.getAndSetUnchecked(x, y, z, state);
                }
            }
        }
        return copy;
    }
    //endregion
}
