package net.alex.guzhenren.gameplay.mind;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.core.WeightedPick;
import net.minecraft.util.StringRepresentable;

/**
 * Brilliance [才情]: the grade deciding how fast thought [念] refills.
 *
 * <p>Closed vocabulary enum, rolled once at birth by {@code onBirth} and independent of aptitude.
 * Rates 1/4/16/64/256 念/s with weights 15/25/25/25/10; {@code shift(int)} clamps at both ends.
 * No sibling mod may add a grade.
 *
 * <p>⚠ There is deliberately no {@code NONE} grade, because a mortal still thinks. The lowest grade is
 * a real value rather than an absence, and code reading it as "unset" stops a mortal thinking at all.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public enum Brilliance implements NamedEnum {

    ORDINARY(1, 15),
    DECENT(4, 25),
    DISTINCTIVE(16, 25),
    OUTSTANDING(64, 25),
    UNRIVALED(256, 10);

    public static final Codec<Brilliance> CODEC = StringRepresentable.fromEnum(Brilliance::values);
    public static final Brilliance LOWEST = ORDINARY;
    public static final Brilliance HIGHEST = UNRIVALED;
    private final long thoughtsPerSecond;
    private final int weight;

    Brilliance(long thoughtsPerSecond, int weight) {
        this.thoughtsPerSecond = thoughtsPerSecond;
        this.weight = weight;
    }

    public long getThoughtsPerSecond() { return thoughtsPerSecond; }

    public int getWeight() { return weight; }

    public Brilliance shift(int delta) {
        return values()[Math.clamp(ordinal() + delta, LOWEST.ordinal(), HIGHEST.ordinal())];
    }

    public static Brilliance randomBrilliance() { return WeightedPick.pick(values(), brilliance -> brilliance.weight); }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.wisdom.brilliance."; }
}
