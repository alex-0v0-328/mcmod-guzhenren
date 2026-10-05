package net.alex.guzhenren.gameplay.path.qi;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.core.Ticks;
import net.minecraft.util.StringRepresentable;

/**
 * The kinds of Qi [气], and how long a holding lasts before it starts decaying.
 *
 * <p>Closed vocabulary enum: eight kinds, and {@code HEAVEN}/{@code EARTH} have no source in phase 2
 * (升仙's threshold, not a gap). {@code SWORD} has no effect at all. No sibling mod may add a kind.
 *
 * <p>⚠ Timed tiers drain in 10/20/40/80/160 seconds: the tier amount is {@code 10 << 2×tier} and
 * its decay rate is {@code 1 << tier}. Move one without the other and those durations stop matching.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public enum QiKind implements NamedEnum {

    SWORD(5 * Ticks.MINUTE, 0),
    STRENGTH(10 * Ticks.MINUTE, 0),
    LIFE(0, 2 * Ticks.MINUTE),
    ESSENCE(0, 2 * Ticks.MINUTE),
    DEATH(0, 0),
    HUMAN(0, 0),
    HEAVEN(0, 0),
    EARTH(0, 0);

    public static final Codec<QiKind> CODEC = StringRepresentable.fromEnum(QiKind::values);
    private static final int TIERS = 5;
    private final long holdFlatTicks;
    private final long holdPerTierTicks;

    QiKind(long holdFlatTicks, long holdPerTierTicks) {
        this.holdFlatTicks = holdFlatTicks;
        this.holdPerTierTicks = holdPerTierTicks;
    }

    public boolean isTimed() { return holdFlatTicks > 0L || holdPerTierTicks > 0L; }

    public long holdTicks(int tier) {
        return holdFlatTicks > 0L ? holdFlatTicks : (tier + 1L) * holdPerTierTicks;
    }

    public static long tierAmount(int tier) { return 10L << (2 * tier); }

    public static long decayPerSecond(int tier) { return 1L << tier; }

    public static int tierOf(long amount) {
        for (int tier = TIERS - 1; tier >= 0; tier--) {
            if (amount >= tierAmount(tier)) return tier;
        }
        return -1;
    }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.qi."; }
}
