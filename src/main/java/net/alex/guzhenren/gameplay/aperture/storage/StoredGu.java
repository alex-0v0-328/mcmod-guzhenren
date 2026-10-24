package net.alex.guzhenren.gameplay.aperture.storage;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What aperture storage needs to know about the Gu [蛊] it holds, so the aperture does not depend on the item
 * package that defines them: which stacks it may store and at what rank, a Gu's path, and the Vital Gu's binding
 * token and owner. The vault's menu and per-second settle sit above the item package
 * ({@code net.alex.guzhenren.vault}) and ask the Gu classes themselves.
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
    @Nullable Rank getRank(@NotNull ItemStack stack);

    /** The path of a Gu, or {@code null} when the stack is no Gu. */
    @Nullable GuPath getPath(@NotNull ItemStack stack);

    /** The vital binding token on a stack; {@code 0} when unbound. */
    long getToken(@NotNull ItemStack stack);

    boolean isVitalOf(@NotNull ItemStack stack, @NotNull Player player);
}
