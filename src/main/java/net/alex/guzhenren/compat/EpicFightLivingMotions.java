package net.alex.guzhenren.compat;

import yesman.epicfight.api.animation.LivingMotion;

/**
 * This mod's own Epic Fight living motions, beside Epic Fight's {@code LivingMotions}.
 *
 * <p>{@link #SECOND_STAGE_FLIGHT_LOOP} is the base motion of a second-stage flier [二段飞行]:
 * {@link EpicFightClientIntegration} sets it and maps it to this mod's flight loop. Epic Fight numbers living
 * motions across every mod through {@code LivingMotion.ENUM_MANAGER}; {@link EpicFightIntegration#initialize}
 * registers this class there during mod construction, so both ends load it in the same order and the ids agree.
 * {@link #PLUNGE_FALL} is the base motion of a plunging player [坠击], set and mapped to the fall loop the same way.
 * {@link #MEDITATE_LOOP} is the base motion of a player sitting cross-legged [打坐], nourishing an aperture or held
 * still by the awakening ritual or pouring essence into a channeled Gu [灌注], set and mapped to the seated loop
 * the same way. {@link #REFINE_LOOP} is the composite motion of a player refining a wild Gu [炼化], set through the
 * composite-layer hook and mapped to the refining hold, which lies over the base motion on the chest, the head and
 * the arms. New motions go at the end: the ids follow the declaration order.
 *
 * @author Alex
 * @version 1.0.0
 * @see EpicFightClientIntegration
 * @since 1.0.0
 */

public enum EpicFightLivingMotions implements LivingMotion {
    SECOND_STAGE_FLIGHT_LOOP,
    PLUNGE_FALL,
    MEDITATE_LOOP,
    REFINE_LOOP;

    private final int id;

    EpicFightLivingMotions() { this.id = LivingMotion.ENUM_MANAGER.assign(this); }

    @Override
    public int universalOrdinal() { return id; }
}
