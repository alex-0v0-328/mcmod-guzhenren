package net.alex.guzhenren.item.gu.tending;

import java.util.Map;
import java.util.WeakHashMap;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The forced use [强行催动]: a use its Gu cannot pay in hunger [饱食] -- the bar after the bag's meal -- is first
 * refused with a warning, and only the same Gu used again within {@link #WINDOW_TICKS} goes through, its effect
 * landing before the Gu dies of exhaustion. {@code TendedGuItem.drive} asks {@link #confirm}.
 *
 * <p>⚠ The window is server-only and transient: one per player, in no save and no data component, and gone with the
 * player object (a death, a logout). "The same Gu" is the same stack object, so two Gu of one kind never confirm
 * each other; a stack moved by hand may become a new object, which costs a fresh warning, never an unwanted death.
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @see GuHunger
 * @since 1.0.0
 */

public final class GuForcedUse {

    public static final int WINDOW_TICKS = 5 * Ticks.SECOND;
    private static final Map<ServerPlayer, Pending> PENDING = new WeakHashMap<>();

    private record Pending(ItemStack stack, int until) {}

    private GuForcedUse() {}

    /** True, closing the window, when this stack was warned within the window; otherwise warns it afresh. */
    public static boolean confirm(ServerPlayer player, ItemStack stack) {
        int now = player.server.getTickCount();
        Pending pending = PENDING.remove(player);
        if (pending != null && pending.stack() == stack && now <= pending.until()) return true;

        PENDING.put(player, new Pending(stack, now + WINDOW_TICKS));
        return false;
    }
}
