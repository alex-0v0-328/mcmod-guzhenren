package net.alex.guzhenren.entity.projectile;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * How a Beast Charge [兽力冲击] rolls its {@link MoonBladeArc}, kept free of entities so plain JUnit pins it. Where
 * the Moon Blade bends left or right only, a charge bends any way round the upper half circle about its chord: from
 * level to the shooter's left, over the top, to level to the right, never downward, since a block in the way takes a
 * charge away with no burst. The charges of one punch split that half circle into as many equal sectors, left to
 * right in the order thrown, and each rolls its angle inside its own, so no two fly one curve.
 *
 * <p>The bend is {@link #MIN_BEND_RATIO} to {@link #MAX_BEND_RATIO} of the chord's length, and the curve's peak is
 * moved along the chord by up to {@link #MAX_SHIFT_RATIO} of it either way, so one charge turns early and another
 * late. ⚠ The three ratios are provisional.
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

    /** {@code turn} runs 0 for level to the left, 0.5 for straight over the top, 1 for level to the right. */
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
