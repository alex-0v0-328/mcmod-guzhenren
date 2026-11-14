package net.alex.guzhenren.gameplay.aperture;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * 称号：由转数推导，从不存储。
 *
 * <ul>
 *   <li>封闭词汇枚举，兄弟模组不得新增；凡人与蛊仙之分如今只在此处。</li>
 *   <li>{@code fromRank}：{@code NONE} 凡人，{@code ONE} 至 {@code FIVE} 蛊师，{@code SIX} 至 {@code NINE} 蛊仙。</li>
 *   <li>⚠ 存储会多出一个可与转数矛盾的答案；「凡人」只归本枚举，所以 {@link Rank#NONE} 译成空串。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see Rank
 * @since 1.0.0
 */

public enum Title implements NamedEnum {

    MORTAL,
    GU_MASTER,
    GU_IMMORTAL;

    public static final Codec<Title> CODEC = StringRepresentable.fromEnum(Title::values);

    public static @NotNull Title fromRank(Rank rank) {
        if (rank == Rank.NONE) return MORTAL;
        return rank.ordinal() > Rank.HIGHEST.ordinal() ? GU_IMMORTAL : GU_MASTER;
    }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.aperture.title."; }
}
