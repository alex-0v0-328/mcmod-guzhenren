package net.alex.guzhenren.gameplay.blissful;

import net.alex.guzhenren.gameplay.secretrealm.RealmSite;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 一处福地的落位：山心、山巅高度、行宫站立点与入口列，全部相对秘境山心推出（{@link BlissfulLands#siteOf}）。
 * <ul>
 *   <li>{@code center} 是荡魂山山心方块，y 为山底（第一层山石）；山心与山底取自秘境的结构起点（
 *       {@link net.alex.guzhenren.gameplay.secretrealm.SecretRealms}）。</li>
 *   <li>{@code summitY} = 山底 + 秘境的 {@code summitRise()}：山巅最高一层荡魂山石的 y；行宫站立点 = 山心 +
 *       {@link #PALACE}；入口 = 山心 + 锚定维度的出生点偏移（{@link ModDimensions#BLISSFUL_LAND_FOX_IMMORTAL_SPAWN}，正
 *       南 160 格），y 未定（记山底），落地时取地表。</li>
 *   <li>⚠ {@link #PALACE} 与 Gu World 山形（{@code tools/soulsunder_shape.py}）是会话间契约：洞厅中心、头顶至少 3 格空；
 *       Gu World 改山形时两边同改。</li>
 * </ul>
 */

public record LandSite(@NotNull BlockPos center, int summitY, @NotNull BlockPos palace, @NotNull BlockPos entry) {

    public static final BlockPos PALACE = new BlockPos(-2, 40, -3);

    public static @NotNull LandSite of(@NotNull RealmSite realm, int summitRise) {
        BlockPos center = realm.center();
        Vec3 spawn = ModDimensions.BLISSFUL_LAND_FOX_IMMORTAL_SPAWN;
        return new LandSite(center, center.getY() + summitRise, center.offset(PALACE),
                center.offset((int) Math.floor(spawn.x), 0, (int) Math.floor(spawn.z)));
    }
}
