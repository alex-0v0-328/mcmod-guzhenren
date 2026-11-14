package net.alex.guzhenren.gameplay.flight;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 二段飞行[Second-Stage Flight]的巡航数值，纯 JVM 测试可钉；{@link SecondStageService#travel} 两端用它移动玩家。
 * <ul>
 *   <li>沿视线飞，含俯仰，无平移、升降。油门 0–1 乘 {@link #getTopSpeed}：{@link #nextThrottle} 疾跑
 *       {@link #RAMP_TICKS} 加满，停跑 {@link #COAST_TICKS} 耗尽；进入从 {@link #ENTRY_THROTTLE}（原版疾跑飞行速度）
 *       起，不减速。</li>
 *   <li>{@link #nextVelocity} 每 tick 向「油门 × 视线」靠拢 {@link #STEERING}，兼作转向，9 tick（约 0.45 秒）转过
 *       90%。</li>
 *   <li>速度单位：每 tick 实际移动格数。原版飞行速度下普通飞行为 {@link #PLAIN_FLIGHT_SPEED}（输入 0.98 抗 0.91 空气阻
 *       力，稳定于飞行速度的 0.98 / 0.09 倍）；最高为其 {@link SecondStageService#SPEED_MULTIPLIER} 倍，约 3.3 格/tick
 *       （约 65 格/秒）。</li>
 *   <li>{@link #BARRIER_SPEED} 为普通飞行 3 倍：过此出音障环，撞墙算撞毁，路径上的生物被撞飞。</li>
 *   <li>撞墙按鞘翅掉血（{@link #getWallDamage}）：每损失 1 格/tick 速度 10 点再减 3，三维计速，地面、天花板也算
 *       墙。</li>
 *   <li>撞毁炸坑威力 {@link #getCrashPower}：每损失 1 格/tick 计 {@link #CRASH_POWER_PER_SPEED}，限于音障时
 *       {@link #MIN_CRASH_POWER} 到 TNT 级 {@link #MAX_CRASH_POWER}，满速正撞约 3.9；撞人伤害每 1 格/tick 飞行速度计
 *       {@link #RAM_DAMAGE_PER_SPEED}（{@link #getRamDamage}）。</li>
 *   <li>脆弱方块（{@link #isFragile}）爆炸抗性低于 {@link #FRAGILE_RESISTANCE}：树叶 0.2、玻璃 0.3、雪 0.1、仙人掌
 *       0.4、蘑菇块 0.2，黏液块、蜂蜜块、脚手架 0；泥土、沙 0.5 不算。</li>
 *   <li>过音障即撞碎不撞上：服务端破坏碰撞箱之后 {@link #BREAK_LOOKAHEAD_TICKS} tick 扫过的全部脆弱方块（满速
 *       约 10 格），早一个包的客户端到达时已是空气。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see SecondStageService
 * @since 1.0.0
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
