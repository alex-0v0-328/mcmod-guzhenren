package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * The body's tendons [筋]: native [原生], or the steel tendons [钢筋] a Steel Tendon Gu leaves for good, rank three
 * or four. Closed vocabulary enum, one slot on {@code BodyData}; permanent, so a respawn keeps it.
 *
 * <p>Steel tendons raise the strength capacity [承受上限] the body can bring to bear, which
 * {@code BodyStrengthService.getCapacity} adds to the physique's base; the number is this enum's. One family,
 * ordered by declaration: a grade replaces every grade before it and nothing after ({@link #canReplace}).
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyData
 * @see BodyGrade
 * @since 1.0.0
 */

public enum Tendon implements BodyGrade<Tendon> {

    NATIVE(0, 0),
    STEEL_TENDON_3(3, 100),
    STEEL_TENDON_4(4, 200);

    public static final Codec<Tendon> CODEC = StringRepresentable.fromEnum(Tendon::values);
    private final int rank;
    private final int capacityBonus;

    Tendon(int rank, int capacityBonus) {
        this.rank = rank;
        this.capacityBonus = capacityBonus;
    }

    @Override
    public int getRank() { return rank; }

    /** The jin [斤] added to the body's strength capacity. */
    public int getCapacityBonus() { return capacityBonus; }

    @Override
    public boolean isNative() { return this == NATIVE; }

    @Override
    public boolean canReplace(@NotNull Tendon worn) { return worn.ordinal() < ordinal(); }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.tendon."; }
}
