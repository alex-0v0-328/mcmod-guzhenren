package net.alex.guzhenren.gameplay.body;

import net.alex.guzhenren.core.NamedEnum;

/**
 * One grade of a tempered body part [炼体]: the skin [皮], the tendons [筋] or the bones [骨], native [原生] or
 * what a Strength Path defensive Gu made of it. Each part is a closed vocabulary enum implementing this with
 * itself as {@code G}, and one slot on {@code BodyData}.
 *
 * <p>{@link #canReplace} is the one gate every tempering Gu asks: whether the holder's current grade gives
 * way to this one. Each part answers for itself; the item never compares grades on its own.
 *
 * @author Alex
 * @version 1.0.0
 * @see Skin
 * @see Tendon
 * @see Bone
 * @since 1.0.0
 */

public interface BodyGrade<G extends BodyGrade<G>> extends NamedEnum {

    boolean isNative();

    /** Whether a Gu that grants this grade may be used by a holder whose part is {@code worn}. */
    boolean canReplace(G worn);
}
