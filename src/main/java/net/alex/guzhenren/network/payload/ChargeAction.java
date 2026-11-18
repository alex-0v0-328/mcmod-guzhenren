package net.alex.guzhenren.network.payload;

/**
 * 蓄力蛊[Store Strength Gu]蓄力意图的动作枚举，区分开始（{@link #START}）、取消（{@link #CANCEL}）与对锁定目标释放
 * （{@link #RELEASE}），与 {@link StoreStrengthPayload} 同包以避免载荷包反向依赖上层 {@code network} 包。
 */

public enum ChargeAction { START, CANCEL, RELEASE }
