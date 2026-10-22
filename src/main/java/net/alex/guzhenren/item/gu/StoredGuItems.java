package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.storage.StoredGu;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.item.GuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The item side of {@link StoredGu}: answers aperture storage's questions from the Gu item classes. A store holds
 * mortal Gu ({@link MortalGuItem}); its vital slot takes a refined {@link TendedGuItem} that may be vital and is
 * no one else's; binding and tokens are {@link GuItem}'s, upkeep is {@link GuUpkeep}'s.
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
    public @Nullable Rank rank(@NotNull ItemStack stack) {
        return stack.getItem() instanceof MortalGuItem gu ? gu.rank() : null;
    }

    @Override
    public @Nullable GuPath path(@NotNull ItemStack stack) {
        return stack.getItem() instanceof GuItem gu ? gu.path() : null;
    }

    @Override
    public long token(@NotNull ItemStack stack) { return GuItem.token(stack); }

    @Override
    public boolean isVitalOf(@NotNull ItemStack stack, @NotNull Player player) {
        return GuItem.isVitalOf(stack, player);
    }

    @Override
    public void bindVital(@NotNull ItemStack stack, @NotNull ServerPlayer player, int aperture) {
        if (stack.getItem() instanceof GuItem
                && (!GuItem.isCurrentVital(stack, player) || GuItem.boundIndex(stack, player) != aperture)) {
            GuItem.bind(stack, player, aperture);
        }
    }

    @Override
    public void unbind(@NotNull ItemStack stack) { GuItem.unbind(stack); }

    @Override
    public boolean canBeVital(@NotNull ItemStack stack, @NotNull Player player) {
        return stack.getItem() instanceof TendedGuItem gu && gu.refined(stack) && gu.canBeVital()
                && !GuItem.isForeignVital(stack, player);
    }

    @Override
    public boolean isTended(@NotNull ItemStack stack) { return stack.getItem() instanceof TendedGuItem; }

    @Override
    public boolean isRefinedTended(@NotNull ItemStack stack) {
        return stack.getItem() instanceof TendedGuItem gu && gu.refined(stack);
    }

    @Override
    public boolean upkeep(@NotNull ServerPlayer player, @NotNull ItemStack stack, long days) {
        return GuUpkeep.tickInContainer(player, stack, days);
    }

    @Override
    public void starve(@NotNull ServerPlayer player, @NotNull ItemStack stack) { TendedGuItem.starved(player, stack); }
}
