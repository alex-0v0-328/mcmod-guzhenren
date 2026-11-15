package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.mortal.path.wood.LeafGrowth;
import net.alex.guzhenren.item.gu.mortal.path.wood.NineLeafVitalityGrassItem;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestGu.charge;
import static net.alex.guzhenren.gametest.GameTestGu.hold;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;

/**
 * 九叶生机草[Nine Leaf Vitality Grass]测试。
 * <ul>
 *   <li>点按摘一片生机叶蛊[Vitality Leaf Gu]，无叶拒绝；长按蓄力 2 秒付 80 真元[Essence]长一叶，满叶或真元不足 80
 *       拒绝；松手时长居中则不摘不长；花方块掉野生满叶草。</li>
 *   <li>点按走真实 {@code use} → {@code releaseUsingItem}，长按走 {@code use} → 蓄满后 {@code finishUsingItem}。</li>
 *   <li>经状态记录设为已炼化，不走 {@code bornRefined}，免炼化后窗口拒首次点击；测试体同一 tick 内运行，
 *       靠把叶子时间戳设到过去固定重新生长。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NineLeafVitalityGrassGameTests {
    private static final int REGROW_TICKS = 3 * Ticks.MINUTE;
    private static final long ESSENCE_PER_LEAF = 80L;

    private NineLeafVitalityGrassGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void pickingTakesOneLeafUntilTheGrassIsBare(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack grass = hold(player, ModItems.NINE_LEAF_VITALITY_GRASS_3.get(), 0);
        NineLeafVitalityGrassItem item = item(grass);
        long now = helper.getLevel().getGameTime();

        for (int picked = 1; picked <= LeafGrowth.MAX_LEAVES; picked++) {
            tap(helper, player, grass);
            helper.assertValueEqual(item.getLeaves(grass, now), LeafGrowth.MAX_LEAVES - picked,
                    "leaves after a pick");
        }
        helper.assertValueEqual(player.getInventory().countItem(ModItems.VITALITY_LEAF_GU_1.get()),
                LeafGrowth.MAX_LEAVES, "every pick gives one Vitality Leaf Gu");

        tap(helper, player, grass);
        helper.assertValueEqual(player.getInventory().countItem(ModItems.VITALITY_LEAF_GU_1.get()),
                LeafGrowth.MAX_LEAVES, "a refused pick gives nothing");

        ApertureEssenceService.set(player, ApertureData.PRIMARY, ESSENCE_PER_LEAF - 1);
        grass.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!player.isUsingItem(), "a bare grass on 79 essence refuses before the charge starts");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void leavesRegrowOnePerThreeMinutesOfGameTime(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack grass = hold(player, ModItems.NINE_LEAF_VITALITY_GRASS_3.get(), 0);
        long now = helper.getLevel().getGameTime();
        grass.set(ModDataComponents.LEAF_GROWTH, new LeafGrowth(0, now - 2L * REGROW_TICKS - 5));

        helper.assertValueEqual(item(grass).getLeaves(grass, now), 2, "two periods regrow two leaves");
        helper.assertValueEqual(item(grass).getPickedStage(grass, now), LeafGrowth.PICKED_HALF,
                "two leaves look half");
        tap(helper, player, grass);
        helper.assertValueEqual(player.getInventory().countItem(ModItems.VITALITY_LEAF_GU_1.get()), 1,
                "a regrown leaf can be picked");
        helper.assertValueEqual(item(grass).getLeaves(grass, now), 1, "the pick leaves one");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void holdGrowingPaysEightyEssencePerLeafAndStopsAtNine(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack grass = hold(player, ModItems.NINE_LEAF_VITALITY_GRASS_3.get(), 0);
        long now = helper.getLevel().getGameTime();
        grass.set(ModDataComponents.LEAF_GROWTH, new LeafGrowth(7, now));

        helper.assertValueEqual(grass.getUseDuration(player),
                2 * Ticks.SECOND, "a two-second charge");
        long before = ApertureEssenceService.getSpendable(player);
        charge(helper, player, grass);
        helper.assertValueEqual(item(grass).getLeaves(grass, now), 8, "one leaf grown");
        helper.assertValueEqual(before - ApertureEssenceService.getSpendable(player), ESSENCE_PER_LEAF,
                "essence paid");
        helper.assertValueEqual(player.getInventory().countItem(ModItems.VITALITY_LEAF_GU_1.get()), 0,
                "growing never picks");

        charge(helper, player, grass);
        helper.assertValueEqual(item(grass).getLeaves(grass, now), LeafGrowth.MAX_LEAVES,
                "the ninth leaf grown");
        long full = ApertureEssenceService.getSpendable(player);
        charge(helper, player, grass);
        helper.assertValueEqual(ApertureEssenceService.getSpendable(player), full,
                "a full grass refuses to grow, at no cost");
        helper.assertValueEqual(player.getInventory().countItem(ModItems.VITALITY_LEAF_GU_1.get()), 0,
                "a refused grow never picks");

        grass.set(ModDataComponents.LEAF_GROWTH, new LeafGrowth(4, now));
        ApertureEssenceService.set(player, ApertureData.PRIMARY, ESSENCE_PER_LEAF - 1);
        charge(helper, player, grass);
        helper.assertValueEqual(item(grass).getLeaves(grass, now), 4, "79 essence refuses to grow");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aReleasePastTheTapButShortOfTheChargeLandsNeither(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack grass = hold(player, ModItems.NINE_LEAF_VITALITY_GRASS_3.get(), 0);
        long now = helper.getLevel().getGameTime();
        grass.set(ModDataComponents.LEAF_GROWTH, new LeafGrowth(7, now));
        long before = ApertureEssenceService.getSpendable(player);

        grass.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.isUsingItem(), "the press starts a charge");
        grass.releaseUsing(helper.getLevel(), player, grass.getUseDuration(player) - GuItem.TAP_TICKS - 1);
        player.stopUsingItem();

        helper.assertValueEqual(item(grass).getLeaves(grass, now), 7, "neither picked nor grown");
        helper.assertValueEqual(player.getInventory().countItem(ModItems.VITALITY_LEAF_GU_1.get()), 0,
                "no leaf picked");
        helper.assertValueEqual(ApertureEssenceService.getSpendable(player), before, "no essence paid");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theFlowerDropsAWildFullGrass(GameTestHelper helper) {
        helper.setBlock(CENTER_POS.below(), Blocks.DIRT);
        helper.setBlock(CENTER_POS, ModBlocks.NINE_LEAF_VITALITY_GRASS_3.get());
        helper.getLevel().destroyBlock(helper.absolutePos(CENTER_POS), true);

        helper.assertItemEntityPresent(ModItems.NINE_LEAF_VITALITY_GRASS_3.get(), CENTER_POS, 2.0);
        for (ItemEntity drop : helper.getEntities(EntityType.ITEM, CENTER_POS, 2.0)) {
            ItemStack stack = drop.getItem();
            if (!stack.is(ModItems.NINE_LEAF_VITALITY_GRASS_3.get())) continue;

            helper.assertTrue(!item(stack).isRefined(stack), "the dropped grass is wild");
            helper.assertValueEqual(item(stack).getLeaves(stack, helper.getLevel().getGameTime()),
                    LeafGrowth.MAX_LEAVES, "the dropped grass is full");
        }
        helper.succeed();
    }

    private static void tap(GameTestHelper helper, ServerPlayer player, ItemStack grass) {
        grass.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.isUsingItem(), "the press starts a charge");
        player.releaseUsingItem();
    }

    private static ServerPlayer holder(GameTestHelper helper) {
        return GameTestGu.awakened(GameTestPlayers.survival(helper, "gzr-nine-leaf-mock").build(), Rank.THREE);
    }

    private static NineLeafVitalityGrassItem item(ItemStack stack) {
        return (NineLeafVitalityGrassItem) stack.getItem();
    }
}
