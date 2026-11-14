package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * 肉身的皮：原生、力道防御蛊暂借的玉皮，或蛊永久留下的一至四转铜皮、铁皮。
 *
 * <ul>
 *   <li>{@code BodyData} 一槽，一次一层；数值归本枚举：皮系、转、固定减免（每次可抵挡伤害的扣除点数，
 *       {@link #reduce}）、承受力加成（力量点[Strength Points]，穿着时 {@code StrengthService.getNormalCapacity}
 *       加上）。</li>
 *   <li>玉皮不加承受力：承受力在心跳间读取而限时皮只在心跳蜕下，否则加成会多留至多 1 秒；玉皮时长归物品注册。</li>
 *   <li>玉皮系限时（{@link #isTimed}），耗尽由心跳蜕下；其余永久，重生保留。</li>
 *   <li>{@link #canReplace}：任何皮可盖原生；永久皮只盖同系低转（三转铜皮盖一转铜皮）；玉皮只盖原生，两种玉皮类蛊互不叠
 *       加、替换、延长，耗尽前无蛊可替。</li>
 *   <li>⚠ 皮是肉身档位，不是玩家贴图：{@code SkinFlashPayload} 的闪现层只叠色，不换贴图。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyData
 * @see BodyGrade
 * @since 1.0.0
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
