package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * The body's bones [骨]: native [原生], iron bones [铁骨] or essence iron bones [精铁骨], left for good by the
 * Iron Bone Gu and the Essence Iron Bone Gu. Closed vocabulary enum, one slot on {@code BodyData}; permanent,
 * so a respawn keeps it.
 *
 * <p>Iron bones add max health and knockback resistance, read by {@code MaxHealthService} and
 * {@code KnockbackResistanceService}; the numbers are this enum's. One family, ordered by declaration: a
 * grade replaces every grade before it and nothing after ({@link #canReplace}).
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyData
 * @see BodyGrade
 * @since 1.0.0
 */

public enum Bone implements BodyGrade<Bone> {

    NATIVE(0.0D, 0.0D),
    IRON_BONE(4.0D, 0.3D),
    ESSENCE_IRON_BONE(10.0D, 0.6D);

    public static final Codec<Bone> CODEC = StringRepresentable.fromEnum(Bone::values);
    private final double maxHealthBonus;
    private final double knockbackResistance;

    Bone(double maxHealthBonus, double knockbackResistance) {
        this.maxHealthBonus = maxHealthBonus;
        this.knockbackResistance = knockbackResistance;
    }

    /** The health points added to the max health attribute. */
    public double getMaxHealthBonus() { return maxHealthBonus; }

    /** The knockback resistance added, on vanilla's 0 to 1 scale. */
    public double getKnockbackResistance() { return knockbackResistance; }

    @Override
    public boolean isNative() { return this == NATIVE; }

    @Override
    public boolean canReplace(@NotNull Bone worn) { return worn.ordinal() < ordinal(); }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.bone."; }
}
