package net.alex.guzhenren.entity.projectile;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 兽力冲击（{@link BeastChargeEntity}）生成飞行弧线 {@link MoonBladeArc} 的纯数学工厂，不依赖实体状态、可直接用 JUnit
 * 测试。
 * <ul>
 *   <li>月刃仅在水平方向左右弯曲，而兽力冲击绕起止弦线在上半圆任意偏转（{@link #bend} 中 {@code turn} 取 {@code 0} 为左
 *       侧水平、{@code 0.5} 为正上方、{@code 1} 为右侧水平），绝不向下弯曲以免撞地提前消散。</li>
 *   <li>同一拳发出的多道冲击按序号从左到右等分上半圆，各自在所属扇区内随机取角，保证同拳弧线互不重叠。</li>
 *   <li>弯曲幅度在弦长的 {@link #MIN_BEND_RATIO}～{@link #MAX_BEND_RATIO} 之间随机，控制点另沿弦线方向前后偏移至多
 *       {@link #MAX_SHIFT_RATIO}，使各道冲击的转折早晚各异。</li>
 * </ul>
 */

public final class BeastChargeArc {

    public static final float MIN_BEND_RATIO = 0.15F;
    public static final float MAX_BEND_RATIO = 0.45F;
    public static final float MAX_SHIFT_RATIO = 0.2F;

    private BeastChargeArc() {}

    public static @NotNull MoonBladeArc aim(@NotNull Vec3 start, @NotNull Vec3 end, float speed,
                                            @NotNull RandomSource random, int index, int count) {
        float turn = (index + random.nextFloat()) / Math.max(1, count);
        float ratio = Mth.lerp(random.nextFloat(), MIN_BEND_RATIO, MAX_BEND_RATIO);
        float shift = Mth.lerp(random.nextFloat(), -MAX_SHIFT_RATIO, MAX_SHIFT_RATIO);
        return new MoonBladeArc(start, bend(start, end, turn, ratio, shift),
                MoonBladeArc.flightTicks(start.distanceTo(end), speed));
    }

    public static @NotNull Vec3 bend(@NotNull Vec3 start, @NotNull Vec3 end, float turn, float ratio, float shift) {
        Vec3 chord = end.subtract(start);
        double length = chord.length();
        Vec3 left = MoonBladeArc.leftOf(chord);
        Vec3 along = length < 1.0E-6D ? Vec3.ZERO : chord.scale(1.0D / length);
        Vec3 over = length < 1.0E-6D ? new Vec3(0.0D, 1.0D, 0.0D) : along.cross(left);
        double angle = turn * Math.PI;
        return left.scale(Math.cos(angle) * length * ratio)
                .add(over.scale(Math.sin(angle) * length * ratio))
                .add(along.scale(length * shift));
    }
}
