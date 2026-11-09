package net.alex.guzhenren.gameplay.path.strength;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * The beast strengths a body can hold, each declaring what it is worth in strength points [力量点].
 *
 * <p>Closed vocabulary enum stored as {@code Set<BeastStrength>} on {@code PathStrengthData}. Each constant
 * carries its own family, form and point worth, so adding a beast is one constant touching neither
 * the Gu item nor the attack sum. A boar is 24 points, a bear 40, a tiger 64, the stone turtle, the galloping
 * horse and the green bull 48, the brown bear's innate strength [棕熊本力] 96; a point is worth
 * {@link PathStrengthData#ATTACK_PER_POINT} attack, the same as one jin of human strength, and goes through the same
 * carrying capacity. No sibling mod may add a species.
 *
 * <p>A family holds one strength: a new one {@link #replaces} whatever its family holds, so the bear's one strength
 * [一熊之力] becomes the brown bear's innate strength and never adds to it, and {@link #isCoveredBy} keeps a Gu from
 * replacing an equal or stronger one. The {@link Form#PAIRED} white and black boars are the one exception, now and
 * later: they sit side by side as the two boars' strength [两猪之力]. {@link Form#INNATE} reads by its own name, not
 * as a count of its family.
 *
 * <p>A species may also carry a body trait for as long as it is held: {@link #getCapacityBonus} adds to the normal
 * carrying capacity [常态承受力] (the stone turtle's 128, the green bull's 64), {@link #getSprintBonus} to the
 * sprint's multiple of the walking speed (the galloping horse's 0.5, so 1.3 becomes 1.8),
 * {@link #getDurationBonusTicks} to the length of the strength path's timed states as they start -- the All-Out Effort
 * [全力以赴], the Hardship [苦力] and Self Reliance [自力更生] windows and the two timed beast strengths, never the
 * Strength Qi Gu [力气蛊] -- (the green bull's ten seconds), and {@link #getChanceBonus} to the opening punch's chance
 * of a full release outside the All-Out Effort (the brown bear's 0.05, so 5% becomes 10%).
 *
 * <p>⚠ A species tag is per SPECIES, not per constant: {@code WHITE_BOAR} and {@code BLACK_BOAR} share
 * {@code BOAR}. Do not move the family or the points into the reader.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public enum BeastStrength implements NamedEnum {

    WHITE_BOAR(BeastStrengthFamily.BOAR, Form.PAIRED, 24),
    BLACK_BOAR(BeastStrengthFamily.BOAR, Form.PAIRED, 24),
    BEAR(BeastStrengthFamily.BEAR, Form.COUNTED, 40),
    TIGER(BeastStrengthFamily.TIGER, Form.COUNTED, 64),
    STONE_TURTLE(BeastStrengthFamily.TURTLE, Form.COUNTED, 48, 128, 0.0D, 0, 0.0F),
    GALLOPING_HORSE(BeastStrengthFamily.HORSE, Form.COUNTED, 48, 0, 0.5D, 0, 0.0F),
    GREEN_BULL(BeastStrengthFamily.OX, Form.COUNTED, 48, 64, 0.0D, 10 * Ticks.SECOND, 0.0F),
    BROWN_BEAR(BeastStrengthFamily.BEAR, Form.INNATE, 96, 0, 0.0D, 0, 0.05F);

    public enum Form { COUNTED, PAIRED, INNATE }

    public static final Codec<BeastStrength> CODEC = StringRepresentable.fromEnum(BeastStrength::values);
    private final BeastStrengthFamily family;
    private final Form form;
    private final int points;
    private final int capacityBonus;
    private final double sprintBonus;
    private final int durationBonusTicks;
    private final float chanceBonus;

    BeastStrength(BeastStrengthFamily family, Form form, int points) {
        this(family, form, points, 0, 0.0D, 0, 0.0F);
    }

    BeastStrength(BeastStrengthFamily family, Form form, int points, int capacityBonus, double sprintBonus,
                  int durationBonusTicks, float chanceBonus) {
        this.family = family;
        this.form = form;
        this.points = points;
        this.capacityBonus = capacityBonus;
        this.sprintBonus = sprintBonus;
        this.durationBonusTicks = durationBonusTicks;
        this.chanceBonus = chanceBonus;
    }

    public BeastStrengthFamily getFamily() { return family; }

    public Form getForm() { return form; }

    public boolean replaces(BeastStrength held) {
        return held != this && held.family == family && !(held.form == Form.PAIRED && form == Form.PAIRED);
    }

    public boolean isCoveredBy(BeastStrength held) {
        return held == this || (replaces(held) && held.points >= points);
    }

    public int getPoints() { return points; }

    public int getCapacityBonus() { return capacityBonus; }

    public double getSprintBonus() { return sprintBonus; }

    public int getDurationBonusTicks() { return durationBonusTicks; }

    public float getChanceBonus() { return chanceBonus; }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.strength.beast_strength."; }
}
