package net.alex.guzhenren.gameplay.aperture;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.core.WeightedPick;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 空窍资质[Aptitude]枚举：由空窍基础真元百分比（{@link Aperture#baseEssence}）实时推导，不单独落盘。
 * <ul>
 *   <li>封闭词汇枚举，兄弟模组不得新增；统一定义各档百分比区间、抽取权重、真元自然回复倍率与 Epic Fight 耐力加成百分
 *       比；{@link #NONE} 不可直接设置，{@link #shift} 夹取在 {@link #TEN_EXTREMES} 至 {@link #FOURTH} 之间。</li>
 *   <li>{@link #formatPercent} 将基础真元数值格式化为中文「几成几」（如 85 为「八成五」），供 HUD 与开窍[Awakening]播报
 *       共用。</li>
 *   <li>⚠ 枚举常量按从高到低声明，因此提升资质 {@code shift(+1)} 对应 {@code ordinal - 1}，切勿按声明顺序反向推算；
 *       百分比 {@code 1..19} 为未定义空洞，{@link #fromPercent} 对其返回 {@link #NONE}。</li>
 * </ul>
 */

public enum Talent implements NamedEnum {

    TEN_EXTREMES(100, 100, 10, 20, 50),
    FIRST(80, 99, 20, 8, 20),
    SECOND(60, 79, 30, 4, 20),
    THIRD(40, 59, 30, 2, 10),
    FOURTH(20, 39, 10, 1, 10),

    NONE(0, 0, 0, 0, 0);

    public static final Talent HIGHEST = TEN_EXTREMES;
    public static final Talent LOWEST = FOURTH;
    private final int minPercent;
    private final int maxPercent;
    private final int weight;
    private final int regenRate;
    private final int staminaMaxPercent;

    Talent(int minPercent, int maxPercent, int weight, int regenRate, int staminaMaxPercent) {
        this.minPercent = minPercent;
        this.maxPercent = maxPercent;
        this.weight = weight;
        this.regenRate = regenRate;
        this.staminaMaxPercent = staminaMaxPercent;
    }

    public int getMinPercent() { return minPercent; }

    public int getMaxPercent() { return maxPercent; }

    public int getWeight() { return weight; }

    public int getRegenRate() { return regenRate; }

    public int getStaminaMaxPercent() { return staminaMaxPercent; }

    public Talent shift(int delta) {
        return values()[Mth.clamp(ordinal() - delta, HIGHEST.ordinal(), LOWEST.ordinal())];
    }

    public static Talent[] getSettable() {
        return Arrays.copyOfRange(values(), HIGHEST.ordinal(), LOWEST.ordinal() + 1);
    }

    public static Talent randomTalent() { return WeightedPick.pick(values(), talent -> talent.weight); }

    public static int randomPercent(Talent talent) {
        if (talent.minPercent == talent.maxPercent) return talent.minPercent;
        return ThreadLocalRandom.current().nextInt(talent.minPercent, talent.maxPercent + 1);
    }

    public static Component formatPercent(int percent) {
        if (percent >= 100) return Component.translatable("guzhenren.display.base_full");
        Component tens = Component.translatable("guzhenren.display.base_tens." + (percent / 10));
        int units = percent % 10;
        return units == 0
                ? Component.translatable("guzhenren.display.base_round", tens)
                : Component.translatable("guzhenren.display.base_fraction", tens,
                Component.translatable("guzhenren.display.base_units." + units));
    }

    public static Talent fromPercent(int percent) {
        for (Talent t : values()) {
            if (percent >= t.minPercent && percent <= t.maxPercent) return t;
        }
        return NONE;
    }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.aperture.talent."; }
}
