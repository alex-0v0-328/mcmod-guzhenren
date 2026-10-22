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
 * two-second post-refinement cooldown. The gate checks essence against {@code refineCost};
 * {@code apply} pays the cost and fires
 * {@code useApply} in one step. Hope Gu [希望蛊] is the exception: {@code stacksTo(1)} and an 80-tick
 * ritual, handled by its own leaf class.
 *
 * @author Alex
 * @version 1.0.0
 * @see MortalGuItem
 * @since 1.0.0
 */

public abstract class OneShotGuItem extends MortalGuItem {

    private static final String TOOLTIP_REFINE_COST = "guzhenren.item.gu.refine_cost";

    protected OneShotGuItem(Properties properties, GuSpec spec) {
        super(properties, spec);
    }

    //region the one press
    @Override
    protected final @Nullable Refusal gate(Player player, ItemStack stack) {
        Refusal cost = essenceGate(player, refineCost(), FAILED_REFINE_ESSENCE);
        return cost != null ? cost : useGate(player, stack);
    }

    @Override
    protected final int apply(ServerPlayer player, ItemStack stack) {
        payRefineCost(player);
        return useApply(player, stack);
    }

    @Override
    protected final int useDurationTicks(Player player, ItemStack stack) { return 0; }

    @Override
    protected int cooldownTicks(ItemStack stack) { return REFINE_DONE_COOLDOWN_TICKS; }
    //endregion

    //region display
    @Override
    protected @Nullable MutableComponent progressLine(ItemStack stack) {
        return refineCost() > 0 ? Component.translatable(TOOLTIP_REFINE_COST, refineCost()) : null;
    }
    //endregion
}
