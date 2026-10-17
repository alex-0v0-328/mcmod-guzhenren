package net.alex.guzhenren.core;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * The one safe way to take back a toss: {@link #stow} puts the tossed stack back in the bag and never drops.
 *
 * <p>⚠ {@code placeItemBackInInventory} drops what does not fit, and that drop posts {@code ItemTossEvent} again:
 * a handler that cancels a toss and puts it back that way recurses on a full bag until the server crashes -- a
 * cursor stack or the 2x2 crafting grid falling back on a closing screen is enough. {@link #stow} adds what fits
 * and leaves the rest on the tossed entity, hovering without gravity where it was dropped, so it never falls into
 * the void and can be picked up once there is room. It answers whether everything fit: only then may the caller
 * cancel the toss, because the event has already taken the entity out of the world and a canceled toss whose rest
 * did not fit would delete it.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class TossedItems {

    private TossedItems() {}

    public static boolean stow(@NotNull Player player, @NotNull ItemEntity tossed) {
        ItemStack rest = tossed.getItem().copy();
        player.getInventory().add(rest);
        if (rest.isEmpty()) return true;

        tossed.setItem(rest);
        tossed.setNoGravity(true);
        tossed.setDeltaMovement(Vec3.ZERO);
        return false;
    }
}
