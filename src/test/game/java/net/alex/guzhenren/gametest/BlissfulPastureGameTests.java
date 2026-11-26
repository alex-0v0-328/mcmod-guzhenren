package net.alex.guzhenren.gametest;

import java.util.List;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.blissful.BlissfulLand;
import net.alex.guzhenren.gameplay.blissful.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.herd.HerdWork;
import net.alex.guzhenren.gameplay.blissful.herd.PastureWork;
import net.alex.guzhenren.gameplay.blissful.mountain.MountainWork;
import net.alex.guzhenren.gameplay.secretrealm.RealmSite;
import net.alex.guzhenren.gameplay.secretrealm.SoulsunderMountain;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 福地牧场：圈地放栅栏与门、挡住的格子留着、再按只补缺；收拢把栏外可支使的动物传进栏并开冷却；养殖清点只算栏内；挪生物上山
 * 跳过栏内。牧场都取测试区里的小方格，不碰别的测试。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlissfulPastureGameTests {

    private BlissfulPastureGameTests() {}

    private static PastureWork.Pasture pasture(GameTestHelper helper, int min, int max) {
        BlockPos low = helper.absolutePos(new BlockPos(min, 0, min));
        BlockPos high = helper.absolutePos(new BlockPos(max, 0, max));
        return new PastureWork.Pasture(Math.min(low.getX(), high.getX()), Math.min(low.getZ(), high.getZ()),
                Math.max(low.getX(), high.getX()), Math.max(low.getZ(), high.getZ()));
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void enclosingLaysAJoinedFenceWithAGateAndOnlyFillsGaps(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = GameTestPlayers.survival(helper, "gzr-land-enclose-mock").onGround();
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        PastureWork.Pasture pasture = pasture(helper, 1, 7);
        int fenceY = helper.absolutePos(BlockPos.ZERO).getY() + 1;
        BlockPos pool = new BlockPos(pasture.maxX(), fenceY, pasture.maxZ() - 2);
        BlockPos wall = new BlockPos(pasture.minX(), fenceY, pasture.maxZ() - 2);
        level.setBlockAndUpdate(pool, Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(wall, Blocks.STONE_BRICKS.defaultBlockState());
        try {
            PastureWork.Enclosed first = PastureWork.enclose(level, data, pasture, owner);
            helper.assertTrue(first != null, "the owner may enclose");
            helper.assertValueEqual(first, new PastureWork.Enclosed(23, 1),
                    "a seven by seven ring is 24 cells, the water one blocked");
            helper.assertTrue(level.getBlockState(wall).is(Blocks.STONE_BRICKS), "a player's block is kept");
            helper.assertTrue(level.getBlockState(wall.above()).is(Blocks.OAK_FENCE), "the fence sits on top of it");
            helper.assertTrue(level.getBlockState(pasture.gate().atY(fenceY)).is(BlockTags.FENCE_GATES),
                    "the north middle is a gate");
            BlockState corner = level.getBlockState(new BlockPos(pasture.minX(), fenceY, pasture.minZ()));
            helper.assertTrue(corner.is(Blocks.OAK_FENCE) && corner.getValue(FenceBlock.EAST)
                    && corner.getValue(FenceBlock.SOUTH), "the north-west corner joins both of its sides");
            helper.assertTrue(data.herd().enclosed(), "the pasture counts as enclosed");

            level.setBlockAndUpdate(pool, Blocks.AIR.defaultBlockState());
            helper.assertValueEqual(PastureWork.enclose(level, data, pasture, owner), new PastureWork.Enclosed(1, 0),
                    "enclosing again fills only the gap");
            helper.succeed();
        } finally {
            PastureWork.forceChunks(level, pasture, false);
        }
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void gatheringPensLooseAnimalsAndStartsTheCooldown(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        PastureWork.Pasture pasture = pasture(helper, 0, 5);
        Cow loose = helper.spawn(EntityType.COW, new BlockPos(8, 1, 8));
        Cow named = helper.spawn(EntityType.COW, new BlockPos(7, 1, 8));
        named.setCustomName(Component.literal("Named"));
        Cow penned = helper.spawn(EntityType.COW, new BlockPos(2, 1, 2));

        helper.assertFalse(pasture.contains(loose), "the loose cow starts outside");
        int moved = PastureWork.gather(level, data, pasture, level.getRandom(), List.of(loose, named, penned));
        helper.assertValueEqual(moved, 1, "only the unnamed loose cow is gathered");
        helper.assertTrue(pasture.contains(loose), "the loose cow now stands in the pasture");
        helper.assertFalse(pasture.contains(named), "a named cow stays put");
        helper.assertValueEqual(PastureWork.gatherCooldown(level, data), (long) PastureWork.GATHER_COOLDOWN_TICKS,
                "gathering starts the sixty-second cooldown");

        Map<ResourceLocation, List<Animal>> herds = PastureWork.penned(HerdWork.census(List.of(loose, named, penned)),
                pasture);
        helper.assertValueEqual(PastureWork.count(herds), 2, "the census keeps only the penned cows");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void movingMobsUpLeavesThePastureAlone(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(4, 1, 4));
        ChunkPos at = cow.chunkPosition();
        BlissfulLand land = new BlissfulLand("gametest_land", level.dimension(), SoulsunderMountain.INSTANCE, 0,
                BlockPos.ZERO, 64, 0, new ChunkPos(0, 0), 1, at, 1);
        BlissfulLandData data = BlissfulLandData.create(land);
        RealmSite site = new RealmSite(helper.absolutePos(new BlockPos(4, 1, 4)).offset(64, 0, 64), 2);

        int moved = MountainWork.moveMobsUp(level, site, data, level.getRandom(), List.of(cow));
        helper.assertValueEqual(moved, 0, "a cow in the pasture is not moved up the mountain");
        helper.succeed();
    }
}
