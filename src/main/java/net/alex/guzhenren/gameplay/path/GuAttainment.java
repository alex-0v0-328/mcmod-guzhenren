package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

/**
 * Attainment [造诣] in a path, from nothing up to the highest grade.
 *
 * <p>Closed vocabulary enum paired with {@link GuPath}: stored once per path in {@code PathEntry}, and
 * no sibling mod may extend the grade ladder. {@code shift(int)} clamps at both ends, so callers never
 * touch {@code ordinal()}.
 *
 * <p>⚠ Each grade carries its own refinement bonus here, so adding one is a single line and no change
 * anywhere else. Do not move that table into the code that reads it.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuPath
 * @since 1.0.0
 */

public enum GuAttainment implements NamedEnum {

    NONE(0),
    ORDINARY(2),
    QUASI_MASTER(5),
    MASTER(10),
    QUASI_GRANDMASTER(20),
    GRANDMASTER(40),
    QUASI_GREAT_GRANDMASTER(50),
    GREAT_GRANDMASTER(70),
    QUASI_SUPREME_GRANDMASTER(80),
    SUPREME_GRANDMASTER(100);

    public static final Codec<GuAttainment> CODEC = StringRepresentable.fromEnum(GuAttainment::values);
    private final int refinementBonus;

    GuAttainment(int refinementBonus) { this.refinementBonus = refinementBonus; }

    public int getRefinementBonus() { return refinementBonus; }

    public GuAttainment shift(int delta) { return values()[Mth.clamp(ordinal() + delta, 0, values().length - 1)]; }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.path.attainment."; }
}
