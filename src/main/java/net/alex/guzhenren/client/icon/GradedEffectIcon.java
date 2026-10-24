package net.alex.guzhenren.client.icon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Draws a graded effect's icon from its amplifier, so one effect can wear a different face per grade.
 *
 * <p>An {@link EffectIconLayout} carrying a name, a directory ({@code mob_effect} or {@code
 * item}) and the rank range. The texture path is {@code directory/name_rank}, where rank is
 * {@code amplifier + amplifierOffset} clamped to the range; the offset stays 1 unless the family
 * counts its amplifier from a later rank (the malicious thought Gu, whose grades start at rank two). The texture
 * location of every rank in the range is built once, so a frame only indexes them.
 *
 * <p>⚠ Keeps a family of grades one effect -- splitting it per grade would let two grades be held at once.
 *
 * @author Alex
 * @version 1.0.0
 * @see EffectIconLayout
 * @see ItemEffectIcon
 * @since 1.0.0
 */

public final class GradedEffectIcon implements EffectIconLayout {

    private final String name;
    private final String directory;
    private final int lowestRank;
    private final int highestRank;
    private final int amplifierOffset;
    private final ResourceLocation[] locations;

    private GradedEffectIcon(String name, String directory, int lowestRank, int highestRank, int amplifierOffset) {
        this.name = name;
        this.directory = directory;
        this.lowestRank = lowestRank;
        this.highestRank = highestRank;
        this.amplifierOffset = amplifierOffset;
        this.locations = new ResourceLocation[highestRank - lowestRank + 1];
        for (int rank = lowestRank; rank <= highestRank; rank++) {
            locations[rank - lowestRank] = EffectIconLayout.getLocationOf(getTexture(rank));
        }
    }

    public static GradedEffectIcon createMobEffect(String name, int lowestRank, int highestRank) {
        return new GradedEffectIcon(name, "mob_effect", lowestRank, highestRank, 1);
    }

    public static GradedEffectIcon createItem(String name, int lowestRank, int highestRank) {
        return new GradedEffectIcon(name, "item", lowestRank, highestRank, 1);
    }

    public static GradedEffectIcon createItem(String name, int lowestRank, int highestRank, int amplifierOffset) {
        return new GradedEffectIcon(name, "item", lowestRank, highestRank, amplifierOffset);
    }

    @Override
    public String getTextureFor(MobEffectInstance instance) { return getTexture(getRankOf(instance)); }

    @Override
    public ResourceLocation getLocationFor(MobEffectInstance instance) {
        return locations[getRankOf(instance) - lowestRank];
    }

    private int getRankOf(MobEffectInstance instance) {
        return Mth.clamp(instance.getAmplifier() + amplifierOffset, lowestRank, highestRank);
    }

    private String getTexture(int rank) { return directory + "/" + name + "_" + rank; }
}
