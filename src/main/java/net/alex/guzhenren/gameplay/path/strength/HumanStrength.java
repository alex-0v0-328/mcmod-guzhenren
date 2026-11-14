package net.alex.guzhenren.gameplay.path.strength;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 肉体累积的人力，以斤、钧计。存于 {@code PathStrengthData}；五种层数上限合计恰为 99,999 斤；兄弟模组不得新增；
 * 1 斤须等于 1 力量点[Strength Points]（{@code PathStrengthData.ATTACK_PER_POINT}），否则破坏与兽力[Beast Strength]
 * 共用的结算；可发挥多少只归 {@code StrengthService}。
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthPathBranch
 * @since 1.0.0
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
