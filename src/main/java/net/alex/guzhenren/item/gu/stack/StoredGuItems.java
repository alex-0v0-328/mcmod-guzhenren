package net.alex.guzhenren.item.gu.stack;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.storage.StoredGu;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@link StoredGu} 的物品侧，答空窍[Aperture]存储的查询；入口类交给 {@code ApertureStorageService.setStoredGu}。
 * 只存凡蛊（{@link MortalGuItem}）；道[Path]取自 {@link GuItem}，本命绑定取自 {@link VitalGuState}。
 *
 * @author Alex
 * @version 1.0.0
 * @see StoredGu
 * @since 1.0.0
 */

public final class StoredGuItems implements StoredGu {

    @Override
    public @Nullable Rank getRank(@NotNull ItemStack stack) {
        return stack.getItem() instanceof MortalGuItem gu ? gu.getRank() : null;
    }

    @Override
    public @Nullable GuPath getPath(@NotNull ItemStack stack) {
        return stack.getItem() instanceof GuItem gu ? gu.getPath() : null;
    }

    @Override
    public long getToken(@NotNull ItemStack stack) { return VitalGuState.getToken(stack); }

    @Override
    public boolean isVitalOf(@NotNull ItemStack stack, @NotNull Player player) {
        return VitalGuState.isVitalOf(stack, player);
    }

    @Override
    public void setBoundAperture(@NotNull ItemStack stack, int kind) { VitalGuState.setBoundAperture(stack, kind); }
}
