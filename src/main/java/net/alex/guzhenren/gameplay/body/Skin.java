package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * 肉身皮肤品阶枚举：涵盖原生皮、力道防御蛊赋予的限时玉皮/白玉皮，以及一至四转铜皮蛊、铁皮蛊永久改造的铜皮与铁皮。
 * <ul>
 *   <li>在 {@link BodyData} 中占一槽（同一时刻仅有一层皮肤），统一定义皮肤系别、转数、每次可抵挡伤害的固定减免点数
 *       （{@link #reduce}）与肉身承受力加成（力量点[Strength Points]，由 {@code StrengthService.getNormalCapacity} 计
 *       入）。</li>
 *   <li>玉皮系属于限时皮肤（{@link #isTimed}，持续时间由物品定义，到期由心跳脱下），不提供承受力加成（以免承受力在两次
 *       心跳之间残留最多 1 秒）；铜皮系与铁皮系为永久改造，死亡重生后保留。</li>
 *   <li>{@link #canReplace} 替换规则：任何皮肤均可替换原生皮；永久皮仅能替换同系更低转数的皮肤；限时玉皮仅能替换原生皮
 *       （玉皮与白玉皮互不叠加、替换或续期）。</li>
 *   <li>⚠ 本枚举表示肉身防御档位而非玩家外观贴图，{@code SkinFlashPayload} 仅在受击或穿戴时叠加闪光染色。</li>
 * </ul>
 */

public enum Skin implements BodyGrade<Skin> {

    NATIVE(Family.NATIVE, 0, 0, 0),
    JADE_SKIN(Family.JADE, 1, 2, 0),
    WHITE_JADE_SKIN(Family.JADE, 2, 4, 0),
    BRONZE_SKIN_1(Family.BRONZE, 1, 1, 4),
    BRONZE_SKIN_2(Family.BRONZE, 2, 2, 8),
    BRONZE_SKIN_3(Family.BRONZE, 3, 3, 16),
    BRONZE_SKIN_4(Family.BRONZE, 4, 5, 32),
    IRON_SKIN_1(Family.IRON, 1, 2, 4),
    IRON_SKIN_2(Family.IRON, 2, 3, 12),
    IRON_SKIN_3(Family.IRON, 3, 4, 36),
    IRON_SKIN_4(Family.IRON, 4, 6, 108);

    public enum Family { NATIVE, JADE, BRONZE, IRON }

    public static final Codec<Skin> CODEC = StringRepresentable.fromEnum(Skin::values);
    private final Family family;
    private final int rank;
    private final int damageReduction;
    private final int capacityBonus;

    Skin(Family family, int rank, int damageReduction, int capacityBonus) {
        this.family = family;
        this.rank = rank;
        this.damageReduction = damageReduction;
        this.capacityBonus = capacityBonus;
    }

    public @NotNull Family getFamily() { return family; }

    @Override
    public int getRank() { return rank; }

    public int getDamageReduction() { return damageReduction; }

    public int getCapacityBonus() { return capacityBonus; }

    @Override
    public boolean isNative() { return this == NATIVE; }

    public boolean isTimed() { return family == Family.JADE; }

    @Override
    public boolean canReplace(@NotNull Skin worn) {
        if (worn.isNative()) return true;
        return !isTimed() && worn.family == family && worn.rank < rank;
    }

    public float reduce(float amount) { return Math.max(0.0F, amount - damageReduction); }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.skin."; }
}
