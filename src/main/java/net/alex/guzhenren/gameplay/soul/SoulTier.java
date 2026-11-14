package net.alex.guzhenren.gameplay.soul;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.gameplay.aperture.Title;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * 魂魄档次：由上限推导，从不存储。封闭词表：九档（{@code 1} 至 {@code 1e10}）是只读显示标签，真值在 {@code SoulData} 上
 * 限；兄弟模组不得新增。
 *
 * @author Alex
 * @version 1.0.0
 * @see Title
 * @since 1.0.0
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

    public static final Codec<SoulTier> CODEC = StringRepresentable.fromEnum(SoulTier::values);
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
