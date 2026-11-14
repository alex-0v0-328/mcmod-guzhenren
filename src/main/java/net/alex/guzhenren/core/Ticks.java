package net.alex.guzhenren.core;

/**
 * 模组全部时间单位，以 tick 计。
 * <ul>
 *   <li>全模组时间常量的唯一来源；别处不得再声明第二个 {@code 24000}。</li>
 *   <li>模组没有配置，也永远不许有：服主把一天“配置”成 5000，只会让衰老、回复、补食与昼夜脱节。</li>
 *   <li>⚠ 将来可能要调的时长也作为具名常量放这里，不内联在最先用它的代码里。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class Ticks {

    private Ticks() {}

    public static final int SECOND = 20;
    public static final int HALF_SECOND = SECOND / 2;
    public static final int MINUTE = 60 * SECOND;
    public static final int HALF_MINUTE = MINUTE / 2;
    public static final int DAY = 24000;
    public static final int HALF_DAY = DAY / 2;
}
