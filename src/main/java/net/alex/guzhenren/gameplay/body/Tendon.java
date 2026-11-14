package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * 肉身的筋：原生，或钢筋蛊[Steel Tendon Gu]永久留下的三转、四转钢筋；{@code BodyData} 一槽，重生保留。
 *
 * <ul>
 *   <li>钢筋加承受力（力量点[Strength Points]），{@code StrengthService.getNormalCapacity} 加到该转基础值，
 *       数值归本枚举。</li>
 *   <li>同族按声明顺序，新品阶只替换其前各级（{@link #canReplace}）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyData
 * @see BodyGrade
 * @since 1.0.0
 */

public enum Tendon implements BodyGrade<Tendon> {

    NATIVE(0, 0),
    STEEL_TENDON_3(3, 128),
    STEEL_TENDON_4(4, 256);

    public static final Codec<Tendon> CODEC = StringRepresentable.fromEnum(Tendon::values);
    private final int rank;
    private final int capacityBonus;

    Tendon(int rank, int capacityBonus) {
        this.rank = rank;
        this.capacityBonus = capacityBonus;
    }

    @Override
    public int getRank() { return rank; }

    public int getCapacityBonus() { return capacityBonus; }

    @Override
    public boolean isNative() { return this == NATIVE; }

    @Override
    public boolean canReplace(@NotNull Tendon worn) { return worn.ordinal() < ordinal(); }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.tendon."; }
}
