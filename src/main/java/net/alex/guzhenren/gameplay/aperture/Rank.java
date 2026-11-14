package net.alex.guzhenren.gameplay.aperture;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

/**
 * 转数：修炼者的粗粒度度量及各转数值表。
 *
 * <ul>
 *   <li>封闭词汇枚举，{@code rankBase} 与 {@code maxHealth} 的唯一来源，兄弟模组不得新增；{@code NONE} 不可
 *       设，{@code shift(int)} 夹在 {@code ONE} 至 {@code FIVE}。</li>
 *   <li>⚠ {@code maxHealth} 是玩家在该转的最大生命值；持续照顾型蛊[Tended Gu]的 {@code getMaxHealth()} 取自
 *       {@code GuProperties}（4 × 转 + 4），永不挪进此表。</li>
 *   <li>⚠ {@code NONE} 故意译成空串：「凡人」只归 {@link Title}，一词两主会走样；⚠ {@code SIX} 至 {@code NINE} 刻意为
 *       零（三期），别「修」。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see Title
 * @since 1.0.0
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
