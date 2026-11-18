package net.alex.guzhenren.gameplay.aperture;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

/**
 * 修为转数枚举：定义转数基数（{@link #getRankBase}）与玩家各转最大生命值（{@link #getMaxHealth}）。
 * <ul>
 *   <li>封闭词汇枚举，兄弟模组不得新增；{@link #NONE} 不可由指令或业务直接设置，{@link #shift} 夹取在 {@link #ONE} 至
 *       {@link #FIVE} 之间。</li>
 *   <li>⚠ {@link #getMaxHealth} 仅代表玩家在该转的最大生命值；持续照顾型蛊[Tended Gu]的生命上限取自
 *       {@code GuProperties}（{@code 4 * rank + 4}），切勿混入此表。</li>
 *   <li>⚠ {@link #NONE} 的语言键刻意映射为空字符串，因为「凡人」称号唯一归属 {@link Title}；{@link #SIX} 至
 *       {@link #NINE} 属于远期仙蛊阶段占位，数值刻意留 {@code 0}，切勿自行补值。</li>
 * </ul>
 */

public enum Rank implements NamedEnum {

    NONE(0L, 20),
    ONE(1L, 20),
    TWO(10L, 40),
    THREE(100L, 60),
    FOUR(1_000L, 80),
    FIVE(10_000L, 100),

    SIX(0L, 0),
    SEVEN(0L, 0),
    EIGHT(0L, 0),
    NINE(0L, 0);

    public static final Codec<Rank> CODEC = StringRepresentable.fromEnum(Rank::values);
    public static final Rank LOWEST = ONE;
    public static final Rank HIGHEST = FIVE;
    private final long rankBase;
    private final int maxHealth;

    Rank(long rankBase, int maxHealth) {
        this.rankBase = rankBase;
        this.maxHealth = maxHealth;
    }

    public long getRankBase() { return rankBase; }

    public int getMaxHealth() { return maxHealth; }

    public Rank shift(int delta) {
        return values()[Mth.clamp(ordinal() + delta, LOWEST.ordinal(), HIGHEST.ordinal())];
    }

    public static Rank[] getSettable() { return Arrays.copyOfRange(values(), LOWEST.ordinal(), HIGHEST.ordinal() + 1); }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.aperture.rank."; }
}
