package net.alex.guzhenren.block;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.fluid.ModFluids;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 纯水视同原版水：属 {@code #minecraft:water} 标签；可成无限水源；桶可放可舀；鱼可呼吸（{@code EntityMixin}）；遇岩浆凝
 * 固；发射器可倒桶（{@link PureWaterEvents}）。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PureWaterGameTests {

    private static final BlockPos CENTER = new BlockPos(4, 2, 4);

    private PureWaterGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void pureWaterJoinsTheWaterTag(GameTestHelper helper) {
        helper.assertTrue(ModFluids.PURE_WATER.get().defaultFluidState().is(FluidTags.WATER), "source is water");
        helper.assertTrue(ModFluids.FLOWING_PURE_WATER.get().defaultFluidState().is(FluidTags.WATER),
                "flowing arm is water");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 200)
    public static void pureWaterMintsANewSourceBetweenTwoSources(GameTestHelper helper) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -2; z <= 2; z++) {
                helper.setBlock(CENTER.offset(x, -1, z), Blocks.STONE);
                boolean trough = z == 0 && x >= -1 && x <= 1;
                if (!trough) helper.setBlock(CENTER.offset(x, 0, z), Blocks.STONE);
            }
        }
        helper.setBlock(CENTER.offset(-1, 0, 0), ModBlocks.PURE_WATER.get());
        helper.setBlock(CENTER.offset(1, 0, 0), ModBlocks.PURE_WATER.get());
        helper.succeedWhen(() -> helper.assertBlockState(CENTER,
                state -> state.getFluidState().getType() == ModFluids.PURE_WATER.get(),
                () -> "the gap must turn into a new source, but is "
                        + helper.getLevel().getBlockState(helper.absolutePos(CENTER))));
    }

    @GameTest(template = "empty9x9x9")
    public static void bucketPlacesAndScoopsPureWater(GameTestHelper helper) {
        helper.setBlock(CENTER.below(), Blocks.STONE);
        BlockPos absolute = helper.absolutePos(CENTER);
        ItemStack full = new ItemStack(ModItems.PURE_WATER_BUCKET.get());
        BucketItem bucket = (BucketItem) full.getItem();
        helper.assertTrue(bucket.emptyContents(null, helper.getLevel(), absolute, null, full), "the bucket pours");
        helper.assertTrue(helper.getLevel().getFluidState(absolute).getType() == ModFluids.PURE_WATER.get(),
                "the poured block is a pure water source");
        BlockState placed = helper.getLevel().getBlockState(absolute);
        ItemStack scooped = ((LiquidBlock) placed.getBlock()).pickupBlock(null, helper.getLevel(), absolute, placed);
        helper.assertTrue(scooped.is(ModItems.PURE_WATER_BUCKET.get()), "an empty bucket scoops a pure water bucket");
        helper.assertBlockPresent(Blocks.AIR, CENTER);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 200)
    public static void fishInPureWaterIsInWaterAndKeepsItsAir(GameTestHelper helper) {
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    boolean shell = Math.abs(x) == 2 || Math.abs(z) == 2 || y == -1;
                    helper.setBlock(CENTER.offset(x, y, z), shell ? Blocks.STONE : ModBlocks.PURE_WATER.get());
                }
            }
        }
        Cod cod = helper.spawn(EntityType.COD, CENTER.above());
        helper.runAtTickTime(60, () -> {
            helper.assertTrue(cod.isAlive(), "the cod is alive");
            helper.assertTrue(cod.isInWater(), "a cod in pure water is in water");
            helper.assertTrue(cod.isUnderWater(), "a cod in pure water is under water");
            helper.assertTrue(cod.getAirSupply() == cod.getMaxAirSupply(), "a cod in pure water keeps its air");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void lavaSourceTouchingPureWaterHardensToObsidian(GameTestHelper helper) {
        helper.setBlock(CENTER.below(), Blocks.STONE);
        helper.setBlock(CENTER.offset(1, -1, 0), Blocks.STONE);
        helper.setBlock(CENTER, Blocks.LAVA);
        helper.setBlock(CENTER.east(), ModBlocks.PURE_WATER.get());
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.OBSIDIAN, CENTER));
    }

    @GameTest(template = "empty9x9x9")
    public static void dispenserPoursPureWaterLikeAWaterBucket(GameTestHelper helper) {
        helper.assertTrue(DispenserBlock.DISPENSER_REGISTRY.get(ModItems.PURE_WATER_BUCKET.get())
                        == DispenserBlock.DISPENSER_REGISTRY.get(Items.WATER_BUCKET),
                "the pure water bucket dispenses with the water bucket's behavior");
        helper.succeed();
    }
}
