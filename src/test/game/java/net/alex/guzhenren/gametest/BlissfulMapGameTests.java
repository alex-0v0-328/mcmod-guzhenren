package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapScanner;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTerrain;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTile;
import net.alex.guzhenren.gameplay.immortalaperture.state.Territory;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 福地地图[Land Map]的区块扫描：地面高跟真实方块一致（树叶、栅栏、作物、花、甘蔗、火把、地毯、雪层跳过，台阶仍算地面），
 * 每列的段只记真实方块（悬空方块、悬空树冠下的空气不成段，树干、灌木、栅栏各成一段，薄件压平给下面那段上色），流体列与
 * 方块实体列的标志、地貌归类，以及已割弃区块给整片虚空。只扫测试区所在的主世界区块，不碰福地维度。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlissfulMapGameTests {

    private BlissfulMapGameTests() {}

    private static MapTile tileAt(GameTestHelper helper, BlockPos relative) {
        return MapScanner.scan(helper.getLevel().getChunkAt(helper.absolutePos(relative)));
    }

    private static int index(BlockPos absolute) { return MapTile.index(absolute.getX(), absolute.getZ()); }

    private static void assertRun(GameTestHelper helper, MapTile tile, BlockPos absolute, int k, int top,
                                  MapTerrain terrain, String what) {
        int index = index(absolute);
        helper.assertTrue(tile.runs(index) > k, what + ": run " + k + " exists");
        int run = tile.run(index, k);
        helper.assertValueEqual(tile.runTop(run), top, what + ": run " + k + " top");
        helper.assertValueEqual(tile.runTerrain(run), terrain, what + ": run " + k + " terrain");
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void scanMatchesTheBlocksBelowIt(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.GRASS_BLOCK);
        helper.setBlock(new BlockPos(1, 3, 1), Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, true));
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.OAK_FENCE);
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.OAK_FENCE);
        helper.setBlock(new BlockPos(6, 1, 2), Blocks.FARMLAND);
        helper.setBlock(new BlockPos(6, 2, 2), ((CropBlock) Blocks.WHEAT).getStateForAge(CropBlock.MAX_AGE));
        helper.setBlock(new BlockPos(2, 1, 6), Blocks.SAND);
        ServerLevel world = helper.getLevel();
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        world.setBlock(helper.absolutePos(new BlockPos(5, 1, 5)), Blocks.POPPY.defaultBlockState(), quiet);
        for (int y = 1; y <= 3; y++) {
            world.setBlock(helper.absolutePos(new BlockPos(7, y, 7)), Blocks.SUGAR_CANE.defaultBlockState(), quiet);
        }

        BlockPos leafy = helper.absolutePos(new BlockPos(1, 1, 1));
        MapTile tile = tileAt(helper, new BlockPos(1, 1, 1));
        helper.assertValueEqual(tile.groundAt(leafy.getX(), leafy.getZ()), leafy.getY(),
                "the ground is the grass under the floating leaves");
        assertRun(helper, tile, leafy, 0, leafy.getY() + 2, MapTerrain.FOLIAGE, "the leaves are a run of their own");
        helper.assertValueEqual(tile.runBottom(tile.run(index(leafy), 0)), leafy.getY() + 2, "one leaf high");
        assertRun(helper, tile, leafy, 1, leafy.getY(), MapTerrain.GRASS, "the grass under the gap");

        BlockPos fence = helper.absolutePos(new BlockPos(3, 0, 3));
        tile = tileAt(helper, new BlockPos(3, 0, 3));
        helper.assertValueEqual(tile.groundAt(fence.getX(), fence.getZ()), fence.getY(),
                "a two-high fence is skipped down to the stone floor");
        assertRun(helper, tile, fence, 0, fence.getY() + 2, MapTerrain.FENCE, "the fence stands as its own run");
        assertRun(helper, tile, fence, 1, fence.getY(), MapTerrain.STONE, "the floor under the fence");

        BlockPos crop = helper.absolutePos(new BlockPos(6, 2, 2));
        tile = tileAt(helper, new BlockPos(6, 2, 2));
        assertRun(helper, tile, crop, 0, crop.getY() - 1, MapTerrain.FARMLAND, "ripe wheat lies flat on its field");
        helper.assertValueEqual(tile.groundAt(crop.getX(), crop.getZ()), crop.getY() - 1,
                "a crop is a plant, so the farmland under it is the ground");

        for (BlockPos relative : new BlockPos[] {new BlockPos(5, 1, 5), new BlockPos(7, 1, 7)}) {
            BlockPos plant = helper.absolutePos(relative);
            tile = tileAt(helper, relative);
            helper.assertValueEqual(tile.groundAt(plant.getX(), plant.getZ()), plant.getY() - 1,
                    "a flower or a sugar cane stands on the ground, it is not the ground");
            assertRun(helper, tile, plant, 0, plant.getY() - 1, MapTerrain.STONE, "a flower or a cane lies flat");
        }

        BlockPos sand = helper.absolutePos(new BlockPos(2, 1, 6));
        tile = tileAt(helper, new BlockPos(2, 1, 6));
        assertRun(helper, tile, sand, 0, sand.getY(), MapTerrain.SAND, "sand reads as sand");
        helper.assertValueEqual(tile.groundAt(sand.getX(), sand.getZ()), sand.getY(), "sand is the ground");

        ServerLevel level = helper.getLevel();
        LevelChunk chunk = level.getChunkAt(leafy);
        MapTile whole = MapScanner.scan(chunk);
        for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
            for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                int index = MapTile.index(x, z);
                helper.assertTrue(whole.runs(index) == 0 || whole.runTop(whole.run(index, 0)) <= height,
                        "no run rises over the world height map at " + x + ", " + z);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void canopiesFloatOverTheirGroundAndThinBlocksLieFlat(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.GRASS_BLOCK);
        helper.setBlock(new BlockPos(1, 4, 1), leaves);
        helper.setBlock(new BlockPos(1, 5, 1), leaves);
        for (int y = 1; y <= 3; y++) helper.setBlock(new BlockPos(4, y, 4), Blocks.OAK_LOG);
        helper.setBlock(new BlockPos(4, 4, 4), leaves);
        helper.setBlock(new BlockPos(7, 1, 1), leaves);
        helper.setBlock(new BlockPos(1, 1, 7), Blocks.TORCH);
        helper.setBlock(new BlockPos(3, 1, 7), Blocks.RED_CARPET);
        helper.setBlock(new BlockPos(5, 1, 7), Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 4));
        helper.setBlock(new BlockPos(7, 1, 7), Blocks.OAK_SLAB);

        BlockPos floating = helper.absolutePos(new BlockPos(1, 1, 1));
        MapTile tile = tileAt(helper, new BlockPos(1, 1, 1));
        int x = floating.getX();
        int z = floating.getZ();
        helper.assertValueEqual(tile.groundAt(x, z), floating.getY(), "the grass under the canopy is the ground");
        assertRun(helper, tile, floating, 0, floating.getY() + 4, MapTerrain.FOLIAGE, "the canopy");
        helper.assertValueEqual(tile.runBottom(tile.run(index(floating), 0)), floating.getY() + 3,
                "the canopy floats from its lowest leaf");
        assertRun(helper, tile, floating, 1, floating.getY(), MapTerrain.GRASS, "the grass under the canopy");

        BlockPos tree = helper.absolutePos(new BlockPos(4, 0, 4));
        tile = tileAt(helper, new BlockPos(4, 0, 4));
        helper.assertValueEqual(tile.groundAt(tree.getX(), tree.getZ()), tree.getY(), "logs are skipped to the floor");
        assertRun(helper, tile, tree, 0, tree.getY() + 4, MapTerrain.FOLIAGE, "the canopy sits on the trunk");
        assertRun(helper, tile, tree, 1, tree.getY() + 3, MapTerrain.WOOD, "three logs make a trunk");
        helper.assertValueEqual(tile.runBottom(tile.run(index(tree), 1)), tree.getY() + 1, "the trunk stands on");
        assertRun(helper, tile, tree, 2, tree.getY(), MapTerrain.STONE, "the floor under the tree");

        BlockPos bush = helper.absolutePos(new BlockPos(7, 0, 1));
        tile = tileAt(helper, new BlockPos(7, 0, 1));
        assertRun(helper, tile, bush, 0, bush.getY() + 1, MapTerrain.FOLIAGE, "a bush rests on the floor");
        assertRun(helper, tile, bush, 1, bush.getY(), MapTerrain.STONE, "the floor under the bush");

        BlockPos[] thin = { new BlockPos(1, 0, 7), new BlockPos(3, 0, 7), new BlockPos(5, 0, 7) };
        MapTerrain[] reads = { MapTerrain.STONE, MapTerrain.STONE, MapTerrain.SNOW };
        for (int i = 0; i < thin.length; i++) {
            BlockPos at = helper.absolutePos(thin[i]);
            tile = tileAt(helper, thin[i]);
            helper.assertValueEqual(tile.groundAt(at.getX(), at.getZ()), at.getY(),
                    "a torch, a carpet or a snow layer is not the ground");
            assertRun(helper, tile, at, 0, at.getY(), reads[i],
                    "a torch and a carpet take the floor colour, a snow layer turns it snow");
        }
        BlockPos slab = helper.absolutePos(new BlockPos(7, 1, 7));
        tile = tileAt(helper, new BlockPos(7, 1, 7));
        helper.assertValueEqual(tile.groundAt(slab.getX(), slab.getZ()), slab.getY(), "a slab is still ground");
        helper.assertValueEqual(tile.runTop(tile.run(index(slab), 0)), slab.getY(), "the slab tops its run");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void floatingBlocksLeaveTheAirUnderThemEmpty(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        helper.setBlock(new BlockPos(4, 6, 4), Blocks.STONE);
        for (int y = 1; y <= 4; y++) helper.setBlock(new BlockPos(6, y, 6), Blocks.COBBLESTONE);
        helper.setBlock(new BlockPos(6, 2, 6), Blocks.AIR);

        BlockPos floating = helper.absolutePos(new BlockPos(4, 6, 4));
        MapTile tile = tileAt(helper, new BlockPos(4, 6, 4));
        int index = index(floating);
        helper.assertTrue(tile.runs(index) >= 2, "the floating stone and the floor");
        assertRun(helper, tile, floating, 0, floating.getY(), MapTerrain.STONE, "the floating stone");
        helper.assertValueEqual(tile.runBottom(tile.run(index, 0)), floating.getY(), "one block, no pillar under it");
        assertRun(helper, tile, floating, 1, floating.getY() - 6, MapTerrain.STONE, "the floor five blocks below");
        helper.assertValueEqual(tile.groundAt(floating.getX(), floating.getZ()), floating.getY(),
                "a floating stone is the ground for siting, as on the land itself");

        BlockPos pillar = helper.absolutePos(new BlockPos(6, 0, 6));
        tile = tileAt(helper, new BlockPos(6, 0, 6));
        index = index(pillar);
        assertRun(helper, tile, pillar, 0, pillar.getY() + 4, MapTerrain.STONE, "the pillar head");
        helper.assertValueEqual(tile.runBottom(tile.run(index, 0)), pillar.getY() + 3, "two blocks over the hole");
        assertRun(helper, tile, pillar, 1, pillar.getY() + 1, MapTerrain.STONE, "the pillar foot under the hole");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void fluidAndBlockEntityColumnsAreFlagged(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.WATER);
        helper.setBlock(new BlockPos(7, 1, 7), Blocks.CHEST);
        helper.setBlock(new BlockPos(7, 2, 7), Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, true));

        BlockPos water = helper.absolutePos(new BlockPos(4, 1, 4));
        MapTile tile = tileAt(helper, new BlockPos(4, 1, 4));
        helper.assertTrue(tile.hasFlag(water.getX(), water.getZ(), MapTile.FLAG_FLUID), "the water column is flagged");
        helper.assertValueEqual(tile.groundAt(water.getX(), water.getZ()), water.getY(), "ground is the water top");
        assertRun(helper, tile, water, 0, water.getY(), MapTerrain.WATER, "water reads as water");
        assertRun(helper, tile, water, 1, water.getY() - 1, MapTerrain.STONE, "the bed under the water");

        BlockPos chest = helper.absolutePos(new BlockPos(7, 1, 7));
        tile = tileAt(helper, new BlockPos(7, 1, 7));
        helper.assertTrue(tile.hasFlag(chest.getX(), chest.getZ(), MapTile.FLAG_BLOCK_ENTITY),
                "the chest column is flagged even under leaves");
        helper.assertFalse(tile.hasFlag(chest.getX(), chest.getZ(), MapTile.FLAG_FLUID), "a chest is not a fluid");

        BlockPos plain = helper.absolutePos(new BlockPos(0, 0, 0));
        tile = tileAt(helper, new BlockPos(0, 0, 0));
        helper.assertFalse(tile.hasFlag(plain.getX(), plain.getZ(), MapTile.FLAG_FLUID | MapTile.FLAG_BLOCK_ENTITY),
                "bare stone carries no flag");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void buriedBlockEntitiesOnlyCountNearTheGround(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.CHEST);
        helper.setBlock(new BlockPos(6, 1, 6), Blocks.CHEST);
        int depth = MapScanner.BLOCK_ENTITY_BELOW;
        for (int y = 2; y <= 2 + depth; y++) helper.setBlock(new BlockPos(2, y, 2), Blocks.STONE);
        for (int y = 2; y <= 1 + depth; y++) helper.setBlock(new BlockPos(6, y, 6), Blocks.STONE);

        BlockPos deep = helper.absolutePos(new BlockPos(2, 1, 2));
        MapTile tile = tileAt(helper, new BlockPos(2, 1, 2));
        helper.assertValueEqual(tile.groundAt(deep.getX(), deep.getZ()), deep.getY() + depth + 1,
                "the stone pillar is the ground");
        helper.assertFalse(tile.hasFlag(deep.getX(), deep.getZ(), MapTile.FLAG_BLOCK_ENTITY),
                "a chest nine blocks under the ground is a mine, not in the way");

        BlockPos edge = helper.absolutePos(new BlockPos(6, 1, 6));
        tile = tileAt(helper, new BlockPos(6, 1, 6));
        helper.assertTrue(tile.hasFlag(edge.getX(), edge.getZ(), MapTile.FLAG_BLOCK_ENTITY),
                "a chest eight blocks under the ground is still inside the fill depth");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void cededChunksAreWholeVoid(GameTestHelper helper) {
        LevelChunk chunk = helper.getLevel().getChunkAt(helper.absolutePos(new BlockPos(4, 1, 4)));
        ChunkPos pos = chunk.getPos();
        int half = Math.max(Math.abs(pos.x), Math.abs(pos.z)) + 4;
        Territory whole = Territory.whole(half, 0);
        Territory ceded = pos.x >= 0 ? whole.cede(Territory.Side.EAST, half - pos.x)
                : whole.cede(Territory.Side.WEST, pos.x + half + 1);

        helper.assertTrue(ceded.isCeded(pos.x, pos.z), "the test chunk sits in the ceded strip");
        MapTile cut = MapScanner.tile(ceded, chunk);
        helper.assertTrue(cut.isEmpty(), "a ceded chunk is all void");
        helper.assertValueEqual(cut.chunkX(), pos.x, "the void tile keeps its chunk");
        helper.assertFalse(MapScanner.tile(whole, chunk).isEmpty(), "the same chunk inside the land is scanned");
        helper.succeed();
    }
}
