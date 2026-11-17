package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.block.IntimateGrassCropBlock;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;

/**
 * 知心草的野生采集与种植：野生两格草不论工具、打哪半都只掉一份（2 草束加 1–4 种子）；种子只能种在耕地（含黑土耕地）
 * 上；作物成熟掉 1 草束加 1–2 种子，未成熟退 1 种子；骨粉同小麦；生长次数是同条件小麦的 1/4；作物在农夫乐事的沃土
 * 排除标签里。破坏均为真实生存 {@code gameMode.destroyBlock}。生长对照用固定种子的随机源直接驱动随机刻，结果确定。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IntimateGrassGameTests {

    private static final TagKey<Block> UNAFFECTED_BY_RICH_SOIL = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "unaffected_by_rich_soil"));
    private static final long GROWTH_SEED = 20261118L;
    private static final int GROWTH_ROLLS = 12_000;
    private static final int WHEAT_TO_CROP = 4;
    private static final double GROWTH_TOLERANCE = 0.6D;
    private static final int LIGHT_TO_GROW = 9;

    private IntimateGrassGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void wildGrassDropsOnceFromEitherHalfWithAnyTool(GameTestHelper helper) {
        BlockPos lower = CENTER_POS;
        BlockPos other = CENTER_POS.east(3);
        plantWildGrass(helper, lower);
        plantWildGrass(helper, other);

        breakWith(helper, ItemStack.EMPTY, lower);
        breakWith(helper, new ItemStack(Items.WOODEN_HOE), other.above());

        for (BlockPos pos : new BlockPos[] { lower, other }) {
            helper.assertValueEqual(count(helper, pos, ModItems.INTIMATE_GRASS_BUNDLE.get()), 2, "bundles at " + pos);
            int seeds = count(helper, pos, ModItems.INTIMATE_GRASS_SEEDS.get());
            helper.assertTrue(seeds >= 1 && seeds <= 4, "1 to 4 seeds at " + pos + ", got " + seeds);
            helper.assertBlockNotPresent(ModBlocks.INTIMATE_GRASS.get(), pos);
            helper.assertBlockNotPresent(ModBlocks.INTIMATE_GRASS.get(), pos.above());
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void seedsPlantOnFarmlandOnly(GameTestHelper helper) {
        BlockPos farmland = CENTER_POS.below();
        BlockPos dirt = farmland.east(3);
        BlockPos blackSoil = farmland.west(3);
        helper.setBlock(farmland, Blocks.FARMLAND);
        helper.setBlock(dirt, Blocks.DIRT);
        helper.setBlock(blackSoil, ModBlocks.BLACK_SOIL_FARMLAND.get());
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-seed-mock").build();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.INTIMATE_GRASS_SEEDS.get(), 3));

        useOnTop(helper, player, dirt);
        helper.assertBlockNotPresent(ModBlocks.INTIMATE_GRASS_CROP.get(), dirt.above());
        helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "seeds kept on dirt");

        useOnTop(helper, player, farmland);
        helper.assertBlockState(CENTER_POS, state -> state.is(ModBlocks.INTIMATE_GRASS_CROP.get())
                && state.getValue(CropBlock.AGE) == 0, () -> "a seedling on the farmland");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "one seed spent");

        useOnTop(helper, player, blackSoil);
        helper.assertBlockPresent(ModBlocks.INTIMATE_GRASS_CROP.get(), blackSoil.above());
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "one more seed spent on black soil");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void matureCropGivesABundleAndSeedsButASeedlingReturnsItsSeed(GameTestHelper helper) {
        BlockPos mature = CENTER_POS;
        BlockPos seedling = CENTER_POS.east(3);
        plantCrop(helper, mature, ModBlocks.INTIMATE_GRASS_CROP.get(), CropBlock.MAX_AGE);
        plantCrop(helper, seedling, ModBlocks.INTIMATE_GRASS_CROP.get(), CropBlock.MAX_AGE - 1);

        breakWith(helper, ItemStack.EMPTY, mature);
        breakWith(helper, ItemStack.EMPTY, seedling);

        helper.assertValueEqual(count(helper, mature, ModItems.INTIMATE_GRASS_BUNDLE.get()), 1, "mature bundles");
        int seeds = count(helper, mature, ModItems.INTIMATE_GRASS_SEEDS.get());
        helper.assertTrue(seeds >= 1 && seeds <= 2, "1 to 2 seeds from a mature crop, got " + seeds);
        helper.assertValueEqual(count(helper, seedling, ModItems.INTIMATE_GRASS_BUNDLE.get()), 0, "seedling bundles");
        helper.assertValueEqual(count(helper, seedling, ModItems.INTIMATE_GRASS_SEEDS.get()), 1, "seedling seeds");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void boneMealAdvancesTheCropLikeWheat(GameTestHelper helper) {
        plantCrop(helper, CENTER_POS, ModBlocks.INTIMATE_GRASS_CROP.get(), 0);
        ItemStack boneMeal = new ItemStack(Items.BONE_MEAL);

        helper.assertTrue(BoneMealItem.growCrop(boneMeal, helper.getLevel(), helper.absolutePos(CENTER_POS)),
                "bone meal applies");

        int age = helper.getBlockState(CENTER_POS).getValue(CropBlock.AGE);
        helper.assertTrue(age >= 2 && age <= 5, "2 to 5 stages per bone meal, got " + age);
        helper.assertTrue(boneMeal.isEmpty(), "the bone meal is spent");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void cropGrowsAtAQuarterOfWheatSpeed(GameTestHelper helper) {
        int crop = countGrowths(helper, CENTER_POS, ModBlocks.INTIMATE_GRASS_CROP.get());
        int wheat = countGrowths(helper, CENTER_POS.east(3), (CropBlock) Blocks.WHEAT);

        helper.assertValueEqual(IntimateGrassCropBlock.GROWTH_SLOWDOWN, WHEAT_TO_CROP, "the slowdown constant");
        helper.assertTrue(crop > 0, "the crop grows at all");
        double ratio = wheat / (double) crop;
        helper.assertTrue(Math.abs(ratio - WHEAT_TO_CROP) <= GROWTH_TOLERANCE,
                "wheat grows " + WHEAT_TO_CROP + " times as often, got " + wheat + " to " + crop);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void richSoilLeavesTheCropAlone(GameTestHelper helper) {
        helper.assertTrue(ModBlocks.INTIMATE_GRASS_CROP.get().defaultBlockState().is(UNAFFECTED_BY_RICH_SOIL),
                "the crop is in the Farmer's Delight rich soil exclusion tag");
        helper.succeed();
    }

    //region Scene [场景]
    private static void plantWildGrass(GameTestHelper helper, BlockPos lower) {
        helper.setBlock(lower.below(), Blocks.GRASS_BLOCK);
        DoublePlantBlock.placeAt(helper.getLevel(), ModBlocks.INTIMATE_GRASS.get().defaultBlockState(),
                helper.absolutePos(lower), Block.UPDATE_ALL);
    }

    private static void plantCrop(GameTestHelper helper, BlockPos pos, CropBlock crop, int age) {
        helper.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState()
                .setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
        helper.setBlock(pos, crop.getStateForAge(age));
    }

    private static int countGrowths(GameTestHelper helper, BlockPos pos, CropBlock crop) {
        plantCrop(helper, pos, crop, 0);
        ServerLevel level = helper.getLevel();
        BlockPos absolute = helper.absolutePos(pos);
        helper.assertTrue(level.getRawBrightness(absolute, 0) >= LIGHT_TO_GROW, "the scene is lit enough to grow");
        BlockState seedling = crop.getStateForAge(0);
        RandomSource random = RandomSource.create(GROWTH_SEED);
        int growths = 0;
        for (int roll = 0; roll < GROWTH_ROLLS; roll++) {
            seedling.randomTick(level, absolute, random);
            if (level.getBlockState(absolute).getValue(CropBlock.AGE) == 0) continue;

            growths++;
            level.setBlock(absolute, seedling, Block.UPDATE_CLIENTS);
        }
        return growths;
    }

    private static void useOnTop(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0.0D, 0.5D, 0.0D), Direction.UP,
                absolute, false);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static void breakWith(GameTestHelper helper, ItemStack tool, BlockPos pos) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-harvest-mock").build();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(pos)), "the survival break lands");
    }

    private static int count(GameTestHelper helper, BlockPos pos, Item item) {
        int total = 0;
        for (ItemEntity drop : helper.getEntities(EntityType.ITEM, pos, 1.0)) {
            if (drop.getItem().is(item)) total += drop.getItem().getCount();
        }
        return total;
    }
    //endregion
}
