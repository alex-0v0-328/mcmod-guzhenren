package net.alex.guzhenren.gameplay.aperture;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * Title [称号], derived from the rank [转数] and never stored.
 *
 * <p>Closed vocabulary enum: the only place the mortal-versus-immortal distinction still lives today.
 * {@code fromRank} maps {@code NONE} -> {@code MORTAL}, {@code ONE..FIVE} -> {@code GU_MASTER},
 * {@code SIX..NINE} -> {@code GU_IMMORTAL}. No sibling mod may add a title.
 *
 * <p>⚠ Storing it would create a second answer that the rank could then contradict. The mortal's word
 * belongs to this enum alone, which is why {@link Rank#NONE} translates to the empty string.
 *
 * @author Alex
 * @version 1.0.0
 * @see Rank
 * @since 1.0.0
 */

public enum Title implements NamedEnum {

    MORTAL,
    GU_MASTER,
    GU_IMMORTAL;

    public static final Codec<Title> CODEC = StringRepresentable.fromEnum(Title::values);

    public static @NotNull Title fromRank(Rank rank) {
        if (rank == Rank.NONE) return MORTAL;
        return rank.ordinal() > Rank.HIGHEST.ordinal() ? GU_IMMORTAL : GU_MASTER;
    }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.aperture.title."; }
}
