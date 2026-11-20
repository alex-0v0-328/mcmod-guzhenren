package net.alex.guzhenren.entity.projectile;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 锁定月刃（{@link MoonBladeEntity}）与兽力冲击共用的二次贝塞尔飞行弧线，不依赖实体状态、可直接用 JUnit 测试。
 * <ul>
 *   <li>曲线从发射点 {@link #start} 出发，经弦线中点加上控制点偏移 {@link #bend} 后到达目标中心，耗时
 *       {@link #flightTicks} tick；调用方每 tick 重新传入目标当前中心，因此移动中的目标也能被准确追踪。</li>
 *   <li>{@link #aim} 为每发月刃随机掷出左右弯向（概率各半）：{@link #bend(Vec3, Vec3, boolean, float)} 沿弦线左侧水平单
 *       位向量 {@link #leftOf}（从发射点看向终点时的左侧，如向南飞行时指向正东）偏移弦长的 {@link #MIN_BEND_RATIO}～
 *       {@link #MAX_BEND_RATIO}，并额外向上抬升该偏移量的 {@link #LIFT_RATIO} 倍。</li>
 *   <li>{@link #flightTicks(double, float)} 按飞行速度折算弦长并保底 {@link #MIN_FLIGHT_TICKS} tick；第 {@code n} tick
 *       的轨迹点由 {@link #pointAt}（{@code t = n / flightTicks}）给出，对 {@code t} 的一阶导数 {@link #tangentAt} 则在
 *       兽力冲击爆开时作为范围伤害环的朝向轴。</li>
 *   <li>{@link #bankDegrees} 计算月刃渲染时的转弯侧倾角（正值使左缘下压，对应左弯），达到最大弯度时倾角为
 *       {@link #MAX_BANK_DEGREES}，其余按比例缩放；{@link #isAimable} 供服务端校验客户端上报的目标是否处于射程与视野前
 *       方。</li>
 * </ul>
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

    public float bankDegrees(@NotNull Vec3 end) {
        Vec3 chord = end.subtract(start);
        double across = Math.sqrt(chord.x * chord.x + chord.z * chord.z);
        if (across < FLAT) return 0.0F;

        double toLeft = bend.dot(leftOf(chord));
        double ratio = Math.abs(toLeft) / across;
        float bank = MAX_BANK_DEGREES * (float) Math.min(1.0D, ratio / MAX_BEND_RATIO);
        return toLeft < 0.0D ? -bank : bank;
    }

    static @NotNull Vec3 leftOf(@NotNull Vec3 chord) {
        Vec3 left = new Vec3(chord.z, 0.0D, -chord.x);
        return left.lengthSqr() < FLAT ? new Vec3(1.0D, 0.0D, 0.0D) : left.normalize();
    }
}
