package net.alex.guzhenren.gameplay.soul;

import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.gameplay.aperture.Title;
import org.jetbrains.annotations.NotNull;

/**
 * 魂魄档次枚举：由 {@link SoulData#maxSoul()} 经 {@link #fromSoul} 实时推导，从不落盘存储。
 * <ul>
 *   <li>九档（{@code 1} 至 {@code 10^10}）为只读显示标签，真值始终是 {@link SoulData} 的魂魄上限；属于封闭词
 *       表，其他系统不得擅自扩展档位。</li>
 * </ul>
 */

public enum SoulTier implements NamedEnum {

    ONE(1L),
    TEN(1_000L),
    HUNDRED(10_000L),
    THOUSAND(100_000L),
    TEN_THOUSAND(1_000_000L),
    HUNDRED_THOUSAND(10_000_000L),
    MILLION(100_000_000L),
    TEN_MILLION(1_000_000_000L),
    HUNDRED_MILLION(10_000_000_000L);

    private final long minSoul;

    SoulTier(long minSoul) {
        this.minSoul = minSoul;
    }

    public static @NotNull SoulTier fromSoul(long soul) {
        SoulTier[] tiers = values();
        for (int i = tiers.length - 1; i >= 0; i--) {
            if (soul >= tiers[i].minSoul) return tiers[i];
        }
        return ONE;
    }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.soul.tier."; }
}
