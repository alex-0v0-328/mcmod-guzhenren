package net.alex.guzhenren.item.gu;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 即时一次性蛊：炼化即催动，一瞬完成，故无状态、可堆叠。{@code useDurationTicks = 0}，用共用的炼化后 2 秒冷却；门槛先按
 * {@code refineCost} 查真元[Essence] 再查行为，{@code performUse} 一步付费、执行行为、扣一只蛊。希望蛊[Hope Gu] 例外：
 * {@code stacksTo(1)}，点击开始开窍[Awakening] 仪式，由其叶类处理。
 *
 * @author Alex
 * @version 1.0.0
 * @see MortalGuItem
 * @since 1.0.0
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
