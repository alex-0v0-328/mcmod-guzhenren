package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * The body's bones [骨]: native [原生], or the iron bones [铁骨] an Iron Bone Gu leaves for good, rank three or
 * four. Closed vocabulary enum, one slot on {@code BodyData}; permanent,
 * so a respawn keeps it.
 *
 * <p>Iron bones add max health, in health points, and knockback resistance on vanilla's 0 to 1 scale, read by
 * {@code MaxHealthService} and {@code KnockbackResistanceService}; the numbers are this enum's. One family,
 * ordered by declaration: a grade replaces every grade before it and nothing after ({@link #canReplace}).
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyData
 * @see BodyGrade
 * @since 1.0.0
 */

public enum Bone implements BodyGrade<Bone> {

    NATIVE(0, 0.0D, 0.0D),
    IRON_BONE_3(3, 4.0D, 0.3D),
    IRON_BONE_4(4, 10.0D, 0.6D);

    public static final Codec<Bone> CODEC = StringRepresentable.fromEnum(Bone::values);
    private final int rank;
    private final double maxHealthBonus;
    private final double knockbackResistance;

    Bone(int rank, double maxHealthBonus, double knockbackResistance) {
        this.rank = rank;
        this.maxHealthBonus = maxHealthBonus;
        this.knockbackResistance = knockbackResistance;
    }

    @Override
    public int getRank() { return rank; }

    public double getMaxHealthBonus() { return maxHealthBonus; }

    public double getKnockbackResistance() { return knockbackResistance; }

    @Override
    public boolean isNative() { return this == NATIVE; }

    @Override
    public boolean canReplace(@NotNull Bone worn) { return worn.ordinal() < ordinal(); }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.bone."; }
}
