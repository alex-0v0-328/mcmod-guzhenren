package net.alex.guzhenren.item.gu;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A one-shot Gu [一次性]: refining and using are one instant act, so it carries no state and it stacks.
 *
 * <p>Extends {@link MortalGuItem} with {@code useDurationTicks = 0} (instant) and the shared
 * two-second post-refinement cooldown. The gate checks essence against {@code refineCost}, then the modules;
 * {@code performUse} pays the cost, runs every module and takes the one Gu, in one step. Hope Gu [希望蛊] is the
 * exception: {@code stacksTo(1)} and an 80-tick ritual, handled by its own leaf class.
 *
 * @author Alex
 * @version 1.0.0
 * @see MortalGuItem
 * @since 1.0.0
 */

public class OneShotGuItem extends MortalGuItem {

    private static final String TOOLTIP_REFINE_COST = "guzhenren.item.gu.refine_cost";

    public OneShotGuItem(Properties properties, GuSpec spec, GuModule... modules) {
        super(properties, spec, modules);
    }

    //region the one press
    @Override
    protected final @Nullable Refusal checkUse(Player player, ItemStack stack) {
        Refusal cost = checkEssenceGate(player, getRefineCost(), FAILED_REFINE_ESSENCE);
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

    //region display
    @Override
    protected @Nullable MutableComponent formatProgressLine(ItemStack stack) {
        return getRefineCost() > 0 ? Component.translatable(TOOLTIP_REFINE_COST, getRefineCost()) : null;
    }
    //endregion
}
