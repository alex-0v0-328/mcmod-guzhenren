package net.alex.guzhenren.gameplay.path;

import net.alex.guzhenren.core.NamedEnum;

/**
 * 流派效果的性质分类枚举：区分有益增益（{@link #BENEFICIAL}）与有害减益（{@link #HARMFUL}），由各
 * {@link PathEffectType} 指定，供 B 面板流派页分组展示与后续规则检索（不影响道痕[Path Marks]记账）。
 */

public enum PathEffectCategory implements NamedEnum {

    BENEFICIAL,
    HARMFUL;


    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.path.effect_category."; }
}
