package net.alex.guzhenren.gameplay.aperture;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

/**
 * Rank [转数], the coarse measure of a cultivator, and the tables each grade carries.
 *
 * <p>Closed vocabulary enum: {@code rankBase} and {@code maxHealth} both live here as the single source.
 * {@code NONE} is outside the settable range; {@code shift(int)} clamps at {@code ONE} to {@code FIVE}. No sibling
 * mod may add a rank.
 *
 * <p>⚠ {@code maxHealth} is the player's maximum health at that rank. A tended Gu also answers
 * {@code getMaxHealth()}, from {@code TendedGuItem.GU_MAX_HEALTH}; Gu health never moves onto this table.
 *
 * <p>⚠ {@code NONE} translates to the empty string on purpose -- the mortal's word belongs to
 * {@link Title} alone, and a word with two owners drifts apart. {@code SIX} to {@code NINE} carry deliberate
 * zeroes (phase 3); do not "fix" them.
 *
 * @author Alex
 * @version 1.0.0
 * @see Title
 * @since 1.0.0
 */

public enum Rank implements NamedEnum {

    NONE(0L, 20),
    ONE(1L, 20),
    TWO(10L, 40),
    THREE(100L, 60),
    FOUR(1_000L, 80),
    FIVE(10_000L, 100),

    SIX(0L, 0),
    SEVEN(0L, 0),
    EIGHT(0L, 0),
    NINE(0L, 0);

    public static final Codec<Rank> CODEC = StringRepresentable.fromEnum(Rank::values);
    public static final Rank LOWEST = ONE;
    public static final Rank HIGHEST = FIVE;
    private final long rankBase;
    private final int maxHealth;

    Rank(long rankBase, int maxHealth) {
        this.rankBase = rankBase;
        this.maxHealth = maxHealth;
    }

    public long getRankBase() { return rankBase; }

    public int getMaxHealth() { return maxHealth; }

    public Rank shift(int delta) {
        return values()[Mth.clamp(ordinal() + delta, LOWEST.ordinal(), HIGHEST.ordinal())];
    }

    public static Rank[] getSettable() { return Arrays.copyOfRange(values(), LOWEST.ordinal(), HIGHEST.ordinal() + 1); }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.aperture.rank."; }
}
