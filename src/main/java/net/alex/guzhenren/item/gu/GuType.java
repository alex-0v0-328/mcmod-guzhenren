package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.core.NamedEnum;
import org.jetbrains.annotations.Nullable;

/**
 * 蛊虫用途类型枚举：描述蛊虫的功能维度，与转数、流派及生命周期类型正交。
 * <ul>
 *   <li>封闭词表：子分类（{@link #RETALIATION}、{@link #STEED}、{@link #FLYING}、{@link #IDENTITY}）通过
 *       {@link #getParent} 指向顶层父类型，顶层类型父节点为空。</li>
 *   <li>每只蛊虫通过 {@link GuProperties#type} 至多绑定一个用途类型，也可留空（如寿蛊[Lifespan Gu]、尸蛊无用途类型，提
 *       示框不显示该行）；生命周期分类（一次性消耗、持续照顾型[Tended Gu]、照顾后消耗型）由物品子类表达，不放入本枚
 *       举。</li>
 * </ul>
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
