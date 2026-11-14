package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * 肉身的骨：原生，或铁骨蛊[Iron Bone Gu]永久留下的三转、四转铁骨；{@code BodyData} 一槽，重生保留。
 *
 * <ul>
 *   <li>铁骨加最大生命值（生命点）、击退抗性（原版 0–1）、承受力（力量点[Strength Points]），由
 *       {@code MaxHealthService}、{@code KnockbackResistanceService}、{@code StrengthService} 读取，数值归本枚举。</li>
 *   <li>同族按声明顺序，新品阶只替换其前各级（{@link #canReplace}）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyData
 * @see BodyGrade
 * @since 1.0.0
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
