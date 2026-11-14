package net.alex.guzhenren.network.payload;

/**
 * 蓄力蛊[Store Strength Gu]蓄力意图的三个动作：开始、取消、对锁定目标释放。
 * 与所属 payload 同包，免其依赖 {@code net.alex.guzhenren.network}。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public enum ChargeAction { START, CANCEL, RELEASE }
