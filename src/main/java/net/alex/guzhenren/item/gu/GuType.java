package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.core.NamedEnum;
import org.jetbrains.annotations.Nullable;

/**
 * What a Gu is used for [蛊虫类型]: the usage axis beside rank [转数], path [流派] and the lifecycle class.
 *
 * <p>Closed vocabulary enum. A branch ({@link #RETALIATION}, {@link #STEED}, {@link #FLYING},
 * {@link #IDENTITY}) names its parent; a top-level type has none. A Gu carries at most one constant, set through
 * {@code GuSpec.type}, and may carry none: a lifespan Gu or a zombie Gu belongs to no usage type and shows no
 * line. The lifecycle (one-shot, tended, consumed) stays with the item class and never becomes a constant
 * here.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuSpec
 * @since 1.0.0
 */

public enum GuType implements NamedEnum {

    DEFENSIVE,
    RETALIATION(DEFENSIVE),
    OFFENSIVE,
    MOVEMENT,
    STEED(MOVEMENT),
    FLYING(MOVEMENT),
    INVESTIGATION,
    IDENTITY(INVESTIGATION),
    CONCEALMENT,
    HEALING,
    STORAGE,
    CULTIVATION,
    AMPLIFICATION,
    DAO_MARK_CARVING;

    private final @Nullable GuType parent;

    GuType() { this(null); }

    GuType(@Nullable GuType parent) { this.parent = parent; }

    public @Nullable GuType getParent() { return parent; }

    public boolean isBranch() { return parent != null; }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.gu.gu_type."; }
}
