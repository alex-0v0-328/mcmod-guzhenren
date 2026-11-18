package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * 肉身骨骼品阶枚举：涵盖原生骨，以及三转、四转铁骨蛊[Iron Bone Gu]永久改造的铁骨档位（存于 {@link BodyData}，重生保
 * 留）。
 * <ul>
 *   <li>铁骨同时提供最大生命值加成、原版击退抗性（{@code 0..1}）与肉身承受力（力量点[Strength Points]），分别由
 *       {@code MaxHealthService}、{@code KnockbackResistanceService} 与 {@code StrengthService} 读取，数值唯一定义在本
 *       枚举中。</li>
 *   <li>同族按声明顺序由低到高排列，新品阶仅能替换低于自身的旧档位（{@link #canReplace}）。</li>
 * </ul>
 */

public enum Bone implements BodyGrade<Bone> {

    NATIVE(0, 0.0D, 0.0D, 0),
    IRON_BONE_3(3, 4.0D, 0.3D, 128),
    IRON_BONE_4(4, 10.0D, 0.6D, 256);

    public static final Codec<Bone> CODEC = StringRepresentable.fromEnum(Bone::values);
    private final int rank;
    private final double maxHealthBonus;
    private final double knockbackResistance;
    private final int capacityBonus;

    Bone(int rank, double maxHealthBonus, double knockbackResistance, int capacityBonus) {
        this.rank = rank;
        this.maxHealthBonus = maxHealthBonus;
        this.knockbackResistance = knockbackResistance;
        this.capacityBonus = capacityBonus;
    }

    @Override
    public int getRank() { return rank; }

    public double getMaxHealthBonus() { return maxHealthBonus; }

    public double getKnockbackResistance() { return knockbackResistance; }

    public int getCapacityBonus() { return capacityBonus; }

    @Override
    public boolean isNative() { return this == NATIVE; }

    @Override
    public boolean canReplace(@NotNull Bone worn) { return worn.ordinal() < ordinal(); }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.bone."; }
}
