package net.alex.guzhenren.core;

/**
 * 全模组通用的游戏刻（tick）时间换算常量表。
 * <ul>
 *   <li>作为全模组时间常量的唯一真值来源，严禁在其他类中重复硬编码 {@code 20} 或 {@code 24000} 等字面量；不提供外部配置
 *       文件覆写，以保证寿元流逝、真元回复、蛊虫喂养与原版昼夜周期严格同步。</li>
 *   <li>⚠ 新增共享时长基准时应统一在此声明为具名常量，避免散落在业务代码中。</li>
 *   <li>{@link #ceilSeconds}：剩余刻数换成向上取整的秒数（倒计时显示用），负数按 0 算。</li>
 * </ul>
 */

public final class Ticks {

    private Ticks() {}

    public static final int SECOND = 20;
    public static final int HALF_SECOND = SECOND / 2;
    public static final int MINUTE = 60 * SECOND;
    public static final int HALF_MINUTE = MINUTE / 2;
    public static final int DAY = 24000;
    public static final int HALF_DAY = DAY / 2;

    public static int ceilSeconds(long ticks) { return (int) ((Math.max(0L, ticks) + SECOND - 1) / SECOND); }
}
