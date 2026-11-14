package net.alex.guzhenren.gameplay.aperture.storage;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 空窍[Aperture]存储查所存蛊的接口，使空窍不依赖定义蛊的物品包。
 * <ul>
 *   <li>{@code net.alex.guzhenren.vault} 的菜单与每秒结算在物品包之上，直问蛊类。</li>
 *   <li>物品侧实现基于蛊物品类，由入口类在玩家能开存储前交给 {@code ApertureStorageService.setStoredGu}。</li>
 *   <li>非蛊栈一律答 {@code null}、零或 {@code false}；{@link #getRank} 对不许存的蛊亦答 {@code null}，
 *       {@link #getToken} 对未绑定蛊答零。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureStorageService
 * @since 1.0.0
 */

public interface StoredGu {

    @Nullable Rank getRank(@NotNull ItemStack stack);

    @Nullable GuPath getPath(@NotNull ItemStack stack);

    long getToken(@NotNull ItemStack stack);

    boolean isVitalOf(@NotNull ItemStack stack, @NotNull Player player);

    void setBoundAperture(@NotNull ItemStack stack, int kind);
}
