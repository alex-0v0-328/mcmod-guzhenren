package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 蛊可归属的全部道[Path]；没有 {@code NONE}，「未选择」用 {@code null}。
 *
 * <ul>
 *   <li>闭合枚举，模组封闭规则的标准范例：其他模组不得新增常量，因此按道分键的存储都用普通 {@code EnumMap}。</li>
 *   <li>⚠ 别从英文名推中文名：宇道[Space Path]与宙道[Time Path]都存在，猜错不报错，只会悄悄指向另一条真实的道。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see EffectCarrier
 * @see GuAttainment
 * @since 1.0.0
 */

public enum GuPath implements NamedEnum {

    HEAVEN, RULE, SPACE, TIME, HUMAN,

    METAL, WOOD, WATER, FIRE, EARTH, ICE_SNOW, LIGHTNING, CLOUD,
    QI, SOUND, LIGHT, DARK, MOON, POISON,

    STRENGTH, DREAM, REFINEMENT, WISDOM, INFORMATION, THEFT,
    LUCK, KILLING, BLOOD, SOUL, ENSLAVEMENT,
    FOOD, FORMATION, PAINTING, TRANSFORMATION;
    public static final Codec<GuPath> CODEC = StringRepresentable.fromEnum(GuPath::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.path.path."; }
}
