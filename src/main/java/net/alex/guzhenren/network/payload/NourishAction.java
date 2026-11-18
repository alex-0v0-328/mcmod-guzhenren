package net.alex.guzhenren.network.payload;

/**
 * 空窍温养[Nourish]意图的动作枚举，区分开始打坐温养（{@link #START}）与取消温养（{@link #CANCEL}），与
 * {@link NourishAperturePayload} 同包以避免载荷包反向依赖上层 {@code network} 包。
 */

public enum NourishAction { START, CANCEL }
