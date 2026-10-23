package net.alex.guzhenren.item.gu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * A tended Gu [需照顾] that its own use takes: cared for like any other, and gone once driven.
 *
 * <p>Extends {@link TendedGuItem} to override {@code drive} so it returns 1, making the base's own
 * {@code spend} shrink the stack. The third answer to "is it still there afterward" (after
 * {@link OneShotGuItem} and a plain tended Gu), so the Vital [本命] slot asks {@code canBeVital}
 * rather than testing the class.
 *
 * <p>⚠ Binding one would lose it, and bill its owner, on the very first click. ⚠ It never eats on its way out:
 * {@code drive} only spends and grants ({@code trySpendAndGrant}), and the sneak use skips the base's eat-first,
 * so no food goes into a stack that is gone after the click.
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @see OneShotGuItem
 * @since 1.0.0
 */

public abstract class ConsumedGuItem extends TendedGuItem {

    protected ConsumedGuItem(Properties properties, GuSpec spec) {
        super(properties, spec);
    }

    @Override
    public boolean canBeVital() { return false; }

    @Override
    protected int drive(ServerPlayer player, ItemStack stack) {
        trySpendAndGrant(player, stack);
        return 1;
    }

    @Override
    protected int sneakApply(ServerPlayer player, ItemStack stack) { return drive(player, stack); }
}
