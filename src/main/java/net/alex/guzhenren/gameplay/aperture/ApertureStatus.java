package net.alex.guzhenren.gameplay.aperture;

import net.alex.guzhenren.core.NamedEnum;

/**
 * 空窍状态枚举：只按需推导、从不落盘存储的封闭词汇表。
 * <ul>
 *   <li>{@link #NORMAL} 为正常活窍；{@link #DEAD}（死窍）涵盖僵尸、临时僵尸化与石化空窍，停止自然回复、温养[Nourish]与
 *       冲刷窍壁。</li>
 *   <li>⚠ 状态唯一由 {@link ApertureService#getStatus} 推导，各处业务门槛统一判定 {@code status == NORMAL}。</li>
 * </ul>
 */

public enum ApertureStatus implements NamedEnum {

    NORMAL, DEAD;

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.aperture.status."; }
}
