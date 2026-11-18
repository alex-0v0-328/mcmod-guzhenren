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
 * 空窍存储接口 {@link StoredGu} 的物品栈适配实现：在模组初始化期通过 {@code ApertureStorageService.setStoredGu} 注入，
 * 供空窍存储层查询凡蛊[Mortal Gu]（{@link MortalGuItem}）的转数、流派（{@link GuItem}）与本命绑定
 * （{@link VitalGuState}）。
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
