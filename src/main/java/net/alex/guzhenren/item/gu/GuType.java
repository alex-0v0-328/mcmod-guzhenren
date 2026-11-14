package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.core.NamedEnum;
import org.jetbrains.annotations.Nullable;

/**
 * 蛊虫类型：蛊的用途轴，与转数、道[Path]、生命周期类并列。封闭词表；分支 {@link #RETALIATION}、{@link #STEED}、
 * {@link #FLYING}、{@link #IDENTITY} 指明父类型，顶层无父。每只蛊至多一个（经 {@code GuProperties.type}），可无：寿蛊、
 * 尸蛊无用途类型，不显示该行。生命周期（即时一次性、持续照顾型[Tended Gu]、照顾后消耗型）留在物品类，不进本枚举。
 *
 * @author Alex
 * @version 1.0.0
 * @see GuProperties
 * @since 1.0.0
 */

public enum GuType implements NamedEnum {

    DEFENSIVE,
    RETALIATION(DEFENSIVE),
    OFFENSIVE,
    MOVEMENT,
    STEED(MOVEMENT),
    FLYING(MOVEMENT),
    INVESTIGATION,
    IDENTITY(INVESTIGATION),
    CONCEALMENT,
    HEALING,
    STORAGE,
    CULTIVATION,
    AMPLIFICATION,
    PATH_MARK_CARVING;

    private final @Nullable GuType parent;

    GuType() { this(null); }

    GuType(@Nullable GuType parent) { this.parent = parent; }

    public @Nullable GuType getParent() { return parent; }

    public boolean isBranch() { return parent != null; }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.gu.gu_type."; }
}
