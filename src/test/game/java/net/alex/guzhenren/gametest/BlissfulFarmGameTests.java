package net.alex.guzhenren.gametest;

import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.blissful.BlissfulLand;
import net.alex.guzhenren.gameplay.blissful.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.farm.FarmModule;
import net.alex.guzhenren.gameplay.blissful.farm.FarmWork;
import net.alex.guzhenren.gameplay.blissful.farm.FieldPlans;
import net.alex.guzhenren.gameplay.blissful.farm.FieldPlot;
import net.alex.guzhenren.gameplay.secretrealm.SoulsunderMountain;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 福地标准田：开垦取列数最多的地面层（花不算地面）、铺土径、水与耕地并按开垦作物播种，收割补种与托管，拆田收作物换回泥
 * 土，规划队列按序开垦、不合格取消、主人不在线保留。田都取测试区里的小田（边长 9 或 7，同整区块田的布局），不碰别的测试；
 * 自建存档，规划用例另建疆域罩住测试区的福地。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlissfulFarmGameTests {

    private static final int WHOLE = 9;
    private static final int FARMLAND_CELLS = 45;

    private BlissfulFarmGameTests() {}

    private static FieldPlot plot(GameTestHelper helper, int min, int max) {
        BlockPos low = helper.absolutePos(new BlockPos(min, 0, min));
        BlockPos high = helper.absolutePos(new BlockPos(max, 0, max));
        return new FieldPlot(Math.min(low.getX(), high.getX()), low.getY(), Math.min(low.getZ(), high.getZ()),
                max - min + 1);
    }

    private static BlockPos at(FieldPlot field, int localX, int dy, int localZ) {
        return new BlockPos(field.minX() + localX, field.y() + dy, field.minZ() + localZ);
    }

    private static ServerPlayer owner(GameTestHelper helper, String name) {
        return GameTestPlayers.survival(helper, name).at(new Vec3(4.5D, 3.0D, 4.5D));
    }

    private static void ripen(ServerLevel level, FieldPlot field, Block block) {
        BlockState ripe = ((CropBlock) block).getStateForAge(((CropBlock) block).getMaxAge());
        for (BlockPos pos : field.cells()) level.setBlock(pos, ripe, Block.UPDATE_CLIENTS);
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void reclaimingLaysPathsWaterAndFarmlandAndHarvestReplants(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        data.storage().insert(new ItemStack(Items.WHEAT_SEEDS, 100));
        ServerPlayer owner = owner(helper, "gzr-farm-lay-mock");
        FieldPlot plot = plot(helper, 0, WHOLE - 1);
        FieldPlot field = null;
        try {
            helper.setBlock(new BlockPos(4, 1, 4), Blocks.WATER);
            helper.assertTrue(FarmWork.reclaim(level, data, plot, owner) == null,
                    "a pool on the plot is not reclaimed");
            helper.setBlock(new BlockPos(4, 1, 4), Blocks.AIR);
            helper.assertFalse(FarmWork.isSeed(new ItemStack(Items.TORCHFLOWER_SEEDS)), "other crop seeds are refused");
            field = FarmWork.reclaim(level, data, plot, owner);
            helper.assertTrue(field != null, "the field is laid");
            helper.assertValueEqual(field.y(), plot.y(), "the layer is the commonest ground");
            helper.assertTrue(level.getBlockState(at(field, 0, 0, 0)).is(Blocks.DIRT_PATH), "the rim is a path");
            helper.assertTrue(level.getBlockState(at(field, 2, 0, 6)).is(Blocks.WATER), "a quarter line is water");
            helper.assertTrue(level.getBlockState(at(field, 1, 0, 1)).is(Blocks.FARMLAND), "the inside is farmland");
            helper.assertTrue(level.getBlockState(at(field, 7, 1, 7)).is(Blocks.WHEAT), "the farmland is sown");
            helper.assertValueEqual(field.cells().size(), FARMLAND_CELLS, "seven by seven less four water holes");
            helper.assertValueEqual(data.storage().count(stack -> stack.is(Items.WHEAT_SEEDS)),
                    100L - FARMLAND_CELLS, "one seed per farmland cell");
            helper.assertTrue(FarmWork.reclaimCooldown(level, data) > 0, "reclaiming starts the cooldown");

            ripen(level, field, Blocks.WHEAT);
            helper.assertValueEqual(FarmWork.census(level, data).get(Blocks.WHEAT),
                    new FarmWork.Count(FARMLAND_CELLS, 0), "every ripe wheat is counted");
            helper.assertValueEqual(FarmWork.harvest(level, data, Integer.MAX_VALUE).reaped(), FARMLAND_CELLS,
                    "all are cut");
            helper.assertValueEqual(FarmWork.census(level, data).get(Blocks.WHEAT),
                    new FarmWork.Count(0, FARMLAND_CELLS), "every cell is replanted");
            helper.succeed();
        } finally {
            if (field != null) FarmWork.setForced(level, field, false);
        }
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void reclaimingLevelsToTheCommonestGroundAndSkipsPlants(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        for (int z = 0; z < WHOLE; z++) {
            level.setBlock(helper.absolutePos(new BlockPos(0, 1, z)), Blocks.POPPY.defaultBlockState(), quiet);
            for (int x = 4; x < WHOLE; x++) helper.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
            for (int x = 6; x < WHOLE; x++) helper.setBlock(new BlockPos(x, 2, z), Blocks.DIRT);
        }
        FieldPlot plot = plot(helper, 0, WHOLE - 1);
        FieldPlot field = FarmWork.reclaim(level, data, plot, owner(helper, "gzr-farm-layer-mock"));
        try {
            helper.assertTrue(field != null, "the field is laid");
            helper.assertValueEqual(field.y(), plot.y(),
                    "36 columns on the floor beat 27 two up and 18 one up, flowers not counted as ground");
            helper.assertTrue(level.getBlockState(at(field, 0, 0, 3)).is(Blocks.DIRT_PATH), "the path is on the floor");
            helper.assertTrue(level.getBlockState(at(field, 0, 1, 3)).isAir(), "the flower is cleared");
            helper.assertTrue(level.getBlockState(at(field, 7, 0, 7)).is(Blocks.FARMLAND), "the high side is cut down");
            helper.assertTrue(level.getBlockState(at(field, 7, 1, 7)).isAir()
                    && level.getBlockState(at(field, 7, 2, 7)).isAir(), "nothing is left above the cut");
            helper.succeed();
        } finally {
            if (field != null) FarmWork.setForced(level, field, false);
        }
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void aFieldLaidWithoutSeedsIsSownLater(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        FieldPlot field = FarmWork.reclaim(level, data, plot(helper, 0, WHOLE - 1), owner(helper, "gzr-farm-sow-mock"));
        try {
            helper.assertTrue(field != null, "the field is laid with an empty storage");
            helper.assertTrue(FarmWork.census(level, data).isEmpty(), "nothing is sown without seeds");

            data.storage().insert(new ItemStack(Items.CARROT, 30));
            helper.assertValueEqual(FarmWork.harvest(level, data, Integer.MAX_VALUE).sown(), 30,
                    "thirty carrots sow thirty");
            data.storage().insert(new ItemStack(Items.WHEAT_SEEDS, 100));
            data.storage().insert(new ItemStack(Items.CARROT, 100));
            BlockPos trampled = field.cells().getLast().below();
            level.setBlock(trampled, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            helper.assertValueEqual(FarmWork.harvest(level, data, 0), new FarmWork.Harvest(0, 0, 0),
                    "no field in the budget, nothing is sown");
            helper.assertValueEqual(FarmWork.harvest(level, data, 1), new FarmWork.Harvest(0, FARMLAND_CELLS - 30, 1),
                    "one field in the budget is filled whole");
            helper.assertValueEqual(FarmWork.census(level, data).get(Blocks.CARROTS),
                    new FarmWork.Count(0, FARMLAND_CELLS), "the field fills with its own crop, trampled cell included");
            helper.assertTrue(level.getBlockState(trampled).is(Blocks.FARMLAND), "trampled farmland is tilled again");
            helper.assertValueEqual(data.storage().count(stack -> stack.is(Items.WHEAT_SEEDS)), 100L,
                    "seeds of another crop are left alone");
            helper.succeed();
        } finally {
            if (field != null) FarmWork.setForced(level, field, false);
        }
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void reclaimingSowsTheChosenCropAndHostingReportsTheRound(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        data.storage().insert(new ItemStack(Items.WHEAT_SEEDS, 100));
        data.storage().insert(new ItemStack(Items.CARROT, 100));
        data.farm().setCrop(FarmWork.CROPS.indexOf(Blocks.CARROTS));
        FieldPlot field = FarmWork.reclaim(level, data, plot(helper, 0, WHOLE - 1),
                owner(helper, "gzr-farm-crop-mock"));
        try {
            helper.assertTrue(field != null, "the field is laid");
            helper.assertValueEqual(FarmWork.census(level, data).get(Blocks.CARROTS),
                    new FarmWork.Count(0, FARMLAND_CELLS), "the chosen carrots are sown, not the first seed");
            helper.assertValueEqual(data.storage().count(stack -> stack.is(Items.WHEAT_SEEDS)), 100L,
                    "the wheat seeds stay");

            BlockState ripe = ((CropBlock) Blocks.CARROTS).getStateForAge(((CropBlock) Blocks.CARROTS).getMaxAge());
            for (int localX = 1; localX <= 5; localX++) level.setBlock(at(field, localX, 1, 1), ripe, 2);
            data.farm().setHosting(true);
            helper.assertValueEqual(FarmModule.INSTANCE.host(level, data, null), 1,
                    "hosting works the one field with ripe carrots");
            helper.assertValueEqual(data.farm().lastReaped(), 5, "the round is reported");
            helper.assertValueEqual(data.farm().lastSown(), 0, "nothing was bare");
            helper.succeed();
        } finally {
            if (field != null) FarmWork.setForced(level, field, false);
        }
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void removingAFieldStoresEveryCropAndTurnsTheLayerBackToDirt(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        data.storage().insert(new ItemStack(Items.WHEAT_SEEDS, FARMLAND_CELLS));
        ServerPlayer owner = owner(helper, "gzr-farm-remove-mock");
        FieldPlot field = FarmWork.reclaim(level, data, plot(helper, 0, WHOLE - 1), owner);
        helper.assertTrue(field != null, "the field is laid");
        ripen(level, field, Blocks.WHEAT);

        helper.assertValueEqual(FarmWork.remove(level, data, field, owner), FARMLAND_CELLS, "every crop is taken");
        helper.assertTrue(data.storage().count(stack -> stack.is(Items.WHEAT)) >= FARMLAND_CELLS,
                "the wheat goes to storage");
        helper.assertTrue(data.farm().fields().isEmpty(), "the field is struck off");
        for (BlockPos cell : new BlockPos[] {at(field, 0, 0, 0), at(field, 2, 0, 2), at(field, 1, 0, 1)}) {
            helper.assertTrue(level.getBlockState(cell).is(Blocks.DIRT), "path, water and farmland turn to dirt");
        }
        helper.assertTrue(level.getBlockState(at(field, 1, 1, 1)).isAir(), "no crop is left standing");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void plansAreReclaimedInTurnAndAnUnfitOneIsCancelled(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = owner(helper, "gzr-farm-plan-mock");
        FieldPlot plot = plot(helper, 1, 7);
        ChunkPos first = plot.chunk();
        int half = Math.max(Math.abs(first.x), Math.abs(first.z)) + 4;
        BlissfulLandData data = BlissfulLandData.create(new BlissfulLand("gametest_land", level.dimension(),
                SoulsunderMountain.INSTANCE, half, 0));
        ChunkPos second = new ChunkPos(first.x + 1, first.z);
        data.farm().addPlan(first.toLong());
        data.farm().addPlan(second.toLong());
        try {
            FieldPlans.Step reclaimed = FieldPlans.step(level, data, owner, chunk -> plot);
            helper.assertTrue(reclaimed != null && reclaimed.reclaimed(), "the head of the queue is reclaimed");
            helper.assertValueEqual(data.farm().fields().size(), 1, "the field is recorded");
            helper.assertValueEqual(data.farm().plans().size(), 1, "the queue moves on");

            FieldPlans.Step cancelled = FieldPlans.step(level, data, owner, chunk -> plot);
            helper.assertTrue(cancelled != null && !cancelled.reclaimed(),
                    "the next plan lands on water holes and is cancelled");
            helper.assertTrue(data.farm().plans().isEmpty(), "a cancelled plan leaves the queue");
            helper.assertValueEqual(data.farm().fields().size(), 1, "no second field is laid");
            helper.assertTrue(FieldPlans.step(level, data, owner, chunk -> plot) == null,
                    "an empty queue does nothing");

            data.setOwner(UUID.randomUUID(), "gzr-farm-away");
            data.farm().setReclaimReadyAt(0L);
            data.farm().addPlan(second.toLong());
            FieldPlans.tick(level, data);
            helper.assertValueEqual(data.farm().plans().size(), 1, "the queue waits while the owner is away");
            helper.succeed();
        } finally {
            FarmWork.release(level, data);
        }
    }
}
