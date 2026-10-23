package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.storage.StoredGu;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.item.GuItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The item side of {@link StoredGu}: answers aperture storage's questions from the Gu item classes. A store holds
 * mortal Gu ({@link MortalGuItem}); paths, binding tokens and owners are {@link GuItem}'s.
 *
 * <p>The mod's entry point hands one to {@code ApertureStorageService.setStoredGu}.
 *
 * @author Alex
 * @version 1.0.0
 * @see StoredGu
 * @since 1.0.0
 */

public final class StoredGuItems implements StoredGu {

    @Override
    public @Nullable Rank getRank(@NotNull ItemStack stack) {
        return stack.getItem() instanceof MortalGuItem gu ? gu.rank() : null;
    }

    @Override
    public @Nullable GuPath getPath(@NotNull ItemStack stack) {
        return stack.getItem() instanceof GuItem gu ? gu.path() : null;
    }

    @Override
    public long getToken(@NotNull ItemStack stack) { return GuItem.token(stack); }

    @Override
    public boolean isVitalOf(@NotNull ItemStack stack, @NotNull Player player) {
        return GuItem.isVitalOf(stack, player);
    }
}
