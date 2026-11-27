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
 * 福地地图[Land Map]的区块扫描：地面高与表面高跟真实方块一致（树叶、栅栏、作物、花、甘蔗、火把、地毯、雪层跳过，台阶
 * 仍算地面）、悬空树冠、树干与贴地灌木、流体列与方块实体列的标志、地貌归类，以及已割弃区块给整片虚空。只扫测试区所在的主世界区块，不碰福地维度。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlissfulMapGameTests {

    private BlissfulMapGameTests() {}

    private static MapTile tileAt(GameTestHelper helper, BlockPos relative) {
        return MapScanner.scan(helper.getLevel().getChunkAt(helper.absolutePos(relative)));
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
        helper.assertValueEqual(tile.surfaceAt(leafy.getX(), leafy.getZ()), leafy.getY() + 2,
                "the surface is the leaves");
        helper.assertValueEqual(tile.terrainAt(leafy.getX(), leafy.getZ()), MapTerrain.FOLIAGE,
                "the surface reads as foliage");

        BlockPos fence = helper.absolutePos(new BlockPos(3, 0, 3));
        tile = tileAt(helper, new BlockPos(3, 0, 3));
        helper.assertValueEqual(tile.groundAt(fence.getX(), fence.getZ()), fence.getY(),
                "a two-high fence is skipped down to the stone floor");
        helper.assertValueEqual(tile.surfaceAt(fence.getX(), fence.getZ()), fence.getY() + 2, "the fence top");
        helper.assertValueEqual(tile.terrainAt(fence.getX(), fence.getZ()), MapTerrain.FENCE, "fences have a class");

        BlockPos crop = helper.absolutePos(new BlockPos(6, 2, 2));
        tile = tileAt(helper, new BlockPos(6, 2, 2));
        helper.assertValueEqual(tile.terrainAt(crop.getX(), crop.getZ()), MapTerrain.FARMLAND,
                "ripe wheat reads as the field it grows in");
        helper.assertValueEqual(tile.groundAt(crop.getX(), crop.getZ()), crop.getY() - 1,
                "a crop is a plant, so the farmland under it is the ground");

        for (BlockPos relative : new BlockPos[] {new BlockPos(5, 1, 5), new BlockPos(7, 1, 7)}) {
            BlockPos plant = helper.absolutePos(relative);
            tile = tileAt(helper, relative);
            helper.assertValueEqual(tile.groundAt(plant.getX(), plant.getZ()), plant.getY() - 1,
                    "a flower or a sugar cane stands on the ground, it is not the ground");
        }

        BlockPos sand = helper.absolutePos(new BlockPos(2, 1, 6));
        tile = tileAt(helper, new BlockPos(2, 1, 6));
        helper.assertValueEqual(tile.terrainAt(sand.getX(), sand.getZ()), MapTerrain.SAND, "sand reads as sand");
        helper.assertValueEqual(tile.groundAt(sand.getX(), sand.getZ()), sand.getY(), "sand is the ground");

        ServerLevel level = helper.getLevel();
        LevelChunk chunk = level.getChunkAt(leafy);
        MapTile whole = MapScanner.scan(chunk);
        for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
            for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                helper.assertValueEqual(whole.surfaceAt(x, z), height,
                        "every surface matches the world height map at " + x + ", " + z);
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
        helper.assertValueEqual(tile.lowAt(x, z), floating.getY() + 3, "the canopy floats from its lowest leaf");
        helper.assertValueEqual(tile.underAt(x, z), MapTerrain.GRASS, "the ground under the canopy reads as grass");
        helper.assertFalse(tile.hasFlag(x, z, MapTile.FLAG_TRUNK), "air under a canopy is no trunk");

        BlockPos tree = helper.absolutePos(new BlockPos(4, 0, 4));
        tile = tileAt(helper, new BlockPos(4, 0, 4));
        helper.assertValueEqual(tile.groundAt(tree.getX(), tree.getZ()), tree.getY(), "logs are skipped to the floor");
        helper.assertValueEqual(tile.lowAt(tree.getX(), tree.getZ()), tree.getY() + 4, "the canopy sits on the trunk");
        helper.assertTrue(tile.hasFlag(tree.getX(), tree.getZ(), MapTile.FLAG_TRUNK), "three logs make a trunk");

        BlockPos bush = helper.absolutePos(new BlockPos(7, 0, 1));
        tile = tileAt(helper, new BlockPos(7, 0, 1));
        helper.assertValueEqual(tile.lowAt(bush.getX(), bush.getZ()), bush.getY() + 1, "a bush rests on the floor");

        BlockPos[] thin = { new BlockPos(1, 0, 7), new BlockPos(3, 0, 7), new BlockPos(5, 0, 7) };
        MapTerrain[] reads = { MapTerrain.STONE, MapTerrain.STONE, MapTerrain.SNOW };
        for (int i = 0; i < thin.length; i++) {
            BlockPos at = helper.absolutePos(thin[i]);
            tile = tileAt(helper, thin[i]);
            helper.assertValueEqual(tile.groundAt(at.getX(), at.getZ()), at.getY(),
                    "a torch, a carpet or a snow layer is not the ground");
            helper.assertValueEqual(tile.terrainAt(at.getX(), at.getZ()), reads[i],
                    "a torch and a carpet take the floor colour, a snow layer stays snow");
        }
        BlockPos slab = helper.absolutePos(new BlockPos(7, 1, 7));
        tile = tileAt(helper, new BlockPos(7, 1, 7));
        helper.assertValueEqual(tile.groundAt(slab.getX(), slab.getZ()), slab.getY(), "a slab is still ground");
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
        helper.assertValueEqual(tile.terrainAt(water.getX(), water.getZ()), MapTerrain.WATER, "water reads as water");

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
