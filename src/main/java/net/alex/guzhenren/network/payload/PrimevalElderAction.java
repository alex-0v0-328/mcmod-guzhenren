package net.alex.guzhenren.network.payload;

/**
 * 元老蛊[Primeval Elder Gu]交互意图的动作枚举，区分点按存入元石[Primeval Stone]（{@link #DEPOSIT}）与长按滑条按组取出
 * （{@link #WITHDRAW}），与 {@link PrimevalElderPayload} 同包以避免载荷包反向依赖上层 {@code network} 包。
 */

public enum PrimevalElderAction { DEPOSIT, WITHDRAW }
