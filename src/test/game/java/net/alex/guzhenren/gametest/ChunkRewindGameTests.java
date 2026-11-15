package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.rewind.ChunkOriginGenerator;
import net.alex.guzhenren.gameplay.rewind.ChunkRewindService;
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
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 山如故[Mountain As Before Gu]、江如故[River As Before Gu]背后的裸地：恢复只写异于原貌的方块并清除放置的箱子；装饰过滤
 * 留岩石重着色，去树、植物、雪、冰、湖、矿石；按种子重生成的区块复原改动地表。恢复改写整区块会抹掉相邻测试，故都在远离
 * 测试网格的区块上跑（{@link #farChunk}：测试以东 {@code slot} × 4096 格），测试期间强制加载、结束释放。
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

    @GameTest(template = "empty9x9x9", timeoutTicks = 400)
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

    @GameTest(template = "empty9x9x9", timeoutTicks = 400)
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

    private static ProtoChunk copyOf(ServerLevel level, LevelChunk chunk) {
        ProtoChunk origin = new ProtoChunk(chunk.getPos(), UpgradeData.EMPTY, level,
                level.registryAccess().registryOrThrow(Registries.BIOME), null);
        for (int index = 0; index < chunk.getSectionsCount(); index++) {
            LevelChunkSection live = chunk.getSection(index);
            origin.getSections()[index] = new LevelChunkSection(live.getStates().copy(), live.getBiomes());
        }
        return origin;
    }
    //endregion
}
