package net.alex.guzhenren.gameplay.flight;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * The cruise of second-stage flight [二段飞行]: pure numbers, so the pure JVM tests can pin them;
 * {@link SecondStageService#travel} moves the player with them on both ends.
 *
 * <p>The flight follows the look, pitch included, with no strafe and no climb input. A throttle between 0 and 1
 * scales {@link #getTopSpeed}: {@link #nextThrottle} fills it in {@link #RAMP_TICKS} while the player sprints and
 * drains it in {@link #COAST_TICKS} once the sprint stops. An entry starts at {@link #ENTRY_THROTTLE}, the speed of
 * vanilla's sprinting flight, so entering never brakes. {@link #nextVelocity} moves the velocity
 * {@link #STEERING} of the way to the throttled look each tick, which also turns it: 90% of a turn lands within
 * 9 ticks (≈0.45 s).
 *
 * <p>Speeds are blocks a tick of actual movement. Plain flight at vanilla's flying speed moves
 * {@link #PLAIN_FLIGHT_SPEED}: its input of 0.98 accelerates against an air drag of 0.91, which settles at
 * 0.98 / 0.09 times the flying speed. The top is {@link SecondStageService#SPEED_MULTIPLIER} times that, ≈3.3 blocks a
 * tick (≈65 a second). {@link #BARRIER_SPEED}, three times plain flight, is where the sound barrier rings form, a
 * wall hit counts as a crash and a body in the path is rammed.
 *
 * <p>A wall costs what an elytra pays ({@link #getWallDamage}): ten points a block a tick of horizontal speed lost,
 * less three. A crash blasts a crater of {@link #getCrashPower}: {@link #CRASH_POWER_PER_SPEED} a block a tick lost,
 * between {@link #MIN_CRASH_POWER} at the barrier and {@link #MAX_CRASH_POWER}, a TNT's, ≈3.9 for a head-on hit at
 * full speed. A ram deals {@link #RAM_DAMAGE_PER_SPEED} a block a tick of the flier's speed ({@link #getRamDamage}).
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

    public static @NotNull Vec3 getRamPush(@NotNull Vec3 velocity) {
        return velocity.normalize().scale(RAM_KNOCKBACK).add(0.0D, RAM_LIFT, 0.0D);
    }
}
