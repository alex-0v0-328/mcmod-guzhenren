package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.block.BlackSoilFarmlandBlock;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;

/**
 * 黑土耕地：腐泥冻土右键原版耕地或其上的作物掺成，保留湿润度和作物（不发形状更新，用一受更新即碎的枯灌木验证），
 * 生存消耗 1 个、创造不消耗，别的方块不认；踩踏、干燥退泥土、近水湿润、湿润加成、掉泥土同原版耕地；每个随机刻给正上方
 * 作物补 3 次生长判定，成熟即停，标签作物（瓜茎）算、树苗不算，知心草作物每补一刻都过自己的 1/4 判定。生长判定用恒掷 0
 * 并计数的随机源直接驱动耕地随机刻，次数与阶段都确定。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlackSoilFarmlandGameTests {

    private static final int EXTRA_ROLLS = 3;
    private static final int LIGHT_TO_GROW = 9;
    private static final float TRAMPLING_FALL = 2.0F;
    private static final BlockPos SOIL = CENTER_POS.below();
    private static final BlockPos OTHER_SOIL = SOIL.east(3);

    private BlackSoilFarmlandGameTests() {}

    //region Conversion [转换]
    @GameTest(template = "empty9x9x9")
    public static void mudTurnsFarmlandIntoBlackSoilKeepingMoistureAndCrop(GameTestHelper helper) {
        placeSoil(helper, SOIL, Blocks.FARMLAND, FarmBlock.MAX_MOISTURE);
        helper.setBlock(SOIL.above(), ((CropBlock) Blocks.WHEAT).getStateForAge(3));
        placeSoil(helper, OTHER_SOIL, Blocks.FARMLAND, 0);
        ServerPlayer player = holdMud(helper, 2);

        helper.assertTrue(useOn(helper, player, SOIL).consumesAction(), "the mud is used on wet farmland");
        assertBlackSoil(helper, SOIL, FarmBlock.MAX_MOISTURE);
        helper.assertBlockState(SOIL.above(), state -> state.is(Blocks.WHEAT) && state.getValue(CropBlock.AGE) == 3,
                () -> "the wheat keeps its stage");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "one mud spent");

        helper.assertTrue(useOn(helper, player, OTHER_SOIL).consumesAction(), "the mud is used on dry farmland");
        assertBlackSoil(helper, OTHER_SOIL, 0);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "the last mud is spent");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void mudOnACropTurnsTheFarmlandBeneathIt(GameTestHelper helper) {
        placeSoil(helper, SOIL, Blocks.FARMLAND, FarmBlock.MAX_MOISTURE);
        helper.setBlock(SOIL.above(), ((CropBlock) Blocks.WHEAT).getStateForAge(CropBlock.MAX_AGE));
        placeSoil(helper, OTHER_SOIL, Blocks.FARMLAND, 0);
        helper.setBlock(OTHER_SOIL.above(), Blocks.OAK_SAPLING);
        ServerPlayer player = holdMud(helper, 2);

        helper.assertTrue(useOn(helper, player, SOIL.above()).consumesAction(), "the mud is used on the wheat");
        assertBlackSoil(helper, SOIL, FarmBlock.MAX_MOISTURE);
        helper.assertBlockState(SOIL.above(), state -> state.is(Blocks.WHEAT)
                && state.getValue(CropBlock.AGE) == CropBlock.MAX_AGE, () -> "the wheat stays mature");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "one mud spent");

        helper.assertTrue(useOn(helper, player, SOIL.above()) == InteractionResult.PASS,
                "a crop on black soil takes no more");
        helper.assertTrue(useOn(helper, player, OTHER_SOIL.above()) == InteractionResult.PASS,
                "a sapling does not pass the mud down");
        helper.assertBlockPresent(Blocks.FARMLAND, OTHER_SOIL);
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "no more mud spent");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void conversionSendsNoShapeUpdateToThePlantAbove(GameTestHelper helper) {
        placeSoil(helper, SOIL, Blocks.FARMLAND, 0);
        helper.setBlock(SOIL.above(), Blocks.DEAD_BUSH);
        ServerPlayer player = holdMud(helper, 1);

        helper.assertTrue(useOn(helper, player, SOIL).consumesAction(), "the mud is used");
        assertBlackSoil(helper, SOIL, 0);
        helper.assertBlockPresent(Blocks.DEAD_BUSH, SOIL.above());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void creativeConversionKeepsTheMud(GameTestHelper helper) {
        placeSoil(helper, SOIL, Blocks.FARMLAND, 0);
        ServerPlayer player = holdMud(helper, 1);
        player.getAbilities().instabuild = true;

        helper.assertTrue(useOn(helper, player, SOIL).consumesAction(), "the mud is used");
        assertBlackSoil(helper, SOIL, 0);
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "creative keeps the mud");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void mudLeavesOtherBlocksAlone(GameTestHelper helper) {
        helper.setBlock(SOIL, Blocks.DIRT);
        placeSoil(helper, OTHER_SOIL, ModBlocks.BLACK_SOIL_FARMLAND.get(), 3);
        ServerPlayer player = holdMud(helper, 2);

        helper.assertTrue(useOn(helper, player, SOIL) == InteractionResult.PASS, "dirt is not converted");
        helper.assertTrue(useOn(helper, player, OTHER_SOIL) == InteractionResult.PASS, "black soil takes no more");
        helper.assertBlockPresent(Blocks.DIRT, SOIL);
        assertBlackSoil(helper, OTHER_SOIL, 3);
        helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "no mud spent");
        helper.succeed();
    }
    //endregion

    //region Vanilla farmland behavior [原版耕地行为]
    @GameTest(template = "empty9x9x9")
    public static void tramplingTurnsBlackSoilIntoDirt(GameTestHelper helper) {
        placeSoil(helper, SOIL, ModBlocks.BLACK_SOIL_FARMLAND.get(), FarmBlock.MAX_MOISTURE);
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-trample-mock").build();
        BlockPos absolute = helper.absolutePos(SOIL);
        BlockState soil = helper.getLevel().getBlockState(absolute);

        soil.getBlock().fallOn(helper.getLevel(), soil, absolute, player, TRAMPLING_FALL);

        helper.assertBlockPresent(Blocks.DIRT, SOIL);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void dryBlackSoilRevertsToDirtUnlessACropHoldsIt(GameTestHelper helper) {
        for (BlockPos soil : new BlockPos[] { SOIL, OTHER_SOIL }) {
            placeSoil(helper, soil, ModBlocks.BLACK_SOIL_FARMLAND.get(), 0);
            helper.setBlock(soil.above(2), Blocks.GLASS);
        }
        helper.setBlock(OTHER_SOIL.above(), Blocks.WHEAT);

        tick(helper, SOIL, RandomSource.create(0L));
        tick(helper, OTHER_SOIL, RandomSource.create(0L));

        helper.assertBlockPresent(Blocks.DIRT, SOIL);
        assertBlackSoil(helper, OTHER_SOIL, 0);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void waterWithinFourBlocksMoistensAndFertilizesBlackSoil(GameTestHelper helper) {
        placeSoil(helper, SOIL, ModBlocks.BLACK_SOIL_FARMLAND.get(), 0);
        helper.setBlock(SOIL.east(4).below(), Blocks.STONE);
        helper.setBlock(SOIL.east(4), Blocks.WATER);
        BlockPos absolute = helper.absolutePos(SOIL);
        helper.assertFalse(helper.getBlockState(SOIL).isFertile(helper.getLevel(), absolute),
                "dry soil is not fertile");

        tick(helper, SOIL, RandomSource.create(0L));

        assertBlackSoil(helper, SOIL, FarmBlock.MAX_MOISTURE);
        helper.assertTrue(helper.getBlockState(SOIL).isFertile(helper.getLevel(), absolute), "wet soil is fertile");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void minedBlackSoilDropsDirtOnly(GameTestHelper helper) {
        placeSoil(helper, SOIL, ModBlocks.BLACK_SOIL_FARMLAND.get(), FarmBlock.MAX_MOISTURE);
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-dig-mock").build();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));

        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(SOIL)), "the survival break lands");

        helper.assertValueEqual(count(helper, SOIL, Items.DIRT), 1, "dirt drops");
        helper.assertValueEqual(count(helper, SOIL, ModItems.BLACK_SOIL_FARMLAND.get()), 0, "black soil drops");
        helper.assertValueEqual(count(helper, SOIL, ModItems.ROTTEN_FROZEN_MUD.get()), 0, "mud drops");
        helper.succeed();
    }
    //endregion

    //region Growth boost [生长加速]
    @GameTest(template = "empty9x9x9")
    public static void blackSoilGivesTheCropThreeExtraGrowthRolls(GameTestHelper helper) {
        CropBlock wheat = (CropBlock) Blocks.WHEAT;

        int boosted = rollsFor(helper, SOIL, ModBlocks.BLACK_SOIL_FARMLAND.get(), wheat.getStateForAge(0));
        int plain = rollsFor(helper, OTHER_SOIL, Blocks.FARMLAND, wheat.getStateForAge(0));

        helper.assertValueEqual(boosted, EXTRA_ROLLS, "growth rolls on black soil");
        helper.assertValueEqual(ageAt(helper, SOIL), EXTRA_ROLLS, "stages on black soil");
        helper.assertValueEqual(plain, 0, "growth rolls from vanilla farmland");
        helper.assertValueEqual(ageAt(helper, OTHER_SOIL), 0, "stages on vanilla farmland");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void extraRollsStopOnceTheCropMatures(GameTestHelper helper) {
        CropBlock wheat = (CropBlock) Blocks.WHEAT;

        int rolls = rollsFor(helper, SOIL, ModBlocks.BLACK_SOIL_FARMLAND.get(),
                wheat.getStateForAge(CropBlock.MAX_AGE - 1));

        helper.assertValueEqual(rolls, 1, "growth rolls before maturity");
        helper.assertValueEqual(ageAt(helper, SOIL), CropBlock.MAX_AGE, "the wheat is mature");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void taggedStemsAreBoostedButSaplingsAreNot(GameTestHelper helper) {
        int stem = rollsFor(helper, SOIL, ModBlocks.BLACK_SOIL_FARMLAND.get(), Blocks.MELON_STEM.defaultBlockState());
        int sapling = rollsFor(helper, OTHER_SOIL, ModBlocks.BLACK_SOIL_FARMLAND.get(),
                Blocks.OAK_SAPLING.defaultBlockState());

        helper.assertValueEqual(stem, EXTRA_ROLLS, "growth rolls for a melon stem");
        helper.assertValueEqual(helper.getBlockState(SOIL.above()).getValue(StemBlock.AGE),
                EXTRA_ROLLS, "stem stages");
        helper.assertValueEqual(sapling, 0, "growth rolls for a sapling");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void cropBlocksCountAsCropsEvenOutsideTheTag(GameTestHelper helper) {
        helper.assertTrue(BlackSoilFarmlandBlock.isCrop(false, Blocks.WHEAT), "an untagged CropBlock is a crop");
        helper.assertTrue(BlackSoilFarmlandBlock.isCrop(true, Blocks.MELON_STEM), "a tagged stem is a crop");
        helper.assertFalse(BlackSoilFarmlandBlock.isCrop(false, Blocks.MELON_STEM), "an untagged stem is not");
        helper.assertFalse(BlackSoilFarmlandBlock.isCrop(false, Blocks.OAK_SAPLING), "a sapling is not");
        for (Block block : BuiltInRegistries.BLOCK) {
            BlockState state = block.defaultBlockState();
            boolean split = BlackSoilFarmlandBlock.isCrop(state.is(BlockTags.CROPS), block);
            helper.assertTrue(BlackSoilFarmlandBlock.isCrop(state) == split, "both crop tests agree on " + block);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void intimateGrassPassesItsQuarterGateOnEveryExtraRoll(GameTestHelper helper) {
        int rolls = rollsFor(helper, SOIL, ModBlocks.BLACK_SOIL_FARMLAND.get(),
                ModBlocks.INTIMATE_GRASS_CROP.get().getStateForAge(0));

        helper.assertValueEqual(rolls, EXTRA_ROLLS * 2, "gate and growth rolls");
        helper.assertValueEqual(ageAt(helper, SOIL), EXTRA_ROLLS, "crop stages");
        helper.succeed();
    }
    //endregion

    //region Scene [场景]
    private static void placeSoil(GameTestHelper helper, BlockPos pos, Block soil, int moisture) {
        helper.setBlock(pos, soil.defaultBlockState().setValue(FarmBlock.MOISTURE, moisture));
    }

    private static ServerPlayer holdMud(GameTestHelper helper, int count) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-mud-mock").build();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ROTTEN_FROZEN_MUD.get(), count));
        return player;
    }

    private static InteractionResult useOn(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false);
        return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static void tick(GameTestHelper helper, BlockPos soil, RandomSource random) {
        ServerLevel level = helper.getLevel();
        BlockPos absolute = helper.absolutePos(soil);
        level.getBlockState(absolute).randomTick(level, absolute, random);
    }

    private static int rollsFor(GameTestHelper helper, BlockPos soil, Block soilBlock, BlockState plant) {
        placeSoil(helper, soil, soilBlock, FarmBlock.MAX_MOISTURE);
        helper.setBlock(soil.above(), plant);
        helper.assertTrue(helper.getLevel().getRawBrightness(helper.absolutePos(soil.above()), 0) >= LIGHT_TO_GROW,
                "the scene is lit enough to grow");
        CountingRandom random = new CountingRandom();
        tick(helper, soil, random);
        return random.rolls;
    }

    private static int ageAt(GameTestHelper helper, BlockPos soil) {
        return helper.getBlockState(soil.above()).getValue(CropBlock.AGE);
    }

    private static void assertBlackSoil(GameTestHelper helper, BlockPos pos, int moisture) {
        helper.assertBlockState(pos, state -> state.is(ModBlocks.BLACK_SOIL_FARMLAND.get())
                && state.getValue(FarmBlock.MOISTURE) == moisture, () -> "black soil with moisture " + moisture);
    }

    private static int count(GameTestHelper helper, BlockPos pos, Item item) {
        int total = 0;
        for (ItemEntity drop : helper.getEntities(EntityType.ITEM, pos, 1.0)) {
            if (drop.getItem().is(item)) total += drop.getItem().getCount();
        }
        return total;
    }

    /** 恒掷 0 的随机源：每次生长判定都成功，并数出掷了几次。 */
    private static final class CountingRandom extends LegacyRandomSource {

        private int rolls;

        private CountingRandom() { super(0L); }

        @Override
        public int nextInt(int bound) {
            rolls++;
            return 0;
        }
    }
    //endregion
}
