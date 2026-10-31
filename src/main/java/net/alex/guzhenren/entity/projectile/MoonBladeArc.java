package net.alex.guzhenren.entity.projectile;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * The curve a locked-on Moon Blade [月刃] flies, kept free of entities so plain JUnit pins it: a quadratic Bezier
 * from the {@code start} it was fired at, through a control point set off the chord's midpoint by {@code bend},
 * to the target's center, which the caller reads afresh every tick so a moving target is still met.
 *
 * <p>{@link #aim} rolls the bend once per shot: left or right of the chord with equal odds, by
 * {@link #MIN_BEND_RATIO} to {@link #MAX_BEND_RATIO} of the chord's length, plus {@link #LIFT_RATIO} of that as a
 * lift. "Left" is the shooter's left looking along the chord. The flight takes {@link #flightTicks}, the chord at the
 * speed given, never under {@link #MIN_FLIGHT_TICKS}; the blade's position at tick {@code n} is {@link #pointAt} with
 * {@code t = n / flightTicks}.
 *
 * <p>{@link #bankDegrees} is the roll the renderer gives the blade for the turn: toward the side it bends to,
 * {@link #MAX_BANK_DEGREES} at the largest bend and in proportion below it, so every shot leans a little differently.
 * {@link #isAimable} is the server's geometric check on a client-picked target: within range and ahead of the eye.
 *
 * @param start where the blade was fired from
 * @param bend the control point's offset from the chord's midpoint
 * @param flightTicks ticks from start to target
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

    /** The control point's offset: {@code ratio} of the chord sideways, plus {@link #LIFT_RATIO} of that upward. */
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

    /** Positive rolls the blade's left edge down, for a bend to the left. */
    public float bankDegrees(@NotNull Vec3 end) {
        Vec3 chord = end.subtract(start);
        double across = Math.sqrt(chord.x * chord.x + chord.z * chord.z);
        if (across < FLAT) return 0.0F;

        double toLeft = bend.dot(leftOf(chord));
        double ratio = Math.abs(toLeft) / across;
        float bank = MAX_BANK_DEGREES * (float) Math.min(1.0D, ratio / MAX_BEND_RATIO);
        return toLeft < 0.0D ? -bank : bank;
    }

    /** The unit vector to the left of a flight along {@code chord}, level with the ground; east for a flight south. */
    static @NotNull Vec3 leftOf(@NotNull Vec3 chord) {
        Vec3 left = new Vec3(chord.z, 0.0D, -chord.x);
        return left.lengthSqr() < FLAT ? new Vec3(1.0D, 0.0D, 0.0D) : left.normalize();
    }
}
