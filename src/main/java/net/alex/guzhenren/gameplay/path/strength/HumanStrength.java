package net.alex.guzhenren.gameplay.path.strength;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 肉体永久累积的人力枚举（以斤、钧为单位，存于 {@link PathStrengthData#humanStrength}，五档满层合计 99,999 斤）。
 * <ul>
 *   <li>⚠ 1 斤人力严格等于 1 点力量点[Strength Points]（对应 {@link PathStrengthData#ATTACK_PER_POINT} 空手攻击力），
 *       保证人力与兽力[Beast Strength]共用同一套准入、承受力与攻击力结算；结算力量点唯一由 {@link StrengthService}
 *       得出。</li>
 *   <li>人力暴击不加点数（只放范围伤害）；高档人力一层的点数超过凡人承受力时过不了准入（百钧之力留给蛊仙阶段）。</li>
 * </ul>
 */

public enum HumanStrength implements NamedEnum {

    JIN(1, 9),
    TEN_JIN(10, 9),
    JUN(30, 30),
    TEN_JUN(300, 30),
    HUNDRED_JUN(3_000, 30);

    public static final Codec<HumanStrength> CODEC = StringRepresentable.fromEnum(HumanStrength::values);
    public static final int TEN_FACTOR = 10;
    public static final int HUNDRED_FACTOR = 100;
    private final int jin;
    private final int maxLayers;

    HumanStrength(int jin, int maxLayers) {
        this.jin = jin;
        this.maxLayers = maxLayers;
    }

    public int getJin() { return jin; }

    public int getMaxLayers() { return maxLayers; }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.strength.human_strength."; }
}
