package net.alex.guzhenren.gametest;

import java.util.List;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdState;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdWork;
import net.alex.guzhenren.gameplay.immortalaperture.herd.PastureWork;
import net.alex.guzhenren.gameplay.immortalaperture.soulsundermountain.SoulsunderMountainWork;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLand;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.secretrealm.RealmSite;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 福地牧场：圈地按邻列地面叠栅栏、门在北边正中、挡住的格子留着、再圈只补缺、花草树苗甘蔗被栅栏替换而不垫高；拆栅栏连玩
 * 家放的一并拆；迁移把旧栏动物全带进新栏；收拢把栏外可支使的动物传进栏并开冷却；养殖清点只算栏内；挪生物上山跳过栏内。
 * 牧场都取测试区里的小方格，不碰别的测试。
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
    public static void enclosingStacksFencesOverTheHighestNeighbourAndOnlyFillsGaps(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = GameTestPlayers.survival(helper, "gzr-land-enclose-mock").onGround();
        PastureWork.Pasture pasture = pasture(helper, 1, 7);
        int fenceY = helper.absolutePos(BlockPos.ZERO).getY() + 1;
        BlockPos pool = new BlockPos(pasture.maxX(), fenceY, pasture.maxZ() - 2);
        BlockPos wall = new BlockPos(pasture.minX(), fenceY, pasture.maxZ() - 2);
        BlockPos mound = new BlockPos(pasture.minX() - 1, fenceY, pasture.minZ() + 3);
        level.setBlockAndUpdate(pool, Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(wall, Blocks.STONE_BRICKS.defaultBlockState());
        level.setBlockAndUpdate(mound, Blocks.STONE.defaultBlockState());
        PastureWork.Enclosed first = PastureWork.enclose(level, pasture, owner);
        helper.assertTrue(first != null, "the owner may enclose");
        helper.assertValueEqual(first.blocked(), 1, "only the water column is blocked");
        helper.assertTrue(level.getBlockState(wall).is(Blocks.STONE_BRICKS), "a player's block is kept");
        helper.assertTrue(level.getBlockState(wall.above()).is(Blocks.OAK_FENCE), "the fence sits on top of it");
        BlockPos byMound = new BlockPos(pasture.minX(), fenceY, pasture.minZ() + 3);
        helper.assertTrue(level.getBlockState(byMound).is(Blocks.OAK_FENCE)
                && level.getBlockState(byMound.above()).is(Blocks.OAK_FENCE),
                "beside a raised neighbour it stacks two");
        BlockPos flat = new BlockPos(pasture.maxX(), fenceY, pasture.minZ() + 1);
        helper.assertTrue(level.getBlockState(flat).is(Blocks.OAK_FENCE) && level.getBlockState(flat.above()).isAir(),
                "on flat ground one fence is enough");
        helper.assertTrue(level.getBlockState(pasture.gate().atY(fenceY)).is(BlockTags.FENCE_GATES),
                "the north middle is a gate");
        BlockState corner = level.getBlockState(new BlockPos(pasture.minX(), fenceY, pasture.minZ()));
        helper.assertTrue(corner.is(Blocks.OAK_FENCE) && corner.getValue(FenceBlock.EAST)
                && corner.getValue(FenceBlock.SOUTH), "the north-west corner joins both of its sides");

        level.setBlockAndUpdate(pool, Blocks.AIR.defaultBlockState());
        helper.assertValueEqual(PastureWork.enclose(level, pasture, owner), new PastureWork.Enclosed(1, 0),
                "enclosing again fills only the gap");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void enclosingReplacesPlantsInsteadOfStandingOnThem(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = GameTestPlayers.survival(helper, "gzr-land-plants-mock").onGround();
        PastureWork.Pasture pasture = pasture(helper, 1, 7);
        int fenceY = helper.absolutePos(BlockPos.ZERO).getY() + 1;
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        BlockPos flower = new BlockPos(pasture.maxX(), fenceY, pasture.minZ() + 2);
        BlockPos sapling = new BlockPos(pasture.minX(), fenceY, pasture.maxZ() - 2);
        BlockPos rose = new BlockPos(pasture.minX() + 2, fenceY, pasture.maxZ());
        BlockPos cane = new BlockPos(pasture.maxX() - 2, fenceY, pasture.maxZ());
        BlockPos outside = new BlockPos(pasture.maxX() + 1, fenceY, pasture.minZ() + 4);
        level.setBlock(flower, Blocks.POPPY.defaultBlockState(), quiet);
        level.setBlock(sapling, Blocks.OAK_SAPLING.defaultBlockState(), quiet);
        level.setBlock(rose, Blocks.ROSE_BUSH.defaultBlockState(), quiet);
        level.setBlock(rose.above(), Blocks.ROSE_BUSH.defaultBlockState()
                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), quiet);
        for (int dy = 0; dy < 3; dy++) level.setBlock(cane.above(dy), Blocks.SUGAR_CANE.defaultBlockState(), quiet);
        level.setBlock(outside, Blocks.DANDELION.defaultBlockState(), quiet);

        PastureWork.Enclosed done = PastureWork.enclose(level, pasture, owner);
        helper.assertTrue(done != null, "the owner may enclose");
        helper.assertValueEqual(done.blocked(), 0, "no plant blocks the line");
        for (BlockPos pos : List.of(flower, sapling, rose, cane)) {
            helper.assertTrue(level.getBlockState(pos).is(Blocks.OAK_FENCE), "the fence replaces the plant at " + pos);
            helper.assertTrue(level.getBlockState(pos.above()).isAir(), "one fence on flat ground at " + pos);
        }
        helper.assertTrue(level.getBlockState(cane.above(2)).isAir(), "the cane above the fence is cleared too");
        BlockPos byOutside = new BlockPos(pasture.maxX(), fenceY, pasture.minZ() + 4);
        helper.assertTrue(level.getBlockState(byOutside.above()).isAir(), "a flower beside the line does not raise it");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void unfencingTakesDownEveryFenceOnTheLine(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = GameTestPlayers.survival(helper, "gzr-land-unfence-mock").onGround();
        PastureWork.Pasture pasture = pasture(helper, 1, 7);
        int fenceY = helper.absolutePos(BlockPos.ZERO).getY() + 1;
        PastureWork.Enclosed done = PastureWork.enclose(level, pasture, owner);
        helper.assertTrue(done != null, "the pasture is enclosed");
        BlockPos extra = new BlockPos(pasture.maxX(), fenceY + 1, pasture.minZ() + 1);
        level.setBlockAndUpdate(extra, Blocks.SPRUCE_FENCE.defaultBlockState());

        helper.assertValueEqual(PastureWork.unfence(level, pasture, owner), done.placed() + 1,
                "the player's fence on the line goes too");
        for (int x = pasture.minX(); x <= pasture.maxX(); x++) {
            for (int z = pasture.minZ(); z <= pasture.maxZ(); z++) {
                for (int y = fenceY; y <= fenceY + 1; y++) {
                    BlockState state = level.getBlockState(new BlockPos(x, y, z));
                    helper.assertFalse(state.is(BlockTags.FENCES) || state.is(BlockTags.FENCE_GATES),
                            "no fence is left");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void relocatingCarriesEveryPennedAnimalIntoTheNewPen(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        PastureWork.Pasture from = pasture(helper, 0, 1);
        PastureWork.Pasture to = pasture(helper, 3, 8);
        Cow penned = helper.spawn(EntityType.COW, new BlockPos(0, 1, 0));
        Cow named = helper.spawn(EntityType.COW, new BlockPos(1, 1, 0));
        named.setCustomName(Component.literal("Named"));
        Cow settled = helper.spawn(EntityType.COW, new BlockPos(6, 1, 6));
        helper.assertTrue(from.contains(penned) && !to.contains(penned), "the cow starts in the old pen only");

        int moved = PastureWork.relocate(level, from, to, level.getRandom(), List.of(penned, named, settled));
        helper.assertValueEqual(moved, 1, "only the unnamed cow of the old pen moves");
        helper.assertTrue(to.contains(penned), "it now stands in the new pen");
        helper.assertTrue(from.contains(named), "a named cow stays put");
        helper.succeed();
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
                List.of(pasture));
        helper.assertValueEqual(PastureWork.count(herds), 2, "the census keeps only the penned cows");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void movingMobsUpLeavesThePastureAlone(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(4, 1, 4));
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        HerdState.of(data).addPasture(cow.chunkPosition());
        RealmSite site = new RealmSite(helper.absolutePos(new BlockPos(4, 1, 4)).offset(64, 0, 64), 2);

        helper.assertTrue(PastureWork.Pasture.at(cow.chunkPosition().x, cow.chunkPosition().z).contains(cow),
                "a pasture's account is its whole two by two chunks");
        int moved = SoulsunderMountainWork.moveMobsUp(level, site, data, level.getRandom(), List.of(cow));
        helper.assertValueEqual(moved, 0, "a cow in the pasture is not moved up the mountain");
        helper.succeed();
    }
}
