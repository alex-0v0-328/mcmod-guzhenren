package net.alex.guzhenren.gameplay.mind;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 念头池的分类标签枚举：标识念池中各部分念头的性质（作为 {@link MindData#taggedThoughts} 的键）。
 * <ul>
 *   <li>⚠ {@link #NATURAL}（自然念）仅由念池当前总量减去所有带标签念头推算得出
 *       （{@link MindService#getNaturalThoughts}），从不落盘存储；映射表中仅保存 {@link #EVIL}（恶念）及未来扩展的特殊
 *       念头标签。</li>
 * </ul>
 */

public enum ThoughtTag implements NamedEnum {

    NATURAL,
    EVIL;

    public static final Codec<ThoughtTag> CODEC = StringRepresentable.fromEnum(ThoughtTag::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.wisdom.tag."; }
}
