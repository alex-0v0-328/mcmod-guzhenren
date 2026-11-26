package net.alex.guzhenren.gameplay.blissful;

import java.util.List;
import net.alex.guzhenren.gameplay.blissful.territory.Territory;
import net.alex.guzhenren.gameplay.secretrealm.SecretRealm;
import net.alex.guzhenren.gameplay.secretrealm.SoulsunderMountain;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 一处福地[Blissful Land]的固定设定：所在维度、中心秘境、疆域尺寸。
 * <ul>
 *   <li>每个福地一个 Gu World 维度；主模组只按维度键引用（{@link ModDimensions}），维度不存在（没装 Gu World）时整个福
 *       地关闭，见 {@link BlissfulLands}。</li>
 *   <li>山巅、行宫与入口不存在这里：山心随世界种子挑址，都由 {@link BlissfulLands#siteOf} 相对山心推出（
 *       {@link LandSite}）。</li>
 *   <li>疆域边长 {@code 2 * half} 区块，中心 {@code 2 * core} 见方不可割弃（{@link Territory}）；标准田与牧场在疆域里
 *       自由选址，不另划区（{@code map.Siting}）。</li>
 *   <li>英文名福地在前：{@code name} 只存福地自己的名字，完整 ID 由 {@link #id()} 加 {@code blissful_land_} 前缀（实体、
 *       存档数据都用它），翻译键 {@link #nameKey()} 放在 {@code guzhenren.blissful_land.} 下。</li>
 * </ul>
 */

public record BlissfulLand(@NotNull String name, @NotNull ResourceKey<Level> dimension, @NotNull SecretRealm realm,
                           int half, int core) {

    public static final int SUMMIT_RADIUS = 8;
    public static final BlissfulLand FOX_IMMORTAL = new BlissfulLand("fox_immortal",
            ModDimensions.BLISSFUL_LAND_FOX_IMMORTAL, SoulsunderMountain.INSTANCE, 612, 32);
    public static final List<BlissfulLand> ALL = List.of(FOX_IMMORTAL);

    public @NotNull String id() { return "blissful_land_" + name; }

    public @NotNull String nameKey() { return "guzhenren.blissful_land." + name; }

    public @NotNull Territory wholeTerritory() { return Territory.whole(half, core); }
}
