package net.alex.guzhenren.gameplay.aperture.storage;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What aperture storage needs to know about the Gu [蛊] it holds, so the aperture does not depend on the item
 * package that defines them: which stacks it may store and at what rank, the vital Gu's binding token, and the
 * upkeep a stored Gu pays while it waits.
 *
 * <p>The item side fills it: the mod's entry point hands {@code ApertureStorageService.setStoredGu} the one built on
 * the Gu item classes, before any player can open a store. A stack that is no Gu answers {@code null}, zero or
 * {@code false} everywhere.
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureStorageService
 * @since 1.0.0
 */

public interface StoredGu {

    /** The rank of a Gu the store may hold, or {@code null} when the stack is no storable Gu. */
    @Nullable Rank rank(@NotNull ItemStack stack);

    /** The path of a Gu, or {@code null} when the stack is no Gu. */
    @Nullable GuPath path(@NotNull ItemStack stack);

    /** The vital binding token on a stack; {@code 0} when unbound. */
    long token(@NotNull ItemStack stack);

    boolean isVitalOf(@NotNull ItemStack stack, @NotNull Player player);

    /** Binds a Gu as the player's vital Gu of {@code aperture}, unless it already is. */
    void bindVital(@NotNull ItemStack stack, @NotNull ServerPlayer player, int aperture);

    void unbind(@NotNull ItemStack stack);

    /** Whether the stack may sit in the player's vital slot: a refined tended Gu that is no one else's vital Gu. */
    boolean canBeVital(@NotNull ItemStack stack, @NotNull Player player);

    boolean isTended(@NotNull ItemStack stack);

    boolean isRefinedTended(@NotNull ItemStack stack);

    /** Advances a stored tended Gu's upkeep by {@code days}; {@code true} when it starved. */
    boolean upkeep(@NotNull ServerPlayer player, @NotNull ItemStack stack, long days);

    /** A tended Gu's death of starvation, with its message to the holder. */
    void starve(@NotNull ServerPlayer player, @NotNull ItemStack stack);
}
