package net.alex.guzhenren.gameplay.mind;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * The kinds of thought [念] a mind holds, each with its own capacity.
 *
 * <p>Closed vocabulary enum: the three pools on {@code MindData}, and no sibling mod may add a fourth.
 * Capacities {@code 50000 / 12 / 8} live here as the single declaration; {@code BURST_NUMERATOR/DENOMINATOR}
 * is the one place the overfill ratio is written.
 *
 * <p>⚠ Only some may be overfilled past the cap, and only those can ever be lethal.
 * {@code isBurstable()} is the single declaration of which; never re-test the constant at a call site.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public enum WisdomType implements NamedEnum {

    THOUGHTS(50_000L, true),
    WILLS(12L, false),
    EMOTIONS(8L, false);

    public static final Codec<WisdomType> CODEC = StringRepresentable.fromEnum(WisdomType::values);
    public static final long BURST_NUMERATOR = 6L;
    public static final long BURST_DENOMINATOR = 5L;
    private final long defaultCapacity;
    private final boolean burstable;

    WisdomType(long defaultCapacity, boolean burstable) {
        this.defaultCapacity = defaultCapacity;
        this.burstable = burstable;
    }

    public long getDefaultCapacity() { return defaultCapacity; }

    public boolean isBurstable() { return burstable; }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.wisdom.type."; }
}
