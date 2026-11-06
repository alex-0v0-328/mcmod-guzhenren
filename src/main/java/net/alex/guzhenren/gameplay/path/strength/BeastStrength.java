package net.alex.guzhenren.gameplay.path.strength;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * The beast strengths a body can hold, each declaring what it is worth in strength points [力量点].
 *
 * <p>Closed vocabulary enum stored as {@code Set<BeastStrength>} on {@code PathStrengthData}. Each constant
 * carries its own family and point worth, so adding a beast is one constant touching neither
 * the Gu item nor the attack sum. A boar is 24 points, a bear 40, a tiger 64; a point is worth
 * {@link PathStrengthData#ATTACK_PER_POINT} attack, the same as one jin of human strength, and goes through the
 * same carrying capacity. No sibling mod may add a species.
 *
 * <p>⚠ A species tag is per SPECIES, not per constant: {@code WHITE_BOAR} and {@code BLACK_BOAR} share
 * {@code BOAR}. Do not move the family or the points into the reader.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public enum BeastStrength implements NamedEnum {

    WHITE_BOAR(BeastStrengthFamily.BOAR, 1, 24),
    BLACK_BOAR(BeastStrengthFamily.BOAR, 1, 24),
    BEAR(BeastStrengthFamily.BEAR, 1, 40),
    TIGER(BeastStrengthFamily.TIGER, 1, 64);

    public static final Codec<BeastStrength> CODEC = StringRepresentable.fromEnum(BeastStrength::values);
    private final BeastStrengthFamily family;
    private final int reading;
    private final int points;

    BeastStrength(BeastStrengthFamily family, int reading, int points) {
        this.family = family;
        this.reading = reading;
        this.points = points;
    }

    public BeastStrengthFamily getFamily() { return family; }

    public int getReading() { return reading; }

    public int getPoints() { return points; }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.strength.beast_strength."; }
}
