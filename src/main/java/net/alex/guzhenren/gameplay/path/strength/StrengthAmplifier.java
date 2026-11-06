package net.alex.guzhenren.gameplay.path.strength;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One temporary strength amplifier [增幅]: the strength points [力量点] a running state adds to the body, kept apart
 * from the permanent record by its {@code source}, and the carrying capacity [承受力] it brings along. A timed beast
 * buff (the Flower Boar Gu's boar, the Brute Force Longhorn Beetle Gu's ox) is one with its {@code family} set and a
 * capacity bonus equal to its points, so it is always brought to bear; the Hardship Strength Gu's [苦力蛊] amplifier
 * has no family, since it is not a beast's strength.
 *
 * <p>Amplifiers never enter a full release [全面解放] base and never count toward admitting a permanent beast
 * strength; {@code StrengthService} sums them beside the permanent points.
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthAmplifierProvider
 * @since 1.0.0
 */

public record StrengthAmplifier(@NotNull ResourceLocation source, @Nullable BeastStrengthFamily family, int points,
                                int capacityBonus) {

    /** A beast's strength for a while: its points, always carried. */
    public static StrengthAmplifier beast(@NotNull ResourceLocation source, @NotNull BeastStrengthFamily family,
                                          int points) {
        return new StrengthAmplifier(source, family, points, points);
    }

    public boolean isBeast() { return family != null; }
}
