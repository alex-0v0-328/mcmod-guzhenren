package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.core.NamedEnum;

/**
 * 力道四大专精分支枚举（基础力道 {@link #NORMAL} 为无专精默认分类）。
 * <ul>
 *   <li>兽力虚影流（{@link #BEAST_STRENGTH_PHANTOM}）与人力钧力流（{@link #HUMAN_JUN_STRENGTH}）对应玩家身上的永久力道
 *       效果，且通过对称的 {@link #interferesWith} 互相排斥（持有其一即拒绝修习另一分支）；气象天地流
 *       （{@link #ATMOSPHERIC_HEAVEN_AND_EARTH}）暂未实现。</li>
 * </ul>
 */

public enum StrengthPathBranch implements NamedEnum {

    BEAST_STRENGTH_PHANTOM,
    HUMAN_JUN_STRENGTH,
    ATMOSPHERIC_HEAVEN_AND_EARTH,
    NORMAL;


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
