package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 蛊师流派（道）枚举：不设 {@code NONE} 常量，「未选择流派」统一用 {@code null} 表示。
 * <ul>
 *   <li>封闭词汇枚举，其他模组不得扩展常量，因此按流派分键的映射一律使用 {@code EnumMap<GuPath, ...>}。</li>
 *   <li>⚠ 切勿仅凭英文单词猜测流派对应关系：宇道[Space Path]（{@link #SPACE}）与宙道[Time Path]（{@link #TIME}）同时存
 *       在，混淆两者不会触发编译报错，只会静默指向错误的流派。</li>
 *   <li>⚠ 代码命名中 {@code refine} 指炼化蛊虫（认主），{@code refinement} 指炼蛊（合成升转）；{@link #REFINEMENT} 已
 *       占用「炼道」语义，其他包避免单独以裸 {@code Refinement} 命名类。</li>
 * </ul>
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
