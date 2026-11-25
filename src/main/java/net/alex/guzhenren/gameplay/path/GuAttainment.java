package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

/**
 * 单条流派（{@link GuPath}）上的境界造诣枚举：从无造诣（{@link #NONE}）至无上大宗师（{@link #SUPREME_GRANDMASTER}）。
 * <ul>
 *   <li>每条流派在 {@link PathEntry} 中记录一档造诣；{@link #shift} 自动夹取在枚举首尾之间，外部调用方无需直接操作
 *       {@code ordinal()}。</li>
 *   <li>⚠ 各档造诣提供的炼蛊成功率加成（{@link #getRefinementBonus}）唯一定义在本枚举中，切勿在外部业务代码中重复维护
 *       数值表。</li>
 * </ul>
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
