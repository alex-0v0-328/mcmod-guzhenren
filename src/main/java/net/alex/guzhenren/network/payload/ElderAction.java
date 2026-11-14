package net.alex.guzhenren.network.payload;

/**
 * 元老蛊[Primeval Elder Gu]意图的两个动作：存入元石[Primeval Stone]，或按组取出。
 * 与所属 payload 同包，免其依赖 {@code net.alex.guzhenren.network}。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public enum ElderAction { DEPOSIT, WITHDRAW }
