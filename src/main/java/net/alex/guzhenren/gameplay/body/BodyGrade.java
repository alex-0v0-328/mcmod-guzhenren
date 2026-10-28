package net.alex.guzhenren.gameplay.body;

import net.alex.guzhenren.core.NamedEnum;

/**
 * One grade of a body part [筋骨皮肉]: the tendons [筋], the bones [骨], the skin [皮] or the flesh [肉], native
 * [原生] or what a Strength Path defensive Gu made of it. Each part is a closed vocabulary enum implementing this
 * with itself as {@code G}, and one slot on {@code BodyData}. A grade carries the rank [转] of the Gu that grants
 * it, zero for native; within one family the ranks run without a gap, which {@code /gzr body} relies on to
 * name a grade by family and rank.
 *
 * <p>{@link #canReplace} is the one gate every tempering Gu asks: whether the holder's current grade gives
 * way to this one. Each part answers for itself; the item never compares grades on its own.
 *
 * @author Alex
 * @version 1.0.0
 * @see Skin
 * @see Tendon
 * @see Bone
 * @see Flesh
 * @since 1.0.0
 */

public interface BodyGrade<G extends BodyGrade<G>> extends NamedEnum {

    boolean isNative();

    int getRank();

    /** Whether a Gu that grants this grade may be used by a holder whose part is {@code worn}. */
    boolean canReplace(G worn);
}
