package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * The body's skin [皮肤]: native [原生], a jade skin a Strength Path defensive Gu lends for a while, or a bronze
 * [铜皮] or iron [铁皮] skin one leaves for good.
 *
 * <p>Closed vocabulary enum, one slot on {@code BodyData}: a player wears one skin at a time. Every skin
 * carries its family [皮系], its rank [转] and its flat damage reduction [固定减免], the points taken off every
 * hit the skin can turn ({@link #reduce}); the number is this enum's, a jade skin's duration the item
 * registration's. The jade family is the timed one ({@link #isTimed}); the others are permanent and survive a
 * respawn.
 *
 * <p>{@link #canReplace} is the gate: any skin goes over a native one; a permanent skin also goes over a lower
 * rank of its own family (a rank-three bronze skin over a rank-one bronze skin), never over another family's;
 * a jade skin goes over nothing but a native skin, so neither jade Gu stacks on, replaces or extends the
 * other, and no Gu replaces a jade skin before it runs out.
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyData
 * @see BodyGrade
 * @since 1.0.0
 */

public enum Skin implements BodyGrade<Skin> {

    NATIVE(Family.NATIVE, 0, 0),
    JADE_SKIN(Family.JADE, 1, 2),
    WHITE_JADE_SKIN(Family.JADE, 2, 4),
    BRONZE_SKIN_1(Family.BRONZE, 1, 1),
    BRONZE_SKIN_2(Family.BRONZE, 2, 2),
    BRONZE_SKIN_3(Family.BRONZE, 3, 3),
    ANCIENT_BRONZE_SKIN(Family.BRONZE, 4, 5),
    IRON_SKIN_1(Family.IRON, 1, 2),
    IRON_SKIN_2(Family.IRON, 2, 3),
    IRON_SKIN_3(Family.IRON, 3, 4),
    ESSENCE_IRON_SKIN(Family.IRON, 4, 6);

    /** The skin families [皮系]: only a permanent family's own higher rank replaces a worn skin of it. */
    public enum Family { NATIVE, JADE, BRONZE, IRON }

    public static final Codec<Skin> CODEC = StringRepresentable.fromEnum(Skin::values);
    private final Family family;
    private final int rank;
    private final int damageReduction;

    Skin(Family family, int rank, int damageReduction) {
        this.family = family;
        this.rank = rank;
        this.damageReduction = damageReduction;
    }

    public @NotNull Family getFamily() { return family; }

    public int getRank() { return rank; }

    public int getDamageReduction() { return damageReduction; }

    @Override
    public boolean isNative() { return this == NATIVE; }

    /** A jade skin is worn for a while and shed by the heartbeat; every other skin stays. */
    public boolean isTimed() { return family == Family.JADE; }

    @Override
    public boolean canReplace(@NotNull Skin worn) {
        if (worn.isNative()) return true;
        return !isTimed() && worn.family == family && worn.rank < rank;
    }

    /** The damage left after this skin's flat reduction, never below zero. */
    public float reduce(float amount) { return Math.max(0.0F, amount - damageReduction); }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.skin."; }
}
