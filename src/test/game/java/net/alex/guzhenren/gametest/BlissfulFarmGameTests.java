package net.alex.guzhenren.gametest;

import java.util.UUID;
import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmRules;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmState;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmWork;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FieldKind;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FieldPlans;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FieldPlot;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.MapPart;
import net.alex.guzhenren.gameplay.immortalaperture.plot.Siting;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLand;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.storage.BlissfulStorage;
import net.alex.guzhenren.gameplay.secretrealm.SoulsunderMountain;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.item.ModItems;
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
 * 福地标准田：开垦取列数最多的地面层（花不算地面）、铺土径、水与耕地并按新田作物播种，收割补种与托管，单田改作物熟一株
 * 换一株，蛊植田扣腐泥冻土换黑土、被踩补回、改回不退，拆田收作物换回泥土，规划队列按序开垦、不合格取消、主人不在线保留。田都取测试区里的小田（边长 9 或 7，同整区块田的布局），不碰别的测试；
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
        BlissfulStorage.of(data).insert(new ItemStack(Items.WHEAT_SEEDS, 100));
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
            helper.assertValueEqual(BlissfulStorage.of(data).count(stack -> stack.is(Items.WHEAT_SEEDS)),
                    100L - FARMLAND_CELLS, "one seed per farmland cell");

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

            BlissfulStorage.of(data).insert(new ItemStack(Items.CARROT, 30));
            helper.assertValueEqual(FarmWork.harvest(level, data, Integer.MAX_VALUE).sown(), 30,
                    "thirty carrots sow thirty");
            BlissfulStorage.of(data).insert(new ItemStack(Items.WHEAT_SEEDS, 100));
            BlissfulStorage.of(data).insert(new ItemStack(Items.CARROT, 100));
            BlockPos trampled = field.cells().getLast().below();
            level.setBlock(trampled, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            helper.assertValueEqual(FarmWork.harvest(level, data, 0), new FarmWork.Harvest(0, 0, 0),
                    "no field in the budget, nothing is sown");
            helper.assertValueEqual(FarmWork.harvest(level, data, 1), new FarmWork.Harvest(0, FARMLAND_CELLS - 30, 1),
                    "one field in the budget is filled whole");
            helper.assertValueEqual(FarmWork.census(level, data).get(Blocks.CARROTS),
                    new FarmWork.Count(0, FARMLAND_CELLS), "the field fills with its own crop, trampled cell included");
            helper.assertTrue(level.getBlockState(trampled).is(Blocks.FARMLAND), "trampled farmland is tilled again");
            helper.assertValueEqual(BlissfulStorage.of(data).count(stack -> stack.is(Items.WHEAT_SEEDS)), 100L,
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
        BlissfulStorage.of(data).insert(new ItemStack(Items.WHEAT_SEEDS, 100));
        BlissfulStorage.of(data).insert(new ItemStack(Items.CARROT, 100));
        FarmState.of(data).setCrop(FarmRules.crops().indexOf(Blocks.CARROTS));
        FieldPlot field = FarmWork.reclaim(level, data, plot(helper, 0, WHOLE - 1),
                owner(helper, "gzr-farm-crop-mock"));
        try {
            helper.assertTrue(field != null, "the field is laid");
            helper.assertValueEqual(FarmWork.census(level, data).get(Blocks.CARROTS),
                    new FarmWork.Count(0, FARMLAND_CELLS), "the chosen carrots are sown, not the first seed");
            helper.assertValueEqual(BlissfulStorage.of(data).count(stack -> stack.is(Items.WHEAT_SEEDS)), 100L,
                    "the wheat seeds stay");
            helper.assertValueEqual(FarmState.of(data).crop(field), FarmRules.crops().indexOf(Blocks.CARROTS),
                    "the new field keeps the new-field crop as its own");

            BlockState ripe = ((CropBlock) Blocks.CARROTS).getStateForAge(((CropBlock) Blocks.CARROTS).getMaxAge());
            for (int localX = 1; localX <= 5; localX++) level.setBlock(at(field, localX, 1, 1), ripe, 2);
            FarmState.of(data).setHosting(true);
            helper.assertValueEqual(FarmWork.host(level, data, null), 1,
                    "hosting works the one field with ripe carrots");
            helper.assertValueEqual(FarmState.of(data).lastReaped(), 5, "the round is reported");
            helper.assertValueEqual(FarmState.of(data).lastSown(), 0, "nothing was bare");
            helper.succeed();
        } finally {
            if (field != null) FarmWork.setForced(level, field, false);
        }
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void aFieldOfItsOwnCropSwapsRipePlantsOneByOne(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        BlissfulStorage.of(data).insert(new ItemStack(Items.WHEAT_SEEDS, FARMLAND_CELLS));
        FieldPlot field = FarmWork.reclaim(level, data, plot(helper, 0, WHOLE - 1),
                owner(helper, "gzr-farm-own-crop-mock"));
        try {
            helper.assertTrue(field != null, "the field is laid with wheat");
            Block grass = ModBlocks.INTIMATE_GRASS_CROP.get();
            FarmState.of(data).setCrop(field, FarmRules.crops().indexOf(grass));
            BlissfulStorage.of(data).insert(new ItemStack(ModItems.INTIMATE_GRASS_SEEDS.get(), 100));
            BlockState ripe = ((CropBlock) Blocks.WHEAT).getStateForAge(((CropBlock) Blocks.WHEAT).getMaxAge());
            for (int localX = 1; localX <= 5; localX++) level.setBlock(at(field, localX, 1, 1), ripe, 2);

            helper.assertValueEqual(FarmWork.harvest(level, data, Integer.MAX_VALUE).reaped(), 5,
                    "only the ripe wheat is cut");
            helper.assertValueEqual(FarmWork.census(level, data).get(grass), new FarmWork.Count(0, 5),
                    "each cut plant is replaced by the field's own crop");
            helper.assertValueEqual(FarmWork.census(level, data).get(Blocks.WHEAT),
                    new FarmWork.Count(0, FARMLAND_CELLS - 5), "growing wheat is left standing");
            helper.assertTrue(BlissfulStorage.of(data).count(stack -> stack.is(Items.WHEAT_SEEDS)) >= 5,
                    "the wheat seeds of the cut plants go to storage");
            helper.assertValueEqual(BlissfulStorage.of(data).count(stack -> stack.is(
                    ModItems.INTIMATE_GRASS_SEEDS.get())), 95L, "the grass seeds come from storage");
            helper.succeed();
        } finally {
            if (field != null) FarmWork.setForced(level, field, false);
        }
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void aGuPlantFieldCostsMudAndKeepsItsBlackSoil(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        BlissfulStorage storage = BlissfulStorage.of(data);
        storage.insert(new ItemStack(Items.WHEAT_SEEDS, FARMLAND_CELLS + 10));
        ServerPlayer owner = owner(helper, "gzr-farm-gu-plant-mock");
        FieldPlot field = FarmWork.reclaim(level, data, plot(helper, 0, WHOLE - 1), owner);
        Block blackSoil = ModBlocks.BLACK_SOIL_FARMLAND.get();
        try {
            helper.assertTrue(field != null, "the field is laid");
            helper.assertValueEqual(field.farmland(), FARMLAND_CELLS, "the cost is one mud per farmland cell");
            storage.insert(new ItemStack(ModItems.ROTTEN_FROZEN_MUD.get(), FARMLAND_CELLS - 1));
            helper.assertValueEqual(FarmWork.retype(level, data, field, FieldKind.GU_PLANT, owner),
                    FarmWork.Retype.SHORT, "one mud short refuses");
            helper.assertValueEqual(storage.count(FarmWork::isMud), (long) FARMLAND_CELLS - 1, "nothing is taken");
            helper.assertValueEqual(FarmState.of(data).kind(field), FieldKind.PLAIN, "the field stays plain");

            storage.insert(new ItemStack(ModItems.ROTTEN_FROZEN_MUD.get(), 1));
            helper.assertValueEqual(FarmWork.retype(level, data, field, FieldKind.GU_PLANT, owner),
                    FarmWork.Retype.CHANGED, "enough mud turns it");
            helper.assertValueEqual(storage.count(FarmWork::isMud), 0L, "all the mud is taken at once");
            helper.assertValueEqual(FarmState.of(data).kind(field), FieldKind.GU_PLANT, "the field is a gu plant field");
            for (BlockPos cell : field.cells()) {
                helper.assertTrue(level.getBlockState(cell.below()).is(blackSoil), "every farmland is black soil");
            }
            helper.assertValueEqual(FarmWork.census(level, data).get(Blocks.WHEAT),
                    new FarmWork.Count(0, FARMLAND_CELLS), "the crops stay on the new soil");

            BlockPos trampled = field.cells().getLast();
            level.setBlock(trampled, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(trampled.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            helper.assertValueEqual(FarmWork.harvest(level, data, Integer.MAX_VALUE).sown(), 1,
                    "the trampled cell is sown again");
            helper.assertTrue(level.getBlockState(trampled.below()).is(Blocks.FARMLAND),
                    "without mud it is tilled as plain farmland");
            storage.insert(new ItemStack(ModItems.ROTTEN_FROZEN_MUD.get(), 3));
            helper.assertValueEqual(FarmWork.harvest(level, data, Integer.MAX_VALUE).fields(), 1,
                    "mixing the soil back counts as work on the field");
            helper.assertTrue(level.getBlockState(trampled.below()).is(blackSoil), "one mud mixes it back");
            helper.assertValueEqual(storage.count(FarmWork::isMud), 2L, "only one mud is spent");

            helper.assertValueEqual(FarmWork.retype(level, data, field, FieldKind.PLAIN, owner),
                    FarmWork.Retype.CHANGED, "turning back needs nothing");
            helper.assertTrue(level.getBlockState(trampled.below()).is(Blocks.FARMLAND), "black soil turns plain");
            helper.assertValueEqual(storage.count(FarmWork::isMud), 2L, "no mud is refunded");
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
        BlissfulStorage.of(data).insert(new ItemStack(Items.WHEAT_SEEDS, FARMLAND_CELLS));
        ServerPlayer owner = owner(helper, "gzr-farm-remove-mock");
        FieldPlot field = FarmWork.reclaim(level, data, plot(helper, 0, WHOLE - 1), owner);
        helper.assertTrue(field != null, "the field is laid");
        ripen(level, field, Blocks.WHEAT);

        helper.assertValueEqual(FarmWork.remove(level, data, field, owner), FARMLAND_CELLS, "every crop is taken");
        helper.assertTrue(BlissfulStorage.of(data).count(stack -> stack.is(Items.WHEAT)) >= FARMLAND_CELLS,
                "the wheat goes to storage");
        helper.assertTrue(FarmState.of(data).fields().isEmpty(), "the field is struck off");
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
                SoulsunderMountain.INSTANCE, half, 0, BlissfulLand.FOX_IMMORTAL.entry(),
                BlissfulLand.FOX_IMMORTAL.spirit()));
        ChunkPos second = new ChunkPos(first.x + 1, first.z);
        FarmState.of(data).addPlan(first.toLong());
        FarmState.of(data).addPlan(second.toLong());
        Supplier<Siting.Plot> taken = () -> MapPart.plot(level, data);
        try {
            FieldPlans.Step reclaimed = FieldPlans.step(level, data, owner, chunk -> plot, taken);
            helper.assertTrue(reclaimed != null && reclaimed.reclaimed(), "the head of the queue is reclaimed");
            helper.assertValueEqual(FarmState.of(data).fields().size(), 1, "the field is recorded");
            helper.assertValueEqual(FarmState.of(data).plans().size(), 1, "the queue moves on");

            FieldPlans.Step cancelled = FieldPlans.step(level, data, owner, chunk -> plot, taken);
            helper.assertTrue(cancelled != null && !cancelled.reclaimed(),
                    "the next plan lands on water holes and is cancelled");
            helper.assertTrue(FarmState.of(data).plans().isEmpty(), "a cancelled plan leaves the queue");
            helper.assertValueEqual(FarmState.of(data).fields().size(), 1, "no second field is laid");
            helper.assertTrue(FieldPlans.step(level, data, owner, chunk -> plot, taken) == null,
                    "an empty queue does nothing");

            data.setOwner(UUID.randomUUID(), "gzr-farm-away");
            FarmState.of(data).addPlan(second.toLong());
            FieldPlans.tick(level, data, taken);
            helper.assertValueEqual(FarmState.of(data).plans().size(), 1, "the queue waits while the owner is away");
            helper.succeed();
        } finally {
            FarmWork.release(level, data);
        }
    }
}
