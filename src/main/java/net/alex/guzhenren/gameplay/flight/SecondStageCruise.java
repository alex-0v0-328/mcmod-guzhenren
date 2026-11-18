package net.alex.guzhenren.gameplay.flight;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 二段飞行[Second-Stage Flight]的巡航运动学与碰撞数值纯函数，供双端 {@link SecondStageService#travel} 驱动移动并支持裸
 * JVM 单元测试。
 * <ul>
 *   <li>沿视线方向三维巡航（含俯仰，屏蔽水平横移与垂直升降）：油门在 {@code [0, 1]} 内缩放 {@link #getTopSpeed}，
 *       {@link #nextThrottle} 在疾跑时经 {@link #RAMP_TICKS} 爬满、停止疾跑时经 {@link #COAST_TICKS} 归零；进入二段飞行
 *       时从 {@link #ENTRY_THROTTLE}（对应原版疾跑飞行速度）起步，不发生顿挫减速。</li>
 *   <li>{@link #nextVelocity} 每 tick 将当前速度向「油门 × 视线方向」插值靠拢 {@link #STEERING}（兼作转向惯性，约 9
 *       tick 转过 90%）。</li>
 *   <li>速度单位为每 tick 实际位移格数：默认飞行速度下普通飞行为 {@link #PLAIN_FLIGHT_SPEED}（输入 {@code 0.98} 与空气
 *       阻力 {@code 0.91} 平衡于 {@code 0.98 / 0.09} 倍），二段极速为其 {@link SecondStageService#SPEED_MULTIPLIER} 倍
 *       （约 3.3 格/tick，合 65 格/秒）。</li>
 *   <li>{@link #BARRIER_SPEED}（普通飞行的 3 倍）为音障阈值：超过该速度时产生音障环、撞击固体触发撞毁，且沿途撞飞生
 *       物。</li>
 *   <li>碰撞自伤（{@link #getWallDamage}）沿用原版鞘翅撞墙公式（每损失 1 格/tick 三维速度扣 10 点再减 3，地面与天花板同
 *       样计入）；撞毁炸坑威力（{@link #getCrashPower}）按损失速度乘以 {@link #CRASH_POWER_PER_SPEED} 并夹取在
 *       {@link #MIN_CRASH_POWER} 至 {@link #MAX_CRASH_POWER}（TNT 级）之间；撞人伤害（{@link #getRamDamage}）按每 1
 *       格/tick 飞行速度乘以 {@link #RAM_DAMAGE_PER_SPEED}。</li>
 *   <li>爆炸抗性低于 {@link #FRAGILE_RESISTANCE}（{@code 0.5}）的方块视为脆弱方块（{@link #isFragile}，含树叶、玻璃、
 *       雪、仙人掌、蘑菇块、黏液块、蜂蜜块、脚手架；泥土与沙子抗性为 {@code 0.5} 不计入）：超过音障时服务端提前破除前方
 *       {@link #BREAK_LOOKAHEAD_TICKS} tick 轨迹内的脆弱方块，使客户端穿过时不发生碰撞。</li>
 * </ul>
 */

public final class SecondStageCruise {

    private SecondStageCruise() {}

    public static final int RAMP_TICKS = 30;
    public static final int COAST_TICKS = 15;
    public static final double ENTRY_THROTTLE = 2.0D / SecondStageService.SPEED_MULTIPLIER;
    public static final double STEERING = 0.25D;
    public static final float DEFAULT_FLYING_SPEED = 0.05F;
    public static final double PLAIN_FLIGHT_SPEED = DEFAULT_FLYING_SPEED * 0.98D / 0.09D;
    public static final double BARRIER_SPEED = 3.0D * PLAIN_FLIGHT_SPEED;
    public static final double CRASH_POWER_PER_SPEED = 1.2D;
    public static final float MIN_CRASH_POWER = 2.0F;
    public static final float MAX_CRASH_POWER = 4.0F;
    public static final double RAM_DAMAGE_PER_SPEED = 4.0D;
    public static final double RAM_KNOCKBACK = 2.0D;
    public static final double RAM_LIFT = 0.4D;
    public static final int RAM_COOLDOWN_TICKS = 20;
    public static final float FRAGILE_RESISTANCE = 0.5F;
    public static final int BREAK_LOOKAHEAD_TICKS = 3;
    private static final double EMPTY = 1.0E-9D;

    public static double nextThrottle(double throttle, boolean sprinting) {
        double next = sprinting ? throttle + 1.0D / RAMP_TICKS : throttle - 1.0D / COAST_TICKS;
        if (next < EMPTY) return 0.0D;
        return Math.min(next, 1.0D);
    }

    public static double getTopSpeed(float flyingSpeed) {
        return SecondStageService.SPEED_MULTIPLIER * PLAIN_FLIGHT_SPEED * flyingSpeed / DEFAULT_FLYING_SPEED;
    }

    public static @NotNull Vec3 nextVelocity(@NotNull Vec3 velocity, @NotNull Vec3 look, double throttle,
                                             double topSpeed) {
        Vec3 target = look.normalize().scale(throttle * topSpeed);
        return velocity.add(target.subtract(velocity).scale(STEERING));
    }

    public static float getWallDamage(double lostSpeed) { return (float) (lostSpeed * 10.0D - 3.0D); }

    public static boolean isCrash(double lostSpeed) { return lostSpeed >= BARRIER_SPEED; }

    public static float getCrashPower(double lostSpeed) {
        return (float) Math.clamp(lostSpeed * CRASH_POWER_PER_SPEED, MIN_CRASH_POWER, MAX_CRASH_POWER);
    }

    public static float getRamDamage(double speed) { return (float) (speed * RAM_DAMAGE_PER_SPEED); }

    public static boolean isFragile(float explosionResistance) { return explosionResistance < FRAGILE_RESISTANCE; }

    public static @NotNull Vec3 getRamPush(@NotNull Vec3 velocity) {
        return velocity.normalize().scale(RAM_KNOCKBACK).add(0.0D, RAM_LIFT, 0.0D);
    }
}
