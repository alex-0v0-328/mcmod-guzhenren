package net.alex.guzhenren.gameplay.aperture.storage;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 空窍存储访问蛊虫物品属性的桥接接口，使 {@code gameplay.aperture} 不依赖定义蛊虫的 {@code item} 包。
 * <ul>
 *   <li>上层 {@code net.alex.guzhenren.vault} 的容器菜单与每秒养护结算位于物品包之上，可直接访问蛊物品类；本包则通过模
 *       组主类注入 {@link ApertureStorageService#setStoredGu} 的实现查询。</li>
 *   <li>非蛊物品栈一律返回 {@code null}、{@code 0} 或 {@code false}；{@link #getRank} 对不可存入空窍的蛊虫也返回
 *       {@code null}，{@link #getToken} 对未绑定本命蛊返回 {@code 0}。</li>
 * </ul>
 */

public interface StoredGu {

    @Nullable Rank getRank(@NotNull ItemStack stack);

    @Nullable GuPath getPath(@NotNull ItemStack stack);

    long getToken(@NotNull ItemStack stack);

    boolean isVitalOf(@NotNull ItemStack stack, @NotNull Player player);

    void setBoundAperture(@NotNull ItemStack stack, int kind);
}
