package net.alex.guzhenren.core;

/**
 * 九个共用拒绝提示键：境界不符、无法使用、效果未结束、自动生效、蛊虫饥饿、作用目标错误、背包已满、真元[Essence] 不足、
 * 尚未开窍[Awakening]；处处同名共用（Alex 定），经 {@link ActionBar#refuse} 显示。
 * <ul>
 *   <li>放在 core：蛊物品与玩法层（空窍温养、魂魄交易）都用，玩法层不得引 {@code item}；蛊的拒绝记录
 *       {@code item.GuItem.Refusal} 转引同名常量，物品层照旧写 {@code Refusal.ESSENCE}。</li>
 *   <li>⚠ 字符串常量在编译期内联，字节码不留类引用，ArchUnit 看不出跨层引用；分层由读源码 import 的
 *       {@code SourceLayersTest} 补查。</li>
 * </ul>
 */

public final class Refusals {

    public static final String RANK_MISMATCH = "guzhenren.item.failed.rank_mismatch";
    public static final String CANNOT_USE = "guzhenren.item.failed.cannot_use";
    public static final String EFFECT_ACTIVE = "guzhenren.item.failed.effect_active";
    public static final String AUTO_EFFECT = "guzhenren.item.failed.auto_effect";
    public static final String GU_HUNGRY = "guzhenren.item.failed.gu_hungry";
    public static final String WRONG_TARGET = "guzhenren.item.failed.wrong_target";
    public static final String BAG_FULL = "guzhenren.item.failed.bag_full";
    public static final String ESSENCE = "guzhenren.item.failed.essence";
    public static final String UNAWAKENED = "guzhenren.item.failed.unawakened";

    private Refusals() {}
}
