package net.alex.guzhenren.gameplay.mind;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 念池组成的标签：某部分念属于哪一类。
 *
 * <ul>
 *   <li>闭合枚举，{@code MindData} 标签表的键。</li>
 *   <li>⚠ {@code NATURAL} 只推算不存储（池当前量减带标签部分），无写入方；表里只有 {@code EVIL} 及将来的标签。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public enum ThoughtTag implements NamedEnum {

    NATURAL,
    EVIL;

    public static final Codec<ThoughtTag> CODEC = StringRepresentable.fromEnum(ThoughtTag::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.wisdom.tag."; }
}
