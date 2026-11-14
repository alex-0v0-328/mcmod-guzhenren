package net.alex.guzhenren.entity.projectile;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 兽力冲击掷出 {@link MoonBladeArc} 的方式；不依赖实体，纯 JUnit 可测。
 * <ul>
 *   <li>月刃只左右弯；兽力冲击绕弦在上半圆任意弯，从左侧水平经正上方到右侧水平；不向下：挡路方块会让它消失不爆。</li>
 *   <li>同拳冲击按掷出顺序从左到右等分半圆，各在本扇区随机取角，绝无同弧。</li>
 *   <li>⚠ 三比例暂定：弯度为弦长 {@link #MIN_BEND_RATIO}～{@link #MAX_BEND_RATIO}；弧顶沿弦前后挪至多
 *       {@link #MAX_SHIFT_RATIO}，早转晚转各异。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastChargeEntity
 * @since 1.0.0
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

    /** {@code turn} 取 0 为左侧水平，0.5 为正上方，1 为右侧水平。 */
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
