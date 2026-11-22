package net.alex.guzhenren.gameplay.blissful;

import java.util.List;
import net.alex.guzhenren.gameplay.blissful.territory.Territory;
import net.alex.guzhenren.gameplay.secretrealm.SecretRealm;
import net.alex.guzhenren.gameplay.secretrealm.SoulsunderMountain;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 一处福地[Blissful Land]的固定设定：所在维度、中心秘境、山巅与行宫坐标、疆域尺寸与田区。
 * <ul>
 *   <li>每个福地一个 Gu World 维度；主模组只按维度键引用（{@link ModDimensions}），维度不存在（没装 Gu World）时整个福
 *       地关闭，见 {@link BlissfulLands}。</li>
 *   <li>⚠ {@link #summitY}、{@link #palace} 是会话间契约：与 Gu World 荡魂山迁入后的山体一致（山巅最高一层荡魂山石
 *       y 181、行宫锚点站立点 (6, 104, 5)），Gu World 改山形时两边同改。山巅判定的水平中心取秘境山心，不另存。</li>
 *   <li>疆域边长 {@code 2 * half} 区块，中心 {@code 2 * core} 见方不可割弃（{@link Territory}）；田区是从
 *       {@link #farmMin} 起 {@code farmChunks} 见方的区块，标准田只开在这里（{@link FarmWork}）。</li>
 * </ul>
 */

public record BlissfulLand(@NotNull String id, @NotNull ResourceKey<Level> dimension, @NotNull SecretRealm realm,
                           int summitY, @NotNull BlockPos palace, int half, int core, @NotNull ChunkPos farmMin,
                           int farmChunks) {

    public static final int SUMMIT_RADIUS = 8;
    public static final BlissfulLand FOX_IMMORTAL = new BlissfulLand("fox_immortal_blissful_land",
            ModDimensions.FOX_IMMORTAL_BLISSFUL_LAND, SoulsunderMountain.INSTANCE, 181, new BlockPos(6, 104, 5),
            612, 32, new ChunkPos(-2, 11), 4);
    public static final List<BlissfulLand> ALL = List.of(FOX_IMMORTAL);

    public @NotNull String nameKey() { return "guzhenren.blissful_land." + id; }

    public @NotNull Territory wholeTerritory() { return Territory.whole(half, core); }
}
