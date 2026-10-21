package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/**
 * Race [种族]: human, plus the variant races that each grant standing on one path.
 *
 * <p>Closed vocabulary enum on {@code BodyData}. Everyone is born {@code HUMAN}; a variant is only ever
 * made, never rolled. {@code DRAGONMEN} is the only variant a player may become; the other ten are
 * NPC-only. No sibling mod may add a race.
 *
 * <p>⚠ A race can be changed, so both halves of that standing must be exactly revocable: the marks are
 * booked under {@code RACE} rather than {@code NATURAL}, and the attainment MOVES (a shift,
 * {@link #TALENT_SHIFT}) rather than being set, so leaving the race can undo it exactly.
 * {@code Race.HUMAN} is not {@link GuPath#HUMAN} [人道].
 *
 * @author Alex
 * @version 1.0.0
 * @see GuPath
 * @since 1.0.0
 */

public enum Race implements NamedEnum {

    HUMAN(null),

    HAIRY_MEN(GuPath.REFINEMENT),
    EGGMEN(GuPath.SPACE),
    ROCKMEN(GuPath.EARTH),
    FEATHERMEN(GuPath.CLOUD),
    INKMEN(GuPath.INFORMATION),
    MINIMEN(GuPath.WOOD),
    MERMEN(GuPath.WATER),
    BEASTMEN(GuPath.TRANSFORMATION),
    DRAGONMEN(GuPath.ENSLAVEMENT),
    MUSHROOMMEN(GuPath.POISON),
    SNOWMEN(GuPath.ICE_SNOW);

    public static final long TALENT_MARKS = 10L;
    public static final int TALENT_SHIFT = 1;
    public static final Codec<Race> CODEC = StringRepresentable.fromEnum(Race::values);
    private final @Nullable GuPath talentPath;

    Race(@Nullable GuPath talentPath) { this.talentPath = talentPath; }

    public @Nullable GuPath talentPath() { return talentPath; }

    public boolean isVariant() { return talentPath != null; }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.race."; }
}
