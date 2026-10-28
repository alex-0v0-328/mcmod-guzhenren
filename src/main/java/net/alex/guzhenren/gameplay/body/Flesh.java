package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * The body's flesh [肉]: native [原生] only, until a Gu tempers it. Closed vocabulary enum, one slot on
 * {@code BodyData}; it adds nothing yet. One family, ordered by declaration: a grade replaces every grade before
 * it and nothing after ({@link #canReplace}).
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyData
 * @see BodyGrade
 * @since 1.0.0
 */

public enum Flesh implements BodyGrade<Flesh> {

    NATIVE;

    public static final Codec<Flesh> CODEC = StringRepresentable.fromEnum(Flesh::values);

    @Override
    public boolean isNative() { return this == NATIVE; }

    @Override
    public int getRank() { return 0; }

    @Override
    public boolean canReplace(@NotNull Flesh worn) { return worn.ordinal() < ordinal(); }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.flesh."; }
}
