package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 流派效果对载体有益或有害：流派增益或流派减益。闭合枚举，每个 {@link PathEffectType} 指定一种；B 面板分两类列出、
 * 各用自己的名称，以后的规则可能读它；不改变任何记账。
 *
 * @author Alex
 * @version 1.0.0
 * @see PathEffectType
 * @since 1.0.0
 */

public enum PathEffectCategory implements NamedEnum {

    BENEFICIAL,
    HARMFUL;

    public static final Codec<PathEffectCategory> CODEC = StringRepresentable.fromEnum(PathEffectCategory::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.path.effect_category."; }
}
