package net.alex.guzhenren.gameplay.secretrealm;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

/**
 * 一处天地秘境[Secret Realm]在世界中的范围：以山心方块 {@code center} 为轴、水平半径 {@code radius} 格、贯通全高的圆
 * 柱。
 * <ul>
 *   <li>只比水平距离，从山心方块的中心（{@code x + 0.5}、{@code z + 0.5}）量起；{@code center} 的 y 不参与判定。</li>
 *   <li>⚠ 性能：判定只做一次平方距离比较，不开方、不查方块；{@link #box} 只供实体查询粗筛，结果仍须再过
 *       {@link #contains}。</li>
 * </ul>
 */

public record RealmSite(@NotNull BlockPos center, int radius) {

    public boolean contains(double x, double z) { return isWithin(x, z, radius); }

    public boolean contains(@NotNull Entity entity) { return contains(entity.getX(), entity.getZ()); }

    public boolean isWithin(double x, double z, int distance) {
        double dx = x - (center.getX() + 0.5D);
        double dz = z - (center.getZ() + 0.5D);
        return dx * dx + dz * dz <= (double) distance * distance;
    }

    public @NotNull AABB box(@NotNull Level level) {
        double x = center.getX() + 0.5D;
        double z = center.getZ() + 0.5D;
        return new AABB(x - radius, level.getMinBuildHeight(), z - radius,
                x + radius, level.getMaxBuildHeight(), z + radius);
    }
}
