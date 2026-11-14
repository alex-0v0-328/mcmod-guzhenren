package net.alex.guzhenren.entity.projectile;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 锁定月刃的飞行曲线；不依赖实体，纯 JUnit 可测。
 * <ul>
 *   <li>二次贝塞尔：{@code start} 经弦中点偏 {@code bend} 的控制点到目标中心；调用方每 tick 重读中心，移动目标也能命
 *       中。</li>
 *   <li>{@link #aim} 每发掷一次弯：左右各半，弦长的 {@link #MIN_BEND_RATIO}～{@link #MAX_BEND_RATIO}，另加其
 *       {@link #LIFT_RATIO} 抬升；「左」指沿弦看射手左侧。</li>
 *   <li>{@link #flightTicks} 按速度折算弦长，至少 {@link #MIN_FLIGHT_TICKS}；第 n tick 位于 {@link #pointAt}
 *       （{@code t = n / flightTicks}），{@link #tangentAt} 为其对 t 的导数，兽力冲击爆开时作范围冲击轴。</li>
 *   <li>{@link #bankDegrees}：渲染转弯侧倾，倒向弯侧，最大弯度时 {@link #MAX_BANK_DEGREES}，以下成比例；
 *       {@link #isAimable}：服务端校验客户端所选目标在射程内、视线前方。</li>
 * </ul>
 *
 * @param start 发射点
 * @param bend 控制点相对弦中点的偏移
 * @param flightTicks 从发射点到目标的 tick 数
 * @author Alex
 * @version 1.0.0
 * @see MoonBladeEntity
 * @since 1.0.0
 */

public record MoonBladeArc(Vec3 start, Vec3 bend, int flightTicks) {

    public static final float MIN_BEND_RATIO = 0.15F;
    public static final float MAX_BEND_RATIO = 0.35F;
    public static final float LIFT_RATIO = 0.2F;
    public static final float MAX_BANK_DEGREES = 30.0F;
    public static final int MIN_FLIGHT_TICKS = 2;
    private static final double FLAT = 1.0E-6D;

    public static @NotNull MoonBladeArc aim(@NotNull Vec3 start, @NotNull Vec3 end, float speed,
                                            @NotNull RandomSource random) {
        boolean left = random.nextBoolean();
        float ratio = Mth.lerp(random.nextFloat(), MIN_BEND_RATIO, MAX_BEND_RATIO);
        return of(start, end, left, ratio, speed);
    }

    public static @NotNull MoonBladeArc of(@NotNull Vec3 start, @NotNull Vec3 end, boolean left, float ratio,
                                           float speed) {
        return new MoonBladeArc(start, bend(start, end, left, ratio), flightTicks(start.distanceTo(end), speed));
    }

    /** 控制点偏移：横向弦长的 {@code ratio}，再加其 {@link #LIFT_RATIO} 向上。 */
    public static @NotNull Vec3 bend(@NotNull Vec3 start, @NotNull Vec3 end, boolean left, float ratio) {
        Vec3 chord = end.subtract(start);
        double size = chord.length() * ratio;
        Vec3 side = leftOf(chord).scale(left ? size : -size);
        return side.add(0.0D, size * LIFT_RATIO, 0.0D);
    }

    public static int flightTicks(double distance, float speed) {
        return Math.max(MIN_FLIGHT_TICKS, Mth.ceil(distance / speed));
    }

    public static boolean isAimable(@NotNull Vec3 eye, @NotNull Vec3 look, @NotNull Vec3 target, double range) {
        Vec3 to = target.subtract(eye);
        return to.lengthSqr() <= range * range && to.dot(look) > 0.0D;
    }

    public @NotNull Vec3 pointAt(@NotNull Vec3 end, double t) {
        double u = 1.0D - t;
        Vec3 control = start.add(end).scale(0.5D).add(bend);
        return start.scale(u * u).add(control.scale(2.0D * u * t)).add(end.scale(t * t));
    }

    public @NotNull Vec3 tangentAt(@NotNull Vec3 end, double t) {
        Vec3 control = start.add(end).scale(0.5D).add(bend);
        return control.subtract(start).scale(2.0D * (1.0D - t)).add(end.subtract(control).scale(2.0D * t));
    }

    /** 正值使刃左缘向下，对应左弯。 */
    public float bankDegrees(@NotNull Vec3 end) {
        Vec3 chord = end.subtract(start);
        double across = Math.sqrt(chord.x * chord.x + chord.z * chord.z);
        if (across < FLAT) return 0.0F;

        double toLeft = bend.dot(leftOf(chord));
        double ratio = Math.abs(toLeft) / across;
        float bank = MAX_BANK_DEGREES * (float) Math.min(1.0D, ratio / MAX_BEND_RATIO);
        return toLeft < 0.0D ? -bank : bank;
    }

    /** 沿 {@code chord} 飞行的左侧水平单位向量；向南飞时为东。 */
    static @NotNull Vec3 leftOf(@NotNull Vec3 chord) {
        Vec3 left = new Vec3(chord.z, 0.0D, -chord.x);
        return left.lengthSqr() < FLAT ? new Vec3(1.0D, 0.0D, 0.0D) : left.normalize();
    }
}
