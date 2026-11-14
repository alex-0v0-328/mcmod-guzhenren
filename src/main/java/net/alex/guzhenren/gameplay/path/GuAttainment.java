package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

/**
 * 一条道[Path]上的造诣，从无到最高级；闭合枚举，与 {@link GuPath} 配对。
 *
 * <ul>
 *   <li>每条道在 {@code PathEntry} 存一个；{@code shift(int)} 两端夹紧，调用方不碰 {@code ordinal()}。</li>
 *   <li>⚠ 每级炼蛊加成写在本枚举，新增一级只加一行；别把这张表挪进读它的代码。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see GuPath
 * @since 1.0.0
 */

public enum GuAttainment implements NamedEnum {

    NONE(0),
    ORDINARY(2),
    QUASI_MASTER(5),
    MASTER(10),
    QUASI_GRANDMASTER(20),
    GRANDMASTER(40),
    QUASI_GREAT_GRANDMASTER(50),
    GREAT_GRANDMASTER(70),
    QUASI_SUPREME_GRANDMASTER(80),
    SUPREME_GRANDMASTER(100);

    public static final Codec<GuAttainment> CODEC = StringRepresentable.fromEnum(GuAttainment::values);
    private final int refinementBonus;

    GuAttainment(int refinementBonus) { this.refinementBonus = refinementBonus; }

    public int getRefinementBonus() { return refinementBonus; }

    public GuAttainment shift(int delta) { return values()[Mth.clamp(ordinal() + delta, 0, values().length - 1)]; }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.path.attainment."; }
}
