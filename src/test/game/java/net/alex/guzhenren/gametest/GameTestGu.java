package net.alex.guzhenren.gametest;

import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.item.gu.mortal.path.space.PrimevalElderGuItem;
import net.alex.guzhenren.item.gu.stack.RefinedGuState;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * GameTest 共用的蛊夹具：真元[Essence]满的已开窍空窍[Aperture]、设定饱食[Hunger]的已炼化蛊、仿按住右键跑完整次催动的主
 * 手点击。{@link #click} 容忍被拒，也能驱动瞬发与被拒的蛊；{@link #charge} 为严格版，用于须开始蓄力的蛊，被拒即失败。
 */

final class GameTestGu {

    private GameTestGu() {}

    /** 以资质[Aptitude] 80 开窍[Awakening]到 {@code rank}，并充满真元[Essence]。 */
    static ServerPlayer awakened(ServerPlayer player, Rank rank) {
        ApertureService.awaken(player, 80);
        ApertureService.setRank(player, rank);
        ApertureEssenceService.refill(player);
        return player;
    }

    /** 饱食[Hunger]为 {@code hunger} 的已炼化物品；{@code item} 非持续照顾型蛊[Tended Gu]则测试失败。 */
    static ItemStack refined(Item item, int hunger) {
        assertTrue(item instanceof TendedGuItem, item + " is no tended Gu");
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.REFINED_GU_STATE,
                new RefinedGuState(true, ((TendedGuItem) item).getRefineCost(), 0, hunger));
        return stack;
    }

    static ItemStack hold(ServerPlayer player, Item item, int hunger) {
        return hold(player, InteractionHand.MAIN_HAND, item, hunger);
    }

    static ItemStack hold(ServerPlayer player, InteractionHand hand, Item item, int hunger) {
        ItemStack stack = refined(item, hunger);
        player.setItemInHand(hand, stack);
        return stack;
    }

    static ItemStack inSlot(ServerPlayer player, int slot, Item item, int hunger) {
        ItemStack stack = refined(item, hunger);
        player.getInventory().setItem(slot, stack);
        return stack;
    }

    /** 存有 {@code stones} 块元石[Primeval Stone]的已炼化元老蛊[Primeval Elder Gu]。 */
    static ItemStack elder(int stones) {
        ItemStack stack = refined(ModItems.PRIMEVAL_ELDER_GU_1.get(), 0);
        ((PrimevalElderGuItem) stack.getItem()).storeStones(stack, stones);
        return stack;
    }

    static int hunger(ItemStack stack) { return TendedGuItem.getState(stack).hunger(); }

    static void click(GameTestHelper helper, ServerPlayer player, ItemStack stack) {
        stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        if (player.isUsingItem()) stack.finishUsingItem(helper.getLevel(), player);
        player.stopUsingItem();
    }

    static void charge(GameTestHelper helper, ServerPlayer player, ItemStack gu) {
        gu.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        assertTrue(player.isUsingItem(), "the click was refused");
        gu.finishUsingItem(helper.getLevel(), player);
        player.stopUsingItem();
    }
}
