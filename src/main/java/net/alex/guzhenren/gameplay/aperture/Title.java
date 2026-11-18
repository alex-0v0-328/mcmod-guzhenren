package net.alex.guzhenren.gameplay.aperture;

import net.alex.guzhenren.core.NamedEnum;
import org.jetbrains.annotations.NotNull;

/**
 * 修士大境界称号枚举：由 {@link Rank} 实时推导（{@link #fromRank}：无转为凡人、一至五转为蛊师、六至九转为蛊仙），从不落
 * 盘存储。
 * <ul>
 *   <li>⚠ 单独存储称号会制造与转数不一致的双重数据源；「凡人」译名唯一归属 {@link #MORTAL}，因此 {@link Rank#NONE} 的
 *       语言键刻意留空以避免拼接重复。</li>
 * </ul>
 */

public enum Title implements NamedEnum {

    MORTAL,
    GU_MASTER,
    GU_IMMORTAL;


    public static @NotNull Title fromRank(Rank rank) {
        if (rank == Rank.NONE) return MORTAL;
        return rank.ordinal() > Rank.HIGHEST.ordinal() ? GU_IMMORTAL : GU_MASTER;
    }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.aperture.title."; }
}
