package net.alex.guzhenren.gameplay.mind;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.core.WeightedPick;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

/**
 * 才情：念的回复速度等级。
 *
 * <ul>
 *   <li>闭合枚举；出生时 {@code onBirth} 掷一次，与资质[Aptitude]无关；{@code shift(int)} 两端夹紧。</li>
 *   <li>回复 1/4/16/64/256 念/秒，权重 15/25/25/25/10。</li>
 *   <li>⚠ 故意不设 {@code NONE}：凡人也思考；最低档是真实值而非缺省，当成「未设置」凡人就不回念。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
