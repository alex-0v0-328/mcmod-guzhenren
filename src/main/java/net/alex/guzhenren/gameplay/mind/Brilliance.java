package net.alex.guzhenren.gameplay.mind;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.core.WeightedPick;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

/**
 * 脑海才情[Brilliance]等级枚举：决定玩家每秒自然产出的念头数量（{@link #getThoughtsPerSecond}）。
 * <ul>
 *   <li>封闭词汇枚举；角色出生时（{@code onBirth}）按权重随机抽取一次，与空窍资质无关；{@link #shift} 夹取在
 *       {@link #LOWEST} 至 {@link #HIGHEST} 之间。</li>
 *   <li>⚠ 刻意不设 {@code NONE} 常量：凡人同样拥有脑海并自然产念，最低档 {@link #ORDINARY}（每秒 1 念）是合法的初始值而
 *       非「未设置」占位。</li>
 * </ul>
 */

public enum Brilliance implements NamedEnum {

    ORDINARY(1, 15),
    DECENT(4, 25),
    DISTINCTIVE(16, 25),
    OUTSTANDING(64, 25),
    UNRIVALED(256, 10);

    public static final Codec<Brilliance> CODEC = StringRepresentable.fromEnum(Brilliance::values);
    public static final Brilliance LOWEST = ORDINARY;
    public static final Brilliance HIGHEST = UNRIVALED;
    private final long thoughtsPerSecond;
    private final int weight;

    Brilliance(long thoughtsPerSecond, int weight) {
        this.thoughtsPerSecond = thoughtsPerSecond;
        this.weight = weight;
    }

    public long getThoughtsPerSecond() { return thoughtsPerSecond; }

    public int getWeight() { return weight; }

    public Brilliance shift(int delta) {
        return values()[Mth.clamp(ordinal() + delta, LOWEST.ordinal(), HIGHEST.ordinal())];
    }

    public static Brilliance randomBrilliance() { return WeightedPick.pick(values(), brilliance -> brilliance.weight); }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.wisdom.brilliance."; }
}
