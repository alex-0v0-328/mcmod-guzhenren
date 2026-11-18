package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * 肉身筋络品阶枚举：涵盖原生筋，以及三转、四转钢筋蛊[Steel Tendon Gu]永久改造的钢筋档位（存于 {@link BodyData}，重生保
 * 留）。
 * <ul>
 *   <li>钢筋提供肉身承受力加成（力量点[Strength Points]，由 {@code StrengthService.getNormalCapacity} 计入基础承受
 *       力），数值唯一定义在本枚举中。</li>
 *   <li>同族按声明顺序由低到高排列，新品阶仅能替换低于自身的旧档位（{@link #canReplace}）。</li>
 * </ul>
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
