package net.alex.guzhenren.gameplay.aperture;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.core.WeightedPick;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

/**
 * 资质[Aptitude]：由空窍[Aperture]基础真元[Essence]推导，从不另存。
 *
 * <ul>
 *   <li>封闭词汇枚举，兄弟模组不得新增；回复倍率、Epic Fight 耐力百分比、抽取权重都在此；{@code NONE} 不可
 *       设，{@code shift(int)} 夹在 {@code TEN_EXTREMES} 至 {@code FOURTH}。</li>
 *   <li>{@code formatPercent} 把基础值读成「成」（85 即八成五），HUD 与开窍[Awakening]公告都读它，故放其下。</li>
 *   <li>⚠ 常量由高到低，{@code shift(+1)} 即 {@code ordinal - 1}，照声明顺序读方向正是弄反的由来；⚠ {@code 1} 至
 *       {@code 19} 是空洞（{@code fromPercent} → {@code NONE}）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public enum Talent implements NamedEnum {

    TEN_EXTREMES(100, 100, 10, 20, 50),
    FIRST(80, 99, 20, 8, 20),
    SECOND(60, 79, 30, 4, 20),
    THIRD(40, 59, 30, 2, 10),
    FOURTH(20, 39, 10, 1, 10),

    NONE(0, 0, 0, 0, 0);

    public static final Codec<Talent> CODEC = StringRepresentable.fromEnum(Talent::values);
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
