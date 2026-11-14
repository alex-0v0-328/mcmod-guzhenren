package net.alex.guzhenren.compat;

import yesman.epicfight.api.animation.LivingMotion;

/**
 * 本模组自有的 Epic Fight 常态动作，与 {@code LivingMotions} 并列。
 * <ul>
 *   <li>{@link #SECOND_STAGE_FLIGHT_LOOP}、{@link #PLUNGE_FALL}、{@link #MEDITATE_LOOP}：
 *       二段飞行[Second-Stage Flight]、坠击、打坐（温养[Nourish]空窍[Aperture]、开窍[Awakening]仪式定身、
 *       灌注类蛊[Channeled Gu]灌注真元[Essence]）基础动作，由 {@link EpicFightClientIntegration} 设置并映射到循
 *       环。</li>
 *   <li>{@link #REFINE_LOOP}：炼化野生蛊的叠加动作，经叠加层钩子设置，映射到炼化持握，盖住基础动作的胸、头、臂。</li>
 *   <li>Epic Fight 经 {@code LivingMotion.ENUM_MANAGER} 统一编号；{@link EpicFightIntegration#initialize} 在模组构造期
 *       注册本类，两端顺序同、id 才一致；新动作只加末尾。</li>
 * </ul>
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
