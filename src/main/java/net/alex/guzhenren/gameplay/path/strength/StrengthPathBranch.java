package net.alex.guzhenren.gameplay.path.strength;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 力道四分支，基础力道为默认分类（封闭枚举）。兽力虚影流、人力钧力流持有现有持久数据；气象天地流未实现；基础力道归类其
 * 余力道蛊，不加字段。道痕[Path Marks]互斥：{@link #interferesWith} 对称，持一支即拒互斥分支；现仅兽力虚影流与人力钧力
 * 流，新增一对加一个分支。
 *
 * @author Alex
 * @version 1.0.0
 * @see HumanStrength
 * @see BeastStrength
 * @since 1.0.0
 */

public enum StrengthPathBranch implements NamedEnum {

    BEAST_STRENGTH_PHANTOM,
    HUMAN_JUN_STRENGTH,
    ATMOSPHERIC_HEAVEN_AND_EARTH,
    NORMAL;

    public static final Codec<StrengthPathBranch> CODEC = StringRepresentable.fromEnum(StrengthPathBranch::values);

    public boolean interferesWith(StrengthPathBranch other) {
        return switch (this) {
            case BEAST_STRENGTH_PHANTOM -> other == HUMAN_JUN_STRENGTH;
            case HUMAN_JUN_STRENGTH -> other == BEAST_STRENGTH_PHANTOM;
            case ATMOSPHERIC_HEAVEN_AND_EARTH, NORMAL -> false;
        };
    }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.strength.strength_path_branch."; }
}
