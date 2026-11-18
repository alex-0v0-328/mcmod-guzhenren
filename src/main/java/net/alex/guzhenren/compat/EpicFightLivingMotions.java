package net.alex.guzhenren.compat;

import yesman.epicfight.api.animation.LivingMotion;

/**
 * 本模组扩展的 Epic Fight 玩家常态动作枚举（实现 {@link LivingMotion}）。
 * <ul>
 *   <li>基础层动作：{@link #SECOND_STAGE_FLIGHT_LOOP}（二段飞行[Second-Stage Flight]）、{@link #PLUNGE_FALL}（坠击下
 *       落）与 {@link #MEDITATE_LOOP}（温养[Nourish]空窍、开窍[Awakening]仪式定身及灌注类蛊[Channeled Gu]注入真元的盘腿
 *       打坐），由 {@link EpicFightClientIntegration} 在基础层替换原版待机或移动动作。</li>
 *   <li>合成层叠加动作：{@link #REFINE_LOOP}（炼化野生蛊时的双手捧持姿势）与 {@link #CHARGE_HOLD_MOVE}
 *       （惯力蛊[Habitual Strength Gu]可移动蓄力时的上半身架势），通过合成层钩子覆盖胸、头、臂关节，同时保留腿部基础移
 *       动动画。</li>
 *   <li>⚠ Epic Fight 通过 {@code LivingMotion.ENUM_MANAGER} 按注册顺序分配全局序号；
 *       {@link EpicFightIntegration#initialize} 在模组构造期统一注册本枚举以保证客户端与服务端序号一致，新增枚举常量只
 *       能追加在末尾。</li>
 * </ul>
 */

public enum EpicFightLivingMotions implements LivingMotion {
    SECOND_STAGE_FLIGHT_LOOP,
    PLUNGE_FALL,
    MEDITATE_LOOP,
    REFINE_LOOP,
    CHARGE_HOLD_MOVE;

    private final int id;

    EpicFightLivingMotions() { this.id = LivingMotion.ENUM_MANAGER.assign(this); }

    @Override
    public int universalOrdinal() { return id; }
}
