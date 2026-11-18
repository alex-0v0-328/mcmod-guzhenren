package net.alex.guzhenren.item.gu;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 即时一次性消耗凡蛊宿主：继承 {@link MortalGuItem}，右键单击一步完成炼化与催动并消耗物品栈，自身不维护任何物品栈状态。
 * <ul>
 *   <li>{@link #useDurationTicks} 恒为 0，催动后施加 {@link #REFINE_DONE_COOLDOWN_TICKS}（2 秒）冷却；{@link #checkUse}
 *       先按 {@link #getRefineCost} 校验真元门槛，通过后再依次校验行为列表；{@link #performUse} 一次性扣除炼化真元、执
 *       行全部行为并消耗 1 只蛊虫（希望蛊[Hope Gu]因需启动持续开窍[Awakening]仪式而直接继承 {@link MortalGuItem}，不走
 *       本类）。</li>
 * </ul>
 */

public class OneShotGuItem extends MortalGuItem {

    private static final String TOOLTIP_REFINE_COST = "guzhenren.item.gu.refine_cost";

    public OneShotGuItem(GuProperties guProperties, GuBehavior... behaviors) {
        super(new Properties(), guProperties, behaviors);
    }

    //region the one press [一按]
    @Override
    protected final @Nullable Refusal checkUse(Player player, ItemStack stack) {
        Refusal cost = checkEssenceGate(player, getRefineCost(), Refusal.ESSENCE);
        return cost != null ? cost : checkOwnUse(player, stack);
    }

    @Override
    protected final int performUse(ServerPlayer player, ItemStack stack) {
        payRefineCost(player);
        return performOwnUse(player, stack);
    }

    @Override
    protected @Nullable Refusal checkOwnUse(Player player, ItemStack stack) { return checkModules(player, stack); }

    @Override
    protected int performOwnUse(ServerPlayer player, ItemStack stack) {
        applyModules(player, stack);
        return 1;
    }

    @Override
    protected final int useDurationTicks(Player player, ItemStack stack) { return 0; }

    @Override
    protected int getCooldownTicks(ItemStack stack) { return REFINE_DONE_COOLDOWN_TICKS; }

    @Override
    public long getUseEssence() { return getRefineCost(); }
    //endregion

    //region display [显示]
    @Override
    protected @Nullable MutableComponent formatProgressLine(ItemStack stack) {
        return getRefineCost() > 0 ? Component.translatable(TOOLTIP_REFINE_COST, getRefineCost()) : null;
    }
    //endregion
}
